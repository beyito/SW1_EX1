# Funcionalidades y Casos de Uso (CU)

Este documento se construye tomando como base `ARCHITECTURE.md` y validando la implementación real del repositorio.

## 1) Autenticación y sesión

### CU-01: Login Web
- Actor: `SOFTWARE_ADMIN`, `COMPANY_ADMIN`, `FUNCTIONARY`.
- Frontend:
  - Método: `AuthService.login(username, password)` en `frontend/src/app/auth.service.ts`.
  - Llamada: `POST /api/auth/login`.
- Backend:
  - Endpoint: `AuthController.login(...)` (`backend/src/main/java/com/politicanegocio/core/controller/AuthController.java`).
  - Servicio: `AuthService.login(...)` -> `authenticate(...)` -> `TokenService.createTokenForUser(...)`.
  - Repositorio: `UserRepository.findByUsername(...)`.
- Flujo de información:
  1. Front envía `username/password`.
  2. Backend valida credenciales, genera token en memoria (6h).
  3. Respuesta: `{ token, username, roles, company, parentCompany, area, laneId }`.
  4. Front guarda token/perfil en `localStorage`.

### CU-02: Login Mobile (solo clientes)
- Actor: `CLIENT`.
- Mobile:
  - Método: `AuthService.login(...)` en `mobile/lib/features/auth/data/auth_service.dart`.
  - Llamada: `POST /api/auth/mobile/login`.
- Backend:
  - Endpoint: `AuthController.loginMobile(...)`.
  - Servicio: `AuthService.loginForMobileClient(...)` (valida que pertenezca a área cliente).
  - Token: `TokenService`.
- Flujo:
  1. Mobile envía credenciales.
  2. Backend autentica y restringe a usuarios de carril/área cliente.
  3. Mobile guarda token en `SecureStorage`.

### CU-03: Registro inicial
- Frontend: `AuthService.register(...)` -> `POST /api/auth/register`.
- Backend: `AuthController.register(...)` -> `AuthService.registerUser(...)`.
- Validaciones clave:
  - username único.
  - roles permitidos según reglas actuales.

### CU-04: Obtener perfil autenticado
- Endpoint: `GET /api/auth/me`.
- Backend: `AuthController.me(...)`.
- Fuente de usuario: `SecurityContext` cargado por `AuthTokenFilter` con `Bearer token`.

## 2) Administración (empresa/usuarios/áreas)

Todos los endpoints están en `AdminController` (`/api/admin`) y lógica en `AdminService`.

### CU-05: Gestión de empresas (Software Admin)
- Endpoints:
  - `POST /api/admin/companies`
  - `GET /api/admin/companies`
- Métodos:
  - `AdminService.createCompany(...)`
  - `AdminService.listCompanies()`
- Repositorios: `CompanyRepository`, `AreaRepository`.
- Flujo:
  1. Software admin crea empresa.
  2. Se persiste empresa en Mongo.
  3. Se garantiza creación del área `Cliente`.

### CU-06: Gestión de company-admins (Software Admin)
- Endpoints:
  - `POST /api/admin/company-admins`
  - `GET /api/admin/company-admins`
  - `PUT /api/admin/company-admins/{userId}`
  - `DELETE /api/admin/company-admins/{userId}`
- Métodos: `createCompanyAdmin`, `listCompanyAdmins`, `updateCompanyAdmin`, `deleteCompanyAdmin`.
- Repositorio: `UserRepository`.

### CU-07: Gestión de áreas (Company Admin)
- Endpoints:
  - `POST /api/admin/areas`
  - `GET /api/admin/areas`
  - `PUT /api/admin/areas/{areaId}`
  - `DELETE /api/admin/areas/{areaId}`
- Métodos: `createArea`, `listCompanyAreas`, `updateArea`, `deleteArea`.
- Reglas:
  - No eliminar área `Cliente`.
  - No eliminar área con usuarios asignados.
  - Al renombrar área, actualiza `area/laneId` de usuarios afectados.

### CU-08: Gestión de funcionarios (Company Admin)
- Endpoints:
  - `POST /api/admin/functionaries`
  - `GET /api/admin/functionaries`
  - `PUT /api/admin/functionaries/{userId}`
  - `DELETE /api/admin/functionaries/{userId}`
- Métodos: `createFunctionary`, `listCompanyFunctionaries`, `updateFunctionary`, `deleteFunctionary`.
- Regla: funcionario no puede pertenecer al área `Cliente`.

### CU-09: Gestión de clientes (Company Admin)
- Endpoints:
  - `POST /api/admin/clients`
  - `GET /api/admin/clients`
  - `PUT /api/admin/clients/{userId}`
  - `DELETE /api/admin/clients/{userId}`
