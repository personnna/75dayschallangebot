package com.habitbot.leetcode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.habitbot.dsa.Difficulty;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

public final class LeetCodeProblemService {

    private static final String GRAPHQL_URL = "https://leetcode.com/graphql/";

    private final HttpClient httpClient =
            HttpClient.newBuilder()
                    .connectTimeout(
                            Duration.ofSeconds(15)
                    )
                    .build();


    private final ObjectMapper mapper = new ObjectMapper();

    private final String session =
            requireEnv("LEETCODE_SESSION");

    private final String csrfToken =
            requireEnv("LEETCODE_CSRF_TOKEN");

    public LeetCodeProblem randomProblem(
            Difficulty difficulty,
            String excludeSlug
    ) throws IOException, InterruptedException {

        int total = fetchCount(difficulty);

        if (total <= 0) {
            throw new IOException("No " + difficulty + " problems returned by LeetCode.");
        }

        for (int attempt = 0; attempt < 8; attempt++) {
            int skip = ThreadLocalRandom.current().nextInt(total);
            JsonNode summary = fetchOne(difficulty, skip);
            String slug = summary.path("titleSlug").asText();

            if (slug.isBlank()) {
                continue;
            }

            if (excludeSlug != null && excludeSlug.equals(slug) && total > 1) {
                continue;
            }

            return fetchDetails(slug);
        }

        throw new IOException("Could not choose a random LeetCode problem.");
    }

    private int fetchCount(Difficulty difficulty)
            throws IOException, InterruptedException {

        JsonNode root = fetchProblemList(difficulty, 0, 1);

        return root.path("data")
                .path("problemsetQuestionList")
                .path("total")
                .asInt(0);
    }

    private JsonNode fetchOne(
            Difficulty difficulty,
            int skip
    ) throws IOException, InterruptedException {

        JsonNode root = fetchProblemList(difficulty, skip, 1);

        JsonNode questions = root.path("data")
                .path("problemsetQuestionList")
                .path("questions");

        if (!questions.isArray() || questions.isEmpty()) {
            throw new IOException("LeetCode returned an empty problem list.");
        }

        return questions.get(0);
    }

    private JsonNode fetchProblemList(
            Difficulty difficulty,
            int skip,
            int limit
    ) throws IOException, InterruptedException {

        String query = """
                query problemsetQuestionList(
                  $categorySlug: String,
                  $limit: Int,
                  $skip: Int,
                  $filters: QuestionListFilterInput
                ) {
                  problemsetQuestionList: questionList(
                    categorySlug: $categorySlug
                    limit: $limit
                    skip: $skip
                    filters: $filters
                  ) {
                    total: totalNum
                    questions: data {
                      frontendQuestionId: questionFrontendId
                      title
                      titleSlug
                      difficulty
                      paidOnly: isPaidOnly
                    }
                  }
                }
                """;

        ObjectNode filters = mapper.createObjectNode();
        filters.put("difficulty", difficulty.name());
        filters.put("premiumOnly", false);

        ObjectNode variables = mapper.createObjectNode();
        variables.put("categorySlug", "");
        variables.put("skip", skip);
        variables.put("limit", limit);
        variables.set("filters", filters);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("operationName", "problemsetQuestionList");
        payload.put("query", query);
        payload.set("variables", variables);

        return postGraphQl(payload);
    }

    public LeetCodeProblem fetchDetails(String slug)
            throws IOException, InterruptedException {

        String query = """
                query questionData($titleSlug: String!) {
                  question(titleSlug: $titleSlug) {
                    questionFrontendId
                    title
                    titleSlug
                    content
                    difficulty
                    isPaidOnly
                    codeSnippets {
                      lang
                      langSlug
                      code
                    }
                  }
                }
                """;

        ObjectNode variables = mapper.createObjectNode();
        variables.put("titleSlug", slug);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("operationName", "questionData");
        payload.put("query", query);
        payload.set("variables", variables);

        JsonNode root = postGraphQl(payload);
        JsonNode q = root.path("data").path("question");

        if (q.isMissingNode() || q.isNull()) {
            throw new IOException("Problem not found: " + slug);
        }

        if (q.path("isPaidOnly").asBoolean(false)) {
            throw new IOException("Premium-only problem selected.");
        }

        int frontendId;
        try {
            frontendId = Integer.parseInt(q.path("questionFrontendId").asText("0"));
        } catch (NumberFormatException e) {
            frontendId = 0;
        }

        return new LeetCodeProblem(
                frontendId,
                q.path("title").asText(),
                q.path("titleSlug").asText(),
                q.path("difficulty").asText(),
                htmlToText(q.path("content").asText("")),
                javaTemplate(q.path("codeSnippets"))
        );
    }

