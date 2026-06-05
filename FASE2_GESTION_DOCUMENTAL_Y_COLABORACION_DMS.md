# FASE 2: Gestion Documental, Privilegios, Auditoria y OnlyOffice Colaborativo

## Plan de Accion

- `backend/src/main/java/com/politicanegocio/core/model/DocumentAction.java`
- `backend/src/main/java/com/politicanegocio/core/model/DocumentPermission.java`
- `backend/src/main/java/com/politicanegocio/core/repository/DocumentPermissionRepository.java`
- `backend/src/main/java/com/politicanegocio/core/repository/DocumentRecordRepository.java`
- `backend/src/main/java/com/politicanegocio/core/repository/DocumentAuditLogRepository.java`
- `backend/src/main/java/com/politicanegocio/core/dto/AdminDocumentDto.java`
- `backend/src/main/java/com/politicanegocio/core/dto/DocumentPermissionDto.java`
- `backend/src/main/java/com/politicanegocio/core/dto/DocumentPermissionUpdateRequest.java`
- `backend/src/main/java/com/politicanegocio/core/dto/DocumentAuditLogDto.java`
- `backend/src/main/java/com/politicanegocio/core/service/DocumentAccessService.java`
- `backend/src/main/java/com/politicanegocio/core/service/DocumentAdminService.java`
- `backend/src/main/java/com/politicanegocio/core/service/DocumentService.java`
- `backend/src/main/java/com/politicanegocio/core/service/OnlyOfficeService.java`
- `backend/src/main/java/com/politicanegocio/core/service/S3Service.java`
- `backend/src/main/java/com/politicanegocio/core/controller/DocumentAdminController.java`
- `backend/src/main/java/com/politicanegocio/core/controller/DocumentController.java`
- `frontend/src/app/features/execution/components/document-viewer/document-viewer.component.ts`
- `frontend/src/app/features/execution/components/document-viewer/document-viewer.component.html`
- `frontend/src/app/features/execution/components/document-viewer/document-viewer.component.scss`
- `frontend/src/app/features/document-admin/document-privileges.component.ts`
- `frontend/src/app/features/document-admin/document-privileges.component.html`
- `frontend/src/app/features/document-admin/document-audit.component.ts`
- `frontend/src/app/features/document-admin/document-audit.component.html`
- `frontend/src/app/features/document-admin/document-admin.scss`
- `frontend/src/app/features/execution/services/execution.service.ts`
- `frontend/src/app/features/execution/models/execution.models.ts`
- `frontend/src/app/features/admin-layout/admin-layout.component.html`
- `frontend/src/app/app.routes.ts`
- `backend/pom.xml`

## Modificaciones

### [archivo: backend/src/main/java/com/politicanegocio/core/model/DocumentAction.java]

- Se agrego la accion `DELETE`.
- La auditoria ahora distingue entre `VIEW`, `EDIT` y `DELETE`.

### [archivo: backend/src/main/java/com/politicanegocio/core/model/DocumentPermission.java]

- Nueva coleccion `document_permissions`.
- Guarda privilegios por documento y funcionario:
  - `canView`
  - `canEdit`
  - `canDelete`
- Incluye `company`, `updatedBy` y `updatedAt` para trazabilidad administrativa.
- Tiene indice unico por `documentId + userId`.

### [archivo: backend/src/main/java/com/politicanegocio/core/repository/DocumentPermissionRepository.java]

- Nuevo repositorio MongoDB para consultar y guardar permisos.
- Permite listar permisos por documento y buscar permisos por documento/usuario.

### [archivo: backend/src/main/java/com/politicanegocio/core/repository/DocumentRecordRepository.java]

- Se agrego consulta por lista de politicas:
  - `findByPolicyIdInOrderByCreatedAtDesc`
- Permite listar documentos de una empresa desde el panel admin.

### [archivo: backend/src/main/java/com/politicanegocio/core/repository/DocumentAuditLogRepository.java]

- Se agrego consulta de auditoria por multiples documentos.
- Permite ver auditoria global de documentos de la empresa.

### [archivo: backend/src/main/java/com/politicanegocio/core/dto/AdminDocumentDto.java]

- Nuevo DTO para mostrar documentos en el panel administrativo sin exponer detalles internos de S3.

### [archivo: backend/src/main/java/com/politicanegocio/core/dto/DocumentPermissionDto.java]

- Nuevo DTO de salida para privilegios documentales.

### [archivo: backend/src/main/java/com/politicanegocio/core/dto/DocumentPermissionUpdateRequest.java]

- Nuevo DTO de entrada para asignar permisos a funcionarios.

