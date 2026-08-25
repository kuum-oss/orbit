Дивлюся що вже є через Vault, і що з реальних вакансій дивились — щоб закрити конкретні gap: AWS/GCP production, Kubernetes, gRPC, Camunda, WebFlux глибоко, реактивне програмування, mTLS.

---

# `orbit` — розподілена платформа моніторингу IoT-флоту

Один сервіс з реальною доменною логікою, який закриває залишкові gap через продукт а не вправи.

---

## Концепція

**Orbit** — платформа для моніторингу флоту IoT-пристроїв (умовно: банкомати, POS-термінали, вендінгові автомати). Реальний домен: пристрої надсилають телеметрію, платформа детектує аномалії, оркеструє процеси обслуговування через BPMN, і візуалізує стан флоту в реальному часі.

---

## Що покриває

| Gap | Як закривається |
|---|---|
| AWS (production-style) | LocalStack Pro-режим: EKS-подібний деплой через k3d, S3, SQS, SNS, RDS-сумісний Postgres |
| Kubernetes | Повний k3d/kind кластер в Docker, Helm charts, HPA, readiness/liveness probes |
| Camunda | BPMN-процес обслуговування пристрою: детекція → тікет → призначення → підтвердження |
| WebFlux глибоко | Reactive Streams end-to-end: SSE для live-телеметрії, backpressure на вході |
| gRPC | Внутрішня комунікація між orbit-ingest і orbit-processor через gRPC замість REST |
| mTLS | Взаємна автентифікація між усіма внутрішніми сервісами через self-signed CA |
| Terraform | IaC для локальної інфраструктури (LocalStack + k3d), reproducible from scratch |
| Візуалізація | Grafana dashboard + власний React/Next.js real-time dashboard через WebSocket |

---

## Модулі

```
orbit/
├── orbit-ingest/          # WebFlux, приймає телеметрію (gRPC + REST)
├── orbit-processor/        # аномалія-детекція, event-driven
├── orbit-orchestrator/     # Camunda BPMN процеси обслуговування
├── orbit-gateway/          # Spring Cloud Gateway, mTLS termination
├── orbit-dashboard/        # Next.js, WebSocket live view
├── infra/
│   ├── terraform/          # LocalStack + k3d provisioning
│   ├── k8s/                # Helm charts
│   └── certs/              # mTLS CA + service certs generation script
└── docker-compose.yml      # для локального dev без k8s
```

---

## Домен

```java
// DeviceTelemetry
deviceId, timestamp, metricType, value, location

// AnomalyEvent
eventId, deviceId, severity (LOW/MEDIUM/HIGH/CRITICAL), detectedAt, ruleTriggered

// MaintenanceTicket (Camunda process instance)
ticketId, deviceId, status (BPMN state), assignedTechnician, createdFromEvent
```

---

## BPMN-процес (Camunda)

```
[Telemetry Anomaly] 
    → Gateway: Severity Check
        HIGH/CRITICAL → Auto-create Ticket → Assign Technician → Wait Confirmation → Close
        LOW/MEDIUM → Aggregate (wait 15min) → Re-evaluate → (escalate or discard)
```

Кожен крок BPMN — реальний Java delegate, не заглушка.

---

## gRPC контракт (orbit-ingest ↔ orbit-processor)

```protobuf
service TelemetryProcessor {
  rpc StreamTelemetry(stream TelemetryPoint) returns (stream ProcessingAck);
}
```

Двонаправлений streaming — реальне навантаження на розуміння gRPC, не toy example.

---

## mTLS

- Власний CA генерується скриптом (OpenSSL)
- Кожен сервіс отримує сертифікат при старті контейнера
- Spring Boot SSL bundles для мутуальної автентифікації між orbit-gateway ↔ orbit-ingest ↔ orbit-processor
- Zero trust: сервіс без валідного сертифіката не проходить handshake

---

## Kubernetes (k3d в Docker)

```yaml
# приклад: orbit-processor deployment
readinessProbe: /actuator/health/readiness
livenessProbe: /actuator/health/liveness
HPA: scale 2-6 replicas based on CPU + custom metric (queue depth)
```

Helm chart на кожен сервіс, `helm install orbit ./charts` піднімає весь кластер локально.

---

## Terraform (LocalStack)

```hcl
resource "aws_s3_bucket" "orbit_archives" { ... }
resource "aws_sqs_queue" "telemetry_dlq" { ... }
resource "aws_sns_topic" "critical_alerts" { ... }
```

`terraform apply` проти `localstack` endpoint — реальний AWS-workflow, нуль коштів.

---

## Візуалізація

**Grafana:** метрики флоту, latency gRPC stream, Camunda process duration

**orbit-dashboard (Next.js):**
- Live-карта пристроїв (WebSocket push)
- Timeline аномалій
- BPMN process viewer (camunda-bpmn-js вбудований) — бачиш живий стан кожного тікета на діаграмі процесу

---

## Docker Compose (все одразу)

```yaml
services:
  localstack:
  k3d-cluster:        # опційно, або через docker-in-docker
  postgres:
  kafka:               # переносиш з Vault, той самий стек
  orbit-ingest:
  orbit-processor:
  orbit-orchestrator:  # + Camunda engine
  orbit-gateway:
  orbit-dashboard:
  grafana:
  prometheus:
```

Один `docker compose up` — весь orbit піднімається, mTLS хендшейки проходять, BPMN процес видно в браузері.

---

## Порядок реалізації

**Фаза 1** — інфра: Terraform + LocalStack, mTLS CA generation, docker-compose skeleton

**Фаза 2** — orbit-ingest (WebFlux + gRPC) + orbit-processor, Kafka pipeline

**Фаза 3** — orbit-orchestrator: Camunda embedded engine, BPMN diagram, delegates

**Фаза 4** — orbit-gateway з mTLS termination, routing до всіх сервісів

**Фаза 5** — k3d Kubernetes deploy, Helm charts, HPA

**Фаза 6** — orbit-dashboard: Next.js + WebSocket + camunda-bpmn-js viewer

---
