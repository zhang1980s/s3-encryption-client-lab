package main

import (
	"github.com/pulumi/pulumi-aws/sdk/v6/go/aws/s3"
	"github.com/pulumi/pulumi/sdk/v3/go/pulumi"
)

func main() {
	pulumi.Run(func(ctx *pulumi.Context) error {
		// Create an AWS resource (S3 Bucket)
		// Note: Pulumi will append a unique suffix to this name
		bucket, err := s3.NewBucketV2(ctx, "zzhe-sin-encrption-client-lab-bucket", nil)
		if err != nil {
			return err
		}

		// Export the name of the bucket
		// This exported name should be used in application.properties (aws.s3.bucket)
		ctx.Export("bucketName", bucket.ID())
		return nil
	})
}
