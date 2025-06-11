# Autocoin Production Log Monitoring Setup

이 폴더는 Autocoin API의 프로덕션 환경에서 로그 수집, 저장, 분석을 위한 ELK Stack과 모니터링 시스템을 구성합니다.

## 📊 모니터링 스택 구조

```
Autocoin API ─→ Logstash ─→ Elasticsearch ─→ Kibana (시각화)
     │                                        ↗
     ├─→ Prometheus ─→ Grafana (메트릭 대시보드)
     │                    ↗
     └─→ AlertManager ─→ Slack (알림)
```

## 🛠️ 구성 요소

### 1. **Elasticsearch** - 로그 저장소
- **용도**: 모든 애플리케이션 로그를 중앙 집중식으로 저장
- **포트**: 9200 (HTTP), 9300 (Transport)
- **데이터**: `/var/lib/elasticsearch`
- **설정**: `elasticsearch/elasticsearch.yml`

### 2. **Logstash** - 로그 수집 및 처리
- **용도**: 다양한 소스에서 로그를 수집하고 구조화하여 Elasticsearch로 전송
- **포트**: 5044 (Beats), 9600 (API)
- **파이프라인**: `logstash/logstash.conf`
- **패턴**: `logstash/patterns/`

### 3. **Kibana** - 로그 시각화 (현재 비활성화)
- **용도**: Elasticsearch 데이터 시각화 및 검색
- **포트**: 5601
- **대시보드**: 사전 구성된 로그 분석 대시보드

### 4. **Prometheus** - 메트릭 수집
- **용도**: 애플리케이션 메트릭, 시스템 리소스 모니터링
- **포트**: 9090
- **설정**: `prometheus/prometheus.yml`
- **규칙**: `prometheus/rules/`

### 5. **Grafana** - 메트릭 대시보드
- **용도**: Prometheus 데이터 시각화, 대시보드, 알림
- **포트**: 3000
- **계정**: admin / [GRAFANA_ADMIN_PASSWORD]
- **대시보드**: `grafana/dashboards/`

### 6. **AlertManager** - 알림 관리
- **용도**: Prometheus 알림 규칙 기반 Slack/이메일 알림
- **포트**: 9093
- **설정**: `alertmanager/alertmanager.yml`

### 7. **ElastAlert** - 로그 기반 알림
- **용도**: Elasticsearch 로그 패턴 기반 알림
- **설정**: `elastalert/rules/`

## 🚀 빠른 시작

### 1. 기본 설정
```bash
# 환경 변수 설정 (.env.prod 파일에)
GRAFANA_ADMIN_PASSWORD=your_secure_password
GRAFANA_SECRET_KEY=your_secret_key
SLACK_WEBHOOK_URL=your_slack_webhook_url
ELASTICSEARCH_PASSWORD=your_elasticsearch_password
```

### 2. 모니터링 스택 시작
```bash
# 전체 스택 시작 (이미 docker-compose.prod.yml에 포함됨)
docker-compose -f docker/docker-compose.prod.yml up -d

# 모니터링만 시작
docker-compose -f monitoring/docker-compose.monitoring.yml up -d
```

### 3. 접속 URL
- **Grafana**: https://monitoring.autocoin.com
- **Prometheus**: https://monitoring.autocoin.com/prometheus
- **AlertManager**: https://monitoring.autocoin.com/alertmanager
- **Elasticsearch**: http://localhost:9200 (내부 접근만)

## 📊 사전 구성된 대시보드

### Grafana 대시보드

1. **Application Overview**
   - API 요청 수, 응답 시간, 에러율
   - 사용자 인증 통계
   - 업비트 API 호출 현황

2. **System Metrics**
   - CPU, 메모리, 디스크 사용률
   - 네트워크 트래픽
   - 컨테이너 리소스 사용량

3. **Database Performance**
   - MySQL 쿼리 성능
   - 연결 풀 상태
   - 슬로우 쿼리 모니터링

