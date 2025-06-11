package com.autocoin.file.application.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "cloud.aws.stack.auto", havingValue = "true")
public class RealS3Uploader implements S3UploaderInterface {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Override
    public String upload(MultipartFile multipartFile, String dirName) throws IOException {
        if (multipartFile == null || multipartFile.isEmpty()) {
            return null;
        }

        String originalFileName = multipartFile.getOriginalFilename();
        String fileName = createFileName(originalFileName, dirName);
        String fileKey = dirName + "/" + fileName;

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(multipartFile.getSize());
        metadata.setContentType(multipartFile.getContentType());

        try {
            amazonS3.putObject(new PutObjectRequest(bucket, fileKey, multipartFile.getInputStream(), metadata));
            log.info("S3 파일 업로드 성공: bucket={}, key={}", bucket, fileKey);
        } catch (IOException e) {
            log.error("S3 파일 업로드 중 오류 발생: {}", e.getMessage());
            throw new IOException("파일 업로드 실패", e);
        }

        return amazonS3.getUrl(bucket, fileKey).toString();
    }

    @Override
    public void delete(String fileKey) {
        if (fileKey == null || fileKey.isEmpty()) {
            return;
        }
        
        try {
            amazonS3.deleteObject(new DeleteObjectRequest(bucket, fileKey));
            log.info("S3 파일 삭제 성공: {}", fileKey);
        } catch (Exception e) {
            log.error("S3 파일 삭제 중 오류 발생: {}", e.getMessage());
        }
    }

    private String createFileName(String originalFileName, String dirName) {
        return UUID.randomUUID().toString() + "_" + originalFileName;
    }
}
