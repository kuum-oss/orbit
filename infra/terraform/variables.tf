variable "aws_region" {
  description = "AWS region for all resources"
  type        = string
  default     = "us-east-1"
}

variable "localstack_endpoint" {
  description = "Endpoint URL for LocalStack"
  type        = string
  default     = "http://localstack:4566"
}

variable "environment" {
  description = "Deployment environment"
  type        = string
  default     = "dev"
}
