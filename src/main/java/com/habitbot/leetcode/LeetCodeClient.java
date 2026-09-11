package com.habitbot.leetcode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class LeetCodeClient {

    private static final String BASE_URL = "https://leetcode.com";
    private static final String GRAPHQL_URL = BASE_URL + "/graphql/";

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final String session;
    private final String csrfToken;

    public LeetCodeClient() {
        this.session = requireEnv("LEETCODE_SESSION");
        this.csrfToken = requireEnv("LEETCODE_CSRF_TOKEN");

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        this.mapper = new ObjectMapper();
    }

    public LeetCodeSubmissionResult submitAndWait(
            String slug,
            String code,
            String language
    ) throws IOException, InterruptedException {

        long questionId = fetchQuestionId(slug);
        long submissionId = submit(slug, questionId, code, language);

        for (int attempt = 0; attempt < 60; attempt++) {
            Thread.sleep(1000);

            JsonNode result = check(slug, submissionId);
            String state = result.path("state").asText();

            if ("SUCCESS".equals(state)) {
                return LeetCodeSubmissionResult.from(result, submissionId);
            }

            if (!state.isBlank()
                    && !"PENDING".equals(state)
                    && !"STARTED".equals(state)) {
                throw new IOException(
                        "Unexpected LeetCode submission state: " + state
                );
            }
        }

        throw new IOException("LeetCode did not finish judging within 60 seconds.");
    }

    private long fetchQuestionId(String slug)
            throws IOException, InterruptedException {

        String query = """
                query questionEditorData($titleSlug: String!) {
                  question(titleSlug: $titleSlug) {
                    questionId
                    questionFrontendId
                    title
                  }
                }
                """;

        ObjectNode variables = mapper.createObjectNode();
        variables.put("titleSlug", slug);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("operationName", "questionEditorData");
        payload.put("query", query);
        payload.set("variables", variables);

        HttpRequest request = baseRequest(
                URI.create(GRAPHQL_URL),
                BASE_URL + "/problems/" + slug + "/"
        )
                .POST(HttpRequest.BodyPublishers.ofString(
                        mapper.writeValueAsString(payload)
                ))
                .build();

        JsonNode root = sendJson(request);

        JsonNode question = root.path("data").path("question");

        if (question.isMissingNode() || question.isNull()) {
            throw new IOException("LeetCode problem not found: " + slug);
        }

        String id = question.path("questionId").asText();

        if (id.isBlank()) {
            throw new IOException("LeetCode did not return questionId for: " + slug);
        }

        return Long.parseLong(id);
    }

    private long submit(
            String slug,
            long questionId,
            String code,
            String language
    ) throws IOException, InterruptedException {

        ObjectNode payload = mapper.createObjectNode();
        payload.put("lang", language);
        payload.put("question_id", questionId);
        payload.put("typed_code", code);

        URI uri = URI.create(
                BASE_URL + "/problems/" + slug + "/submit/"
        );

        HttpRequest request = baseRequest(
                uri,
                BASE_URL + "/problems/" + slug + "/"
        )
                .POST(HttpRequest.BodyPublishers.ofString(
                        mapper.writeValueAsString(payload)
                ))
                .build();

        JsonNode root = sendJson(request);

        long submissionId = root.path("submission_id").asLong(0);

        if (submissionId == 0) {
            throw new IOException(
                    "LeetCode submit failed: " + root
            );
        }

        return submissionId;
    }

    private JsonNode check(
            String slug,
            long submissionId
    ) throws IOException, InterruptedException {

        URI uri = URI.create(
                BASE_URL
                        + "/submissions/detail/"
                        + submissionId
                        + "/check/"
        );

        HttpRequest request = baseRequest(
                uri,
                BASE_URL + "/problems/" + slug + "/"
        )
                .GET()
                .build();

        return sendJson(request);
    }

    private HttpRequest.Builder baseRequest(
            URI uri,
            String referer
    ) {
        return HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Origin", BASE_URL)
                .header("Referer", referer)
                .header("X-CSRFToken", csrfToken)
                .header(
                        "Cookie",
                        "LEETCODE_SESSION=" + session
                                + "; csrftoken=" + csrfToken
                )
                .header(
                        "User-Agent",
                        "Mozilla/5.0 DSA75TelegramBot/1.0"
                );
    }

    private JsonNode sendJson(HttpRequest request)
            throws IOException, InterruptedException {

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {
            throw new IOException(
                    "LeetCode HTTP "
                            + response.statusCode()
                            + ": "
                            + response.body()
            );
        }

        return mapper.readTree(response.body());
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Missing environment variable: " + name
            );
        }

        return value;
    }
}
