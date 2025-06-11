package com.autocoin.global.filter;

import com.autocoin.global.util.FilterUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 개발 환경에서 로그인 API 응답의 토큰을 자동으로 관리하기 위한 필터
 * 임시 비활성화 - Swagger 접근 문제 해결
 */
@Slf4j
@Component
@Profile("disabled") // 임시 비활성화
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class DevAuthTokenFilter extends OncePerRequestFilter {

    private static final String LOGIN_API_PATH = "/api/v1/auth/login";
    private static final String SWAGGER_UI_PATH = "/swagger-ui/";
    private static final Pattern JSON_PATTERN = Pattern.compile("application/json.*");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        
        // Swagger UI 요청인 경우 자동 인증 스크립트 추가
        if (requestURI.startsWith(SWAGGER_UI_PATH) && requestURI.endsWith(".html")) {
            log.debug("Swagger UI 요청 감지: {}", requestURI);
            ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
            filterChain.doFilter(request, responseWrapper);
            injectDevTokenHelperScript(responseWrapper);
            return;
        }
        
        // 로그인 API 호출인 경우
        if (LOGIN_API_PATH.equals(requestURI) && "POST".equals(request.getMethod())) {
            log.debug("로그인 API 호출 감지: {}", requestURI);
            ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
            
            // 필터 체인 실행
            filterChain.doFilter(request, responseWrapper);
            
            // 응답 상태 코드 확인 (성공인 경우만 처리)
            if (responseWrapper.getStatus() >= 200 && responseWrapper.getStatus() < 300) {
                // Content-Type 확인
                String contentType = responseWrapper.getContentType();
                if (contentType != null && JSON_PATTERN.matcher(contentType).matches()) {
                    // 응답 바디 가져오기
                    byte[] responseBody = responseWrapper.getContentAsByteArray();
                    if (responseBody.length > 0) {
                        String responseContent = new String(responseBody, responseWrapper.getCharacterEncoding());
                        
                        // 토큰 자동 저장 스크립트 추가
                        addTokenSavingScript(responseWrapper, responseContent);
                        return;
                    }
                }
            }
            
            // 변경이 없는 경우 원본 응답 반환
            responseWrapper.copyBodyToResponse();
            return;
        }
        
        // 그 외 요청은 변경 없이 처리
        filterChain.doFilter(request, response);
    }
    
    /**
     * 로그인 응답에 토큰 자동 저장 스크립트를 추가합니다.
     */
    private void addTokenSavingScript(ContentCachingResponseWrapper responseWrapper, String responseContent) throws IOException {
        String contentType = responseWrapper.getContentType();
        
        if (contentType != null && JSON_PATTERN.matcher(contentType).matches()) {
            // 원본 응답에 토큰 자동 저장 스크립트를 추가한 새 응답 생성
            String modifiedResponse = responseContent + "\n" +
                    "<script type=\"text/javascript\">\n" +
                    "try {\n" +
                    "  const responseData = " + responseContent + ";\n" +
                    "  if (responseData && responseData.token) {\n" +
                    "    localStorage.setItem('auth_token', responseData.token);\n" +
                    "    console.log('로그인 성공: 토큰이 자동으로 저장되었습니다.');\n" +
                    "    console.log('이제 Swagger UI에서 인증이 자동으로 적용됩니다.');\n" +
                    "  }\n" +
                    "} catch(e) { console.error('토큰 저장 중 오류:', e); }\n" +
                    "</script>";
            
            // 응답 헤더 설정
            responseWrapper.setContentLength(modifiedResponse.getBytes().length);
            responseWrapper.getResponse().setContentLength(modifiedResponse.getBytes().length);
            
            // 새 응답 쓰기
            PrintWriter out = responseWrapper.getResponse().getWriter();
            out.write(modifiedResponse);
            out.flush();
        } else {
            // 변경이 없는 경우 원본 응답 반환
            responseWrapper.copyBodyToResponse();
        }
    }
    
    /**
     * Swagger UI HTML에 개발 도우미 스크립트를 추가합니다.
     */
    private void injectDevTokenHelperScript(ContentCachingResponseWrapper responseWrapper) throws IOException {
        String contentType = responseWrapper.getContentType();
        
        if (contentType != null && contentType.contains("text/html")) {
            String originalContent = new String(responseWrapper.getContentAsByteArray(), responseWrapper.getCharacterEncoding());
            
            // </body> 태그 앞에 스크립트 추가
            String injectedScript = 
                    "<script type=\"text/javascript\">\n" +
                    "// 개발 환경 토큰 관리 도우미\n" +
                    "function setupDevAuthHelpers() {\n" +
                    "  // 현재 토큰 정보 표시\n" +
                    "  const token = localStorage.getItem('auth_token');\n" +
                    "  const tokenInfo = document.createElement('div');\n" +
                    "  tokenInfo.style.position = 'fixed';\n" +
                    "  tokenInfo.style.top = '10px';\n" +
                    "  tokenInfo.style.right = '10px';\n" +
                    "  tokenInfo.style.padding = '5px 10px';\n" +
                    "  tokenInfo.style.background = token ? '#e6ffe6' : '#ffe6e6';\n" +
                    "  tokenInfo.style.border = '1px solid #ccc';\n" +
                    "  tokenInfo.style.borderRadius = '4px';\n" +
                    "  tokenInfo.style.fontSize = '12px';\n" +
                    "  tokenInfo.style.zIndex = '9999';\n" +
                    "  tokenInfo.innerHTML = token \n" +
                    "    ? '<span style=\"color:green\">✓ 인증 적용됨</span> - <a href=\"#\" id=\"clear-token\">초기화</a>' \n" +
                    "    : '<span style=\"color:red\">✗ 인증 없음</span> - <a href=\"#\" id=\"test-login\">테스트 계정 로그인</a>';\n" +
                    "  document.body.appendChild(tokenInfo);\n" +
                    "  \n" +
                    "  // 토큰 초기화 기능\n" +
                    "  if (token) {\n" +
                    "    document.getElementById('clear-token').addEventListener('click', function(e) {\n" +
                    "      e.preventDefault();\n" +
                    "      localStorage.removeItem('auth_token');\n" +
                    "      alert('토큰이 초기화되었습니다. 페이지를 새로고침하세요.');\n" +
                    "      location.reload();\n" +
                    "    });\n" +
                    "  } else {\n" +
                    "    // 테스트 계정 로그인 기능\n" +
                    "    document.getElementById('test-login').addEventListener('click', function(e) {\n" +
                    "      e.preventDefault();\n" +
                    "      fetch('/api/v1/auth/login', {\n" +
                    "        method: 'POST',\n" +
                    "        headers: { 'Content-Type': 'application/json' },\n" +
                    "        body: JSON.stringify({ email: 'test@autocoin.com', password: 'Test1234!' })\n" +
                    "      })\n" +
                    "      .then(response => response.json())\n" +
                    "      .then(data => {\n" +
                    "        if (data.token) {\n" +
                    "          localStorage.setItem('auth_token', data.token);\n" +
                    "          alert('테스트 계정으로 로그인 성공. 페이지를 새로고침하세요.');\n" +
                    "          location.reload();\n" +
                    "        } else {\n" +
                    "          alert('로그인 실패. 관리자에게 문의하세요.');\n" +
                    "        }\n" +
                    "      })\n" +
                    "      .catch(error => {\n" +
                    "        console.error('로그인 오류:', error);\n" +
                    "        alert('로그인 중 오류가 발생했습니다. 개발자 도구를 확인하세요.');\n" +
                    "      });\n" +
                    "    });\n" +
                    "  }\n" +
                    "}\n" +
                    "\n" +
                    "// 페이지 로드 후 실행\n" +
                    "window.addEventListener('load', setupDevAuthHelpers);\n" +
                    "</script>\n";
            
            String modifiedContent = originalContent.replace("</body>", injectedScript + "</body>");
            
            // 응답 헤더 설정
            responseWrapper.setContentLength(modifiedContent.getBytes().length);
            
            // 새 응답 쓰기
            PrintWriter out = responseWrapper.getResponse().getWriter();
            out.write(modifiedContent);
            out.flush();
        } else {
            // 변경이 없는 경우 원본 응답 반환
            responseWrapper.copyBodyToResponse();
        }
    }
}
