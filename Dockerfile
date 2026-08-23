FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY bank-common/pom.xml bank-common/pom.xml
COPY bank-common/src bank-common/src
RUN mvn -q -f bank-common/pom.xml -DskipTests install
COPY pom.xml ./
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/target/bank-backend-springboot-1.0.0.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
