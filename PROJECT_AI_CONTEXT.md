# PROJECT AI CONTEXT

## 1) Resumen ejecutivo
Proyecto BPMN colaborativo multi-cliente con 3 aplicaciones:
- `backend/`: API principal (Spring Boot + MongoDB + WebSocket STOMP + S3).
- `frontend/`: panel web administrativo y de ejecucion interna (Angular + JointJS).
- `mobile/`: app Flutter para clientes (`CLIENT`) para iniciar tramites y gestionar tareas.

Dominio principal:
- Diseno de politicas de negocio en BPMN (lienzo JointJS).
- Ejecucion de procesos e instancias de tareas.
- Colaboracion en tiempo real en el disenador via WebSockets.
- Adjuntos en formularios via AWS S3.

---

## 2) Estructura del repositorio
- `backend/` -> API Spring Boot.
- `frontend/` -> Angular app (`flow-web`).
- `mobile/` -> Flutter app para cliente final.
- `infra/` -> `docker-compose.yml` e infraestructura local.

Documentacion existente:
- `ARCHITECTURE.md`
- `ARQUITECTURA_PROYECTO.md`
- otros md historicos de cambios.

Este archivo es el contexto consolidado y actualizado para agentes IA.

---

## 3) Stack tecnologico

### Backend
- Java 17
- Spring Boot (parent `4.0.5`)
- Spring Web + WebSocket + Security + GraphQL
- Spring Data MongoDB
- STOMP broker relay con RabbitMQ
- AWS SDK v2 (S3)
- Lombok

Dependencias clave: `backend/pom.xml`.

### Frontend web
- Angular 21 standalone
- JointJS Plus (`@joint/plus`) para el lienzo BPMN
- STOMP JS (`@stomp/stompjs`) para colaboracion en tiempo real

### Mobile
- Flutter (Dart)
- `dio` para HTTP
- `flutter_secure_storage` para token
- `flutter_dotenv` para config
- `file_picker` para adjuntos locales

### Infra local
- Docker Compose con:
  - frontend (nginx)
  - backend
  - rabbitmq (management + plugin stomp)
  - postgres (simulacion local; no es la DB principal actual)

---

## 4) Persistencia y modelos de datos

### DB principal actual
- MongoDB (`spring.mongodb.uri`)

### Colecciones principales
- `users`
- `areas`
- `companies`
- `policies`
- `process_instances`
- `task_instances`

### Entidades clave
- `User`: `username`, `roles`, `company`, `area`, `laneId`.
- `Policy`: `diagramJson`, `startLaneId`, metadatos de empresa.
- `ProcessInstance`: `policyId`, `startedBy`, `status`.
- `TaskInstance`: `processInstanceId`, `taskId`, `laneId`, `status`, `formData`.

---

## 5) Roles y reglas de negocio
Roles usados en el sistema:
- `SOFTWARE_ADMIN`
- `COMPANY_ADMIN`
- `FUNCTIONARY`
- `CLIENT`

Reglas importantes vigentes:
- Cada empresa tiene area `Cliente` por defecto.
- `FUNCTIONARY` debe pertenecer a un area distinta de `Cliente`.
- `CLIENT` se administra separado y pertenece al area `Cliente`.
- Canonico de carril cliente: **`Cliente`**.
  - Internamente hay normalizacion para compatibilidad con legado `lane_cliente` en lectura/creacion de tareas.

---

## 6) Seguridad y autenticacion

### Auth
- Endpoints REST: `/api/auth/login`, `/api/auth/mobile/login`, `/api/auth/register`, `/api/auth/me`.
- Token actual: no JWT firmado; token en memoria (`TokenService`, UUID con expiracion 6h).
- `AuthTokenFilter` resuelve `Bearer` y carga `User` en `SecurityContext`.

### Nota critica
- El modelo actual de token en memoria no es ideal para multi-instancia/produccion porque no es distribuido ni persistente.

### CORS y autorizacion
- Config en `SecurityConfig`.
- `@EnableMethodSecurity` y `@PreAuthorize` en endpoints criticos.

---

## 7) Backend - modulos funcionales

### 7.1 Administracion (`/api/admin`)
Controlador: `AdminController`.
- Empresas: crear/listar.
- Company admins: CRUD.
- Areas: CRUD.
- Funcionarios: CRUD.
- Clientes: CRUD.

Servicio: `AdminService` (validaciones de unicidad y reglas por rol).

### 7.2 Politicas BPMN
Servicio: `PolicyService`.
Funciones clave:
- Crear politica.
- Guardar/actualizar grafo (`diagramJson`).
- Determinar `startLaneId` (parser BPMN/JSON).
- Listar politicas iniciables segun usuario/rol.

Parser: `BpmnStartLaneParser`.

### 7.3 Ejecucion de procesos y tareas (`/api/execution`)
Controlador: `ProcessExecutionController`.
Endpoints principales:
- `POST /process/start`
- `POST /tasks/{id}/take`
- `POST /tasks/{id}/complete`
- `GET /tasks/{id}`
- `GET /startable-policies`
- `GET /my-tasks`
- `GET /client/tasks/pending` (compatibilidad, delega en logica unificada)

Servicio: `ProcessExecutionService`.
Incluye:
- Motor de avance recursivo (fork/join/end).
- Creacion de `TaskInstance` pendientes.
- Mapeo a DTOs de bandeja.
- Validaciones de acceso por tarea (`assertUserCanAccessTask`).

### 7.4 Upload de archivos a S3
Controlador: `FileUploadController`.
- `POST /api/files/upload` multipart (`file`, opcional `policyId`).

Servicio: `S3Service`.

