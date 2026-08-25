FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy root pom and module poms
COPY pom.xml .
COPY orbit-ingest/pom.xml orbit-ingest/
COPY orbit-processor/pom.xml orbit-processor/

# Copy sources
COPY orbit-ingest/src orbit-ingest/src

# Build jar
RUN mvn clean package -pl orbit-ingest -am -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/orbit-ingest/target/*.jar app.jar

EXPOSE 8081 9090
ENTRYPOINT ["java", "-jar", "app.jar"]
