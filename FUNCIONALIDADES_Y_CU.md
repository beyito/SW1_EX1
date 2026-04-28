# Especificacion de Casos de Uso (CU) para UML

## 1. Alcance
Este documento define los casos de uso funcionales del sistema BPMN colaborativo para derivar diagramas UML (Use Case, Activity, Sequence).

## 2. Actores
- `Software Admin`: administra empresas y administradores de empresa.
- `Company Admin`: diseña politicas BPMN, administra areas/carriles y usuarios de su empresa.
- `Functionary`: ejecuta tareas operativas de su carril.
- `Client`: inicia tramites y atiende tareas de su carril (principalmente mobile).
- `Copilot IA`: asistente que sugiere o aplica cambios al diagrama.
- `Sistema BPMN`: backend + motor de workflow + persistencia.

## 3. Matriz CU resumida
- `CU-01` Iniciar sesion web.
- `CU-02` Iniciar sesion mobile cliente.
- `CU-03` Gestionar empresas (Software Admin).
- `CU-04` Gestionar areas/carriles (Company Admin).
- `CU-05` Crear politica BPMN.
- `CU-06` Editar diagrama BPMN manualmente.
- `CU-07` Editar diagrama con Copilot IA.
- `CU-08` Guardar politica y sincronizar colaboracion.
- `CU-09` Iniciar proceso desde una politica.
- `CU-10` Tomar/iniciar tarea.
- `CU-11` Completar tarea y enrutar decision.
- `CU-12` Gestionar adjuntos de formulario.
- `CU-13` Consultar bandeja y detalle de tareas.

## 4. Casos de uso detallados

### CU-01 Iniciar sesion web
- Objetivo: autenticar usuario interno.
- Actores: `Software Admin`, `Company Admin`, `Functionary`.
- Disparador: usuario envia credenciales.
- Precondiciones: usuario existe y esta activo.
- Flujo principal:
1. Actor ingresa usuario y contrasena.
2. Frontend envia `POST /api/auth/login`.
3. Backend valida credenciales.
4. Backend devuelve token y perfil.
5. Frontend guarda sesion y redirige.
- Flujos alternos:
1. Credenciales invalidas: backend responde error y no crea sesion.
2. Rol no permitido en web: se rechaza acceso.
- Postcondiciones: sesion autenticada en cliente web.
- Servicios: `AuthService.login`, `AuthController.login`.

### CU-02 Iniciar sesion mobile cliente
- Objetivo: autenticar cliente en app mobile.
- Actores: `Client`.
- Disparador: cliente envia credenciales en mobile.
- Precondiciones: usuario con rol cliente.
- Flujo principal:
1. Mobile envia `POST /api/auth/mobile/login`.
2. Backend valida rol y credenciales.
3. Backend devuelve token/perfil.
4. Mobile habilita dashboard de tramites.
- Flujos alternos:
1. Usuario no cliente: acceso denegado.
- Postcondiciones: sesion cliente activa.
- Servicios: `AuthController.loginMobile`.

### CU-03 Gestionar empresas
- Objetivo: alta/listado de empresas.
- Actores: `Software Admin`.
- Precondiciones: sesion con rol `SOFTWARE_ADMIN`.
- Flujo principal:
1. Actor crea empresa (`POST /api/admin/companies`).
2. Sistema persiste empresa.
3. Actor consulta listado (`GET /api/admin/companies`).
- Flujos alternos:
1. Rol insuficiente: 403.
- Postcondiciones: empresa disponible para administracion.
- Servicios: `AdminController`, `AdminService`.

### CU-04 Gestionar areas/carriles
- Objetivo: administrar areas que mapean a carriles de politicas.
- Actores: `Company Admin`.
- Precondiciones: sesion valida en empresa.
- Flujo principal:
1. Actor crea/edita/elimina area (`/api/admin/areas`).
2. Frontend del diseñador carga areas (`CompanyAreaService.getCompanyAreas`).
3. Areas se usan como base de carriles en politicas.
- Flujos alternos:
1. Nombre duplicado o invalido: error de validacion.
- Postcondiciones: catalogo de areas actualizado.

