# Stripe Payment Demo

Spring Boot payment demo with JWT authentication, order management, Stripe checkout, webhook verification, refund-safe cancellation, Redis-backed duplicate protection, and Kafka event publishing.

## Features

- Stateless JWT-based authentication for signup and login
- Dedicated `user`, `order`, and `payment` APIs
- Stripe Checkout integration with webhook signature verification
- Payment confirmation from both webhook delivery and browser return flow
- Order cancellation with Stripe refund support
- Redis-backed guards for:
  - payment idempotency reservation
  - cancel request locking
  - webhook event deduplication
- Kafka event publishing for order and payment lifecycle updates
- Kafka consumer for recent event auditing
- Kafka-driven email notification hook for payment receipts and refund notifications
- Multi-page UI with `Home`, `Products`, `Cart`, `Profile`, and `Logout`

## Architecture

### Core flow

1. User signs up or logs in with JWT auth.
2. User creates a cart and starts checkout from the cart page.
3. The backend creates an order, then creates a payment record with an idempotency key.
4. Stripe Checkout opens for card payment.
5. On success, the app confirms the payment from:
   - Stripe webhook events
   - browser return flow fallback using `session_id`
6. Paid orders move to `CONFIRMED / SUCCEEDED`.
7. Users can cancel confirmed paid orders, which triggers a Stripe refund and updates order/payment state to `CANCELLED / REFUNDED`.

### Where Redis helps

Redis is used to reduce duplicate or conflicting operations:

- `payment:idempotency:{key}`
  prevents duplicate in-flight payment creation for the same idempotency key
- `order:cancel:{orderId}`
  prevents double-refund attempts from repeated cancel requests
- `stripe:webhook:event:{eventId}`
  prevents duplicate webhook processing for the same Stripe event

If Redis is unavailable, the app falls back gracefully so local development still works.

### Where Kafka helps

Kafka is used as the project’s event backbone:

- `order-events`
  receives events such as:
  - `ORDER_CREATED`
  - `ORDER_PAYMENT_PENDING`
  - `ORDER_CONFIRMED`
  - `ORDER_PAYMENT_FAILED`
  - `ORDER_CANCELLED`
- `payment-events`
  receives events such as:
  - `PAYMENT_CREATED`
  - `PAYMENT_SUCCEEDED`
  - `PAYMENT_FAILED`
  - `PAYMENT_REFUNDED`
  - `PAYMENT_CONFIRMED_FROM_RETURN`

A Kafka consumer stores the latest consumed messages in an in-memory audit buffer, exposed at:

- `GET /api/events/recent`

This makes the event-driven part of the system visible during demos and testing.

## Local Run

### 1. Start infrastructure

```bash
docker compose up -d
```

Services:

- Kafka: `localhost:9094`
- Redis: `localhost:6379`

### 2. Run the Spring Boot app

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8080
```

### 3. Open the app

```text
http://localhost:8080
```

## Important Config

See [application.properties](/Users/ajaybandi/Downloads/Stripe1/stripe-payment-main%202/src/main/resources/application.properties):

- `stripe.secretKey`
- `stripe.webhookSecret`
- `app.base-url`
- `spring.kafka.bootstrap-servers`
- `spring.data.redis.host`
- `app.kafka.topics.order-events`
- `app.kafka.topics.payment-events`

## Production on AWS EC2

Recommended AWS setup:

- EC2 for the Spring Boot app
- RDS MySQL for the database
- ElastiCache Redis for idempotency and locks
- Route 53 for `app.bharathiecommerce.com`
- ACM + Nginx for HTTPS

Files added for production:

- `src/main/resources/application-prod.properties`
- `deploy/stripe-payment.env.example`
- `deploy/nginx-app.bharathiecommerce.com.conf`
- `.github/workflows/main.yml`
- `deploy/deploy.sh`

Suggested EC2 steps:

1. Install Java 17 and Nginx on the EC2 instance.
2. Copy `deploy/stripe-payment.env.example` to `/etc/stripe-payment.env` and replace placeholders.
3. Point Route 53 `A` or `CNAME` record for `app.bharathiecommerce.com` to the EC2 public endpoint or load balancer.
4. Configure Nginx using `deploy/nginx-app.bharathiecommerce.com.conf`.
5. Add TLS using ACM through a load balancer, or use Certbot on the instance if you terminate TLS on EC2.
6. Add GitHub repository secrets:
   - `EC2_HOST`
   - `EC2_USER`
   - `EC2_SSH_PRIVATE_KEY`
7. Add GitHub repository variable:
   - `APP_PORT`
8. Push to `main` to trigger the deploy workflow.

Logging:

- Local default log file: `logs/stripe-payment.log`
- EC2 default log file: `/var/log/stripe-payment/application.log`
- Override with `LOG_FILE_NAME` in `/etc/stripe-payment.env`

Stripe production checklist:

- Set `APP_BASE_URL=https://app.bharathiecommerce.com`
- Configure Stripe live webhook:
  - `https://app.bharathiecommerce.com/api/payments/webhook/stripe`
- Set `STRIPE_SECRET_KEY` and `STRIPE_WEBHOOK_SECRET` in `/etc/stripe-payment.env`
- Do not use ngrok in production

## Useful Demo Endpoints

- `POST /auth/register`
- `POST /auth/login`
- `GET /api/users/me`
- `POST /api/orders`
- `GET /api/orders`
- `POST /api/orders/{orderId}/cancel`
- `POST /api/payments`
- `POST /api/payments/confirm/{sessionId}`
- `POST /api/payments/webhook/stripe`
- `GET /api/events/recent`

## Resume Alignment

This project now demonstrates:

- stateless Spring Boot APIs for user, order, and payment domains
- Stripe webhook handling with signature verification
- idempotent and retry-safe payment creation
- duplicate-safe webhook and cancel processing
- event-driven extensibility through Kafka
- short-lived duplicate protection through Redis
- event-triggered email receipt and refund notification flow via Kafka consumer
