FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /build
COPY . .

RUN mvn -f pom.xml clean package -pl ledger-service -am -DskipTests


FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /build/ledger-service/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java","-jar","app.jar"]