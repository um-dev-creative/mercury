---
name: Kafka CLI
description: Tool for Kafka topic management and message inspection for Mercury local development
type: terminal
command-prefix: kafka-topics.sh
used-by: [devops-engineer, developer]
---

## Purpose

Manages Kafka topics and consumer groups for local Mercury development and debugging. Mercury uses three primary topics for multi-channel message dispatch.

## Mercury Topics

| Topic | Channel | Consumer Service |
|---|---|---|
| `email-topic` | EMAIL | `EmailChannelService` |
| `sms-topic` | SMS | `SmsChannelService` |
| `telegram-topic` | TELEGRAM | `TelegramChannelService` |

Consumer group: `mercury-multi-channel` (default, set via `${prx.consumer.group-id}`)

## Available Commands

### Create topics (local dev)
```bash
kafka-topics.sh --create --topic email-topic --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
kafka-topics.sh --create --topic sms-topic --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
kafka-topics.sh --create --topic telegram-topic --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
```

### List topics
```bash
kafka-topics.sh --list --bootstrap-server localhost:9092
```

### Describe topic
```bash
kafka-topics.sh --describe --topic email-topic --bootstrap-server localhost:9092
```

### Consume messages (debug)
```bash
kafka-console-consumer.sh --topic email-topic --from-beginning --bootstrap-server localhost:9092
```

### Check consumer group lag
```bash
kafka-consumer-groups.sh --describe --group mercury-multi-channel --bootstrap-server localhost:9092
```

### Delete topic (reset for local dev)
```bash
kafka-topics.sh --delete --topic email-topic --bootstrap-server localhost:9092
```

## Notes

- `PRX_KAFKA_AUTO_STARTUP=false` by default in `bootstrap.yml` — Mercury listeners do NOT auto-start locally without a broker
- Set `PRX_KAFKA_AUTO_STARTUP=true` to enable listeners when a local broker is available
- Bootstrap server for local dev: `localhost:9092` (configure via `${BOOTSTRAP_SERVER_URI}:${BOOTSTRAP_SERVER_PORT}`)
- For local dev with Docker: `docker run -p 9092:9092 apache/kafka:latest`
