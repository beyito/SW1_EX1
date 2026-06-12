# Versionamiento Documental y Cuadrículas en Formularios

## Objetivo

Completar dos capacidades del iBPMS:

- Versionamiento de documentos almacenados en S3 y editados colaborativamente con OnlyOffice.
- Soporte de campos tipo cuadrícula en formularios dinámicos de tareas BPMN.

## Cambios Backend

### Versionamiento documental

- `backend/src/main/java/com/politicanegocio/core/model/DocumentRecord.java`
  - Se agregaron `currentVersionNumber` y `currentS3VersionId`.
  - Permite saber cuál es la versión vigente del documento.

- `backend/src/main/java/com/politicanegocio/core/model/DocumentVersion.java`
  - Nueva colección `document_versions`.
  - Guarda historial lógico de versiones por documento.
  - Campos principales: `documentId`, `versionNumber`, `s3Key`, `s3VersionId`, `size`, `source`, `createdBy`, `createdAt`.

- `backend/src/main/java/com/politicanegocio/core/repository/DocumentVersionRepository.java`
  - Permite listar versiones por documento y buscar una versión específica.

- `backend/src/main/java/com/politicanegocio/core/service/S3Service.java`
  - `upload()` captura el `versionId` devuelto por AWS S3.
  - `replaceObject()` devuelve el nuevo `versionId`.
  - Se agregó lectura y URL prefirmada por `versionId`.

- `backend/src/main/java/com/politicanegocio/core/service/DocumentService.java`
  - Registra versión inicial al subir un documento.
  - Lista versiones disponibles.
  - Permite restaurar una versión anterior cuando existe `s3VersionId`.

- `backend/src/main/java/com/politicanegocio/core/service/OnlyOfficeService.java`
  - Cada callback guardable de OnlyOffice crea una nueva versión documental.
  - Actualiza `currentVersionNumber`, `currentS3VersionId`, `size` y `updatedAt`.

- `backend/src/main/java/com/politicanegocio/core/controller/DocumentController.java`
  - Nuevo endpoint `GET /api/documents/{documentId}/versions`.
  - Nuevo endpoint `POST /api/documents/{documentId}/versions/{versionNumber}/restore`.

### Cuadrículas en formularios

- `backend/src/main/java/com/politicanegocio/core/service/ProcessExecutionService.java`
  - La validación de campos obligatorios reconoce `type = grid`.
  - Una cuadrícula obligatoria solo pasa si tiene al menos una celda con valor.

## Cambios Frontend

### Formularios BPMN

- `frontend/src/app/features/policy-designer/models/policy-designer.models.ts`
  - Se agregó `type: grid`.
  - Se agregaron `gridColumns` y `gridRows`.

- `frontend/src/app/features/policy-designer/components/policy-designer/policy-designer.component.ts`
  - Permite configurar columnas de cuadrícula y filas iniciales.
  - Valida que una cuadrícula tenga al menos una columna.

- `frontend/src/app/features/policy-designer/components/policy-designer/policy-designer.component.html`
  - Se agregó la opción `Cuadricula` al selector de tipos de pregunta.

- `frontend/src/app/features/execution/components/task-execution/task-execution.component.ts`
  - Renderiza datos de cuadrícula como arreglo de filas.
  - Permite agregar, quitar y editar filas.

- `frontend/src/app/features/execution/components/task-execution/task-execution.component.html`
  - Se agregó tabla editable para campos tipo `grid`.

### Historial documental

- `frontend/src/app/features/execution/models/execution.models.ts`
  - Se agregó `DocumentVersionDto`.
  - `DocumentDto` ahora expone versión actual.

- `frontend/src/app/features/execution/services/execution.service.ts`
  - Se agregaron métodos para listar y restaurar versiones.

- `frontend/src/app/features/execution/components/document-viewer/document-viewer.component.*`
  - El visor muestra la versión actual.
  - Lista historial de versiones.
  - Permite restaurar versiones si el usuario tiene permiso de edición.

## Configuración Requerida en AWS

Para que la restauración de versiones funcione completamente, se debe activar **Bucket Versioning** en el bucket S3.

Ruta en AWS Console:

```text
S3 -> Buckets -> seleccionar bucket -> Properties -> Bucket Versioning -> Edit -> Enable
```

Sin Bucket Versioning:

- El sistema registra el historial lógico en MongoDB.
- Se incrementa el número de versión.
- No se puede restaurar una versión anterior porque S3 no conserva el objeto histórico.

Con Bucket Versioning:

- AWS devuelve `versionId` en cada subida o reemplazo.
- MongoDB guarda ese `versionId`.
- El sistema puede restaurar una versión anterior leyendo esa versión específica desde S3.

## Verificación

- Build Angular ejecutado correctamente con `npm run build`.
- Maven no pudo ejecutarse desde el sandbox local por error de arranque del entorno Windows, no por error de compilación reportado por Maven.
