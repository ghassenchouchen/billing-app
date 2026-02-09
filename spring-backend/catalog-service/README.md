# Catalog Service

Product catalog microservice for the Telecom Billing Platform.

## Port: 8082

## Database: db_catalog

Tables:
- `service` - Telecom services (voice, data, SMS, etc.)
- `offre` - Service packages/offers
- `offre_service` - Many-to-many relationship

## API Endpoints

### Services
- `GET /services` - List all services
- `GET /services/active` - List active services
- `GET /services/{id}` - Get service by ID
- `GET /services/code/{code}` - Get service by code
- `POST /services` - Create service
- `PUT /services/{id}` - Update service

### Offers
- `GET /offres` - List all offers
- `GET /offres/active` - List active offers
- `GET /offres/{id}` - Get offer by ID
- `POST /offres` - Create offer

## Kafka Events Published

- `billing.catalog.service.created`
- `billing.catalog.service.updated`
- `billing.catalog.offer.created`

## Running

```bash
mvn spring-boot:run
```