4. **Business Metrics**
   - 신규 사용자 등록
   - 거래 활동
   - 뉴스 수집 통계

### Kibana 대시보드 (옵션)

1. **Error Analysis**
   - 에러 로그 분석
   - 스택 트레이스 검색
   - 에러 발생 패턴

2. **Security Monitoring**
   - 인증 실패 로그
   - 의심스러운 접근 패턴
   - API 남용 탐지

3. **Performance Analysis**
   - 느린 API 요청 분석
   - 데이터베이스 쿼리 성능
   - 메모리 사용 패턴

## 🚨 알림 설정

### 1. Prometheus 알림 규칙

**Critical 알림 (즉시 대응 필요)**
- API 서버 다운
- 데이터베이스 연결 실패
- 메모리 사용률 90% 이상
- 디스크 사용률 90% 이상

**Warning 알림 (모니터링 필요)**
- 높은 응답 시간 (> 1초)
- 높은 에러율 (> 5%)
- CPU 사용률 80% 이상
- 메모리 사용률 80% 이상

### 2. ElastAlert 규칙

**보안 관련**
- 연속된 로그인 실패
- SQL 인젝션 시도
- 비정상적인 API 호출 패턴

**비즈니스 관련**
- 업비트 API 에러 급증
- 뉴스 수집 실패
- 사용자 등록 급증/급감

### 3. Slack 알림 채널

- `#alerts` - 일반 알림
- `#critical-alerts` - 즉시 대응 필요한 알림
- `#performance-alerts` - 성능 관련 알림
- `#security-alerts` - 보안 관련 알림

## 🔧 설정 가이드

### Elasticsearch 최적화

```yaml
# elasticsearch/elasticsearch.yml
cluster.name: autocoin-cluster
network.host: 0.0.0.0
http.port: 9200
discovery.type: single-node

# 메모리 설정 (물리 메모리의 50% 권장)
ES_JAVA_OPTS: "-Xms2g -Xmx2g"

# 인덱스 설정
index.number_of_shards: 1
index.number_of_replicas: 0
indices.query.bool.max_clause_count: 10000
```

### Logstash 파이프라인

```ruby
# logstash/logstash.conf
input {
  # 애플리케이션 로그 파일
  file {
    path => "/var/log/autocoin/*.log"
    start_position => "beginning"
    codec => "json"
  }
  
  # Docker 컨테이너 로그
  beats {
    port => 5044
  }
}

filter {
  # Spring Boot 로그 파싱
  if [message] =~ /^\d{4}-\d{2}-\d{2}/ {
    grok {
      match => { "message" => "%{TIMESTAMP_ISO8601:timestamp} %{LOGLEVEL:level} %{DATA:thread} %{DATA:logger} - %{GREEDYDATA:log_message}" }
    }
    
    date {
      match => [ "timestamp", "yyyy-MM-dd HH:mm:ss.SSS" ]
    }
  }
  
  # 에러 로그 분류
  if [level] == "ERROR" {
    mutate {
      add_tag => [ "error" ]
    }
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "autocoin-logs-%{+YYYY.MM.dd}"
  }
  
  # 디버그용 stdout (개발환경에서만)
  # stdout { codec => rubydebug }
}
```

### Prometheus 설정

```yaml
# prometheus/prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "rules/*.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093

scrape_configs:
  # Spring Boot Actuator
  - job_name: 'autocoin-api'
    static_configs:
      - targets: ['autocoin-api:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 30s
    
  # MySQL Exporter
  - job_name: 'mysql'
    static_configs:
      - targets: ['mysql-exporter:9104']
      
  # Redis Exporter
  - job_name: 'redis'
    static_configs:
      - targets: ['redis-exporter:9121']
      
  # Node Exporter (시스템 메트릭)
  - job_name: 'node'
    static_configs:
      - targets: ['node-exporter:9100']
```

## 🔍 로그 분석 쿼리 예제

