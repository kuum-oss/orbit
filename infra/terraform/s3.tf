resource "aws_s3_bucket" "telemetry_archives" {
  bucket = "orbit-telemetry-archives"
}

resource "aws_s3_bucket_versioning" "telemetry_archives_versioning" {
  bucket = aws_s3_bucket.telemetry_archives.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "telemetry_archives_lifecycle" {
  bucket = aws_s3_bucket.telemetry_archives.id

  rule {
    id     = "archive_to_glacier"
    status = "Enabled"

    transition {
      days          = 90
      storage_class = "GLACIER"
    }
  }
}
