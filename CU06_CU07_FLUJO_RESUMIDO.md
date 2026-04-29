# CU06 y CU07 - Flujo Resumido para Diagramas de Secuencia

## Objetivo
Reducir el nivel de detalle para que los diagramas de secuencia de **CU06** y **CU07** sean legibles, compactos y aptos para Visual Paradigm sin perder la trazabilidad tecnica principal.

---

## CU06 - Editar diagrama BPMN manualmente (Resumen)

### Participantes minimos recomendados
- `Company Admin` (actor)
- `PolicyDesignerComponent` (UI)
- `DiagramCanvasService` (logica de canvas)
- `PolicyDataService` (cliente GraphQL)
- `PolicyGraphQLController`
- `PolicyService`
- `PolicyRepository`
- `WebSocketService`
- `DesignerSocketController`
- `RabbitMQ`

### Flujo resumido
1. El actor modifica nodos, enlaces y carriles en `PolicyDesignerComponent`.
2. `DiagramCanvasService` valida y actualiza estado local (`createLink`, `hasExistingLink`, `recalculateLanePositions`).
3. El frontend genera snapshot persistible (`getPersistedGraphJSON`).
4. `PolicyDataService.updatePolicyDiagram(...)` solicita persistencia.
5. `PolicyGraphQLController.updatePolicyGraph(...)` delega a `PolicyService.updatePolicyGraph(...)`.
6. `PolicyService` recalcula `laneId`, actualiza diagrama/carriles y persiste con `PolicyRepository.save(...)`.
7. El frontend emite sincronizacion (`WebSocketService.sendMessage(...)`).
8. `DesignerSocketController.handlePolicyChange(...)` retransmite y `RabbitMQ` distribuye a clientes remotos.

### Excepciones clave
- Enlace duplicado/invalido: se rechaza localmente y no se persiste.
- Error de guardado: falla GraphQL/backend y se informa en UI.
- Error de autorizacion/politica inexistente: `PolicyService` lanza excepcion.

---

## CU07 - Editar diagrama con Copilot IA (Resumen)

### Participantes minimos recomendados
- `Company Admin` (actor)
- `PolicyDesignerComponent` (UI)
- `CopilotService` (frontend)
- `CopilotController`
- `CopilotService` (backend)
- `DiagramAgentService` (IA Python)
- `DiagramCanvasService`
- `PolicyDataService`
- `PolicyService`
- `PolicyRepository`
- `WebSocketService`
- `DesignerSocketController`
- `RabbitMQ`

### Flujo resumido
1. El actor envia instruccion en `sendToCopilot(...)`.
2. `CopilotService.sendMessage(...)` obtiene respuesta conversacional.
3. Si hay intencion de cambio, frontend llama `applyChange(...)`.
4. Backend procesa `CopilotService.apply(...)` y delega a IA (`DiagramAgentService.process(...)`).
5. IA devuelve `diagram` + `warnings` (+ `lanes` opcional).
6. Frontend resuelve y normaliza (`resolveCopilotDiagram`, `normalizeDiagramForDesigner`).
7. Frontend aplica el diagrama, persiste (`updatePolicyDiagram`) y sincroniza (`sendMessage`).
8. Backend retransmite por `DesignerSocketController` y `RabbitMQ` a clientes remotos.

### Excepciones clave
- Timeout/fallo IA: backend devuelve error de gateway y UI notifica.
- Respuesta IA invalida/incompleta: no se aplica diagrama.
- Error de persistencia/autorizacion: falla en `PolicyService.updatePolicyGraph(...)`.

---

## Sugerencia para diagrama compacto (Visual Paradigm)
- Usar solo 8-10 participantes por CU.
- Agrupar validaciones internas en un solo mensaje ("validar y normalizar").
- Mostrar solo un `alt` de errores principal por CU.
- Omitir metodos auxiliares internos (helpers privados) en el diagrama.

