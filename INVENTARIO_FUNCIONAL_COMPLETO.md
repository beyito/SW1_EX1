# Inventario Funcional Completo del Sistema

## 1. Objetivo del documento
Este documento consolida funcionalidades reales implementadas en el monorepo (`backend`, `frontend`, `bpmn-ai-engine`, `mobile`) para:
1. Tener una base confiable de alcance funcional.
2. Servir como insumo para prompts de diagramas UML (casos de uso, secuencia, componentes).
3. Evitar omisiones como métricas, colaboración en tiempo real y copilot IA.

## 2. Actores del sistema
- `SOFTWARE_ADMIN`: administra empresas y administradores de empresa.
- `COMPANY_ADMIN`: administra áreas, funcionarios, clientes y diseña políticas BPMN.
- `FUNCTIONARY` / `FUNCIONARIO`: ejecuta tareas operativas en su carril.
- `CLIENT`: inicia trámites y completa tareas desde mobile (el login web lo bloquea).
- `Asistente IA BPMN`: sugiere cambios, genera/edita diagrama y sanea estructura.

## 3. Módulos y funcionalidades

### 3.1 Autenticación y sesión
- Login web con token bearer.
- Login mobile exclusivo para `CLIENT`.
- Registro de usuarios (según flujo administrativo).
- Consulta de perfil autenticado.
- Logout y limpieza de sesión local.

Back/Front clave:
- Backend: `AuthController`, `AuthService`, `TokenService`, `AuthTokenFilter`.
- Frontend: `auth.service.ts`, `login.component.ts`, `role.guard.ts`.
- Mobile: `auth_service.dart` (`/api/auth/mobile/login`).

### 3.2 Administración corporativa
- Gestión de empresas.
- Gestión de company-admins (crear/listar/editar/eliminar).
- Gestión de áreas.
- Gestión de funcionarios.
- Gestión de clientes.

Back/Front clave:
- Backend: `AdminController`, `AdminService`.
- Frontend: `admin-software.component.ts`, `admin-company.component.ts`, `funcionarios.component.ts`.

### 3.3 Gestión de políticas BPMN
- Crear política.
- Listar políticas por empresa.
- Abrir política por ID.
- Guardar diagrama (`diagramJson`) y carriles (`lanes`).
- Calcular orden de ejecución de tareas (dependencias, orden y carril).
- Calcular/persistir `startLaneId` según nodo START.

Back/Front clave:
- Backend GraphQL: `PolicyGraphQLController`, `PolicyService`.
- Frontend: `policy-manager.component.ts`, `policy-data.service.ts`.

### 3.4 Diseñador BPMN colaborativo (Angular + JointJS)
- Canvas responsive con `ResizeObserver`.
- Zoom con rueda y límites de escala.
- Panning del lienzo.
- Drag & drop de nodos.
- Nodos soportados: `START`, `TASK`, `DECISION`, `FORK`, `JOIN`, `SYNCHRONIZATION`, `END`.
- Gestión de enlaces, incluyendo condiciones para decisiones.
- Gestión de formularios en nodo TASK:
  - Campos dinámicos (requeridos/opciones/tipos).
  - Flag de “requiere adjunto” en preguntas/campos.
- Gestión de carriles:
  - Alta/baja de lane.
  - Posición y tamaño (`x`, `width`, `height`) persistibles.
  - Full-sync para consistencia entre navegadores.
- Autoguardado y sincronización incremental de celdas.

Back/Front clave:
- Frontend: `policy-designer.component.ts`, `diagram-canvas.service.ts`, `diagram-storage.service.ts`, `web-socket.service.ts`.
- Backend: `PolicyService.updatePolicyGraph`, `DesignerSocketController`.

### 3.5 Colaboración en tiempo real
- Canal STOMP por política.
- Eventos soportados:
  - `add`
  - `remove`
  - `move`
  - `cell-sync`
  - `full-sync`
- Estrategia híbrida:
  - Incremental para celdas.
  - Full-sync para cambios estructurales de carriles/layout.

Back/Front clave:
- Backend: `DesignerSocketController` (`/app/policy/{policyId}/change` -> `/topic/policy.{policyId}`).
- Frontend: `web-socket.service.ts`, manejadores remotos en `policy-designer.component.ts`.

