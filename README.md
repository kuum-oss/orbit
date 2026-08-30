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

> Стан репозиторію: завершені фази 1–3. `orbit-gateway`, `orbit-dashboard`, Kubernetes/Helm та runtime-налаштування mTLS ще не реалізовані. Вони описані в архітектурі як цільовий стан, а не як доступні сервіси.

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

- Docker Engine, запущений локально, та Docker Compose v2+
- OpenSSL (для генерації сертифікатів)
- Java 25 і Maven 3.9+ (для запуску перевірок поза Docker)

Перевірка інструментів:

```bash
docker --version
docker compose version
java --version
mvn --version
```

Якщо `docker compose up` повертає `permission denied ... docker.sock`, Docker Engine не запущено або поточний користувач не має доступу до його socket. Це проблема локального Docker-оточення, а не застосунку.

### 1. Згенерувати mTLS сертифікати

```bash
chmod +x infra/certs/generate-certs.sh
./infra/certs/generate-certs.sh
```

Створює Root CA + сертифікати для кожного сервісу в `infra/certs/generated/`.

Каталог є локальним і навмисно не потрапляє до Git. Повторний запуск перегенеровує dev-сертифікати; не використовуйте їх у production.

### 2. Запустити інфраструктуру

```bash
docker compose up -d
docker compose ps
```

Це піднімає: LocalStack, PostgreSQL, Kafka, Prometheus, Grafana. Дочекайтеся стану `healthy` у `docker compose ps` перед запуском застосунків: Kafka і PostgreSQL мають healthcheck-и, а старт одразу після створення контейнерів може дати тимчасові помилки з'єднання.

### 3. Запустити готові аплікації

```bash
docker compose --profile app up -d
docker compose ps
```

Профіль `app` запускає `orbit-ingest`, `orbit-processor` і `orbit-orchestrator`. Сертифікати монтуються з `infra/certs/generated` у режимі лише читання. Gateway буде додано до цього шляху після завершення фази 4.

Перший запуск збирає образи Maven і може тривати довше. Для діагностики конкретного сервісу:

```bash
docker compose logs -f orbit-orchestrator
```

### 4. Перевірити стан

| Сервіс | URL |
|---|---|
| LocalStack | http://localhost:4566 |
| PostgreSQL | localhost:5432 |
| Kafka | localhost:9092 |
| Prometheus | http://localhost:9091 |
| Grafana | http://localhost:3000 |
| orbit-ingest | http://localhost:8081/actuator/health |
| orbit-processor | http://localhost:8082/actuator/health |
| orbit-orchestrator | http://localhost:8083/actuator/health |

`orbit-gateway` на портах 8080/8443 і dashboard на 3001 на цьому етапі не запускаються.

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

Скрипт генерує матеріали для взаємної TLS-автентифікації:

```
Root CA (orbit-ca, 3650 днів)
├── orbit-ingest.p12      (SAN: orbit-ingest, localhost, *.orbit.local)
├── orbit-processor.p12
├── orbit-orchestrator.p12
├── orbit-gateway.p12
└── truststore.p12        (спільний, містить CA cert)
```

Пароль keystores: `orbit-dev` (змінюється через `CERT_PASSWORD`).

Важливо: на завершених фазах сертифікати генеруються й монтуються в контейнери, але TLS termination і Spring SSL bundles ще не ввімкнені в runtime-конфігурації. Реальна перевірка mTLS між сервісами є завданням фази 4; поточні HTTP-приклади нижче використовують локальний `http://`.

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

### Наскрізний сценарій для локального користувача

Після того як усі контейнери профілю `app` стали готовими, надішліть тестову HIGH-аномалію прямо в Kafka. Це скорочений, але реальний шлях до оркестратора: Kafka consumer → Camunda → PostgreSQL → REST API.

```bash
docker compose exec -T kafka kafka-console-producer \
  --bootstrap-server kafka:29092 \
  --topic anomaly-events <<'EOF'
{"eventId":"a7e9e7ab-5c43-4b92-9b9e-1029384756aa","deviceId":"device-42","severity":"HIGH","detectedAt":"2026-08-30T12:00:00Z","ruleTriggered":"TEMPERATURE_THRESHOLD","description":"Temperature exceeded safe range","telemetryValue":91.5,"metricType":"TEMPERATURE"}
EOF
```

Перегляньте створений ticket:

```bash
curl -fsS 'http://localhost:8083/api/v1/tickets?deviceId=device-42'
```

У відповіді ticket має стан `WAITING_CONFIRMATION` і заповнене поле `assignedTechnician`. Якщо встановлено `jq`, можна взяти ідентифікатор першого ticket:

