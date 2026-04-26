# Cambios: Historial de Chat para BPMN Copilot

Fecha: 2026-04-26

## Objetivo

Guardar y recuperar el historial del chat de IA para que el Copilot mantenga continuidad por usuario y por politica.

## Implementacion realizada

## 1) Backend (Spring Boot)

- Persistencia en MongoDB de conversaciones:
  - `backend/src/main/java/com/politicanegocio/core/model/CopilotConversation.java`
  - `backend/src/main/java/com/politicanegocio/core/repository/CopilotConversationRepository.java`

- Nuevos DTOs de historial:
  - `CopilotConversationDto`
  - `CopilotHistoryMessageDto`

- DTOs de chat extendidos:
  - `CopilotRequestDto` ahora soporta:
    - `conversationId`
    - `policyId`
    - `policyName`
  - `CopilotResponseDto` ahora devuelve:
    - `conversationId`

- `CopilotService` actualizado:
  - Resuelve la conversacion del usuario (por `conversationId` o ultima por `policyId`).
  - Guarda cada turno:
    - Mensaje del usuario
    - Respuesta del asistente
  - Reenvia al motor IA una ventana de historial reciente (`conversation_history`).
  - Expone metodo de lectura de historial para frontend.

- `CopilotController` actualizado:
  - `POST /api/copilot/chat` ahora usa el usuario autenticado para el owner del historial.
  - Nuevo endpoint:
    - `GET /api/copilot/history?policyId=...`
    - opcional: `conversationId=...`

## 2) Motor IA (FastAPI)

- `bpmn-ai-engine/app/main.py`:
  - `CopilotRequest` acepta `history`.
  - Se incluye en el payload enviado al modelo como `conversation_history`.

## 3) Frontend (Angular)

- `frontend/src/app/features/policy-designer/services/copilot.service.ts`:
  - `sendMessage(...)` ahora envia `conversationId/policyId/policyName`.
  - Nuevo metodo `getHistoryByPolicy(policyId)`.
  - `CopilotResponse` ahora lee `conversationId`.

- `frontend/src/app/features/policy-designer/components/policy-designer/policy-designer.component.ts`:
  - Nuevo estado `copilotConversationId`.
  - Al cargar politica, consulta historial y lo pinta en el panel chat.
  - Al enviar mensaje, reutiliza la conversacion existente.

## Validacion

- Backend compilado: `mvnw -q -DskipTests compile` OK.
- Frontend compilado: `npm run build` OK.
- AI Engine compilado (python bytecode): `python -m compileall bpmn-ai-engine/app` OK.

## Resultado

El Copilot ahora conserva conversaciones, recupera historial al volver al diseñador y usa contexto previo para responder con continuidad.
