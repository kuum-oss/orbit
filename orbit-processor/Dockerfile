FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Copy root pom and module poms
COPY pom.xml .
COPY orbit-ingest/pom.xml orbit-ingest/
COPY orbit-processor/pom.xml orbit-processor/

# Copy sources
COPY orbit-processor/src orbit-processor/src

# Build jar
RUN mvn clean package -pl orbit-processor -am -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/orbit-processor/target/*.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
