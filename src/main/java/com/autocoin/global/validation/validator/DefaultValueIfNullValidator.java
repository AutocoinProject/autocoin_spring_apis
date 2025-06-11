package com.autocoin.global.validation.validator;

import com.autocoin.global.validation.annotation.DefaultValueIfNull;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;

/**
 * DefaultValueIfNull 어노테이션에 대한 Validator 구현
 */
public class DefaultValueIfNullValidator implements ConstraintValidator<DefaultValueIfNull, String> {
    
    private String defaultValue;

    @Override
    public void initialize(DefaultValueIfNull constraintAnnotation) {
        this.defaultValue = constraintAnnotation.defaultValue();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // 값이 null이면 기본값을 설정합니다.
        // 항상 true를 반환하여 유효성 검증은 통과하도록 합니다.
        // 실제 값 설정은 별도의 Aspect나 Interceptor에서 처리해야 합니다.
        return true;
    }
}