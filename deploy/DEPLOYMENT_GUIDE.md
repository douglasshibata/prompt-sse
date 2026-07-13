# Deployment and Scaling Guide: Angular + Spring Boot (SSE) on Kubernetes

This guide outlines the production-ready architecture, containerization strategy, and Kubernetes configurations for deploying and scaling the **Prompt SSE** application (Angular frontend + Spring Boot backend).

---

## 1. High-Level Architecture Overview

The system is split into two primary layers:
1. **Frontend**: An Angular 19 SPA compiled to optimized static files and served via Nginx.
2. **Backend**: A Java 21 (Spring Boot 3.4.3) REST API and Server-Sent Events (SSE) stream server.

The **Nginx Ingress Controller** acts as the single entry point. It handles SSL termination (if configured), rewrites/routes paths, and manages long-lived client-server connections.

```
                  ┌────────────────────────┐
                  │   Client Browser       │
                  └───────────┬────────────┘
                              │
                              ▼ (HTTP/HTTPS)
                  ┌────────────────────────┐
                  │ Nginx Ingress Controller│
                  └──────┬──────────┬──────┘
                         │          │
        (Path: `/`)      │          │  (Paths: `/api`, `/api/events`)
                         ▼          ▼
             ┌─────────────┐      ┌─────────────┐
             │  frontend   │      │   backend   │
             │  Service    │      │  Service    │
             └──────┬──────┘      └──────┬──────┘
                    │                    │ (Sticky: ClientIP)
             ┌──────▼──────┐      ┌──────▼──────┐
             │ frontend-pod│      │ backend-pod │
             │  (Nginx SPA)│      │(Spring Boot)│
             └─────────────┘      └─────────────┘
```

---

## 2. Dockerization Strategy

### 2.1 Backend (Java 21 / Spring Boot)
The backend container is structured as a **multi-stage build** located in `prompt-sse/backend/Dockerfile`.

* **Build Stage**: Compiles the codebase using Maven and Eclipse Temurin JDK 21.
* **Run Stage**: Copies only the built JAR into a minimal Alpine JRE 21 runner, avoiding build-tool bloat.
* **Security Best Practice**: Runs the JAR under a custom, non-root user (`appuser` with `appgroup`), adhering to the principle of least privilege in container security.

### 2.2 Frontend (Angular 19 / Nginx)
The frontend container uses a **multi-stage build** located in `prompt-sse/frontend/Dockerfile`.

* **Build Stage**: Uses Node 20 to install dependencies, perform compiler optimizations, and generate static files in `dist/frontend/browser`.
* **Run Stage**: Uses an official `nginx:alpine` image to host the static files.
* **SPA Routing**: The Nginx configuration (`nginx.conf`) includes fallback directives (`try_files $uri $uri/ /index.html`) so that Angular router handles deep routes without throwing 404 errors.

---

## 3. Server-Sent Events (SSE) & Connection Timeouts

Server-Sent Events (SSE) rely on keeping an HTTP connection open indefinitely so the server can push chunks of text (`text/event-stream`) to the client.

### 3.1 Nginx Ingress Specifics
By default, Nginx buffers upstream responses before sending them to the client. This buffer blocks SSE events from arriving in real-time. Additionally, default connection read timeouts (usually 60 seconds) will prematurely terminate idle SSE channels.

The Kubernetes manifest `prompt-sse/deploy/k8s-deployment.yaml` resolves this using specialized Nginx Ingress annotations:

```yaml
metadata:
  annotations:
    kubernetes.io/ingress.class: "nginx"

    # 1. Disable proxy buffering so that events are flushed immediately to the client in real-time.
    nginx.ingress.kubernetes.io/proxy-buffering: "off"
    nginx.ingress.kubernetes.io/proxy-cache: "off"

    # 2. Extend proxy timeouts so Nginx does not terminate long-running SSE connections.
    nginx.ingress.kubernetes.io/proxy-read-timeout: "3600"
    nginx.ingress.kubernetes.io/proxy-send-timeout: "3600"

    # 3. Maintain connection headers
    nginx.ingress.kubernetes.io/connection-proxy-header: "keep-alive"
    nginx.ingress.kubernetes.io/proxy-http-version: "1.1"
```

---

## 4. State Persistence & Horizontal Scaling

### 4.1 The Challenge
The backend manages chatbot conversation state (user name, selected preferred topic, current flow position) dynamically in-memory using `ConversationService` via a thread-safe `ConcurrentHashMap`.

If the backend is scaled horizontally to `replicas: 2` (or more via the `HorizontalPodAutoscaler`):
1. User **POSTs** to `/api/prompt` (landing on **Backend Pod A**). Pod A updates the conversation state and broadcasts the AI response.
2. The user's **EventSource** (`/api/events`) connection is established on **Backend Pod B**.
3. Because Pod B does not share the in-memory state of Pod A, the user will **never receive** the AI response event!

### 4.2 Solution: Session Sticky Routing
To support seamless scaling while keeping the lightweight in-memory configuration, we configure **Session Affinity** on the Kubernetes backend service:

```yaml
spec:
  sessionAffinity: ClientIP
  sessionAffinityConfig:
    clientIP:
      timeoutSeconds: 10800 # 3-hour session stickiness
```

This guarantees that a client's IP is consistently mapped to the same backend pod, ensuring their prompt POSTs and SSE stream connections terminate at the exact same Java process.

### 4.3 Alternative Distributed Scaling Architecture (Zero-Stickiness)
For larger setups requiring true stateless containers and zero-affinity routing, the following migration path is recommended:
1. **Redis Session State / Spring Session**: Externalize conversation maps into a shared, fast-access distributed cache (Redis cluster).
2. **Redis PubSub or RabbitMQ SSE Hub**: Instead of broadcasting events to an in-memory emitter list, publish responses to a shared queue/topic. Every backend instance subscribes to the message broker, ensuring any pod can deliver events to any client, regardless of where the client's connection resides.

---

## 5. Step-by-Step Deployment Instructions

### 5.1 Local Image Building

First, build the Docker images locally:

```bash
# Build Backend Image
docker build -t prompt-sse-backend:latest -f prompt-sse/backend/Dockerfile prompt-sse/backend/

# Build Frontend Image
docker build -t prompt-sse-frontend:latest -f prompt-sse/frontend/Dockerfile prompt-sse/frontend/
```

### 5.2 Deploying to Kubernetes

Apply the unified Kubernetes manifests:

```bash
kubectl apply -f prompt-sse/deploy/k8s-deployment.yaml
```

Verify that the deployments, services, Ingress, and HPA are properly initialized:

```bash
kubectl get deployments
kubectl get services
kubectl get ingress
kubectl get hpa
```

### 5.3 Local Host Routing and Access

To access the application using the configured host (`prompt-sse.local`), add an entry mapping your Ingress Controller external IP to this domain in your local `/etc/hosts` (Mac/Linux) or `C:\Windows\System32\drivers\etc\hosts` (Windows) file:

```text
# Replace with your actual Ingress Controller External IP or 127.0.0.1 for Minikube
127.0.0.1   prompt-sse.local
```

Once mapped, open your browser and navigate to:
* **Application URL**: `http://prompt-sse.local/`
* **Health API Check**: `http://prompt-sse.local/api/actuator/health`
