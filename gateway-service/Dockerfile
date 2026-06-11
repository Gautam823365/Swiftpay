FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /build

COPY . .

# Build full Maven reactor (parent + modules)
RUN mvn clean install -DskipTests

# Build only gateway service
RUN mvn clean package -pl gateway-service -am -DskipTests


FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /build/gateway-service/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]