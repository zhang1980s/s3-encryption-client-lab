# Amazon S3 Encryption Client Lab Application

This lab application demonstrates how to use the Amazon S3 Encryption Client for Java to perform client-side encryption of data stored in Amazon S3. It uses AWS KMS for secure key management.

## Current Implementation Status

The application has been implemented with the following components:

1. **Core Components**:
   - `S3EncryptionClientLabApplication`: Main application class that orchestrates the workflow
   - `KmsKeyImporter`: Service for importing key material to AWS KMS
   - `KmsKeyService`: Service for creating S3 encryption clients using KMS keys
   - `S3FileUploadEncryptionService`: Handles encrypted file uploads to S3
   - `KeyPairUtil`: Utility for RSA key pair operations

2. **Known Issues**:
   - RSA Key Size Limitation: When attempting to import a new key, the application encounters an error: `javax.crypto.IllegalBlockSizeException: Data must not be longer than 190 bytes`. This occurs because the application tries to encrypt the entire private key using RSA encryption, which has size limitations.
   - Configuration: The application.properties file contains a placeholder `${KMS_KEY_ID}` that needs to be replaced with an actual KMS key ID.

3. **Implementation Details**:
   - The application can automatically import a new KMS key if one is not configured
   - It demonstrates the complete workflow of encrypting, uploading, downloading, and decrypting files
   - It includes utilities for key pair generation and management
   - It provides a simplified API for working with the S3 Encryption Client

4. **Potential Fix for RSA Key Size Limitation**:
   The current implementation attempts to encrypt the entire private key using RSA encryption, which fails due to size limitations. To fix this issue, consider:
   
   - Using a symmetric key (like AES) for the actual key material and then encrypting that symmetric key with RSA
   - Using a smaller portion of the private key as the key material
   - Implementing a chunking mechanism to encrypt the key material in smaller pieces
   
   Example approach using a symmetric key:
   ```java
   // Generate a symmetric key (e.g., AES)
   KeyGenerator keyGen = KeyGenerator.getInstance("AES");
   keyGen.init(256);
   SecretKey secretKey = keyGen.generateKey();
   byte[] keyMaterial = secretKey.getEncoded(); // Much smaller than RSA private key
   
   // Encrypt this smaller key material with RSA
   byte[] encryptedKeyMaterial = encryptKeyMaterial(keyMaterial, wrappingPublicKey);
   ```

5. **AWS Credentials Configuration**:
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
┌─────────────────┐     ┌───────────────┐     ┌───────────────┐
│                 │     │               │     │               │
│  Java           │     │  AWS KMS      │     │  Amazon S3    │
│  Application    │◄────┤  (Key         │     │  (Encrypted   │
│                 │     │  Management)  │     │  Objects)     │
│                 │     │               │     │               │
└────────┬────────┘     └───────────────┘     └───────┬───────┘
         │                                            │
         │                                            │
         │           Encrypted Data                   │
         └────────────────────────────────────────────┘
```

The application uses AWS KMS to securely store and retrieve encryption keys, which are then used by the S3 Encryption Client to encrypt data before uploading to S3 and decrypt data after downloading from S3.

## Prerequisites

- Java 8 (Amazon Corretto 8 recommended)
- Maven
- AWS account with appropriate permissions
- AWS credentials configured

## Setup

1. Deploy the infrastructure using Pulumi:
   ```
   cd infrastructure
   pulumi up
   ```
   
   Note: Pulumi will create:
   - An S3 bucket with a name like "zzhe-sin-encrption-client-lab-bucket-a9733f6"
   - A KMS key for encryption
   
   The exact names and IDs will be shown in the Pulumi output.

2. Import the key material into KMS:
   
   Run the KmsKeyImportUtil to create a new KMS key and import key material:
   ```
   mvn exec:java -Dexec.mainClass="xyz.zzhe.s3encryptionclientlab.util.KmsKeyImportUtil" -Dexec.args="My Key Description"
   ```
   
   The utility will:
   - Create a new KMS key with EXTERNAL origin
   - Import key material
   - Output the key ID that you should use in your application.properties file
   
   Note: The main application can also automatically import a key if one is not configured.

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
   
   b. Set the `aws.kms.keyId` property to the key ID obtained from the KmsKeyImportUtil:
   ```properties
   aws.kms.keyId=12345678-abcd-1234-efgh-123456789012
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

### Exercise 1: Setting up the encryption client with AWS KMS

The lab demonstrates how to set up the S3 encryption client using RSA key pairs stored in AWS KMS. The S3EncryptionClientLabApplication will:
- Load the KMS key ID from application.properties
- Retrieve the RSA key pair from KMS
- Create an S3 encryption client using the key pair from KMS

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
- Importing multiple keys for different encryption purposes
- Managing key expiration and rotation

## Using the KmsKeyImporter

The application includes a `KmsKeyImporter` class that simplifies the process of importing keys to KMS:

1. **Import a new key programmatically**:
   ```java
   KmsKeyImporter keyImporter = new KmsKeyImporter();
   String keyId = keyImporter.importNewKey("My Key Description");
   ```