```bash
TICKET_ID=$(curl -fsS 'http://localhost:8083/api/v1/tickets?deviceId=device-42' | jq -r '.[0].ticketId')
```

Підтвердіть виконання робіт через публічний API та перевірте фінальний стан:

```bash
curl -fsS -X POST "http://localhost:8083/api/v1/tickets/${TICKET_ID}/confirm" \
  -H 'Content-Type: application/json' \
  -d '{"technicianId":"tech-thermo-alpha","notes":"Replaced cooling fan"}'

curl -fsS "http://localhost:8083/api/v1/tickets/${TICKET_ID}"
```

Очікуваний результат: відповідь на `confirm` має `CONFIRMED_AND_COMPLETED`, а останній `GET` — `status: "CLOSED"` та передані `resolutionNotes`. Ім'я техніка має відповідати фактично призначеному `assignedTechnician`; у прикладі використовується перший технік температурного пулу.

Низькі та середні аномалії не створюють ticket одразу: BPMN чекає 15 хвилин, агрегує події для пристрою й лише після перевищення порогу ескалує їх. Для швидкої ручної перевірки використовуйте `HIGH` або `CRITICAL`.

### Корисні API оркестратора

| Метод | Шлях | Призначення |
|---|---|---|
| `GET` | `/api/v1/tickets` | Усі tickets; підтримує `deviceId` і `status` |
| `GET` | `/api/v1/tickets/{ticketId}` | Один ticket |
| `GET` | `/api/v1/tickets/stats` | Статистика за станом і severity |
| `POST` | `/api/v1/tickets/{ticketId}/assign` | Ручне призначення, тіло: `{"technicianId":"..."}` |
| `POST` | `/api/v1/tickets/{ticketId}/confirm` | Підтвердження й закриття BPMN-процесу |
| `POST` | `/api/v1/tickets/{ticketId}/close` | Ручне закриття, тіло: `{"notes":"..."}` |

---

## Тестування

```bash
./tests/test-phase1.sh
mvn -pl orbit-orchestrator -am test
```

Покриття: структура файлів, Terraform конфігурація, mTLS генерація (з реальним OpenSSL), Docker Compose валідація, Prometheus, LocalStack init; а також наскрізний BPMN-сценарій Kafka event → HIGH ticket → technician → HTTP confirmation → `CLOSED`.

Інтеграційний тест оркестратора використовує H2, вбудований Camunda engine і `MockMvc`; Kafka listener у ньому вимкнено, а handler викликається безпосередньо. Тому тест перевіряє доменний і HTTP-потік без потреби у Docker, але не замінює ручний Compose-сценарій вище.

## Зупинка та очищення

```bash
docker compose --profile app down
```

Команда зупиняє контейнери, але зберігає дані PostgreSQL і Kafka у Docker volumes. Для повного скидання локальних даних використовуйте `docker compose --profile app down -v`; це незворотно видалить tickets, Camunda history і Kafka-дані.

## Типові проблеми

| Симптом | Що перевірити |
|---|---|
| `permission denied ... docker.sock` | Запустіть Docker Desktop/Engine і надайте користувачу доступ до Docker socket. |
| Контейнер застосунку завершується одразу | `docker compose logs orbit-orchestrator`; перевірте готовність PostgreSQL/Kafka та наявність `infra/certs/generated`. |
| Немає ticket після повідомлення | Переконайтеся, що topic — `anomaly-events`, JSON містить коректні UUID, ISO-8601 `detectedAt` і `severity` з `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`. Для `LOW`/`MEDIUM` ticket не з'являється негайно. |
| `confirm` повертає конфлікт | Ticket уже закритий або процес не перебуває на кроці `Wait Technician Confirmation`. Спочатку перевірте `GET /api/v1/tickets/{ticketId}`. |
| Порт уже зайнятий | Зупиніть попередні контейнери або змініть host-port у `docker-compose.yml`. |

---

## Фази реалізації

- [x] **Фаза 1** — Інфраструктура: Terraform + LocalStack, mTLS CA, Docker Compose
- [x] **Фаза 2** — orbit-ingest (WebFlux + gRPC) + orbit-processor, Kafka pipeline
- [x] **Фаза 3** — orbit-orchestrator: Camunda engine, BPMN, delegates
- [ ] **Фаза 4** — orbit-gateway: mTLS termination, routing
- [ ] **Фаза 5** — Kubernetes: k3d, Helm charts, HPA
- [ ] **Фаза 6** — orbit-dashboard: Next.js + WebSocket + BPMN viewer
