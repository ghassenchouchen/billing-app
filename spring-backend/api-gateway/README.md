# API Gateway

Central entry point for the Telecom Billing Platform microservices.

## Port: 8080

## Routes

| Path Pattern | Target Service | Port |
|--------------|----------------|------|
| `/api/customers/**`, `/api/auth/**` | customer-service | 8081 |
| `/api/services/**`, `/api/offres/**` | catalog-service | 8082 |
| `/api/subscriptions/**`, `/api/contrats/**` | subscription-service | 8083 |
| `/api/usage/**` | usage-service | 8084 |
| `/api/bills/**`, `/api/factures/**` | billing-service | 8085 |
| `/api/payments/**` | payment-service | 8086 |
| `/api/audit/**` | audit-service | 8087 |

## Running

```bash
mvn spring-boot:run
```

## Features

- Request routing to microservices
- JWT authentication filter
- Correlation ID propagation
- Request/response logging
- CORS configuration for Angular frontend
