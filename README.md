# Amazon S3 Encryption Client Lab Application

This lab application demonstrates how to use the Amazon S3 Encryption Client for Java to perform client-side encryption of data stored in Amazon S3. It uses local RSA keys for encryption.

## Current Implementation Status

The application has been implemented with the following components:

1. **Core Components**:
   - `S3EncryptionClientLabApplication`: Main application class that orchestrates the workflow
   - `LocalKeyService`: Service for creating S3 encryption clients using local RSA keys
   - `S3FileUploadEncryptionService`: Handles encrypted file uploads to S3
   - `KeyPairUtil`: Utility for RSA key pair operations

2. **Implementation Details**:
   - The application uses local RSA keys for client-side encryption
   - It demonstrates the complete workflow of encrypting, uploading, downloading, and decrypting files
   - It includes utilities for key pair generation and management
   - It provides a simplified API for working with the S3 Encryption Client
   - It supports configuring key paths or providing key content directly in application.properties

3. **AWS Credentials Configuration**:
   The application requires valid AWS credentials to interact with AWS services. You can configure credentials in several ways:
   
   - **Environment Variables**:
     ```bash
     export AWS_ACCESS_KEY_ID=your_access_key
     export AWS_SECRET_ACCESS_KEY=your_secret_key
     export AWS_REGION=ap-southeast-1
     ```
   
   - **AWS Credentials File**:
     Create or edit `~/.aws/credentials`:
     ```
     [default]
     aws_access_key_id=your_access_key
     aws_secret_access_key=your_secret_key
     ```
     
     And `~/.aws/config`:
     ```
     [default]
     region=ap-southeast-1
     ```
   
   - **System Properties**:
     ```
     -Daws.accessKeyId=your_access_key
     -Daws.secretAccessKey=your_secret_key
     -Daws.region=ap-southeast-1
     ```
   
   For more information, see the [AWS SDK for Java Developer Guide](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html).

## Architecture

```
┌─────────────────┐                      ┌───────────────┐
│                 │                      │               │
│  Java           │                      │  Amazon S3    │
│  Application    │◄─────────────────────┤  (Encrypted   │
│  (Local Keys)   │                      │  Objects)     │
│                 │                      │               │
└─────────────────┘                      └───────────────┘
        │                                        │
        │                                        │
        │           Encrypted Data               │
        └────────────────────────────────────────┘
```

The application uses local RSA keys to encrypt data before uploading to S3 and decrypt data after downloading from S3.

## Prerequisites

- Java 8 
- Maven
- AWS account with appropriate permissions
- AWS credentials configured

## Setup

1. Deploy the infrastructure using Pulumi:
   ```
   cd infrastructure
   pulumi up
   ```
   
   Note: Pulumi will create an S3 bucket with a name like "zzhe-sin-encrption-client-lab-bucket-a9733f6"

2. Generate RSA key pair (if not already available):
   
   You can use the KeyPairUtil to generate a new RSA key pair:
   ```java
   KeyPair keyPair = KeyPairUtil.generateRSAKeyPair();
   KeyPairUtil.saveKeyPair(keyPair, "keys/public_key.pem", "keys/private_key.pem");
   ```
   
   Or use OpenSSL:
   ```bash
   mkdir -p keys
   openssl genrsa -out keys/private_key.pem 2048
   openssl rsa -in keys/private_key.pem -pubout -out keys/public_key.pem
   ```

3. Build the project:
   ```
   mvn clean package
   ```

4. Update the configuration in application.properties:
   
   Open `src/main/resources/application.properties` and:
   
   a. Set the `aws.s3.bucket` property to the bucket name created by Pulumi:
   ```properties
   aws.s3.bucket=zzhe-sin-encrption-client-lab-bucket-a9733f6
   ```
   
   b. Configure the key paths or key content:
   ```properties
   # Key file paths (if not specified or files not found, will use key content below)
   key.public.path=keys/public_key.pem
   key.private.path=keys/private_key.pem

   # Key content (used as fallback if key files are not found)
   # key.public.content=-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...\n-----END PUBLIC KEY-----
   # key.private.content=-----BEGIN PRIVATE KEY-----\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBAK...\n-----END PRIVATE KEY-----
   ```

5. Run the S3EncryptionClientLabApplication:
   ```
   java -jar target/s3-encryption-client-lab-1.0-SNAPSHOT-jar-with-dependencies.jar
   ```

   Or directly with Maven:
   ```
   mvn exec:java -Dexec.mainClass="xyz.zzhe.s3encryptionclientlab.S3EncryptionClientLabApplication"
   ```
   
   Or simply:
   ```
   mvn exec:java
   ```

## Lab Exercises

### Exercise 1: Setting up the encryption client with local RSA keys

The lab demonstrates how to set up the S3 encryption client using local RSA keys. The S3EncryptionClientLabApplication will:
- Load the RSA key pair from the configured location or from application.properties
- Create an S3 encryption client using the local RSA key pair

### Exercise 2: Uploading encrypted objects

The lab demonstrates how to upload encrypted objects to S3. The S3EncryptionClientLabApplication will:
- Create a test file with sample content
- Add metadata to the file
- Upload the file to S3 using the encryption client
- Display the upload response, including the file key and pre-signed URL

### Exercise 3: Downloading and decrypting objects

The lab demonstrates how to download and decrypt objects from S3. The S3EncryptionClientLabApplication will:
- Download the previously uploaded file
- Decrypt the file using the encryption client
- Display the decrypted content

### Exercise 4: Working with pre-signed URLs

