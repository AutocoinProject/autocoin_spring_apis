package com.autocoin.global.exception.core;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "Invalid Input Value"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "Method Not Allowed"),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "C003", "Entity Not Found"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C004", "Server Error"),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C005", "Invalid Type Value"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "C006", "Access is Denied"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "C007", "Invalid Request"),
    INVALID_CATEGORY(HttpStatus.BAD_REQUEST, "C008", "Invalid Category"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "C009", "Invalid Credentials"),
    
    // User
    EMAIL_DUPLICATION(HttpStatus.BAD_REQUEST, "U001", "Email is Duplicated"),
    EMAIL_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "U001", "Email Already Exists"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U002", "User Not Found"),
    EMAIL_NOT_FOUND(HttpStatus.NOT_FOUND, "U003", "Email Not Found: 해당 이메일로 등록된 계정을 찾을 수 없습니다"),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "U004", "Login Failed: Invalid Credentials"),
    
    // Post
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "Post Not Found"),
    NOT_POST_OWNER(HttpStatus.FORBIDDEN, "P002", "Not the Post Owner"),
    
    // File
    FILE_UPLOAD_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "F001", "File Upload Failed"),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "F002", "File Not Found"),
    FILE_DOWNLOAD_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "F003", "File Download Failed"),
    
    // News
    NEWS_NOT_FOUND(HttpStatus.NOT_FOUND, "N001", "News Not Found"),
    NEWS_COLLECTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "N002", "News Collection Failed"),
    
    // Upbit
    UPBIT_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "UB001", "Upbit Account Not Found"),
    UPBIT_CONNECTION_FAILED(HttpStatus.BAD_REQUEST, "UB002", "Upbit Connection Failed"),
    UPBIT_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "UB003", "Upbit API Error"),
    UPBIT_ACCOUNT_INACTIVE(HttpStatus.BAD_REQUEST, "UB004", "Upbit Account Inactive"),
    UPBIT_SYNC_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "UB005", "Upbit Sync Failed"),
    INVALID_UPBIT_API_KEYS(HttpStatus.BAD_REQUEST, "UB006", "Invalid Upbit API Keys");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
