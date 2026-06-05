# Ticket: FASE 1 - Reestructuración Core y Almacenamiento

## Plan de Acción
Archivos modificados:
- `backend/src/main/java/com/politicanegocio/core/model/PolicyInitialRequirement.java`
- `backend/src/main/java/com/politicanegocio/core/model/Policy.java`
- `backend/src/main/resources/graphql/schema.graphqls`
- `backend/src/main/java/com/politicanegocio/core/graphql/PolicyGraphQLController.java`
- `backend/src/main/java/com/politicanegocio/core/service/PolicyService.java`
- `backend/src/main/java/com/politicanegocio/core/dto/StartablePolicyDto.java`
- `backend/src/main/java/com/politicanegocio/core/controller/ProcessExecutionController.java`
- `backend/src/main/java/com/politicanegocio/core/service/S3Service.java`
- `backend/src/main/java/com/politicanegocio/core/controller/FileUploadController.java`
- `backend/src/main/java/com/politicanegocio/core/dto/TaskDetailDto.java`
- `backend/src/main/java/com/politicanegocio/core/service/ProcessExecutionService.java`
- `frontend/src/app/features/policy-designer/models/policy-designer.models.ts`
- `frontend/src/app/features/policy-designer/services/policy-data.service.ts`
- `frontend/src/app/features/policy-designer/components/policy-designer/policy-designer.component.ts`
- `frontend/src/app/features/policy-designer/components/policy-designer/policy-designer.component.html`
- `frontend/src/app/features/execution/models/execution.models.ts`
- `frontend/src/app/features/execution/services/execution.service.ts`
- `frontend/src/app/features/execution/components/task-execution/task-execution.component.ts`

## Modificaciones

### [archivo: backend/src/main/java/com/politicanegocio/core/model/PolicyInitialRequirement.java]
- Se creó el nuevo modelo `PolicyInitialRequirement`.
- Funcionalidad: representa los requisitos iniciales de un trámite con: `id`, `name`, `description`, `required`, `allowedExtensions`.

### [archivo: backend/src/main/java/com/politicanegocio/core/model/Policy.java]
- Se agregó `initialRequirements` a la entidad `Policy`.
- Funcionalidad: cada Policy ahora persiste su lista de requisitos iniciales en MongoDB.

### [archivo: backend/src/main/resources/graphql/schema.graphqls]
- Se agregaron tipo e input GraphQL para requisitos iniciales:
  - `PolicyInitialRequirement`
  - `PolicyInitialRequirementInput`
- Se extendió `Policy` para exponer `initialRequirements`.
- Se actualizaron mutaciones:
  - `createPolicy(..., initialRequirements)`
  - `updatePolicyGraph(..., initialRequirements)`
- Funcionalidad: el frontend puede crear/editar requisitos iniciales junto con el diagrama.

### [archivo: backend/src/main/java/com/politicanegocio/core/graphql/PolicyGraphQLController.java]
- Se actualizó el controlador GraphQL para recibir y enviar `initialRequirements` en `createPolicy` y `updatePolicyGraph`.
- Funcionalidad: enruta la nueva data al servicio de dominio.

### [archivo: backend/src/main/java/com/politicanegocio/core/service/PolicyService.java]
- Se actualizaron firmas:
  - `createPolicy(name, description, initialRequirements)`
  - `updatePolicyGraph(policyId, diagramJson, lanes, initialRequirements)`
- Se agregó saneamiento defensivo de requisitos (`sanitizeInitialRequirements`):
  - ignora nulos y registros incompletos,
  - normaliza extensiones,
  - elimina duplicados,
  - protege contra valores vacíos.
- Funcionalidad: persistencia consistente y validada de requisitos iniciales.

### [archivo: backend/src/main/java/com/politicanegocio/core/dto/StartablePolicyDto.java]
- Se agregó `initialRequirements` al DTO de trámites iniciables.
- Funcionalidad: el frontend de ejecución puede conocer qué documentos pide cada trámite.

### [archivo: backend/src/main/java/com/politicanegocio/core/controller/ProcessExecutionController.java]
- Se actualizó el mapper `toStartablePolicyDto` para incluir requisitos iniciales.
- Funcionalidad: exposición completa del nuevo modelo en `/api/execution/startable-policies`.

