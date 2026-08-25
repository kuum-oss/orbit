resource "aws_sns_topic" "critical_alerts" {
  name = "orbit-critical-alerts"
}

resource "aws_sns_topic_subscription" "dlq_monitoring" {
  topic_arn = aws_sns_topic.critical_alerts.arn
  protocol  = "sqs"
  endpoint  = aws_sqs_queue.telemetry_dlq.arn
}

resource "aws_sqs_queue_policy" "dlq_monitoring_policy" {
  queue_url = aws_sqs_queue.telemetry_dlq.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect    = "Allow"
        Principal = "*"
        Action    = "sqs:SendMessage"
        Resource  = aws_sqs_queue.telemetry_dlq.arn
        Condition = {
          ArnEquals = {
            "aws:SourceArn" = aws_sns_topic.critical_alerts.arn
          }
        }
      }
    ]
  })
}
