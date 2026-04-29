# Captura de Requisitos

## 1. Priorizaci�n de Casos de Uso

| ID | Nombre del Caso de Uso | Estado | Prioridad | Riesgo | Actores involucrados | Ciclo |
|---|---|---|---|---|---|---|
| CU01 | Iniciar sesi�n web | Aprobado | Cr�tico | Normal | Software Admin, Company Admin, Functionary | C1 |
| CU02 | Iniciar sesi�n mobile cliente | Aprobado | Cr�tico | Normal | Client | C1 |
| CU03 | Gestionar empresas | Aprobado | Significativo | Normal | Software Admin | C1 |
| CU04 | Gestionar �reas/carriles | Aprobado | Cr�tico | Cr�tico | Company Admin | C1 |
| CU05 | Crear pol�tica BPMN | Aprobado | Cr�tico | Normal | Company Admin | C1 |
| CU06 | Editar diagrama BPMN manualmente | Aprobado | Cr�tico | Cr�tico | Company Admin | C2 |
| CU07 | Editar diagrama con Copilot IA | Incorporado | Significativo | Cr�tico | Company Admin, Copilot IA | C4 |
| CU08 | Guardar pol�tica y sincronizar colaboraci�n | Aprobado | Cr�tico | Cr�tico | Company Admin, Sistema BPMN | C2 |
| CU09 | Iniciar proceso desde pol�tica | Aprobado | Cr�tico | Normal | Functionary, Client | C3 |
| CU10 | Tomar/iniciar tarea | Aprobado | Cr�tico | Normal | Functionary, Client | C3 |
| CU11 | Completar tarea y enrutar decisi�n | Aprobado | Cr�tico | Cr�tico | Functionary, Client, Sistema BPMN | C3 |
| CU12 | Gestionar adjuntos de formulario | Aprobado | Significativo | Normal | Company Admin, Functionary, Client | C3 |
| CU13 | Consultar bandeja y detalle de tareas | Aprobado | Significativo | Normal | Functionary, Client | C3 |
| CU14 | Consultar m�tricas de pol�tica | Incorporado | Significativo | Accesorio | Company Admin | C4 |
| CU15 | Gestionar funcionarios y clientes de empresa | Aprobado | Cr�tico | Normal | Company Admin | C2 |
| CU16 | Gestionar administradores de empresa | Aprobado | Significativo | Normal | Software Admin | C2 |
| CU17 | Consultar historial de conversaci�n Copilot | Incorporado | Normal | Accesorio | Company Admin, Copilot IA | C4 |
| CU18 | Consultar orden de ejecuci�n de tareas de una pol�tica | Aprobado | Significativo | Normal | Company Admin | C2 |
| CU19 | Sincronizar cambios de diagrama en tiempo real | Incorporado | Cr�tico | Cr�tico | Company Admin, Sistema BPMN | C4 |
| CU20 | Normalizar diagrama generado por IA para vista de dise�ador | Incorporado | Significativo | Cr�tico | Company Admin, Copilot IA, Sistema BPMN | C4 |

## 2. Especificaci�n detallada por Caso de Uso

### CU01
**ID**: CU01  
**Nombre de caso de uso**: Iniciar sesi�n web  
**Prop�sito**: Permitir autenticaci�n de usuarios internos en el sistema web.  
**Actores**: Software Admin, Company Admin, Functionary  
**Actor iniciador**: Software Admin / Company Admin / Functionary  
**Pre condici�n**: El usuario debe tener cuenta activa y credenciales v�lidas.  
**Flujo Principal**:
1. El usuario accede a la pantalla de login web.
2. Ingresa usuario y contrase�a.
3. El sistema valida credenciales y rol permitido en web.
4. El sistema emite token y perfil del usuario.
5. El sistema redirige al m�dulo seg�n rol.
**Post condici�n**: Sesi�n web iniciada con contexto de rol y empresa.  
**Excepci�n**:
- Credenciales incorrectas.
- Rol `CLIENT` intentando ingresar por web.

