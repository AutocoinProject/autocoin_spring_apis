package com.autocoin.file.application.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Uploader implements S3UploaderInterface {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    /**
     * S3에 파일 업로드
     * @param multipartFile 업로드할 파일
     * @param dirName S3 내 디렉토리명
     * @return 업로드된 파일의 S3 URL
     * @throws IOException 파일 업로드 실패 시 발생
     */
    public String upload(MultipartFile multipartFile, String dirName) throws IOException {
        log.info("=== S3Uploader.upload 시작 ===");
        
        if (multipartFile == null) {
            log.warn("업로드할 파일이 null입니다");
            return null;
        }
        
        if (multipartFile.isEmpty()) {
            log.warn("업로드할 파일이 비어 있습니다");
            return null;
        }
        
        log.info("파일 업로드 시도: 파일명={}, 크기={}, 디렉토리={}, 버킷={}", 
                multipartFile.getOriginalFilename(), 
                multipartFile.getSize(), 
                dirName, 
                bucket);

        try {
            // S3 연결 상태 확인
            if (!isS3Available()) {
                log.warn("S3 서비스에 연결할 수 없습니다. 파일 업로드를 건너뛁니다.");
                return null;
            }

            String originalFileName = multipartFile.getOriginalFilename();
            String fileName = createFileName(originalFileName, dirName);
            String fileKey = dirName + "/" + fileName;
            
            log.info("생성된 파일 Key: {}", fileKey);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(multipartFile.getSize());
            metadata.setContentType(multipartFile.getContentType());
            
            log.info("S3 putObject 시작");
            amazonS3.putObject(new PutObjectRequest(bucket, fileKey, multipartFile.getInputStream(), metadata));
            log.info("S3 putObject 완료");
                    
            String s3Url = amazonS3.getUrl(bucket, fileKey).toString();
            log.info("S3 업로드 성공: URL={}", s3Url);
            return s3Url;
            
        } catch (Exception e) {
            log.error("S3 파일 업로드 실패: {}", e.getMessage());
            log.debug("S3 업로드 상세 오류", e);
            return null; // 예외 대신 null 반환
        }
    }

    /**
     * S3에서 파일 삭제 (실패해도 예외 발생 안함)
     * @param fileKey 삭제할 파일의 S3 키
     */
    public void delete(String fileKey) {
        if (fileKey == null || fileKey.isEmpty()) {
            log.warn("삭제할 파일 키가 없습니다");
            return;
        }
        
        try {
            if (!isS3Available()) {
                log.warn("S3 서비스에 연결할 수 없습니다. 파일 삭제를 건너뛁니다.");
                return;
            }
            
            amazonS3.deleteObject(new DeleteObjectRequest(bucket, fileKey));
            log.info("S3 파일 삭제 성공: {}", fileKey);
        } catch (Exception e) {
            log.error("S3 파일 삭제 실패: fileKey={}, error={}", fileKey, e.getMessage());
        }
    }

    /**
     * S3 서비스 연결 상태 확인
     * @return S3 연결 가능 여부
     */
    private boolean isS3Available() {
        try {
            log.info("S3 연결 테스트 시작 - 버킷: {}, 리전: 미설정", bucket);
            
            // 버킷 존재 여부 확인으로 연결 테스트
            boolean bucketExists = amazonS3.doesBucketExistV2(bucket);
            
            if (bucketExists) {
                log.info("S3 버킷 '{}' 연결 성공 - 버킷 존재 확인", bucket);
                
                // 버킷 위치 확인
                String bucketLocation = amazonS3.getBucketLocation(bucket);
                log.info("S3 버킷 위치: {}", bucketLocation);
                
                return true;
            } else {
                log.warn("S3 버킷 '{}'이 존재하지 않습니다. 버킷을 먼저 생성해주세요.", bucket);
                return false;
            }
        } catch (Exception e) {
            log.error("S3 연결 실패 - 버킷: {}, 오류: {}", bucket, e.getMessage());
            log.debug("S3 연결 실패 상세 오류", e);
            return false;
        }
    }

    /**
     * 업로드할 파일명 생성
     * @param originalFileName 원본 파일명
     * @param dirName 디렉토리명
     * @return 생성된 파일명 (UUID 포함)
     */
    private String createFileName(String originalFileName, String dirName) {
        if (originalFileName == null) {
            return UUID.randomUUID().toString();
        }
        return UUID.randomUUID().toString() + "_" + originalFileName;
    }

    /**
     * S3 연결 상태 체크 (공개 메서드)
     * @return S3 연결 가능 여부
     */
    @Override
    public boolean checkS3Connection() {
        return isS3Available();
    }
}
