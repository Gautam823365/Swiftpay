FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /build

COPY . .

# IMPORTANT: build FULL reactor (parent + all modules)
RUN mvn -N clean install -DskipTests
RUN mvn clean package -pl gateway-service -am -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder gateway-service/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]