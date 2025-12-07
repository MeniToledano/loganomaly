# Stage 1: Build the Java application using a JDK base image

FROM eclipse-temurin:17-jdk-focal as build



# Copy the JAR file produced by Gradle into the container

# ARG specifies the build artifact created by your Gradle build

ARG JAR_FILE=build/libs/*.jar

COPY ${JAR_FILE} app.jar



# Stage 2: Create the final, lean runtime image using JRE

FROM eclipse-temurin:17-jre-focal



# Copy the application JAR from the build stage

COPY --from=build app.jar app.jar



# Run the JAR file when the container starts

ENTRYPOINT ["java", "-jar", "/app.jar"]



# Expose the default Spring Boot port (for internal networking)

EXPOSE 8080


