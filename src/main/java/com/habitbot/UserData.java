package com.habitbot;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class UserData {
    public int days;
    public int bestStreak;
    public int missedDays;
    public List<String> sections;
    public List<String> doneSections;
    public LocalDate lastActiveDate;

    public UserData(int days, List<String> sections, List<String> doneSections) {
        this.days = days;
        this.sections = sections;
        this.doneSections = doneSections;
        this.lastActiveDate = LocalDate.now();
        this.bestStreak = 0;
        this.missedDays = 0;
    }

    public boolean allDone() {
        return !sections.isEmpty() && doneSections.containsAll(sections);
    }

    public void resetDay() {
        doneSections = new ArrayList<>();
    }

    // Новый день наступил?
    public boolean isNewDay() {
        return lastActiveDate != null && LocalDate.now().isAfter(lastActiveDate);
    }
}