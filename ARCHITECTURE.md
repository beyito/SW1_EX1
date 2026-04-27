# Arquitectura del Sistema BPMN Colaborativo

## 1. Monorepo y componentes

```text
/c:/SW1_EX1
|- backend/          Spring Boot (REST + GraphQL + WebSocket + MongoDB + S3 + gateway IA)
|- frontend/         Angular + JointJS (disenador BPMN colaborativo)
|- bpmn-ai-engine/   FastAPI + OpenAI (copilot chat + agente de modificacion de diagramas)
|- mobile/           Flutter (cliente final para inicio y seguimiento de tramites)
|- infra/            docker-compose y configuracion de red/proxy
|- ARCHITECTURE.md
|- FUNCIONALIDADES_Y_CU.md
|- COMUNICACION_SERVICIOS.md
`- PROJECT_AI_CONTEXT.md
```

## 2. Arquitectura logica

- Frontend Angular:
  - Renderiza y edita el diagrama en JointJS.
  - Persiste diagrama y lanes por GraphQL.
  - Sincroniza cambios en vivo por STOMP.
  - Consume Copilot (chat y apply) por REST.
- Backend Spring Boot:
  - API de autenticacion, administracion, ejecucion y metricas.
  - API GraphQL para politicas y tareas.
  - WebSocket relay para colaboracion.
  - Gateway hacia microservicio IA.
  - Orquestacion BPMN (WorkflowEngine + ProcessExecutionService).
- IA FastAPI:
  - `POST /api/ai/copilot-chat` para asistencia conversacional.
  - `POST /api/v1/agent/diagram` para crear/modificar JSON de diagrama.
  - Sanitiza y valida nodos/enlaces antes de devolver cambios.
- Persistencia:
  - MongoDB: users, areas, companies, policies, process_instances, task_instances, copilot_conversations.
  - AWS S3: adjuntos de formularios.

## 3. Disenador BPMN (frontend)

### 3.1 Canvas y experiencia de edicion
- Canvas responsive con `ResizeObserver` y `paper.setDimensions(...)`.
- Zoom con rueda (`@HostListener('wheel')`) y limites `0.2` a `2.0`.
- Panning del fondo sobre `blank:pointerdown/move/up` usando `paper.translate(...)`.
- Grilla visual, enlaces ortogonales, flechas configuradas y herramientas de edicion.

### 3.2 Lanes (swimlanes) y geometria
- Lanes persistidas con `id`, `name`, `color`, `x`, `width`.
- Dibujo de carriles con `HeaderedRectangle` de JointJS como fondo no interactivo de flujo.
- Captura de cambios de ancho/posicion y sincronizacion por full-sync entre navegadores.
- Normalizacion de geometria cuando faltan `x/width`.

### 3.3 Modelo de nodos
- Tipos soportados: `START`, `TASK`, `DECISION`, `FORK`, `JOIN`, `SYNCHRONIZATION`, `END`.
- Cada nodo mantiene `nodeType`, metadata (`nodeMeta`) y puertos `in/out`.
- Enlaces desde `DECISION` pueden llevar `condition` (SpEL) y `conditionLabel`.

## 4. Ejecucion de procesos (backend)

- `PolicyService` calcula `laneId` de nodos al guardar diagrama y determina `startLaneId`.
- `WorkflowEngine` evalua flujo siguiente:
  - Nodos normales: sigue enlaces salientes.
  - `DECISION`: evalua expresiones SpEL con variables del formulario.
  - `FORK`: abre ramas en paralelo.
  - `JOIN`: espera finalizacion de ramas entrantes.
  - `END`: cierra instancia de proceso.
- `ProcessExecutionService` crea/avanza tareas pendientes, valida permisos por lane/usuario y completa formularios.

## 5. Colaboracion en tiempo real

- Cliente publica a `/app/policy/{policyId}/change`.
- Backend retransmite a `/topic/policy.{policyId}`.
- Eventos usados:
  - `add`, `remove`, `move`, `cell-sync`, `full-sync`.
- Estrategia:
  - Cambios de celdas se propagan incrementalmente.
  - Cambios de lanes usan `full-sync` para evitar divergencia de layout.

## 6. Copilot y agente IA

- Chat:
  - Frontend -> `POST /api/copilot/chat` (Spring)
  - Spring -> `POST /api/ai/copilot-chat` (FastAPI)
  - Historial por usuario/politica en `copilot_conversations`.
- Apply:
  - Frontend -> `POST /api/copilot/apply`
  - Spring -> `POST /api/v1/agent/diagram`
  - Agente IA:
    - genera parche o diagrama
    - fusiona con base si instruccion no destructiva
    - sanitiza nodos/enlaces/lanes

## 7. Seguridad

- Autenticacion por token bearer.
- Roles principales:
  - `SOFTWARE_ADMIN`
  - `COMPANY_ADMIN`
  - `FUNCTIONARY`
  - `CLIENT`
- Reglas clave:
  - `CLIENT` usa mobile para login operativo.
  - Inicio de procesos limitado por `startLaneId` y area/lane del usuario.

## 8. Catalogo de metodos clave

### Frontend
- `PolicyDesignerComponent`:
  - `initializeResponsiveCanvas`
  - `onCanvasWheel`
  - `startCanvasPanning`, `stopCanvasPanning`
  - `syncLanesFromCanvas`, `broadcastLaneLayoutSync`
  - `sendToCopilot`, `resolveCopilotDiagram`, `mergeGraphCells`
- `DiagramCanvasService`:
  - `createGraph`, `createPaper`, `createShape`, `createLink`
  - `renderLaneBackgrounds`, `renderPolicy`, `getPersistedGraphJSON`
  - `recalculateLanePositions`, `getLaneIdByX`, `updateLinkCondition`
- `PolicyDataService`:
  - `getPolicyById`, `updatePolicyDiagram`, `getTaskExecutionOrder`
- `WebSocketService`:
  - `connect`, `subscribeToPolicy`, `sendMessage`
- `CopilotService`:
  - `sendMessage`, `getHistoryByPolicy`, `applyChange`

### Backend
- `PolicyService`:
  - `createPolicy`, `getPolicyById`, `updatePolicyGraph`, `getTaskExecutionOrder`
  - `getStartablePoliciesForUser`, `canUserStartPolicy`
- `WorkflowEngine`:
  - `getNextNodes`, `getNodeName`, `getFormSchemaForNode`, `getIncomingNodeIds`
- `ProcessExecutionService`:
  - `startProcess`, `completeTask`, `startTask`, `getMyTasks`, `getTaskDetail`
- `CopilotService`:
  - `chat`, `getConversationHistory`, `apply`
- `DesignerSocketController`:
  - `handlePolicyChange`

### IA (FastAPI)
- `DiagramAgentService`:
  - `process`, `_run_llm`, `_merge_diagrams`, `_fallback_result`
- `diagram_tools.py`:
  - `sanitize_diagram`, `create_default_diagram`

## 9. Riesgos y notas tecnicas

- Consistencia cross-browser depende de persistir y sincronizar `lane.x` y `lane.width`.
- Cambios IA no destructivos deben fusionarse, no reemplazar diagrama completo.
- Si un enlace referencia nodos inexistentes, el saneamiento lo elimina.
- Validar siempre que el backend y schema GraphQL expongan `Lane.width`.