### 3.6 Copilot BPMN (chat + apply)
- Chat contextual del diagrama actual.
- Historial por política/conversación.
- Apply de instrucciones para modificar diagrama.
- Merge no destructivo cuando la instrucción es aditiva.
- Paso de `lanes` y `context` al motor IA.
- Manejo robusto de timeouts/errores con `requestId`.

Back/Front/IA clave:
- Frontend: `copilot.service.ts`, `copilot-chat.component.ts`.
- Backend: `CopilotController`, `CopilotService`.
- IA: `main.py` (`/api/ai/copilot-chat`, `/api/v1/agent/diagram`), `DiagramAgentService`.

### 3.7 Normalización y saneamiento de diagramas IA
- `normalizeDiagramForDesigner(...)` en frontend:
  - Estandariza estilo visual para que nodos IA y manuales se vean iguales.
  - Corrige geometría/posición para mantener nodos dentro de carriles.
  - Normaliza metadata de TASK/DECISION y links condicionales.
- `sanitize_diagram(...)` en IA:
  - Elimina enlaces huérfanos.
  - Corrige `nodeType`, `size`, `position`, `laneId`.
  - Fuerza consistencia de links y condiciones de decisión.
  - Garantiza limpieza de puertos visuales (`ports` removidos).

### 3.8 Ejecución de procesos
- Iniciar proceso desde política habilitada para el carril del usuario.
- Crear tareas pendientes por nodo/lane.
- Tomar/iniciar/completar tareas.
- Validación dinámica de formulario antes de completar tarea.
- Enrutamiento por decisiones usando variables del formulario.
- Soporte de paralelismo BPMN:
  - `FORK`: abre ramas.
  - `JOIN`: espera ramas entrantes.
  - `END`: cierra proceso.
- Vistas de tareas:
  - Mis tareas.
  - Mis procesos con agrupación por instancia.
  - Pendientes por carril.
  - Pendientes del cliente.

Back/Front/Mobile clave:
- Backend: `ProcessExecutionController`, `ProcessExecutionService`, `WorkflowEngine`.
- Frontend: `execution.service.ts`, `task-inbox`, `task-execution`, `funcionario-dashboard`.
- Mobile: `dashboard_service.dart`, `task_service.dart`.

### 3.9 Métricas de políticas
- Consulta de métricas por política.
- Cálculo por tarea completada:
  - Total ejecutadas.
  - Tiempo promedio de espera.
  - Tiempo promedio de ejecución.
  - Tiempo total promedio.
- Ordenamiento por mayor tiempo total (detección de cuellos de botella).

Back/Front clave:
- Backend: `MetricsController`, `MetricsService`.
- Frontend: `policy-metrics.service.ts`, `policy-metrics.component.ts`.

### 3.10 Gestión de adjuntos
- Subida de archivos a S3.
- Integración de adjuntos en ejecución de tareas/formularios.
- Endpoint compartido para web y mobile.

Back/Front/Mobile clave:
- Backend: `FileUploadController`, `S3Service`.
- Frontend: `file.service.ts`, `ExecutionService.uploadFile(...)`.
- Mobile: `task_service.dart` (`/api/files/upload`).

## 4. Casos de uso (CU) para UML

### CU-01: Administrar empresas y admins
Actor principal: `SOFTWARE_ADMIN`
1. Lista empresas y admins existentes.
2. Crea empresa.
3. Crea/edita/elimina company-admin.
Resultado: estructura organizacional lista para operar.

### CU-02: Administrar áreas, funcionarios y clientes
Actor principal: `COMPANY_ADMIN`
1. Lista áreas, funcionarios y clientes.
2. Crea/edita/elimina áreas.
3. Crea/edita/elimina funcionarios.
4. Crea/edita/elimina clientes.
Resultado: actores operativos y carriles organizacionales configurados.

### CU-03: Crear política BPMN
Actor principal: `COMPANY_ADMIN`
1. Crea política con nombre/descrición.
2. Abre diseñador.
3. Agrega nodos/enlaces/carriles/formularios.
4. Guarda diagrama y carriles.
Resultado: política versionada con grafo y geometría persistidos.