### [archivo: backend/src/main/java/com/politicanegocio/core/dto/DocumentAuditLogDto.java]

- Nuevo DTO de auditoria para mostrar acciones documentales en frontend.

### [archivo: backend/src/main/java/com/politicanegocio/core/service/DocumentAccessService.java]

- Nuevo servicio central de seguridad documental.
- Reglas aplicadas:
  - `COMPANY_ADMIN`: puede ver, editar y eliminar documentos de su empresa.
  - `CLIENT`: puede ver documentos de sus propios tramites, pero no editar/eliminar.
  - `FUNCTIONARY`: necesita privilegios explicitos por documento.
- Este servicio evita duplicar reglas en `DocumentService` y `OnlyOfficeService`.

### [archivo: backend/src/main/java/com/politicanegocio/core/service/DocumentAdminService.java]

- Nuevo servicio administrativo DMS.
- Funcionalidades:
  - Lista documentos de la empresa.
  - Lista permisos por documento.
  - Asigna privilegios a funcionarios.
  - Lista auditoria por documento.
  - Lista auditoria global de la empresa.

### [archivo: backend/src/main/java/com/politicanegocio/core/service/DocumentService.java]

- Se elimino toda la edicion casera temporal.
- Se eliminaron operaciones basadas en Apache POI.
- Ahora solo gestiona:
  - Registro de documentos.
  - Consulta con permiso `VIEW`.
  - Listado filtrado por privilegios.
  - Eliminacion con permiso `DELETE`.
  - Auditoria de acciones.
- Si un funcionario sube un documento, se le asignan automaticamente permisos `VIEW`, `EDIT` y `DELETE` sobre ese documento.

### [archivo: backend/src/main/java/com/politicanegocio/core/service/OnlyOfficeService.java]

- OnlyOffice queda como unico flujo de edicion documental.
- Para generar configuracion de editor exige permiso `EDIT`.
- Registra auditoria `EDIT` cuando se inicia la sesion de edicion.
- En callback guarda cambios en S3 para estados `2` y `6`.
- Convierte URLs publicas de OnlyOffice a URL interna Docker cuando el backend debe descargar el archivo editado.

### [archivo: backend/src/main/java/com/politicanegocio/core/service/S3Service.java]

- Se agrego `deleteObject`.
- Permite eliminar fisicamente archivos de S3 cuando un usuario con privilegio `DELETE` elimina un documento.

### [archivo: backend/src/main/java/com/politicanegocio/core/controller/DocumentAdminController.java]

- Nuevo controlador administrativo:
  - `GET /api/admin/documents`
  - `GET /api/admin/documents/audit`
  - `GET /api/admin/documents/{documentId}/permissions`
  - `PUT /api/admin/documents/{documentId}/permissions`
  - `GET /api/admin/documents/{documentId}/audit`
- Protegido con `COMPANY_ADMIN`.

### [archivo: backend/src/main/java/com/politicanegocio/core/controller/DocumentController.java]

- Se eliminaron endpoints de editor temporal:
  - `GET /editor-content`
  - `PUT /editor-content`
- Se agrego:
  - `DELETE /api/documents/{documentId}`

### [archivo: frontend/src/app/features/execution/components/document-viewer/document-viewer.component.ts]

- Se elimino navegacion al editor casero.
- La unica edicion disponible abre OnlyOffice.
- El boton ahora inicia una sesion colaborativa real.

### [archivo: frontend/src/app/features/execution/components/document-viewer/document-viewer.component.html]

- Se cambio el texto del boton a `Iniciar edicion`.
- El overlay sigue usando `onlyoffice-placeholder` para renderizar `DocsAPI.DocEditor`.

### [archivo: frontend/src/app/features/execution/components/document-viewer/document-viewer.component.scss]

- Se mantienen estilos del overlay OnlyOffice.
- Se elimino dependencia visual del editor temporal.

### [archivo: frontend/src/app/features/document-admin/document-privileges.component.ts]

- Nueva pantalla de administracion de privilegios.
- Carga documentos y funcionarios.
- Permite asignar `ver`, `editar` y `eliminar` por funcionario y documento.

### [archivo: frontend/src/app/features/document-admin/document-privileges.component.html]

- Nueva tabla de asignacion de permisos.
- Permite seleccionar documento y guardar permisos por funcionario.

### [archivo: frontend/src/app/features/document-admin/document-audit.component.ts]

- Nueva pantalla de auditoria documental.
- Permite ver auditoria global o filtrada por documento.

### [archivo: frontend/src/app/features/document-admin/document-audit.component.html]

- Nueva tabla de auditoria con fecha, usuario, accion, documento y ruta HTTP.

