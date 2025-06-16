package com.autocoin.file.application;

import com.autocoin.file.domain.File;
import com.autocoin.file.domain.FileRepository;
import com.autocoin.global.exception.core.CustomException;
import com.autocoin.global.exception.core.ErrorCode;
import com.autocoin.file.application.service.S3UploaderInterface;
import com.autocoin.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRepository fileRepository;
    private final S3UploaderInterface s3Uploader;
    
    private static final String S3_DIRECTORY = "files";

    @Transactional
    public File uploadFile(MultipartFile multipartFile, User user) {
        // 파일이 없으면 null 반환 (선택적 업로드)
        if (multipartFile == null || multipartFile.isEmpty()) {
            log.info("파일이 없어서 업로드를 건너뛁니다.");
            return null;
        }

        try {
            log.info("파일 업로드 시작: {}", multipartFile.getOriginalFilename());
            
            // S3에 파일 업로드 시도
            String fileUrl = s3Uploader.upload(multipartFile, S3_DIRECTORY);
            
            if (fileUrl == null) {
                log.warn("S3 업로드 실패 - 파일 없이 계속 진행");
                return null;
            }
            
            // 파일 정보 저장
            File file = File.builder()
                    .originalFileName(multipartFile.getOriginalFilename())
                    .storedFileName(extractFileNameFromUrl(fileUrl))
                    .fileUrl(fileUrl)
                    .contentType(multipartFile.getContentType())
                    .fileSize(multipartFile.getSize())
                    .user(user)
                    .build();
                    
            File savedFile = fileRepository.save(file);
            log.info("파일 업로드 성공: ID={}, URL={}", savedFile.getId(), fileUrl);
            return savedFile;
            
        } catch (Exception e) {
            log.error("파일 업로드 중 오류 발생: {}", e.getMessage());
            // S3 업로드 실패해도 게시글 작성은 계속 진행
            return null;
        }
    }
    
    /**
     * 파일 업로드 (옵션널) - S3 연결 실패해도 예외 발생 안함
     */
    @Transactional
    public File uploadFileOptional(MultipartFile multipartFile, User user) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            return null;
        }

        try {
            return uploadFile(multipartFile, user);
        } catch (Exception e) {
            log.warn("파일 업로드 실패했지만 계속 진행: {}", e.getMessage());
            return null;
        }
    }
    
    @Transactional(readOnly = true)
    public File findFileById(Long fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new CustomException(ErrorCode.FILE_NOT_FOUND));
    }
    
    @Transactional(readOnly = true)
    public List<File> findFilesByUser(User user) {
        return fileRepository.findByUser(user);
    }
    
    @Transactional
    public void deleteFile(Long fileId, User user) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new CustomException(ErrorCode.FILE_NOT_FOUND));
                
        // 파일 소유자 확인
        if (!file.getUser().getId().equals(user.getId())) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
        
        try {
            // S3에서 파일 삭제 시도
            String fileKey = extractFileKeyFromUrl(file.getFileUrl());
            s3Uploader.delete(fileKey);
            log.info("S3 파일 삭제 성공: {}", fileKey);
        } catch (Exception e) {
            log.warn("S3 파일 삭제 실패: {}", e.getMessage());
        }
        
        // DB에서 파일 정보는 무조건 삭제
        fileRepository.delete(file);
        log.info("DB에서 파일 정보 삭제 완료: ID={}", fileId);
    }
    
    private String extractFileNameFromUrl(String fileUrl) {
        if (fileUrl == null) return null;
        return fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
    }
    
    private String extractFileKeyFromUrl(String fileUrl) {
        if (fileUrl == null) return null;
        // https://bucket-name.s3.region.amazonaws.com/files/filename.ext
        // 에서 "files/filename.ext" 부분만 추출
        String fileName = extractFileNameFromUrl(fileUrl);
        return S3_DIRECTORY + "/" + fileName;
    }
    
    /**
     * S3 연결 상태 확인
     * @return S3 연결 가능 여부
     */
    public boolean checkS3Connection() {
        return s3Uploader.checkS3Connection();
    }
}
