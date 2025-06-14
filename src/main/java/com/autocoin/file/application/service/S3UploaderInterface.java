package com.autocoin.file.application.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * S3 파일 업로드 인터페이스
 */
public interface S3UploaderInterface {
    
    /**
     * S3에 파일 업로드
     * @param multipartFile 업로드할 파일
     * @param dirName S3 내 디렉토리명
     * @return 업로드된 파일의 S3 URL
     * @throws IOException 파일 업로드 실패 시 발생
     */
    String upload(MultipartFile multipartFile, String dirName) throws IOException;
    
    /**
     * S3에서 파일 삭제
     * @param fileKey 삭제할 파일의 S3 키
     */
    void delete(String fileKey);
    
    /**
     * S3 연결 상태 확인
     * @return S3 연결 가능 여부
     */
    default boolean checkS3Connection() {
        return true; // 기본값
    }
}
