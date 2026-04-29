# CU06 - Flujo Detallado de Ejecucion (Ingenieria Inversa)

## 1. Alcance
Este documento describe el flujo real del caso de uso **CU06: Editar diagrama BPMN manualmente** a partir del codigo del proyecto (`frontend`, `backend`), incluyendo clases, metodos, validaciones, persistencia, sincronizacion y manejo de errores.

## 2. Participantes Tecnologicos
- **Actor**: `Company Admin`
- **Frontend (Angular)**:
  - `PolicyDesignerComponent`
  - `DiagramCanvasService`
  - `PolicyDataService`
  - `WebSocketService`
  - `DiagramStorageService`
- **Backend (Spring Boot)**:
  - `PolicyGraphQLController`
  - `PolicyService`
  - `PolicyRepository`
  - `DesignerSocketController`
  - `SimpMessagingTemplate`
- **Infraestructura**:
  - `RabbitMQ` (relay STOMP)
  - `MongoDB Atlas` (coleccion de politicas)

## 3. Flujo Principal End-to-End

### 3.1 Edicion manual en lienzo
1. El actor edita el diagrama en el editor (nodos, enlaces, carriles, metadata).
2. `PolicyDesignerComponent` coordina las acciones de UI.
3. Para carriles:
   - `addLane()` agrega carril y recalcula geometria con `diagramCanvasService.recalculateLanePositions(...)`.
   - `removeLane(laneId)` elimina carril y recalcula geometria.
4. Para enlaces manuales:
   - `createConnectionFromSelection()` obtiene `source`/`target`.
   - valida duplicado con `diagramCanvasService.hasExistingLink(graph, source.id, target.id)`.
   - crea enlace con `diagramCanvasService.createLink(source, target, conditionLabel)`.
5. Para decisiones:
   - asigna `conditionLabel` por salida (`Si`, `No`) y luego puede ajustar con `updateLinkCondition(...)`.
6. Actualizacion visual:
   - `applyPolicy(...)` y/o renderizado del graph en memoria.

### 3.2 Reglas de consistencia local durante edicion
7. Limpieza para persistencia:
   - `diagramCanvasService.getPersistedGraphJSON(graph)` elimina fondos de carril (`isLaneBackground`) via `sanitizeGraphJSON(...)`.
8. Geometria de carriles:
   - `syncLanesFromCanvas(...)` captura `x/width/height` reales desde shapes de carril.
   - actualiza arreglo `lanes` y dispara guardado local/autoguardado.
9. Posiciones de nodos:
   - `normalizeDiagramForDesigner(...)` estandariza nodos/enlaces cuando aplica.
   - funciones de soporte: `clampNodeX(...)`, `clampNodeY(...)`, `normalizeElementCell(...)`.

### 3.3 Guardado automatico de politica
10. `scheduleAutoSave()` programa persistencia diferida.
11. `persistPolicyGraph()`:
   - serializa: `graphJson = JSON.stringify(diagramCanvasService.getPersistedGraphJSON(this.graph))`
   - llama `policyDataService.updatePolicyDiagram(policyId, graphJson, lanes)`.
12. `PolicyDataService.updatePolicyDiagram(...)` ejecuta mutacion GraphQL `updatePolicyGraph`.
13. Backend GraphQL:
   - `PolicyGraphQLController.updatePolicyGraph(policyId, diagramJson, lanes)`
14. Servicio de dominio:
   - `PolicyService.updatePolicyGraph(...)`
   - valida acceso por empresa (`companyId` del actor vs politica)
   - parsea `diagramJson`
   - recorre `cells` y recalcula `laneId` de nodos segun coordenada X:
     - `getCellXPosition(...)`
     - `determineLaneId(...)`
   - actualiza:
     - `policy.diagramJson`
     - `policy.startLaneId` (via `bpmnStartLaneParser.extractStartLaneId(...)`)
     - `policy.lanes`
     - `policy.updatedAt`
   - persiste con `policyRepository.save(policy)`.

### 3.4 Sincronizacion colaborativa en tiempo real
15. Al cambiar layout/carriles o cuando corresponde full sync:
   - `broadcastLaneLayoutSync()` -> construye snapshot con `getPersistedGraphJSON(...)`
   - `broadcastFullSync(diagram, lanes)` arma `DiagramEvent` con `action='full-sync'`.
16. Publicacion websocket:
   - `webSocketService.sendMessage(policyId, event)`
   - publica a `/app/policy/{policyId}/change`.
17. Backend STOMP:
   - `DesignerSocketController.handlePolicyChange(policyId, event)`
   - retransmite con `messagingTemplate.convertAndSend("/topic/policy." + policyId, event)`.
18. RabbitMQ relay distribuye a suscriptores.
19. Cliente remoto:
   - recibe en `subscribeToPolicy(...)`
   - aplica evento con `applyRemoteFullSync(...)` o flujo incremental, segun tipo.

## 4. Excepciones y Manejo de Error (Real)

### 4.1 Enlace duplicado o invalido
- `createConnectionFromSelection()` valida con `hasExistingLink(...)`.
- Si existe:
  - limpia seleccion de origen/destino
  - informa `infoMessage` y no agrega el enlace.

### 4.2 Nodo fuera de limites / geometria inconsistente
- Se corrige mediante normalizacion y clamps de posicion:
  - `clampNodeX(...)`, `clampNodeY(...)`, `normalizeNodePosition(...)`
- En carriles, se recalculan posiciones con `recalculateLanePositions(...)`.

### 4.3 Error en guardado automatico
- `persistPolicyGraph()` captura excepcion de GraphQL/backend.
- Actualiza `infoMessage` con error de guardado.
- Mecanismo de cola local:
  - `autoSaveInFlight` + `pendingAutoSave` reintenta al finalizar la operacion actual.

### 4.4 Error de autorizacion o politica inexistente (backend)
- `PolicyService.updatePolicyGraph(...)` lanza `RuntimeException` si:
  - politica no existe
  - actor pertenece a otra empresa
  - JSON invalido
- Error asciende al cliente GraphQL.

## 5. Datos Principales que Viajan
- **Snapshot de diagrama para persistencia**:
  - `diagramJson` (string JSON sin celdas de fondo de carril)
- **Carriles**:
  - `id`, `name`, `color`, `x`, `width`, `height`
- **Mutacion GraphQL**:
  - `updatePolicyGraph(policyId, diagramJson, lanes)`
- **Evento colaborativo**:
  - `DiagramEvent` (`full-sync` y otros tipos de colaboracion)
  - payload tipico de full-sync: `diagram`, `lanes`, `clientId`

## 6. Nota de Comportamiento Funcional Importante
En la implementacion actual, CU06 combina:
- guardado local temporal (`DiagramStorageService.save(...)`),
- autoguardado remoto por GraphQL (`persistPolicyGraph()`),
- y sincronizacion colaborativa por STOMP/RabbitMQ (`broadcastFullSync(...)` + `DesignerSocketController`).

