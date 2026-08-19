# ⚡ Prompt SSE - Real-Time Streaming Assistant Engine

[![Tech Stack](https://img.shields.io/badge/Stack-Java%2021%20%7C%20Spring%20Boot%203.x%20%7C%20SSE%20%7C%20Angular-blue)](#)
[![Status](https://img.shields.io/badge/Status-Active-brightgreen)](#)
[![Streaming](https://img.shields.io/badge/Streaming-Server--Sent%20Events-purple)](#)

**Prompt SSE** is a high-performance real-time streaming AI conversation engine built with **Java 21**, **Spring Boot 3.x**, and **Angular**. It leverages **Server-Sent Events (SSE)** via Spring `SseEmitter` factories to stream low-latency LLM completions token-by-token directly to web clients.

---

## 📐 1. Architecture & Component Structure

### Folder Structure
```text
prompt-sse/
├── backend/                  # Java 21 Spring Boot Backend
│   ├── src/main/java/com/example/prompt/
│   │   ├── controller/       # PromptController & SseController
│   │   ├── factory/          # SseEmitterFactory (thread-safe emitter pool)
│   │   ├── model/            # Conversation & ConversationState
│   │   └── service/          # SseService & ConversationService
│   ├── src/test/java/        # Comprehensive SSE integration & multi-client tests
│   └── Dockerfile
├── frontend/                 # Angular Real-time SSE Chat Frontend
│   ├── src/app/services/     # SseService & PromptService (EventSource wrappers)
│   └── src/app/              # AppComponent & reactive streaming views
└── deploy/                   # Kubernetes deployment manifests (`k8s-deployment.yaml`)
```

### Key Architectural Features
- **Thread-Safe SseEmitter Factory:** Manages connection timeouts, heartbeat ping intervals, and graceful client disconnect handling.
- **Multi-Client Isolation:** Concurrent token streaming to hundreds of simultaneous SSE client connections.
- **Resilient Connection Handling:** Front-end `SseService` automatically handles reconnects and backoff when network interruptions occur.

---

## 🚀 2. Getting Started & Running

### Prerequisites
- **Java 21 JDK**
- **Node.js 18+ & npm**
- **Docker & Docker Compose** (optional)

### Running the Backend
```bash
cd prompt-sse/backend
mvn clean spring-boot:run
```

### Running the Frontend
```bash
cd prompt-sse/frontend
npm install
npm start
```
Open `http://localhost:4200` to interact with the streaming prompt interface.

---

## 🧪 3. Running Integration Tests
The backend includes dedicated multi-client and edge-case SSE tests:
```bash
cd prompt-sse/backend
mvn test
```
