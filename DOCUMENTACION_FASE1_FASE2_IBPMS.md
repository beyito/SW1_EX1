# Documentación de Fase 1 y Fase 2 - Evolución iBPMS

## 1. Resumen Ejecutivo

Este documento describe las funcionalidades implementadas en la Fase 1 y Fase 2 del proyecto iBPMS, tomando como base la planificación definida en `FASES_A_HACER.md` y los cambios desarrollados en el backend Spring Boot, frontend Angular, almacenamiento AWS S3 y edición colaborativa con OnlyOffice Document Server.

La Fase 1 se enfocó en reestructurar el núcleo del sistema para soportar requisitos documentales iniciales y una organización de archivos escalable en S3. La Fase 2 incorporó capacidades de Gestión Documental (DMS), privilegios por documento, auditoría y edición colaborativa real mediante OnlyOffice.

## 2. Actores Principales

- **Administrador de Empresa:** configura procesos, requisitos iniciales, privilegios documentales y consulta auditoría.
- **Funcionario:** ejecuta tareas de procesos activos, visualiza documentos permitidos y edita documentos si tiene privilegios.
- **Cliente o Solicitante:** inicia trámites y adjunta documentos requeridos para la instancia.
- **Sistema iBPMS:** orquesta procesos, valida políticas, gestiona documentos, registra auditoría y coordina integraciones.
- **OnlyOffice Document Server:** motor externo encargado de la edición colaborativa en tiempo real de documentos Office.
- **AWS S3:** repositorio de archivos físicos asociado a clientes, trámites e instancias de proceso.

## 3. Fase 1 - Reestructuración Core y Almacenamiento

### 3.1 Objetivo

Preparar el sistema para comportarse como un iBPMS más robusto, permitiendo que las políticas o trámites definan requisitos iniciales y que los documentos se almacenen en una estructura jerárquica controlada dentro de S3.

### 3.2 Funcionalidades Implementadas

- Se agregó soporte para **requisitos iniciales** en las políticas/trámites.
- Cada requisito puede ser obligatorio u opcional.
- Los requisitos permiten definir información como nombre, descripción y extensiones permitidas.
- El modelador Angular permite configurar estos requisitos desde la administración del proceso.
- El backend persiste los requisitos dentro de la entidad `Policy`.
- Al iniciar o ejecutar procesos, el sistema puede reconocer qué documentos están asociados a requisitos iniciales.
- Se refactorizó el almacenamiento S3 para evitar archivos sueltos en la raíz del bucket.
- Se adoptó la estructura:

```text
clientes/{clientId}/tramites/{processInstanceId}/{documentId}_{fileName}
```

### 3.3 Componentes Técnicos Involucrados

- `Policy.java`
- `PolicyInitialRequirement.java`
- `PolicyService.java`
- `StartablePolicyDto.java`
- `ProcessExecutionService.java`
- `S3Service.java`
- `FileUploadController.java`
- `schema.graphqls`
- Componentes Angular del modelador de políticas
- Servicios Angular de políticas y archivos

### 3.4 Casos de Uso Generados

#### CU-F1-01: Definir Requisitos Iniciales de un Trámite

- **Actor principal:** Administrador de Empresa.
- **Objetivo:** configurar los documentos que se deben adjuntar para iniciar un trámite.
- **Precondición:** el administrador está creando o editando una política BPMN.
- **Flujo principal:** el administrador abre el modelador, agrega requisitos iniciales, marca cada requisito como obligatorio u opcional, define extensiones permitidas y guarda la política.
- **Resultado:** la política queda persistida con sus requisitos iniciales.

#### CU-F1-02: Consultar Trámites Iniciables con Requisitos

- **Actor principal:** Cliente o Funcionario autorizado.
- **Objetivo:** visualizar qué trámites puede iniciar y qué requisitos documentales solicita cada uno.
- **Precondición:** existen políticas activas publicadas.
- **Flujo principal:** el usuario entra a la bandeja de procesos disponibles, el sistema consulta las políticas iniciables y devuelve los requisitos iniciales asociados.
- **Resultado:** la UI puede mostrar los documentos requeridos antes de iniciar el trámite.

#### CU-F1-03: Subir Documento Asociado a una Instancia de Trámite

- **Actor principal:** Cliente, Funcionario o Sistema.
- **Objetivo:** almacenar un archivo de forma ordenada y trazable.
- **Precondición:** existe una instancia de proceso o se está creando una instancia con documentos requeridos.
- **Flujo principal:** el usuario adjunta un archivo, el backend genera un identificador documental, construye la ruta jerárquica y guarda el archivo en S3.
- **Resultado:** el archivo queda almacenado bajo `clientes/{clientId}/tramites/{processInstanceId}/`.

#### CU-F1-04: Leer Archivo desde S3 con Ruta Jerárquica

- **Actor principal:** Sistema iBPMS.
- **Objetivo:** recuperar documentos usando la nueva estructura de almacenamiento.
- **Precondición:** el documento fue registrado con su `s3Key`.
- **Flujo principal:** el backend localiza el registro documental, genera una URL de lectura o descarga y la entrega al frontend o a un servicio autorizado.
- **Resultado:** el archivo puede ser consultado sin exponer la estructura interna como lógica de UI.

