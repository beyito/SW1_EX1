# Cambios - Control de Errores de IA (Mensajes Amigables)

## Objetivo
Evitar mensajes técnicos o “feos” cuando el agente de IA falla (caído, saturado, timeout o respuesta inválida), mostrando errores claros para usuario final.

## Cambios implementados

### 1) Backend Spring Boot (`CopilotService`)
Archivo:
- `backend/src/main/java/com/politicanegocio/core/service/CopilotService.java`

Mejoras:
- Se agregaron helpers centralizados para normalizar errores:
  - `unavailableAiException(...)` -> **503 Service Unavailable**
  - `timeoutAiException(...)` -> **504 Gateway Timeout**
  - `badGatewayAiException(...)` -> **502 Bad Gateway**
- Se aplicaron en operaciones:
  - `chat`
  - `apply`
  - `voice-fill`

Resultado:
- El backend ya no expone mensajes crudos de excepciones técnicas al usuario final.
- Mantiene `requestId` para trazabilidad operativa.

### 2) Frontend Policy Designer (`copilot.service.ts`)
Archivo:
- `frontend/src/app/features/policy-designer/services/copilot.service.ts`

Mejoras:
- Se incorporó `mapAiError(status, raw, fallback)` para transformar respuestas HTTP en mensajes amigables:
  - 503 -> “servicio IA no disponible”
  - 504 -> “servicio IA saturado/demorado”
  - 502 -> “respuesta IA inválida”
  - 5xx -> “error temporal”

### 3) Frontend Execution (`execution.service.ts`)
Archivo:
- `frontend/src/app/features/execution/services/execution.service.ts`

Mejoras:
- Se aplicó el mismo mapeo `mapAiError(...)` en `voiceFill(...)`.
- Cuando falla autocompletado por voz vía IA, el usuario ahora ve mensajes controlados.

## Mensajes finales visibles (usuario)
- **503**: “El servicio de IA no está disponible en este momento. Intenta nuevamente en unos minutos.”
- **504**: “El servicio de IA está saturado o demoró demasiado en responder. Intenta nuevamente.”
- **502**: “El servicio de IA devolvió una respuesta inválida. Intenta nuevamente.”
- **5xx general**: “Ocurrió un error temporal en el servicio de IA. Intenta nuevamente.”

## Beneficios
1. Mejor experiencia de usuario ante fallos de infraestructura IA.
2. Menor exposición de detalles internos técnicos.
3. Trazabilidad mantenida en backend mediante `requestId`.

