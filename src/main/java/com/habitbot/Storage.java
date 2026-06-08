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
                writer.println(chatId + "|" + data.days + "|" + sections + "|" + done + "|" + date + "|" + data.bestStreak + "|" + data.missedDays);
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
                String[] parts = line.split("\\|");
                long chatId = Long.parseLong(parts[0]);
                int days = Integer.parseInt(parts[1]);
                List<String> sections = parts.length > 2 && !parts[2].isEmpty()
                        ? new ArrayList<>(Arrays.asList(parts[2].split(",")))
                        : new ArrayList<>();
                List<String> done = parts.length > 3 && !parts[3].isEmpty()
                        ? new ArrayList<>(Arrays.asList(parts[3].split(",")))
                        : new ArrayList<>();
                LocalDate date = parts.length > 4 ? LocalDate.parse(parts[4]) : LocalDate.now();
                int bestStreak = parts.length > 5 ? Integer.parseInt(parts[5]) : 0;
                int missedDays = parts.length > 6 ? Integer.parseInt(parts[6]) : 0;

                UserData userData = new UserData(days, sections, done);
                userData.lastActiveDate = date;
                userData.bestStreak = bestStreak;
                userData.missedDays = missedDays;
                users.put(chatId, userData);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return users;
    }
}