### [archivo: backend/src/main/java/com/politicanegocio/core/service/S3Service.java]
- Se refactorizó subida para usar parámetros de negocio: `clientId`, `processInstanceId`, `documentId`.
- Se reemplazó la generación anterior por key jerárquica:
  - `clientes/{clientId}/tramites/{processInstanceId}/{documentId}_{fileName}`
- Se añadió sanitización defensiva de segmentos de ruta y fallback seguro.
- Funcionalidad: cumplimiento de la nueva estructura S3 y reducción de riesgo por nombres inválidos.

### [archivo: backend/src/main/java/com/politicanegocio/core/controller/FileUploadController.java]
- API `/api/files/upload` ahora recibe:
  - `processInstanceId` (opcional)
  - `documentId` (opcional)
- `clientId` se deriva del usuario autenticado (id/username fallback).
- Funcionalidad: garantiza que el path S3 se construya con contexto real de cliente y trámite.

### [archivo: backend/src/main/java/com/politicanegocio/core/dto/TaskDetailDto.java]
- Se agregó `processInstanceId` al DTO.
- Funcionalidad: permite al frontend adjuntar documentos asociados al trámite correcto.

### [archivo: backend/src/main/java/com/politicanegocio/core/service/ProcessExecutionService.java]
- Se actualizó construcción de `TaskDetailDto` para incluir `processInstanceId`.
- Funcionalidad: propagación del identificador de instancia hasta la UI.

### [archivo: frontend/src/app/features/policy-designer/models/policy-designer.models.ts]
- Se agregó interfaz `PolicyInitialRequirement`.
- Se extendieron `PolicySummary` y `PolicyPayload` con `initialRequirements`.
- Funcionalidad: tipado fuerte para requisitos iniciales en el modelador.

### [archivo: frontend/src/app/features/policy-designer/services/policy-data.service.ts]
- Se amplió GraphQL de lectura (`getAllPolicies`, `getPolicyById`) para traer `initialRequirements`.
- `createPolicy` ahora soporta `initialRequirements`.
- `updatePolicyDiagram` ahora envía `initialRequirements`.
- Funcionalidad: sincronización full-stack de requisitos iniciales con backend.

### [archivo: frontend/src/app/features/policy-designer/components/policy-designer/policy-designer.component.ts]
- Se agregó estado UI para requisitos iniciales.
- Se agregaron acciones:
  - `addInitialRequirement()`
  - `removeInitialRequirement()`
  - normalización/parsing de extensiones.
- Se incorporó `initialRequirements` en:
  - carga de policy (`applyPolicy`),
  - autosave (`persistPolicyGraph`),
  - guardado tras cambios de Copilot.
- Funcionalidad: el administrador puede definir/editar requisitos iniciales desde el diseñador y se persisten automáticamente.

### [archivo: frontend/src/app/features/policy-designer/components/policy-designer/policy-designer.component.html]
- Se agregó bloque de UI “REQUISITOS INICIALES” con:
  - formulario de creación (nombre, descripción, extensiones, obligatorio/opcional),
  - listado y eliminación de requisitos.
- Funcionalidad: gestión visual directa dentro del modelador BPMN.

### [archivo: frontend/src/app/features/execution/models/execution.models.ts]
- Se agregó `PolicyInitialRequirement` para ejecución.
- `StartablePolicyDto` ahora incluye `initialRequirements`.
- `TaskDetailDto` ahora incluye `processInstanceId`.
- Funcionalidad: la capa de ejecución conoce los requisitos y contexto de trámite.

### [archivo: frontend/src/app/features/execution/services/execution.service.ts]
- `uploadFile` ahora envía:
  - `processInstanceId`
  - `documentId`
- Funcionalidad: alinea los uploads con la nueva estructura de almacenamiento S3.

### [archivo: frontend/src/app/features/execution/components/task-execution/task-execution.component.ts]
- En subida de archivos se usa `processInstanceId` de la tarea y `controlName` como `documentId`.
- Funcionalidad: los adjuntos del formulario quedan organizados por trámite y documento en S3.

## Validación ejecutada
- Frontend build: `npm run build` exitoso.
- Backend compile: `mvnw -q -DskipTests compile` exitoso.
