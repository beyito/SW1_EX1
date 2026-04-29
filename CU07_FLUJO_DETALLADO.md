# CU07 - Flujo Detallado de Ejecucion (Ingenieria Inversa)

## 1. Alcance
Este documento describe el flujo real del caso de uso **CU07: Editar diagrama con Copilot IA** a partir del codigo del proyecto (`frontend`, `backend`, `bpmn-ai-engine`), incluyendo clases, metodos, datos intercambiados, persistencia y errores.

## 2. Participantes Tecnologicos
- **Actor**: `Company Admin`
- **Frontend (Angular)**:
  - `PolicyDesignerComponent`
  - `CopilotService` (frontend)
  - `DiagramCanvasService`
  - `PolicyDataService`
  - `WebSocketService`
- **Backend (Spring Boot)**:
  - `CopilotController`
  - `CopilotService` (backend)
  - `CopilotConversationRepository`
  - `PolicyGraphQLController`
  - `PolicyService`
  - `PolicyRepository`
  - `DesignerSocketController`
  - `SimpMessagingTemplate`
- **IA (Python/FastAPI)**:
  - `main.py` -> `process_diagram(...)`
  - `DiagramAgentService.process(...)`
  - `sanitize_diagram(...)`
- **Infraestructura**:
  - `RabbitMQ` (relay STOMP)
  - `MongoDB Atlas` (colecciones de politicas y conversaciones)

## 3. Flujo Principal End-to-End

### 3.1 Disparo desde UI (chat)
1. El actor escribe texto en el panel de Copilot.
2. `PolicyDesignerComponent.sendToCopilot(userText)`:
   - normaliza texto (`trim`)
   - agrega mensaje local a `copilotMessages`
   - activa `copilotLoading`
   - captura estado actual:
     - `currentDiagram = this.graph.toJSON()`
     - `activeLanesPayload = this.lanes.map(...)`
   - construye contexto de areas disponibles.

### 3.2 Consulta conversacional al Copilot (chat)
3. Frontend llama:
   - `copilotService.sendMessage(text, currentDiagram, activeLanesPayload, options)`
4. `frontend/CopilotService.sendMessage(...)` arma body con:
   - `userMessage`
   - `currentDiagram`
   - `lanes`
   - `context`
   - `conversationId`, `policyId`, `policyName`
5. Backend recibe en:
   - `CopilotController.chat(...)`
6. Orquestacion backend:
   - `CopilotService.chat(request, actor)`
   - valida actor y request (`requireActor`, `userMessage obligatorio`)
   - resuelve conversacion (`resolveConversation`)
   - prepara historial IA (`toAiHistory`)
   - invoca IA conversacional (request bloqueante):
     - `restClient.post().uri("/api/ai/copilot-chat")...`
   - parsea con `parseChatResponse(...)`
   - persiste mensajes:
     - `appendMessage(...user...)`
     - `appendMessage(...assistant...)`
     - `copilotConversationRepository.save(conversation)`
   - retorna `CopilotResponseDto` con `message`, `suggestedActions`, `conversationId`.
7. Frontend actualiza chat con respuesta del asistente.

### 3.3 Rama de mutacion (aplicar cambios al diagrama)
8. `PolicyDesignerComponent.isMutationIntent(text)` decide si aplica cambios reales.
9. Si es mutacion:
   - ejecuta `applyLaneCommandsFromText(text)` (ajustes previos de carriles por regex)
   - llama `copilotService.applyChange(instruction, currentDiagram, lanes, context)`
10. Backend recibe en:
   - `CopilotController.apply(request)`
11. `CopilotService.apply(request)`:
   - valida `instruction` y `currentDiagram`
   - arma payload para agente:
     - `operation = "modify"`
     - `instruction`
     - `current_diagram`
     - `lanes`
     - `context` (si existe)
   - invoca IA:
     - `restClient.post().uri("/api/v1/agent/diagram")...`
   - parsea con `parseApplyResponse(...)`
   - valida que exista `response.diagram`
   - retorna `CopilotApplyResponseDto`.

### 3.4 Procesamiento en microservicio IA
12. FastAPI entra por:
   - `main.py -> process_diagram(request: AgentRequest)`
13. Servicio IA:
   - `DiagramAgentService.process(request)`
   - calcula `base_diagram` (modo `modify`)
   - llama `_run_llm(...)`
   - normaliza carriles (`_normalize_lanes`)
   - fusiona diagrama:
     - destructivo: usa `ai_diagram`
     - no destructivo: `_merge_diagrams(base, ai)`
   - sanea estructura:
     - `sanitize_diagram(merged_diagram, lanes)`
   - responde `AgentResult` con:
     - `summary`, `changes`, `warnings`, `diagram`, `lanes`
