#!/bin/bash

# ====================================
# Autocoin API Server Setup Script
# ====================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 Autocoin API Server Setup${NC}"

# Check if running as root
if [ "$EUID" -ne 0 ]; then
    echo -e "${RED}❌ This script must be run as root${NC}"
    echo -e "${YELLOW}💡 Run: sudo $0${NC}"
    exit 1
fi

# Configuration
DEPLOY_USER="deploy"
DEPLOY_HOME="/home/$DEPLOY_USER"
DEPLOY_PATH="/opt/autocoin"

echo -e "${BLUE}📋 System Information${NC}"
echo "OS: $(lsb_release -d | cut -f2)"
echo "Kernel: $(uname -r)"
echo "Architecture: $(uname -m)"
echo "Memory: $(free -h | grep Mem | awk '{print $2}')"
echo "Disk: $(df -h / | tail -1 | awk '{print $2}')"

# Functions
update_system() {
    echo -e "${BLUE}📦 Updating system packages...${NC}"
    
    apt update && apt upgrade -y
    apt install -y \
        curl \
        wget \
        git \
        htop \
        nano \
        vim \
        unzip \
        software-properties-common \
        apt-transport-https \
        ca-certificates \
        gnupg \
        lsb-release \
        fail2ban \
        ufw \
        jq \
        openssl \
        rsync
    
    echo -e "${GREEN}✅ System packages updated${NC}"
}

install_docker() {
    echo -e "${BLUE}🐳 Installing Docker...${NC}"
    
    # Remove old versions
    apt remove -y docker docker-engine docker.io containerd runc || true
    
    # Add Docker's official GPG key
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
    
    # Add Docker repository
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null
    
    # Install Docker
    apt update
    apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
    
    # Enable and start Docker
    systemctl enable docker
    systemctl start docker
    
    # Add deploy user to docker group
    usermod -aG docker $DEPLOY_USER
    
    echo -e "${GREEN}✅ Docker installed${NC}"
}

