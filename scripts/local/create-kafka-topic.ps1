<#
  Creates topic transaction-events (matches CreditFlow default).
  Requires: Kafka installed, broker at localhost:9092, kafka-topics on PATH.

  Usage:
    .\scripts\local\create-kafka-topic.ps1
    .\scripts\local\create-kafka-topic.ps1 -BootstrapServer localhost:9093
#>
param(
  [string]$BootstrapServer = "localhost:9092",
  [string]$Topic = "transaction-events"
)

$ErrorActionPreference = "Stop"

$kafkaTopics = $null
foreach ($name in @("kafka-topics.bat", "kafka-topics.cmd", "kafka-topics")) {
  $kafkaTopics = Get-Command $name -ErrorAction SilentlyContinue
  if ($kafkaTopics) { break }
}

if (-not $kafkaTopics) {
  Write-Host "kafka-topics not found on PATH. Add Kafka's bin\windows directory to PATH, or run:" -ForegroundColor Yellow
  Write-Host "  kafka-topics.bat --bootstrap-server $BootstrapServer --create --if-not-exists --topic $Topic --partitions 1 --replication-factor 1" -ForegroundColor Gray
  exit 1
}

& $kafkaTopics.Source --bootstrap-server $BootstrapServer --create --if-not-exists --topic $Topic --partitions 1 --replication-factor 1
if ($LASTEXITCODE -ne 0) { throw "kafka-topics failed" }
Write-Host "Topic '$Topic' ready on $BootstrapServer" -ForegroundColor Green
