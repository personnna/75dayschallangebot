package com.habitbot;

import java.io.*;
import java.time.LocalDate;
import java.util.*;

public class Storage {

    private static final String FILE = "users.txt";

    public static void save(HashMap<Long, UserData> users) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE))) {
            for (Long chatId : users.keySet()) {
                UserData data = users.get(chatId);
                String sections = String.join(",", data.sections);
                String done = String.join(",", data.doneSections);
                String date = data.lastActiveDate != null ? data.lastActiveDate.toString() : LocalDate.now().toString();

                // сохраняем таски: section1:task1;task2|section2:task1;task2
                StringBuilder tasksBuilder = new StringBuilder();
                for (String section : data.sections) {
                    List<String> tasks = data.tasks.getOrDefault(section, new ArrayList<>());
                    tasksBuilder.append(section).append(":").append(String.join(";", tasks)).append("|");
                }

                // сохраняем выполненные таски
                StringBuilder doneTasksBuilder = new StringBuilder();
                for (String section : data.doneTasks.keySet()) {
                    List<String> doneTasks = data.doneTasks.get(section);
                    doneTasksBuilder.append(section).append(":").append(String.join(";", doneTasks)).append("|");
                }

                writer.println(chatId + "~" + data.days + "~" + sections + "~" + done + "~" + date + "~" + data.bestStreak + "~" + data.missedDays + "~" + tasksBuilder + "~" + doneTasksBuilder);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static HashMap<Long, UserData> load() {
        HashMap<Long, UserData> users = new HashMap<>();
        File file = new File(FILE);
        if (!file.exists()) return users;

        try (BufferedReader reader = new BufferedReader(new FileReader(FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    String[] parts = line.split("~", -1);
                    long chatId = Long.parseLong(parts[0]);
                    int days = Integer.parseInt(parts[1]);

                    List<String> sections = parts[2].isEmpty()
                            ? new ArrayList<>()
                            : new ArrayList<>(Arrays.asList(parts[2].split(",")));

                    List<String> doneSections = parts[3].isEmpty()
                            ? new ArrayList<>()
                            : new ArrayList<>(Arrays.asList(parts[3].split(",")));

                    LocalDate date = parts[4].isEmpty() ? LocalDate.now() : LocalDate.parse(parts[4]);
                    int bestStreak = Integer.parseInt(parts[5]);
                    int missedDays = Integer.parseInt(parts[6]);

                    // загружаем таски
                    Map<String, List<String>> tasks = new HashMap<>();
                    if (parts.length > 7 && !parts[7].isEmpty()) {
                        for (String entry : parts[7].split("\\|")) {
                            if (entry.isEmpty()) continue;
                            String[] kv = entry.split(":", 2);
                            if (kv.length == 2) {
                                List<String> taskList = kv[1].isEmpty()
                                        ? new ArrayList<>()
                                        : new ArrayList<>(Arrays.asList(kv[1].split(";")));
                                tasks.put(kv[0], taskList);
                            }
                        }
                    }

                    // загружаем выполненные таски
                    Map<String, List<String>> doneTasks = new HashMap<>();
                    if (parts.length > 8 && !parts[8].isEmpty()) {
                        for (String entry : parts[8].split("\\|")) {
                            if (entry.isEmpty()) continue;
                            String[] kv = entry.split(":", 2);
                            if (kv.length == 2) {
                                List<String> doneList = kv[1].isEmpty()
                                        ? new ArrayList<>()
                                        : new ArrayList<>(Arrays.asList(kv[1].split(";")));
                                doneTasks.put(kv[0], doneList);
                            }
                        }
                    }

                    UserData userData = new UserData(days, sections, doneSections);
                    userData.lastActiveDate = date;
                    userData.bestStreak = bestStreak;
                    userData.missedDays = missedDays;
                    userData.tasks = tasks;
                    userData.doneTasks = doneTasks;
                    users.put(chatId, userData);

                } catch (Exception e) {
                    System.err.println("Ошибка парсинга строки: " + line);
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return users;
    }
}