### CU-04: Diseñar flujo con carriles colaborativamente
Actor principal: `COMPANY_ADMIN`
Actores secundarios: otro `COMPANY_ADMIN` conectado
1. Usuario A modifica nodos/carriles.
2. Usuario A emite evento STOMP.
3. Usuario B recibe y aplica evento.
4. Para cambios globales de carriles se usa `full-sync`.
Resultado: ambos navegadores convergen al mismo layout.

### CU-05: Consultar orden de ejecución de tareas
Actor principal: `COMPANY_ADMIN`
1. Solicita orden de tareas de una política.
2. Sistema calcula dependencias y orden BFS desde START.
3. Devuelve secuencia con laneId/laneName.
Resultado: trazabilidad del flujo y soporte para documentación/proceso.

### CU-06: Iniciar proceso
Actor principal: `FUNCTIONARY` o `CLIENT`
1. Consulta políticas iniciables para su carril.
2. Inicia proceso con título/descrición.
3. Motor crea tareas iniciales según flujo.
Resultado: instancia activa con tareas pendientes.

### CU-07: Ejecutar y completar tarea
Actor principal: `FUNCTIONARY` o `CLIENT` autorizado
1. Lista tareas visibles.
2. Toma/inicia tarea.
3. Completa formulario (validación dinámica de campos requeridos).
4. Motor avanza flujo, evaluando DECISION/FORK/JOIN/END.
Resultado: tarea completada y nuevas tareas creadas según reglas BPMN.

### CU-08: Subir adjunto en tarea
Actor principal: `FUNCTIONARY` o `CLIENT`
1. Selecciona archivo.
2. Sube a `/api/files/upload`.
3. Guarda URL en datos de formulario.
Resultado: evidencia documental asociada a ejecución.

### CU-09: Usar asistente IA (chat)
Actor principal: `COMPANY_ADMIN`
1. Envía consulta con contexto del diagrama.
2. Gateway envía a microservicio IA.
3. IA responde recomendaciones y acciones sugeridas.
4. Sistema guarda historial de conversación.
Resultado: asistencia contextual sin modificar diagrama aún.

### CU-10: Aplicar cambios IA al diagrama
Actor principal: `COMPANY_ADMIN`
1. Envía instrucción de cambio.
2. IA devuelve diagrama/patch + lanes.
3. Frontend normaliza estilo y geometría.
4. Se guarda política y se difunde `full-sync`.
Resultado: cambio aplicado con consistencia visual y estructural.

### CU-11: Consultar métricas de política
Actor principal: `COMPANY_ADMIN`
1. Solicita métricas por policyId.
2. Backend agrega tiempos de tareas completadas.
3. Frontend muestra barras y posibles cuellos de botella.
Resultado: visibilidad de rendimiento del proceso.

## 5. Contratos de comunicación principales

### REST Backend
- Auth:
  - `POST /api/auth/register`
  - `POST /api/auth/login`
  - `POST /api/auth/mobile/login`
  - `GET /api/auth/me`
- Admin:
  - `POST/GET /api/admin/companies`
  - `POST/GET/PUT/DELETE /api/admin/company-admins`
  - `POST/GET/PUT/DELETE /api/admin/areas`
  - `POST/GET/PUT/DELETE /api/admin/functionaries`
  - `POST/GET/PUT/DELETE /api/admin/clients`
- Execution:
  - `POST /api/execution/process/start`
  - `POST /api/execution/tasks/{id}/take`
  - `POST /api/execution/tasks/{id}/start`
  - `POST /api/execution/tasks/{id}/complete`
  - `GET /api/execution/tasks/{id}`
  - `GET /api/execution/tasks/pending/{laneId}`
  - `GET /api/execution/startable-policies`
  - `GET /api/execution/my-tasks`
  - `GET /api/execution/my-processes/tasks`
  - `GET /api/execution/client/tasks/pending`
- Metrics:
  - `GET /api/metrics/policy/{policyId}`
- Files:
  - `POST /api/files/upload`
- Copilot:
  - `POST /api/copilot/chat`
  - `GET /api/copilot/history`
  - `POST /api/copilot/apply`

### GraphQL Backend
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

### WebSocket/STOMP
- Publish: `/app/policy/{policyId}/change`
- Subscribe: `/topic/policy.{policyId}`

