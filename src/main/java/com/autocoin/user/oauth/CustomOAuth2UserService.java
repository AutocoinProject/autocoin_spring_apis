package com.autocoin.user.oauth;

import com.autocoin.user.domain.Role;
import com.autocoin.user.domain.User;
import com.autocoin.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "oauth2.enabled", havingValue = "true", matchIfMissing = false)
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);
        
        // OAuth2 서비스 ID (google, kakao, naver)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        
        // OAuth2 로그인 진행 시 키가 되는 필드 값 (PK)
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
        
        // OAuth2UserService를 통해 가져온 데이터를 담을 클래스
        OAuthAttributes attributes = OAuthAttributes.of(
                registrationId, userNameAttributeName, oAuth2User.getAttributes());
        
        User user = saveOrUpdate(attributes);
        
        return new CustomOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                attributes.getAttributes(),
                attributes.getNameAttributeKey(),
                user.getEmail(),
                user.getId(),
                user.getProvider());
    }

    private User saveOrUpdate(OAuthAttributes attributes) {
        String provider = attributes.getProvider();
        String providerId = attributes.getAttributes().get(attributes.getNameAttributeKey()).toString();
        String email = attributes.getEmail();
        
        System.out.println("OAuth 사용자 처리 - 이메일: " + email + ", 제공자: " + provider + ", 제공자ID: " + providerId);
        
        // 1. 본인의 주요 식별자로 제공자와 제공자ID 사용
        // OAuth 제공자와 고유 ID를 환경변수에 저장한 이메일이 아니라
        // 오통적으로 식별 가능한 식별자 조합을 사용하는 게 좋음
        User user = userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> {
                    // 2. 제공자 + 제공자ID로 찾을 수 없을 경우, 이메일로 검색
                    Optional<User> userOptional = userRepository.findByEmail(email);
                    
                    if (userOptional.isPresent()) {
                        User existingUser = userOptional.get();
                        System.out.println("이메일로 사용자 찾음 - ID: " + existingUser.getId() + ", 이메일: " + existingUser.getEmail());
                        return existingUser;
                    } else {
                        // 3. 신규 사용자 생성
                        User newUser = User.builder()
                                .email(email)
                                .username(attributes.getName())
                                .password("") // OAuth 사용자는 비밀번호가 없음
                                .role(Role.USER)
                                .provider(provider)
                                .providerId(providerId)
                                .build();
                        
                        User savedUser = userRepository.save(newUser);
                        System.out.println("새 사용자 저장 - ID: " + savedUser.getId() + ", 이메일: " + savedUser.getEmail());
                        return savedUser;
                    }
                });
                
        return user;
    }
}