14. Si falla IA internamente:
   - `DiagramAgentService.process(...)` usa `_fallback_result(...)`
   - devuelve diagrama base saneado + warning de error.

### 3.5 Aplicacion local, normalizacion y guardado
15. Frontend recibe `apply` y, si incluye `lanes`, remapea `id/name/color/x/width/height`.
16. Obtiene base persistible:
   - `currentGraph = diagramCanvasService.getPersistedGraphJSON(this.graph)`
17. Resuelve resultado final:
   - `resolvedDiagram = resolveCopilotDiagram(text, currentGraph, apply.diagram)`
   - dentro:
     - `asGraphJson(...)`
     - si vacio -> `normalizeDiagramForDesigner(currentDiagram, lanes)`
     - si no destructivo -> `mergeGraphCells(base, patch)`
     - normaliza siempre -> `normalizeDiagramForDesigner(...)`
18. Aplica en canvas:
   - `applyPolicy(updatedPolicy)`
19. Persiste en backend (si hay `selectedPolicyId`):
   - `policyDataService.updatePolicyDiagram(policyId, JSON.stringify(resolvedDiagram), lanes)`
20. `PolicyDataService.updatePolicyDiagram(...)` ejecuta mutacion GraphQL `updatePolicyGraph`.
21. Backend GraphQL:
   - `PolicyGraphQLController.updatePolicyGraph(...)`
   - `PolicyService.updatePolicyGraph(policyId, diagramJson, lanes)`
22. `PolicyService.updatePolicyGraph(...)`:
   - lee JSON
   - recorre `cells` de nodos
   - recalcula `laneId` por geometria (`determineLaneId(...)`)
   - actualiza `diagramJson`, `startLaneId`, `lanes`, `updatedAt`
   - persiste con `policyRepository.save(policy)`.

### 3.6 Sincronizacion colaborativa en tiempo real
23. Frontend difunde sincronizacion completa:
   - `broadcastFullSync(resolvedDiagram, lanes)`
   - internamente usa `webSocketService.sendMessage(policyId, event)`
24. WebSocket publish:
   - destino: `/app/policy/{policyId}/change`
25. Backend STOMP:
   - `DesignerSocketController.handlePolicyChange(policyId, event)`
   - retransmite: `messagingTemplate.convertAndSend("/topic/policy." + policyId, event)`
26. RabbitMQ relay distribuye a suscriptores del topic.
27. Clientes remotos consumen y aplican evento (`full-sync` o incremental segun tipo).

## 4. Excepciones y Manejo de Error (Real)

### 4.1 Timeout o fallo del servicio IA
- En `CopilotService.apply(...)` y `CopilotService.chat(...)`:
  - `ResourceAccessException` -> `ResponseStatusException`:
    - timeout: `GATEWAY_TIMEOUT`
    - conectividad: `BAD_GATEWAY`
- Frontend captura excepcion en `sendToCopilot(...)` y agrega mensaje `system` en chat.

### 4.2 Respuesta IA invalida o incompleta
- `parseApplyResponse(...)` / `parseChatResponse(...)` lanzan `BAD_GATEWAY` si JSON invalido.
- Si `apply` no trae `diagram`, backend devuelve error.
- Frontend muestra mensaje de fallo y no aplica cambios corruptos.

### 4.3 Errores de persistencia o seguridad
- `PolicyService.updatePolicyGraph(...)` falla si:
  - politica no existe
  - empresa del actor no coincide
  - JSON invalido
- Error asciende al cliente GraphQL.

## 5. Datos Principales que Viajan
- **Chat request**:
  - `userMessage`, `currentDiagram`, `lanes`, `context`, `conversationId`, `policyId`, `policyName`
- **Apply request**:
  - `instruction`, `currentDiagram`, `lanes`, `context`
- **Apply response**:
  - `summary`, `changes`, `warnings`, `diagram`, `lanes`
- **Persistencia politica**:
  - `diagramJson` (string JSON), `lanes`, `startLaneId`, `updatedAt`
- **Evento colaborativo**:
  - `DiagramEvent` (`full-sync`/otros tipos segun accion)

## 6. Nota de Comportamiento Funcional Importante
En la implementacion actual, cuando la instruccion es de mutacion, el sistema **aplica cambios automaticamente** (no exige confirmacion manual explicita del usuario antes del guardado y sincronizacion).

