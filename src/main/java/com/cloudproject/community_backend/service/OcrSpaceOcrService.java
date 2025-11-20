package com.cloudproject.community_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.File;

@Service
public class OcrSpaceOcrService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebClient webClient;
    private final String apiKey;

    public OcrSpaceOcrService(@Value("${ocr.space.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.webClient = WebClient.builder()
                .baseUrl("https://api.ocr.space")
                .build();
    }

    /**
     * OCR에서 학교명만 추출
     */
    public String extractSchoolName(MultipartFile studentCard) {
        try {
            String originalExt;
            String originalName = studentCard.getOriginalFilename();
            if (originalName != null && originalName.contains(".")) {
                originalExt = originalName.substring(originalName.lastIndexOf("."));
            } else {
                originalExt = ".png"; // 기본 확장자
            }
    
            // 임시 파일 생성 (확장자 유지)
            File tempFile = File.createTempFile("studentCard", originalExt);
            studentCard.transferTo(tempFile);
    
            // Multipart 데이터 생성
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("apikey", apiKey);
            body.add("language", "eng");
            body.add("file", new FileSystemResource(tempFile));
    
            // OCR.Space API 호출
            String response = webClient.post()
                    .uri("/parse/image")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .header("apikey", apiKey)
                    .body(BodyInserters.fromMultipartData(body))
                    .retrieve()
                    .bodyToMono(String.class)
                    .onErrorResume(e -> Mono.just("{\"ParsedResults\":[]}"))
                    .block();
    
            System.out.println("OCR.Space raw response: " + response);
    
            // 결과 파싱
            JsonNode root = objectMapper.readTree(response);
            if (root.has("ParsedResults") && root.get("ParsedResults").isArray()) {
                String parsedText = root.get("ParsedResults").get(0).get("ParsedText").asText();
    
                String[] lines = parsedText.split("\\r?\\n");
                String schoolName = "";
                for (int i = 0; i < lines.length; i++) {
                    String upper = lines[i].toUpperCase();
    
                    // 중학교/고등학교/초등학교/대학교 패턴 체크
                    if (upper.contains("MIDDLE SCHOOL") ||
                        upper.contains("HIGH SCHOOL") ||
                        upper.contains("ELEMENTARY SCHOOL") ||
                        upper.contains("UNIVERSITY")) {
    
                        // 줄 그대로 반환
                        schoolName = lines[i];
                        break;
                    }
                }
    
                return schoolName.trim();
            }
    
            return "인식 실패";
    
        } catch (Exception e) {
            e.printStackTrace();
            return "에러 발생";
        }
    }
    

    /**
     * 사용자 입력값과 OCR 학교명 비교
     * - 대소문자 무시
     * - 공백 무시
     * - "KYUNGHEE"만 입력해도 승인
     */
    public boolean verifySchoolName(String inputSchoolName, MultipartFile studentCard) {
        String extracted = extractSchoolName(studentCard);

        // OCR 실패 시 바로 false
        if (extracted.equals("인식 실패") || extracted.equals("에러 발생") || extracted.isEmpty()) {
            return false;
        }

        // 공백 제거 + 대문자로 통일
        String normalizedExtracted = extracted.replaceAll("\\s+", "").toUpperCase();   // ex: KYUNGHEEUNIVERSITY
        String normalizedInput = inputSchoolName.replaceAll("\\s+", "").toUpperCase(); // ex: KYUNGHEE

        System.out.println("입력값: " + normalizedInput + " / OCR값: " + normalizedExtracted);

        // 조건1: OCR 결과가 입력값을 포함 (KYUNGHEE → KYUNGHEEUNIVERSITY)
        // 조건2: 입력값이 OCR 결과를 포함 (KYUNGHEEUNIVERSITY → KYUNGHEE)
        return normalizedExtracted.contains(normalizedInput) || normalizedInput.contains(normalizedExtracted);
    }

    /**
     * 학생증에서 입학년도 추출
     * @param studentCard 학생증 이미지
     * @return 입학년도 (4자리 숫자, 예: 2022) 또는 null (인식 실패 시)
     */
    public Integer extractAdmissionYear(MultipartFile studentCard) {
        try {
            String originalExt;
            String originalName = studentCard.getOriginalFilename();
            if (originalName != null && originalName.contains(".")) {
                originalExt = originalName.substring(originalName.lastIndexOf("."));
            } else {
                originalExt = ".png";
            }

            File tempFile = File.createTempFile("studentCard", originalExt);
            studentCard.transferTo(tempFile);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("apikey", apiKey);
            body.add("language", "eng");
            body.add("file", new FileSystemResource(tempFile));

            String response = webClient.post()
                    .uri("/parse/image")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .header("apikey", apiKey)
                    .body(BodyInserters.fromMultipartData(body))
                    .retrieve()
                    .bodyToMono(String.class)
                    .onErrorResume(e -> Mono.just("{\"ParsedResults\":[]}"))
                    .block();

            System.out.println("OCR 연도 추출 응답: " + response);

            JsonNode root = objectMapper.readTree(response);
            if (root.has("ParsedResults") && root.get("ParsedResults").isArray()) {
                String parsedText = root.get("ParsedResults").get(0).get("ParsedText").asText();

                // 4자리 연도 패턴 찾기 (2020-2030 범위)
                java.util.regex.Pattern yearPattern = java.util.regex.Pattern.compile("(202[0-9]|203[0-9])");
                java.util.regex.Matcher matcher = yearPattern.matcher(parsedText);

                if (matcher.find()) {
                    int year = Integer.parseInt(matcher.group(1));
                    System.out.println("추출된 입학년도: " + year);
                    return year;
                }
            }

            tempFile.delete();
            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 입학년도로부터 현재 학년 계산
     * @param admissionYear 입학년도
     * @return 학년 (1, 2, 3) 또는 null (유효하지 않은 경우)
     */
    public Integer calculateGradeFromYear(Integer admissionYear) {
        if (admissionYear == null) {
            return null;
        }

        int currentYear = java.time.Year.now().getValue();
        int grade = currentYear - admissionYear + 1;

        // 1~3학년만 유효
        if (grade >= 1 && grade <= 3) {
            return grade;
        }

        return null;
    }

}