2. **Use the imported key for encryption**:
   ```java
   KmsKeyService kmsKeyService = new KmsKeyService(keyId);
   S3EncryptionClient s3EncryptionClient = kmsKeyService.createS3EncryptionClient();
   ```

3. **Import a key with specific parameters**:
   ```java
   KmsKeyImporter keyImporter = new KmsKeyImporter();
   
   // Create a KMS key with EXTERNAL origin
   String keyId = keyImporter.createExternalKmsKey("My Key Description");
   
   // Get import parameters
   byte[][] importParams = keyImporter.getImportParameters(keyId);
   byte[] wrappingPublicKey = importParams[0];
   byte[] importToken = importParams[1];
   
   // Encrypt and import your key material
   byte[] keyMaterial = ...; // Your key material
   byte[] encryptedKeyMaterial = keyImporter.encryptKeyMaterial(keyMaterial, wrappingPublicKey);
   keyImporter.importKeyMaterial(keyId, encryptedKeyMaterial, importToken, 
                                ExpirationModelType.KEY_MATERIAL_DOES_NOT_EXPIRE);
   ```

4. **Run the standalone utility**:
   ```bash
   mvn exec:java -Dexec.mainClass="xyz.zzhe.s3encryptionclientlab.util.KmsKeyImportUtil" -Dexec.args="My Key Description"
   ```

## Key Features

### AWS KMS Integration

The application uses AWS KMS for secure key management using the standard KMS import process:

1. **Key Creation with EXTERNAL Origin**: A KMS key is created with EXTERNAL origin, which allows importing external key material:
   ```bash
   aws kms create-key --description "KMS key for S3 encryption" --origin EXTERNAL
   ```

2. **Key Material Import Process**: The application imports existing key material into KMS using the standard import process:
   ```bash
   # Get parameters for import
   aws kms get-parameters-for-import \
     --key-id <KMS key ID> \
     --wrapping-algorithm RSAES_OAEP_SHA_256 \
     --wrapping-key-spec RSA_2048
   
   # Wrap the key material using the public key from KMS
   openssl pkeyutl -encrypt -in private_key.pem -out wrapped_key.bin \
     -inkey public_key.bin -keyform DER -pubin \
     -pkeyopt rsa_padding_mode:oaep -pkeyopt rsa_oaep_md:sha256 \
     -pkeyopt rsa_mgf1_md:sha256
   
   # Import the wrapped key material
   aws kms import-key-material \
     --key-id <KMS key ID> \
     --encrypted-key-material fileb://wrapped_key.bin \
     --import-token fileb://import_token.bin \
     --expiration-model KEY_MATERIAL_DOES_NOT_EXPIRE
   ```

3. **Simplified Key Import Process**: The application now includes a simplified way to import keys to KMS:
   ```java
   // Import a new key to KMS in one step
   KmsKeyImporter keyImporter = new KmsKeyImporter();
   String keyId = keyImporter.importNewKey("My Key Description");
   
   // Use the key for S3 encryption
   KmsKeyService kmsKeyService = new KmsKeyService(keyId);
   S3EncryptionClient s3EncryptionClient = kmsKeyService.createS3EncryptionClient();
   ```

3. **KMS Key Verification**: The application verifies that the KMS key is properly configured:
   ```java
   public boolean verifyKmsKeyAccess() {
       DescribeKeyResponse response = kmsClient.describeKey(
               DescribeKeyRequest.builder()
                       .keyId(keyId)
                       .build());
       
       boolean isEnabled = response.keyMetadata().enabled();
       String keyState = response.keyMetadata().keyStateAsString();
       String keyOrigin = response.keyMetadata().originAsString();
       
       // Check if the key is enabled and has EXTERNAL origin
       return isEnabled && "Enabled".equals(keyState) && "EXTERNAL".equals(keyOrigin);
   }
   ```

4. **S3 Encryption Client Creation**: The application creates an S3 encryption client using the KMS key:
   ```java
   KmsKeyService kmsKeyService = new KmsKeyService(s3Properties.getKmsKeyId());
   S3EncryptionClient s3EncryptionClient = kmsKeyService.createS3EncryptionClient();
   ```

This approach provides several security benefits:
- The private key material is protected by KMS's security mechanisms
- Cryptographic operations happen within KMS's secure environment
- The private key is never exposed in plaintext outside of KMS
- You benefit from KMS's access controls, audit logging, and key rotation capabilities

## Resources

- [Amazon S3 Encryption Client for Java](https://github.com/aws/amazon-s3-encryption-client-java)
- [AWS SDK for Java Developer Guide](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/home.html)
- [Amazon S3 Developer Guide](https://docs.aws.amazon.com/AmazonS3/latest/dev/Welcome.html)
- [Amazon S3 Encryption Client Documentation](https://docs.aws.amazon.com/AmazonS3/latest/userguide/UsingClientSideEncryption.html)
- [AWS KMS Developer Guide](https://docs.aws.amazon.com/kms/latest/developerguide/overview.html)