### CU-05 Crear politica BPMN
- Objetivo: crear contenedor de politica.
- Actores: `Company Admin`.
- Precondiciones: existen areas de empresa.
- Flujo principal:
1. Actor crea politica por GraphQL `createPolicy`.
2. Sistema crea politica con `diagramJson` inicial y metadatos.
3. Frontend abre diseñador de la nueva politica.
- Postcondiciones: politica creada en estado editable.
- Servicios: `PolicyGraphQLController.createPolicy`, `PolicyService.createPolicy`.

### CU-06 Editar diagrama BPMN manualmente
- Objetivo: construir flujo con nodos, enlaces y carriles.
- Actores: `Company Admin`.
- Precondiciones: politica cargada.
- Flujo principal:
1. Actor agrega carriles (areas).
2. Actor agrega nodos (`START`, `TASK`, `DECISION`, `FORK`, `JOIN`, `END`).
3. Actor conecta nodos con enlaces.
4. Actor define metadata de tareas y condiciones de decisiones.
5. Sistema autosave local + persistencia remota.
- Flujos alternos:
1. Enlace duplicado: se rechaza.
2. Nodo fuera de limites: se clampa posicion.
- Postcondiciones: diagrama consistente persistido.
- Servicios: `DiagramCanvasService`, `PolicyDesignerComponent`.

### CU-07 Editar diagrama con Copilot IA
- Objetivo: modificar diagrama por lenguaje natural.
- Actores: `Company Admin`, `Copilot IA`.
- Precondiciones: politica abierta y sesion activa.
- Flujo principal:
1. Actor envia instruccion al chat Copilot.
2. Frontend envia snapshot actual + lanes.
3. Backend gateway reenvia a microservicio IA.
4. IA devuelve `diagram` y opcionalmente `lanes`.
5. Frontend fusiona (si no destructivo).
6. Frontend normaliza visual/geometricamente con `normalizeDiagramForDesigner`.
7. Frontend aplica, guarda y publica `full-sync`.
- Flujos alternos:
1. Timeout IA: gateway devuelve 504.
2. Respuesta invalida IA: gateway devuelve error controlado.
- Postcondiciones: diagrama actualizado sin romper estilo/carriles.
- Servicios:
  - Front: `sendToCopilot`, `resolveCopilotDiagram`.
  - Back: `CopilotService.chat/apply`.
  - IA: `DiagramAgentService.process`.

### CU-08 Guardar politica y sincronizar colaboracion
- Objetivo: persistir cambios y propagar en tiempo real.
- Actores: `Company Admin`, `Sistema BPMN`.
- Precondiciones: politica seleccionada.
- Flujo principal:
1. Frontend serializa grafo (sin fondos de carril).
2. Frontend envia GraphQL `updatePolicyGraph`.
3. Backend recalcula `laneId` por posicion.
4. Backend guarda `diagramJson` y `lanes` (`x`,`width`,`height`).
5. Frontend emite eventos STOMP.
6. Otros clientes aplican evento remoto.
- Flujos alternos:
1. Conflicto visual remoto: se usa `full-sync`.
- Postcondiciones: todos los clientes convergen al mismo estado.

### CU-09 Iniciar proceso desde politica
- Objetivo: crear instancia de proceso.
- Actores: `Functionary`, `Client`.
- Precondiciones:
1. Politica activa.
2. Actor autorizado para `startLaneId`.
- Flujo principal:
1. Actor solicita inicio (`POST /api/execution/process/start`).
2. Backend valida permiso por lane.
3. Crea `ProcessInstance`.
4. Motor calcula primeros nodos y crea tareas pendientes.
- Flujos alternos:
1. Lane no autorizado: se rechaza inicio.
- Postcondiciones: proceso activo con tareas iniciales.

