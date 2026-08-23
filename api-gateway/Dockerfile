FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY bank-common/pom.xml bank-common/pom.xml
COPY bank-common/src bank-common/src
RUN mvn -q -f bank-common/pom.xml -DskipTests install
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY api-gateway/src api-gateway/src
RUN mvn -q -f api-gateway/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/api-gateway/target/api-gateway-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