- Métodos: `createClient`, `listCompanyClients`, `updateClient`, `deleteClient`.
- Regla: cliente queda en área/carril `Cliente`.

## 3) Diseño BPMN (GraphQL + colaboración)

### CU-10: Crear política
- Frontend: `PolicyDataService.createPolicy(...)` (GraphQL mutation).
- Backend GraphQL: `PolicyGraphQLController.createPolicy(...)`.
- Servicio: `PolicyService.createPolicy(...)`.
- Persistencia: `PolicyRepository.save(...)`.

### CU-11: Listar/consultar políticas
- GraphQL queries:
  - `getAllPolicies`
  - `getPolicyById`
- Backend: `PolicyGraphQLController` -> `PolicyService`.
- Regla: aislamiento por empresa (`companyId`).

### CU-12: Guardado de diagrama (auto-save)
- Frontend:
  - `policy-designer.component.ts`: `persistPolicyGraph()`.
  - Llama `PolicyDataService.updatePolicyDiagram(...)`.
- Backend:
  - GraphQL mutation `updatePolicyGraph(...)`.
  - `PolicyService.updatePolicyGraph(...)` recalcula `laneId` por posición y carriles.
- Datos:
  - `diagramJson` + `lanes`.
  - Se actualiza `startLaneId`, `updatedAt`.

### CU-13: Calcular orden de ejecución de tareas
- Front: `PolicyDataService.getTaskExecutionOrder(policyId)`.
- GraphQL query: `getTaskExecutionOrder`.
- Servicio: `PolicyService.getTaskExecutionOrder(...)`.

### CU-14: Colaboración en tiempo real del diseñador
- Front WebSocket:
  - Conexión: `WebSocketService.connect()` a `ws(s)://<host>/ws-designer`.
  - Publicación: `/app/policy/{policyId}/change`.
  - Suscripción: `/topic/policy.{policyId}`.
- Backend:
  - `@MessageMapping("/policy/{policyId}/change")` en `DesignerSocketController`.
  - Reenvío por `SimpMessagingTemplate` a `/topic/policy.{policyId}`.
- Broker:
  - `WebSocketConfig` usa STOMP relay RabbitMQ (`/topic`, `/queue`).

### CU-14A: Panel de propiedades de flecha desde nodo de decisión
- Frontend:
  - Componente standalone: `link-properties-panel.component` (Reactive Forms).
  - Se activa al seleccionar una flecha cuyo nodo origen tenga `nodeType: "DECISION"`.
- Lectura (JointJS -> Angular):
  - Se lee `link.prop('condition')` y `conditionLabel`.
- Escritura (Angular -> JointJS):
  - Ruta por defecto: `link.prop('condition', { type: 'default' })`.
  - Ruta evaluable: se autogenera script con etiqueta:
    `link.prop('condition', { type: 'expression', script: "#_decisionTomada == 'Sí'" })`.
  - Ya no se capturan manualmente variable/operador/valor en UI.
  - Además actualiza etiqueta visual del link para reflejar la condición.

## 4) Ejecución de procesos y tareas

### CU-15: Iniciar proceso
- Front/Mobile: `POST /api/execution/process/start`.
- Backend: `ProcessExecutionController.startProcess(...)`.
- Servicio: `ProcessExecutionService.startProcess(...)`.
- Flujo:
  1. Valida que usuario puede iniciar según `startLaneId` (`PolicyService.canUserStartPolicy`).
  2. Crea `ProcessInstance` (`ACTIVE`).
  3. Obtiene nodos siguientes con `WorkflowEngine.getNextNodes(...)`.
  4. `advanceWorkflow(...)` crea tareas pendientes o avanza por `FORK/JOIN/END`.

### CU-16: Tomar tarea
- Endpoint: `POST /api/execution/tasks/{taskInstanceId}/take`.
- Servicio: `ProcessExecutionService.takeTask(...)`.
- Reglas:
  - Pasa de `PENDING` -> `IN_PROGRESS`.
  - Evita tomar tarea ya tomada por otro usuario.
  - Valida acceso por carril/usuario.

### CU-17: Completar tarea
- Endpoint: `POST /api/execution/tasks/{taskInstanceId}/complete`.
- Payload: `{ formData }` (JSON serializado).
- Servicio: `ProcessExecutionService.completeTask(...)`.
- Flujo:
  1. Valida acceso y estado.
  2. Valida campos requeridos dinámicos del formulario.
  3. Marca `COMPLETED`, guarda `formData`.
  4. Parsea variables de ruteo desde `formData` (incluyendo `_decisionTomada` cuando aplica).
  5. Ejecuta avance de flujo (`advanceWorkflow`) y crea siguientes tareas / cierra proceso.