### CU-10 Tomar/iniciar tarea
- Objetivo: mover tarea de `PENDING` a `IN_PROGRESS`.
- Actores: `Functionary`, `Client`.
- Precondiciones: tarea visible y autorizada.
- Flujo principal:
1. Actor ejecuta `take/start`.
2. Backend valida acceso por lane/asignacion.
3. Backend actualiza estado y `assignedTo`.
- Postcondiciones: tarea en progreso.

### CU-11 Completar tarea y enrutar decision
- Objetivo: completar tarea y avanzar flujo.
- Actores: `Functionary`, `Client`, `Sistema BPMN`.
- Precondiciones: tarea en `IN_PROGRESS`.
- Flujo principal:
1. Actor envia formulario (`complete`).
2. Backend valida obligatorios.
3. Backend parsea variables de ruteo (ej. `_decisionTomada`).
4. `WorkflowEngine` evalua enlaces.
5. Se crean siguientes tareas o se completa proceso.
- Flujos alternos:
1. Formulario invalido: error de validacion.
2. Decision sin match: usa rama default si existe.
- Postcondiciones: flujo avanza segun reglas BPMN.

### CU-12 Gestionar adjuntos de formulario
- Objetivo: subir y asociar archivos a formularios.
- Actores: `Company Admin`, `Functionary`, `Client`.
- Flujo principal:
1. Actor selecciona archivo.
2. Frontend sube a `POST /api/files/upload`.
3. Backend guarda en S3 y retorna metadatos.
4. Frontend referencia adjunto en `taskForm`.
- Postcondiciones: adjunto disponible por URL.

### CU-13 Consultar bandeja y detalle de tareas
- Objetivo: ver pendientes e historial por proceso.
- Actores: `Functionary`, `Client`.
- Flujo principal:
1. Actor consulta `my-tasks` / `my-processes/tasks`.
2. Actor abre detalle de tarea (`/tasks/{id}` o GraphQL).
3. Sistema devuelve formulario, estado y contexto de proceso.
- Postcondiciones: actor informado para ejecutar tarea.

## 5. Relaciones UML entre casos de uso
- `CU-07 Editar con Copilot` <<include>> `CU-08 Guardar y sincronizar`.
- `CU-06 Editar manual` <<include>> `CU-08 Guardar y sincronizar`.
- `CU-11 Completar tarea` <<include>> evaluacion de decision SpEL.
- `CU-10 Tomar tarea` <<extend>> `CU-13 Bandeja de tareas`.
- `CU-09 Iniciar proceso` <<include>> validacion de lane de inicio.

## 6. Frontera del sistema (para Use Case Diagram)
- Dentro del sistema:
  - Auth, Admin, Diseñador BPMN, Copilot Gateway, Motor Workflow, Colaboracion STOMP, S3 Upload.
- Fuera del sistema:
  - Usuario humano (actores).
  - Servicio IA (si lo modelas como actor externo en un diagrama de contexto).
  - AWS S3.

## 7. Prompt maestro para generar UML
Usa este prompt con otra IA cuando quieras diagramas UML consistentes:

```text
Actua como analista UML senior. Con base en los siguientes casos de uso del sistema BPMN colaborativo:
[PEGAR SECCION 4 COMPLETA]

Genera:
1) Un diagrama de Casos de Uso UML (actores + relaciones include/extend).
2) Un diagrama de Actividad para CU-07 (Editar diagrama con Copilot IA).
3) Un diagrama de Secuencia para CU-11 (Completar tarea y enrutar decision).

Reglas:
- Usa nomenclatura exacta de actores: Software Admin, Company Admin, Functionary, Client, Copilot IA, Sistema BPMN.
- Respeta precondiciones, flujos alternos y postcondiciones.
- Refleja endpoints/servicios cuando aplique.
- Entrega en PlantUML, separado por bloques.
- No inventes actores ni pasos fuera de los casos definidos.
```

## 8. Prompt corto por CU (plantilla)
```text
Genera UML para el caso de uso [CU-XX].
Incluye: actores, precondiciones, flujo principal (paso a paso), alternos, postcondiciones.
Salida: PlantUML (Use Case + Activity + Sequence).
Contexto del sistema:
[PEGAR CU-XX]
```

