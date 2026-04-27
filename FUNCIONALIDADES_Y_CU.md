# Funcionalidades y Casos de Uso (CU)

## 1. Autenticacion y sesion

### CU-01 Login Web
- Actor: `SOFTWARE_ADMIN`, `COMPANY_ADMIN`, `FUNCTIONARY`.
- Entrada: usuario + password.
- Endpoint: `POST /api/auth/login`.
- Frontend: `AuthService.login`.
- Resultado: token + perfil (`roles`, `company`, `area`, `laneId`).

### CU-02 Login Mobile Cliente
- Actor: `CLIENT`.
- Endpoint: `POST /api/auth/mobile/login`.
- Regla: login de cliente separado del flujo web.

### CU-03 Perfil autenticado
- Endpoint: `GET /api/auth/me`.
- Uso: refrescar perfil de sesion y lane operativo.

## 2. Administracion de empresa, areas y usuarios

### CU-04 Gestion de empresas
- Actor: `SOFTWARE_ADMIN`.
- Endpoints:
  - `POST /api/admin/companies`
  - `GET /api/admin/companies`

### CU-05 Gestion de company-admins
- Actor: `SOFTWARE_ADMIN`.
- Endpoints:
  - `POST /api/admin/company-admins`
  - `GET /api/admin/company-admins`
  - `PUT /api/admin/company-admins/{userId}`
  - `DELETE /api/admin/company-admins/{userId}`

### CU-06 Gestion de areas (swimlanes de negocio)
- Actor: `COMPANY_ADMIN`.
- Endpoints:
  - `POST /api/admin/areas`
  - `GET /api/admin/areas`
  - `PUT /api/admin/areas/{areaId}`
  - `DELETE /api/admin/areas/{areaId}`
- Uso en disenador: alimenta lista de carriles disponibles.

### CU-07 Gestion de functionaries
- Actor: `COMPANY_ADMIN`.
- Endpoints:
  - `POST /api/admin/functionaries`
  - `GET /api/admin/functionaries`
  - `PUT /api/admin/functionaries/{userId}`
  - `DELETE /api/admin/functionaries/{userId}`

### CU-08 Gestion de clientes
- Actor: `COMPANY_ADMIN`.
- Endpoints:
  - `POST /api/admin/clients`
  - `GET /api/admin/clients`
  - `PUT /api/admin/clients/{userId}`
  - `DELETE /api/admin/clients/{userId}`

## 3. Disenador BPMN (GraphQL + JointJS + tiempo real)

### CU-09 Crear politica
- Actor: `COMPANY_ADMIN`.
- GraphQL: `createPolicy(name, description)`.
- Backend: `PolicyService.createPolicy`.

### CU-10 Consultar politicas
- GraphQL:
  - `getAllPolicies`
  - `getPolicyById(id)` con `diagramJson` y `lanes{x,width,...}`.
- Frontend: `PolicyDataService.getAllPolicies`, `getPolicyById`.

### CU-11 Edicion de diagrama y auto-save
- Actor: `COMPANY_ADMIN`.
- GraphQL: `updatePolicyGraph(policyId, diagramJson, lanes)`.
- Frontend:
  - `DiagramCanvasService.getPersistedGraphJSON`
  - `PolicyDesignerComponent.persistPolicyGraph`
- Backend:
  - recalculo de `laneId` por nodo segun posicion X y carriles.
  - persistencia de `lanes` con `x` y `width`.

### CU-12 Canvas enterprise (responsive + zoom + panning)
- Funcionalidades:
  - `ResizeObserver` para ajustar `paper.setDimensions`.
  - Zoom con rueda y foco en cursor.
  - Panning del lienzo en area vacia.
- Metodos:
  - `initializeResponsiveCanvas`
  - `onCanvasWheel`
  - `startCanvasPanning`, `stopCanvasPanning`.

### CU-13 Lanes dinamicos y sincronizados
- Crear/eliminar carriles desde UI y por comandos IA.
- Redimensionar carriles y persistir geometria exacta (`x`, `width`).
- Sincronizacion cross-browser:
  - eventos incrementales para celdas.
  - `full-sync` para layout de carriles.

### CU-14 Edicion de nodos y enlaces
- Nodos:
  - tipos: `START`, `TASK`, `DECISION`, `FORK`, `JOIN`, `SYNCHRONIZATION`, `END`.
  - metadata: formularios, expresiones.
- Enlaces:
  - multiples conexiones permitidas.
  - condicion SpEL desde `DECISION`.
  - panel de propiedades de enlace para etiqueta y condicion.