#### CU-F1-05: Iniciar Trámite desde la App Móvil con Requisitos Iniciales

- **Actor principal:** Cliente móvil.
- **Objetivo:** iniciar un trámite adjuntando previamente los documentos requeridos por la política.
- **Precondición:** el cliente inició sesión en la aplicación móvil y existen trámites disponibles con requisitos iniciales configurados.
- **Flujo principal:** el cliente selecciona un trámite, la app muestra sus requisitos iniciales, el cliente adjunta un archivo por requisito obligatorio u opcional, el sistema valida que no falten obligatorios, crea la instancia y sube cada archivo asociado al `processInstanceId` y al identificador del requisito.
- **Resultado:** el trámite queda iniciado y los documentos iniciales quedan registrados en S3 y en el DMS.

## 4. Fase 2 - Gestión Documental y Colaboración DMS

### 4.1 Objetivo

Construir una capa DMS sobre los documentos del trámite, incorporando control de acceso, auditoría y edición colaborativa real para documentos Word y Excel mediante OnlyOffice.

### 4.2 Funcionalidades Implementadas

- Se creó un modelo documental persistente para registrar documentos asociados a procesos.
- Se implementó administración de privilegios por documento.
- Los privilegios soportan acciones de visualización, edición y eliminación.
- Se agregó auditoría para registrar accesos y acciones sobre documentos.
- Se implementaron endpoints administrativos para listar documentos, asignar permisos y consultar auditoría.
- Se integró OnlyOffice Document Server para edición colaborativa real.
- El backend genera la configuración oficial de OnlyOffice con URL prefirmada de S3.
- El backend recibe callbacks de OnlyOffice y sobreescribe el documento original en S3 cuando el editor finaliza el guardado.
- El frontend carga dinámicamente el script de OnlyOffice y renderiza el editor dentro de un contenedor dedicado.
- Se eliminaron piezas temporales de edición colaborativa casera para evitar concurrencia manual y duplicidad arquitectónica.
- En Procesos Activos se agregó un panel de documentos por instancia, con acciones condicionadas por permisos.

### 4.3 Componentes Técnicos Involucrados

- `DocumentRecord.java`
- `DocumentPermission.java`
- `DocumentAuditLog.java`
- `DocumentAction.java`
- `DocumentRecordRepository.java`
- `DocumentPermissionRepository.java`
- `DocumentAuditLogRepository.java`
- `DocumentService.java`
- `DocumentAccessService.java`
- `DocumentAdminService.java`
- `OnlyOfficeService.java`
- `DocumentController.java`
- `DocumentAdminController.java`
- `OnlyOfficeController.java`
- `DocumentAuditInterceptor.java`
- `WebMvcConfig.java`
- `SecurityConfig.java`
- `DocumentDto.java`
- `OnlyOfficeConfigDto.java`
- `DocumentPermissionDto.java`
- `DocumentAuditLogDto.java`
- `execution.service.ts`
- `execution.models.ts`
- `document-viewer.component.*`
- `funcionario-dashboard.component.*`
- `mobile/lib/features/dashboard/models/startable_policy.dart`
- `mobile/lib/features/dashboard/models/process_instance.dart`
- `mobile/lib/features/dashboard/presentation/start_process_requirements_screen.dart`
- `mobile/lib/features/dashboard/presentation/dashboard_screen.dart`
- `mobile/lib/features/tasks/services/task_service.dart`
- Módulo Angular de administración documental
- `infra/docker-compose.yml`
- `infra/nginx.conf`
- Variables de configuración de OnlyOffice y backend interno

### 4.4 Casos de Uso Generados

#### CU-F2-01: Registrar Documento en el DMS

- **Actor principal:** Sistema iBPMS.
- **Objetivo:** crear un registro lógico para cada archivo subido.
- **Precondición:** un usuario sube un archivo asociado a una instancia de trámite.
- **Flujo principal:** el backend guarda el archivo en S3, crea un `DocumentRecord`, conserva datos como nombre, tipo, tamaño, instancia, política y ruta S3.
- **Resultado:** el documento queda disponible para control de permisos, auditoría y edición.

#### CU-F2-02: Asignar Privilegios Documentales

- **Actor principal:** Administrador de Empresa.
- **Objetivo:** definir qué funcionarios pueden ver, editar o eliminar un documento.
- **Precondición:** existe al menos un documento registrado en el DMS.
- **Flujo principal:** el administrador abre el panel de privilegios, selecciona un documento, asigna permisos por usuario y guarda los cambios.
- **Resultado:** el sistema persiste permisos granulares en `DocumentPermission`.

#### CU-F2-03: Visualizar Documento con Control de Acceso

- **Actor principal:** Funcionario.
- **Objetivo:** abrir un documento permitido sin descargarlo automáticamente.
- **Precondición:** el funcionario tiene privilegio de visualización o rol administrativo aplicable.
- **Flujo principal:** el funcionario abre el documento, el backend valida permisos, genera URL de lectura y registra auditoría.
- **Resultado:** el usuario visualiza el documento y queda registrado el evento `VIEW`.

