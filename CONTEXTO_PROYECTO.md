# Contexto General del Proyecto

## 1. ¿Qué es este sistema?
Este proyecto implementa una plataforma **BPMN colaborativa multi-tenant** que permite:

1. Diseñar políticas de proceso (diagramas BPMN) de forma visual.
2. Ejecutar operativamente esas políticas mediante instancias de proceso y tareas.
3. Colaborar en tiempo real entre varios usuarios sobre el mismo diagrama.
4. Integrar inteligencia artificial para asistencia en edición y autocompletado guiado.

En términos prácticos, el sistema cubre desde la **definición del flujo** (modelado) hasta la **ejecución real** (tareas de funcionarios/clientes), incluyendo trazabilidad y métricas.

## 2. Arquitectura del monorepo
Estructura principal:

- `backend/`: Spring Boot (REST + GraphQL + WebSocket + seguridad JWT + lógica BPMN).
- `frontend/`: Angular + JointJS (diseñador BPMN, formularios dinámicos, colaboración).
- `bpmn-ai-engine/`: FastAPI (chat Copilot, apply de cambios y utilidades IA).
- `mobile/`: Flutter (operación de tareas, notificaciones push).
- `infra/`: Docker Compose, Nginx, variables de entorno y despliegue.

## 3. Flujo funcional de alto nivel

1. **Administración y acceso**
   - Gestión de empresas, áreas (carriles), funcionarios y clientes.
   - Autenticación por token y autorización por roles.

2. **Diseño de políticas BPMN**
   - Creación de políticas y edición del diagrama en canvas.
   - Definición de nodos, enlaces, decisiones y formularios de tareas.
   - Persistencia del grafo y carriles.

3. **Colaboración en tiempo real**
   - Los cambios se emiten por WebSocket STOMP.
   - El backend retransmite por tópico para convergencia entre clientes.

4. **Ejecución operativa**
   - Inicio de procesos desde políticas.
   - Gestión de tareas (`PENDING`, `IN_PROGRESS`, `COMPLETED`, `REJECTED`).
   - Enrutamiento dinámico por reglas BPMN (`DECISION`, `FORK`, `JOIN`, `END`).

5. **Asistencia IA**
   - Copilot para consulta y modificación de diagramas.
   - Flujo de autocompletado por voz/texto para prellenar formularios sin envío automático.

## 4. Modelo de datos (resumen práctico)
Colecciones principales:

- `users`: usuarios, roles, tenant, lane, token FCM.
- `companies`: empresas.
- `areas`: áreas/carriles por empresa.
- `policies`: diagrama BPMN (`diagramJson`), carriles (`lanes`), metadatos.
- `process_instances`: instancias activas/completadas.
- `task_instances`: tareas operativas y `formData`.
- `copilot_conversations`: historial de interacción IA.

## 5. Componentes clave de código

### Backend (Spring Boot)
- `PolicyService`: persistencia y normalización de políticas.
- `ProcessExecutionService`: orquestación de tareas/proceso y validación de formularios.
- `WorkflowEngine`: cálculo de siguiente ruta BPMN.
- `CopilotService`: gateway al motor IA.
- `DesignerSocketController`: distribución de eventos colaborativos.

### Frontend (Angular)
- `PolicyDesignerComponent`: modelado BPMN y formulario del nodo TASK.
- `TaskExecutionComponent`: ejecución de tareas y completado de formularios.
- `ExecutionService`: APIs de operación.
- `CopilotService`: APIs de asistencia IA.
- `VoiceRecognitionService`: captura nativa de voz (Web Speech API).

### IA (FastAPI)
- `main.py`: endpoints de chat, apply y voice-fill.
- `DiagramAgentService`: transformación y saneamiento de diagramas.

## 6. Principios de comportamiento actuales

1. En diseño BPMN, los cambios pueden ser manuales o asistidos por IA.
2. En ejecución de tareas, el sistema **no debe completar tarea** si el formulario requerido está incompleto.
3. En autocompletado IA de formularios, el usuario revisa y confirma manualmente antes de enviar la tarea.
4. La colaboración en tiempo real prioriza convergencia visual (`full-sync` cuando corresponde).

## 7. Paquetes funcionales vigentes

1. **Paquete 1: Gestión de Acceso y Administración** (`CU01`, `CU02`, `CU03`, `CU04`, `CU15`, `CU16`)
2. **Paquete 2: Diseño Colaborativo de Políticas BPMN** (`CU05`, `CU06`, `CU08`, `CU18`, `CU19`)
3. **Paquete 3: Ejecución Operativa de Procesos** (`CU09`, `CU10`, `CU11`, `CU13`, `CU23`)
4. **Paquete 4: Servicios de Soporte** (`CU12`, `CU14`, `CU21`, `CU22`)
5. **Paquete 5: Integración Inteligente Copilot IA** (`CU07`, `CU17`, `CU20`)

## 8. Nuevo caso de uso incorporado en esta iteración
- **CU23: Rellenar formulario de tarea con IA (voz/texto)**
  - Se ubica en **Paquete 3** por su impacto directo en ejecución de tareas.
  - Permite prellenado asistido sin envío automático, preservando validación manual del usuario.

