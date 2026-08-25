#!/bin/bash
set -euo pipefail
echo "=========================================="
echo "Initializing LocalStack AWS resources..."
echo "=========================================="

# S3 — telemetry archives bucket with versioning
echo "Creating S3 bucket..."
awslocal s3 mb s3://orbit-telemetry-archives
awslocal s3api put-bucket-versioning \
    --bucket orbit-telemetry-archives \
    --versioning-configuration Status=Enabled
echo "  ✓ orbit-telemetry-archives (versioning enabled)"

# SQS — Dead Letter Queue
echo "Creating SQS queues..."
DLQ_URL=$(awslocal sqs create-queue \
    --queue-name orbit-telemetry-dlq \
    --attributes '{"MessageRetentionPeriod":"1209600"}' \
    --query 'QueueUrl' --output text)

DLQ_ARN=$(awslocal sqs get-queue-attributes \
    --queue-url "${DLQ_URL}" \
    --attribute-names QueueArn \
    --query 'Attributes.QueueArn' --output text)
echo "  ✓ orbit-telemetry-dlq (retention 14 days)"

# SQS — Main telemetry queue with redrive to DLQ
awslocal sqs create-queue \
    --queue-name orbit-telemetry-queue \
    --attributes "{\"VisibilityTimeout\":\"30\",\"RedrivePolicy\":\"{\\\"deadLetterTargetArn\\\":\\\"${DLQ_ARN}\\\",\\\"maxReceiveCount\\\":\\\"3\\\"}\"}"
echo "  ✓ orbit-telemetry-queue (redrive → DLQ, maxReceiveCount=3)"

# SNS — Critical alerts topic
echo "Creating SNS topic..."
TOPIC_ARN=$(awslocal sns create-topic \
    --name orbit-critical-alerts \
    --query 'TopicArn' --output text)
echo "  ✓ orbit-critical-alerts"

# SNS → SQS subscription (alerts → DLQ for monitoring)
awslocal sns subscribe \
    --topic-arn "${TOPIC_ARN}" \
    --protocol sqs \
    --notification-endpoint "${DLQ_ARN}"
echo "  ✓ SNS subscription: orbit-critical-alerts → orbit-telemetry-dlq"

echo ""
echo "=========================================="
echo "LocalStack initialization complete!"
echo "=========================================="
