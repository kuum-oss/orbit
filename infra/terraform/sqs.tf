resource "aws_sqs_queue" "telemetry_dlq" {
  name                      = "orbit-telemetry-dlq"
  message_retention_seconds = 1209600 # 14 days
}

resource "aws_sqs_queue" "telemetry_queue" {
  name                       = "orbit-telemetry-queue"
  visibility_timeout_seconds = 30

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.telemetry_dlq.arn
    maxReceiveCount     = 3
  })
}
