FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY build/libs/billiard-club-0.0.1-SNAPSHOT.jar app.jar
RUN chown spring:spring app.jar

USER spring

EXPOSE 7070

ENTRYPOINT ["java", "-jar", "app.jar"]
