#!/bin/bash

# Exit on error and print commands
set -ex

echo "=== S3 Encryption Client Docker Test ==="
echo "Current directory: $(pwd)"
echo "Listing keys directory:"
ls -la keys/

echo "Checking application.properties:"
cat src/main/resources/application.properties

echo "Building Docker image..."
docker build -t s3-encryption-client .

echo "Running S3 Encryption Client in Docker container..."
docker run --network host \
           -e AWS_REGION=${AWS_REGION:-ap-southeast-1} \
           -v $(pwd)/keys:/app/keys \
           -v $(pwd)/src/main/resources/application.properties:/app/src/main/resources/application.properties \
           -e JAVA_TOOL_OPTIONS="-XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xmx512m" \
           s3-encryption-client

echo "Docker test completed."