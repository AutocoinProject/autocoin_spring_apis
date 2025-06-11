package com.autocoin.file.application.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Service
@ConditionalOnProperty(name = "cloud.aws.stack.auto", havingValue = "false", matchIfMissing = true)
@Slf4j
public class MockS3Uploader implements S3UploaderInterface {

    @Override
    public String upload(MultipartFile multipartFile, String dirName) throws IOException {
        log.info("Mock S3 Upload: 파일명={}, 디렉토리={}", multipartFile.getOriginalFilename(), dirName);
        // Mock URL 반환
        return "https://mock-s3-bucket.s3.ap-northeast-2.amazonaws.com/" + dirName + "/" + multipartFile.getOriginalFilename();
    }

    @Override
    public void delete(String fileName) {
        log.info("Mock S3 Delete: 파일명={}", fileName);
    }
}
