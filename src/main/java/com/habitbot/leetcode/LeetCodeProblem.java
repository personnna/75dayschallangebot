package com.habitbot.leetcode;

public record LeetCodeProblem(
        int frontendId,
        String title,
        String slug,
        String difficulty,
        String description,
        String javaTemplate
) {}