## 9. CU adicionales (sin modificar CU-01 a CU-13)

### CU-14 Consultar metricas de politica
- Objetivo: analizar rendimiento real de tareas por politica.
- Actores: `Company Admin`.
- Precondiciones: politica existente con ejecuciones completadas.
- Flujo principal:
1. Actor abre vista de metricas de una politica.
2. Frontend solicita `GET /api/metrics/policy/{policyId}`.
3. Backend agrega tareas completadas por proceso de esa politica.
4. Backend calcula promedio de espera, ejecucion y total por tarea.
5. Frontend muestra ranking y posibles cuellos de botella.
- Flujos alternos:
1. Sin datos historicos: respuesta vacia sin error.
- Postcondiciones: actor obtiene evidencia para optimizar el flujo.
- Servicios: `MetricsController.getPolicyMetrics`, `MetricsService.getPolicyMetrics`.

### CU-15 Gestionar funcionarios y clientes de empresa
- Objetivo: administrar usuarios operativos de la empresa.
- Actores: `Company Admin`.
- Precondiciones: sesion valida en empresa.
- Flujo principal:
1. Actor crea/edita/elimina funcionarios (`/api/admin/functionaries`).
2. Actor crea/edita/elimina clientes (`/api/admin/clients`).
3. Sistema valida pertenencia de empresa y persistencia.
- Flujos alternos:
1. Rol insuficiente o usuario fuera de empresa: acceso denegado.
- Postcondiciones: usuarios operativos actualizados.
- Servicios: `AdminController`, `AdminService`.

### CU-16 Gestionar administradores de empresa
- Objetivo: administrar cuentas `COMPANY_ADMIN`.
- Actores: `Software Admin`.
- Precondiciones: sesion con rol `SOFTWARE_ADMIN`.
- Flujo principal:
1. Actor crea company admin (`POST /api/admin/company-admins`).
2. Actor consulta lista (`GET /api/admin/company-admins`).
3. Actor actualiza o elimina (`PUT/DELETE /api/admin/company-admins/{userId}`).
- Flujos alternos:
1. Usuario duplicado o empresa invalida: error de validacion.
- Postcondiciones: gobierno administrativo por empresa actualizado.
- Servicios: `AdminController`, `AdminService`.

### CU-17 Consultar historial de conversacion Copilot
- Objetivo: recuperar contexto de asistencia IA por politica.
- Actores: `Company Admin`.
- Precondiciones: existe conversacion previa o policyId valido.
- Flujo principal:
1. Actor solicita historial por `policyId` o `conversationId`.
2. Frontend invoca `GET /api/copilot/history`.
3. Backend recupera mensajes persistidos en `copilot_conversations`.
4. Frontend renderiza historial en el chat.
- Flujos alternos:
1. Sin historial: se devuelve conversacion vacia.
- Postcondiciones: continuidad contextual del asistente.
- Servicios: `CopilotController.history`, `CopilotService.getConversationHistory`.

### CU-18 Consultar orden de ejecucion de tareas de una politica
- Objetivo: visualizar secuencia y dependencias del flujo definido.
- Actores: `Company Admin`.
- Precondiciones: politica con diagrama valido.
- Flujo principal:
1. Actor solicita orden desde el diseÃ±ador.
2. Frontend invoca query GraphQL `getTaskExecutionOrder(policyId)`.
3. Backend recorre nodos/enlaces y calcula orden/dependencias.
4. Sistema devuelve tareas con `laneId` y `laneName`.
- Flujos alternos:
1. Diagrama sin nodo START: backend devuelve error de flujo invalido.
- Postcondiciones: actor entiende secuencia esperada de ejecucion.
- Servicios: `PolicyGraphQLController.getTaskExecutionOrder`, `PolicyService.getTaskExecutionOrder`.

