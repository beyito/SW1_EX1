# Modelo de Contexto del Sistema BPMN Colaborativo

## 1. Prop贸sito
Definir el contexto del sistema para alinear alcance funcional, actores, fronteras e integraciones externas.

## 2. Alcance del sistema
El sistema permite dise帽ar pol铆ticas BPMN colaborativas, ejecutarlas por carriles/谩reas, asistir el dise帽o con IA y monitorear m茅tricas de ejecuci贸n.

## 3. Funcionalidades principales
1. Autenticaci贸n web y m贸vil con control por roles.
2. Administraci贸n organizacional: empresas, admins, 谩reas, funcionarios y clientes.
3. Dise帽o BPMN en canvas colaborativo (nodos, enlaces, carriles, formularios).
4. Colaboraci贸n en tiempo real entre navegadores (sincronizaci贸n por eventos).
5. Asistente Copilot IA (chat y aplicaci贸n de cambios al diagrama).
6. Persistencia de diagrama y geometr铆a de carriles.
7. Ejecuci贸n de procesos y tareas con enrutamiento por decisiones.
8. Gesti贸n de adjuntos de formularios (subida y referencia de archivos).
9. Consulta de m茅tricas por pol铆tica para detectar cuellos de botella.
10. Consulta de orden de ejecuci贸n de tareas de una pol铆tica.

## 4. Actores
- `Software Admin`: administra empresas y administradores de empresa.
- `Company Admin`: dise帽a pol铆ticas BPMN y administra recursos de su empresa.
- `Functionary`: ejecuta tareas de proceso en su carril.
- `Client`: inicia tr谩mites y atiende tareas (principalmente desde mobile).
- `Copilot IA`: asistente inteligente para an谩lisis y modificaci贸n de diagramas.

## 5. Entidades externas
- `OpenAI API`: proveedor de inferencia para chat y generaci贸n de cambios de diagrama.
- `AWS S3`: almacenamiento de archivos adjuntos.
- `MongoDB`: persistencia documental principal (usuarios, pol铆ticas, instancias, conversaciones).
- `Broker STOMP/WebSocket`: canal de colaboraci贸n en tiempo real.
- `Aplicaci贸n m贸vil Flutter`: cliente externo para operaci贸n de usuarios cliente.

## 6. Frontera del sistema
### Dentro del sistema
- Frontend Angular (dise帽ador, paneles administrativos, ejecuci贸n, m茅tricas).
- Backend Spring Boot (REST, GraphQL, WebSocket, seguridad, workflow, gateway IA).
- Microservicio IA FastAPI (copilot chat y agente de diagrama).

### Fuera del sistema
- Usuarios humanos (actores).
- Servicios de terceros (OpenAI, AWS S3).
- Infraestructura de persistencia/comunicaci贸n administrada externamente (MongoDB, broker).

## 7. Requisitos funcionales (RF)
- `RF-01`: El sistema debe autenticar usuarios por rol (web y mobile).
- `RF-02`: El sistema debe permitir CRUD de empresas y usuarios administrativos.
- `RF-03`: El sistema debe permitir CRUD de 谩reas/carriles y usuarios operativos.
- `RF-04`: El sistema debe crear, editar y guardar pol铆ticas BPMN.
- `RF-05`: El sistema debe soportar nodos BPMN (`START`, `TASK`, `DECISION`, `FORK`, `JOIN`, `SYNCHRONIZATION`, `END`) y enlaces condicionales.
- `RF-06`: El sistema debe sincronizar cambios de diagrama en tiempo real.
- `RF-07`: El sistema debe permitir asistencia IA por chat y aplicar cambios al diagrama.
- `RF-08`: El sistema debe validar formularios de tareas antes de completar.
- `RF-09`: El sistema debe enrutar flujo seg煤n condiciones de decisi贸n y reglas BPMN.
- `RF-10`: El sistema debe permitir adjuntar archivos y almacenarlos en S3.
- `RF-11`: El sistema debe exponer m茅tricas de ejecuci贸n por pol铆tica.
- `RF-12`: El sistema debe exponer orden de ejecuci贸n de tareas de cada pol铆tica.