### CU02
**ID**: CU02  
**Nombre de caso de uso**: Iniciar sesi�n mobile cliente  
**Prop�sito**: Permitir autenticaci�n de clientes en app m�vil.  
**Actores**: Client  
**Actor iniciador**: Client  
**Pre condici�n**: Usuario con rol cliente registrado y activo.  
**Flujo Principal**:
1. El cliente abre la app m�vil.
2. Ingresa credenciales.
3. La app solicita autenticaci�n m�vil.
4. El sistema valida rol `CLIENT` y credenciales.
5. El sistema devuelve token y datos de sesi�n.
6. La app habilita dashboard y tareas del cliente.
**Post condici�n**: Sesi�n m�vil cliente activa.  
**Excepci�n**:
- Usuario no pertenece al rol cliente.
- Credenciales inv�lidas.

### CU03
**ID**: CU03  
**Nombre de caso de uso**: Gestionar empresas  
**Prop�sito**: Administrar alta y consulta de empresas del sistema.  
**Actores**: Software Admin  
**Actor iniciador**: Software Admin  
**Pre condici�n**: El administrador de software debe haber iniciado sesi�n.  
**Flujo Principal**:
1. El Software Admin accede al m�dulo de empresas.
2. Registra una nueva empresa.
3. El sistema valida que no exista duplicidad.
4. El sistema persiste la empresa.
5. El actor consulta el listado actualizado de empresas.
**Post condici�n**: Empresa creada y disponible para administraci�n.  
**Excepci�n**:
- Nombre de empresa duplicado.
- Usuario sin permisos de Software Admin.

### CU04
**ID**: CU04  
**Nombre de caso de uso**: Gestionar �reas/carriles  
**Prop�sito**: Mantener cat�logo de �reas que se usan como carriles en pol�ticas BPMN.  
**Actores**: Company Admin  
**Actor iniciador**: Company Admin  
**Pre condici�n**: Company Admin autenticado en su empresa.  
**Flujo Principal**:
1. El Company Admin abre gesti�n de �reas.
2. Crea, edita o elimina �reas.
3. El sistema valida reglas de negocio.
4. El sistema guarda cambios.
5. El dise�ador BPMN utiliza el cat�logo actualizado para carriles.
**Post condici�n**: �reas/carriles disponibles y sincronizadas para dise�o.  
**Excepci�n**:
- Nombre de �rea inv�lido o duplicado.
- Intento de eliminar �rea con dependencias activas.

### CU05
**ID**: CU05  
**Nombre de caso de uso**: Crear pol�tica BPMN  
**Prop�sito**: Crear una pol�tica base para su posterior dise�o de flujo.  
**Actores**: Company Admin  
**Actor iniciador**: Company Admin  
**Pre condici�n**: Company Admin autenticado; empresa y �reas existentes.  
**Flujo Principal**:
1. El actor accede al m�dulo de pol�ticas.
2. Registra nombre y descripci�n de la pol�tica.
3. El sistema crea pol�tica con `diagramJson` inicial.
4. El sistema abre la pol�tica en el dise�ador.
**Post condici�n**: Pol�tica creada en estado editable.  
**Excepci�n**:
- Nombre de pol�tica vac�o o inv�lido.
- Error de persistencia.

### CU06
**ID**: CU06  
**Nombre de caso de uso**: Editar diagrama BPMN manualmente  
**Prop�sito**: Dise�ar el flujo de proceso con nodos, enlaces, carriles y formularios.  
**Actores**: Company Admin  
**Actor iniciador**: Company Admin  
**Pre condici�n**: Pol�tica existente abierta en dise�ador.  
**Flujo Principal**:
1. El actor agrega o elimina carriles.
2. Arrastra y coloca nodos BPMN en el canvas.
3. Conecta nodos con enlaces.
4. Configura metadata de tareas y decisiones.
5. El sistema actualiza vista y estado local.
6. El actor contin�a refinando el flujo.
**Post condici�n**: Diagrama actualizado en sesi�n de dise�o.  
**Excepci�n**:
- Nodo fuera de l�mites v�lidos.
- Intento de enlace duplicado o inv�lido.

