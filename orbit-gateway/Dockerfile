FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Copy root pom and module pom
COPY pom.xml .
COPY orbit-gateway/pom.xml orbit-gateway/

# Download dependencies
RUN mvn dependency:go-offline -pl orbit-gateway -am -q || true

# Copy sources
COPY orbit-gateway/src orbit-gateway/src

# Build jar
RUN mvn clean package -pl orbit-gateway -am -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/orbit-gateway/target/*.jar app.jar

EXPOSE 8080 8443
ENTRYPOINT ["java", "-jar", "app.jar"]