install_docker_compose() {
    echo -e "${BLUE}🔧 Installing Docker Compose...${NC}"
    
    # Docker Compose is now included with Docker, but let's ensure we have the latest standalone version
    COMPOSE_VERSION=$(curl -s https://api.github.com/repos/docker/compose/releases/latest | jq -r .tag_name)
    curl -L "https://github.com/docker/compose/releases/download/$COMPOSE_VERSION/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose
    
    # Create symlink for compatibility
    ln -sf /usr/local/bin/docker-compose /usr/bin/docker-compose
    
    echo -e "${GREEN}✅ Docker Compose installed: $COMPOSE_VERSION${NC}"
}

setup_firewall() {
    echo -e "${BLUE}🔥 Configuring firewall (UFW)...${NC}"
    
    # Reset UFW to defaults
    ufw --force reset
    
    # Default policies
    ufw default deny incoming
    ufw default allow outgoing
    
    # Allow SSH (important - don't lock yourself out!)
    ufw allow ssh
    ufw allow 22/tcp
    
    # Allow HTTP and HTTPS
    ufw allow 80/tcp
    ufw allow 443/tcp
    
    # Allow specific application ports (only from specific IPs if needed)
    # These will be restricted by nginx proxy
    # ufw allow from 10.0.0.0/8 to any port 8080 # Spring Boot
    # ufw allow from 10.0.0.0/8 to any port 3306 # MySQL
    # ufw allow from 10.0.0.0/8 to any port 6379 # Redis
    # ufw allow from 10.0.0.0/8 to any port 3000 # Grafana
    # ufw allow from 10.0.0.0/8 to any port 9090 # Prometheus
    
    # Enable UFW
    ufw --force enable
    
    echo -e "${GREEN}✅ Firewall configured${NC}"
    ufw status
}

setup_fail2ban() {
    echo -e "${BLUE}🛡️  Configuring Fail2ban...${NC}"
    
    # Create custom jail configuration
    cat > /etc/fail2ban/jail.local << 'EOF'
[DEFAULT]
# Ignore local IPs
ignoreip = 127.0.0.1/8 ::1 10.0.0.0/8 172.16.0.0/12 192.168.0.0/16

# Ban time (in seconds) - 1 hour
bantime = 3600

# Find time window (in seconds) - 10 minutes
findtime = 600

# Max retries before ban
maxretry = 5

# Email settings (configure if needed)
# destemail = admin@autocoin.com
# sendername = Fail2Ban
# mta = sendmail

[sshd]
enabled = true
port = ssh
filter = sshd
logpath = /var/log/auth.log
maxretry = 3

[nginx-http-auth]
enabled = true
filter = nginx-http-auth
port = http,https
logpath = /var/log/nginx/error.log
maxretry = 5

[nginx-noscript]
enabled = true
filter = nginx-noscript
port = http,https
logpath = /var/log/nginx/access.log
maxretry = 6

[nginx-bad-request]
enabled = true
filter = nginx-bad-request
port = http,https
logpath = /var/log/nginx/access.log
maxretry = 2

[nginx-botsearch]
enabled = true
filter = nginx-botsearch
port = http,https
logpath = /var/log/nginx/access.log
maxretry = 2
EOF

    # Create nginx filters for fail2ban
    cat > /etc/fail2ban/filter.d/nginx-bad-request.conf << 'EOF'
[Definition]
failregex = ^<HOST> -.*"(GET|POST).*HTTP.*" (444|403|400|401) .*$
ignoreregex =
EOF

    cat > /etc/fail2ban/filter.d/nginx-botsearch.conf << 'EOF'
[Definition]
failregex = ^<HOST> -.*"(GET|POST).*(\.php|\.asp|\.exe|\.pl|\.cgi|\.scgi).*HTTP.*" .*$
ignoreregex =
EOF

    # Enable and start fail2ban
    systemctl enable fail2ban
    systemctl restart fail2ban
    
    echo -e "${GREEN}✅ Fail2ban configured${NC}"
}

create_deploy_user() {
    echo -e "${BLUE}👤 Creating deploy user...${NC}"
    
    # Create user if doesn't exist
    if ! id "$DEPLOY_USER" &>/dev/null; then
        useradd -m -s /bin/bash $DEPLOY_USER
        echo -e "${GREEN}✅ Created user: $DEPLOY_USER${NC}"
    else
        echo -e "${YELLOW}⚠️  User $DEPLOY_USER already exists${NC}"
    fi
    
    # Add to sudo group
    usermod -aG sudo $DEPLOY_USER
    
    # Create SSH directory
    mkdir -p $DEPLOY_HOME/.ssh
    chmod 700 $DEPLOY_HOME/.ssh
    chown $DEPLOY_USER:$DEPLOY_USER $DEPLOY_HOME/.ssh
    
    # Create deployment directory
    mkdir -p $DEPLOY_PATH
    chown $DEPLOY_USER:$DEPLOY_USER $DEPLOY_PATH
    
    # Set up sudo without password for deploy user (for deployment scripts)
    echo "$DEPLOY_USER ALL=(ALL) NOPASSWD: /bin/systemctl, /usr/bin/docker, /usr/local/bin/docker-compose, /usr/bin/docker-compose" > /etc/sudoers.d/$DEPLOY_USER
    
    echo -e "${GREEN}✅ Deploy user configured${NC}"
    echo -e "${YELLOW}💡 Add your SSH public key to $DEPLOY_HOME/.ssh/authorized_keys${NC}"
}

setup_log_rotation() {
    echo -e "${BLUE}📋 Setting up log rotation...${NC}"
    
    # Create log rotation configuration for autocoin
    cat > /etc/logrotate.d/autocoin << 'EOF'
/opt/autocoin/logs/*.log {
    daily
    missingok
    rotate 30
    compress
    delaycompress
    notifempty
    create 644 deploy deploy
    postrotate
        # Restart application container to reopen log files
        /usr/bin/docker exec autocoin-api-prod pkill -USR1 java || true
    endscript
}

/var/log/nginx/*.log {
    daily
    missingok
    rotate 30
    compress
    delaycompress
    notifempty
    create 644 www-data www-data
    postrotate
        /bin/systemctl reload nginx
    endscript
}
EOF
    
    echo -e "${GREEN}✅ Log rotation configured${NC}"
}

optimize_system() {
    echo -e "${BLUE}⚡ Optimizing system performance...${NC}"
    
    # Set kernel parameters for better network performance
    cat >> /etc/sysctl.conf << 'EOF'

# Network optimizations for high load
net.core.rmem_max = 16777216
net.core.wmem_max = 16777216
net.ipv4.tcp_rmem = 4096 65536 16777216
net.ipv4.tcp_wmem = 4096 65536 16777216
net.ipv4.tcp_congestion_control = bbr
net.core.netdev_max_backlog = 5000
net.ipv4.tcp_max_syn_backlog = 8192
net.ipv4.tcp_slow_start_after_idle = 0

# File descriptor limits
fs.file-max = 2097152

# Virtual memory settings
vm.swappiness = 10
vm.dirty_ratio = 15
vm.dirty_background_ratio = 5
EOF

    # Apply sysctl settings
    sysctl -p
    
    # Set file descriptor limits
    cat >> /etc/security/limits.conf << 'EOF'

# File descriptor limits for deploy user
deploy soft nofile 65536
deploy hard nofile 65536
root soft nofile 65536
root hard nofile 65536
EOF

    # Configure systemd limits
    mkdir -p /etc/systemd/system.conf.d
    cat > /etc/systemd/system.conf.d/limits.conf << 'EOF'
[Manager]
DefaultLimitNOFILE=65536
EOF

    echo -e "${GREEN}✅ System optimized${NC}"
}

install_monitoring_tools() {
    echo -e "${BLUE}📊 Installing monitoring tools...${NC}"
    
    # Install htop, iotop, netstat, etc.
    apt install -y \
        htop \
        iotop \
        nethogs \
        net-tools \
        sysstat \
        dstat \
        ncdu \
        tree \
        zip \
        unzip
    
    echo -e "${GREEN}✅ Monitoring tools installed${NC}"
}

setup_timezone() {
    echo -e "${BLUE}🕒 Setting up timezone...${NC}"
    
    # Set timezone to Asia/Seoul (adjust as needed)
    timedatectl set-timezone Asia/Seoul
    
    # Enable NTP synchronization
    timedatectl set-ntp true
    
    echo -e "${GREEN}✅ Timezone set to Asia/Seoul${NC}"
    echo "Current time: $(date)"
}

create_basic_scripts() {
    echo -e "${BLUE}📝 Creating basic management scripts...${NC}"
    
    # Create system status script
    cat > /usr/local/bin/system-status << 'EOF'
#!/bin/bash
echo "=== System Status ==="
echo "Date: $(date)"
echo "Uptime: $(uptime)"
echo "Load: $(cat /proc/loadavg)"
echo "Memory: $(free -h | grep Mem | awk '{print "Used:", $3, "Free:", $7}')"
echo "Disk: $(df -h / | tail -1 | awk '{print "Used:", $3, "Available:", $4, "Use%:", $5}')"
echo "Network connections: $(ss -tuln | wc -l)"
echo ""
echo "=== Docker Status ==="
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo ""
echo "=== Autocoin Services ==="
systemctl status docker --no-pager -l
echo ""
echo "=== Recent logs ==="
journalctl -n 10 --no-pager
EOF
    
    chmod +x /usr/local/bin/system-status
    
    # Create cleanup script
    cat > /usr/local/bin/cleanup-system << 'EOF'
#!/bin/bash
echo "=== System Cleanup ==="
echo "Cleaning package cache..."
apt autoremove -y
apt autoclean

echo "Cleaning Docker..."
docker system prune -f

echo "Cleaning logs..."
journalctl --vacuum-time=30d

echo "Cleaning temporary files..."
find /tmp -type f -atime +7 -delete
find /var/tmp -type f -atime +7 -delete

echo "=== Cleanup Complete ==="
df -h
EOF
    
    chmod +x /usr/local/bin/cleanup-system
    
    echo -e "${GREEN}✅ Management scripts created${NC}"
    echo -e "${YELLOW}💡 Use 'system-status' to check system status${NC}"
    echo -e "${YELLOW}💡 Use 'cleanup-system' to clean up the system${NC}"
}

print_summary() {
    echo -e "${GREEN}🎉 Server setup completed successfully!${NC}"
    echo ""
    echo -e "${BLUE}📋 Setup Summary:${NC}"
    echo "✅ System packages updated"
    echo "✅ Docker and Docker Compose installed"
    echo "✅ Firewall (UFW) configured"
    echo "✅ Fail2ban security setup"
    echo "✅ Deploy user created"
    echo "✅ Log rotation configured"
    echo "✅ System optimized"
    echo "✅ Monitoring tools installed"
    echo "✅ Timezone configured"
    echo "✅ Management scripts created"
    echo ""
    echo -e "${BLUE}🔑 Next Steps:${NC}"
    echo "1. Add your SSH public key to $DEPLOY_HOME/.ssh/authorized_keys"
    echo "2. Test SSH access with the deploy user"
    echo "3. Configure your .env.prod file"
    echo "4. Run the deployment script"
    echo ""
    echo -e "${BLUE}💡 Useful Commands:${NC}"
    echo "• system-status    - Check system status"
    echo "• cleanup-system   - Clean up system"
    echo "• fail2ban-client status  - Check fail2ban status"
    echo "• ufw status       - Check firewall status"
    echo "• docker ps        - Check running containers"
    echo ""
    echo -e "${YELLOW}⚠️  Security Notes:${NC}"
    echo "• Change default passwords"
    echo "• Regularly update the system"
    echo "• Monitor logs for suspicious activity"
    echo "• Keep backups of important data"
}

# Main setup process
echo -e "${BLUE}🚀 Starting server setup...${NC}"

update_system
install_docker
install_docker_compose
setup_firewall
setup_fail2ban
create_deploy_user
setup_log_rotation
optimize_system
install_monitoring_tools
setup_timezone
create_basic_scripts
print_summary

echo -e "${GREEN}✅ Server setup script completed!${NC}"