### CU07
**ID**: CU07  
**Nombre de caso de uso**: Editar diagrama con Copilot IA  
**Prop�sito**: Permitir modificaciones del diagrama mediante lenguaje natural.  
**Actores**: Company Admin, Copilot IA  
**Actor iniciador**: Company Admin  
**Pre condici�n**: Pol�tica abierta y servicio Copilot disponible.  
**Flujo Principal**:
1. El actor escribe instrucci�n en el chat Copilot.
2. El sistema env�a instrucci�n + diagrama actual + carriles.
3. Copilot IA analiza y propone cambios.
4. El sistema recibe diagrama modificado y advertencias.
5. El frontend normaliza formato/posiciones.
**Post condici�n**: Diagrama actualizado con cambios asistidos por IA.  
**Excepci�n**:
- Timeout o fallo del servicio IA.
- Respuesta IA inv�lida o incompleta.

### CU08
**ID**: CU08  
**Nombre de caso de uso**: Guardar pol�tica y sincronizar colaboraci�n  
**Prop�sito**: Persistir el diagrama y propagar cambios a otros usuarios conectados.  
**Actores**: Company Admin, Sistema BPMN  
**Actor iniciador**: Company Admin  
**Pre condici�n**: Existe una pol�tica activa en edici�n.  
**Flujo Principal**:
1. El actor guarda cambios del diagrama.
2. El sistema serializa el grafo persistible.
3. El backend recalcula `laneId` por geometr�a.
4. El sistema guarda `diagramJson` y `lanes`.
5. El sistema emite eventos de sincronizaci�n en tiempo real.
6. Otros clientes aplican actualizaci�n.
**Post condici�n**: Estado persistido y compartido entre sesiones activas.  
**Excepci�n**:
- Error de persistencia GraphQL.
- Conflicto remoto; se requiere `full-sync`.

### CU09
**ID**: CU09  
**Nombre de caso de uso**: Iniciar proceso desde pol�tica  
**Prop�sito**: Crear una instancia de proceso ejecutable desde una pol�tica BPMN.  
**Actores**: Functionary, Client  
**Actor iniciador**: Functionary / Client  
**Pre condici�n**: Actor autorizado para el `startLaneId` de la pol�tica.  
**Flujo Principal**:
1. El actor selecciona pol�tica disponible para iniciar.
2. Ingresa datos de inicio (t�tulo/descripcion).
3. El sistema valida permisos por carril.
4. El sistema crea `ProcessInstance`.
5. El motor crea tareas iniciales correspondientes.
**Post condici�n**: Proceso activo con tareas pendientes.  
**Excepci�n**:
- Actor no autorizado por lane de inicio.
- Pol�tica inexistente o inv�lida.

### CU10
**ID**: CU10  
**Nombre de caso de uso**: Tomar/iniciar tarea  
**Prop�sito**: Permitir que un actor asuma una tarea pendiente y la pase a ejecuci�n.  
**Actores**: Functionary, Client  
**Actor iniciador**: Functionary / Client  
**Pre condici�n**: Tarea en estado `PENDING` y visible para el actor.  
**Flujo Principal**:
1. El actor abre su bandeja de tareas.
2. Selecciona una tarea pendiente.
3. Solicita tomar/iniciar tarea.
4. El sistema valida permisos y estado.
5. El sistema cambia estado a `IN_PROGRESS` y asigna usuario.
**Post condici�n**: Tarea en ejecuci�n por el actor.  
**Excepci�n**:
- Tarea no autorizada.
- Tarea ya tomada o en estado no v�lido.

### CU11
**ID**: CU11  
**Nombre de caso de uso**: Completar tarea y enrutar decisi�n  
**Prop�sito**: Registrar respuesta de tarea y avanzar flujo seg�n reglas BPMN.  
**Actores**: Functionary, Client, Sistema BPMN  
**Actor iniciador**: Functionary / Client  
**Pre condici�n**: Tarea en estado `IN_PROGRESS`.  
**Flujo Principal**:
1. El actor abre el detalle de tarea.
2. Completa y env�a formulario.
3. El sistema valida acceso y estado de la tarea.
4. El sistema valida campos obligatorios.
5. El sistema guarda `formData` y marca tarea `COMPLETED`.
6. El sistema eval�a siguiente ruta (incluye decisiones).
7. El sistema crea nuevas tareas o finaliza proceso en `END`.
**Post condici�n**: Flujo del proceso avanza al siguiente estado v�lido.  
**Excepci�n**:
- Formulario inv�lido.
- Decisi�n sin match y sin ruta por defecto.
- Intento de completar tarea no autorizada.

