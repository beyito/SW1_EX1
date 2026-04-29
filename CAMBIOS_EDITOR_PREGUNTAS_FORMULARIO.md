# Cambios - Editor de preguntas del formulario (Policy Designer)

## Problemas reportados
1. No existía un control explícito para marcar una pregunta como **campo obligatorio**.
2. No era posible **editar** una pregunta ya creada (solo eliminar y volver a crear).

## Solución implementada

### 1) Campo obligatorio visible y editable
Se agregó en el diseñador de preguntas un checkbox:
- `Campo obligatorio`

Este valor ahora se guarda en `FormField.required` y se mantiene en la metadata del nodo `TASK`.

### 2) Edición real de preguntas existentes
Se implementó flujo de edición:
- Botón `Editar` en cada pregunta.
- Carga de datos en el formulario de edición.
- Botón `Guardar cambios`.
- Botón `Cancelar edición`.

El flujo conserva el `id` del campo para no romper referencias y actualiza en sitio el arreglo `selectedTaskFormFields`.

## Archivos modificados
- `frontend/src/app/features/policy-designer/components/policy-designer/policy-designer.component.ts`
  - Nuevos estados:
    - `newFieldRequired`
    - `editingFieldIndex`
  - Lógica de edición/creación unificada en `addTaskField()`
  - Nuevo método `cancelEditField()`
  - `editField(index)` ahora sí precarga y habilita edición real.
- `frontend/src/app/features/policy-designer/components/policy-designer/policy-designer.component.html`
  - Nuevo checkbox `Campo obligatorio`
  - UI dinámica `Agregar pregunta` / `Editar pregunta`
  - Botones `Guardar cambios` y `Cancelar edición`

## Impacto funcional
- El modelador ahora define correctamente obligatoriedad de respuestas sin depender solo de “requiere adjunto”.
- Se mejora la mantenibilidad del diseño BPMN al permitir correcciones de preguntas sin eliminarlas.

