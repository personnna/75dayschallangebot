package com.habitbot;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Storage {

    private static Connection getConnection() throws SQLException {
        String url = System.getenv("DATABASE_URL");
        return DriverManager.getConnection(url);
    }

    public static void init() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    chat_id BIGINT PRIMARY KEY,
                    days INT DEFAULT 0,
                    best_streak INT DEFAULT 0,
                    missed_days INT DEFAULT 0,
                    last_active DATE,
                    sections TEXT DEFAULT '',
                    done_sections TEXT DEFAULT '',
                    tasks TEXT DEFAULT '',
                    done_tasks TEXT DEFAULT ''
                )
            """);
            System.out.println("БД инициализирована!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void save(HashMap<Long, UserData> users) {
        try (Connection conn = getConnection()) {
            for (Long chatId : users.keySet()) {
                UserData data = users.get(chatId);
                String sections = String.join(",", data.sections);
                String done = String.join(",", data.doneSections);
                String date = data.lastActiveDate != null ? data.lastActiveDate.toString() : LocalDate.now().toString();
                String tasks = serializeTasks(data.tasks);
                String doneTasks = serializeTasks(data.doneTasks);

                String sql = """
                    INSERT INTO users (chat_id, days, best_streak, missed_days, last_active, sections, done_sections, tasks, done_tasks)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (chat_id) DO UPDATE SET
                        days = EXCLUDED.days,
                        best_streak = EXCLUDED.best_streak,
                        missed_days = EXCLUDED.missed_days,
                        last_active = EXCLUDED.last_active,
                        sections = EXCLUDED.sections,
                        done_sections = EXCLUDED.done_sections,
                        tasks = EXCLUDED.tasks,
                        done_tasks = EXCLUDED.done_tasks
                """;
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setLong(1, chatId);
                    ps.setInt(2, data.days);
                    ps.setInt(3, data.bestStreak);
                    ps.setInt(4, data.missedDays);
                    ps.setDate(5, Date.valueOf(date));
                    ps.setString(6, sections);
                    ps.setString(7, done);
                    ps.setString(8, tasks);
                    ps.setString(9, doneTasks);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static HashMap<Long, UserData> load() {
        HashMap<Long, UserData> users = new HashMap<>();

        // Сначала пробуем загрузить из БД
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM users")) {

            while (rs.next()) {
                long chatId = rs.getLong("chat_id");
                int days = rs.getInt("days");
                int bestStreak = rs.getInt("best_streak");
                int missedDays = rs.getInt("missed_days");
                java.sql.Date date = rs.getDate("last_active");

                List<String> sections = parseList(rs.getString("sections"));
                List<String> doneSections = parseList(rs.getString("done_sections"));
                Map<String, List<String>> tasks = deserializeTasks(rs.getString("tasks"));
                Map<String, List<String>> doneTasks = deserializeTasks(rs.getString("done_tasks"));

                UserData userData = new UserData(days, sections, doneSections);
                userData.bestStreak = bestStreak;
                userData.missedDays = missedDays;
                userData.lastActiveDate = date != null ? date.toLocalDate() : LocalDate.now();
                userData.tasks = tasks;
                userData.doneTasks = doneTasks;
                users.put(chatId, userData);
            }
            System.out.println("Загружено из БД: " + users.size() + " пользователей");

        } catch (SQLException e) {
            System.out.println("БД недоступна, загружаем из файла: " + e.getMessage());
            users = loadFromFile();
        }
        return users;
    }

    // Миграция из файла в БД
    private static HashMap<Long, UserData> loadFromFile() {
        HashMap<Long, UserData> users = new HashMap<>();
        java.io.File file = new java.io.File("users.txt");
        if (!file.exists()) return users;

        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    String[] parts = line.split("~", -1);
                    long chatId = Long.parseLong(parts[0]);
                    int days = Integer.parseInt(parts[1]);
                    List<String> sections = parseList(parts[2]);
                    List<String> doneSections = parseList(parts[3]);
                    LocalDate date = parts[4].isEmpty() ? LocalDate.now() : LocalDate.parse(parts[4]);
                    int bestStreak = Integer.parseInt(parts[5]);
                    int missedDays = Integer.parseInt(parts[6]);
                    Map<String, List<String>> tasks = parts.length > 7 ? deserializeTasks(parts[7]) : new HashMap<>();
                    Map<String, List<String>> doneTasks = parts.length > 8 ? deserializeTasks(parts[8]) : new HashMap<>();

                    UserData userData = new UserData(days, sections, doneSections);
                    userData.bestStreak = bestStreak;
                    userData.missedDays = missedDays;
                    userData.lastActiveDate = date;
                    userData.tasks = tasks;
                    userData.doneTasks = doneTasks;
                    users.put(chatId, userData);
                } catch (Exception e) {
                    System.err.println("Ошибка парсинга: " + line);
                }
            }
            System.out.println("Загружено из файла: " + users.size() + " пользователей");
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return users;
    }

    // ─── Helpers ──────────────────────────────────────────────────
    private static String serializeTasks(Map<String, List<String>> tasks) {
        StringBuilder sb = new StringBuilder();
        for (String section : tasks.keySet()) {
            sb.append(section).append(":").append(String.join(";", tasks.get(section))).append("|");
        }
        return sb.toString();
    }

    private static Map<String, List<String>> deserializeTasks(String raw) {
        Map<String, List<String>> result = new HashMap<>();
        if (raw == null || raw.isEmpty()) return result;
        for (String entry : raw.split("\\|")) {
            if (entry.isEmpty()) continue;
            String[] kv = entry.split(":", 2);
            if (kv.length == 2) {
                List<String> list = kv[1].isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(kv[1].split(";")));
                result.put(kv[0], list);
            }
        }
        return result;
    }

    private static List<String> parseList(String raw) {
        if (raw == null || raw.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(raw.split(",")));
    }
}