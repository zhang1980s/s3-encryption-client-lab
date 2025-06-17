package main

import (
	"io/ioutil"
	"path/filepath"
	"strings"

	"github.com/pulumi/pulumi-aws/sdk/v6/go/aws/ec2"
	"github.com/pulumi/pulumi-aws/sdk/v6/go/aws/iam"
	"github.com/pulumi/pulumi-aws/sdk/v6/go/aws/kms"
	"github.com/pulumi/pulumi-aws/sdk/v6/go/aws/s3"
	"github.com/pulumi/pulumi/sdk/v3/go/pulumi"
)

func main() {
	pulumi.Run(func(ctx *pulumi.Context) error {
		// Create a VPC
		vpc, err := ec2.NewVpc(ctx, "s3-encryption-lab-vpc", &ec2.VpcArgs{
			CidrBlock:          pulumi.String("10.0.0.0/16"),
			EnableDnsHostnames: pulumi.Bool(true),
			EnableDnsSupport:   pulumi.Bool(true),
			Tags: pulumi.StringMap{
				"Name": pulumi.String("s3-encryption-lab-vpc"),
			},
		})
		if err != nil {
			return err
		}

		// Create an Internet Gateway
		igw, err := ec2.NewInternetGateway(ctx, "s3-encryption-lab-igw", &ec2.InternetGatewayArgs{
			VpcId: vpc.ID(),
			Tags: pulumi.StringMap{
				"Name": pulumi.String("s3-encryption-lab-igw"),
			},
		})
		if err != nil {
			return err
		}

		// Create a public subnet
		publicSubnet, err := ec2.NewSubnet(ctx, "s3-encryption-lab-public-subnet", &ec2.SubnetArgs{
			VpcId:               vpc.ID(),
			CidrBlock:           pulumi.String("10.0.1.0/24"),
			AvailabilityZone:    pulumi.String("ap-southeast-1a"), // Singapore region
			MapPublicIpOnLaunch: pulumi.Bool(true),
			Tags: pulumi.StringMap{
				"Name": pulumi.String("s3-encryption-lab-public-subnet"),
			},
		})
		if err != nil {
			return err
		}

		// Create a route table
		routeTable, err := ec2.NewRouteTable(ctx, "s3-encryption-lab-rt", &ec2.RouteTableArgs{
			VpcId: vpc.ID(),
			Routes: ec2.RouteTableRouteArray{
				&ec2.RouteTableRouteArgs{
					CidrBlock: pulumi.String("0.0.0.0/0"),
					GatewayId: igw.ID(),
				},
			},
			Tags: pulumi.StringMap{
				"Name": pulumi.String("s3-encryption-lab-rt"),
			},
		})
		if err != nil {
			return err
		}

		// Associate the route table with the subnet
		_, err = ec2.NewRouteTableAssociation(ctx, "s3-encryption-lab-rt-assoc", &ec2.RouteTableAssociationArgs{
			SubnetId:     publicSubnet.ID(),
			RouteTableId: routeTable.ID(),
		})
		if err != nil {
			return err
		}

		// Create a security group for the EC2 instance
		securityGroup, err := ec2.NewSecurityGroup(ctx, "s3-encryption-lab-sg", &ec2.SecurityGroupArgs{
			VpcId: vpc.ID(),
			Ingress: ec2.SecurityGroupIngressArray{
				&ec2.SecurityGroupIngressArgs{
					Protocol:   pulumi.String("tcp"),
					FromPort:   pulumi.Int(22),
					ToPort:     pulumi.Int(22),
					CidrBlocks: pulumi.StringArray{pulumi.String("0.0.0.0/0")},
				},
			},
			Egress: ec2.SecurityGroupEgressArray{
				&ec2.SecurityGroupEgressArgs{
					Protocol:   pulumi.String("-1"),
					FromPort:   pulumi.Int(0),
					ToPort:     pulumi.Int(0),
					CidrBlocks: pulumi.StringArray{pulumi.String("0.0.0.0/0")},
				},
			},
			Tags: pulumi.StringMap{
				"Name": pulumi.String("s3-encryption-lab-sg"),
			},
		})
		if err != nil {
			return err
		}

		// Create an IAM role for the EC2 instance
		role, err := iam.NewRole(ctx, "s3-encryption-lab-role", &iam.RoleArgs{
			AssumeRolePolicy: pulumi.String(`{
				"Version": "2012-10-17",
				"Statement": [{
					"Action": "sts:AssumeRole",
					"Principal": {
						"Service": "ec2.amazonaws.com"
					},
					"Effect": "Allow",
					"Sid": ""
				}]
			}`),
		})
		if err != nil {
			return err
		}

		// Attach the AmazonS3FullAccess policy to the role
		_, err = iam.NewRolePolicyAttachment(ctx, "s3-encryption-lab-policy-attachment", &iam.RolePolicyAttachmentArgs{
			Role:      role.Name,
			PolicyArn: pulumi.String("arn:aws:iam::aws:policy/AmazonS3FullAccess"),
		})
		if err != nil {
			return err
		}

		// Attach the AWSKeyManagementServicePowerUser policy to the role
		_, err = iam.NewRolePolicyAttachment(ctx, "s3-encryption-lab-kms-policy-attachment", &iam.RolePolicyAttachmentArgs{
			Role:      role.Name,
			PolicyArn: pulumi.String("arn:aws:iam::aws:policy/AWSKeyManagementServicePowerUser"),
		})
		if err != nil {
			return err
		}

		// Create an instance profile
		instanceProfile, err := iam.NewInstanceProfile(ctx, "s3-encryption-lab-instance-profile", &iam.InstanceProfileArgs{
			Role: role.Name,
		})
		if err != nil {
			return err
		}

		// Get the latest Amazon Linux 2023 AMI
		ami, err := ec2.LookupAmi(ctx, &ec2.LookupAmiArgs{
			Owners:     []string{"amazon"},
			MostRecent: pulumi.BoolRef(true),
			Filters: []ec2.GetAmiFilter{
				{
					Name:   "name",
					Values: []string{"al2023-ami-2023.*-x86_64"},
				},
				{
					Name:   "virtualization-type",
					Values: []string{"hvm"},
				},
				{
					Name:   "root-device-type",
					Values: []string{"ebs"},
				},
			},
		})
		if err != nil {
			return err
		}

		// We'll create the EC2 instance after preparing the user data script
		var instance *ec2.Instance

		// Create an S3 bucket
		bucket, err := s3.NewBucketV2(ctx, "zzhe-sin-encrption-client-lab-bucket", &s3.BucketV2Args{
			Tags: pulumi.StringMap{
				"Name": pulumi.String("s3-encryption-lab-bucket"),
			},
		})
		if err != nil {
			return err
		}

		// Create a VPC endpoint policy for S3
		endpointPolicy := pulumi.String(`{
			"Version": "2012-10-17",
			"Statement": [
				{
					"Effect": "Allow",
					"Principal": "*",
					"Action": "s3:*",
					"Resource": [
						"arn:aws:s3:::*/*",
						"arn:aws:s3:::*"
					]
				}
			]
		}`)

		// Create a VPC endpoint for S3
		_, err = ec2.NewVpcEndpoint(ctx, "s3-encryption-lab-s3-endpoint", &ec2.VpcEndpointArgs{
			VpcId:             vpc.ID(),
			ServiceName:       pulumi.String("com.amazonaws.ap-southeast-1.s3"),
			VpcEndpointType:   pulumi.String("Gateway"),
			RouteTableIds:     pulumi.StringArray{routeTable.ID()},
			Policy:            endpointPolicy,
			PrivateDnsEnabled: pulumi.Bool(false), // Not applicable for Gateway endpoints
		})
		if err != nil {
			return err
		}

		// Read the existing key pair from keys directory
		publicKeyPath := filepath.Join("..", "keys", "public_key.pem")
		privateKeyPath := filepath.Join("..", "keys", "private_key.pem")

		publicKeyBytes, err := ioutil.ReadFile(publicKeyPath)
		if err != nil {
			return err
		}
		publicKeyPEM := string(publicKeyBytes)
		publicKeyContent := strings.ReplaceAll(
			strings.ReplaceAll(publicKeyPEM, "-----BEGIN PUBLIC KEY-----", ""),
			"-----END PUBLIC KEY-----", "")
		publicKeyContent = strings.ReplaceAll(publicKeyContent, "\n", "")

		privateKeyBytes, err := ioutil.ReadFile(privateKeyPath)
		if err != nil {
			return err
		}
		privateKeyPEM := string(privateKeyBytes)
		privateKeyContent := strings.ReplaceAll(
			strings.ReplaceAll(privateKeyPEM, "-----BEGIN PRIVATE KEY-----", ""),
			"-----END PRIVATE KEY-----", "")
		privateKeyContent = strings.ReplaceAll(privateKeyContent, "\n", "")

		// Create KMS key for S3 encryption
		// Note: Pulumi AWS SDK doesn't directly support setting origin=EXTERNAL
		// We'll create a standard KMS key and then use AWS CLI to create a new key with EXTERNAL origin
		kmsKey, err := kms.NewKey(ctx, "s3-encryption-lab-kms-key", &kms.KeyArgs{
			Description: pulumi.String("KMS key for S3 encryption client lab"),
			KeyUsage:    pulumi.String("ENCRYPT_DECRYPT"),
			Tags: pulumi.StringMap{
				"Name": pulumi.String("s3-encryption-lab-kms-key"),
			},
		})
		if err != nil {
			return err
		}

		// Create a script to create a new KMS key with EXTERNAL origin and import key material
		importScript := `#!/bin/bash
set -e

# Store paths
PUBLIC_KEY_PATH="/tmp/kms_public_key.bin"
IMPORT_TOKEN_PATH="/tmp/import_token.bin"
WRAPPED_KEY_PATH="/tmp/wrapped_key.bin"

# Create a new KMS key with EXTERNAL origin
echo "Creating new KMS key with EXTERNAL origin..."
NEW_KEY_ID=$(aws kms create-key --description "KMS key for S3 encryption client lab (EXTERNAL)" --origin EXTERNAL --tags TagKey=Name,TagValue=s3-encryption-lab-kms-key-external --query 'KeyMetadata.KeyId' --output text)

echo "Created new KMS key with ID: $NEW_KEY_ID"
echo "NEW_KMS_KEY_ID=$NEW_KEY_ID" >> /home/ec2-user/kms_env.sh

# Get parameters for import
echo "Getting import parameters..."
aws kms get-parameters-for-import --key-id $NEW_KEY_ID --wrapping-algorithm RSAES_OAEP_SHA_1 --wrapping-key-spec RSA_2048 --output text --query 'PublicKey' > $PUBLIC_KEY_PATH

aws kms get-parameters-for-import --key-id $NEW_KEY_ID --wrapping-algorithm RSAES_OAEP_SHA_1 --wrapping-key-spec RSA_2048 --output text --query 'ImportToken' > $IMPORT_TOKEN_PATH

# Wrap the key material using OpenSSL
echo "Wrapping key material..."
openssl pkeyutl -encrypt -in /home/ec2-user/keys/private_key.pem -out $WRAPPED_KEY_PATH -inkey $PUBLIC_KEY_PATH -keyform DER -pubin -pkeyopt rsa_padding_mode:oaep -pkeyopt rsa_oaep_md:sha1

# Import the wrapped key material
echo "Importing key material..."
aws kms import-key-material --key-id $NEW_KEY_ID --encrypted-key-material fileb://$WRAPPED_KEY_PATH --import-token fileb://$IMPORT_TOKEN_PATH --expiration-model KEY_MATERIAL_DOES_NOT_EXPIRE

echo "Key material imported successfully!"
echo "Use the NEW_KMS_KEY_ID from kms_env.sh in your application"
`

		// Create a user data script to run on EC2 instance startup
		userData := pulumi.Sprintf(`#!/bin/bash
echo "Setting up KMS key import..."

# Update system and install required packages
yum update -y
yum install -y openssl jq

# Configure AWS CLI
mkdir -p /home/ec2-user/.aws
cat > /home/ec2-user/.aws/config << 'EOL'
[default]
region = ap-southeast-1
output = json
EOL

# Set up key material
mkdir -p /home/ec2-user/keys
echo "%s" > /home/ec2-user/keys/public_key.pem
echo "%s" > /home/ec2-user/keys/private_key.pem
echo "%s" > /home/ec2-user/import_key_material.sh
chmod +x /home/ec2-user/import_key_material.sh

# Set environment variables
export KMS_KEY_ID=%s
echo "KMS_KEY_ID=$KMS_KEY_ID" > /home/ec2-user/kms_env.sh

# Set proper permissions
chown -R ec2-user:ec2-user /home/ec2-user/keys
chown -R ec2-user:ec2-user /home/ec2-user/import_key_material.sh
chown -R ec2-user:ec2-user /home/ec2-user/kms_env.sh
chown -R ec2-user:ec2-user /home/ec2-user/.aws

echo "Key material prepared for import. Run import_key_material.sh to complete the process."
`, publicKeyPEM, privateKeyPEM, importScript, kmsKey.ID())

		// Create the EC2 instance with the user data script
		instance, err = ec2.NewInstance(ctx, "s3-encryption-lab-instance", &ec2.InstanceArgs{
			InstanceType:        pulumi.String("t3.medium"),
			Ami:                 pulumi.String(ami.Id),
			SubnetId:            publicSubnet.ID(),
			VpcSecurityGroupIds: pulumi.StringArray{securityGroup.ID()},
			IamInstanceProfile:  instanceProfile.Name,
			KeyName:             pulumi.String("keypair-sandbox0-sin-mymac.pem"), // Using pre-created key pair
			UserData:            userData,
			Tags: pulumi.StringMap{
				"Name": pulumi.String("s3-encryption-lab-instance"),
			},
		})
		if err != nil {
			return err
		}

		// Export the name of the bucket and KMS key ID
		ctx.Export("bucketName", bucket.ID())
		ctx.Export("kmsKeyId", kmsKey.ID())
		ctx.Export("vpcId", vpc.ID())
		ctx.Export("publicSubnetId", publicSubnet.ID())
		ctx.Export("instanceId", instance.ID())
		ctx.Export("instancePublicIp", instance.PublicIp)
		ctx.Export("instancePrivateIp", instance.PrivateIp)
		ctx.Export("importInstructions", pulumi.String("Connect to the EC2 instance and run: ./import_key_material.sh"))

		return nil
	})
}
