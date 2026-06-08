# 75 Days Challenge Bot 🏆

A Telegram bot for tracking your 75-day self-improvement challenge. Built with Java.

➡️ **Try it:** [@challange_ellkaden_bot](https://t.me/challange_ellkaden_bot)

---

## What it does

- Create up to 7 custom sections (e.g. Java, Sport, University)
- Add tasks inside each section
- Mark tasks as done with inline buttons — no typing needed
- Day is counted only when **all tasks in all sections** are completed
- If you miss a day — the streak resets (no cheating 💪)
- Daily reminder at 8 PM if you haven't completed your tasks
- Progress bar, best streak, and missed days statistics

---

## Demo

```
📁 Java
⬜ Solve 2 LeetCode problems
⬜ Watch 1 lecture

📁 Sport
⬜ 30 min workout

→ tap a button → task marked ✅
→ all done → Day 3/75 counted! 🎉
```

---

## Tech stack

- **Java 17+**
- **TelegramBots** library (v6.7.0)
- **Maven** for dependency management
- **File-based storage** (no database needed)
- **Deployed on Railway**

---

## Run locally

1. Clone the repo
```bash
git clone https://github.com/personnna/habit-tracker-bot.git
cd habit-tracker-bot
```

2. Set environment variables
```bash
export BOT_TOKEN=your_token_here
export BOT_USERNAME=your_bot_username
```

3. Build and run
```bash
mvn package
java -jar target/habit-tracker-bot-1.0-SNAPSHOT-jar-with-dependencies.jar
```

---

## Project structure

```
src/main/java/com/habitbot/
├── Main.java        # Entry point
├── HabitBot.java    # Bot logic, commands, keyboards
├── UserData.java    # User model + state machine
└── Storage.java     # File-based persistence
```

---

## Features roadmap

- [ ] PostgreSQL database instead of file storage
- [ ] Weekly summary stats
- [ ] Custom reminder time
- [ ] Streak freeze (skip 1 day per week)

---

## Author

Made by [@ellkaden](https://github.com/personnna) — iOS Engineer learning Java backend 🚀
