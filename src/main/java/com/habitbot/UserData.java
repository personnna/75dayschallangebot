package com.habitbot;

import java.time.LocalDate;
import java.util.*;

public class UserData {
    public int days;
    public int bestStreak;
    public int missedDays;
    public List<String> sections;
    public Map<String, List<String>> tasks; // раздел → список тасков
    public Map<String, List<String>> doneTasks; // раздел → выполненные таски сегодня
    public List<String> doneSections;
    public LocalDate lastActiveDate;

    // Состояния диалога
    public enum State {
        IDLE,
        WAITING_SECTION_NAME,
        WAITING_TASKS
    }

    public State state = State.IDLE;
    public String currentSection; // раздел который сейчас добавляем

    public UserData(int days, List<String> sections, List<String> doneSections) {
        this.days = days;
        this.sections = sections;
        this.doneSections = doneSections;
        this.tasks = new HashMap<>();
        this.doneTasks = new HashMap<>();
        this.lastActiveDate = LocalDate.now();
        this.bestStreak = 0;
        this.missedDays = 0;
        this.state = State.IDLE;
    }

    public boolean allDone() {
        if (sections.isEmpty()) return false;
        for (String section : sections) {
            List<String> sectionTasks = tasks.get(section);
            List<String> done = doneTasks.getOrDefault(section, new ArrayList<>());
            if (sectionTasks != null && !sectionTasks.isEmpty()) {
                if (!done.containsAll(sectionTasks)) return false;
            } else {
                if (!doneSections.contains(section)) return false;
            }
        }
        return true;
    }

    public void resetDay() {
        doneSections = new ArrayList<>();
        doneTasks = new HashMap<>();
    }

    public boolean isNewDay() {
        return lastActiveDate != null && LocalDate.now().isAfter(lastActiveDate);
    }
}