# syntax=docker/dockerfile:1

# Build stage uses the official Gradle image. This image includes Gradle and OpenJDK 17.
FROM gradle:9.4-jdk17 AS build

# Set the working directory inside the container
WORKDIR /home/gradle/src

# Copy source code
COPY . .

# Build the application using the Gradle wrapper. Use --no-daemon to avoid spinning up the Gradle daemon.
RUN gradle clean bootJar --no-daemon

# Runtime stage uses a slim JDK image for reduced size
FROM eclipse-temurin:17-jre-jammy AS runtime

# Set working directory for the runtime image
WORKDIR /app

# Copy the built jar from the previous stage. The jar is placed in build/libs with the project name.
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar

# Expose the port configured in application.yml (8080 by default)
EXPOSE 8080

# Set the entrypoint to run the Spring Boot application
ENTRYPOINT ["java","-jar","/app/app.jar"]