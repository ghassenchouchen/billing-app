# Project Handoff: Billing Application

This document provides a high-level overview of the current state of the `billing-app` repository, detailing its architecture, recent foundational fixes, and readiness for production deployment.

## 1. Project Architecture

The **Billing Application** is a distributed system consisting of:
- **Frontend**: An Angular application (`billing-app-frontend`).
- **Backend (Spring Boot)**: A suite of Java-based microservices:
  - `api-gateway` (Spring Cloud Gateway)
  - `authentication-service`
  - `billing-service`
  - `boutique-service`
  - `catalog-service`
  - `customer-service`
  - `payment-service`
  - `subscription-service`
  - `usage-service`
- **Data & Messaging**: 
  - **MySQL 8.0** for relational persistence.
  - **Kafka** for asynchronous messaging (e.g., `boutique.sim.lifecycle` topic).
- **Deployment & Orchestration**: Kubernetes manifests actively defined in `k8s/environments/dev/` for local (Minikube) deployments.

## 2. Current Status & Recent Fixes

The local Minikube environment is completely stable. We recently resolved multiple critical crash loops that were causing the Kubernetes control plane to freeze:

### A. The "CrashLoopBackOff" DNS Resolution Fix (ndots:5 Issue)
- **Problem**: The Spring Boot microservices experienced intermittent `java.net.UnknownHostException: mysql` failures on startup. This was caused by the well-known Kubernetes `ndots:5` DNS search path issue, where Java's DNS resolution times out under load in Alpine/Linux containers.
- **Solution**: The `billing-config` ConfigMap (`k8s/environments/dev/config/configmap.yaml`) was rigorously updated. **All database and microservice URLs were changed to Fully Qualified Domain Names (FQDNs)** (e.g., `mysql.billing-dev.svc.cluster.local:3306`). This guarantees single-query, instant DNS resolution.

### B. The MySQL OOM (Out Of Memory) Protection
- **Problem**: The `mysql-0` pod was repeatedly dying with an `OOMKilled` (Exit Code 137) state. It was running in the `BestEffort` QoS class because it lacked defined memory boundaries, making it the first target for Kubernetes eviction.
- **Solution**: Added explicitly defined `resources` (Requests: 512Mi, Limits: 1Gi) to `k8s/environments/dev/infra/mysql.yaml`. MySQL now runs under the `Burstable` QoS class, ensuring survival during startup spikes.

## 3. Operations & CI/CD
- **Jenkins CI**: Active `Jenkinsfile` configurations exist for building the Spring backend and pushing Docker images to a registry.
- **Service Discovery**: The `api-gateway` currently routes traffic directly via Kubernetes internal DNS. A dedicated Service Registry (like Eureka) is intentionally omitted in favor of native Kubernetes capabilities.

## 4. Next Steps (Production Phase)
As the project enters the production phase, the immediate roadmap includes:
1. Translating the `dev` Kubernetes manifests to robust `prod` templates.
2. Migrating the localized MySQL StatefulSet to a managed cloud database (e.g., Amazon RDS / Aurora) for High Availability and automated backups.
3. Implementing advanced resilience patterns (Circuit Breakers in the API Gateway) to tolerate downstream service failures.
4. Setting up an NGINX Ingress Controller mapped to an external Cloud Load Balancer to securely accept internet traffic.
