# PROJECT AI CONTEXT

Este archivo es el contexto maestro para cualquier IA que deba trabajar en este repositorio.

## 1) Resumen ejecutivo

Sistema BPMN empresarial colaborativo con 4 piezas principales:
- `frontend` Angular: disenador JointJS, colaboracion en tiempo real, copilot.
- `backend` Spring Boot: REST, GraphQL, WebSocket, motor de ejecucion BPMN, S3.
- `bpmn-ai-engine` FastAPI: chat BPMN + modificacion automatica de diagramas.
- `mobile` Flutter: canal cliente para iniciar procesos y gestionar tareas.

Objetivo de negocio:
- Disenar politicas BPMN por empresa.
- Ejecutar procesos y tareas por carril/area.
- Mantener colaboracion multiusuario en vivo.
- Asistir con IA sin romper consistencia del diagrama.

## 2) Estructura del repositorio

```text
c:/SW1_EX1
|- backend/
|- frontend/
|- bpmn-ai-engine/
|- mobile/
|- infra/
|- ARCHITECTURE.md
|- FUNCIONALIDADES_Y_CU.md
|- COMUNICACION_SERVICIOS.md
`- PROJECT_AI_CONTEXT.md
```

## 3) Stack tecnico

### Backend
- Java 17
- Spring Boot
- Spring Security
- Spring GraphQL
- Spring WebSocket (STOMP)
- MongoDB
- AWS S3 SDK

### Frontend
- Angular standalone
- `@joint/plus`
- `@stomp/stompjs`

### IA
- FastAPI + Pydantic
- OpenAI Python SDK

### Mobile
- Flutter + Dio

## 4) Dominio y modelos clave

### Politica
- `Policy`: `id`, `name`, `description`, `diagramJson`, `lanes`, `startLaneId`.
- `Lane`: `id`, `name`, `color`, `x`, `width`.

### Diagrama (JointJS)
- `cells` con nodos y enlaces.
- Nodos deben tener:
  - `id`, `type`, `nodeType`, `position`.
- Tipos `nodeType` permitidos:
  - `START`, `TASK`, `DECISION`, `FORK`, `JOIN`, `SYNCHRONIZATION`, `END`.
- Enlaces de decision:
  - `condition.type` (`expression`/`default`)
  - `condition.script` (SpEL)
  - `conditionLabel`.

### Ejecucion
- `ProcessInstance`: proceso activo/completado.
- `TaskInstance`: tarea con `taskId`, `laneId`, `status`, `assignedTo`, `formData`.

## 5) Roles y reglas de autorizacion

- Roles:
  - `SOFTWARE_ADMIN`
  - `COMPANY_ADMIN`
  - `FUNCTIONARY`
  - `CLIENT`
- Reglas:
  - `CLIENT` opera principalmente por mobile.
  - Inicio de proceso validado por `startLaneId` y lane del usuario.
  - Acceso a tarea: por lane o asignacion, segun rol.

## 6) Arquitectura de comunicacion

### Frontend -> Backend
- GraphQL `/graphql`: politicas + tareas.
- REST `/api/*`: auth, admin, execution, files, metrics, copilot.
- WebSocket `/ws-designer`:
  - publish `/app/policy/{policyId}/change`
  - subscribe `/topic/policy.{policyId}`.

### Backend -> IA
- `POST /api/ai/copilot-chat`
- `POST /api/v1/agent/diagram`

### Backend -> externos
- MongoDB.
- S3 para uploads.
- RabbitMQ para relay STOMP.

## 7) Catalogo de metodos (fuente de verdad del comportamiento)

## 7.1 Frontend: servicios

### `PolicyDataService`
- `getAllPolicies()`
- `getPolicyById(id)`
- `createPolicy(name, description)`
- `updatePolicyDiagram(policyId, diagramJson, lanes)`
- `getTaskExecutionOrder(policyId)`
- fallback de compatibilidad: `getPolicyByIdWithoutWidth(id)`.

### `DiagramCanvasService`
- Creacion:
  - `createGraph()`
  - `createPaper(graph)`
  - `createShape(type, label, x, y)`
  - `createLink(source, target, condition?)`
- Lanes y render:
  - `renderLaneBackgrounds(graph, lanes)`
  - `renderPolicy(graph, policy, lanes)`
  - `recalculateLanePositions(lanes)`
  - `getLaneIdByX(lanes, x)`
- Persistencia/sanitizado:
  - `getPersistedGraphJSON(graph)`
  - `sanitizeGraphJSON(graphJson)`
- Edicion:
  - `updateNodeLabel(...)`
  - `updateLinkCondition(...)`
  - `deleteElement(...)`.

### `WebSocketService`
- `connect()`
- `disconnect()`
- `subscribeToPolicy(policyId, callback)`
- `sendMessage(policyId, event)`.

### `CopilotService` (frontend)
- `sendMessage(userText, currentDiagram, options)`
- `getHistoryByPolicy(policyId)`
- `applyChange(instruction, currentDiagram, lanes, context)`.

### `CompanyAreaService`
- `getCompanyAreas()`.

### `FileService`
- `uploadAttachment(file, policyId?)`.

## 7.2 Frontend: componente principal del disenador

### `PolicyDesignerComponent` metodos funcionales criticos
- Ciclo de vida:
  - `ngOnInit`, `ngAfterViewInit`, `ngOnDestroy`.
- Canvas UX:
  - `initializeResponsiveCanvas`
  - `onCanvasWheel`
  - `startCanvasPanning`, `stopCanvasPanning`.
- Eventos y sync:
  - `registerPaperEvents`, `registerGraphEvents`
  - `connectToPolicyTopic`, `handleRemoteEvent`
  - `applyRemoteMove/add/remove/cellSnapshot/fullSync`.
- Lanes:
  - `addLane`, `removeLane`
  - `syncLanesFromCanvas`
  - `broadcastLaneLayoutSync`.
- Persistencia:
  - `scheduleAutoSave`, `persistPolicyGraph`.
- IA:
  - `sendToCopilot`
  - `applyLaneCommandsFromText`
  - `resolveCopilotDiagram`
  - `mergeGraphCells`.

## 7.3 Backend: controladores

### GraphQL
- `PolicyGraphQLController`
  - `createPolicy`
  - `getAllPolicies`
  - `getPolicyById`
  - `getTaskExecutionOrder`
  - `updatePolicyGraph`.
- `ExecutionGraphQLController`
  - `myTasks`
  - `getTaskDetail`
  - `takeTask`
  - `completeTask`.

### REST
- `AuthController`
  - `register`, `login`, `loginMobile`, `me`.
- `AdminController`
  - CRUD empresas/admins/areas/funcionarios/clientes.
- `ProcessExecutionController`
  - `startProcess`, `takeTask/startTask`, `completeTask`
  - `getTaskDetail`, `getMyTasks`, `getMyProcessTaskGroups`
  - `getStartablePolicies`, `getClientPendingTasks`.
- `CopilotController`
  - `chat`, `history`, `apply`.
- `FileUploadController`
  - `upload`.
- `MetricsController`
  - `getPolicyMetrics`.

### WebSocket
- `DesignerSocketController.handlePolicyChange`.

## 7.4 Backend: servicios

### `PolicyService`
- `createPolicy`
- `getAllPolicies`
- `getStartablePoliciesForUser`
- `canUserStartPolicy`
- `getPolicyById`
- `updatePolicyGraph` (recalcula `laneId` por posicion + lanes)
- `getTaskExecutionOrder`.

### `WorkflowEngine`
- `getNextNodes(policyId, currentNodeId[, routingVariables])`
- `getNodeName`
- `getFormSchemaForNode`
- `getIncomingNodeIds`.

### `ProcessExecutionService`
- `startProcess`
- `completeTask`
- `startTask` / `takeTask`
- `getPendingTasksForLane`
- `getMyPendingTasks`
- `getMyTasks`
- `getMyProcessTaskGroups`
- `getClientPendingTasks`
- `getTaskDetail`
- motor interno `advanceWorkflow` para `FORK`, `JOIN`, `DECISION`, `END`.

### `CopilotService` (backend gateway)
- `chat`
- `getConversationHistory`
- `apply`.

## 7.5 IA microservice

### Endpoints (`main.py`)
- `GET /health`
- `POST /api/v1/agent/diagram`
- `POST /api/ai/copilot-chat`.

### `DiagramAgentService`
- `process(request)`
- `_run_llm(...)`
- `_merge_diagrams(...)`
- `_fallback_result(...)`.

### `diagram_tools.py`
- `create_default_diagram()`
- `sanitize_diagram(diagram, lanes)`:
  - corrige/valida nodos
  - valida `laneId`
  - elimina enlaces huerfanos.

## 8) Invariantes de consistencia (muy importantes)

1. Nunca persistir fondos de lane en `diagramJson` (solo nodos/enlaces).
2. Mantener `Lane.x` y `Lane.width` actualizados para evitar drift entre navegadores.
3. Todo nodo debe tener `nodeType`.
4. Todo enlace debe apuntar a nodos existentes.
5. Al aplicar IA:
   - si instruccion no es destructiva, fusionar con base.
   - si es destructiva, reemplazo completo permitido.
6. `DECISION` debe tener condiciones coherentes (`expression/default`) para que el motor enrute correctamente.

## 9) Problemas historicos que ya se mitigaron

- Perdida de diagrama al agregar nodos por IA:
  - mitigado con merge no destructivo.
- Lanes duplicados/superpuestos entre navegadores:
  - mitigado con sincronizacion de geometria + `full-sync`.
- Error GraphQL de `Lane.width` undefined:
  - mitigado exponiendo `width` en schema y modelos compatibles.
- Desfase visual al hacer zoom:
  - mitigado separando transform de viewport vs coordenadas persistidas.

## 10) Checklist para cambios futuros

Antes de tocar el disenador:
1. Verificar impacto en `PolicyDesignerComponent`, `DiagramCanvasService` y `PolicyService`.
2. Confirmar que GraphQL schema y frontend models siguen alineados.
3. Probar colaboracion con 2 navegadores.
4. Probar persistencia:
   - mover nodos
   - redimensionar lanes
   - guardar, recargar, comparar.

Antes de tocar IA:
1. Mantener `nodeType` y estructura de `cells`.
2. Conservar referencias de `id` al generar enlaces.
3. Pasar por `sanitize_diagram`.

## 11) Comandos utiles de validacion

### Frontend
```powershell
Set-Location c:\SW1_EX1\frontend
npm run build
```

### Backend
```powershell
Set-Location c:\SW1_EX1\backend
cmd /c mvnw.cmd -q -DskipTests compile
```

### IA
```powershell
Set-Location c:\SW1_EX1\bpmn-ai-engine
python -m uvicorn app.main:app --host 0.0.0.0 --port 8010
```

## 12) Referencias de documentacion relacionada

- `ARCHITECTURE.md`
- `FUNCIONALIDADES_Y_CU.md`
- `COMUNICACION_SERVICIOS.md`
