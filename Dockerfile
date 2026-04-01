# STEP 1: Build JAR inside Docker
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app
COPY . .

RUN mvn clean package -DskipTests

# STEP 2: Run JAR
FROM eclipse-temurin:21-jdk

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

ENTRYPOINT ["java","-jar","app.jar"]