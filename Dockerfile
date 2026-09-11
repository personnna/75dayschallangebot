FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY habit-tracker-bot-1.0-SNAPSHOT.jar app.jar

CMD ["java", "-jar", "habit-tracker-bot-1.0-SNAPSHOT.jar"]