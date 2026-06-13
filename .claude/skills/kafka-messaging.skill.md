---
name: kafka-messaging
used-by: [developer, devops-engineer]
version: 1.0
---

## Kafka Messaging Shared Skill — Mercury Multi-Channel Dispatch

### Topics
Defined in `bootstrap.yml`:
```yaml
prx:
  consumer:
    topics:
      telegram: telegram-topic
      sms: sms-topic
      email: email-topic
```
Never hardcode topic strings in Java source. Reference via `${prx.consumer.topics.email}` etc.

### Publish (CampaignServiceImpl)
One Kafka message per recipient:
```java
kafkaTemplate.send(channelType.topicName(), emailMessageTO);
```
The `topicName()` is derived from the `ChannelTypeEntity` which maps to the bootstrap.yml topic names.

### Consume (MultiChannelListener)
```java
@KafkaListener(topics = "${prx.consumer.mercury.topic}", groupId = "${prx.consumer.group-id}")
public void listen(ConsumerRecord<String, Object> record) {
    messageChannelRouter.route(record);
}
```
`MercuryEmailListener` handles `email-topic` specifically.

### Channel Router
`MessageChannelRouter` in `com.prx.mercury.kafka.router` dispatches to the correct `ChannelService` implementation based on message type:
- `EmailChannelService` → `EmailMessageTO`
- `SmsChannelService` → `SmsMessageTO`
- `TelegramChannelService` → `TelegramMessageTO`
- `WhatsAppChannelService` → `WhatsAppMessageTO`

### Kafka TOs (com.prx.mercury.kafka.to)
- `EmailMessageTO` — email channel
- `SmsMessageTO` — SMS channel
- `TelegramMessageTO` — Telegram channel
- `WhatsAppMessageTO` — WhatsApp channel
- `PushNotificationMessageTO` — push channel

### Auto-Startup Toggle
`PRX_KAFKA_AUTO_STARTUP=false` by default — prevents listener container crash in local dev without a broker.

In `bootstrap.yml`:
```yaml
prx:
  kafka:
    enabled: ${PRX_KAFKA_ENABLED:true}
    auto-startup: ${PRX_KAFKA_AUTO_STARTUP:false}
```

Set `PRX_KAFKA_AUTO_STARTUP=true` in deployed environments.

### New Channel Service Implementation
Any new channel (e.g., PUSH) must:
1. Create a `*MessageTO` record in `com.prx.mercury.kafka.to`
2. Create a `*ChannelService` implementing `ChannelService<*MessageTO>` in `com.prx.mercury.kafka.consumer.service`
3. Register in `MessageChannelRouter`
4. Add topic to `bootstrap.yml` under `prx.consumer.topics.*`
5. Add `ChannelType` enum value in `com.prx.mercury.constant.ChannelType`

### ConsumerConfig
`com.prx.mercury.kafka.config.ConsumerConfig` — Kafka consumer factory configuration. Group ID: `${prx.consumer.group-id:mercury-multi-channel}`.