### 7.5 WebSockets colaborativos
Config: `WebSocketConfig`.
- Endpoints STOMP: `/ws-designer`, `/ws-bpmn`.
- App prefix: `/app`.
- Broker relay RabbitMQ: `/topic`, `/queue`.

Controller socket: `DesignerSocketController`.
- Entrada: `/app/policy/{policyId}/change`
- Broadcast: `/topic/policy.{policyId}`

---

## 8) Frontend web (Angular)

### Routing principal
Archivo: `frontend/src/app/app.routes.ts`
- `/login`
- `/admin-software`
- `/admin/funcionarios`
- `/admin/policies`
- `/designer/:id`
- `/funcionario-dashboard`
- `/execution/task/:id`
- `/bandeja-tareas`

### Modulos relevantes
- Auth/Login
- Admin software y admin company
- Policy manager
- Policy designer (JointJS + STOMP)
- Execution dashboard e inbox

### Colaboracion en tiempo real
- Servicio websocket: `web-socket.service.ts`
- Suscripcion a `/topic/policy.{policyId}`
- Publicacion a `/app/policy/{policyId}/change`

### Proxy local
- `frontend/proxy.conf.json` -> `/api`, `/graphql`, `/ws-designer` hacia `localhost:8080`.

---

## 9) Mobile (Flutter)

### Config base
- `main.dart` inicializa `Env`, `ApiClient`, `AuthService`, `DashboardService`, `TaskService`.
- `app.dart` usa `AuthGate` con `FutureBuilder`.

### Auth cliente
- Login via `/api/auth/mobile/login`.
- Token persistido en `flutter_secure_storage`.

### Dashboard cliente
- Lista politicas iniciables (`/api/execution/startable-policies`).
- Permite iniciar proceso (`/api/execution/process/start`).
- Navega a bandeja de tareas pendientes.

### Tareas cliente
Feature path: `mobile/lib/features/tasks/`
- `models/pending_task_model.dart`
- `models/task_detail_model.dart`
- `services/task_service.dart`
- `screens/pending_tasks_screen.dart`
- `screens/task_detail_screen.dart`

Flujo:
1. Obtener tareas (`/api/execution/my-tasks`).
2. Ver detalle (`/api/execution/tasks/{id}`).
3. Tomar tarea (`/take`).
4. Subir adjuntos a S3 (`/api/files/upload`).
5. Completar tarea (`/complete`, `formData` serializado JSON string).

UI reactiva:
- `FutureBuilder` en bandejas/detalles para evitar pantallas en blanco.

---

## 10) Infraestructura y despliegue local

`infra/docker-compose.yml` levanta:
- `bpmn-frontend`
- `bpmn-backend`
- `bpmn-rabbitmq`
- `bpmn-postgres`

Variables de entorno backend esperadas:
- Mongo: `SPRING_DATA_MONGODB_URI` / `MONGODB_URI`
- RabbitMQ: `SPRING_RABBITMQ_HOST`, `SPRING_RABBITMQ_STOMP_PORT`, `SPRING_RABBITMQ_USERNAME`, `SPRING_RABBITMQ_PASSWORD`
- S3: `AWS_REGION`, `AWS_S3_BUCKET`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`
- Upload size y otras

---

## 11) Convenciones y decisiones de arquitectura
- Canonico lane cliente: `Cliente`.
- Compatibilidad legado: normalizacion interna para `lane_cliente` en servicios criticos.
- `TaskInstance.laneId` debe guardar el valor normalizado.
- Para clientes, aislamiento estricto por `ProcessInstance.startedBy`.
- Endpoints de tareas tienden a unificarse en `/api/execution/my-tasks` + validaciones de acceso por usuario.

---

## 12) Riesgos tecnicos actuales (importante para agentes IA)
1. `TokenService` en memoria (no distribuido).
2. Hay mezcla historica REST + GraphQL en ejecucion; revisar ambos al refactorizar firmas.
3. Mensajes/logs y comentarios con mojibake en algunos archivos (encoding).
4. Se detectaron credenciales hardcodeadas en `backend/src/main/resources/application.properties`.
   - Debe migrarse a variables de entorno antes de compartir repo o desplegar.

---

## 13) Guia rapida para nuevos agentes IA

Orden recomendado para tocar el sistema sin romper:
1. Revisar modelos + repositorios de `ProcessInstance`/`TaskInstance`.
2. Revisar `ProcessExecutionService` (es el core de reglas de ejecucion).
3. Validar controladores REST y GraphQL en paralelo.
4. Si hay cambios de lane/roles, validar tambien:
   - `PolicyService`
   - `AdminService`
   - `AuthService`
   - mobile `TaskService` y `DashboardService`
5. Ejecutar siempre:
   - backend compile: `mvnw -q -DskipTests compile`
   - mobile analyze: `flutter analyze`
   - frontend build: `npm run build`

---

## 14) Comandos utiles
Backend:
- `cd backend && mvnw spring-boot:run`
- `cd backend && mvnw -q -DskipTests compile`

Frontend:
- `cd frontend && npm install`
- `cd frontend && npm start`
- `cd frontend && npm run build`

Mobile:
- `cd mobile && flutter pub get`
- `cd mobile && flutter run`
- `cd mobile && flutter analyze`

Infra:
- `cd infra && docker compose up --build`

---

## 15) Estado funcional actual (alto nivel)
- Web admin: gestion de empresas, admins, areas, funcionarios y clientes.
- Disenador BPMN: colaborativo por WebSockets + persistencia.
- Cliente mobile: login, lista de politicas iniciables, inicio de tramite, bandeja pendiente, detalle de tarea, completar tarea y adjuntos S3.

Este documento debe actualizarse cada vez que cambien contratos API, reglas de lane/rol o estructura de features.
