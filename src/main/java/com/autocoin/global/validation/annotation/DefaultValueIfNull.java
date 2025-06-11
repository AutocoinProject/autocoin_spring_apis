package com.autocoin.global.validation.annotation;

import com.autocoin.global.validation.validator.DefaultValueIfNullValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 값이 null이면 기본값을 사용하는 커스텀 어노테이션
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DefaultValueIfNullValidator.class)
public @interface DefaultValueIfNull {
    String message() default "필수 입력 항목입니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    String defaultValue() default "";
}