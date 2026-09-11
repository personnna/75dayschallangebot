package com.habitbot;

import com.habitbot.dsa.*;
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
import com.habitbot.leetcode.LeetCodeClient;
import com.habitbot.leetcode.LeetCodeSubmissionResult;
import com.habitbot.leetcode.LeetCodeProblem;
import com.habitbot.leetcode.LeetCodeProblemService;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import com.habitbot.dsa.ChallengeDay;
import com.habitbot.dsa.ChallengeCatalog;
import com.habitbot.dsa.Difficulty;

import com.habitbot.leetcode.LeetCodeProblem;

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
    private LeetCodeClient leetCodeClient;
    private final LeetCodeProblemService problemService =
            new LeetCodeProblemService();

    private static final String[] MOTIVATIONS = {
            "💪 Каждый день — это шаг к лучшей версии себя!",
            "🔥 Не сдавайся! Ты уже прошла часть пути!",
            "⭐ Дисциплина — это мост между целями и достижениями!",
            "🚀 Маленькие шаги каждый день = большие результаты!",
            "🌟 Ты сильнее чем думаешь!"
    };

    private LeetCodeClient leetCode() {
        if (leetCodeClient == null) {
            leetCodeClient = new LeetCodeClient();
        }

        return leetCodeClient;
    }

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
        row1.add(new KeyboardButton("🔥 DSA 75 Challenge"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("🎲 Random Problem"));
        row2.add(new KeyboardButton("📊 Progress"));

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();

        keyboard.setKeyboard(List.of(
                row1,
                row2
        ));

        keyboard.setResizeKeyboard(true);

        return keyboard;
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

        if (data != null
                && data.state
                == UserData.State.WAITING_REVIEW) {

            data.challengeReviewText = text;

            data.challengeReviewDone = true;

            data.state =
                    UserData.State.IDLE;

            Storage.save(users);

            sendMsg(
                    chatId,
                    "✅ Review saved.",
                    mainKeyboard()
            );

            handleChallenge(
                    chatId,
                    data
            );

            return;
        }

        if (data != null
                && data.state
                == UserData.State.WAITING_INSIGHT) {

            data.challengeInsightText =
                    text;

            data.challengeInsightDone =
                    true;

            data.state =
                    UserData.State.IDLE;

            Storage.save(users);

            sendMsg(
                    chatId,
                    "✅ Insight saved.",
                    mainKeyboard()
            );

            handleChallenge(
                    chatId,
                    data
            );

            return;
        }

        if (data != null
                && text.matches("\\d+")
                && data.state != UserData.State.SOLVING
                && data.state != UserData.State.WAITING_FOR_COMPLEXITY
                && data.state != UserData.State.WAITING_REVIEW
                && data.state != UserData.State.WAITING_INSIGHT) {

            try {

                int problemNumber =
                        Integer.parseInt(text);

                handleProblemNumber(
                        chatId,
                        data,
                        problemNumber
                );

            } catch (NumberFormatException e) {

                sendMsg(
                        chatId,
                        "Invalid problem number.",
                        mainKeyboard()
                );
            }

            return;
        }

        switch (text) {

            case "/start" -> handleStart(chatId);

            case "🔥 DSA 75 Challenge", "/challenge" ->
                    handleChallenge(chatId, data);

            case "🎲 Random Problem", "/random" ->
                    handleRandom(chatId, data);

            case "📊 Progress", "/progress" ->
                    handleStatus(chatId);

            default -> {

                if (data != null
                        && data.state
                        == UserData.State.SOLVING) {

                    String code =
                            extractCodeFromTelegram(
                                    update.getMessage()
                            );

                    handleCodeSubmission(
                            chatId,
                            data,
                            code
                    );

                } else {

                    sendMsg(
                            chatId,
                            "Choose an option below.",
                            mainKeyboard()
                    );
                }
            }
        }
    }

    // ─── Обработка нажатий InlineKeyboard ────────────────────────
    private void handleCallback(
            org.telegram.telegrambots.meta.api.objects.CallbackQuery callback
    ) {

        String callbackData = callback.getData();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();

        UserData data = users.get(chatId);

        try {
            AnswerCallbackQuery answer = new AnswerCallbackQuery();
            answer.setCallbackQueryId(callback.getId());
            execute(answer);
        } catch (TelegramApiException e) {
            log.severe("Ошибка AnswerCallbackQuery: " + e.getMessage());
        }

        if (callbackData.startsWith("challenge_complete_")) {

            if (data == null) {
                return;
            }

            int completedDay = Integer.parseInt(
                    callbackData.substring(
                            "challenge_complete_".length()
                    )
            );

            if (completedDay != data.challengeDay) {
                return;
            }

            if (data.challengeDay < 75) {

                data.challengeDay++;

                Storage.save(users);

                sendMsg(
                        chatId,
                        "✅ Day " + completedDay
                                + " completed!",
                        mainKeyboard()
                );

                handleChallenge(chatId, data);

            } else {

                sendMsg(
                        chatId,
                        "🏆 You completed DSA 75!",
                        mainKeyboard()
                );
            }

            return;
        }

        if (callbackData.equals(
                "challenge_locked"
        )) {

            sendMsg(
                    chatId,
                    """
                    🔒 Complete all four blocks first:
        
                    📚 Learn
                    💻 Practice
                    🔍 Review
                    🧠 Insight
                    """,
                    mainKeyboard()
            );

            return;
        }

        if (callbackData.equals(
                "challenge_practice"
        )) {

            if (!data.challengeConceptDone) {

                sendMsg(
                        chatId,
                        """
                        📚 Finish today's learning block first.
        
                        Learn the concept before starting practice.
                        """,
                        mainKeyboard()
                );

                return;
            }

            data.practiceMode =
                    UserData.PracticeMode.CHALLENGE;

            Storage.save(users);

            startChallengeProblem(
                    chatId,
                    data
            );

            return;
        }

        // RANDOM
        if (callbackData.startsWith("random_")) {

            if (data == null) {
                return;
            }

            String value =
                    callbackData.substring(
                            "random_".length()
                    );

            Difficulty difficulty =
                    Difficulty.valueOf(
                            value.toUpperCase()
                    );

            try {

                LeetCodeProblem problem =
                        problemService.randomProblem(
                                difficulty,
                                data.lastRandomProblemSlug
                        );

                data.currentProblemSlug =
                        problem.slug();

                data.lastRandomProblemSlug =
                        problem.slug();

                data.state =
                        UserData.State.SOLVING;

                Storage.save(users);

                String text = """
                🎲 RANDOM PROBLEM

                %d. %s
                Difficulty: %s

                %s

                ✍️ Java starter code

                %s

                Send your solution as the next Telegram message.
                """.formatted(
                        problem.frontendId(),
                        problem.title(),
                        problem.difficulty(),
                        problem.description(),
                        problem.javaTemplate()
                );

                sendLongMsg(
                        chatId,
                        text
                );

            } catch (Exception e) {

                e.printStackTrace();

                sendMsg(
                        chatId,
                        "❌ Could not load a LeetCode problem:\n"
                                + e.getMessage(),
                        mainKeyboard()
                );
            }

            return;
        }

        if (callbackData.equals(
                "challenge_learn_done"
        )) {

            data.challengeConceptDone = true;

            Storage.save(users);

            sendMsg(
                    chatId,
                    "✅ Learning block completed.",
                    mainKeyboard()
            );

            handleChallenge(
                    chatId,
                    data
            );

            return;
        }

        if (callbackData.equals(
                "challenge_review"
        )) {

            data.state =
                    UserData.State.WAITING_REVIEW;

            Storage.save(users);

            sendMsg(
                    chatId,
                    """
                    🔍 REVIEW
        
                    Think about today's practice.
        
                    What went wrong?
                    What could you improve?
                    Was there a better approach?
        
                    Send your review as one message.
                    """,
                    mainKeyboard()
            );

            return;
        }

        if (callbackData.equals(
                "challenge_insight"
        )) {

            data.state =
                    UserData.State.WAITING_INSIGHT;

            Storage.save(users);

            sendMsg(
                    chatId,
                    """
                    🧠 DAILY INSIGHT
        
                    Record the most important thing
                    you learned today.
        
                    Good examples:
        
                    Pattern: Sliding Window
                    Mistake: moved left pointer too late
                    Insight: maintain the invariant before expanding
                    """,
                    mainKeyboard()
            );

            return;
        }

        if (callbackData.equals(
                "challenge_complete"
        )) {

            if (!data.canCompleteChallengeDay()) {

                return;
            }

            int finishedDay =
                    data.challengeDay;

            if (finishedDay == 75) {

                sendMsg(
                        chatId,
                        """
                        🏆 DSA 75 COMPLETE
        
                        75 / 75 days completed.
        
                        You finished the entire roadmap.
                        """,
                        mainKeyboard()
                );

                return;
            }

            data.challengeDay++;

            data.resetChallengeDayProgress();

            Storage.save(users);

            sendMsg(
                    chatId,
                    """
                    ✅ DAY %d COMPLETE
        
                    Next up:
        
                    DAY %d / 75
                    """.formatted(
                            finishedDay,
                            data.challengeDay
                    ),
                    mainKeyboard()
            );

            handleChallenge(
                    chatId,
                    data
            );

            return;
        }

        if (callbackData.equals(
                "challenge_learn"
        )) {

            ChallengeDay day =
                    ChallengeCatalog.getDay(
                            data.challengeDay
                    );

            String patterns =
                    day.patterns().isEmpty()
                            ? "No specific patterns today."
                            : "• "
                            + String.join(
                            "\n• ",
                            day.patterns()
                    );

            InlineKeyboardButton done =
                    new InlineKeyboardButton();

            done.setText(
                    "✅ I've learned this"
            );

            done.setCallbackData(
                    "challenge_learn_done"
            );

            InlineKeyboardMarkup keyboard =
                    new InlineKeyboardMarkup();

            keyboard.setKeyboard(
                    List.of(
                            List.of(done)
                    )
            );

            sendInlineMsg(
                    chatId,
                    """
                    📚 STUDY BLOCK
        
                    Topic:
                    %s
        
                    Focus on:
        
                    %s
        
                    Recommended time:
                    45 minutes
        
                    Don't memorize code.
                    Understand when and why the pattern works.
                    """.formatted(
                            day.topic(),
                            patterns
                    ),
                    keyboard
            );

            return;
        }

        if (callbackData.startsWith("section_")) {
            return;
        }

        if (callbackData.startsWith("already_")) {
            return;
        }

        if (callbackData.startsWith("done_")) {

            String payload = callbackData.substring(5);
            String[] parts = payload.split("\\|", 2);

            if (parts.length != 2) {
                return;
            }

            String section = parts[0];
            String task = parts[1];

            if (data == null) {
                return;
            }

            List<String> done =
                    data.doneTasks.getOrDefault(
                            section,
                            new ArrayList<>()
                    );

            if (!done.contains(task)) {
                done.add(task);
                data.doneTasks.put(section, done);
                data.lastActiveDate = LocalDate.now();

                Storage.save(users);
            }

            if (data.allDone()) {

                data.days++;

                if (data.days > data.bestStreak) {
                    data.bestStreak = data.days;
                }

                data.resetDay();
                Storage.save(users);

                try {
                    EditMessageReplyMarkup edit =
                            new EditMessageReplyMarkup();

                    edit.setChatId(chatId);
                    edit.setMessageId(messageId);
                    edit.setReplyMarkup(
                            new InlineKeyboardMarkup()
                    );

                    execute(edit);

                } catch (TelegramApiException e) {
                    log.severe(e.getMessage());
                }

                sendMsg(
                        chatId,
                        "🎉 Все таски выполнены!",
                        mainKeyboard()
                );

            } else {

                try {
                    EditMessageReplyMarkup edit =
                            new EditMessageReplyMarkup();

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
        UserData data = users.get(chatId);

        if (data == null) {

            data = new UserData(
                    0,
                    new ArrayList<>(),
                    new ArrayList<>()
            );

            data.challengeDay = 1;

            users.put(chatId, data);
            Storage.save(users);
        }
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

    private void handleProblemNumber(
            long chatId,
            UserData data,
            int number
    ) {

        try {

            sendMsg(
                    chatId,
                    "🔎 Loading LeetCode #" + number + "...",
                    mainKeyboard()
            );

            LeetCodeProblem problem =
                    problemService.problemByNumber(
                            number
                    );

            data.currentProblemSlug =
                    problem.slug();

            data.practiceMode =
                    UserData.PracticeMode.NONE;

            data.state =
                    UserData.State.SOLVING;

            Storage.save(users);

            String header = """
                💻 LEETCODE #%d

                %s
                Difficulty: %s

                """.formatted(
                    problem.frontendId(),
                    problem.title(),
                    problem.difficulty()
            );

            sendLongMsg(
                    chatId,
                    header
                            + problem.description()
            );

            sendLongMsg(
                    chatId,
                    """
                    ✍️ Java starter code
    
                    %s
    
                    Send your solution as the next message.
                    """.formatted(
                            problem.javaTemplate()
                    )
            );

        }  catch (Exception e) {

            e.printStackTrace();

            sendMsg(
                    chatId,
                    """
                            ❌ Couldn't load LeetCode #%d
                            
                            Error:
                            %s
                            """.formatted(
                            number,
                            e.getMessage()
                    ),
                    mainKeyboard()
            );
        }
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

    private void handleChallenge(
            long chatId,
            UserData data
    ) {

        if (data == null) {
            return;
        }

        ChallengeDay day =
                ChallengeCatalog.getDay(
                        data.challengeDay
                );

        String patterns =
                day.patterns().isEmpty()
                        ? "—"
                        : "• "
                        + String.join(
                        "\n• ",
                        day.patterns()
                );

        String learn =
                data.challengeConceptDone
                        ? "✅"
                        : "⬜";

        String practice =
                data.challengeProblemsSolved >= 2
                        ? "✅"
                        : "⬜";

        String review =
                data.challengeReviewDone
                        ? "✅"
                        : "⬜";

        String insight =
                data.challengeInsightDone
                        ? "✅"
                        : "⬜";

        int completed = 0;

        if (data.challengeConceptDone) completed++;
        if (data.challengeProblemsSolved >= 2) completed++;
        if (data.challengeReviewDone) completed++;
        if (data.challengeInsightDone) completed++;


        String text = """
            🔥 DAY %d / 75

            %s

            📚 Today's topic:
            %s

            🧠 Patterns:
            %s

            ─────────────

            Today's progress: %d / 4

            %s Learn concept
            %s Practice %d / 2
            %s Review mistakes
            %s Save insight

            Daily system:
            45 min — learn
            90 min — practice
            30 min — review
            15 min — reflect
            """.formatted(
                day.day(),
                day.section(),
                day.topic(),
                patterns,
                completed,
                learn,
                practice,
                data.challengeProblemsSolved,
                review,
                insight
        );

        sendInlineMsg(
                chatId,
                text,
                challengeKeyboard(data)
        );
    }

    private InlineKeyboardMarkup difficultyKeyboard() {

        InlineKeyboardButton easy = new InlineKeyboardButton();
        easy.setText("Easy");
        easy.setCallbackData("random_easy");

        InlineKeyboardButton medium = new InlineKeyboardButton();
        medium.setText("Medium");
        medium.setCallbackData("random_medium");

        InlineKeyboardButton hard = new InlineKeyboardButton();
        hard.setText("Hard");
        hard.setCallbackData("random_hard");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        keyboard.setKeyboard(List.of(
                List.of(easy, medium, hard)
        ));

        return keyboard;
    }

    private void handleRandom(long chatId, UserData data) {

        data.state =
                UserData.State.CHOOSING_RANDOM_DIFFICULTY;

        SendMessage message = new SendMessage();

        message.setChatId(chatId);
        message.setText("🎲 Choose difficulty:");
        message.setReplyMarkup(difficultyKeyboard());

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.severe(e.getMessage());
        }
    }

    private void startChallengeProblem(
            long chatId,
            UserData data
    ) {

        ChallengeDay day =
                ChallengeCatalog.getDay(
                        data.challengeDay
                );

        Difficulty difficulty =
                challengeDifficultyForDay(
                        data.challengeDay
                );

        try {

            LeetCodeProblem problem =
                    problemService.randomProblem(
                            difficulty,
                            data.currentProblemSlug
                    );

            data.currentProblemSlug =
                    problem.slug();

            data.practiceMode =
                    UserData.PracticeMode.CHALLENGE;

            data.state =
                    UserData.State.SOLVING;

            Storage.save(users);

            String header = """
                💻 DSA 75 PRACTICE

                Day %d / 75
                Topic: %s

                %d. %s
                Difficulty: %s

                """.formatted(
                    data.challengeDay,
                    day.topic(),
                    problem.frontendId(),
                    problem.title(),
                    problem.difficulty()
            );

            sendLongMsg(
                    chatId,
                    header
                            + problem.description()
            );

            sendLongMsg(
                    chatId,
                    """
                    ✍️ Java starter code
    
                    %s
    
                    Send your solution as the next message.
                    """.formatted(
                            problem.javaTemplate()
                    )
            );

        } catch (Exception e) {

            e.printStackTrace();

            data.state =
                    UserData.State.IDLE;

            Storage.save(users);

            sendMsg(
                    chatId,
                    """
                    ❌ Could not load a practice problem.
    
                    Try again.
                    """,
                    mainKeyboard()
            );
        }
    }

    private Difficulty challengeDifficultyForDay(
            int day
    ) {

        if (day <= 12) {
            return Difficulty.EASY;
        }

        if (day <= 60) {
            return Difficulty.MEDIUM;
        }

        return Difficulty.HARD;
    }



    private void handleCodeSubmission(
            long chatId,
            UserData data,
            String code
    ) {
        System.out.println(
                "========== CODE SENT TO LEETCODE =========="
        );

        System.out.println(code);

        System.out.println(
                "============================================"
        );

        try {

            LeetCodeSubmissionResult result =
                    leetCode().submitAndWait(
                            data.currentProblemSlug,
                            code,
                            "java"
                    );

            sendMsg(
                    chatId,
                    result.toTelegramMessage(),
                    mainKeyboard()
            );

            if (result.accepted()) {
                if (data.practiceMode == UserData.PracticeMode.CHALLENGE) {
                    if (data.challengeProblemsSolved < 2) {
                        data.challengeProblemsSolved++;
                    }
                } data.state = UserData.State.WAITING_FOR_COMPLEXITY;

                Storage.save(users);

                sendMsg(
                        chatId,
                        """
                        Before we move on:
    
                        What is the time complexity?
                        What is the space complexity?
    
                        Reply like:
    
                        Time: O(n)
                        Space: O(n)
                        """,
                        mainKeyboard()
                );
            } else {

                data.state =
                        UserData.State.SOLVING;

                Storage.save(users);
            }

        } catch (IllegalStateException e) {

            e.printStackTrace();

            sendMsg(
                    chatId,
                    """
                    ⚠️ LeetCode judge is not connected yet.
        
                    Your solution was received, but I couldn't submit it for testing.
        
                    Try again after the LeetCode connection is configured.
                    """,
                    mainKeyboard()
            );

        } catch (Exception e) {

            e.printStackTrace();

            sendMsg(
                    chatId,
                    """
                    ❌ Something went wrong while checking your solution.
        
                    Your code was not lost.
                    Please try submitting it again.
                    """,
                    mainKeyboard()
            );
        }
    }

    private InlineKeyboardMarkup challengeKeyboard(
            UserData data
    ) {

        List<List<InlineKeyboardButton>> rows =
                new ArrayList<>();


        // LEARN
        InlineKeyboardButton learn =
                new InlineKeyboardButton();

        learn.setText(
                data.challengeConceptDone
                        ? "✅ Concept learned"
                        : "📚 Learn concept"
        );

        learn.setCallbackData("challenge_learn");

        rows.add(List.of(learn));


        // PRACTICE
        InlineKeyboardButton practice =
                new InlineKeyboardButton();

        practice.setText(
                data.challengeProblemsSolved >= 2
                        ? "✅ Practice 2/2"
                        : "💻 Practice "
                        + data.challengeProblemsSolved
                        + "/2"
        );

        practice.setCallbackData("challenge_practice");

        rows.add(List.of(practice));


        // REVIEW
        InlineKeyboardButton review =
                new InlineKeyboardButton();

        review.setText(
                data.challengeReviewDone
                        ? "✅ Review completed"
                        : "🔍 Review mistakes"
        );

        review.setCallbackData("challenge_review");

        rows.add(List.of(review));


        // INSIGHT
        InlineKeyboardButton insight =
                new InlineKeyboardButton();

        insight.setText(
                data.challengeInsightDone
                        ? "✅ Insight saved"
                        : "🧠 Save insight"
        );

        insight.setCallbackData("challenge_insight");

        rows.add(List.of(insight));


        // COMPLETE
        InlineKeyboardButton complete =
                new InlineKeyboardButton();

        if (data.canCompleteChallengeDay()) {

            complete.setText(
                    "✅ Complete Day "
                            + data.challengeDay
            );

            complete.setCallbackData(
                    "challenge_complete"
            );

        } else {

            complete.setText(
                    "🔒 Complete Day"
            );

            complete.setCallbackData(
                    "challenge_locked"
            );
        }

        rows.add(List.of(complete));


        InlineKeyboardMarkup keyboard =
                new InlineKeyboardMarkup();

        keyboard.setKeyboard(rows);

        return keyboard;
    }

    private void sendInlineMsg(
            long chatId,
            String text,
            InlineKeyboardMarkup keyboard
    ) {

        SendMessage message = new SendMessage();

        message.setChatId(chatId);
        message.setText(text);
        message.setReplyMarkup(keyboard);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendLongMsg(
            long chatId,
            String text
    ) {

        final int max = 3800;

        int start = 0;

        while (start < text.length()) {

            int end =
                    Math.min(
                            start + max,
                            text.length()
                    );

            if (end < text.length()) {

                int newline =
                        text.lastIndexOf(
                                '\n',
                                end
                        );

                if (newline > start) {
                    end = newline;
                }
            }

            String chunk =
                    text.substring(
                            start,
                            end
                    );

            sendMsg(
                    chatId,
                    chunk,
                    mainKeyboard()
            );

            start = end;

            while (
                    start < text.length()
                            && text.charAt(start) == '\n'
            ) {
                start++;
            }
        }
    }

    private String extractCodeFromTelegram(Message message) {

        String text = message.getText();

        if (text == null) {
            return "";
        }

        List<MessageEntity> entities =
                message.getEntities();

        if (entities == null || entities.isEmpty()) {
            return text.trim();
        }

        StringBuilder result =
                new StringBuilder(text);

        entities.stream()
                .filter(entity ->
                        "spoiler".equals(entity.getType())
                )
                .sorted(
                        Comparator.comparingInt(
                                MessageEntity::getOffset
                        ).reversed()
                )
                .forEach(entity -> {

                    int start =
                            entity.getOffset();

                    int end =
                            start + entity.getLength();

                    if (start >= 0
                            && end <= result.length()) {

                        // Telegram removed || around the spoiler.
                        // Restore them as Java boolean OR operators.
                        result.insert(end, "||");
                        result.insert(start, "||");
                    }
                });

        return result.toString().trim();
    }
}