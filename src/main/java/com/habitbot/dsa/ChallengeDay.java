package com.habitbot.dsa;

import java.util.List;

public record ChallengeDay(
        int day,
        String section,
        String topic,
        List<String> patterns,
        List<String> problems
) {}