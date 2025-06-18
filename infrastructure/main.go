package main

import (
	"github.com/pulumi/pulumi-aws/sdk/v6/go/aws/kms"
	"github.com/pulumi/pulumi-aws/sdk/v6/go/aws/s3"
	"github.com/pulumi/pulumi/sdk/v3/go/pulumi"
)

func main() {
	pulumi.Run(func(ctx *pulumi.Context) error {
		// Create an S3 bucket
		bucket, err := s3.NewBucketV2(ctx, "zzhe-sin-encrption-client-lab-bucket", &s3.BucketV2Args{
			Tags: pulumi.StringMap{
				"Name": pulumi.String("s3-encryption-lab-bucket"),
			},
		})
		if err != nil {
			return err
		}

		// Create KMS key for S3 encryption
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

		// Export the name of the bucket and KMS key ID
		ctx.Export("bucketName", bucket.ID())
		ctx.Export("kmsKeyId", kmsKey.ID())

		return nil
	})
}
