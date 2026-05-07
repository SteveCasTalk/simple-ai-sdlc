FROM gradle:8.10-jdk17 AS build
WORKDIR /app
COPY settings.gradle.kts build.gradle.kts ./
COPY src ./src
RUN gradle buildFatJar --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/mindnote-server.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