#### CU-F2-04: Editar Documento con OnlyOffice

- **Actor principal:** Funcionario autorizado.
- **Objetivo:** editar un documento Word o Excel de forma colaborativa en tiempo real.
- **Precondición:** el funcionario tiene privilegio de edición y el documento es compatible con OnlyOffice.
- **Flujo principal:** el usuario presiona `Iniciar edición`, Angular solicita la configuración al backend, Spring Boot genera la configuración oficial con URL prefirmada de S3 y callback, Angular carga el script de OnlyOffice y crea el editor.
- **Resultado:** el documento se abre en una sesión colaborativa real.

#### CU-F2-05: Guardar Cambios de OnlyOffice en S3

- **Actor principal:** OnlyOffice Document Server.
- **Objetivo:** persistir en S3 la versión editada del documento.
- **Precondición:** existe una sesión de edición y OnlyOffice informa estado de guardado.
- **Flujo principal:** OnlyOffice invoca el `callbackUrl`, el backend valida el estado, descarga el archivo modificado desde la URL enviada por OnlyOffice y sobrescribe el objeto original en S3.
- **Resultado:** el documento original queda actualizado con los cambios colaborativos.

#### CU-F2-06: Consultar Auditoría Documental

- **Actor principal:** Administrador de Empresa.
- **Objetivo:** revisar quién vio, editó o eliminó documentos.
- **Precondición:** existen eventos de auditoría registrados.
- **Flujo principal:** el administrador abre el módulo de auditoría, filtra o consulta eventos y el sistema muestra usuario, acción, documento y fecha.
- **Resultado:** la empresa tiene trazabilidad de acciones documentales.

#### CU-F2-07: Eliminar Documento con Privilegios

- **Actor principal:** Administrador de Empresa o Funcionario autorizado.
- **Objetivo:** eliminar un documento cuando el usuario tiene permiso suficiente.
- **Precondición:** el documento existe y el usuario tiene privilegio `DELETE`.
- **Flujo principal:** el usuario solicita eliminar, el backend valida permisos, elimina el objeto de S3, borra el registro documental y registra auditoría.
- **Resultado:** el documento deja de estar disponible y queda evidencia del evento.

#### CU-F2-08: Ver Documentos desde Procesos Activos

- **Actor principal:** Funcionario.
- **Objetivo:** consultar documentos relacionados con una instancia de trámite desde la bandeja de tareas.
- **Precondición:** el funcionario tiene acceso al proceso activo.
- **Flujo principal:** el funcionario selecciona un proceso activo, presiona el botón `Documentos`, el sistema lista los documentos visibles para ese usuario y muestra acciones disponibles según permisos.
- **Resultado:** el funcionario accede rápidamente a documentos del trámite sin salir del contexto de ejecución.

#### CU-F2-09: Bloquear Edición sin Privilegios

- **Actor principal:** Sistema iBPMS.
- **Objetivo:** evitar edición no autorizada con mensajes comprensibles.
- **Precondición:** un usuario intenta editar un documento sin permiso.
- **Flujo principal:** la UI revisa `canEdit`; si no existe permiso, deshabilita la acción o muestra un mensaje claro. El backend también valida permisos si el usuario fuerza la ruta.
- **Resultado:** el sistema protege el documento y evita mostrar errores técnicos al usuario final.

## 5. Reglas de Negocio Implementadas

- Un documento pertenece a una instancia de trámite.
- Un documento puede tener privilegios independientes por funcionario.
- Ver, editar y eliminar son acciones separadas.
- Los administradores de empresa pueden gestionar privilegios y auditoría documental.
- Los funcionarios solo pueden acceder a documentos visibles para ellos.
- La edición colaborativa no se implementa manualmente; se delega a OnlyOffice para evitar problemas de concurrencia.
- El backend es el punto de confianza para generar URLs prefirmadas, validar permisos y persistir callbacks.
- S3 almacena archivos físicos; MongoDB/PostgreSQL registra metadatos, permisos y auditoría según la configuración del proyecto.

## 6. Beneficios Obtenidos

- Mayor trazabilidad documental.
- Separación clara entre archivo físico, registro documental, permisos y auditoría.
- Preparación del sistema para flujos iBPMS más complejos.
- Edición colaborativa real para documentos Office.
- Menor riesgo de concurrencia al evitar un editor casero.
- Experiencia de usuario más clara para funcionarios y administradores.
- Almacenamiento S3 ordenado por cliente e instancia de trámite.

## 7. Estado de Validación

- El backend compila correctamente con Maven.
- El frontend compila correctamente con Angular.
- Se verificó que no queden textos corruptos por problemas de codificación en `frontend/src`, `backend/src` y documentación principal de Fase 2.
- Quedan advertencias conocidas del build relacionadas con budget del diseñador BPMN, dependencias CommonJS de JointJS/STOMP y selectores CSS externos.
