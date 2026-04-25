# Copilot: aplicar cambios visuales en el diagrama

## Problema

El chat del Copilot respondia recomendaciones, pero no actualizaba el lienzo JointJS.

## Solucion implementada

Se agrego un flujo `apply` para ejecutar cambios reales sobre el diagrama cuando el mensaje del usuario es de tipo modificacion.

## Backend (Spring)

### Nuevos DTOs

- `CopilotApplyRequestDto`
  - `instruction`
  - `currentDiagram`
  - `lanes`
  - `context`
- `CopilotApplyResponseDto`
  - `summary`
  - `changes`
  - `warnings`
  - `diagram`

### Endpoint nuevo

- `POST /api/copilot/apply`
  - Reenvia al microservicio IA:
    - `POST /api/v1/agent/diagram`
    - `operation=modify`

### Servicio

- `CopilotService.apply(...)`:
  - valida request
  - envia payload al motor IA
  - devuelve diagrama actualizado
  - mantiene manejo de errores detallado

## Frontend (Angular)

### Servicio

- `copilot.service.ts`
  - nuevo metodo `applyChange(...)` -> `POST /api/copilot/apply`

### Integracion en editor

- `policy-designer.component.ts`
  - en `sendToCopilot`, si el texto indica intencion de cambio (`agrega`, `modifica`, `conecta`, etc.), se llama `applyChange`.
  - el `diagram` devuelto se aplica al canvas mediante `applyPolicy(...)`.
  - se agrega mensaje en chat: `Cambio aplicado: ...`

## Resultado esperado

Cuando pidas una modificacion en lenguaje natural, el sistema no solo responde: ahora intenta aplicar el cambio y mostrarlo visualmente en el diagrama.
