output "s3_bucket_arn" {
  description = "ARN of the telemetry archives S3 bucket"
  value       = aws_s3_bucket.telemetry_archives.arn
}

output "sqs_queue_url" {
  description = "URL of the main telemetry SQS queue"
  value       = aws_sqs_queue.telemetry_queue.id
}

output "sqs_queue_arn" {
  description = "ARN of the main telemetry SQS queue"
  value       = aws_sqs_queue.telemetry_queue.arn
}

output "sqs_dlq_url" {
  description = "URL of the telemetry DLQ"
  value       = aws_sqs_queue.telemetry_dlq.id
}

output "sqs_dlq_arn" {
  description = "ARN of the telemetry DLQ"
  value       = aws_sqs_queue.telemetry_dlq.arn
}

output "sns_topic_arn" {
  description = "ARN of the critical alerts SNS topic"
  value       = aws_sns_topic.critical_alerts.arn
}
