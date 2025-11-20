package com.cloudproject.community_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

/**
 * 학생증 OCR 인증 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentIdVerificationRequest {

    /**
     * 학생증 이미지 파일
     */
    private MultipartFile studentIdImage;
}
