package com.cloudproject.community_backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import com.cloudproject.community_backend.dto.SeniorStatusResponse;
import com.cloudproject.community_backend.dto.SeniorVerificationRequest;
import com.cloudproject.community_backend.entity.School;
import com.cloudproject.community_backend.entity.User;
import com.cloudproject.community_backend.repository.SchoolRepository;
import com.cloudproject.community_backend.repository.UserRepository;
import com.cloudproject.community_backend.security.JwtUtil;
import com.cloudproject.community_backend.service.OcrSpaceOcrService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.List;



@Tag(name = "회원", description = "회원 관련 API (회원가입/조회)")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SchoolRepository schoolRepository;
    private final OcrSpaceOcrService ocrSpaceOcrService;
    private final JwtUtil jwtUtil;
    
    @PostMapping(
        value = "/register",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(summary = "회원가입 (학생증 인증)", description = "학생증 인증을 통해 새로운 사용자를 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "인증 실패 / 중복 이메일")
    })
    public ResponseEntity<?> register(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String username,
            @RequestParam String schoolName,
            @RequestPart MultipartFile studentCard
    ) throws IOException {

        // 이메일 중복 체크
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 가입된 이메일입니다.");
        }

        // 학생증 OCR 처리 및 학교명 추출
        String recognizedSchool = ocrSpaceOcrService.extractSchoolName(studentCard);
        System.out.println("OCR 반환 학교명: " + recognizedSchool);

        // 공백 제거 + 대소문자 무시
        String normalizedInput = schoolName.replaceAll("\\s+", "").toLowerCase();
        String normalizedOcr = recognizedSchool.replaceAll("\\s+", "").toLowerCase();

        if (!normalizedInput.equals(normalizedOcr)) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("학교 인증 실패. 입력한 학교명: " + schoolName + ", OCR 결과: " + recognizedSchool);
        }

        System.out.println("OCR 반환 학교명: " + recognizedSchool);

        // DB에서 학교 조회 (없으면 생성)
        School school = schoolRepository.findByName(schoolName)
                .orElseGet(() -> schoolRepository.save(new School(null, schoolName, null)));


        // 가입 진행
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setUsername(username);
        user.setSchool(school);
        return ResponseEntity.ok(userRepository.save(user));
    }

    @Operation(summary = "모든 사용자 조회", description = "등록된 모든 사용자를 조회합니다.")
    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Operation(summary = "특정 사용자 조회", description = "ID로 특정 사용자를 조회합니다.")
    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userRepository.findById(id).orElse(null);
    }

    /**
     * 선배 인증 (수동 입력)
     */
    @PostMapping("/senior-verification")
    @Operation(summary = "선배 인증 (수동)", description = "2학년 이상 학생이 학년을 직접 입력해서 선배 인증을 진행합니다")
    public ResponseEntity<com.cloudproject.community_backend.dto.ApiResponse<Void>> verifySenior(
        @RequestBody @Valid SeniorVerificationRequest request,
        HttpServletRequest httpRequest
    ) {
        Long userId = getUserIdFromToken(httpRequest);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"));

        // 학년 업데이트
        user.setGrade(request.getGrade());
        user.setIsSeniorVerified(true);
        user.setSeniorVerifiedAt(java.time.LocalDateTime.now());

        userRepository.save(user);

        System.out.println(String.format(
            "선배 인증 완료 - 사용자: %s, 학년: %d",
            user.getUsername(), request.getGrade()
        ));

        return ResponseEntity.ok(
            com.cloudproject.community_backend.dto.ApiResponse.success("선배 인증이 완료되었습니다")
        );
    }

    /**
     * 학생증 OCR 기반 선배 인증
     */
    @PostMapping(value = "/senior-verification/ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "학생증 OCR 선배 인증", description = "학생증 이미지에서 입학년도를 추출하여 자동으로 선배 인증을 진행합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "선배 인증 성공"),
        @ApiResponse(responseCode = "400", description = "OCR 인식 실패 / 유효하지 않은 학년")
    })
    public ResponseEntity<com.cloudproject.community_backend.dto.ApiResponse<String>> verifySeniorWithOcr(
        @RequestPart MultipartFile studentIdImage,
        HttpServletRequest httpRequest
    ) {
        Long userId = getUserIdFromToken(httpRequest);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"));

        // OCR로 입학년도 추출
        Integer admissionYear = ocrSpaceOcrService.extractAdmissionYear(studentIdImage);
        if (admissionYear == null) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(com.cloudproject.community_backend.dto.ApiResponse.error("학생증에서 입학년도를 인식할 수 없습니다. 다시 시도해주세요."));
        }

        // 입학년도로 학년 계산
        Integer grade = ocrSpaceOcrService.calculateGradeFromYear(admissionYear);
        if (grade == null || grade < 2) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(com.cloudproject.community_backend.dto.ApiResponse.error(
                    "선배 인증은 2학년 이상만 가능합니다. 추출된 입학년도: " + admissionYear
                ));
        }

        // 선배 인증 완료
        user.setGrade(grade);
        user.setIsSeniorVerified(true);
        user.setSeniorVerifiedAt(java.time.LocalDateTime.now());
        userRepository.save(user);

        System.out.println(String.format(
            "OCR 선배 인증 완료 - 사용자: %s, 입학년도: %d, 학년: %d",
            user.getUsername(), admissionYear, grade
        ));

        return ResponseEntity.ok(
            com.cloudproject.community_backend.dto.ApiResponse.success(
                String.format("선배 인증이 완료되었습니다. 학년: %d학년 (입학년도: %d)", grade, admissionYear)
            )
        );
    }

    /**
     * 내 선배 인증 상태 조회
     */
    @GetMapping("/me/senior-status")
    @Operation(summary = "선배 인증 상태 조회", description = "본인의 선배 인증 상태를 조회합니다")
    public ResponseEntity<SeniorStatusResponse> getMySeniorStatus(
        HttpServletRequest httpRequest
    ) {
        Long userId = getUserIdFromToken(httpRequest);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"));

        SeniorStatusResponse response = SeniorStatusResponse.builder()
            .isSeniorVerified(user.getIsSeniorVerified())
            .grade(user.getGrade())
            .verifiedAt(user.getSeniorVerifiedAt())
            .canAnswerQuestions(user.getIsSeniorVerified() != null && user.getIsSeniorVerified())
            .build();

        return ResponseEntity.ok(response);
    }

    /**
     * JWT 토큰에서 사용자 ID 추출
     */
    private Long getUserIdFromToken(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        if (token == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 토큰이 없습니다");
        }
        return jwtUtil.getUserIdFromToken(token);
    }

    /**
     * 요청에서 JWT 토큰 추출
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