### CU12
**ID**: CU12  
**Nombre de caso de uso**: Gestionar adjuntos de formulario  
**Prop�sito**: Permitir subir y asociar archivos a formularios de tareas.  
**Actores**: Company Admin, Functionary, Client  
**Actor iniciador**: Company Admin / Functionary / Client  
**Pre condici�n**: Sesi�n activa y archivo v�lido disponible.  
**Flujo Principal**:
1. El actor selecciona archivo desde formulario.
2. El cliente env�a archivo al servicio de carga.
3. El sistema almacena archivo en S3.
4. El sistema devuelve URL/metadatos.
5. El formulario guarda referencia del adjunto.
**Post condici�n**: Adjunto disponible y vinculado al formulario.  
**Excepci�n**:
- Archivo no permitido por tipo/tama�o.
- Falla de carga o acceso a almacenamiento.

### CU13
**ID**: CU13  
**Nombre de caso de uso**: Consultar bandeja y detalle de tareas  
**Prop�sito**: Proveer vista operativa de tareas pendientes y su detalle.  
**Actores**: Functionary, Client  
**Actor iniciador**: Functionary / Client  
**Pre condici�n**: Usuario autenticado con tareas asociadas.  
**Flujo Principal**:
1. El actor abre bandeja de tareas.
2. El sistema lista tareas por lane/proceso/asignaci�n.
3. El actor selecciona una tarea.
4. El sistema muestra detalle, formulario y estado.
**Post condici�n**: Actor informado y listo para ejecutar tarea.  
**Excepci�n**:
- Sin tareas disponibles.
- Tarea seleccionada sin acceso permitido.

### CU14
**ID**: CU14  
**Nombre de caso de uso**: Consultar m�tricas de pol�tica  
**Prop�sito**: Analizar rendimiento de tareas completadas por pol�tica.  
**Actores**: Company Admin  
**Actor iniciador**: Company Admin  
**Pre condici�n**: Pol�tica existente en la empresa del actor.  
**Flujo Principal**:
1. El actor accede a m�tricas de una pol�tica.
2. El sistema recopila procesos y tareas completadas.
3. Calcula tiempos promedio de espera, ejecuci�n y total.
4. Devuelve ranking de tareas por carga temporal.
5. El frontend visualiza m�tricas y cuellos de botella.
**Post condici�n**: M�tricas disponibles para mejora del proceso.  
**Excepci�n**:
- Pol�tica sin datos hist�ricos.
- Pol�tica no pertenece a la empresa del actor.

### CU15
**ID**: CU15  
**Nombre de caso de uso**: Gestionar funcionarios y clientes de empresa  
**Prop�sito**: Administrar usuarios operativos (functionaries y clients) de una empresa.  
**Actores**: Company Admin  
**Actor iniciador**: Company Admin  
**Pre condici�n**: Company Admin autenticado.  
**Flujo Principal**:
1. El actor abre administraci�n de usuarios operativos.
2. Crea, edita o elimina funcionarios.
3. Crea, edita o elimina clientes.
4. El sistema valida consistencia y pertenencia empresarial.
5. El sistema actualiza listados.
**Post condici�n**: Usuarios operativos actualizados en la empresa.  
**Excepci�n**:
- Username duplicado.
- Datos obligatorios incompletos.

### CU16
**ID**: CU16  
**Nombre de caso de uso**: Gestionar administradores de empresa  
**Prop�sito**: Crear y mantener cuentas con rol `COMPANY_ADMIN`.  
**Actores**: Software Admin  
**Actor iniciador**: Software Admin  
**Pre condici�n**: Sesi�n iniciada como Software Admin.  
**Flujo Principal**:
1. El actor abre gesti�n de administradores de empresa.
2. Crea nuevo administrador asociado a empresa.
3. Consulta listado de administradores.
4. Edita o elimina cuentas seg�n necesidad.
5. El sistema confirma cambios.
**Post condici�n**: Administraci�n empresarial delegada correctamente.  
**Excepci�n**:
- Empresa inexistente.
- Usuario duplicado o inv�lido.

