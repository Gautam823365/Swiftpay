FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /build
COPY . .

# FORCE Maven to treat root as parent reactor
RUN mvn -f pom.xml clean package -pl gateway-service -am -DskipTests


FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /build/gateway-service/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]