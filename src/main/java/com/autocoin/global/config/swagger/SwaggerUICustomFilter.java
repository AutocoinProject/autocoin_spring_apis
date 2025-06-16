package com.autocoin.global.config.swagger;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Swagger UI HTML에 자동 인증 스크립트를 삽입하는 필터
 * local 프로필에서만 활성화됩니다.
 * 현재 응답 충돌 문제로 인해 임시 비활성화
 */
@Slf4j
// @Component  // 임시 비활성화
@Profile("local")
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class SwaggerUICustomFilter extends OncePerRequestFilter {

    private static final String SWAGGER_UI_HTML_PATH = "/swagger-ui/index.html";
    private static final Pattern SWAGGER_UI_PATH_PATTERN = Pattern.compile("^/swagger-ui/.*\\.html$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        
        // Swagger UI HTML 파일 요청인 경우에만 처리
        if (SWAGGER_UI_PATH_PATTERN.matcher(requestURI).matches()) {
            log.debug("Swagger UI HTML 요청 감지: {}", requestURI);
            
            ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
            
            // 원래 응답 생성
            filterChain.doFilter(request, responseWrapper);
            
            // Content-Type 확인
            String contentType = responseWrapper.getContentType();
            if (contentType != null && contentType.contains("text/html")) {
                // HTML 응답 가져오기
                byte[] originalContent = responseWrapper.getContentAsByteArray();
                String htmlContent = new String(originalContent, StandardCharsets.UTF_8);
                
                // 자동 인증 스크립트 삽입
                String scriptTag = "<script src=\"/swagger-ui/swagger-auto-auth.js\"></script>";
                String modifiedContent;
                
                // </body> 태그 앞에 스크립트 삽입
                if (htmlContent.contains("</body>")) {
                    modifiedContent = htmlContent.replace("</body>", scriptTag + "</body>");
                } 
                // 또는 </html> 태그 앞에 삽입
                else if (htmlContent.contains("</html>")) {
                    modifiedContent = htmlContent.replace("</html>", scriptTag + "</html>");
                }
                // 둘 다 없으면 끝에 추가
                else {
                    modifiedContent = htmlContent + scriptTag;
                }
                
                // 응답 헤더 설정
                response.setContentLength(modifiedContent.getBytes(StandardCharsets.UTF_8).length);
                
                // 수정된 응답 쓰기
                PrintWriter out = response.getWriter();
                out.write(modifiedContent);
                out.flush();
                
                log.debug("Swagger UI HTML에 자동 인증 스크립트 삽입 완료");
                return;
            }
            
            // HTML이 아닌 경우 원래 응답 반환
            responseWrapper.copyBodyToResponse();
            return;
        }
        
        // Swagger UI HTML이 아닌 요청은 그대로 처리
        filterChain.doFilter(request, response);
    }
}
