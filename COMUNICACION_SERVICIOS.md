# Comunicación entre servicios (Front, Back, AWS, DuckDNS, WebSocket, Mobile)

Documento guiado por `ARCHITECTURE.md` y ajustado al estado implementado en el código actual.

## 1) Topología actual

- Dominio público: `https://bpmn-soft.duckdns.org` (DuckDNS apunta a la IP pública de EC2).
- Contenedores Docker Compose:
  - `frontend` (Nginx + Angular) en `:4200` (host) -> `:80` (contenedor).
  - `backend` (Spring Boot) en `:8080`.
  - `rabbitmq` (STOMP relay) en `:5672` y UI `:15672`.
- Red interna Docker: `bpmn-net`.
- Servicios externos:
  - MongoDB (vía `MONGODB_URI`, actualmente externo/Atlas).
  - AWS S3 (adjuntos + URLs firmadas).

## 2) Matriz de comunicación

| Origen | Destino | Protocolo | Ruta/Canal | Propósito |
|---|---|---|---|---|
| Browser | DuckDNS + Nginx | HTTPS | `https://bpmn-soft.duckdns.org` | Entrada pública web |
| Nginx | Backend | HTTP interno | `/api/* -> backend:8080/api/*` | REST |
| Nginx | Backend | HTTP interno | `/graphql -> backend:8080/graphql` | GraphQL |
| Browser | Backend (via Nginx) | WSS/WS + STOMP | `/ws-designer` | Colaboración en tiempo real |
| Backend STOMP | RabbitMQ | STOMP Relay | Prefijos `/topic`, `/queue` | Distribución de eventos |
| Backend | MongoDB | Mongo protocol | `MONGODB_URI` | Persistencia de negocio |
| Backend | AWS S3 | HTTPS (AWS SDK v2) | `PutObject`, `PresignGetObject` | Adjuntos y URLs firmadas |
| Mobile App | Backend | HTTPS/HTTP (Dio) | `/api/auth/mobile/login`, `/api/execution/*`, `/api/files/upload` | Login cliente + trámites/tareas |

## 3) Flujo web end-to-end

1. Usuario entra a `https://bpmn-soft.duckdns.org`.
2. DuckDNS resuelve dominio a la IP pública EC2.
3. Nginx recibe tráfico:
   - Sirve Angular para `/`.
   - Proxya `/api`, `/graphql`, `/ws-designer` a `backend:8080`.
4. Frontend usa rutas relativas (`/api/...`, `/graphql`), evitando hardcode de host.
5. Backend valida token/cors y procesa casos de uso.

## 4) Flujo de autenticación y autorización

1. Front web: `POST /api/auth/login`; mobile: `POST /api/auth/mobile/login`.
2. `AuthService` autentica contra Mongo (`UserRepository`).
3. `TokenService` emite token de sesión en memoria.
4. Cliente guarda token (`localStorage` web / secure storage mobile).
5. En requests siguientes, se envía `Authorization: Bearer <token>`.
6. `AuthTokenFilter` reconstruye usuario y authorities en `SecurityContext`.
7. `@PreAuthorize` y reglas de servicio aplican control por rol/carril.

## 5) Flujo CORS y dominio

- CORS se define en `SecurityConfig` con `app.cors.allowed-origins`.
- Valor efectivo se inyecta desde `infra/.env` -> `docker-compose.yml` -> backend.
- Para producción web, el origin clave es:
  - `https://bpmn-soft.duckdns.org`
- También están permitidos localhost para desarrollo.

## 6) Flujo de colaboración en tiempo real (WebSocket)

1. Front conecta STOMP a `wss://bpmn-soft.duckdns.org/ws-designer`.
2. Publica cambios del diagrama a `/app/policy/{policyId}/change`.
3. `DesignerSocketController` recibe y reenvía a `/topic/policy.{policyId}`.
4. Broker relay de Spring pasa mensajes por RabbitMQ.
5. Clientes suscritos a `/topic/policy.{policyId}` reciben cambios.
6. El diseñador aplica eventos remotos (`add/move/remove/update`) sobre el grafo.

