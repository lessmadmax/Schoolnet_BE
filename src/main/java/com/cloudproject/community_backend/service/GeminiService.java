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
            .defaultHeader("X-goog-api-key", apiKey)
            .build();
    }

    /**
     * 질문에 대한 AI 답변 생성
     */
    public String generateAnswer(String question) {
        String prompt = "당신은 친절하고 지식이 풍부한 AI 선배입니다.\n" +
            "학생들의 질문에 도움이 되는 답변을 제공해주세요.\n" +
            "답변은 명확하고 이해하기 쉽게 작성하며, 필요한 경우 예시를 들어주세요.\n" +
            "답변은 한국어로 작성하고, 존댓말을 사용해주세요.\n\n" +
            "질문: " + question + "\n\n" +
            "답변:";

        try {
            String response = webClient.post()
                .uri("/gemini-2.0-flash:generateContent")
                .bodyValue(Map.of(
                    "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                    )
                ))
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> {
                    System.err.println("Gemini API 호출 실패: " + e.getMessage());
                    return Mono.just("");
                })
                .block();

            if (response == null || response.isEmpty()) {
                return "죄송합니다. AI 답변 생성에 실패했습니다.";
            }

            // 응답에서 텍스트 추출
            JsonNode root = objectMapper.readTree(response);
            String text = root.at("/candidates/0/content/parts/0/text").asText();

            if (text == null || text.isEmpty()) {
                return "죄송합니다. AI 답변 생성에 실패했습니다.";
            }

            return text.trim();

        } catch (Exception e) {
            System.err.println("AI 답변 생성 중 오류: " + e.getMessage());
            e.printStackTrace();
            return "죄송합니다. AI 답변 생성 중 오류가 발생했습니다.";
        }
    }

    /**
     * 댓글 악플 필터링 (기존 메서드 유지)
     */
    public boolean checkBadComment(String content, String username) {
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
                .uri("/gemini-2.0-flash:generateContent")
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

            JsonNode root = objectMapper.readTree(response);
            String text = root.at("/candidates/0/content/parts/0/text").asText();

            if (text == null || text.isEmpty()) {
                System.out.println("Gemini가 응답 텍스트를 반환하지 않았습니다.");
                return false;
            }

            System.out.println("재미나이가 응답한거: " + text);

            try {
                text = text.replaceAll("(?s)```json", "")
                           .replaceAll("(?s)```", "")
                           .trim();

                JsonNode parsed = objectMapper.readTree(text);

                if (parsed.has("결과")) {
                    boolean isBad = "true".equalsIgnoreCase(parsed.get("결과").asText());
                    return isBad;
                } else {
                    return false;
                }
            } catch (Exception parseEx) {
                return false;
            }

        } catch (Exception e) {
            return false;
        }
    }
}