### IA FastAPI
- `GET /health`
- `POST /api/ai/copilot-chat`
- `POST /api/v1/agent/diagram`

## 6. Métodos clave por servicio (catálogo resumido)

### Backend
- `PolicyService`:
  - `createPolicy`
  - `getAllPolicies`
  - `getStartablePoliciesForUser`
  - `canUserStartPolicy`
  - `getPolicyById`
  - `updatePolicyGraph`
  - `getTaskExecutionOrder`
- `ProcessExecutionService`:
  - `startProcess`
  - `completeTask`
  - `startTask`
  - `takeTask`
  - `getPendingTasksForLane`
  - `getMyTasks`
  - `getMyProcessTaskGroups`
  - `getClientPendingTasks`
  - `getTaskDetail`
  - `advanceWorkflow`
- `WorkflowEngine`:
  - `getNextNodes`
  - `getNodeName`
  - `getFormSchemaForNode`
  - `getIncomingNodeIds`
- `CopilotService`:
  - `chat`
  - `getConversationHistory`
  - `apply`
  - `parseChatResponse`
  - `parseApplyResponse`
- `MetricsService`:
  - `getPolicyMetrics`

### Frontend
- `PolicyDesignerComponent`:
  - `initializeResponsiveCanvas`
  - `onCanvasWheel`
  - `startCanvasPanning`
  - `addLane` / `removeLane`
  - `addNode`
  - `syncLanesFromCanvas`
  - `broadcastLaneLayoutSync`
  - `resolveCopilotDiagram`
  - `mergeGraphCells`
- `DiagramCanvasService`:
  - `createGraph`
  - `createPaper`
  - `createShape`
  - `createLink`
  - `renderLaneBackgrounds`
  - `renderPolicy`
  - `getPersistedGraphJSON`
  - `normalizeDiagramForDesigner`
  - `recalculateLanePositions`
  - `updateLinkCondition`
- `PolicyDataService`:
  - `getAllPolicies`
  - `getPolicyById`
  - `createPolicy`
  - `updatePolicyDiagram`
  - `getTaskExecutionOrder`
- `ExecutionService`:
  - `startProcess`
  - `getStartablePolicies`
  - `getMyTasks`
  - `getMyProcessTaskGroups`
  - `getTaskDetails`
  - `takeTask`
  - `completeTask`
  - `uploadFile`
- `PolicyMetricsService`:
  - `getPolicyMetrics`
- `CopilotService`:
  - `sendMessage`
  - `getHistoryByPolicy`
  - `applyChange`

### IA
- `DiagramAgentService`:
  - `process`
  - `_run_llm`
  - `_merge_diagrams`
  - `_normalize_lanes`
  - `_infer_lanes_from_diagram`
  - `_fallback_result`
- `diagram_tools.py`:
  - `create_default_diagram`
  - `sanitize_diagram`
  - `_ensure_node_meta`
  - `_ensure_link_style`
  - `_ensure_decision_link_condition`

## 7. Notas para prompts UML
Para que un generador UML no falle, usa siempre:
1. Actor explícito por CU.
2. Endpoint/método responsable por paso.
3. Regla de seguridad por rol.
4. Regla de lane (`laneId`, `x`, `width`, `height`) en flujos de diseñador.
5. Regla de decisión (`conditionLabel` + expresión) en secuencias BPMN.
6. Regla de sincronización real-time (`cell-sync` vs `full-sync`).

## Novedades FCM (Push)

### Funcionalidades nuevas
- Registro de token FCM desde app mobile hacia backend autenticado.
- Persistencia del token por usuario (`users.fcmToken`, `users.fcmTokenUpdatedAt`).
- Envio de push al generarse una nueva tarea pendiente para un carril/usuario.
- Manejo mobile de notificaciones en foreground, background y app terminada.
- Solicitud de permisos de notificacion compatible con Android 13+.

### Componentes involucrados
- Backend: `NotificationTokenController`, `FirebaseAdminConfig`, `FirebaseMessagingService`, `ProcessExecutionService`.
- Mobile: `push_notification_service.dart`, `main.dart`, `auth_service.dart`.

### Casos de uso nuevos
- `CU-21` Registrar token FCM de dispositivo movil.
- `CU-22` Recibir notificacion push al asignarse nueva tarea.
