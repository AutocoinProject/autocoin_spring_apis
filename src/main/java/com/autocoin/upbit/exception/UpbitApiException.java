package com.autocoin.upbit.exception;

import com.autocoin.global.exception.core.CustomException;
import com.autocoin.global.exception.core.ErrorCode;

public class UpbitApiException extends CustomException {
    
    public UpbitApiException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    public UpbitApiException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    
    public UpbitApiException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}