The lab demonstrates how to generate pre-signed URLs for encrypted objects. The S3EncryptionClientLabApplication will:
- Generate a pre-signed URL for the uploaded file
- Display the URL, which can be used to access the file for a limited time

## Advanced Topics

- Using different encryption algorithms
- Encryption context
- Key rotation
- Cross-account access to encrypted objects
- Managing key expiration and rotation

## Using the LocalKeyService

The application includes a `LocalKeyService` class that simplifies the process of using local keys for encryption:

1. **Create a LocalKeyService with default key paths**:
   ```java
   LocalKeyService localKeyService = new LocalKeyService();
   S3EncryptionClient s3EncryptionClient = localKeyService.createS3EncryptionClient();
   ```

2. **Create a LocalKeyService with specific key paths**:
   ```java
   LocalKeyService localKeyService = new LocalKeyService("path/to/public_key.pem", "path/to/private_key.pem");
   S3EncryptionClient s3EncryptionClient = localKeyService.createS3EncryptionClient();
   ```

3. **Create a LocalKeyService with properties**:
   ```java
   S3Properties s3Properties = S3Properties.loadFromProperties();
   LocalKeyService localKeyService = new LocalKeyService(s3Properties);
   S3EncryptionClient s3EncryptionClient = localKeyService.createS3EncryptionClient();
   ```

## Key Features

### Local Key Management

The application uses local RSA keys for client-side encryption:

1. **Key Loading Options**: The application provides multiple ways to load RSA keys:
   - From files specified in application.properties
   - From key content provided directly in application.properties
   - From default file paths if neither of the above is specified

2. **Fallback Mechanism**: The application implements a fallback mechanism:
   ```java
   // Try to load keys from files first if paths are specified
   if (s3Properties.hasKeyPaths()) {
       // Check if files exist
       if (publicKeyExists && privateKeyExists) {
           this.keyPair = KeyPairUtil.loadKeyPair(publicKeyPath, privateKeyPath);
       }
   }
   
   // Fallback to key content if available
   if (s3Properties.hasKeyContent()) {
       this.keyPair = KeyPairUtil.reconstructKeyPair(
               s3Properties.getPublicKeyContent(), 
               s3Properties.getPrivateKeyContent());
   }
   
   // If neither paths nor content are available, try default paths
   this.keyPair = KeyPairUtil.loadKeyPair(DEFAULT_PUBLIC_KEY_PATH, DEFAULT_PRIVATE_KEY_PATH);
   ```

3. **Key Verification**: The application verifies that the key pair is valid:
   ```java
   public boolean verifyKeyPair() {
       try {
           // Simple verification by checking if the keys are not null
           if (keyPair.getPublic() == null || keyPair.getPrivate() == null) {
               return false;
           }
           return true;
       } catch (Exception e) {
           return false;
       }
   }
   ```

4. **S3 Encryption Client Creation**: The application creates an S3 encryption client using the local RSA key pair:
   ```java
   S3EncryptionClient.builder()
           .rsaKeyPair(keyPair)
           .build();
   ```

This approach provides several benefits:
- Simple key management without requiring AWS KMS
- Full control over your encryption keys
- No additional costs for key management services
- Keys can be stored securely in your application's configuration

## Resources

- [Amazon S3 Encryption Client for Java](https://github.com/aws/amazon-s3-encryption-client-java)
- [AWS SDK for Java Developer Guide](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/home.html)
- [Amazon S3 Developer Guide](https://docs.aws.amazon.com/AmazonS3/latest/dev/Welcome.html)
- [Amazon S3 Encryption Client Documentation](https://docs.aws.amazon.com/AmazonS3/latest/userguide/UsingClientSideEncryption.html)

## Running in Docker

The application can also be run in a Docker container. This is useful for testing in isolated environments or for deployment scenarios.

### Prerequisites for Docker

- Docker installed on your system
- Docker Compose (optional, but recommended)
- EC2 instance with appropriate IAM role for S3 access

### Building and Running with Docker

1. **Build the Docker image**:
   ```bash
   docker build -t s3-encryption-client .
   ```

2. **Run the container using EC2 instance role**:
   ```bash
   docker run --network host \
              -e AWS_REGION=ap-southeast-1 \
              -v $(pwd)/keys:/app/keys \
              s3-encryption-client
   ```

   The `--network host` flag allows the container to access the EC2 instance metadata service to retrieve role credentials.

### Using Docker Compose

Alternatively, you can use Docker Compose for a simpler setup:

1. **Set your AWS region as environment variable (optional)**:
   ```bash
   export AWS_REGION=ap-southeast-1
   ```

2. **Run with Docker Compose**:
   ```bash
   docker-compose up
   ```

   The Docker Compose file is configured to use the host network mode to access the EC2 instance metadata service.

### Using the Convenience Script

For convenience, a shell script is provided to build and run the Docker container:

1. **Make the script executable** (if not already):
   ```bash
   chmod +x run-docker-test.sh
   ```

2. **Run the script**:
   ```bash
   ./run-docker-test.sh
   ```

This script will build the Docker image and run the container with the appropriate settings, including mounting the keys directory and application.properties file.

### Configuration Options

- The Docker setup mounts the `keys` directory from your host into the container, allowing you to use your existing key files.
- The `application.properties` file is also mounted, so any changes you make to it will be reflected in the container.
- You can override environment variables in the Docker Compose file or when running the Docker container directly.

### Testing in Docker

Running the application in Docker provides several benefits for testing:
- Isolated environment that doesn't affect your local system
- Consistent environment across different development machines
- Easy to test with different configurations by modifying environment variables
- Simulates deployment scenarios more accurately
