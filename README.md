# Checkout Studio

> A SaaS-style merchant operations and payment platform prototype built with Spring Boot, Stripe, Redis, and AWS.

Checkout Studio is a full-stack ecommerce and payment workflow application that demonstrates secure checkout, Stripe integration, order lifecycle management, tenant-aware merchant workspaces, inventory tracking, analytics, and cloud deployment readiness.

## Overview

This project was designed to go beyond a basic CRUD demo by combining:

- secure JWT-based authentication
- Stripe Checkout and webhook-driven payment confirmation
- tenant-aware merchant workspaces
- role-based access control
- inventory and analytics modules
- AWS deployment workflow

It is positioned as a strong backend/full-stack portfolio project with real business workflow coverage.

---

## Key Features

- JWT-based authentication and session-aware frontend flow
- Stripe Checkout integration
- Stripe webhook handling for payment confirmation and refunds
- Order lifecycle tracking
- Tenant-aware merchant workspaces
- Role-based access for `OWNER`, `MANAGER`, and `CASHIER`
- Inventory management
- Sales analytics dashboard
- Redis integration support
- Kafka-ready event-driven architecture
- AWS EC2 deployment workflow with GitHub Actions

---

## Tech Stack

### Backend

- Java 17
- Spring Boot 3
- Spring Security
- Spring Data JPA
- Maven

### Data & Messaging

- MySQL / H2
- Redis
- Kafka-ready event design

### Frontend

- Thymeleaf
- HTML
- CSS
- JavaScript

### Payments & Cloud

- Stripe API
- GitHub Actions
- AWS EC2
- AWS RDS
- AWS ElastiCache
- Nginx

---

## Role Model

### Owner

- Manage inventory
- View analytics
- Cancel or refund eligible orders
- Full merchant workspace visibility

### Manager

- View analytics
- Cancel or refund eligible orders
- Operational access without owner-level inventory creation controls

### Cashier

- Login and process normal order and checkout flow
- Restricted from analytics and refund management
- Limited operational permissions

---

## Core Modules

- Authentication and user management
- Product browsing and cart
- Checkout and payment processing
- Order tracking
- Refund handling
- Tenant management
- Inventory management
- Analytics dashboard
- Event audit visibility

---

## Project Structure

```text
src/main/java/com/javatechie
├── controller/
├── dto/
├── model/
├── repository/
└── service/

src/main/resources
├── static/
├── templates/
├── application.properties
└── application-prod.properties

deploy/
.github/workflows/
```

---

## Local Setup

### Prerequisites

- Java 17
- Maven
- Stripe test keys

### Run Locally

```bash
mvn spring-boot:run
```

Or on a custom port:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

---

## Configuration

Example runtime configuration:

```properties
stripe.secretKey=${STRIPE_SECRET_KEY:}
stripe.webhookSecret=${STRIPE_WEBHOOK_SECRET:}
app.base-url=${APP_BASE_URL:http://localhost:8081}
app.kafka.enabled=${APP_KAFKA_ENABLED:false}
```

Production secrets should be injected through environment variables and never committed to source control.

---

## Stripe Webhook Events

Recommended webhook events:

- `checkout.session.completed`
- `checkout.session.async_payment_succeeded`
- `checkout.session.async_payment_failed`
- `checkout.session.expired`
- `payment_intent.succeeded`
- `payment_intent.payment_failed`
- `payment_intent.canceled`
- `charge.refunded`

Webhook endpoint:

```text
/api/payments/webhook/stripe
```

---

## Analytics

The analytics dashboard includes:

- Total orders
- Confirmed orders
- Refunded orders
- Revenue summary
- Top products

---

## Deployment

This project can be deployed using:

- GitHub Actions
- AWS EC2
- AWS RDS for MySQL
- AWS ElastiCache for Redis
- Nginx as a reverse proxy

---

## Why This Project Stands Out

This project demonstrates:

- payment workflow engineering
- webhook-driven order synchronization
- role-aware merchant operations
- tenant-aware architecture direction
- cloud deployment experience
- real-world backend integration patterns

---

## Future Enhancements

- Product catalog fully driven by merchant inventory
- Invite/join workflow for multiple users per tenant
- Richer analytics charts
- Gross vs net revenue reporting
- HTTPS and custom domain
- Stronger production hardening
- PDF receipts and branded email templates

---

## Author

Built as a SaaS-style merchant operations and payment platform prototype using Spring Boot, Stripe, Redis, and AWS.