### CU-17A: Lookahead Routing en tareas previas a decisión
- Frontend (`TaskExecutionComponent`) realiza lookahead con `diagramJson` + `taskId`:
  1. Busca el siguiente nodo inmediato de la tarea actual.
  2. Si es `DECISION`, extrae `conditionLabel` de sus salidas.
- UI:
  - si no hay opciones: botón normal `Completar Tarea`.
  - si hay opciones: botones dinámicos `Completar: <opción>`.
- Envío:
  - al pulsar botón dinámico se agrega `_decisionTomada` al payload de `formData`.

### CU-18: Bandeja de tareas (usuario actual)
- Endpoint: `GET /api/execution/my-tasks`.
- Servicio: `ProcessExecutionService.getMyTasks(...)`.
- Regla:
  - `CLIENT`: tareas por procesos iniciados por sí mismo.
  - Interno: combina tareas por carril + tareas asignadas.

### CU-19: Detalle de tarea
- Endpoint: `GET /api/execution/tasks/{taskInstanceId}`.
- Servicio: `ProcessExecutionService.getTaskDetail(...)`.
- Respuesta:
  - `id`, `policyId`, `taskId`, nombre proceso/tarea, estado, descripción, `formSchema`, `formData`, `diagramJson`.

### CU-19A: Gateway DECISION automático en backend
- `WorkflowEngine` evalúa condiciones de links salientes de nodos `DECISION`:
  - prioriza links `condition.type = expression` cuyo SpEL resulte `true`;
  - si ninguno cumple, usa la ruta `condition.type = default`.
- `ProcessExecutionService.advanceWorkflow(...)` trata `DECISION` como gateway (auto-avance), no como tarea humana.
- Resultado: se activa un solo camino por decisión, evitando abrir ramas SI/NO simultáneamente.

### CU-20: Políticas iniciables
- Endpoint: `GET /api/execution/startable-policies`.
- Servicio: `PolicyService.getStartablePoliciesForUser(...)`.
- Regla por carril/rol (`CLIENT` -> `Cliente`).

### CU-21: Pendientes de cliente (compatibilidad)
- Endpoint: `GET /api/execution/client/tasks/pending`.
- Servicio: `ProcessExecutionService.getMyPendingTasks(...)`.

### CU-22: Operaciones de ejecución vía GraphQL
- Queries:
  - `myTasks`
  - `getTaskDetail(taskId)`
- Mutations:
  - `takeTask(taskId)`
  - `completeTask(taskId, formData)`
- Controlador: `ExecutionGraphQLController`.

## 5) Adjuntos y S3

### CU-23: Subir adjunto desde diseñador o ejecución
- Front web:
  - `FileService.uploadAttachment(...)` o `ExecutionService.uploadFile(...)`.
  - Endpoint: `POST /api/files/upload` (`multipart/form-data`).
- Mobile:
  - `TaskService.uploadFileToS3(...)` -> mismo endpoint.
- Backend:
  - `FileUploadController.upload(...)` -> `S3Service.upload(...)`.
- S3:
  1. Construye `key` (`policies/{policyId}/attachments/...`).
  2. `putObject` al bucket.
  3. Genera URL firmada (`presigned GET`).
  4. Devuelve `{ key, url, contentType, size }`.
- Persistencia funcional:
  - Front guarda `url`/`key` en metadata del diagrama o `formData` de tarea.

## 6) Seguridad y control de acceso

### CU-24: Autorización por token
- Filtro: `AuthTokenFilter`.
- Si recibe `Authorization: Bearer <token>`:
  - consulta `AuthService.getUserByToken` -> `TokenService.findUserByToken`.
  - coloca `Authentication` en `SecurityContext`.

### CU-25: CORS y rutas públicas/privadas
- Config: `SecurityConfig`.
- CORS: `app.cors.allowed-origins` (lista separada por comas, con `trim`).
- Públicas:
  - `/api/auth/**`
  - `/graphiql/**`
  - `/ws-designer` (handshake).
- El resto requiere autenticación.

---

## Referencias clave
- `ARCHITECTURE.md`
- `backend/src/main/java/com/politicanegocio/core/controller/*`
- `backend/src/main/java/com/politicanegocio/core/service/*`
- `backend/src/main/java/com/politicanegocio/core/graphql/*`
- `frontend/src/app/features/*`
- `mobile/lib/features/*`
