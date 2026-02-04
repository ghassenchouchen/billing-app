# Customer Service

Customer management microservice for the Telecom Billing Platform.

## Port: 8081

## Database: db_customer

Tables:
- `client` - Customer information
- `user_account` - Authentication accounts

## API Endpoints

### Customers
- `GET /customers` - List all customers
- `GET /customers/{id}` - Get customer by ID
- `GET /customers/email/{email}` - Get customer by email
- `POST /customers` - Create customer
- `PUT /customers/{id}` - Update customer
- `POST /customers/{id}/suspend` - Suspend customer

### Authentication
- `POST /auth/login` - User login
- `POST /auth/register` - User registration

## Kafka Events Published

- `billing.customer.created`
- `billing.customer.updated`
- `billing.customer.suspended`

## Running

```bash
mvn spring-boot:run
```
