#!/bin/bash

# ====================================
# SSL Certificate Setup Script for Autocoin API
# ====================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
DOMAINS="autocoin.com www.autocoin.com api.autocoin.com monitoring.autocoin.com"
EMAIL=${SSL_EMAIL:-"admin@autocoin.com"}
SSL_DIR="/opt/autocoin/ssl"
NGINX_SSL_DIR="/etc/nginx/ssl"

echo -e "${BLUE}🔐 SSL Certificate Setup for Autocoin API${NC}"

# Functions
check_requirements() {
    echo -e "${BLUE}📋 Checking requirements...${NC}"
    
    # Check if running as root
    if [ "$EUID" -ne 0 ]; then
        echo -e "${RED}❌ This script must be run as root${NC}"
        echo -e "${YELLOW}💡 Run: sudo $0 $*${NC}"
        exit 1
    fi
    
    # Check if certbot is installed
    if ! command -v certbot &> /dev/null; then
        echo -e "${YELLOW}📦 Installing certbot...${NC}"
        apt update
        apt install -y certbot python3-certbot-nginx
    fi
    
    # Check if nginx is running
    if ! systemctl is-active --quiet nginx; then
        echo -e "${YELLOW}🔄 Starting nginx...${NC}"
        systemctl start nginx
        systemctl enable nginx
    fi
    
    echo -e "${GREEN}✅ Requirements check passed${NC}"
}

create_ssl_directories() {
    echo -e "${BLUE}📁 Creating SSL directories...${NC}"
    
    mkdir -p $SSL_DIR
    mkdir -p $NGINX_SSL_DIR
    chown -R deploy:deploy $SSL_DIR
    
    echo -e "${GREEN}✅ SSL directories created${NC}"
}

generate_self_signed_certificates() {
    echo -e "${BLUE}🔨 Generating self-signed certificates...${NC}"
    
    # Create certificate configuration
    cat > $SSL_DIR/autocoin.conf << EOF
[req]
distinguished_name = req_distinguished_name
x509_extensions = v3_req
prompt = no

[req_distinguished_name]
C = KR
ST = Seoul
L = Seoul
O = Autocoin
OU = IT Department
CN = autocoin.com

[v3_req]
keyUsage = keyEncipherment, dataEncipherment
extendedKeyUsage = serverAuth
subjectAltName = @alt_names

[alt_names]
DNS.1 = autocoin.com
DNS.2 = www.autocoin.com
DNS.3 = api.autocoin.com
DNS.4 = monitoring.autocoin.com
DNS.5 = localhost
IP.1 = 127.0.0.1
EOF

    # Generate private key and certificate
    openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
        -keyout $SSL_DIR/autocoin.com.key \
        -out $SSL_DIR/autocoin.com.crt \
        -config $SSL_DIR/autocoin.conf
    
    # Set permissions
    chmod 600 $SSL_DIR/autocoin.com.key
    chmod 644 $SSL_DIR/autocoin.com.crt
    
    # Copy to nginx directory
    cp $SSL_DIR/autocoin.com.crt $NGINX_SSL_DIR/
    cp $SSL_DIR/autocoin.com.key $NGINX_SSL_DIR/
    
    echo -e "${GREEN}✅ Self-signed certificates generated${NC}"
    echo -e "${YELLOW}⚠️  Warning: Self-signed certificates are not trusted by browsers${NC}"
    echo -e "${YELLOW}💡 For production, use Let's Encrypt: $0 letsencrypt${NC}"
}

