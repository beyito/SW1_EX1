# Control de errores Copilot (500 Debug)

## Objetivo

Mejorar trazabilidad de fallos en flujo:

`Angular -> Spring /api/copilot/chat -> FastAPI /api/ai/copilot-chat -> proveedor IA`

## Cambios aplicados

### Backend (Spring)

- `backend/src/main/java/com/politicanegocio/core/service/CopilotService.java`
  - Validacion de request (`userMessage` obligatorio).
  - Manejo explicito de:
    - `ResourceAccessException` (servicio IA no accesible).
    - `RestClientResponseException` (error devuelto por FastAPI).
    - errores inesperados.
  - Se devuelve `ResponseStatusException` con detalle de estado/cuerpo para diagnostico.
  - Logging estructurado de errores.

- `backend/src/main/java/com/politicanegocio/core/controller/CopilotController.java`
  - Logging de entrada: longitud de mensaje y presencia de diagrama.

### Microservicio IA (FastAPI)

- `bpmn-ai-engine/app/main.py`
  - Logging de entrada (`message_len`, `cell_count`, `model`).
  - `try/except` por etapa:
    - llamada al proveedor IA
    - lectura de contenido de respuesta
    - parseo JSON de respuesta IA
  - `HTTPException` con `detail` especifico por etapa (`ai_provider_call_failed`, `ai_json_parse_failed`, etc).

### Frontend

- `frontend/src/app/features/policy-designer/services/copilot.service.ts`
  - Mensaje de error ahora incluye estado HTTP:
    - `Copilot HTTP <status>: <detail>`

## Verificacion

- `backend`: compila OK (`mvnw -q -DskipTests compile`)
- `frontend`: build OK
- `bpmn-ai-engine`: compile Python OK