### CU17
**ID**: CU17  
**Nombre de caso de uso**: Consultar historial de conversaci�n Copilot  
**Prop�sito**: Recuperar contexto hist�rico de interacci�n con Copilot IA.  
**Actores**: Company Admin, Copilot IA  
**Actor iniciador**: Company Admin  
**Pre condici�n**: Pol�tica seleccionada o conversaci�n existente.  
**Flujo Principal**:
1. El actor abre el chat Copilot.
2. Solicita historial por pol�tica o conversaci�n.
3. El sistema consulta historial persistido.
4. El sistema devuelve mensajes ordenados por tiempo.
5. El actor contin�a conversaci�n con contexto previo.
**Post condici�n**: Historial cargado y disponible en interfaz.  
**Excepci�n**:
- Conversaci�n inexistente.
- Falta de permisos sobre la pol�tica.

### CU18
**ID**: CU18  
**Nombre de caso de uso**: Consultar orden de ejecuci�n de tareas de una pol�tica  
**Prop�sito**: Mostrar secuencia y dependencias del flujo definido.  
**Actores**: Company Admin  
**Actor iniciador**: Company Admin  
**Pre condici�n**: Pol�tica con diagrama v�lido accesible por el actor.  
**Flujo Principal**:
1. El actor solicita orden de ejecuci�n.
2. El sistema lee nodos y enlaces del diagrama.
3. Calcula orden y dependencias de tareas.
4. Asocia tarea con carril (`laneId`, `laneName`).
5. Devuelve resultado para visualizaci�n.
**Post condici�n**: Orden de ejecuci�n disponible para an�lisis y validaci�n.  
**Excepci�n**:
- Diagrama sin nodo `START`.
- Estructura de diagrama inv�lida.

### CU19
**ID**: CU19  
**Nombre de caso de uso**: Sincronizar cambios de diagrama en tiempo real  
**Prop�sito**: Mantener consistencia del diagrama entre usuarios concurrentes.  
**Actores**: Company Admin, Sistema BPMN  
**Actor iniciador**: Company Admin  
**Pre condici�n**: M�ltiples usuarios conectados a la misma pol�tica.  
**Flujo Principal**:
1. Un usuario realiza cambio local en el diagrama.
2. El cliente publica evento de cambio por WebSocket.
3. El sistema retransmite evento a suscriptores de la pol�tica.
4. Clientes remotos aplican cambio incremental o full-sync.
5. El sistema mantiene convergencia de estado.
**Post condici�n**: Vista compartida consistente entre navegadores activos.  
**Excepci�n**:
- Corte de conexi�n WebSocket.
- Conflictos de layout que exigen `full-sync`.

### CU20
**ID**: CU20  
**Nombre de caso de uso**: Normalizar diagrama generado por IA para vista de dise�ador  
**Prop�sito**: Alinear estilo, estructura y geometr�a de diagramas IA con est�ndar del dise�ador.  
**Actores**: Company Admin, Copilot IA, Sistema BPMN  
**Actor iniciador**: Company Admin  
**Pre condici�n**: Existe respuesta de IA con diagrama modificable.  
**Flujo Principal**:
1. El sistema recibe diagrama propuesto por IA.
2. Ejecuta normalizaci�n de nodos, enlaces y metadata.
3. Ajusta posiciones respecto a carriles.
4. Elimina inconsistencias (ej. enlaces hu�rfanos).
5. Aplica resultado al canvas para revisi�n/guardado.
**Post condici�n**: Diagrama normalizado, consistente y compatible con edici�n manual.  
**Excepci�n**:
- Estructura IA irreparable; se aplica fallback manteniendo diagrama base.
- Carriles faltantes o incompatibles.

## 3. CU agregados por Notificaciones Push FCM

| ID | Nombre del Caso de Uso | Estado | Prioridad | Riesgo | Actores involucrados | Ciclo |
|---|---|---|---|---|---|---|
| CU21 | Registrar token FCM de dispositivo movil | Incorporado | Cr�tico | Normal | Client, Sistema BPMN | C4 |
| CU22 | Recibir notificacion push por nueva tarea | Incorporado | Cr�tico | Significativo | Client, Sistema BPMN | C4 |