### CU-19 Sincronizar cambios de diagrama en tiempo real
- Objetivo: mantener consistencia entre multiples navegadores.
- Actores: `Company Admin`, `Sistema BPMN`.
- Precondiciones: clientes conectados a `/ws-designer` y misma politica abierta.
- Flujo principal:
1. Usuario A realiza cambio local (move/add/remove/cell-sync/full-sync).
2. Frontend A publica evento a `/app/policy/{policyId}/change`.
3. Backend retransmite a `/topic/policy.{policyId}`.
4. Usuario B aplica evento remoto sobre su grafo local.
- Flujos alternos:
1. Divergencia de layout: se fuerza `full-sync`.
- Postcondiciones: vistas remotas convergen al mismo estado.
- Servicios: `WebSocketService`, `DesignerSocketController`.

### CU-20 Normalizar diagrama generado por IA para vista de diseÃ±ador
- Objetivo: unificar estilo/forma entre nodos manuales y nodos creados por IA.
- Actores: `Company Admin`, `Copilot IA`.
- Precondiciones: respuesta `apply` con diagrama IA.
- Flujo principal:
1. Frontend recibe `diagram` y `lanes` de Copilot.
2. Ejecuta `normalizeDiagramForDesigner(...)`.
3. Corrige estilos de nodos/enlaces y posicion respecto a carriles.
4. Aplica al canvas, guarda y sincroniza.
- Flujos alternos:
1. Enlaces huÃ©rfanos o metadata incompleta: saneamiento y advertencias.
- Postcondiciones: diagrama visible consistente con el estandar manual.
- Servicios: `DiagramCanvasService.normalizeDiagramForDesigner`, `diagram_tools.sanitize_diagram`.

## 10. Relaciones UML adicionales sugeridas
- `CU-07 Editar diagrama con Copilot IA` <<include>> `CU-17 Consultar historial de conversacion Copilot`.
- `CU-07 Editar diagrama con Copilot IA` <<include>> `CU-20 Normalizar diagrama generado por IA`.
- `CU-06 Editar diagrama BPMN manualmente` <<include>> `CU-19 Sincronizar cambios de diagrama en tiempo real`.
- `CU-08 Guardar politica y sincronizar colaboracion` <<extend>> `CU-19 Sincronizar cambios de diagrama en tiempo real`.
- `CU-05 Crear politica BPMN` <<extend>> `CU-18 Consultar orden de ejecucion de tareas`.

### CU-21 Registrar token FCM de dispositivo movil
- Objetivo: habilitar notificaciones push para el usuario autenticado en mobile.
- Actores: `Client`.
- Precondiciones: sesion mobile iniciada y Firebase inicializado.
- Flujo principal:
1. App solicita permisos de notificacion.
2. App obtiene token FCM del dispositivo.
3. App envia token al backend autenticado.
4. Backend persiste token en el usuario.
- Flujos alternos:
1. Permiso denegado: app sigue operando sin push.
2. Token invalido o vacio: backend rechaza registro.
- Postcondiciones: usuario habilitado para recibir push.
- Servicios: `PushNotificationService.syncTokenWithBackend`, `NotificationTokenController.registerDeviceToken`.

### CU-22 Recibir notificacion push al asignarse nueva tarea
- Objetivo: notificar al usuario cuando el sistema genera una nueva tarea para su carril/asignacion.
- Actores: `Client`, `Sistema BPMN`.
- Precondiciones: usuario con token FCM registrado.
- Flujo principal:
1. Motor BPMN crea tarea pendiente.
2. Backend identifica usuarios objetivo.
3. Backend envia push por FCM.
4. Dispositivo recibe notificacion (foreground/background/terminated).
- Flujos alternos:
1. Firebase no inicializado: se registra warning y no se interrumpe flujo.
2. Token desactualizado: push falla para ese usuario y el proceso continua.
- Postcondiciones: usuario informado en tiempo real de nueva tarea.
- Servicios: `ProcessExecutionService.createSinglePendingTask`, `FirebaseMessagingService.sendTaskAssignedNotification`.

- `CU-22 Recibir push de nueva tarea` <<include>> `CU-21 Registrar token FCM`.
