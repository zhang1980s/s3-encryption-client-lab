FROM openjdk:8-jdk-alpine

# Set working directory
WORKDIR /app

# Install Maven and debugging tools
RUN apk add --no-cache maven curl jq bash

# Copy the Maven POM file
COPY pom.xml .

# Copy the source code
COPY src ./src

# Create keys directory
RUN mkdir -p keys
# Copy the keys directory if it exists
COPY keys ./keys

# List contents for debugging
RUN echo "Contents of /app:" && \
    ls -la /app && \
    echo "Contents of /app/keys:" && \
    ls -la /app/keys && \
    echo "Contents of /app/src:" && \
    ls -la /app/src

# Build the application
RUN mvn clean package

# Verify the build output
RUN echo "Contents of target directory:" && \
    ls -la target/

# Run the application with verbose logging
CMD ["java", "-XX:+PrintGCDetails", "-XX:+PrintGCDateStamps", "-jar", "target/s3-encryption-client-lab-1.0-SNAPSHOT-jar-with-dependencies.jar"]