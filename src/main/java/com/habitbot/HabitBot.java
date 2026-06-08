package com.habitbot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.time.LocalTime;

public class HabitBot extends TelegramLongPollingBot {

    private HashMap<Long, UserData> users = Storage.load();
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static final String[] MOTIVATIONS = {
            "💪 Каждый день — это шаг к лучшей версии себя!",
            "🔥 Не сдавайся! Ты уже прошла часть пути!",
            "⭐ Дисциплина — это мост между целями и достижениями!",
            "🚀 Маленькие шаги каждый день = большие результаты!",
            "🌟 Ты сильнее чем думаешь!"
    };

    public HabitBot() {
        startDailyReminder();
    }

    @Override
    public String getBotUsername() {
        return System.getenv("BOT_USERNAME");
    }

    @Override
    public String getBotToken() {
        return System.getenv("BOT_TOKEN");
    }

    private void startDailyReminder() {
        scheduler.scheduleAtFixedRate(() -> {
            LocalTime now = LocalTime.now();
            // Напоминание в 20:00
            if (now.getHour() == 20 && now.getMinute() == 0) {
                for (Long chatId : users.keySet()) {
                    UserData data = users.get(chatId);
                    if (data.sections.isEmpty()) continue;

                    // Проверяем сброс дня
                    if (data.isNewDay() && !data.doneSections.isEmpty()) {
                        data.missedDays++;
                        data.resetDay();
                        data.lastActiveDate = LocalDate.now();
                        Storage.save(users);
                        sendMsg(chatId, "😔 Вчера ты не выполнила все разделы — день не засчитан.\nНо сегодня новый шанс! 💪\n\n" + formatSections(data));
                    } else if (data.doneSections.isEmpty()) {
                        String motivation = MOTIVATIONS[(int)(Math.random() * MOTIVATIONS.length)];
                        sendMsg(chatId, "⏰ Не забудь отметить разделы сегодня!\n\n" + motivation + "\n\n" + formatSections(data));
                    }
                }
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        String text = update.getMessage().getText().trim();
        long chatId = update.getMessage().getChatId();

        // Проверяем сброс дня при каждом сообщении
        UserData data = users.get(chatId);
        if (data != null && data.isNewDay() && !data.doneSections.isEmpty()) {
            data.missedDays++;
            data.resetDay();
            data.lastActiveDate = LocalDate.now();
            Storage.save(users);
            sendMsg(chatId, "😔 Вчера ты не выполнила все разделы — день не засчитан.\nНачинаем новый день! 💪");
        }

        if (text.equals("/start")) {
            users.put(chatId, new UserData(0, new ArrayList<>(), new ArrayList<>()));
            Storage.save(users);
            sendMsg(chatId, """
                    Привет! 👋 Добро пожаловать в 75 days challenge!
                    
                    Сначала создай свои разделы.
                    Напиши /add и название раздела.
                    
                    Например:
                    /add Java
                    /add Магистратура
                    /add Спорт
                    
                    Можно добавить от 1 до 7 разделов.
                    Когда готова — напиши /begin
                    """);

        } else if (text.startsWith("/add ")) {
            String section = text.substring(5).trim();
            data = users.getOrDefault(chatId, new UserData(0, new ArrayList<>(), new ArrayList<>()));

            if (data.sections.size() >= 7) {
                sendMsg(chatId, "Максимум 7 разделов!");
                return;
            }
            if (data.sections.contains(section)) {
                sendMsg(chatId, "Раздел «" + section + "» уже есть!");
                return;
            }

            data.sections.add(section);
            users.put(chatId, data);
            Storage.save(users);
            sendMsg(chatId, "✅ Раздел «" + section + "» добавлен! (" + data.sections.size() + "/7)\n\nДобавь ещё или напиши /begin чтобы начать.");

        } else if (text.equals("/begin")) {
            data = users.get(chatId);
            if (data == null || data.sections.isEmpty()) {
                sendMsg(chatId, "Сначала добавь хотя бы один раздел через /add");
                return;
            }
            String motivation = MOTIVATIONS[(int)(Math.random() * MOTIVATIONS.length)];
            sendMsg(chatId, "🚀 Челлендж начат! Твои разделы:\n" + formatSections(data) + "\n" + motivation);

        } else if (text.startsWith("/done ")) {
            String section = text.substring(6).trim();
            data = users.get(chatId);

            if (data == null || data.sections.isEmpty()) {
                sendMsg(chatId, "Сначала создай разделы через /add");
                return;
            }
            if (!data.sections.contains(section)) {
                sendMsg(chatId, "Раздел «" + section + "» не найден.\nТвои разделы:\n" + formatSections(data));
                return;
            }
            if (data.doneSections.contains(section)) {
                sendMsg(chatId, "Раздел «" + section + "» уже отмечен сегодня!");
                return;
            }

            data.doneSections.add(section);
            data.lastActiveDate = LocalDate.now();

            if (data.allDone()) {
                data.days++;
                if (data.days > data.bestStreak) {
                    data.bestStreak = data.days;
                }
                data.resetDay();
                Storage.save(users);
                String motivation = MOTIVATIONS[(int)(Math.random() * MOTIVATIONS.length)];
                sendMsg(chatId, "🎉 Все разделы выполнены!\nДень " + data.days + " из 75 засчитан!\n\n" + motivation);
            } else {
                Storage.save(users);
                int remaining = data.sections.size() - data.doneSections.size();
                sendMsg(chatId, "✅ «" + section + "» выполнен!\nОсталось разделов: " + remaining + "\n\n" + formatSections(data));
            }

        } else if (text.equals("/status")) {
            data = users.get(chatId);
            if (data == null) {
                sendMsg(chatId, "Сначала напиши /start");
                return;
            }
            String bar = progressBar(data.days);
            sendMsg(chatId, "📊 Твой прогресс:\n" + bar +
                    "\nДень " + data.days + " из 75" +
                    "\nОсталось: " + (75 - data.days) + " дней" +
                    "\n🏆 Лучшая серия: " + data.bestStreak + " дней" +
                    "\n😔 Пропущено дней: " + data.missedDays +
                    "\n\nСегодня:\n" + formatSections(data));

        } else if (text.equals("/help")) {
            sendMsg(chatId, """
                    Команды:
                    /start — начать заново
                    /add название — добавить раздел
                    /begin — начать челлендж
                    /done название — отметить раздел ✅
                    /status — прогресс 📊
                    """);
        } else {
            sendMsg(chatId, "Не понимаю команду. Напиши /help");
        }
    }

    private String formatSections(UserData data) {
        StringBuilder sb = new StringBuilder();
        for (String section : data.sections) {
            if (data.doneSections.contains(section)) {
                sb.append("✅ ").append(section).append("\n");
            } else {
                sb.append("⬜ ").append(section).append("\n");
            }
        }
        return sb.toString();
    }

    private String progressBar(int days) {
        int filled = days * 10 / 75;
        String bar = "▓".repeat(filled) + "░".repeat(10 - filled);
        return "[" + bar + "] " + (days * 100 / 75) + "%";
    }

    private void sendMsg(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}