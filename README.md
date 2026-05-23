# Kafka + DynamoDB Local Demo

Local demo with:

- Kafka
- DynamoDB Local
- DynamoDB Admin
- Kafka UI
- `user-service` in Spring Boot + WebFlux
- `delivery-service` in Spring Boot + WebFlux
- Gradle builds
- Dockerfiles for both Java services

## Prerequisites

You need:

- Docker or Colima
- `docker-compose`
- Java 17 if you want to run `delivery-service` from IntelliJ
- Optional: `jq`
- Optional: AWS CLI for checking DynamoDB Local from the terminal

## Start the stack

This project intentionally recommends:

```bash
docker-compose up --build --force-recreate
```

because, when changing Java services, you want the images recreated again instead of relying on stale layers or old containers.

If you want detached mode:

```bash
docker-compose up -d --build --force-recreate
```

## Stop the stack

```bash
docker-compose down
```

To also remove anonymous volumes:

```bash
docker-compose down -v
```

## Services and ports

- user-service: `http://localhost:8081`
- delivery-service: `http://localhost:8082`
- Kafka UI: `http://localhost:8085`
- DynamoDB Admin: `http://localhost:8001`
- DynamoDB Local: `http://localhost:8000`
- Kafka external listener from host: `localhost:9094`

## How the demo works

1. `user-service` receives an HTTP request to create an order.
2. It stores the order in DynamoDB.
3. It stores an outbox event in DynamoDB.
4. It publishes the order event to Kafka.
5. `delivery-service` consumes the event from Kafka.
6. `delivery-service` applies the order workflow depending on the order type.
7. You can later rate the order with another HTTP request.

## Useful UIs

### Kafka UI

Open:

```text
http://localhost:8085
```

Look at topic:

```text
order-events
```

### DynamoDB Admin

Open:

```text
http://localhost:8001
```

Look for tables such as:

- `Orders`
- `Outbox`

## Running `delivery-service` from IntelliJ for debugging

Keep infrastructure in Docker, but stop the containerized `delivery-service`.

Environment variables for IntelliJ:

```text
AWS_ACCESS_KEY_ID=local
AWS_SECRET_ACCESS_KEY=local
AWS_REGION=us-east-1
DYNAMODB_ENDPOINT=http://localhost:8000
KAFKA_BOOTSTRAP_SERVERS=localhost:9094
KAFKA_SECURITY_PROTOCOL=PLAINTEXT
KAFKA_MAX_ATTEMPTS=3
KAFKA_RETRY_INTERVAL_MS=0
SERVER_PORT=8082
```

Important:

- If `delivery-service` runs from IntelliJ, stop the Docker `delivery-service` first.
- Kafka from your host should use `localhost:9094`.
- DynamoDB from your host should use `http://localhost:8000`.

## Test scenarios with curl

## 1. Create a NORMAL order

```bash
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "user-001",
    "type": "NORMAL"
  }'
```

## 2. Create a PRIORITY order

```bash
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "user-002",
    "type": "PRIORITY"
  }'
```

## 3. Create a PICKUP order

```bash
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "user-003",
    "type": "PICKUP"
  }'
```

## 4. Create a NO_PREPARATION order

```bash
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "user-004",
    "type": "NO_PREPARATION"
  }'
```

## 5. Create an order and store the returned orderId in a variable

Requires `jq`:

```bash
ORDER_ID=$(curl -s -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "user-005",
    "type": "NORMAL"
  }' | jq -r '.orderId')

echo "$ORDER_ID"
```

## 6. Rate an order with five stars

```bash
curl -X POST http://localhost:8082/orders/$ORDER_ID/rating \
  -H "Content-Type: application/json" \
  -d '{
    "rating": 5
  }' -i
```

## 7. Rate an order with one star

```bash
curl -X POST http://localhost:8082/orders/$ORDER_ID/rating \
  -H "Content-Type: application/json" \
  -d '{
    "rating": 1
  }' -i
```

## 8. Rate a non-existing order

```bash
curl -X POST http://localhost:8082/orders/not-found-order/rating \
  -H "Content-Type: application/json" \
  -d '{
    "rating": 3
  }' -i
```

## 9. Send an invalid order type

Valid types are:

- `NORMAL`
- `PRIORITY`
- `PICKUP`
- `NO_PREPARATION`

This should fail:

```bash
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "user-006",
    "type": "EXPRESS"
  }' -i
```

## 10. Send a malformed request body

```bash
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "user-007"
  }' -i
```

## 11. Send invalid JSON

```bash
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "user-008",
    "type": "NORMAL",
  }' -i
```

## 12. Run all valid order types quickly

```bash
curl -X POST http://localhost:8081/orders -H "Content-Type: application/json" -d '{"customerId":"bulk-1","type":"NORMAL"}'
echo
curl -X POST http://localhost:8081/orders -H "Content-Type: application/json" -d '{"customerId":"bulk-2","type":"PRIORITY"}'
echo
curl -X POST http://localhost:8081/orders -H "Content-Type: application/json" -d '{"customerId":"bulk-3","type":"PICKUP"}'
echo
curl -X POST http://localhost:8081/orders -H "Content-Type: application/json" -d '{"customerId":"bulk-4","type":"NO_PREPARATION"}'
echo
```

## 13. Full happy path

```bash
ORDER_ID=$(curl -s -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"demo-user","type":"NORMAL"}' | jq -r '.orderId')

curl -X POST http://localhost:8082/orders/$ORDER_ID/rating \
  -H "Content-Type: application/json" \
  -d '{"rating":5}' -i
```

## Watching logs

All main services:

```bash
docker-compose logs -f user-service delivery-service kafka dynamodb
```

Only Kafka:

```bash
docker-compose logs -f kafka
```

Only delivery-service:

```bash
docker-compose logs -f delivery-service
```

## Kafka inspection from the terminal

Consume messages from the topic:

```bash
docker exec -it kafka bash
```

Then inside the container:

```bash
kafka-console-consumer \
  --bootstrap-server kafka:9092 \
  --topic order-events \
  --from-beginning
```

Describe the topic:

```bash
kafka-topics --bootstrap-server kafka:9092 --describe --topic order-events
```

## DynamoDB inspection from the terminal

List tables:

```bash
aws dynamodb list-tables \
  --endpoint-url http://localhost:8000 \
  --region us-east-1
```

Scan orders:

```bash
aws dynamodb scan \
  --table-name Orders \
  --endpoint-url http://localhost:8000 \
  --region us-east-1
```

Scan outbox:

```bash
aws dynamodb scan \
  --table-name Outbox \
  --endpoint-url http://localhost:8000 \
  --region us-east-1
```

## Notes

- If DynamoDB Local is started with `-inMemory`, data disappears when the container stops.
- If you are debugging `delivery-service` from IntelliJ, make sure the Docker `delivery-service` is not running at the same time.
- Kafka UI runs inside Docker and should connect to `kafka:9092`.
- Your host machine should use `localhost:9094` for Kafka when accessing Kafka directly outside Docker.

## Recommended demo flow

1. Start the stack with:

```bash
docker-compose up --build --force-recreate
```

2. Open:
   - `http://localhost:8085`
   - `http://localhost:8001`

3. Create orders using the curls above.
4. Watch messages in Kafka UI.
5. Watch records in DynamoDB Admin.
6. Rate an order.
7. Observe logs.