    private JsonNode postGraphQl(
            ObjectNode payload
    ) throws IOException, InterruptedException {

        HttpRequest request =
                HttpRequest.newBuilder(
                                URI.create(GRAPHQL_URL)
                        )
                        .timeout(
                                Duration.ofSeconds(25)
                        )

                        .header(
                                "Content-Type",
                                "application/json"
                        )

                        .header(
                                "Accept",
                                "application/json"
                        )

                        .header(
                                "Origin",
                                "https://leetcode.com"
                        )

                        .header(
                                "Referer",
                                "https://leetcode.com/problemset/"
                        )

                        .header(
                                "X-CSRFToken",
                                csrfToken
                        )

                        .header(
                                "Cookie",
                                "LEETCODE_SESSION="
                                        + session
                                        + "; csrftoken="
                                        + csrfToken
                        )

                        .header(
                                "User-Agent",
                                "Mozilla/5.0 DSA75TelegramBot/1.0"
                        )

                        .POST(
                                HttpRequest.BodyPublishers.ofString(
                                        mapper.writeValueAsString(
                                                payload
                                        )
                                )
                        )

                        .build();


        HttpResponse<String> response =
                httpClient.send(
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


        JsonNode root =
                mapper.readTree(
                        response.body()
                );


        if (root.has("errors")) {

            throw new IOException(
                    "LeetCode GraphQL error: "
                            + root.path("errors")
            );
        }


        return root;
    }

    private String javaTemplate(JsonNode snippets) {
        if (snippets.isArray()) {
            for (JsonNode snippet : snippets) {
                if ("java".equals(snippet.path("langSlug").asText())) {
                    return snippet.path("code").asText();
                }
            }
        }

        return "class Solution {\n    // Java template unavailable\n}";
    }

    private String htmlToText(String html) {
        if (html == null || html.isBlank()) {
            return "Problem description unavailable.";
        }

        String text = Jsoup.parse(html).text();

        return text
                .replaceAll("(?i)Example\\s+(\\d+):", "\n\nExample $1:\n")
                .replaceAll("(?i)Input:", "\nInput:")
                .replaceAll("(?i)Output:", "\nOutput:")
                .replaceAll("(?i)Explanation:", "\nExplanation:")
                .replaceAll("(?i)Constraints:", "\n\nConstraints:\n")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    public LeetCodeProblem problemByNumber(int number)
            throws IOException, InterruptedException {

        String query = """
            query questionList(
                $categorySlug: String,
                $limit: Int,
                $skip: Int,
                $filters: QuestionListFilterInput
            ) {
                problemsetQuestionList: questionList(
                    categorySlug: $categorySlug
                    limit: $limit
                    skip: $skip
                    filters: $filters
                ) {
                    total
                    questions: data {
                        questionFrontendId
                        title
                        titleSlug
                        difficulty
                        paidOnly
                    }
                }
            }
            """;

        ObjectNode variables =
                mapper.createObjectNode();

        variables.put(
                "categorySlug",
                ""
        );

        variables.put(
                "skip",
                0
        );

        variables.put(
                "limit",
                50
        );

        ObjectNode filters =
                mapper.createObjectNode();

        filters.put(
                "searchKeywords",
                String.valueOf(number)
        );

        variables.set(
                "filters",
                filters
        );

        ObjectNode payload =
                mapper.createObjectNode();

        payload.put(
                "query",
                query
        );

        payload.put(
                "operationName",
                "questionList"
        );

        payload.set(
                "variables",
                variables
        );

        JsonNode root =
                postGraphQl(payload);

        JsonNode questions =
                root.path("data")
                        .path("problemsetQuestionList")
                        .path("questions");

        if (!questions.isArray()) {
            throw new RuntimeException(
                    "Invalid response from LeetCode"
            );
        }

        String target =
                String.valueOf(number);

        for (JsonNode question : questions) {

            String frontendId =
                    question.path(
                            "questionFrontendId"
                    ).asText();

            if (!target.equals(frontendId)) {
                continue;
            }

            boolean paidOnly =
                    question.path(
                            "paidOnly"
                    ).asBoolean(false);

            if (paidOnly) {
                throw new RuntimeException(
                        "LeetCode #"
                                + number
                                + " is Premium only."
                );
            }

            String slug =
                    question.path(
                            "titleSlug"
                    ).asText();

            if (slug.isBlank()) {
                throw new RuntimeException(
                        "Problem slug not found"
                );
            }

            return fetchDetails(slug);
        }

        throw new RuntimeException(
                "LeetCode problem #"
                        + number
                        + " not found."
        );
    }

    private static String requireEnv(
            String name
    ) {

        String value =
                System.getenv(name);

        if (value == null
                || value.isBlank()) {

            throw new IllegalStateException(
                    "Missing environment variable: "
                            + name
            );
        }

        return value;
    }
}