### [archivo: frontend/src/app/features/document-admin/document-admin.scss]

- Estilos compartidos para pantallas DMS admin.

### [archivo: frontend/src/app/features/execution/services/execution.service.ts]

- Se eliminaron metodos del editor casero.
- Se conserva `getOnlyOfficeConfig`.

### [archivo: frontend/src/app/features/execution/models/execution.models.ts]

- Se eliminaron modelos del editor temporal.
- Se conserva `OnlyOfficeConfigDto`.

### [archivo: frontend/src/app/features/admin-layout/admin-layout.component.html]

- Se agregaron accesos:
  - `Privilegios DMS`
  - `Auditoria DMS`

### [archivo: frontend/src/app/app.routes.ts]

- Se elimino ruta temporal:
  - `/documents/:id/edit`
- Se agregaron rutas admin:
  - `/admin/document-privileges`
  - `/admin/document-audit`

### [archivo: backend/pom.xml]

- Se eliminaron dependencias Apache POI.
- Ya no son necesarias porque la edicion se delega totalmente a OnlyOffice.

## Validacion

- Backend compilado correctamente:
  - `cmd /c mvnw.cmd -q -DskipTests compile`
- Frontend compilado correctamente:
  - `npm.cmd run build`
- Docker Compose validado correctamente:
  - `docker compose config`
## Actualización - Documentos en Procesos Activos

### Plan de Acción

- [archivo: backend/src/main/java/com/politicanegocio/core/dto/DocumentDto.java] Se agregaron banderas de privilegio efectivas para la UI.
- [archivo: backend/src/main/java/com/politicanegocio/core/service/DocumentAccessService.java] Se expusieron validaciones reutilizables de edición y eliminación.
- [archivo: backend/src/main/java/com/politicanegocio/core/service/DocumentService.java] El listado de documentos por instancia ahora filtra por permisos y devuelve `canEdit`/`canDelete`.
- [archivo: frontend/src/app/features/execution/models/execution.models.ts] Se actualizó el contrato `DocumentDto` para reflejar permisos documentales.
- [archivo: frontend/src/app/features/execution/services/execution.service.ts] Se agregó `getProcessDocuments(processInstanceId)` para consultar documentos vinculados a una instancia de trámite.
- [archivo: frontend/src/app/features/execution/components/funcionario-dashboard/funcionario-dashboard.component.ts] Se agregó estado y acciones para abrir/cerrar el panel de documentos, ver documentos y abrir edición solo con privilegios.
- [archivo: frontend/src/app/features/execution/components/funcionario-dashboard/funcionario-dashboard.component.html] Se agregó el botón flotante `Documentos` en Procesos Activos y el panel lateral con acciones por documento.
- [archivo: frontend/src/app/features/execution/components/funcionario-dashboard/funcionario-dashboard.component.scss] Se agregaron estilos del botón flotante y drawer responsivo.
- [archivo: frontend/src/app/features/execution/components/document-viewer/document-viewer.component.ts] Se agregó autoapertura segura del editor con `?edit=1` y mensajes entendibles si el usuario no tiene privilegios.

### Modificaciones

#### [archivo: backend/src/main/java/com/politicanegocio/core/dto/DocumentDto.java]

- Se agregaron `canEdit` y `canDelete` para que Angular no tenga que inferir permisos.
- El DTO conserva compatibilidad con el método `from(record, url)` y suma una sobrecarga con permisos efectivos.

#### [archivo: backend/src/main/java/com/politicanegocio/core/service/DocumentService.java]

- `getDocumentsForProcess` valida acceso a la instancia, filtra documentos visibles con `canView` y mapea cada documento con permisos efectivos.
- `getDocument` devuelve también `canEdit` y `canDelete`, lo que permite bloquear la edición desde el visor aunque alguien fuerce la URL.

#### [archivo: frontend/src/app/features/execution/components/funcionario-dashboard/funcionario-dashboard.component.*]

- En la bandeja de Procesos Activos se agregó un botón flotante inferior derecho `Documentos`.
- Al presionarlo, se abre un panel lateral con los documentos relacionados a la instancia seleccionada.
- Cada documento permite `Ver` y, si `canEdit` es verdadero, `Editar`. Si no hay permiso, se muestra el botón deshabilitado y un mensaje claro.

#### [archivo: frontend/src/app/features/execution/components/document-viewer/document-viewer.component.ts]

- La navegación con `?edit=1` abre OnlyOffice automáticamente solo cuando el documento es compatible y el usuario tiene permiso de edición.
- Si el permiso no existe, se muestra: `No tienes privilegios para editar este documento.`
