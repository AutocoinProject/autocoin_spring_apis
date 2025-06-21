# GitHub Secrets 설정 가이드

## 🔑 필수 SSH Secrets

### EC2_HOST
```
54.175.66.222
```

### EC2_USER  
```
ec2-user
```

### EC2_SSH_KEY
```
-----BEGIN RSA PRIVATE KEY-----
MIIEowIBAAKCAQEAllaygU3hduAJtvL82l3TvGN59OTbbBkYbMOTtUXWkfpOLIGu
IHcnGN0UBKHwGzSWvBV3aYM5lc9vJS+o7DnWDFEu/T2ow19TRkfCvHsbHJ5yJiq7
OEVoYu/ZWhGqZkFsTEC5bEg+JYXK1UfI/4Sk84Ebv9XuSGFFCEtWFADrpZs/GDXo
9MH9GaFOt9fei/KGOSgzW2xovprnCSENC9+8r93eNiYLOli5O22X5a0IeHGMJFVX
MEgfhSPLTOPasltyzdTexjUZOtqQ6t/Ih4tveczglCsCU8qVFOdvY84BsHaCsA2A
fwlmg0L7ZxF6DRFaUM5xmb01JurLTXEKGX/wiQIDAQABAoIBAHMsuFSXdcO8n7Sp
M02X1D7fdu9G5VNLZorsypx1hI0RVhKCxwPYRPunY06pQWmWQGSXjYQoJFXBNUUn
3iIbrQCCsZ0RP9ltjj6pe2cqaPrj6E+VLRlPE0m4tqtqHUF5nZ5Nd8CdF/9nIJ2F
AVfAJx7mKnwtSP/qhRh1ntx0WBPmTzXhkBSV420pc8NUXsPNjOsH+nH/X1wBziRu
InjeNmZFYc4QK1lWF2yPbzyoaqFV5SROdZ2VKsAvkXU0dTJIfwramdDrs+A72Tl+
Rwd4KOFbBJYP6cNQ+koz/DfSnkv2FWq4n79u2gTkFvnF0tQBIupFFH94aW6HkzCm
eRBxoAECgYEAxcZ1XolqQQ36J4968UzUNx3cRBkMHi/RInrfduB9j/p3A7OfVzLg
JVl63KHTYJOsmjsALXsqb4nvt5Q8CHJFHcZ2QDJ43g6Pre6zs9+r8Y2Z2Mm0+1N6
MFACIevjNb1hkOxZZtSsFPn7WDf65Uz9a67SrkncjS5BeWKbFwtRaSECgYEAwpkh
l1v5oZkMTudiIyEydKvqAosDsLhjafgqtXXx14P8RKDyGNB6czyhf6ZHbRsZbfGN
z5TBS7D+eF4SJj53pqyFbZHpkMWVfBWrLj4mDq4D2LadL9tNodDlmKAkcWb8TM3p
PMkgp+k6cUR6iN76CSt81Gu2WhunhDXsFv9HkmkCgYEAlmGYzRFfoaenFn2a1e+9
TUt/OKhy9mEVfEpBsHpx/xBLfp0OA5fDp6KqMSk7OKwRChTixbJpSfZwN/6WxzVc
KI/HSnkWPoKD7ZUbPyJL85ff6180JgYLWsD7Xh+m3C3sCe4s7Gu5jRmnbCTkuYYZ
C9AakjvHX/JUBqBbnOIWPQECgYBTuNY+7I9F+zbUa7BJNOskTyJq3axEx8/ds6uM
TqYx9tL+I9CE5I0KLze8V6m7Q0F5j8dXW8Sd8BRLbQ2Z7p5D003EilZWtutNa3Uu
fAabojp9bnPKNyv7RojBVYTYVVqPILgMMuwd0bYXLUMax/QDJfaa3Yyiz+sOcrSU
YFhGoQKBgDlYOGquv8dXC88c/AbuDavo/FiKeonYT/QuXMiOqgg84mPjNjVw0l/Q
zNKGhs+no5wKVhAj54CbNtIaPNlaZM0KQ+su14lPWeWV2JDSQdRlHRq4x+gtLyoE
vb1vs3ppPrfzMN05SgYuYUSn1cgZYcorXrF7F722bplxL+EvCwXN
-----END RSA PRIVATE KEY-----
```

## ⚠️ 주의사항

1. **EC2_SSH_KEY에는 전체 키 내용을 복사하세요**
   - `-----BEGIN RSA PRIVATE KEY-----`부터
   - `-----END RSA PRIVATE KEY-----`까지 모두 포함

2. **줄바꿈과 공백 주의**
   - 키를 복사할 때 추가 공백이나 줄바꿈이 들어가지 않도록 주의

3. **키 페어 일치 확인**
   - EC2 인스턴스의 키 페어 이름: `spring_key`
   - 로컬 키 파일: `autocoin-new-key.pem`
   - 이 두 키가 일치하는지 확인 필요

## 🔍 문제 해결 단계

1. **GitHub Secrets 다시 설정**
2. **Debug SSH 워크플로우 실행**
3. **EC2 보안 그룹 확인** (포트 22 허용)
4. **키 페어 일치 여부 확인**

## 🚨 추가 확인사항

EC2 인스턴스에서 다른 키를 사용하고 있을 가능성이 있습니다.
AWS 콘솔에서 다시 한번 확인하거나, 새로운 키 페어를 생성할 수도 있습니다.
