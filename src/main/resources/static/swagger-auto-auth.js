// Swagger UI 자동 인증 스크립트 (Bearer 접두사 자동 추가 기능 포함)
window.addEventListener('load', function() {
    // Swagger UI가 완전히 로드될 때까지 대기
    setTimeout(function() {
        initSwaggerAuth();
    }, 1000);
    
    function initSwaggerAuth() {
        console.log("Swagger 자동 인증 스크립트가 초기화되었습니다. (Bearer 접두사 자동 추가 기능 포함)");
        
        // 인증 상태 표시 UI 추가
        const authInfo = document.createElement('div');
        authInfo.id = 'auth-status';
        authInfo.style.position = 'fixed';
        authInfo.style.top = '10px';
        authInfo.style.right = '10px';
        authInfo.style.padding = '8px 12px';
        authInfo.style.backgroundColor = '#f8f9fa';
        authInfo.style.border = '1px solid #ccc';
        authInfo.style.borderRadius = '4px';
        authInfo.style.fontSize = '13px';
        authInfo.style.zIndex = '9999';
        authInfo.style.boxShadow = '0 2px 5px rgba(0,0,0,0.1)';
        authInfo.style.fontFamily = '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif';
        document.body.appendChild(authInfo);
        
        // 토큰 입력 자동 처리기능 추가 (Bearer 접두사 자동 추가)
        addTokenInputHandler();
        
        // 초기 상태 업데이트
        updateAuthStatus();
        
        // 주기적으로 인증 상태 업데이트 (10초마다)
        setInterval(updateAuthStatus, 10000);
        
        // 자동 인증 적용 (최초 실행)
        setTimeout(applyTokenIfAvailable, 2000);
    }
    
    // 토큰 입력 필드에 자동 Bearer 처리기 추가
    function addTokenInputHandler() {
        // MutationObserver를 사용하여 동적으로 생성되는 토큰 입력 필드 감지
        const observer = new MutationObserver(function(mutations) {
            mutations.forEach(function(mutation) {
                if (mutation.type === 'childList') {
                    // 새로 추가된 노드 중에서 토큰 입력 필드 찾기
                    mutation.addedNodes.forEach(function(node) {
                        if (node.nodeType === Node.ELEMENT_NODE) {
                            // Swagger UI 인증 모달이 열렸는지 확인
                            const authContainer = node.querySelector ? node.querySelector('.auth-container') : null;
                            if (authContainer || node.classList.contains('auth-container')) {
                                setTimeout(function() {
                                    const tokenInput = document.querySelector('.swagger-ui .auth-container input[type="text"]');
                                    if (tokenInput && !tokenInput.hasAttribute('data-bearer-handler')) {
                                        addBearerHandler(tokenInput);
                                    }
                                }, 100);
                            }
                        }
                    });
                }
            });
        });
        
        // document.body를 관찰 대상으로 설정
        observer.observe(document.body, {
            childList: true,
            subtree: true
        });
    }
    
    // Bearer 접두사 자동 추가 핸들러
    function addBearerHandler(tokenInput) {
        tokenInput.setAttribute('data-bearer-handler', 'true');
        tokenInput.placeholder = 'JWT 토큰을 입력하세요 (Bearer 접두사 자동 추가)';
        
        // 입력 시 Bearer 접두사 자동 추가
        tokenInput.addEventListener('blur', function() {
            const value = tokenInput.value.trim();
            if (value && !value.startsWith('Bearer ')) {
                tokenInput.value = 'Bearer ' + value;
                // input 이벤트 발생시켜 Swagger UI가 변경을 인식하도록 함
                const event = new Event('input', { bubbles: true });
                tokenInput.dispatchEvent(event);
                console.log('Bearer 접두사가 자동으로 추가되었습니다.');
            }
        });
        
        // 포커스 시 안내 메시지 표시
        tokenInput.addEventListener('focus', function() {
            const infoDiv = document.getElementById('token-input-info');
            if (!infoDiv) {
                const info = document.createElement('div');
                info.id = 'token-input-info';
                info.style.position = 'absolute';
                info.style.backgroundColor = '#e7f3ff';
                info.style.border = '1px solid #b3d7ff';
                info.style.padding = '5px 8px';
                info.style.borderRadius = '3px';
                info.style.fontSize = '12px';
                info.style.color = '#004085';
                info.style.marginTop = '2px';
                info.style.zIndex = '10000';
                info.textContent = '토큰만 입력하세요. Bearer 접두사는 자동으로 추가됩니다.';
                
                tokenInput.parentNode.appendChild(info);
                
                // 3초 후 메시지 제거
                setTimeout(function() {
                    if (info.parentNode) {
                        info.parentNode.removeChild(info);
                    }
                }, 3000);
            }
        });
        
        console.log('토큰 입력 필드에 Bearer 자동 추가 핸들러가 설정되었습니다.');
    }
    
    // 토큰 상태 업데이트 함수
    function updateAuthStatus() {
        const token = localStorage.getItem('auth_token');
        const authInfo = document.getElementById('auth-status');
        
        if (!authInfo) return;
        
        if (token) {
            // 토큰 검증 API 호출
            fetch(`/swagger-dev/token/validate?token=${encodeURIComponent(token)}`)
                .then(response => response.json())
                .then(data => {
                    if (data.valid) {
                        // 유효한 토큰
                        const minutes = data.expiresInMinutes || 0;
                        const email = data.email || '사용자';
                        
                        // 만료 시간에 따른 상태 표시
                        if (minutes <= 0) {
                            // 만료된 토큰
                            authInfo.style.backgroundColor = '#fff3cd';
                            authInfo.innerHTML = '<span style="color:#856404">⚠ 토큰 만료됨</span> - <a href="#" id="refresh-login">다시 로그인</a>';
                        } else if (minutes < 5) {
                            // 곧 만료될 토큰
                            authInfo.style.backgroundColor = '#fff3cd';
                            authInfo.innerHTML = `<span style="color:#856404">⚠ 곧 만료됨</span> - ${email} <br>만료: ${minutes}분 후 <a href="#" id="apply-token">[적용]</a> <a href="#" id="refresh-login">[갱신]</a>`;
                        } else {
                            // 유효한 토큰
                            authInfo.style.backgroundColor = '#d4edda';
                            authInfo.innerHTML = `<span style="color:#155724">✓ 인증됨</span> - ${email} <br>만료: ${minutes}분 후 <a href="#" id="apply-token">[적용]</a> <a href="#" id="refresh-login">[갱신]</a>`;
                        }
                        
                        // 적용 버튼 이벤트 설정
                        const applyBtn = document.getElementById('apply-token');
                        if (applyBtn) {
                            applyBtn.addEventListener('click', function(e) {
                                e.preventDefault();
                                applyTokenToSwaggerUI(token);
                            });
                        }
                        
                        // 갱신 버튼 이벤트 설정
                        setupLoginHandler();
                    } else {
                        // 유효하지 않은 토큰
                        authInfo.style.backgroundColor = '#f8d7da';
                        authInfo.innerHTML = '<span style="color:#721c24">✗ 유효하지 않은 토큰</span> - <a href="#" id="refresh-login">다시 로그인</a>';
                        setupLoginHandler();
                    }
                })
                .catch(error => {
                    console.error("토큰 검증 오류:", error);
                    // API 오류 시 기본 검증 로직 사용
                    fallbackTokenValidation(token);
                });
        } else {
            // 토큰 없음
            authInfo.style.backgroundColor = '#f8d7da';
            authInfo.innerHTML = '<span style="color:#721c24">✗ 인증 없음</span> - <a href="#" id="refresh-login">테스트 계정 로그인</a>';
            setupLoginHandler();
        }
    }
    
    // API 오류 시 기본 검증 로직
    function fallbackTokenValidation(token) {
        const authInfo = document.getElementById('auth-status');
        if (!authInfo) return;
        
        try {
            // Bearer 접두사 제거 후 JWT 토큰 파싱
            const cleanToken = token.startsWith('Bearer ') ? token.substring(7) : token;
            
            // JWT 토큰 파싱
            const parts = cleanToken.split('.');
            if (parts.length !== 3) {
                throw new Error('Invalid token format');
            }
            
            const base64Url = parts[1];
            const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
            const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
                return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
            }).join(''));
            
            const payload = JSON.parse(jsonPayload);
            const exp = payload.exp * 1000; // 밀리초 단위로 변환
            const now = new Date().getTime();
            const remaining = Math.floor((exp - now) / 1000 / 60); // 분 단위 남은 시간
            
            // 만료 시간에 따른 상태 표시
            if (remaining <= 0) {
                // 만료된 토큰
                authInfo.style.backgroundColor = '#fff3cd';
                authInfo.innerHTML = '<span style="color:#856404">⚠ 토큰 만료됨</span> - <a href="#" id="refresh-login">다시 로그인</a>';
            } else if (remaining < 5) {
                // 곧 만료될 토큰
                authInfo.style.backgroundColor = '#fff3cd';
                authInfo.innerHTML = `<span style="color:#856404">⚠ 곧 만료됨</span> - ${payload.email || '사용자'} <br>만료: ${remaining}분 후 <a href="#" id="apply-token">[적용]</a> <a href="#" id="refresh-login">[갱신]</a>`;
            } else {
                // 유효한 토큰
                authInfo.style.backgroundColor = '#d4edda';
                authInfo.innerHTML = `<span style="color:#155724">✓ 인증됨</span> - ${payload.email || '사용자'} <br>만료: ${remaining}분 후 <a href="#" id="apply-token">[적용]</a> <a href="#" id="refresh-login">[갱신]</a>`;
            }
            
            // 적용 버튼 이벤트 설정
            const applyBtn = document.getElementById('apply-token');
            if (applyBtn) {
                applyBtn.addEventListener('click', function(e) {
                    e.preventDefault();
                    applyTokenToSwaggerUI(token);
                });
            }
            
            // 갱신 버튼 이벤트 설정
            setupLoginHandler();
        } catch (e) {
            console.error("토큰 파싱 오류:", e);
            
            // 유효하지 않은 토큰
            authInfo.style.backgroundColor = '#f8d7da';
            authInfo.innerHTML = '<span style="color:#721c24">✗ 유효하지 않은 토큰</span> - <a href="#" id="refresh-login">다시 로그인</a>';
            setupLoginHandler();
        }
    }
    
    // 로그인 핸들러 설정
    function setupLoginHandler() {
        const loginBtn = document.getElementById('refresh-login');
        if (loginBtn) {
            loginBtn.addEventListener('click', function(e) {
                e.preventDefault();
                
                // 로그인 중 표시
                const authInfo = document.getElementById('auth-status');
                if (authInfo) {
                    authInfo.innerHTML = '<span style="color:#0c5460">⏱ 로그인 중...</span>';
                    authInfo.style.backgroundColor = '#d1ecf1';
                }
                
                // 테스트 계정으로 로그인 시도 (간편 API 사용)
                fetch('/swagger-dev/test-login', {
                    method: 'POST'
                })
                .then(response => {
                    if (!response.ok) {
                        return response.json().then(data => {
                            throw new Error(data.error || '로그인 실패');
                        });
                    }
                    return response.json();
                })
                .then(data => {
                    if (data.token) {
                        localStorage.setItem('auth_token', data.token);
                        
                        // 로그인 성공 표시
                        const authInfo = document.getElementById('auth-status');
                        if (authInfo) {
                            authInfo.innerHTML = `<span style="color:#155724">✓ 로그인 성공</span> - ${data.email || '사용자'}`;
                            authInfo.style.backgroundColor = '#d4edda';
                        }
                        
                        // 토큰 적용
                        setTimeout(() => {
                            applyTokenToSwaggerUI(data.token);
                            // 상태 업데이트
                            setTimeout(updateAuthStatus, 1000);
                        }, 1000);
                    } else {
                        throw new Error('토큰이 없습니다');
                    }
                })
                .catch(error => {
                    console.error('로그인 오류:', error);
                    
                    const authInfo = document.getElementById('auth-status');
                    if (authInfo) {
                        authInfo.innerHTML = `<span style="color:#721c24">✗ 로그인 실패</span> - ${error.message || '알 수 없는 오류'}`;
                        authInfo.style.backgroundColor = '#f8d7da';
                    }
                    
                    // 수동 로그인 대체 방법 제공
                    setTimeout(() => {
                        // 수동 로그인 안내
                        console.log('간편 로그인 API 실패! 기존 로그인 방식으로 시도합니다...');
                        tryLegacyLogin();
                    }, 2000);
                });
            });
        }
    }
    
    // 기존 로그인 방식 시도
    function tryLegacyLogin() {
        const authInfo = document.getElementById('auth-status');
        if (authInfo) {
            authInfo.innerHTML = '<span style="color:#0c5460">⏱ 기본 로그인 시도 중...</span>';
            authInfo.style.backgroundColor = '#d1ecf1';
        }
        
        fetch('/api/v1/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email: 'test@autocoin.com', password: 'Test1234!' })
        })
        .then(response => response.json())
        .then(data => {
            if (data.token) {
                localStorage.setItem('auth_token', data.token);
                
                // 토큰 적용
                applyTokenToSwaggerUI(data.token);
                
                // 상태 업데이트
                updateAuthStatus();
            } else {
                throw new Error('토큰이 없습니다');
            }
        })
        .catch(error => {
            console.error('기존 로그인 오류:', error);
            
            const authInfo = document.getElementById('auth-status');
            if (authInfo) {
                authInfo.innerHTML = `<span style="color:#721c24">✗ 모든 로그인 방식 실패</span>`;
                authInfo.style.backgroundColor = '#f8d7da';
            }
        });
    }
    
    // 토큰이 있으면 Swagger UI에 자동 적용
    function applyTokenIfAvailable() {
        const token = localStorage.getItem('auth_token');
        if (token) {
            applyTokenToSwaggerUI(token);
        }
    }
    
    // Swagger UI에 토큰 적용 (Bearer 접두사 자동 추가)
    function applyTokenToSwaggerUI(token) {
        // 토큰에 Bearer 접두사가 없으면 자동으로 추가
        const finalToken = token.startsWith('Bearer ') ? token : `Bearer ${token}`;
        
        // Swagger UI 인증 버튼 찾기
        const authorizeBtn = document.querySelector('.swagger-ui .auth-wrapper .authorize');
        
        if (!authorizeBtn) {
            console.warn('Swagger UI 인증 버튼을 찾을 수 없습니다. Swagger UI가 로드되지 않았을 수 있습니다.');
            return;
        }
        
        // 인증 버튼 클릭
        authorizeBtn.click();
        
        // 모달이 열리면 토큰 입력
        setTimeout(function() {
            const tokenInput = document.querySelector('.swagger-ui .auth-container input[type="text"]');
            if (tokenInput) {
                // 현재 입력된 값 지우기
                tokenInput.value = '';
                
                // Bearer 접두사가 포함된 토큰 입력
                tokenInput.value = finalToken;
                
                // input 이벤트 발생시켜 Swagger UI가 변경을 인식하도록 함
                const event = new Event('input', { bubbles: true });
                tokenInput.dispatchEvent(event);
                
                // Bearer 자동 추가 핸들러 설정
                if (!tokenInput.hasAttribute('data-bearer-handler')) {
                    addBearerHandler(tokenInput);
                }
                
                // 인증 버튼 클릭
                const authBtn = document.querySelector('.swagger-ui .auth-btn-wrapper .authorize');
                if (authBtn) {
                    authBtn.click();
                    console.log('Swagger UI에 토큰이 적용되었습니다 (Bearer 접두사 자동 추가).');
                    
                    // 닫기 버튼 클릭
                    setTimeout(function() {
                        const closeBtn = document.querySelector('.swagger-ui .btn-done');
                        if (closeBtn) {
                            closeBtn.click();
                        }
                        
                        // 토큰 적용 완료 알림
                        const authInfo = document.getElementById('auth-status');
                        if (authInfo) {
                            authInfo.style.backgroundColor = '#c3e6cb';
                            authInfo.innerHTML = '<span style="color:#155724">✓ 토큰 적용 완료 (Bearer 접두사 자동 추가)</span>';
                            
                            // 2초 후 원래 상태로 복원
                            setTimeout(function() {
                                updateAuthStatus();
                            }, 2000);
                        }
                    }, 500);
                } else {
                    console.warn('Swagger UI 인증 확인 버튼을 찾을 수 없습니다.');
                }
            } else {
                console.warn('Swagger UI 토큰 입력란을 찾을 수 없습니다.');
            }
        }, 300);
    }
});
