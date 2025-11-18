package com.cloudproject.community_backend.service;

import com.cloudproject.community_backend.dto.FilterResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 콘텐츠 필터링 서비스
 * - 기본 욕설 사전 체크 (속도 최적화)
 * - Gemini API 고도화된 프롬프트로 변형 욕설/따돌림/은어 감지
 */
@Service
public class ContentFilterService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String geminiApiKey;

    public ContentFilterService(
        WebClient.Builder webClientBuilder,
        @Value("${gemini.api.key:}") String geminiApiKey
    ) {
        this.webClient = webClientBuilder
            .baseUrl("https://generativelanguage.googleapis.com/v1beta/models")
            .build();
        this.geminiApiKey = geminiApiKey;
    }

    /**
     * 콘텐츠 필터링 메인 메서드
     *
     * @param content 검사할 텍스트
     * @param contentType "POST" | "COMMENT"
     * @param userId 작성자 ID
     * @return FilterResult (isBlocked, category, reason, confidence)
     */
    public FilterResult filterContent(String content, String contentType, Long userId) {
        // 1. 사전 검증 (기본 욕설 사전 체크 - 빠른 차단)
        if (containsBasicProfanity(content)) {
            System.out.println("기본 욕설 감지: " + content);
            return FilterResult.builder()
                .isBlocked(true)
                .category("기본욕설")
                .reason("기본 욕설이 감지되었습니다")
                .confidence(1.0)
                .build();
        }

        // 2. Gemini API 호출 (고도화된 프롬프트)
        try {
            GeminiResponse response = callGeminiAPI(content);

            // 3. 결과 분석 (신뢰도 0.7 이상만 차단)
            boolean isBlocked = response.isHarmful() && response.getConfidence() > 0.7;

            System.out.println(String.format(
                "🤖 Gemini 분석 결과 - 차단여부: %s, 카테고리: %s, 신뢰도: %.2f, 이유: %s",
                isBlocked, response.getCategory(), response.getConfidence(), response.getReason()
            ));

            return FilterResult.builder()
                .isBlocked(isBlocked)
                .category(response.getCategory())
                .reason(response.getReason())
                .confidence(response.getConfidence())
                .detectedWords(response.getDetectedWords())
                .build();

        } catch (Exception e) {
            System.err.println("❌ Gemini API 호출 실패: " + e.getMessage());
            e.printStackTrace();
            // 오류 시 안전하게 통과 (false positive 방지)
            return FilterResult.builder()
                .isBlocked(false)
                .category("정상")
                .reason("필터링 오류 - 기본 통과")
                .confidence(0.0)
                .build();
        }
    }

    /**
     * 기본 욕설 사전 체크 (속도 최적화용)
     */
    private boolean containsBasicProfanity(String content) {
        List<String> profanityList = Arrays.asList(
            // 명시적 욕설
            "시발", "씨발", "시1발", "시.발",
            "개새끼", "개새", "개색", "개 새끼",
            "병신", "븅신", "병1신",
            "씹", "씨1", "씨ㅂ",
            "좆", "ㅈ같", "존나",
            "엿먹어", "꺼져", "죽어", "뒤져",

            // 자음 욕설
            "ㅅㅂ", "ㅆㅂ", "ㅂㅅ", "ㄲㅈ", "ㅈㄴ", "ㅅ1ㅂ",

            // 변형 욕설
            "시부럴", "시1부", "개쉑", "븅1신",

            // 따돌림 표현
            "따돌리자", "무시해", "걸레", "찐따",

            // 은어/비하
            "급식충", "틀딱", "한남충", "김치녀", "루저"
        );

        String normalized = normalizeText(content);
        return profanityList.stream()
            .anyMatch(word -> normalized.contains(normalizeText(word)));
    }

    /**
     * 텍스트 정규화 (띄어쓰기, 특수문자 제거)
     */
    private String normalizeText(String text) {
        return text
            .replaceAll("\\s+", "")  // 공백 제거
            .replaceAll("[^가-힣ㄱ-ㅎㅏ-ㅣa-zA-Z0-9]", "")  // 특수문자 제거
            .toLowerCase();
    }

    /**
     * Gemini API 호출 (고도화된 프롬프트)
     */
    private GeminiResponse callGeminiAPI(String content) throws Exception {
        String prompt = buildAdvancedPrompt(content);

        if (geminiApiKey == null || geminiApiKey.isEmpty()) {
            System.err.println("GEMINI_API_KEY가 설정되지 않았습니다. application.properties를 확인하세요.");
            throw new IllegalStateException("GEMINI_API_KEY가 설정되지 않았습니다");
        }

        String response = webClient.post()
            .uri("/gemini-2.0-flash:generateContent")
            .header("X-goog-api-key", geminiApiKey)
            .bodyValue(Map.of(
                "contents", List.of(
                    Map.of("parts", List.of(Map.of("text", prompt)))
                )
            ))
            .retrieve()
            .bodyToMono(String.class)
            .block();

        return parseGeminiResponse(response);
    }

    /**
     * 고도화된 프롬프트 생성
     */
    private String buildAdvancedPrompt(String content) {
        return "당신은 중학생 커뮤니티의 콘텐츠 필터링 전문가입니다.\n" +
            "다음 텍스트가 청소년에게 부적절한지 판단해주세요.\n\n" +
            "<판단 기준>\n" +
            "1. 명시적 욕설: 시발, 개새끼, 병신, 씹, 좆, 엿먹어, 꺼져, 죽어 등\n" +
            "2. 자음 욕설: ㅅㅂ, ㄲㅈ, ㅂㅅ, ㅈㄴ, ㅆㅂ 등\n" +
            "3. 띄어쓰기 우회: 시 발, 개 새 끼, 병 신 등\n" +
            "4. 특수문자 우회: 시.발, 개*새*끼, 시1발 등\n" +
            "5. 변형 욕설: 멍청아, 바보야, 찌질이, 루저, 븅신 등\n" +
            "6. 따돌림성 표현: \"얘 따돌리자\", \"무시해\", \"걔랑 놀지마\", \"왕따\" 등\n" +
            "7. 은어/비하: 급식충, 틀딱, 한남충, 김치녀, 걸레, 찐따 등\n" +
            "8. 성적 암시: 야한, 19금 관련 노골적 표현\n" +
            "9. 개인정보 노출: 전화번호, 주소 패턴\n" +
            "10. 비방/명예훼손: 특정인 공격, 악의적 루머\n\n" +
            "<중요>\n" +
            "- 단순 의견 표현, 일상 대화는 정상으로 판단\n" +
            "- \"바보같다\", \"멍청하다\" 정도는 맥락에 따라 판단\n" +
            "- 확신이 없으면 confidence를 낮게 설정\n\n" +
            "<응답 형식>\n" +
            "반드시 JSON 형식으로만 응답하세요 (마크다운 코드블록 사용 금지):\n" +
            "{\n" +
            "  \"is_harmful\": true/false,\n" +
            "  \"category\": \"욕설|자음욕설|변형욕설|따돌림|은어|성적표현|개인정보|비방|정상\",\n" +
            "  \"confidence\": 0.0-1.0,\n" +
            "  \"detected_words\": [\"감지된\", \"단어들\"],\n" +
            "  \"reason\": \"판단 이유 (50자 이내)\"\n" +
            "}\n\n" +
            "분석할 텍스트: \"" + content + "\"";
    }

    /**
     * Gemini 응답 파싱
     */
    private GeminiResponse parseGeminiResponse(String response) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        String text = root.at("/candidates/0/content/parts/0/text").asText();

        if (text == null || text.isEmpty()) {
            throw new IllegalStateException("Gemini 응답이 비어있습니다");
        }

        // 코드블록 제거
        text = text.replaceAll("(?s)```json", "")
                   .replaceAll("(?s)```", "")
                   .trim();

        JsonNode parsed = objectMapper.readTree(text);

        return GeminiResponse.builder()
            .isHarmful(parsed.get("is_harmful").asBoolean())
            .category(parsed.get("category").asText())
            .confidence(parsed.get("confidence").asDouble())
            .detectedWords(parseDetectedWords(parsed.get("detected_words")))
            .reason(parsed.get("reason").asText())
            .build();
    }

    private List<String> parseDetectedWords(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        return Arrays.stream(objectMapper.convertValue(node, String[].class))
            .toList();
    }

    /**
     * Gemini 응답 DTO
     */
    @lombok.Builder
    @lombok.Getter
    private static class GeminiResponse {
        private boolean isHarmful;
        private String category;
        private double confidence;
        private List<String> detectedWords;
        private String reason;
    }
}
