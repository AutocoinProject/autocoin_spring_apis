package com.autocoin.global.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.util.regex.Pattern;

/**
 * 필터 관련 유틸리티 메서드를 제공하는 클래스
 */
public class FilterUtils {

    private static final Pattern JSON_PATTERN = Pattern.compile("application/json.*");

    /**
     * 응답이 JSON 형식인지 확인합니다.
     * 
     * @param response HTTP 응답
     * @return JSON 형식이면 true, 아니면 false
     */
    public static boolean isJsonResponse(HttpServletResponse response) {
        String contentType = response.getContentType();
        return contentType != null && JSON_PATTERN.matcher(contentType).matches();
    }

    /**
     * ContentCachingResponseWrapper로 응답을 래핑합니다.
     * 이미 래핑된 경우 그대로 반환합니다.
     * 
     * @param response 원본 응답
     * @return 래핑된 응답
     */
    public static ContentCachingResponseWrapper wrapResponse(HttpServletResponse response) {
        if (response instanceof ContentCachingResponseWrapper) {
            return (ContentCachingResponseWrapper) response;
        }
        return new ContentCachingResponseWrapper(response);
    }

    /**
     * 요청 URI가 지정된 패턴과 일치하는지 확인합니다.
     * 
     * @param request HTTP 요청
     * @param pattern URI 패턴
     * @return 일치하면 true, 아니면 false
     */
    public static boolean matchesUriPattern(HttpServletRequest request, String pattern) {
        String requestUri = request.getRequestURI();
        return requestUri != null && requestUri.matches(pattern);
    }
}
