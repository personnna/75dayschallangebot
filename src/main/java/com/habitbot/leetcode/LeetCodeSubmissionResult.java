package com.habitbot.leetcode;

import com.fasterxml.jackson.databind.JsonNode;

public record LeetCodeSubmissionResult(
        String status,
        String runtime,
        String memory,
        int passed,
        int total,
        String lastTestcase,
        String expectedOutput,
        String codeOutput,
        String compileError,
        String runtimeError,
        long submissionId
) {
    public boolean accepted() {
        return "Accepted".equalsIgnoreCase(status);
    }

    public static LeetCodeSubmissionResult from(JsonNode node, long submissionId) {
        return new LeetCodeSubmissionResult(
                text(node, "status_msg"),
                text(node, "status_runtime"),
                text(node, "status_memory"),
                node.path("total_correct").asInt(0),
                node.path("total_testcases").asInt(0),
                text(node, "last_testcase"),
                text(node, "expected_output"),
                text(node, "code_output"),
                text(node, "compile_error"),
                text(node, "runtime_error"),
                submissionId
        );
    }

    public String toTelegramMessage() {
        if (accepted()) {
            return """
                    ✅ Accepted

                    Tests: %d / %d
                    Runtime: %s
                    Memory: %s

                    Submission: https://leetcode.com/submissions/detail/%d/
                    """.formatted(
                    passed,
                    total,
                    blank(runtime, "N/A"),
                    blank(memory, "N/A"),
                    submissionId
            );
        }

        StringBuilder message = new StringBuilder();
        message.append("❌ ").append(blank(status, "Submission failed")).append("\n\n");

        if (total > 0) {
            message.append("Passed: ").append(passed).append(" / ").append(total).append("\n\n");
        }

        if (!isBlank(compileError)) {
            message.append("Compile error:\n").append(limit(compileError)).append("\n");
        } else if (!isBlank(runtimeError)) {
            message.append("Runtime error:\n").append(limit(runtimeError)).append("\n");
        } else {
            if (!isBlank(lastTestcase)) {
                message.append("Input:\n").append(limit(lastTestcase)).append("\n\n");
            }
            if (!isBlank(expectedOutput)) {
                message.append("Expected:\n").append(limit(expectedOutput)).append("\n\n");
            }
            if (!isBlank(codeOutput)) {
                message.append("Your output:\n").append(limit(codeOutput)).append("\n");
            }
        }

        return message.toString().trim();
    }

    private static String text(JsonNode node, String key) {
        JsonNode value = node.get(key);
        if (value == null || value.isNull()) return null;
        if (value.isArray()) return value.toString();
        return value.asText();
    }

    private static String blank(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank() || "null".equalsIgnoreCase(value);
    }

    private static String limit(String value) {
        if (value == null) return "";
        return value.length() <= 1200 ? value : value.substring(0, 1200) + "\n…";
    }
}
