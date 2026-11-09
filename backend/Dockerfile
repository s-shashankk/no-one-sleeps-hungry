# Builds the MealUp API and runs it on a small Java 21 image.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package && cp target/*.jar /app.jar

FROM eclipse-temurin:21-jre
RUN useradd --system --create-home mealup
USER mealup
COPY --from=build /app.jar /opt/mealup.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-Xmx512m", "-jar", "/opt/mealup.jar"]
