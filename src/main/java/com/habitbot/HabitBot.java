package com.habitbot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class HabitBot extends TelegramLongPollingBot {

    private static final Logger log = Logger.getLogger(HabitBot.class.getName());
    private HashMap<Long, UserData> users = Storage.load();
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static final String[] MOTIVATIONS = {
            "💪 Каждый день — это шаг к лучшей версии себя!",
            "🔥 Не сдавайся! Ты уже прошла часть пути!",
            "⭐ Дисциплина — это мост между целями и достижениями!",
            "🚀 Маленькие шаги каждый день = большие результаты!",
            "🌟 Ты сильнее чем думаешь!"
    };

    private static final long ADMIN_ID = 1024602209L;

    public HabitBot() {
        startDailyReminder();
        log.info("Бот запущен!");
    }

    @Override
    public String getBotUsername() { return System.getenv("BOT_USERNAME"); }

    @Override
    public String getBotToken() { return System.getenv("BOT_TOKEN"); }

    // ─── Клавиатуры ───────────────────────────────────────────────
    private ReplyKeyboardMarkup mainKeyboard() {
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("✅ Отметить таск"));
        row1.add(new KeyboardButton("📊 Статус"));
        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("➕ Добавить раздел"));
        row2.add(new KeyboardButton("🗑 Удалить раздел"));
        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("❓ Помощь"));
        ReplyKeyboardMarkup kb = new ReplyKeyboardMarkup();
        kb.setKeyboard(List.of(row1, row2, row3));
        kb.setResizeKeyboard(true);
        return kb;
    }

    private ReplyKeyboardMarkup cancelKeyboard() {
        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton("❌ Отмена"));
        ReplyKeyboardMarkup kb = new ReplyKeyboardMarkup();
        kb.setKeyboard(List.of(row));
        kb.setResizeKeyboard(true);
        return kb;
    }

    private ReplyKeyboardMarkup doneAddingKeyboard() {
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("✔️ Готово"));
        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("❌ Отмена"));
        ReplyKeyboardMarkup kb = new ReplyKeyboardMarkup();
        kb.setKeyboard(List.of(row1, row2));
        kb.setResizeKeyboard(true);
        return kb;
    }

    // InlineKeyboard для тасков
    private InlineKeyboardMarkup tasksKeyboard(UserData data) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (String section : data.sections) {
            List<String> sectionTasks = data.tasks.getOrDefault(section, new ArrayList<>());
            if (sectionTasks.isEmpty()) continue;

            // Заголовок раздела как кнопка (не кликабельная)
            InlineKeyboardButton sectionBtn = new InlineKeyboardButton();
            sectionBtn.setText("📁 " + section);
            sectionBtn.setCallbackData("section_" + section);
            rows.add(List.of(sectionBtn));

            // Таски раздела
            List<String> done = data.doneTasks.getOrDefault(section, new ArrayList<>());
            for (String task : sectionTasks) {
                InlineKeyboardButton taskBtn = new InlineKeyboardButton();
                boolean isDone = done.contains(task);
                taskBtn.setText(isDone ? "✅ " + task : "⬜ " + task);
                taskBtn.setCallbackData(isDone ? "already_" + section + "|" + task : "done_" + section + "|" + task);
                rows.add(List.of(taskBtn));
            }
        }
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }

    // ─── Напоминания ──────────────────────────────────────────────
    private void startDailyReminder() {
        scheduler.scheduleAtFixedRate(() -> {
            LocalTime now = LocalTime.now();
            if (now.getHour() == 20 && now.getMinute() == 0) {
                for (Long chatId : users.keySet()) {
                    UserData data = users.get(chatId);
                    if (data.sections.isEmpty()) continue;
                    checkAndResetDay(chatId, data);
                    if (data.doneTasks.isEmpty()) {
                        String motivation = MOTIVATIONS[(int)(Math.random() * MOTIVATIONS.length)];
                        sendMsg(chatId, "⏰ Не забудь отметить таски сегодня!\n\n" + motivation, mainKeyboard());
                    }
                }
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    private void checkAndResetDay(long chatId, UserData data) {
        if (data.isNewDay() && !data.doneTasks.isEmpty()) {
            log.warning("Пользователь " + chatId + " не выполнил все таски вчера");
            data.missedDays++;
            data.resetDay();
            data.lastActiveDate = LocalDate.now();
            Storage.save(users);
            sendMsg(chatId, "😔 Вчера не все таски выполнены — день не засчитан.\nНо сегодня новый шанс! 💪", mainKeyboard());
        }
    }

    // ─── Основная логика ──────────────────────────────────────────
    @Override
    public void onUpdateReceived(Update update) {

        // Обработка нажатий на InlineKeyboard
        if (update.hasCallbackQuery()) {
            handleCallback(update.getCallbackQuery());
            return;
        }

        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        String text = update.getMessage().getText().trim();
        long chatId = update.getMessage().getChatId();
        String username = update.getMessage().getFrom().getUserName();

        log.info("[@" + username + " | " + chatId + "]: " + text);

        UserData data = users.get(chatId);
        if (data != null) checkAndResetDay(chatId, data);

        if (text.equals("❌ Отмена")) {
            if (data != null) {
                data.state = UserData.State.IDLE;
                data.currentSection = null;
            }
            sendMsg(chatId, "Отменено.", mainKeyboard());
            return;
        }

        if (data != null && data.state == UserData.State.WAITING_SECTION_NAME) {
            handleSectionName(chatId, data, text);
            return;
        }

        if (data != null && data.state == UserData.State.WAITING_TASKS) {
            handleTaskInput(chatId, data, text);
            return;
        }

        if (data != null && data.state == UserData.State.WAITING_DELETE_SECTION) {
            handleDeleteSection(chatId, data, text);
            return;
        }

        switch (text) {
            case "/start" -> handleStart(chatId);
            case "➕ Добавить раздел" -> handleAddSectionStart(chatId);
            case "/begin" -> handleBegin(chatId);
            case "✅ Отметить таск" -> handleShowTasks(chatId);
            case "📊 Статус", "/status" -> handleStatus(chatId);
            case "❓ Помощь", "/help" -> handleHelp(chatId);
            case "/admin" -> handleAdmin(chatId);
            case "🗑 Удалить раздел" -> handleDeleteSectionStart(chatId);
            default -> sendMsg(chatId, "Используй кнопки внизу 😊", mainKeyboard());
        }
    }

    // ─── Обработка нажатий InlineKeyboard ────────────────────────
    private void handleCallback(org.telegram.telegrambots.meta.api.objects.CallbackQuery callback) {
        String callbackData = callback.getData();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        UserData data = users.get(chatId);

        // Ответ на callback чтобы убрать часики
        try {
            AnswerCallbackQuery answer = new AnswerCallbackQuery();
            answer.setCallbackQueryId(callback.getId());
            execute(answer);
        } catch (TelegramApiException e) {
            log.severe("Ошибка AnswerCallbackQuery: " + e.getMessage());
        }

        if (callbackData.startsWith("section_")) {
            // нажали на заголовок раздела — игнорируем
            return;
        }

        if (callbackData.startsWith("already_")) {
            // таск уже выполнен
            return;
        }

        if (callbackData.startsWith("done_")) {
            String payload = callbackData.substring(5);
            String[] parts = payload.split("\\|", 2);
            if (parts.length != 2) return;

            String section = parts[0];
            String task = parts[1];

            if (data == null) return;

            List<String> done = data.doneTasks.getOrDefault(section, new ArrayList<>());
            if (!done.contains(task)) {
                done.add(task);
                data.doneTasks.put(section, done);
                data.lastActiveDate = LocalDate.now();
                Storage.save(users);
                log.info("Пользователь " + chatId + " выполнил таск: " + section + " | " + task);
            }

            if (data.allDone()) {
                data.days++;
                if (data.days > data.bestStreak) data.bestStreak = data.days;
                data.resetDay();
                Storage.save(users);
                String motivation = MOTIVATIONS[(int)(Math.random() * MOTIVATIONS.length)];
                // Убираем inline кнопки
                try {
                    EditMessageReplyMarkup edit = new EditMessageReplyMarkup();
                    edit.setChatId(chatId);
                    edit.setMessageId(messageId);
                    edit.setReplyMarkup(new InlineKeyboardMarkup());
                    execute(edit);
                } catch (TelegramApiException e) {
                    log.severe(e.getMessage());
                }
                sendMsg(chatId, "🎉 Все таски выполнены!\nДень " + data.days + " из 75 засчитан!\n\n" + motivation, mainKeyboard());
            } else {
                // Обновляем кнопки — таск теперь показывает ✅
                try {
                    EditMessageReplyMarkup edit = new EditMessageReplyMarkup();
                    edit.setChatId(chatId);
                    edit.setMessageId(messageId);
                    edit.setReplyMarkup(tasksKeyboard(data));
                    execute(edit);
                } catch (TelegramApiException e) {
                    log.severe(e.getMessage());
                }
            }
        }
    }

    // ─── Диалог добавления раздела ────────────────────────────────
    private void handleAddSectionStart(long chatId) {
        UserData data = users.getOrDefault(chatId, new UserData(0, new ArrayList<>(), new ArrayList<>()));
        if (data.sections.size() >= 7) {
            sendMsg(chatId, "Максимум 7 разделов!", mainKeyboard());
            return;
        }
        data.state = UserData.State.WAITING_SECTION_NAME;
        users.put(chatId, data);
        sendMsg(chatId, "Введи название раздела:\n(например: Работа, Спорт, Магистратура)", cancelKeyboard());
    }

    private void handleSectionName(long chatId, UserData data, String text) {
        if (data.sections.contains(text)) {
            sendMsg(chatId, "Раздел «" + text + "» уже есть! Введи другое название:", cancelKeyboard());
            return;
        }
        data.currentSection = text;
        data.sections.add(text);
        data.tasks.put(text, new ArrayList<>());
        data.state = UserData.State.WAITING_TASKS;
        Storage.save(users);
        sendMsg(chatId, "✅ Раздел «" + text + "» создан!\n\nТеперь добавь таски.\nВводи по одному и нажимай отправить.\n\nКогда закончишь — нажми ✔️ Готово", doneAddingKeyboard());
    }

    private void handleTaskInput(long chatId, UserData data, String text) {
        if (text.equals("✔️ Готово")) {
            List<String> sectionTasks = data.tasks.get(data.currentSection);
            if (sectionTasks == null || sectionTasks.isEmpty()) {
                sendMsg(chatId, "Добавь хотя бы один таск!", doneAddingKeyboard());
                return;
            }
            data.state = UserData.State.IDLE;
            String section = data.currentSection;
            data.currentSection = null;
            Storage.save(users);
            sendMsg(chatId, "🎉 Раздел «" + section + "» готов!\n\nДобавь ещё раздел или напиши /begin!", mainKeyboard());
            return;
        }
        List<String> sectionTasks = data.tasks.get(data.currentSection);
        if (sectionTasks.contains(text)) {
            sendMsg(chatId, "Такой таск уже есть! Введи другой:", doneAddingKeyboard());
            return;
        }
        sectionTasks.add(text);
        Storage.save(users);
        sendMsg(chatId, "✅ «" + text + "» добавлен! Добавь ещё или нажми ✔️ Готово", doneAddingKeyboard());
    }

    // ─── Показать таски ───────────────────────────────────────────
    private void handleShowTasks(long chatId) {
        UserData data = users.get(chatId);
        if (data == null || data.sections.isEmpty()) {
            sendMsg(chatId, "Сначала добавь разделы!", mainKeyboard());
            return;
        }

        // дебаг
        log.info("Sections: " + data.sections);
        log.info("Tasks: " + data.tasks);

        InlineKeyboardMarkup keyboard = tasksKeyboard(data);
        log.info("Keyboard rows: " + keyboard.getKeyboard().size());

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));  // ← вот это! setChatId принимает String
        message.setText("Отмечай выполненные таски 👇");
        message.setReplyMarkup(keyboard);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.severe(e.getMessage());
        }
    }

    // ─── Остальные handlers ───────────────────────────────────────
    private void handleStart(long chatId) {
        users.put(chatId, new UserData(0, new ArrayList<>(), new ArrayList<>()));
        Storage.save(users);
        log.info("Новый пользователь: " + chatId);
        sendMsg(chatId, """
                Привет! 👋 Добро пожаловать в 75 days challenge!
                
                Нажми ➕ Добавить раздел чтобы начать.
                Можно добавить от 1 до 7 разделов.
                В каждом разделе свои таски.
                
                Когда добавишь всё — напиши /begin 🚀
                """, mainKeyboard());
    }

    private void handleBegin(long chatId) {
        UserData data = users.get(chatId);
        if (data == null || data.sections.isEmpty()) {
            sendMsg(chatId, "Сначала добавь хотя бы один раздел!", mainKeyboard());
            return;
        }
        String motivation = MOTIVATIONS[(int)(Math.random() * MOTIVATIONS.length)];
        sendMsg(chatId, "🚀 Челлендж начат!\n\n" + formatSections(data) + "\n" + motivation, mainKeyboard());
    }

    private void handleStatus(long chatId) {
        UserData data = users.get(chatId);
        if (data == null) {
            sendMsg(chatId, "Сначала напиши /start", mainKeyboard());
            return;
        }
        String bar = progressBar(data.days);
        sendMsg(chatId, "📊 Твой прогресс:\n" + bar +
                "\nДень " + data.days + " из 75" +
                "\nОсталось: " + (75 - data.days) + " дней" +
                "\n🏆 Лучшая серия: " + data.bestStreak + " дней" +
                "\n😔 Пропущено: " + data.missedDays + " дней" +
                "\n\n" + formatSections(data), mainKeyboard());
    }

    private void handleHelp(long chatId) {
        sendMsg(chatId, """
                ❓ Как пользоваться:
                
                1. Нажми ➕ Добавить раздел
                2. Введи название (Работа, Спорт...)
                3. Добавь таски для раздела
                4. Напиши /begin чтобы начать
                5. Каждый день нажимай ✅ Отметить таск
                6. Отмечай кнопками — день засчитан когда все ✅
                """, mainKeyboard());
    }

    // ─── Helpers ──────────────────────────────────────────────────
    private String formatSections(UserData data) {
        StringBuilder sb = new StringBuilder();
        for (String section : data.sections) {
            List<String> sectionTasks = data.tasks.getOrDefault(section, new ArrayList<>());
            List<String> done = data.doneTasks.getOrDefault(section, new ArrayList<>());
            boolean allDone = !sectionTasks.isEmpty() && done.containsAll(sectionTasks);
            sb.append(allDone ? "✅ " : "⬜ ").append(section)
                    .append(" (").append(done.size()).append("/").append(sectionTasks.size()).append(")\n");
        }
        return sb.toString();
    }

    private String progressBar(int days) {
        int filled = days * 10 / 75;
        String bar = "▓".repeat(filled) + "░".repeat(10 - filled);
        return "[" + bar + "] " + (days * 100 / 75) + "%";
    }

    private void sendMsg(long chatId, String text, ReplyKeyboardMarkup keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setReplyMarkup(keyboard);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.severe("Ошибка: " + e.getMessage());
        }
    }

    private void handleAdmin(long chatId) {
        if (chatId != ADMIN_ID) {
            sendMsg(chatId, "Нет доступа.", mainKeyboard());
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("👑 Admin панель\n\n");
        sb.append("👥 Всего пользователей: ").append(users.size()).append("\n\n");

        for (Long id : users.keySet()) {
            UserData data = users.get(id);
            sb.append("🔹 ID: ").append(id).append("\n");
            sb.append("   День: ").append(data.days).append("/75\n");
            sb.append("   Разделов: ").append(data.sections.size()).append("\n");
            sb.append("   Пропущено: ").append(data.missedDays).append("\n\n");
        }
        sendMsg(chatId, sb.toString(), mainKeyboard());
    }

    private void handleDeleteSectionStart(long chatId) {
        UserData data = users.get(chatId);
        if (data == null || data.sections.isEmpty()) {
            sendMsg(chatId, "Нет разделов для удаления!", mainKeyboard());
            return;
        }
        data.state = UserData.State.WAITING_DELETE_SECTION;
        Storage.save(users);
        StringBuilder sb = new StringBuilder("Какой раздел удалить? Нажми:\n\n");
        for (String section : data.sections) {
            sb.append("• ").append(section).append("\n");
        }
        sb.append("\nВведи название раздела точно как написано выше.");
        sendMsg(chatId, sb.toString(), cancelKeyboard());
    }

    private void handleDeleteSection(long chatId, UserData data, String text) {
        if (!data.sections.contains(text)) {
            sendMsg(chatId, "Раздел «" + text + "» не найден!\nПопробуй ещё раз или нажми ❌ Отмена", cancelKeyboard());
            return;
        }
        data.sections.remove(text);
        data.tasks.remove(text);
        data.doneTasks.remove(text);
        data.state = UserData.State.IDLE;
        Storage.save(users);
        log.info("Пользователь " + chatId + " удалил раздел: " + text);
        sendMsg(chatId, "🗑 Раздел «" + text + "» удалён!\n\n" + formatSections(data), mainKeyboard());
    }
}