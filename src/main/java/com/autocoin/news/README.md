# 암호화폐 뉴스 API 명세서

## 개요
이 API는 SerpAPI를 통해 암호화폐 뉴스를 정기적으로 수집하고 제공하는 기능을 제공합니다. 뉴스 데이터는 자동으로 데이터베이스에 저장되며, 중복 데이터는 저장되지 않습니다.

## API 엔드포인트

### 1. 암호화폐 뉴스 조회

#### 기본 정보
- **URL**: `/api/news`
- **Method**: GET
- **Description**: SerpAPI를 통해 최신 암호화폐 뉴스를 가져옵니다.

#### 응답 데이터 형식

| 필드명    | 타입     | 설명                           |
|----------|----------|-------------------------------|
| id       | Number   | 뉴스 ID (API 호출시에는 null)    |
| title    | String   | 뉴스 제목                      |
| link     | String   | 뉴스 원문 URL                  |
| source   | String   | 언론사 이름                    |
| date     | String   | 업로드 시간                    |
| thumbnail| String   | 썸네일 이미지 URL              |

#### 응답 예시 (성공 - 200 OK)
```json
[
  {
    "id": null,
    "title": "Bitcoin Surges Past $60,000",
    "link": "https://example.com/news/bitcoin-60k",
    "source": "Crypto News Today",
    "date": "2 hours ago",
    "thumbnail": "https://example.com/images/bitcoin.jpg"
  },
  {
    "id": null,
    "title": "Ethereum 2.0 Launch Date Announced",
    "link": "https://example.com/news/eth2-launch",
    "source": "CoinDesk",
    "date": "5 hours ago",
    "thumbnail": "https://example.com/images/ethereum.jpg"
  }
]
```

#### 오류 응답 (500 Internal Server Error)
```json
{
  "status": 500,
  "code": "NEWS001",
  "message": "뉴스 데이터를 가져오는데 실패했습니다: API rate limit exceeded",
  "timestamp": "2023-05-15T10:05:00"
}
```

### 2. 저장된 암호화폐 뉴스 조회

#### 기본 정보
- **URL**: `/api/news/saved`
- **Method**: GET
- **Description**: 데이터베이스에 저장된 암호화폐 뉴스를 조회합니다.

#### 응답 데이터 형식

| 필드명    | 타입     | 설명                           |
|----------|----------|-------------------------------|
| id       | Number   | 뉴스 ID                       |
| title    | String   | 뉴스 제목                      |
| link     | String   | 뉴스 원문 URL                  |
| source   | String   | 언론사 이름                    |
| date     | String   | 업로드 시간                    |
| thumbnail| String   | 썸네일 이미지 URL              |

#### 응답 예시 (성공 - 200 OK)
```json
[
  {
    "id": 1,
    "title": "Bitcoin Surges Past $60,000",
    "link": "https://example.com/news/bitcoin-60k",
    "source": "Crypto News Today",
    "date": "2 hours ago",
    "thumbnail": "https://example.com/images/bitcoin.jpg"
  },
  {
    "id": 2,
    "title": "Ethereum 2.0 Launch Date Announced",
    "link": "https://example.com/news/eth2-launch",
    "source": "CoinDesk",
    "date": "5 hours ago",
    "thumbnail": "https://example.com/images/ethereum.jpg"
  }
]
```

## 자동 뉴스 수집 기능

서버에는 주기적으로 암호화폐 뉴스를 자동 수집하는 스케줄러가 내장되어 있습니다.

### 1. 뉴스 수집 스케줄러

- **실행 주기**: 매 시간 정각 (기본값)
- **작동 방식**: SerpAPI에서 최신 암호화폐 뉴스 1개를 가져와 데이터베이스에 저장합니다.
- **중복 방지**: 이미 동일한 링크로 저장된 뉴스는 중복 저장되지 않습니다.

### 2. 데이터베이스 정리 스케줄러

- **실행 주기**: 매일 오후 3시 (기본값)
- **작동 방식**: 데이터베이스에 저장된 뉴스 중 오래된 뉴스를 삭제하여 데이터베이스 크기를 관리합니다.
- **최대 뉴스 수**: 기본적으로 최대 50개의 뉴스만 보관합니다 (설정 가능).

### 3. 스케줄러 설정

