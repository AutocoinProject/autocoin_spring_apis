package com.autocoin.file.api;

import com.autocoin.file.application.FileService;
import com.autocoin.file.domain.File;
import com.autocoin.file.dto.FileResponseDto;
import com.autocoin.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Tag(name = "파일 API", description = "파일 업로드 및 관리 API")
public class FileController {

    private final FileService fileService;

    /**
     * 파일 업로드 API
     * 파일을 S3에 업로드합니다.
     * 
     * @param file 업로드할 파일 (필수)
     * @param user 현재 인증된 사용자
     * @return 업로드된 파일 정보
     */
    @Operation(summary = "파일 업로드", description = "파일을 S3에 업로드합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "업로드 성공",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = FileResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "유효하지 않은 요청", content = @Content),
        @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileResponseDto> uploadFile(
            @Parameter(description = "업로드할 파일", required = true) @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user) {
        File savedFile = fileService.uploadFile(file, user);
        return new ResponseEntity<>(FileResponseDto.of(savedFile), HttpStatus.CREATED);
    }
    
    /**
     * 파일 조회 API
     * ID로 파일 정보를 조회합니다.
     * 
     * @param fileId 파일 ID (필수)
     * @return 파일 정보
     */
    @Operation(summary = "파일 조회", description = "ID로 파일 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = FileResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "파일을 찾을 수 없음", content = @Content),
        @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @GetMapping("/{fileId}")
    public ResponseEntity<FileResponseDto> getFile(
            @Parameter(description = "파일 ID", required = true) @PathVariable Long fileId) {
        File file = fileService.findFileById(fileId);
        return ResponseEntity.ok(FileResponseDto.of(file));
    }
    
    /**
     * 사용자 파일 목록 조회 API
     * 현재 사용자가 업로드한 파일 목록을 조회합니다.
     * 
     * @param user 현재 인증된 사용자
     * @return 파일 정보 목록
     */
    @Operation(summary = "사용자 파일 목록 조회", description = "현재 사용자가 업로드한 파일 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = FileResponseDto.class))),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content),
        @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @GetMapping("/user")
    public ResponseEntity<List<FileResponseDto>> getUserFiles(
            @AuthenticationPrincipal User user) {
        List<File> files = fileService.findFilesByUser(user);
        List<FileResponseDto> responseDtos = files.stream()
                .map(FileResponseDto::of)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDtos);
    }
    
    /**
     * 파일 삭제 API
     * 파일을 삭제합니다.
     * 
     * @param fileId 파일 ID (필수)
     * @param user 현재 인증된 사용자
     * @return 응답 없음 (204 No Content)
     */
    @Operation(summary = "파일 삭제", description = "파일을 삭제합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "삭제 성공", content = @Content),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content),
        @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content),
        @ApiResponse(responseCode = "404", description = "파일을 찾을 수 없음", content = @Content),
        @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(
            @Parameter(description = "파일 ID", required = true) @PathVariable Long fileId,
            @AuthenticationPrincipal User user) {
        fileService.deleteFile(fileId, user);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * S3 연결 상태 확인 API
     */
    @Operation(summary = "S3 연결 상태 확인", description = "S3 서비스 연결 상태를 확인합니다.")
    @GetMapping("/health/s3")
    public ResponseEntity<String> checkS3Connection() {
        boolean isConnected = fileService.checkS3Connection();
        if (isConnected) {
            return ResponseEntity.ok("S3 연결 상태: 연결됨");
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("S3 연결 상태: 연결 실패 (파일 업로드는 스키됩니다)");
        }
    }
}
