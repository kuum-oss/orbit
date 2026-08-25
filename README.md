#  Orbit — Distributed IoT Fleet Monitoring Platform

Платформа для моніторингу флоту IoT-пристроїв (банкомати, POS-термінали, вендінгові автомати).  
Пристрої надсилають телеметрію → платформа детектує аномалії → оркеструє процеси обслуговування через BPMN → візуалізує стан флоту в реальному часі.

---

## Архітектура

```
┌─────────────┐     gRPC Stream      ┌──────────────────┐     Kafka      ┌─────────────────────┐
│ orbit-ingest │ ──────────────────► │ orbit-processor   │ ────────────► │ orbit-orchestrator   │
│ (WebFlux)    │                     │ (anomaly detect)  │               │ (Camunda BPMN)       │
└──────┬───────┘                     └──────────────────┘               └─────────────────────┘
       │                                                                          │
       │ REST/SSE                                                                 │
       ▼                                                                          ▼
┌──────────────┐                                                        ┌──────────────────┐
│ orbit-gateway │ ◄──── mTLS ────────────────────────────────────────── │ orbit-dashboard   │
│ (SCG + mTLS) │                                                        │ (Next.js + WS)    │
└──────────────┘                                                        └──────────────────┘
```

## Модулі

| Модуль | Опис | Порт |
|---|---|---|
| `orbit-ingest` | Приймає телеметрію (WebFlux + gRPC) | 8081 / 9090 |
| `orbit-processor` | Детекція аномалій, event-driven | 8082 |
| `orbit-orchestrator` | BPMN-процеси обслуговування (Camunda) | 8083 |
| `orbit-gateway` | Spring Cloud Gateway, mTLS termination | 8080 / 8443 |
| `orbit-dashboard` | Next.js, WebSocket live view | 3001 |

## Технології

| Категорія | Стек |
|---|---|
| Runtime | Java 25, Spring Boot 3, Spring WebFlux |
| Messaging | Apache Kafka (KRaft), gRPC bidirectional streaming |
| BPMN | Camunda 7 embedded engine |
| Security | mTLS (self-signed CA, PKCS12 keystores, Spring SSL bundles) |
| Cloud | AWS (LocalStack): S3, SQS, SNS |
| IaC | Terraform ≥ 1.5 |
| Database | PostgreSQL 16 |
| Orchestration | Docker Compose, Kubernetes (k3d), Helm |
| Monitoring | Prometheus + Grafana |
| Frontend | Next.js, WebSocket, camunda-bpmn-js |

---

## Швидкий старт

### Передумови

- Docker & Docker Compose
- OpenSSL (для генерації сертифікатів)
- Java 25+ (для збірки модулів)

### 1. Згенерувати mTLS сертифікати

```bash
chmod +x infra/certs/generate-certs.sh
./infra/certs/generate-certs.sh
```

Створює Root CA + сертифікати для кожного сервісу в `infra/certs/generated/`.

### 2. Запустити інфраструктуру

```bash
docker compose up -d
```

Це піднімає: LocalStack, PostgreSQL, Kafka, Prometheus, Grafana.

### 3. Запустити з аплікаціями (коли модулі готові)

```bash
docker compose --profile app up -d
```

### 4. Перевірити стан

| Сервіс | URL |
|---|---|
| LocalStack | http://localhost:4566 |
| PostgreSQL | localhost:5432 |
| Kafka | localhost:9092 |
| Prometheus | http://localhost:9091 |
| Grafana | http://localhost:3000 |

---

## Структура проєкту

```
orbit/
├── infra/
│   ├── terraform/          # IaC: S3, SQS, SNS на LocalStack
│   │   ├── main.tf         # AWS provider → LocalStack
│   │   ├── s3.tf           # orbit-telemetry-archives (versioning + Glacier)
│   │   ├── sqs.tf          # telemetry-queue + DLQ (redrive)
│   │   ├── sns.tf          # critical-alerts + SQS subscription
│   │   ├── variables.tf
│   │   ├── outputs.tf
│   │   └── terraform.tfvars
│   ├── certs/
│   │   ├── generate-certs.sh   # mTLS CA + service certs generation
│   │   └── openssl.cnf
│   ├── localstack/
│   │   └── init-aws.sh     # Ініціалізація AWS ресурсів при старті
│   └── prometheus/
│       └── prometheus.yml   # Scrape config для всіх сервісів
├── tests/
│   └── test-phase1.sh      # Тести інфраструктури (75 перевірок)
├── docker-compose.yml       # Повний dev-стек
├── pom.xml                  # Maven parent POM
└── README.md
```

---

## Terraform (LocalStack)

```bash
cd infra/terraform
terraform init
terraform plan
terraform apply -auto-approve
```

Створює:
- **S3**: `orbit-telemetry-archives` (versioning, Glacier lifecycle 90d)
- **SQS**: `orbit-telemetry-queue` → DLQ з redrive (maxReceiveCount=3)
- **SNS**: `orbit-critical-alerts` → SQS subscription

---

## mTLS

Кожен сервіс автентифікується через взаємні TLS-сертифікати:

```
Root CA (orbit-ca, 3650 днів)
├── orbit-ingest.p12      (SAN: orbit-ingest, localhost, *.orbit.local)
├── orbit-processor.p12
├── orbit-orchestrator.p12
├── orbit-gateway.p12
└── truststore.p12        (спільний, містить CA cert)
```

Пароль keystores: `orbit-dev` (змінюється через `CERT_PASSWORD`).

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

## BPMN-процес

```
[Telemetry Anomaly]
    → Severity Check
        HIGH/CRITICAL → Auto-create Ticket → Assign Technician → Wait Confirmation → Close
        LOW/MEDIUM → Aggregate (15min) → Re-evaluate → (escalate or discard)
```

---

## Тестування

```bash
./tests/test-phase1.sh
```

Покриття: структура файлів, Terraform конфігурація, mTLS генерація (з реальним OpenSSL), Docker Compose валідація, Prometheus, LocalStack init.

---

## Фази реалізації

- [x] **Фаза 1** — Інфраструктура: Terraform + LocalStack, mTLS CA, Docker Compose
- [ ] **Фаза 2** — orbit-ingest (WebFlux + gRPC) + orbit-processor, Kafka pipeline
- [ ] **Фаза 3** — orbit-orchestrator: Camunda engine, BPMN, delegates
- [ ] **Фаза 4** — orbit-gateway: mTLS termination, routing
- [ ] **Фаза 5** — Kubernetes: k3d, Helm charts, HPA
- [ ] **Фаза 6** — orbit-dashboard: Next.js + WebSocket + BPMN viewer