스케줄러 동작은 `application.yml` 파일에서 설정할 수 있습니다:

```yaml
news:
  scheduler:
    collect: "0 0 * * * *"  # 매 시간 정각 (cron 표현식: 초 분 시 일 월 요일)
    cleanup: "0 0 15 * * *"  # 매일 오후 3시
    max-news-count: 50    # 유지할 최대 뉴스 개수
```

## 기술 상세

### 데이터 소스
- SerpAPI를 사용하여 'crypto news' 키워드로 뉴스 검색
- API URL: `https://serpapi.com/search.json?q=crypto+news&tbm=nws&api_key=[YOUR_API_KEY]`

### 데이터베이스 테이블 구조

#### crypto_news 테이블
| 컬럼명         | 데이터 타입       | 설명                   | 제약 조건           |
|---------------|------------------|------------------------|-------------------|
| id            | BIGINT           | 고유 식별자             | PRIMARY KEY, AUTO_INCREMENT |
| title         | VARCHAR(255)     | 뉴스 제목              | NOT NULL          |
| link          | VARCHAR(255)     | 뉴스 원문 URL           | NOT NULL, UNIQUE  |
| source        | VARCHAR(255)     | 언론사 이름             | NOT NULL          |
| published_date| VARCHAR(255)     | 뉴스 발행 시간          |                   |
| thumbnail     | TEXT             | 썸네일 이미지 URL       |                   |
| created_at    | TIMESTAMP        | 데이터 생성 시간        |                   |

### 예외 처리
- SerpAPI 호출 실패 시 `NEWS001` 오류 코드와 함께 500 오류 반환
- 뉴스 결과가 없는 경우 빈 배열 반환

### 중복 방지 로직
- 뉴스 URL(`link` 필드)을 기준으로 중복 검사
- 이미 데이터베이스에 존재하는 링크는 저장하지 않음

## 사용 예시

### cURL로 API 호출
```bash
# 암호화폐 뉴스 조회
curl -X GET http://localhost:8080/api/news

# 저장된 암호화폐 뉴스 조회
curl -X GET http://localhost:8080/api/news/saved
```

### JavaScript로 API 호출
```javascript
// 암호화폐 뉴스 조회
fetch('http://localhost:8080/api/news')
  .then(response => response.json())
  .then(data => console.log(data))
  .catch(error => console.error('Error:', error));

// 저장된 암호화폐 뉴스 조회
fetch('http://localhost:8080/api/news/saved')
  .then(response => response.json())
  .then(data => console.log(data))
  .catch(error => console.error('Error:', error));
```

## 설정

### SerpAPI 키 설정
- `application.yml` 파일에 키 설정:
  ```yaml
  serp:
    api:
      key: YOUR_API_KEY
  ```
- 환경 변수로 설정:
  ```
  SERP_API_KEY=YOUR_API_KEY
  ```

## 자동화 및 스케줄링 로직

### 뉴스 수집 로직
1. 매 시간 정각에 스케줄러 실행
2. SerpAPI 호출하여 최신 암호화폐 뉴스 데이터 가져오기
3. 첫 번째 뉴스 항목(가장 최신 뉴스)만 추출
4. 데이터베이스에 동일한 URL의 뉴스가 없는지 확인
5. 중복이 아닌 경우 새 뉴스 항목 저장

### 데이터베이스 정리 로직
1. 매일 오후 3시에 스케줄러 실행
2. 현재 저장된 총 뉴스 수 확인
3. 최대 보관 수(기본값 50개)를 초과하는지 확인
4. 초과하는 경우, 가장 오래된 뉴스부터 삭제
5. 한 번에 최대 100개까지만 삭제 (안전 장치)

## 보안 고려사항
- API 키는 환경 변수를 통해 관리하는 것이 권장됨
- 민감한 API 키는 소스 코드에 직접 포함시키지 말 것
- 프로덕션 환경에서는 HTTPS 사용 권장

## 제한 사항
- SerpAPI는 계정에 따라 요청 제한이 있음
- 기본 뉴스 조회 API는 최대 5개의 뉴스만 반환
- 자동 수집은 1시간마다 1개의 뉴스만 저장
- 무료 계정 사용 시 SerpAPI 기능 제한 가능성 있음