## 7) Flujo de adjuntos con AWS S3

1. Front/Mobile arma `multipart/form-data` y llama `POST /api/files/upload`.
2. Backend (`S3Service`) sube bytes al bucket (`putObject`).
3. Backend genera URL firmada de lectura (`presignGetObject`).
4. Respuesta al cliente: `key`, `url`, `contentType`, `size`.
5. Cliente guarda `url/key` en metadata de diagrama o `formData` de tarea.

## 8) Flujo de ejecución de procesos y tareas

1. Front/Mobile consulta políticas iniciables (`/api/execution/startable-policies`).
2. Inicia proceso (`/api/execution/process/start`).
3. `ProcessExecutionService` crea `ProcessInstance` y tareas `PENDING`.
4. Usuario toma tarea (`/tasks/{id}/take`) y la completa (`/tasks/{id}/complete`).
5. Si la tarea está antes de una decisión, frontend aplica lookahead y envía `_decisionTomada` en `formData`.
6. Motor de workflow (`WorkflowEngine` + `advanceWorkflow`) evalúa SpEL de links de decisión y activa solo la rama válida (o `default`).
6. Todo queda persistido en Mongo (`policies`, `process_instances`, `task_instances`, etc.).

## 8.1) Flujo específico de decisión (lookahead + SpEL)

1. Front obtiene `diagramJson` y `taskId` en `GET /api/execution/tasks/{id}`.
2. Hace lookahead local:
   - siguiente nodo de la tarea actual;
   - si es `DECISION`, lista labels de salidas (`conditionLabel`).
3. Muestra botones `Completar: <opción>`.
4. En submit agrega `_decisionTomada: <opción>` al `formData`.
5. Backend evalúa scripts tipo `#_decisionTomada == 'Sí'` y continúa por un único camino.

## 9) Comunicación mobile

- Base URL definida por `API_BASE_URL` (`mobile/lib/core/config/env.dart`).
- Cliente mobile usa `Dio` con interceptor de token.
- Consume los mismos endpoints REST que web para ejecución y archivos.
- No usa GraphQL ni WebSocket del diseñador en la implementación actual.

## 10) Variables de entorno críticas

- Backend/App:
  - `APP_PUBLIC_BASE_URL`
  - `CORS_ALLOWED_ORIGINS`
- Mongo:
  - `MONGODB_URI` / `SPRING_DATA_MONGODB_URI`
- RabbitMQ:
  - `SPRING_RABBITMQ_HOST`
  - `SPRING_RABBITMQ_STOMP_PORT`
  - `SPRING_RABBITMQ_USERNAME`
  - `SPRING_RABBITMQ_PASSWORD`
- AWS:
  - `AWS_REGION`
  - `AWS_S3_BUCKET`
  - `AWS_ACCESS_KEY_ID`
  - `AWS_SECRET_ACCESS_KEY`

## 11) Notas de consistencia (importante)

- `ARCHITECTURE.md` menciona Redis y contenedor Mongo local en la visión objetivo.
- La implementación activa usa:
  - RabbitMQ STOMP relay para tiempo real.
  - Mongo externo vía URI (no hay servicio `mongo` en `docker-compose.yml` actual).
- Recomendación: mantener `ARCHITECTURE.md` alineado con este estado para evitar confusión operativa.
- El gateway `DECISION` se resuelve automáticamente en backend (no se crea tarea humana de decisión).

---

## Referencias técnicas
- `infra/nginx.conf`
- `infra/docker-compose.yml`
- `infra/.env`
- `backend/src/main/java/com/politicanegocio/core/config/SecurityConfig.java`
- `backend/src/main/java/com/politicanegocio/core/config/WebSocketConfig.java`
- `backend/src/main/java/com/politicanegocio/core/websocket/DesignerSocketController.java`
- `backend/src/main/java/com/politicanegocio/core/service/S3Service.java`
- `frontend/src/app/features/policy-designer/services/web-socket.service.ts`
- `mobile/lib/core/network/api_client.dart`
