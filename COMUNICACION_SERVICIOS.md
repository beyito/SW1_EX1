# Comunicacion entre servicios

## 1. Topologia actual

- `frontend` (Angular, navegador):
  - HTTP -> `backend` (`/api/*`, `/graphql`)
  - WebSocket STOMP -> `backend` (`/ws-designer`)
- `backend` (Spring Boot):
  - MongoDB (persistencia principal)
  - RabbitMQ/STOMP relay (broadcast colaborativo)
  - AWS S3 (adjuntos)
  - `bpmn-ai-engine` (copilot y agente)
- `bpmn-ai-engine` (FastAPI):
  - OpenAI API (si `AI_API_KEY` activo)

## 2. Contratos frontend -> backend

### 2.1 Auth REST
- `POST /api/auth/login`
- `POST /api/auth/mobile/login`
- `POST /api/auth/register`
- `GET /api/auth/me`

### 2.2 Admin REST
- `GET/POST /api/admin/companies`
- `GET/POST/PUT/DELETE /api/admin/company-admins`
- `GET/POST/PUT/DELETE /api/admin/areas`
- `GET/POST/PUT/DELETE /api/admin/functionaries`
- `GET/POST/PUT/DELETE /api/admin/clients`

### 2.3 Ejecucion REST
- `POST /api/execution/process/start`
- `POST /api/execution/tasks/{taskId}/take`
- `POST /api/execution/tasks/{taskId}/start`
- `POST /api/execution/tasks/{taskId}/complete`
- `GET /api/execution/tasks/{taskId}`
- `GET /api/execution/my-tasks`
- `GET /api/execution/my-processes/tasks`
- `GET /api/execution/startable-policies`
- `GET /api/execution/client/tasks/pending`

### 2.4 GraphQL
- Query:
  - `getAllPolicies`
  - `getPolicyById(id)`
  - `getTaskExecutionOrder(policyId)`
  - `myTasks`
  - `getTaskDetail(taskId)`
- Mutation:
  - `createPolicy(name, description)`
  - `updatePolicyGraph(policyId, diagramJson, lanes)`
  - `takeTask(taskId)`
  - `completeTask(taskId, formData)`

### 2.5 Copilot REST (gateway Spring)
- `POST /api/copilot/chat`
- `GET /api/copilot/history?policyId=...|conversationId=...`
- `POST /api/copilot/apply`

### 2.6 Files REST
- `POST /api/files/upload` (`multipart/form-data`)

### 2.7 WebSocket STOMP
- Connect broker: `ws(s)://<host>/ws-designer`
- Publish: `/app/policy/{policyId}/change`
- Subscribe: `/topic/policy.{policyId}`

## 3. Contratos backend -> IA

### 3.1 Chat
- Backend (`CopilotService.chat`) llama:
  - `POST {aiBaseUrl}/api/ai/copilot-chat`
- Payload:
  - `userMessage`
  - `currentDiagram`
  - `history`
- Response:
  - `message`
  - `suggested_actions`

### 3.2 Apply
- Backend (`CopilotService.apply`) llama:
  - `POST {aiBaseUrl}/api/v1/agent/diagram`
- Payload:
  - `operation=modify`
  - `instruction`
  - `current_diagram`
  - `lanes`
  - `context`
- Response:
  - `summary`
  - `changes`
  - `warnings`
  - `diagram`

## 4. Flujos de comunicacion clave

### 4.1 Guardado de diagrama con lanes
1. Frontend serializa `graph` (sin fondos de lanes) con `getPersistedGraphJSON`.
2. Frontend envia mutation `updatePolicyGraph(policyId, diagramJson, lanes)`.
3. Backend `PolicyService.updatePolicyGraph`:
   - recorre `cells`
   - asigna `laneId` por posicion X usando geometria de lanes (`x`, `width`)
   - persiste `diagramJson`, `startLaneId`, `lanes`.

### 4.2 Sincronizacion colaborativa
1. Usuario A mueve/agrega/elimina celda.
2. Frontend A publica evento STOMP.
3. Backend retransmite al topic de politica.
4. Usuario B recibe y aplica:
   - `move`/`add`/`remove`/`cell-sync` incremental.
   - `full-sync` para cambios de carriles/layout global.

### 4.3 Zoom/pan responsive local
- No se envia por red.
- Afecta viewport local (`paper.scale/translate`), no coordenadas persistidas.
- Coordenadas de nodos/carriles siguen en sistema del diagrama.

### 4.4 Ejecucion de tareas con decision
1. Usuario completa tarea con `formData`.
2. Backend parsea variables (ejemplo `_decisionTomada`).
3. `WorkflowEngine` evalua enlaces `condition.type=expression` (SpEL).
4. Si no hay match, usa `condition.type=default` cuando exista.

### 4.5 IA apply no destructivo
1. Frontend pide `apply`.
2. IA retorna diagrama propuesto.
3. Frontend fusiona con diagrama actual si instruccion no es destructiva.
4. Se aplica al grafo, se persiste por GraphQL y se emite `full-sync`.

## 5. Matriz de metodos y dependencias

### Frontend
- `PolicyDataService` -> `executeGraphql` -> `/graphql`.
- `WebSocketService` -> STOMP broker `/ws-designer`.
- `CopilotService` -> `/api/copilot/*`.
- `CompanyAreaService` -> `/api/admin/areas`.
- `FileService` -> `/api/files/upload`.

### Backend
- `PolicyGraphQLController` -> `PolicyService`.
- `ExecutionGraphQLController` -> `ProcessExecutionService`.
- `ProcessExecutionController` -> `ProcessExecutionService`, `PolicyService`.
- `CopilotController` -> `CopilotService` -> IA.
- `DesignerSocketController` -> `SimpMessagingTemplate`.

### IA
- `main.py`:
  - `/api/ai/copilot-chat` usa OpenAI chat completions.
  - `/api/v1/agent/diagram` usa `DiagramAgentService.process`.
- `diagram_tools.sanitize_diagram` garantiza consistencia estructural.

## 6. Variables y configuracion critica

- Backend:
  - `spring.mongodb.uri`
  - AWS S3 (`bucket`, credenciales)
  - seguridad y CORS
  - `app.ai-engine.base-url`
- IA:
  - `AI_API_KEY`
  - `AI_BASE_URL`
  - `AI_MODELS` / `AI_MODEL`
  - `AI_TIMEOUT_SECONDS`
- Frontend:
  - proxy hacia backend (`/api`, `/graphql`, `/ws-designer`)

## 7. Reglas de consistencia operativa

- Persistir siempre lanes con `x` y `width` para evitar drift entre navegadores.
- No incluir celdas de fondo de carril en `diagramJson`.
- Enlaces deben tener source/target validos; referencias huerfanas son eliminadas por saneamiento.
- Si una operacion IA es aditiva, fusionar con diagrama base en lugar de reemplazarlo completo.

## 8. Extension FCM Push

### Mobile -> Backend
- `POST /api/notifications/device-token` (autenticado)
  - Payload: `{ "token": "<fcm-token>" }`
  - Efecto: asocia token FCM al usuario actual.

### Backend -> FCM
- `FirebaseMessagingService.sendTaskAssignedNotification(token, taskName)`
- Trigger: cuando `ProcessExecutionService` crea una nueva tarea pendiente.

### Consideraciones
- Si FCM falla, no se bloquea el flujo BPMN.
- Android 13+ requiere `POST_NOTIFICATIONS`.