### CU21
**ID**: CU21  
**Nombre de caso de uso**: Registrar token FCM de dispositivo movil  
**Prop�sito**: Asociar el token FCM del dispositivo al usuario autenticado para habilitar notificaciones push.  
**Actores**: Client, Sistema BPMN  
**Actor iniciador**: Client  
**Pre condici�n**: Sesion mobile activa; Firebase inicializado en la app.  
**Flujo Principal**:
1. La app solicita permisos de notificacion.
2. La app obtiene el token FCM del dispositivo.
3. La app envia el token al backend autenticado.
4. El backend valida y persiste token en el usuario.
5. El backend confirma registro exitoso.
**Post condici�n**: Usuario habilitado para recibir notificaciones push.  
**Excepci�n**:
- Permisos denegados en el dispositivo.
- Token vacio o invalido.
- Error de conectividad al registrar token.

### CU22
**ID**: CU22  
**Nombre de caso de uso**: Recibir notificacion push  
**Prop�sito**: Notificar de forma inmediata al usuario cuando el sistema le genera una nueva tarea.  
**Actores**: Client, Sistema BPMN  
**Actor iniciador**: Sistema BPMN  
**Pre condici�n**: Usuario con token FCM vigente registrado en backend.  
**Flujo Principal**:
1. El motor BPMN crea una nueva tarea pendiente.
2. El backend identifica usuarios objetivo por proceso/carril.
3. El backend envia notificacion push via FCM.
4. El dispositivo recibe la notificacion (foreground/background/terminated).
5. El usuario abre la app y atiende la nueva tarea.
**Post condici�n**: Usuario informado en tiempo real de la nueva tarea disponible.  
**Excepci�n**:
- Firebase no inicializado o credenciales invalidas.
- Token expirado/no registrado.
- Falla de entrega en FCM (sin bloquear la ejecucion del flujo BPMN).

## 4. CU agregados por Autocompletado de Formulario con IA

| ID | Nombre del Caso de Uso | Estado | Prioridad | Riesgo | Actores involucrados | Ciclo |
|---|---|---|---|---|---|---|
| CU23 | Rellenar formulario de tarea con IA (voz/texto) | Incorporado | Significativo | Critico | Functionary, Client, Sistema BPMN, Copilot IA | C4 |

### CU23
**ID**: CU23  
**Nombre de caso de uso**: Rellenar formulario de tarea con IA (voz/texto)  
**Proposito**: Asistir al usuario en el prellenado de campos de un formulario dinamico de tarea a partir de transcripcion de voz o texto, manteniendo el envio final bajo confirmacion manual.  
**Actores**: Functionary, Client, Sistema BPMN, Copilot IA  
**Actor iniciador**: Functionary / Client  
**Pre condicion**:
1. Tarea en estado `IN_PROGRESS`.
2. Formulario de tarea disponible en el detalle.
3. Servicio IA de autocompletado disponible.
**Flujo Principal**:
1. El actor inicia captura de voz (o ingresa texto) en el formulario de ejecucion.
2. El sistema obtiene transcripcion y la muestra para revision.
3. El actor solicita explicitamente "Enviar a la IA".
4. El frontend envia transcripcion + lista de campos del formulario al backend.
5. El backend delega al motor IA la extraccion estructurada de valores.
6. El motor IA devuelve un JSON clave-valor restringido a campos del formulario.
7. El frontend aplica `patchValue` y rellena visualmente los controles.
8. El actor revisa respuestas y decide completar manualmente la tarea.
**Post condicion**: Formulario prellenado con sugerencias IA sin completar automaticamente la tarea.  
**Excepcion**:
- Error de reconocimiento de voz.
- Servicio IA no disponible o saturado.
- Respuesta IA invalida o sin campos utilizables.
- El actor cancela o no envia la transcripcion a IA.

### Ubicacion por paquete funcional
- **Paquete 3: Ejecucion Operativa de Procesos**  
Justificacion: el caso de uso ocurre durante la ejecucion de tareas y afecta exclusivamente la captura de `formData` previa al completado.
