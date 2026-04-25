# Implementación: Métricas SLA por Política

## Resumen

Se implementó soporte de métricas de rendimiento de tareas BPMN con enfoque en:

1. **Tiempo de espera** (desde que se crea la tarea hasta que se inicia).
2. **Tiempo de ejecución** (desde que se inicia hasta que se completa).
3. **Dashboard administrativo** para visualizar cuellos de botella por nodo.

---

## Backend

### 1) Refactor del modelo de tarea

- Archivo: `backend/src/main/java/com/politicanegocio/core/model/TaskInstance.java`
- Cambio:
  - Se agregó `startedAt: LocalDateTime`.

### 2) Inicio explícito de tarea (`startTask`)

- Archivo: `backend/src/main/java/com/politicanegocio/core/service/ProcessExecutionService.java`
- Cambios:
  - Nuevo método `startTask(String taskInstanceId, User user)`.
  - Validación estricta: solo permite iniciar tareas en estado `PENDING`.
  - Al iniciar:
    - `status = IN_PROGRESS`
    - `assignedTo = usuario`
    - `startedAt = LocalDateTime.now()`
  - `takeTask(...)` se mantiene como compatibilidad y delega a `startTask(...)`.

- Archivo: `backend/src/main/java/com/politicanegocio/core/controller/ProcessExecutionController.java`
- Endpoints:
  - `POST /api/execution/tasks/{taskInstanceId}/start` (nuevo)
  - `POST /api/execution/tasks/{taskInstanceId}/take` ahora también usa la lógica de `startTask`.

### 3) Completar tarea con validación de inicio

- Archivo: `backend/src/main/java/com/politicanegocio/core/service/ProcessExecutionService.java`
- Cambio:
  - `completeTask(...)` ahora exige que exista `startedAt`.
  - `completedAt` se registra únicamente cuando `startedAt` existe.

### 4) Endpoint de métricas por política

- Nuevo DTO:
  - `backend/src/main/java/com/politicanegocio/core/dto/PolicyTaskMetricDto.java`
- Nuevo servicio:
  - `backend/src/main/java/com/politicanegocio/core/service/MetricsService.java`
- Nuevo controller:
  - `backend/src/main/java/com/politicanegocio/core/controller/MetricsController.java`
- Nuevo endpoint:
  - `GET /api/metrics/policy/{policyId}`

### 5) Lógica de agregación

Por cada `taskId` (nodo BPMN) de una política:

- `totalCompleted`: conteo de tareas `COMPLETED`.
- `avgWaitMinutes` / `avgWaitHours`: promedio de `startedAt - createdAt`.
- `avgExecutionMinutes` / `avgExecutionHours`: promedio de `completedAt - startedAt`.
- `avgTotalMinutes`: suma promedio de espera + ejecución.

Orden de salida: descendente por `avgTotalMinutes` para identificar más fácil el cuello de botella.

### 6) Soporte de repositorio

- Archivo: `backend/src/main/java/com/politicanegocio/core/repository/ProcessInstanceRepository.java`
- Cambio:
  - `findByPolicyId(String policyId)` para ubicar instancias asociadas a la política.

---

## Frontend (Angular)

### 1) Botón "Ver Métricas" en listado de políticas

- Archivos:
  - `frontend/src/app/features/policy-manager/policy-manager.component.html`
  - `frontend/src/app/features/policy-manager/policy-manager.component.ts`
  - `frontend/src/app/features/policy-manager/policy-manager.component.scss`
- Cambio:
  - Se añadió acción `Ver Metricas` con icono visual tipo gráfico.
  - Navega al dashboard de métricas por política.

### 2) Nuevo dashboard `PolicyMetricsComponent`

- Archivos:
  - `frontend/src/app/features/policy-manager/policy-metrics/policy-metrics.component.ts`
  - `frontend/src/app/features/policy-manager/policy-metrics/policy-metrics.component.html`
  - `frontend/src/app/features/policy-manager/policy-metrics/policy-metrics.component.scss`
  - `frontend/src/app/features/policy-manager/services/policy-metrics.service.ts`
  - `frontend/src/app/features/policy-manager/models/policy-metrics.model.ts`

Funcionalidad:

- Consulta `GET /api/metrics/policy/{policyId}`.
- Muestra tabla comparativa por nodo:
  - Espera promedio
  - Ejecución promedio
  - Total promedio
  - Total completadas
- Resalta en rojo el nodo con mayor `avgTotalMinutes` como **Cuello de botella**.

### 3) Ruta nueva en panel admin

- Archivo: `frontend/src/app/app.routes.ts`
- Ruta agregada:
  - `/admin/policies/:policyId/metrics`

---

## Ajuste de compatibilidad móvil

- Archivo: `mobile/lib/features/tasks/screens/task_detail_screen.dart`
- Cambio:
  - Antes de completar, solo llama a `takeTask` cuando la tarea está en `PENDING`.
  - Evita reintentar inicio en tareas ya `IN_PROGRESS`.

---

## Validaciones ejecutadas

- Backend: `mvnw -q -DskipTests compile` OK
- Frontend: `npm run build` OK (con warnings existentes de presupuesto/ESM)
- Mobile: `flutter analyze` OK
