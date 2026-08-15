FROM eclipse-temurin:25-jdk AS build

WORKDIR /app

COPY . .

RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests


FROM mcr.microsoft.com/playwright/java:v1.61.0-noble

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]