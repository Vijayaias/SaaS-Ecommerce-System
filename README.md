# SaaS-Ecommerce-System
diff --git a//Users/ajaybandi/Downloads/Stripe1/stripe-payment-main 2/README.md b//Users/ajaybandi/Downloads/Stripe1/stripe-payment-main 2/README.md
new file mode 100644
--- /dev/null
+++ b//Users/ajaybandi/Downloads/Stripe1/stripe-payment-main 2/README.md
@@ -0,0 +1,183 @@
+# Checkout Studio
+
+Checkout Studio is a Spring Boot based ecommerce and payment workflow application that demonstrates secure checkout, Stripe integration, order lifecycle management, tenant-aware merchant workspaces, inventory tracking, analytics, and AWS deployment readiness.
+
+## Features
+
+- JWT-based authentication and session-aware frontend flow
+- Stripe Checkout integration
+- Stripe webhook handling for payment confirmation and refunds
+- Order lifecycle tracking
+- Tenant-aware merchant workspaces
+- Role-based access for Owner, Manager, and Cashier
+- Inventory management
+- Sales analytics dashboard
+- Redis integration support
+- Kafka-ready event-driven architecture
+- AWS EC2 deployment workflow with GitHub Actions
+
+## Tech Stack
+
+- Java 17
+- Spring Boot 3
+- Spring Security
+- Spring Data JPA
+- MySQL / H2
+- Redis
+- Stripe API
+- Thymeleaf
+- HTML / CSS / JavaScript
+- Maven
+- GitHub Actions
+- AWS EC2
+- AWS RDS
+- AWS ElastiCache
+
+## Role Model
+
+### Owner
+
+- Manage inventory
+- View analytics
+- Cancel or refund eligible orders
+- Full merchant workspace visibility
+
+### Manager
+
+- View analytics
+- Cancel or refund eligible orders
+- Operational access without owner-level inventory creation controls
+
+### Cashier
+
+- Login and process normal order / checkout flow
+- Restricted from analytics and refund management
+- Limited operational permissions
+
+## Core Modules
+
+- Authentication and user management
+- Product browsing and cart
+- Checkout and payment processing
+- Order tracking
+- Refund handling
+- Tenant management
+- Inventory management
+- Analytics dashboard
+- Event audit visibility
+
+## Project Structure
+
+```text
+src/main/java/com/javatechie
+  controller/
+  dto/
+  model/
+  repository/
+  service/
+
+src/main/resources
+  static/
+  templates/
+  application.properties
+  application-prod.properties
+
+deploy/
+.github/workflows/
+```
+
+## Local Run
+
+### Prerequisites
+
+- Java 17
+- Maven
+- Stripe test keys
+
+### Run
+
+```bash
+mvn spring-boot:run
+```
+
+Or on a custom port:
+
+```bash
+mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
+```
+
+## Configuration
+
+Example runtime configuration:
+
+```properties
+stripe.secretKey=${STRIPE_SECRET_KEY:}
+stripe.webhookSecret=${STRIPE_WEBHOOK_SECRET:}
+app.base-url=${APP_BASE_URL:http://localhost:8081}
+app.kafka.enabled=${APP_KAFKA_ENABLED:false}
+```
+
+For production, secrets should be provided through environment variables and not committed to source control.
+
+## Stripe Webhook Events
+
+Recommended webhook events:
+
+- checkout.session.completed
+- checkout.session.async_payment_succeeded
+- checkout.session.async_payment_failed
+- checkout.session.expired
+- payment_intent.succeeded
+- payment_intent.payment_failed
+- payment_intent.canceled
+- charge.refunded
+
+Webhook endpoint example:
+
+```text
+/api/payments/webhook/stripe
+```
+
+## Analytics
+
+The analytics dashboard includes:
+
+- Total orders
+- Confirmed orders
+- Refunded orders
+- Revenue summary
+- Top products
+
+## Deployment
+
+This project can be deployed using:
+
+- GitHub Actions
+- AWS EC2
+- AWS RDS for MySQL
+- AWS ElastiCache for Redis
+- Nginx as reverse proxy
+
+## Demo Value
+
+This project is designed as a strong backend/full-stack portfolio project and demonstrates:
+
+- payment workflow engineering
+- webhook-driven order synchronization
+- role-aware merchant operations
+- tenant-aware architecture direction
+- cloud deployment experience
+
+## Future Enhancements
+
+- Product catalog fully driven by merchant inventory
+- Invite/join workflow for multiple users per tenant
+- Richer analytics charts
+- Gross vs net revenue reporting
+- HTTPS and custom domain
+- Stronger production hardening
+- PDF receipts and branded email templates
+
+## Author
+
+Built as a SaaS-style merchant operations and payment platform prototype using Spring Boot, Stripe, Redis, and AWS.
