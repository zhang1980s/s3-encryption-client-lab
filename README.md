# Amazon S3 Encryption Client Lab Application

This lab application demonstrates how to use the Amazon S3 Encryption Client for Java to perform client-side encryption of data stored in Amazon S3. It uses AWS KMS for secure key management.

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
   - An EC2 instance with scripts to import key material into KMS
   
   The exact names and IDs will be shown in the Pulumi output.

2. Import the key material into KMS:
   
   a. Connect to the EC2 instance created by Pulumi:
   ```
   ssh -i your-key.pem ec2-user@<EC2-INSTANCE-IP>
   ```
   
   b. Run the key import script:
   ```
   ./import_key_material.sh
   ```
   
   c. Note the new KMS key ID with EXTERNAL origin:
   ```
   cat kms_env.sh
   ```
   
   This will show the NEW_KMS_KEY_ID that you'll need to use in the application.

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
   
   b. Set the `aws.kms.keyId` property to the NEW_KMS_KEY_ID from the EC2 instance:
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
     --wrapping-algorithm RSAES_OAEP_SHA_1 \
     --wrapping-key-spec RSA_2048
   
   # Wrap the key material using the public key from KMS
   openssl pkeyutl -encrypt -in private_key.pem -out wrapped_key.bin \
     -inkey public_key.bin -keyform DER -pubin \
     -pkeyopt rsa_padding_mode:oaep -pkeyopt rsa_oaep_md:sha1
   
   # Import the wrapped key material
   aws kms import-key-material \
     --key-id <KMS key ID> \
     --encrypted-key-material fileb://wrapped_key.bin \
     --import-token fileb://import_token.bin \
     --expiration-model KEY_MATERIAL_DOES_NOT_EXPIRE
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