### Elasticsearch 쿼리

```json
// 최근 1시간 에러 로그
{
  "query": {
    "bool": {
      "must": [
        { "term": { "level": "ERROR" }},
        { "range": { "@timestamp": { "gte": "now-1h" }}}
      ]
    }
  }
}

// 특정 사용자의 API 호출 패턴
{
  "query": {
    "bool": {
      "must": [
        { "term": { "logger": "com.autocoin.api.controller" }},
        { "match": { "log_message": "user123" }}
      ]
    }
  }
}
```

### Prometheus 쿼리

```promql
# API 요청 QPS
rate(http_requests_total[5m])

# 평균 응답 시간
rate(http_request_duration_seconds_sum[5m]) / rate(http_request_duration_seconds_count[5m])

# 에러율
rate(http_requests_total{status=~"5.."}[5m]) / rate(http_requests_total[5m]) * 100

# 메모리 사용률
(node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes) / node_memory_MemTotal_bytes * 100
```

## 🛠️ 트러블슈팅

### 일반적인 문제

**1. Elasticsearch 시작 실패**
```bash
# 로그 확인
docker logs autocoin-elasticsearch-prod

# 권한 문제 해결
chown -R 1000:1000 /var/lib/elasticsearch

# 메모리 설정 확인
sysctl vm.max_map_count=262144
```

**2. Logstash 파싱 실패**
```bash
# 파이프라인 설정 확인
docker exec autocoin-logstash-prod /usr/share/logstash/bin/logstash --config.test_and_exit

# 로그 확인
docker logs autocoin-logstash-prod
```

**3. Grafana 대시보드 로딩 실패**
```bash
# 데이터 소스 연결 확인
curl http://prometheus:9090/api/v1/targets

# Grafana 로그 확인
docker logs autocoin-grafana-prod
```

### 성능 최적화

**Elasticsearch**
- 인덱스 템플릿 최적화
- 필드 매핑 개선
- 샤드 수 조정

**Logstash**
- 워커 프로세스 수 증가
- 배치 크기 조정
- 필터 최적화

**Prometheus**
- 스크랩 간격 조정
- 메트릭 선별 수집
- 저장 기간 설정

## 📈 용량 계획

### 로그 용량 추정

**일일 로그 추정**
- API 로그: ~100MB/일
- 애플리케이션 로그: ~50MB/일
- 시스템 로그: ~20MB/일
- **총합**: ~170MB/일

**월간 저장 용량**
- 로그 데이터: ~5GB/월
- 메트릭 데이터: ~2GB/월
- **총합**: ~7GB/월

### 권장 리소스

**최소 사양**
- Elasticsearch: 2GB RAM, 20GB SSD
- Logstash: 1GB RAM
- Prometheus: 1GB RAM, 10GB SSD
- Grafana: 512MB RAM

**권장 사양**
- Elasticsearch: 4GB RAM, 50GB SSD
- Logstash: 2GB RAM
- Prometheus: 2GB RAM, 20GB SSD
- Grafana: 1GB RAM

## 🔒 보안 고려사항

1. **네트워크 분리**: 모니터링 컴포넌트를 별도 네트워크에 격리
2. **인증 강화**: Grafana, Kibana에 LDAP 또는 OAuth 연동
3. **암호화**: Elasticsearch 클러스터 간 통신 암호화
4. **접근 제어**: IP 기반 접근 제한
5. **로그 마스킹**: 민감 정보 자동 마스킹

## 📚 추가 자료

- [Elasticsearch 공식 문서](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- [Logstash 설정 가이드](https://www.elastic.co/guide/en/logstash/current/configuration.html)
- [Prometheus 모니터링 가이드](https://prometheus.io/docs/)
- [Grafana 대시보드 구성](https://grafana.com/docs/grafana/latest/)

---

**💡 Tip**: 모니터링 시스템 자체도 모니터링하는 것을 잊지 마세요!