### CU-15 Copilot BPMN (chat + apply)
- Chat:
  - mensaje contextual con snapshot del diagrama.
  - historial por politica/usuario.
- Apply:
  - genera cambios estructurales en JSON de diagrama.
  - fusion no destructiva cuando corresponde.
  - soporte para crear `FORK`, `JOIN` y sincronizacion.

## 4. Ejecucion de procesos y tareas

### CU-16 Iniciar proceso
- Endpoint: `POST /api/execution/process/start`.
- Regla: solo usuarios cuyo lane coincide con `startLaneId` de la politica.
- Servicio: `ProcessExecutionService.startProcess`.

### CU-17 Tomar/iniciar tarea
- Endpoints:
  - `POST /api/execution/tasks/{taskId}/take`
  - `POST /api/execution/tasks/{taskId}/start`
- Servicio: `ProcessExecutionService.startTask`.

### CU-18 Completar tarea
- Endpoint: `POST /api/execution/tasks/{taskId}/complete`.
- Servicio: `ProcessExecutionService.completeTask`.
- Reglas:
  - valida obligatorios del formulario dinamico.
  - parsea variables para enrutamiento (`_decisionTomada`).
  - avanza flujo con `DECISION`, `FORK`, `JOIN`, `END`.

### CU-19 Bandeja y detalle
- Endpoints:
  - `GET /api/execution/my-tasks`
  - `GET /api/execution/my-processes/tasks`
  - `GET /api/execution/tasks/{taskId}`
- GraphQL equivalente:
  - `myTasks`
  - `getTaskDetail(taskId)`
  - `takeTask`, `completeTask`.

### CU-20 Politicas iniciables por usuario
- Endpoint: `GET /api/execution/startable-policies`.
- Servicio: `PolicyService.getStartablePoliciesForUser`.

## 5. Adjuntos y formularios

### CU-21 Subir archivo
- Endpoint: `POST /api/files/upload`.
- Frontend: `FileService.uploadAttachment`.
- Backend: `S3Service.upload`.

### CU-22 Formularios de tarea
- Metadatos por nodo `TASK`: `taskForm.title`, `description`, `fields`.
- Campo de pregunta:
  - `requiresAttachment` y `attachmentLabel` para indicar adjunto requerido.
- Nota funcional vigente:
  - se conserva la marca de adjunto requerido por pregunta.
  - el adjunto directo del nodo puede estar deshabilitado en UI segun configuracion actual.

## 6. Metricas

### CU-23 Metricas por politica
- Endpoint: `GET /api/metrics/policy/{policyId}`.
- Servicio: `MetricsService.getPolicyMetrics`.
- Rol: `COMPANY_ADMIN` o `SOFTWARE_ADMIN`.

## 7. Colaboracion en tiempo real

### CU-24 Edicion simultanea de diagrama
- STOMP:
  - publica: `/app/policy/{policyId}/change`
  - suscribe: `/topic/policy.{policyId}`
- Servicio frontend: `WebSocketService`.
- Controlador backend: `DesignerSocketController.handlePolicyChange`.

## 8. Catalogo corto de metodos por modulo

### Frontend (policy-designer)
- `PolicyDataService`: `getAllPolicies`, `getPolicyById`, `createPolicy`, `updatePolicyDiagram`, `getTaskExecutionOrder`.
- `DiagramCanvasService`: `createGraph`, `createPaper`, `createShape`, `createLink`, `renderLaneBackgrounds`, `renderPolicy`.
- `PolicyDesignerComponent`:
  - carga/sincronizacion: `loadPolicyFromRoute`, `connectToPolicyTopic`, `handleRemoteEvent`.
  - lanes: `addLane`, `removeLane`, `syncLanesFromCanvas`.
  - copilot: `sendToCopilot`, `applyLaneCommandsFromText`, `resolveCopilotDiagram`.

### Backend
- `PolicyService`: `createPolicy`, `getPolicyById`, `updatePolicyGraph`, `getTaskExecutionOrder`.
- `ProcessExecutionService`: `startProcess`, `completeTask`, `startTask`, `getMyTasks`, `getTaskDetail`.
- `WorkflowEngine`: `getNextNodes`, `getNodeName`, `getIncomingNodeIds`.
- `CopilotService`: `chat`, `getConversationHistory`, `apply`.

### IA
- `DiagramAgentService.process`.
- `diagram_tools.sanitize_diagram`.
