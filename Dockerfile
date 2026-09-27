FROM eclipse-temurin:24-jdk-alpine AS build

WORKDIR /workspace

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY src ./src

RUN chmod +x ./gradlew
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:24-jre-alpine

WORKDIR /app

RUN addgroup -S fooddiary && adduser -S fooddiary -G fooddiary

COPY --from=build /workspace/build/libs/fooddiary-*.jar /app/app.jar

USER fooddiary

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
