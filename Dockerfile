FROM eclipse-temurin:23-jdk-alpine AS builder
WORKDIR /build
COPY . .
RUN ./gradlew bootJar --no-daemon -q

FROM eclipse-temurin:23-jre-alpine
WORKDIR /app
COPY --from=builder /build/build/libs/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
