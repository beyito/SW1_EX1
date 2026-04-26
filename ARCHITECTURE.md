# BPMN Engine Architecture

## Final Monorepo Tree

```text
/
|-- backend/               # Spring Boot API + GraphQL + WebSocket (STOMP/RabbitMQ) + S3
|   |-- src/
|   |-- pom.xml
|   `-- Dockerfile
|-- bpmn-ai-engine/        # FastAPI microservice for AI-assisted BPMN generation/modification
|   |-- app/
|   |-- requirements.txt
|   `-- .env
|-- frontend/              # Angular standalone app + JointJS designer
|   |-- src/
|   |-- package.json
|   `-- Dockerfile
|-- infra/
|   |-- docker-compose.yml
|   `-- nginx.conf
|-- .env.example
`-- ARCHITECTURE.md
```

## Frontend Enterprise UI System

La capa Angular ahora sigue un sistema visual unificado para todas las pantallas de gestion:

- Header global de plataforma (`Enterprise BPMN Suite`) en `app.ts/app.scss`.
- Tokens de diseno globales en `frontend/src/styles.scss`:
  - Primario: `#1E3A8A`
  - Accion positiva: `#10B981`
  - Panel lateral: `#F3F4F6`
  - Canvas: `#F9FAFB`
  - Texto principal/secundario: `#374151` / `#6B7280`
- Mismo patron de componentes para tablas, tarjetas, formularios y estados interactivos.
- Disenador BPMN modernizado con sidebar por secciones, lanes tipo gestor visual, chat lateral premium y grilla sutil de lienzo.

## Attachment Upload Flow to Amazon S3

1. User selects a file in the policy designer task form.
2. Angular calls `FileService.uploadAttachment(file, policyId)` immediately.
3. Backend endpoint `POST /api/files/upload` receives multipart data.
4. `S3Service` uploads file bytes to `s3://<bucket>/policies/{policyId}/attachments/...`.
5. Backend generates a presigned GET URL and returns `{ key, url, contentType, size }`.
6. Frontend stores the returned `url` (not local file path) in the diagram JSON attachment.
7. Auto-save persists the updated diagram JSON; no manual disk uploads and no `C:\fakepath` leakage.

## Real-time Collaboration Across Containers

- Client sends STOMP events to `/app/policy/{policyId}/change`.
- Backend `DesignerSocketController` forwards events to `/topic/policy.{policyId}`.
- Spring WebSocket uses STOMP broker relay backed by RabbitMQ.
- Connected clients in all containers receive updates consistently.

## Lookahead Routing for Decisions

1. While rendering a task form, frontend reads `diagramJson` and `taskId` (current node).
2. Frontend performs lookahead:
   - finds immediate next node from current task,
   - if next node is `DECISION`, extracts outgoing link labels (`conditionLabel`).
3. UI shows one completion button per decision option (`Completar: Sí`, `Completar: No`, etc.).
4. On click, frontend injects `_decisionTomada: <opcion>` into submitted `formData`.
5. Backend evaluates SpEL conditions on decision links (for example `#_decisionTomada == 'Sí'`) and advances only through the matching branch (or `default` fallback).

## Container Communication (Docker)

- `frontend` (Nginx): exposes `:4200` -> serves Angular.
- `backend` (Spring): exposes `:8080` -> REST/GraphQL/WebSocket.
- `bpmn-ai-engine` (FastAPI): exposes `:8010` -> IA copilot and diagram assistant.
- `rabbitmq`: exposes `:5672` (`STOMP relay`) and `:15672` (`management UI`).
- MongoDB is currently external via `MONGODB_URI` (Atlas/self-managed).

Nginx reverse proxy routes:

- `/api/*` -> `backend:8080/api/*`
- `/graphql` -> `backend:8080/graphql`
- `/ws-designer` -> `backend:8080/ws-designer` (WebSocket Upgrade enabled)

All services share network `bpmn-net`.

## BPMN AI Microservice

- Service: `bpmn-ai-engine` (FastAPI).
- Main endpoints:
  - `POST /api/v1/agent/diagram` with:
    - `operation`: `create | modify`
    - `instruction`
    - `current_diagram` (required for `modify`)
    - `lanes`, `context`
  - `POST /api/ai/copilot-chat` with:
    - `userMessage`
    - `currentDiagram` (snapshot de `graph.toJSON()` de JointJS)
- Health endpoint:
  - `GET /health`
- Output:
  - for `/api/v1/agent/diagram`: updated `diagram` (`cells`) + `summary`, `changes`, `warnings`.
  - for `/api/ai/copilot-chat`: `message` + `suggested_actions`.
- Fallback behavior:
  - if OpenAI credentials are not configured, service returns sanitized/default diagram payload.

### Gateway Integration

- Frontend Angular consume:
  - `POST /api/copilot/chat` (Spring Boot)
- Spring Boot reenvía a:
  - `POST http://bpmn-ai-engine:8010/api/ai/copilot-chat` (en Docker network)

## Copilot Chat History

- Persistencia en MongoDB (`copilot_conversations`) por usuario y politica.
- Endpoints:
  - `POST /api/copilot/chat` guarda mensajes y respuesta de IA.
  - `GET /api/copilot/history?policyId={id}` recupera historial previo.
- El gateway incluye historial reciente como `conversation_history` al motor IA.

## Notes for ECS/Fargate

- Do not hardcode secrets; inject from Secrets Manager/SSM.
- Keep S3 bucket private and use presigned URLs.
- Run RabbitMQ as managed broker/cluster in production.
- Use managed Mongo service (DocumentDB/MongoDB Atlas) for persistence.
