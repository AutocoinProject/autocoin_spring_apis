package com.autocoin.user.oauth;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
public class OAuthAttributes {
    private Map<String, Object> attributes;
    private String nameAttributeKey;
    private String name;
    private String email;
    private String provider;

    @Builder
    public OAuthAttributes(Map<String, Object> attributes, String nameAttributeKey, 
                          String name, String email, String provider) {
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        this.name = name;
        this.email = email;
        this.provider = provider;
    }

    public static OAuthAttributes of(String registrationId, String userNameAttributeName, 
                                    Map<String, Object> attributes) {
        if ("kakao".equals(registrationId)) {
            return ofKakao(userNameAttributeName, attributes);
        }
        return ofGoogle(userNameAttributeName, attributes);
    }

    private static OAuthAttributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .name((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .provider("google")
                .build();
    }

    private static OAuthAttributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {
        // Kakao는 kakao_account에 유저정보가 있다
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        // kakao_account안에 profile이라는 JSON객체가 있다
        Map<String, Object> kakaoProfile = (Map<String, Object>) kakaoAccount.get("profile");
        
        // 카카오에서는 이메일을 요청하지 않고 ID를 기반으로 사용자 식별
        String kakaoId = attributes.get(userNameAttributeName).toString();
        String nickname = (String) kakaoProfile.get("nickname");
        
        // providerId를 기반으로 고유한 이메일 생성 - 이메일은 식별자로만 사용
        String email = "kakao_" + kakaoId + "@autocoin.com";
        
        System.out.println("Kakao OAuth 로그인 정보 - ID: " + kakaoId + ", 닉네임: " + nickname);

        return OAuthAttributes.builder()
                .name(nickname)
                .email(email)
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .provider("kakao")
                .build();
    }
}