setup_letsencrypt() {
    echo -e "${BLUE}🔒 Setting up Let's Encrypt certificates...${NC}"
    
    # Stop nginx temporarily
    systemctl stop nginx
    
    # Generate certificates using standalone mode
    certbot certonly --standalone \
        --email $EMAIL \
        --agree-tos \
        --no-eff-email \
        --domains $DOMAINS \
        --expand
    
    # Copy certificates to our SSL directory
    DOMAIN="autocoin.com"
    cp /etc/letsencrypt/live/$DOMAIN/fullchain.pem $SSL_DIR/autocoin.com.crt
    cp /etc/letsencrypt/live/$DOMAIN/privkey.pem $SSL_DIR/autocoin.com.key
    
    # Copy to nginx directory
    cp $SSL_DIR/autocoin.com.crt $NGINX_SSL_DIR/
    cp $SSL_DIR/autocoin.com.key $NGINX_SSL_DIR/
    
    # Set permissions
    chmod 600 $SSL_DIR/autocoin.com.key $NGINX_SSL_DIR/autocoin.com.key
    chmod 644 $SSL_DIR/autocoin.com.crt $NGINX_SSL_DIR/autocoin.com.crt
    
    # Create renewal hook
    cat > /etc/letsencrypt/renewal-hooks/deploy/autocoin-reload.sh << 'EOF'
#!/bin/bash
# Copy renewed certificates to autocoin directories
DOMAIN="autocoin.com"
SSL_DIR="/opt/autocoin/ssl"
NGINX_SSL_DIR="/etc/nginx/ssl"

cp /etc/letsencrypt/live/$DOMAIN/fullchain.pem $SSL_DIR/autocoin.com.crt
cp /etc/letsencrypt/live/$DOMAIN/privkey.pem $SSL_DIR/autocoin.com.key
cp $SSL_DIR/autocoin.com.crt $NGINX_SSL_DIR/
cp $SSL_DIR/autocoin.com.key $NGINX_SSL_DIR/

# Set permissions
chmod 600 $SSL_DIR/autocoin.com.key $NGINX_SSL_DIR/autocoin.com.key
chmod 644 $SSL_DIR/autocoin.com.crt $NGINX_SSL_DIR/autocoin.com.crt

# Reload nginx
systemctl reload nginx

# Restart autocoin containers
cd /opt/autocoin
docker-compose -f docker/docker-compose.prod.yml restart nginx
EOF
    
    chmod +x /etc/letsencrypt/renewal-hooks/deploy/autocoin-reload.sh
    
    # Setup auto-renewal cron job
    echo "0 12 * * * /usr/bin/certbot renew --quiet" | crontab -
    
    # Start nginx
    systemctl start nginx
    
    echo -e "${GREEN}✅ Let's Encrypt certificates configured${NC}"
    echo -e "${GREEN}✅ Auto-renewal cron job created${NC}"
}

test_certificates() {
    echo -e "${BLUE}🧪 Testing SSL certificates...${NC}"
    
    # Check if certificate files exist
    if [ ! -f "$SSL_DIR/autocoin.com.crt" ] || [ ! -f "$SSL_DIR/autocoin.com.key" ]; then
        echo -e "${RED}❌ Certificate files not found${NC}"
        return 1
    fi
    
    # Check certificate validity
    if openssl x509 -in $SSL_DIR/autocoin.com.crt -noout -checkend 86400; then
        echo -e "${GREEN}✅ Certificate is valid for at least 24 hours${NC}"
    else
        echo -e "${YELLOW}⚠️  Certificate expires within 24 hours${NC}"
    fi
    
    # Show certificate info
    echo -e "${BLUE}📋 Certificate Information:${NC}"
    openssl x509 -in $SSL_DIR/autocoin.com.crt -noout -subject -dates -issuer
    
    # Test SSL configuration (if nginx is running)
    if systemctl is-active --quiet nginx; then
        echo -e "${BLUE}🌐 Testing SSL configuration...${NC}"
        
        for domain in $DOMAINS; do
            if curl -I -s --connect-timeout 5 https://$domain > /dev/null 2>&1; then
                echo -e "${GREEN}✅ $domain: SSL working${NC}"
            else
                echo -e "${YELLOW}⚠️  $domain: SSL test failed (may be normal if DNS not configured)${NC}"
            fi
        done
    fi
}

renew_certificates() {
    echo -e "${BLUE}🔄 Renewing SSL certificates...${NC}"
    
    if [ -d "/etc/letsencrypt/live/autocoin.com" ]; then
        # Let's Encrypt renewal
        certbot renew --force-renewal
        
        # Run the renewal hook
        /etc/letsencrypt/renewal-hooks/deploy/autocoin-reload.sh
        
        echo -e "${GREEN}✅ Let's Encrypt certificates renewed${NC}"
    else
        echo -e "${YELLOW}⚠️  No Let's Encrypt certificates found${NC}"
        echo -e "${YELLOW}💡 Use: $0 letsencrypt to set up Let's Encrypt${NC}"
    fi
}

