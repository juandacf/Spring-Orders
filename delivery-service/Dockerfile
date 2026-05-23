# syntax=docker/dockerfile:1

# Build stage: compile the delivery-service using the Gradle image.
FROM gradle:9.4-jdk17 AS build

WORKDIR /home/gradle/src

# Copy source code into the container
COPY . .

# Build the executable Spring Boot jar. Use --no-daemon to speed up CI builds.
RUN gradle clean bootJar --no-daemon

# Runtime stage uses a slim Java runtime to run the jar
FROM eclipse-temurin:17-jre-jammy AS runtime

WORKDIR /app

# Copy only the built jar into the runtime image
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar

# Expose the service port
EXPOSE 8080

# Launch the Spring Boot application
ENTRYPOINT ["java","-jar","/app/app.jar"]