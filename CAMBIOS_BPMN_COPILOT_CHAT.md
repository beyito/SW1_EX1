# BPMN Copilot Chat (Frontend + Gateway + FastAPI)

## Objetivo

Implementar un asistente conversacional BPMN con contexto visual del diagrama actual (`graph.toJSON()`), en arquitectura de 3 capas:

1. Angular + JointJS
2. Spring Boot API Gateway
3. FastAPI IA en puerto `8010`

## 1) Motor IA (Python FastAPI)

Archivo actualizado:

- `bpmn-ai-engine/app/main.py`

Se implemento:

- Modelo request:
  - `userMessage: str`
  - `currentDiagram: Dict[str, Any]`
- Modelo response:
  - `message: str`
  - `suggested_actions: List[str]`
- Endpoint:
  - `POST /api/ai/copilot-chat`
- Llamada OpenAI:
  - `client.chat.completions.create(...)`
  - `response_format={"type":"json_object"}`
- Prompt de sistema aplicado segun requerimiento.

Tambien se actualizo:

- `bpmn-ai-engine/README.md` con contrato del endpoint de copilot.

## 2) API Gateway (Spring Boot)

Archivos nuevos:

- `backend/src/main/java/com/politicanegocio/core/dto/CopilotRequestDto.java`
- `backend/src/main/java/com/politicanegocio/core/dto/CopilotResponseDto.java`
- `backend/src/main/java/com/politicanegocio/core/service/CopilotService.java`
- `backend/src/main/java/com/politicanegocio/core/controller/CopilotController.java`

Contrato expuesto al frontend:

- `POST /api/copilot/chat`

Funcionamiento:

- Recibe `userMessage` y `currentDiagram`.
- Reenvia al microservicio IA:
  - `POST http://localhost:8010/api/ai/copilot-chat`
- Devuelve a Angular:
  - `message`
  - `suggestedActions`

## 3) Frontend (Angular + JointJS)

Archivos nuevos:

- `frontend/src/app/features/policy-designer/services/copilot.service.ts`
- `frontend/src/app/features/policy-designer/components/copilot-chat/copilot-chat.component.ts`
- `frontend/src/app/features/policy-designer/components/copilot-chat/copilot-chat.component.html`
- `frontend/src/app/features/policy-designer/components/copilot-chat/copilot-chat.component.scss`

Archivos actualizados:

- `frontend/src/app/features/policy-designer/components/policy-designer/policy-designer.component.ts`
- `frontend/src/app/features/policy-designer/components/policy-designer/policy-designer.component.html`
- `frontend/src/app/features/policy-designer/components/policy-designer/policy-designer.component.scss`

Se implemento:

- Panel lateral derecho `CopilotChatComponent` con:
  - historial de mensajes tipo burbuja
  - input + boton enviar
  - boton rapido: `Analizar mi diagrama actual`
- Integracion con editor JointJS:
  - en el componente del editor se intercepta el envio
  - se captura `this.graph.toJSON()`
  - se invoca `CopilotService.sendMessage(userText, currentDiagram)`

## 4) Documentacion general actualizada

- `ARCHITECTURE.md`
- `PROJECT_AI_CONTEXT.md`

## Validaciones ejecutadas

- Python `bpmn-ai-engine`: compile OK
- Backend Spring: `mvnw -q -DskipTests compile` OK
- Frontend Angular: `npm run build` OK (warnings no bloqueantes preexistentes)