show_status() {
    echo -e "${BLUE}📊 SSL Certificate Status${NC}"
    echo "=========================="
    
    if [ -f "$SSL_DIR/autocoin.com.crt" ]; then
        echo -e "${GREEN}✅ Certificate file exists${NC}"
        
        # Show expiration date
        EXPIRY=$(openssl x509 -in $SSL_DIR/autocoin.com.crt -noout -enddate | cut -d= -f2)
        echo -e "${BLUE}📅 Expires: $EXPIRY${NC}"
        
        # Show days until expiry
        EXPIRY_SECONDS=$(date -d "$EXPIRY" +%s)
        CURRENT_SECONDS=$(date +%s)
        DAYS_LEFT=$(( ($EXPIRY_SECONDS - $CURRENT_SECONDS) / 86400 ))
        
        if [ $DAYS_LEFT -gt 30 ]; then
            echo -e "${GREEN}✅ $DAYS_LEFT days until expiry${NC}"
        elif [ $DAYS_LEFT -gt 7 ]; then
            echo -e "${YELLOW}⚠️  $DAYS_LEFT days until expiry (renewal recommended)${NC}"
        else
            echo -e "${RED}❌ $DAYS_LEFT days until expiry (renewal required!)${NC}"
        fi
        
        # Show certificate details
        echo -e "${BLUE}📋 Certificate Details:${NC}"
        openssl x509 -in $SSL_DIR/autocoin.com.crt -noout -subject -issuer
        
        # Show SAN (Subject Alternative Names)
        echo -e "${BLUE}🌐 Covered Domains:${NC}"
        openssl x509 -in $SSL_DIR/autocoin.com.crt -noout -text | grep -A1 "Subject Alternative Name" | tail -1 | sed 's/^ *//'
        
    else
        echo -e "${RED}❌ No certificate found${NC}"
        echo -e "${YELLOW}💡 Run: $0 self-signed OR $0 letsencrypt${NC}"
    fi
    
    # Check Let's Encrypt status
    if [ -d "/etc/letsencrypt/live/autocoin.com" ]; then
        echo -e "${BLUE}🔐 Let's Encrypt Status:${NC}"
        certbot certificates | grep -A5 "autocoin.com"
    fi
}

auto_setup() {
    echo -e "${BLUE}🤖 Automatic SSL setup${NC}"
    
    # Check if we have internet and can reach Let's Encrypt
    if curl -s --connect-timeout 5 https://acme-v02.api.letsencrypt.org/directory > /dev/null; then
        echo -e "${GREEN}✅ Internet connection available${NC}"
        echo -e "${BLUE}🔐 Setting up Let's Encrypt certificates...${NC}"
        
        check_requirements
        create_ssl_directories
        setup_letsencrypt
        test_certificates
        
    else
        echo -e "${YELLOW}⚠️  No internet connection or Let's Encrypt not reachable${NC}"
        echo -e "${BLUE}🔨 Setting up self-signed certificates...${NC}"
        
        check_requirements
        create_ssl_directories
        generate_self_signed_certificates
        test_certificates
    fi
}

# Main script logic
case "${1:-auto}" in
    "self-signed")
        echo -e "${BLUE}🔨 Setting up self-signed certificates${NC}"
        check_requirements
        create_ssl_directories
        generate_self_signed_certificates
        test_certificates
        ;;
    
    "letsencrypt")
        echo -e "${BLUE}🔐 Setting up Let's Encrypt certificates${NC}"
        check_requirements
        create_ssl_directories
        setup_letsencrypt
        test_certificates
        ;;
    
    "renew")
        renew_certificates
        test_certificates
        ;;
    
    "test")
        test_certificates
        ;;
    
    "status")
        show_status
        ;;
    
    "auto")
        auto_setup
        ;;
    
    *)
        echo "Usage: $0 {auto|self-signed|letsencrypt|renew|test|status}"
        echo ""
        echo "Commands:"
        echo "  auto        - Automatic setup (Let's Encrypt if internet available, self-signed otherwise)"
        echo "  self-signed - Generate self-signed certificates (for development)"
        echo "  letsencrypt - Setup Let's Encrypt certificates (for production)"
        echo "  renew       - Manually renew Let's Encrypt certificates"
        echo "  test        - Test current SSL configuration"
        echo "  status      - Show current certificate status"
        echo ""
        echo "Examples:"
        echo "  $0 auto                    # Automatic setup"
        echo "  $0 letsencrypt            # Production SSL setup"
        echo "  $0 self-signed            # Development SSL setup"
        echo "  SSL_EMAIL=admin@domain.com $0 letsencrypt  # Custom email"
        exit 1
        ;;
esac

echo -e "${GREEN}🎉 SSL setup completed!${NC}"
