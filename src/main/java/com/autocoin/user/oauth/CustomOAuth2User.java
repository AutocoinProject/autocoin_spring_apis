package com.autocoin.user.oauth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.Collection;
import java.util.Map;

public class CustomOAuth2User extends DefaultOAuth2User {
    private final String email;
    private final Long id;
    private final String provider;

    public CustomOAuth2User(Collection<? extends GrantedAuthority> authorities,
                           Map<String, Object> attributes,
                           String nameAttributeKey,
                           String email,
                           Long id,
                           String provider) {
        super(authorities, attributes, nameAttributeKey);
        this.email = email;
        this.id = id;
        this.provider = provider;
    }

    public String getEmail() {
        return email;
    }
    
    public Long getId() {
        return id;
    }
    
    public String getProvider() {
        return provider;
    }
}
