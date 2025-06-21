# SSH 키 디버깅 및 해결 가이드

## 🔍 현재 문제 상황
- 로컬 SSH 연결: ✅ 성공 (`spring_key.pem`)
- GitHub Actions SSH: ❌ 실패 (동일한 키)

## 🎯 문제 원인 (추정)
1. GitHub Secrets의 키 형식 문제
2. 줄바꿈 문자 문제 (Windows vs Linux)
3. 키 끝부분 공백 또는 문자 누락

## 🛠️ 해결책 1: 키 재설정 (정확한 형식)

### GitHub Secrets EC2_SSH_KEY에 다음 내용을 정확히 복사:

```
-----BEGIN RSA PRIVATE KEY-----
MIIEoQIBAAKCAQEAtWG6hRmaTW5INwKyQPFmmcqHTJBdteMfc7Bxc5LQjEwL4iuT
HLEq9SxgoLZiqSFZzSDRXFY+0RcP3IUMu5idzAW1w2e4mETw2E3RNEw1MpfmvAbv
HOLO+NZmhHXiCFmIjpuSBNe9lDXRLs9T+VREi0jTPeFXGFNzBkQgXzhvswUBZaTo
qI2FqGe+1Y5Cu3A1ry/kkCfiEhz5D66nfBCHf7KRUc1Olli4AkflxTVV2XNrvTgJ
MKr/CQCjSqpEJA2zru3w6Gx7dwsDvPc8enn89ezHQex8oIWjVUVdleT80njbBqs9
r9CsR2+RChYKqBwhE2bpw2lDSg4zFG+Vss7WDwIDAQABAoIBAD8RkE2frWunIS0f
nuO+DLMFHS6eNDd+lf0gKRLKxgFCoA8wn6KmaA2ANMktIfk1Q4h0FNhKlOUXY8iF
6vuAAb5jbeRYOHOZ5TUROeE5bnX9I5nVv2cH5nfX9SWx57qHh1gu+AL/EUhrviTM
qqJTjtHvrKQjqqkmuyQ+pBL4CL7yNEkgqd47rnJVwtqZZf+ti3Z3YsXs7oLgc/cp
eNgGL1wZijI9O7ThnbjKex+uFsMp+xLft91G62v8Z3U1G3Eh1LDJesOxkeKoJwL2
N0QervTmhwYbCQCJT+OeMel4ZHpfgHO7pd7okcRSZb6+XOzAj2o+GNxrYbTccqCC
Z5QQquECgYEA6V415sH/HMXRpDZkvnkxJvlwR+P5tKifMtXxhaFvuN1xmFvAZmgb
OTvAR+nerG1TKmIjQ3GxyMtj7FZG0msGVPkBKW3nJTMwCvmVfBbokJKNmeIKCkus
gtno9kNjX745rbLbzVgssmM85h24P6kmEg0Kd7wNbQ/Hnt1qezMpq20CgYEAxvjd
HJrd5Mu2LMWUXhiYiNh/NHOQfgd/Z69GaVSM3M7R9HhTerzlncEXGRd+fhcNznHs
Ex1OwxUJd0MnoJDXfiLqRJEqMG0sa/0n2VjZ9doTaoZ2HbjN+l2iiAoWsGpYITQK
G239wCOCw+UV5jEuz5a5KIe862PFtV3sQMGbvesCfy9m84vr2+X8SJkSc2Q9Ob6w
7B2uMQqBNdUdn/GE2Syzzl8kQ/CJNP5rJKN23cQocLwrMv6AGZyvs5xN6hTTqHWa
HqJLsKfdveR8zeBmxtaW7ixTvbZZqLDrkmZzSbwo545gxkTvpCSYA8a6QrKrqRAB
lh68E/4TNnkOgJKF0fECgYA75EcySA+IwiT4/xQ06rRnZLuNf0e8F+eqvyCzreak
xICAVQPTyy5WuPuJ6v+BVtEqFjKYYlcF87wgsQ8pjs++wSXBM+z+HK6lZtNq4grh
rR3xD75YHfuqZCr7RgYkXi5e+DoenZInFslSzA82dhGAHP9pCsq8FBE8EaR+rYiC
twKBgQC0NKiYDlNxuhWWhmlDjL5e/Ay7v4Rc42LQm4sRAZKAUrvP56rBGWrBJV0n
C4tFqV6bb4q7muVp8S8Ov26fTcFpnVxfi56r2QmtJqE1+EOxKDq0xrT5zw0t4GJp
WNZ+VK2BJtjwKAEOo0vYiNIeAvNfn1QZSrXaXZrLQJiBx74z+Q==
-----END RSA PRIVATE KEY-----
```

⚠️ **중요**: 
- 첫 줄과 마지막 줄 포함
- 줄바꿈 정확히 유지
- 앞뒤 공백 없음

## 🛠️ 해결책 2: 다른 SSH Action 시도

만약 여전히 안 된다면, 다른 SSH Action을 사용해보겠습니다.
