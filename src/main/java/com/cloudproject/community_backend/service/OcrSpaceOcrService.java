package com.cloudproject.community_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@Service
public class OcrSpaceOcrService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();
    private final String apiKey;
    private static final String OCR_API_URL = "https://api.ocr.space/parse/image";

    public OcrSpaceOcrService(@Value("${ocr.space.api.key}") String apiKey) {
        this.apiKey = apiKey;
    }

    public String extractSchoolName(MultipartFile studentCard) {
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
            body.add("OCREngine", "2");
            body.add("scale", "true");
            body.add("file", new FileSystemResource(tempFile));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            String response;
            try {
                ResponseEntity<String> responseEntity = restTemplate.postForEntity(OCR_API_URL, requestEntity, String.class);
                response = responseEntity.getBody();
                System.out.println("OCR API 응답 성공");
            } catch (Exception e) {
                System.out.println("OCR API 호출 에러: " + e.getMessage());
                response = "{\"ParsedResults\":[]}";
            }

            JsonNode root = objectMapper.readTree(response);
            if (root.has("ParsedResults") && root.get("ParsedResults").isArray()) {
                String parsedText = root.get("ParsedResults").get(0).get("ParsedText").asText();

                String[] lines = parsedText.split("\\r?\\n");
                String schoolName = "";
                for (int i = 0; i < lines.length; i++) {
                    String upper = lines[i].toUpperCase();

                    if (upper.contains("MIDDLE SCHOOL") ||
                        upper.contains("HIGH SCHOOL") ||
                        upper.contains("ELEMENTARY SCHOOL") ||
                        upper.contains("UNIVERSITY")) {

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
    

    public boolean verifySchoolName(String inputSchoolName, MultipartFile studentCard) {
        String extracted = extractSchoolName(studentCard);

        if (extracted.equals("인식 실패") || extracted.equals("에러 발생") || extracted.isEmpty()) {
            return false;
        }

        String normalizedExtracted = extracted.replaceAll("\\s+", "").toUpperCase();
        String normalizedInput = inputSchoolName.replaceAll("\\s+", "").toUpperCase();

        return normalizedExtracted.contains(normalizedInput) || normalizedInput.contains(normalizedExtracted);
    }

    public Integer extractAdmissionYear(MultipartFile studentCard) {
        try {
            System.out.println("extractAdmissionYear 호출됨");
            String originalExt;
            String originalName = studentCard.getOriginalFilename();
            if (originalName != null && originalName.contains(".")) {
                originalExt = originalName.substring(originalName.lastIndexOf("."));
            } else {
                originalExt = ".png";
            }

            // Convert to base64
            byte[] imageBytes = studentCard.getBytes();
            String base64Image = java.util.Base64.getEncoder().encodeToString(imageBytes);
            System.out.println("Base64 변환 완료, 길이: " + base64Image.length());

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("apikey", apiKey);
            body.add("base64Image", "data:image/png;base64," + base64Image);
            body.add("language", "eng");
            body.add("OCREngine", "1");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

            System.out.println("OCR API 호출 중...");
            String response;
            try {
                ResponseEntity<String> responseEntity = restTemplate.postForEntity(OCR_API_URL, requestEntity, String.class);
                response = responseEntity.getBody();
                System.out.println("OCR API 응답: " + response);
            } catch (Exception e) {
                System.out.println("OCR API 호출 에러: " + e.getMessage());
                e.printStackTrace();
                response = "{\"ParsedResults\":[]}";
            }

            JsonNode root = objectMapper.readTree(response);

            if (root.has("IsErroredOnProcessing") && root.get("IsErroredOnProcessing").asBoolean()) {
                String errorMessage = root.has("ErrorMessage") ? root.get("ErrorMessage").asText() : "Unknown error";
                System.out.println("OCR API 에러: " + errorMessage);
            }

            if (root.has("OCRExitCode")) {
                System.out.println("OCR Exit Code: " + root.get("OCRExitCode").asInt());
            }

            if (root.has("ParsedResults") && root.get("ParsedResults").isArray() && root.get("ParsedResults").size() > 0) {
                JsonNode firstResult = root.get("ParsedResults").get(0);
                if (firstResult != null && firstResult.has("ParsedText")) {
                    String parsedText = firstResult.get("ParsedText").asText();
                    System.out.println("추출된 텍스트: " + parsedText);

                    java.util.regex.Pattern yearPattern = java.util.regex.Pattern.compile("(202[0-9]|203[0-9])");
                    java.util.regex.Matcher matcher = yearPattern.matcher(parsedText);

                    if (matcher.find()) {
                        int year = Integer.parseInt(matcher.group(1));
                        System.out.println("매칭된 연도: " + year);
                        return year;
                    } else {
                        System.out.println("연도 패턴 매칭 실패");
                    }
                } else {
                    System.out.println("ParsedText가 없거나 firstResult가 null입니다");
                }
            } else {
                System.out.println("OCR API가 빈 결과를 반환했습니다 - 이미지를 인식하지 못했을 가능성이 있습니다");
            }

            return null;

        } catch (Exception e) {
            System.out.println("OCR 에러 발생:");
            e.printStackTrace();
            return null;
        }
    }

    public Integer calculateGradeFromYear(Integer admissionYear) {
        if (admissionYear == null) {
            return null;
        }

        int currentYear = java.time.Year.now().getValue();
        int grade = currentYear - admissionYear + 1;

        if (grade >= 1 && grade <= 3) {
            return grade;
        }

        return null;
    }

}
