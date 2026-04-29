# Cambios - Validación estricta de formulario en completado de tareas

## Problema detectado
Se podía completar una tarea aunque el formulario dinámico estuviera incompleto, incluyendo casos con:
- campos obligatorios sin respuesta,
- campos boolean/checkbox requeridos en `false`,
- campos de adjunto sin archivo/URL cargada.

## Solución implementada
Se reforzó la validación en dos capas:

1. **Backend (regla definitiva)**
   - Archivo: `backend/src/main/java/com/politicanegocio/core/service/ProcessExecutionService.java`
   - En `completeTask(...)`:
     - Se considera obligatorio si `required=true` **o** `requiresAttachment=true`.
     - Se valida por tipo:
       - `checkbox`/`boolean`: debe ser `true`.
       - `file` o `requiresAttachment=true`: debe tener valor no vacío.
       - `string`: no vacío.
       - `array`: no vacío.
     - Se agregó helper `isMissingRequiredValue(...)`.

2. **Frontend (UX preventiva)**
   - Archivo: `frontend/src/app/features/execution/models/execution.models.ts`
     - Se agregan props opcionales al modelo:
       - `requiresAttachment?: boolean`
       - `attachmentLabel?: string`
   - Archivo: `frontend/src/app/features/execution/components/task-execution/task-execution.component.ts`
     - En `buildForm(...)`, un campo se considera requerido si:
       - `required === true` o `requiresAttachment === true`.
   - Archivo: `frontend/src/app/features/execution/components/task-execution/task-execution.component.html`
     - El asterisco de requerido ahora se muestra también para `requiresAttachment`.

## Resultado esperado
- Si existe formulario, no se completa la tarea hasta cumplir todas las reglas de obligatoriedad.
- Si un adjunto es obligatorio y no se cargó, backend rechaza la operación.
- Si un checkbox/boolean obligatorio está en `false`, backend rechaza la operación.