## 8. Requisitos no funcionales (RNF)
- `RNF-01` Seguridad: autenticaci贸n por token bearer y autorizaci贸n por roles/carriles.
- `RNF-02` Consistencia colaborativa: convergencia entre clientes mediante eventos incrementales y `full-sync`.
- `RNF-03` Disponibilidad: manejo de fallos/timeout del servicio IA con respuesta controlada.
- `RNF-04` Rendimiento: respuesta fluida en edici贸n de canvas (zoom, panning, autosave).
- `RNF-05` Escalabilidad: separaci贸n de frontend, backend y microservicio IA para escalar por componente.
- `RNF-06` Trazabilidad: logs de solicitudes cr铆ticas (copilot, ejecuci贸n, saneamiento).
- `RNF-07` Interoperabilidad: contratos REST/GraphQL/WebSocket definidos y estables.
- `RNF-08` Mantenibilidad: arquitectura modular por servicios/componentes y documentaci贸n t茅cnica.
- `RNF-09` Usabilidad: interfaz de dise帽o con soporte visual consistente y edici贸n asistida.
- `RNF-10` Integridad de datos: validaciones de diagrama/formulario antes de persistir o ejecutar.

## 9. Vista r谩pida de contexto (texto)
- Actores humanos interact煤an con `Frontend Angular` y `Mobile Flutter`.
- Clientes frontend/mobile consumen `Backend Spring Boot`.
- Backend se integra con `MongoDB`, `S3`, `Broker STOMP` y `Microservicio IA`.
- Microservicio IA consume `OpenAI API`.

## 10. Trazabilidad de requisitos (resumen)
### Requisitos funcionales (prioridad y validaci髇)
- `RF-01` Prioridad: Alta. Validaci髇: pruebas de login web/mobile por rol.
- `RF-02` Prioridad: Alta. Validaci髇: pruebas CRUD empresas/admins.
- `RF-03` Prioridad: Alta. Validaci髇: pruebas CRUD 醨eas/funcionarios/clientes.
- `RF-04` Prioridad: Alta. Validaci髇: crear/editar/guardar pol韙ica y recarga.
- `RF-05` Prioridad: Alta. Validaci髇: crear nodos/enlaces y revisar `diagramJson`.
- `RF-06` Prioridad: Alta. Validaci髇: edici髇 simult醤ea entre 2 navegadores.
- `RF-07` Prioridad: Media-Alta. Validaci髇: chat/apply IA con cambios aplicados.
- `RF-08` Prioridad: Alta. Validaci髇: rechazo de formularios incompletos.
- `RF-09` Prioridad: Alta. Validaci髇: ruteo por `DECISION`, `FORK`, `JOIN`, `END`.
- `RF-10` Prioridad: Media. Validaci髇: subida de archivo y URL disponible.
- `RF-11` Prioridad: Media. Validaci髇: consulta de m閠ricas por `policyId`.
- `RF-12` Prioridad: Media. Validaci髇: consulta de orden de ejecuci髇.

### Requisitos no funcionales (m閠rica de control sugerida)
- `RNF-01` Seguridad: 100% endpoints protegidos por auth/roles.
- `RNF-02` Consistencia: 0 divergencias persistentes tras `full-sync`.
- `RNF-03` Disponibilidad: manejo de timeout IA sin ca韉a del backend.
- `RNF-04` Rendimiento: experiencia fluida en zoom/pan y edici髇 base.
- `RNF-05` Escalabilidad: despliegue desacoplado por servicio.
- `RNF-06` Trazabilidad: logs con contexto en operaciones cr韙icas.
- `RNF-07` Interoperabilidad: contratos REST/GraphQL/WebSocket estables.
- `RNF-08` Mantenibilidad: m骴ulos separados y documentaci髇 actualizada.
- `RNF-09` Usabilidad: interfaz operable en dise駉 y ejecuci髇 sin fricci髇.
- `RNF-10` Integridad: validaciones previas a persistencia/avance de flujo.

## 11. Extension de contexto: Notificaciones Push FCM

### Funcionalidad agregada
- Registro de token FCM por usuario autenticado en mobile.
- Envio de push al crearse nuevas tareas pendientes.

### Actor/entidad externa involucrada
- Servicio externo: `Firebase Cloud Messaging (FCM)`.

### RF nuevos
- `RF-13`: El sistema debe registrar el token FCM del dispositivo movil autenticado.
- `RF-14`: El sistema debe enviar notificacion push cuando se asigna una nueva tarea al usuario.

### RNF nuevos
- `RNF-11` Confiabilidad de notificacion: fallo en FCM no debe bloquear el flujo BPMN.
- `RNF-12` Seguridad de token: el registro de token debe requerir autenticacion.
