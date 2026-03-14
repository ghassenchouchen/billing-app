# Subscription Service

Subscription and contract management microservice for the Telecom Billing Platform.

## Port: 8083

## Database: db_subscription

Tables:
- `contrat` - Customer subscriptions/contracts

## API Endpoints

### Subscriptions
- `GET /subscriptions` - List all subscriptions
- `GET /subscriptions/active` - List active subscriptions
- `GET /subscriptions/{id}` - Get subscription by ID
- `GET /subscriptions/client/{clientId}` - Get subscriptions by customer
- `POST /subscriptions` - Create subscription
- `POST /subscriptions/{id}/activate` - Activate subscription
- `POST /subscriptions/{id}/suspend` - Suspend subscription
- `POST /subscriptions/{id}/terminate` - Terminate subscription

## REST Clients (Feign)

- `customer-service` - Validate customer exists
- `catalog-service` - Validate offer exists

## Kafka Events Published

- `billing.subscription.created`
- `billing.subscription.activated`
- `billing.subscription.suspended`
- `billing.subscription.terminated`

## Running

```bash
mvn spring-boot:run
```
