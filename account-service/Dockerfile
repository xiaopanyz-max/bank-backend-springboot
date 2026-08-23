FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY bank-common/pom.xml bank-common/pom.xml
COPY bank-common/src bank-common/src
RUN mvn -q -f bank-common/pom.xml -DskipTests install
COPY account-service/pom.xml account-service/pom.xml
COPY account-service/src account-service/src
RUN mvn -q -f account-service/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/account-service/target/account-service-1.0.0.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
