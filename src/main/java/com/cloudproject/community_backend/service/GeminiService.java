package com.cloudproject.community_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiService(@Value("${gemini.api.key}") String apiKey) {
        this.webClient = WebClient.builder()
            .baseUrl("https://generativelanguage.googleapis.com/v1beta/models")
            .defaultHeader("Content-Type", "application/json")
            .defaultHeader("X-goog-api-key", apiKey) // API 키 인증 방식 수정
            .build();
    }

    public boolean checkBadComment(String content, String username) {
        // 프롬프트 (한국어 + JSON 강제)
        String prompt = "당신은 매우 엄격한 댓글 관리자입니다.\n" +
            "다음과 같은 경우는 모두 '악플'로 간주합니다:\n" +
            "- 한국어 욕설 (\"씨발\", \"ㅅㅂ\", \"시발\", \"개새끼\", \"ㅈ같다\")\n" +
            "- 가족 관련 비하\n" +
            "- 성적 비하\n" +
            "- 공격적이거나 모욕적인 닉네임\n\n" +
            "아래 댓글과 작성자 닉네임이 악플인지 판별하세요.\n" +
            "출력은 반드시 JSON 객체만 반환해야 하며, 다른 어떤 텍스트나 코드 블록도 포함하지 마세요.\n\n" +
            "JSON 구조:\n" +
            "{\n" +
            "\"결과\": \"true\",\n" +
            "\"이유\": \"...\"\n" +
            "}\n\n\n" +
            "댓글: \"" + content + "\"\n" +
            "닉네임: \"" + username + "\"";

        try {
            String response = webClient.post()
                .uri("/gemini-2.0-flash:generateContent") // 올바른 모델명
                .bodyValue(Map.of(
                    "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                    )
                ))
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> Mono.just("false"))
                .block();

            System.out.println("Gemini raw response: " + response);

            // 응답 JSON 추출
            JsonNode root = objectMapper.readTree(response);
            String text = root.at("/candidates/0/content/parts/0/text").asText();

            if (text == null || text.isEmpty()) {
                System.out.println("Gemini가 응답 텍스트를 반환하지 않았습니다.");
                return false;
            }

            System.out.println("재미나이가 응답한거: " + text);

            try {
                // 코드블록 제거
                text = text.replaceAll("(?s)```json", "")
                           .replaceAll("(?s)```", "")
                           .trim();
            
                JsonNode parsed = objectMapper.readTree(text);
            
                if (parsed.has("결과")) {
                    boolean isBad = "true".equalsIgnoreCase(parsed.get("결과").asText());
                    String reason = parsed.has("이유") ? parsed.get("이유").asText() : "이유 없음";
            
                    // 판단 이유 출력
                    System.out.println("판단 이유: " + reason);
            
                    return isBad;
                } else {
                    System.out.println("❌ 예상 JSON 구조 아님 → " + text);
                    return false;
                }
            } catch (Exception parseEx) {
                System.out.println("❌ 응답 파싱 실패, 원본 응답: " + text);
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false; // 오류 시 안전하게 false
        }
    }
}
