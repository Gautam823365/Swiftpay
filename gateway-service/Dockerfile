FROM maven:3.9.11-eclipse-temurin-21 AS builder

WORKDIR /build

COPY . .

RUN mvn -pl gateway-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /build/gateway-service/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]