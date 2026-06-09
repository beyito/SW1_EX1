# Guía UML 2.5 para Diagramas de Comunicación y Secuencia

Este documento analiza los 4 casos de uso más complejos del sistema y describe cómo modelarlos en UML 2.5 usando diagramas de comunicación y diagramas de secuencia.

Casos de uso cubiertos:

- `CU-05: Editar Documento con OnlyOffice`
- `CU-07: Solicitar Recomendación de Trámite en Lenguaje Natural`
- `CU-08: Consultar Análisis Predictivo Operativo`
- `CU-09: Generar Reporte Dinámico`

## Convenciones UML 2.5 Recomendadas

### Estereotipos

Usa estos estereotipos para clasificar participantes:

- `<<actor>>`: usuario externo.
- `<<boundary>>`: pantalla, componente UI o endpoint expuesto.
- `<<control>>`: servicio que coordina el caso de uso.
- `<<entity>>`: repositorio, base de datos o entidad persistida.
- `<<external>>`: servicio externo como S3, OnlyOffice o motor IA.

### Fragmentos Combinados para Secuencia

Usa estos fragmentos UML 2.5:

- `alt`: caminos alternativos, por ejemplo permiso válido/no válido.
- `opt`: comportamiento opcional, por ejemplo fallback heurístico.
- `loop`: iteraciones, por ejemplo recorrer policies, tareas o documentos.
- `break`: corte del flujo por error.

### Reglas para Diagramas de Comunicación

En diagramas de comunicación, numera mensajes con jerarquía:

```text
1
1.1
1.2
2
2.1
```

Esto ayuda a representar orden temporal sin dibujar lifelines.

---

# CU-05: Editar Documentos

## Objetivo del Diagrama

Representar cómo un funcionario autorizado abre un documento Word/Excel en OnlyOffice, cómo Spring genera la configuración oficial del editor, cómo OnlyOffice descarga el archivo desde S3 y cómo guarda cambios mediante callback.

## Código Relacionado

- `frontend/src/app/features/execution/components/document-viewer/document-viewer.component.ts`
- `frontend/src/app/features/execution/services/execution.service.ts`
- `backend/src/main/java/com/politicanegocio/core/controller/OnlyOfficeController.java`
- `backend/src/main/java/com/politicanegocio/core/service/OnlyOfficeService.java`
- `backend/src/main/java/com/politicanegocio/core/service/DocumentAccessService.java`
- `backend/src/main/java/com/politicanegocio/core/service/S3Service.java`
- `backend/src/main/java/com/politicanegocio/core/repository/DocumentRecordRepository.java`
- `backend/src/main/java/com/politicanegocio/core/repository/DocumentAuditLogRepository.java`
- `OnlyOffice Document Server`
- `AWS S3`

## Participantes para Diagrama de Secuencia

- `Funcionario autorizado <<actor>>`
- `DocumentViewerComponent <<boundary>>`
- `ExecutionService <<control>>`
- `OnlyOfficeController <<boundary>>`
- `OnlyOfficeService <<control>>`
- `DocumentAccessService <<control>>`
- `DocumentRecordRepository <<entity>>`
- `S3Service <<control>>`
- `AWS S3 <<external>>`
- `OnlyOffice Document Server <<external>>`
- `DocumentAuditLogRepository <<entity>>`

## Flujo Principal para Diagrama de Secuencia

1. `Funcionario autorizado -> DocumentViewerComponent`: presiona `Iniciar edición`.
2. `DocumentViewerComponent -> DocumentViewerComponent`: valida `document.canEdit`.
3. `DocumentViewerComponent -> ExecutionService`: `getOnlyOfficeConfig(documentId)`.
4. `ExecutionService -> OnlyOfficeController`: `GET /api/onlyoffice/config/{documentId}`.
5. `OnlyOfficeController -> OnlyOfficeService`: `buildEditorConfig(documentId, user)`.
6. `OnlyOfficeService -> DocumentRecordRepository`: `findById(documentId)`.
7. `OnlyOfficeService -> DocumentAccessService`: `assertCanAccessDocument(user, record, EDIT)`.
8. `OnlyOfficeService -> OnlyOfficeService`: valida extensión compatible.
9. `OnlyOfficeService -> S3Service`: `createPresignedReadUrl(record.s3Key, presignedHours)`.
10. `S3Service -> AWS S3`: genera presigned URL de lectura.
11. `OnlyOfficeService -> OnlyOfficeService`: construye JSON oficial de OnlyOffice.
12. `OnlyOfficeService -> DocumentAuditLogRepository`: registra evento `EDIT` por solicitud de configuración.
13. `OnlyOfficeController --> ExecutionService`: retorna `OnlyOfficeConfigDto`.
14. `ExecutionService --> DocumentViewerComponent`: entrega `documentServerUrl` y `config`.
15. `DocumentViewerComponent -> DocumentViewerComponent`: inyecta script `api.js`.
16. `DocumentViewerComponent -> OnlyOffice Document Server`: carga script `/web-apps/apps/api/documents/api.js`.
17. `DocumentViewerComponent -> OnlyOffice Document Server`: `new DocsAPI.DocEditor("onlyoffice-placeholder", config)`.
18. `OnlyOffice Document Server -> AWS S3`: descarga archivo usando `document.url`.
19. `Funcionario autorizado -> OnlyOffice Document Server`: edita documento colaborativamente.
20. `OnlyOffice Document Server -> OnlyOfficeController`: `POST /api/onlyoffice/callback/{documentId}` con `status=2` o `status=6`.
21. `OnlyOfficeController -> OnlyOfficeService`: `handleSaveCallback(documentId, payload)`.
22. `OnlyOfficeService -> OnlyOffice Document Server`: descarga archivo final desde `payload.url`.
23. `OnlyOfficeService -> S3Service`: `replaceObject(record.s3Key, updatedBytes, contentType)`.
24. `S3Service -> AWS S3`: sobrescribe objeto.
25. `OnlyOfficeService -> DocumentRecordRepository`: actualiza `size` y `updatedAt`.
26. `OnlyOfficeService -> DocumentAuditLogRepository`: registra evento `EDIT` con usuario `onlyoffice`.
27. `OnlyOfficeController --> OnlyOffice Document Server`: `{ "error": 0 }`.

## Fragmentos UML 2.5 Recomendados

- `alt [sin permiso de edición]`: la UI muestra mensaje y no llama al backend.
- `alt [formato no soportado]`: Spring responde `400`.
- `alt [OnlyOffice no disponible]`: la UI muestra error de carga del script.
- `alt [callback status != 2 y status != 6]`: Spring ignora el callback.
- `alt [S3/OnlyOffice no entrega archivo final]`: Spring registra warning y no reemplaza el objeto.

## Diagrama de Comunicación - Mensajes Sugeridos

```text
1 Funcionario -> DocumentViewerComponent: iniciarEdicion()
1.1 DocumentViewerComponent -> ExecutionService: getOnlyOfficeConfig(documentId)
1.2 ExecutionService -> OnlyOfficeController: GET /api/onlyoffice/config/{documentId}
1.3 OnlyOfficeController -> OnlyOfficeService: buildEditorConfig(documentId, user)
1.3.1 OnlyOfficeService -> DocumentRecordRepository: findById(documentId)
1.3.2 OnlyOfficeService -> DocumentAccessService: assertCanAccessDocument(EDIT)
1.3.3 OnlyOfficeService -> S3Service: createPresignedReadUrl(s3Key)
1.3.4 S3Service -> AWS S3: presignGetObject()
1.3.5 OnlyOfficeService -> DocumentAuditLogRepository: save(EDIT)
1.4 OnlyOfficeService --> DocumentViewerComponent: OnlyOfficeConfigDto
2 DocumentViewerComponent -> OnlyOffice Document Server: cargar api.js
3 DocumentViewerComponent -> OnlyOffice Document Server: DocEditor(config)
3.1 OnlyOffice Document Server -> AWS S3: GET presigned document.url
4 OnlyOffice Document Server -> OnlyOfficeController: POST callback(status=2|6)
4.1 OnlyOfficeController -> OnlyOfficeService: handleSaveCallback()
4.1.1 OnlyOfficeService -> OnlyOffice Document Server: GET payload.url
4.1.2 OnlyOfficeService -> S3Service: replaceObject()
4.1.3 S3Service -> AWS S3: putObject()
4.1.4 OnlyOfficeService -> DocumentRecordRepository: save(record)
4.1.5 OnlyOfficeService -> DocumentAuditLogRepository: save(EDIT)
```

## Nota de Modelado

En el diagrama de secuencia conviene separar el flujo en dos bloques:

- Apertura del editor.
- Callback de guardado.

Esto evita que el diagrama quede demasiado ancho y permite mostrar claramente que el callback lo inicia `OnlyOffice Document Server`, no el usuario.

---

# CU-07: Solicitar Recomendación de Trámite en Lenguaje Natural

## Objetivo del Diagrama

Representar cómo el cliente móvil escribe o dicta una necesidad, cómo Spring filtra las policies iniciables, cómo FastAPI recomienda candidatos y cómo la app permite elegir uno antes de pasar a requisitos iniciales.

## Código Relacionado

- `mobile/lib/features/dashboard/presentation/dashboard_screen.dart`
- `mobile/lib/features/dashboard/data/dashboard_service.dart`
- `backend/src/main/java/com/politicanegocio/core/controller/IntelligentReceptionController.java`
- `backend/src/main/java/com/politicanegocio/core/service/IntelligentReceptionService.java`
- `backend/src/main/java/com/politicanegocio/core/service/PolicyService.java`
- `bpmn-ai-engine/app/main.py`

## Participantes para Diagrama de Secuencia

- `Cliente móvil <<actor>>`
- `DashboardScreen <<boundary>>`
- `DashboardService <<control>>`
- `IntelligentReceptionController <<boundary>>`
- `IntelligentReceptionService <<control>>`
- `PolicyService <<control>>`
- `PolicyRepository <<entity>>`
- `FastAPI Policy Intent Agent <<external>>`
- `LLM Provider <<external>>`

## Flujo Principal para Diagrama de Secuencia

1. `Cliente móvil -> DashboardScreen`: escribe necesidad y presiona `Buscar trámites`.
2. `DashboardScreen -> DashboardService`: `recommendPoliciesWithAgent(text)`.
3. `DashboardService -> IntelligentReceptionController`: `POST /api/execution/intelligent-reception/recommend`.
4. `IntelligentReceptionController -> IntelligentReceptionService`: `recommendPolicies(request, user)`.
5. `IntelligentReceptionService -> IntelligentReceptionService`: valida usuario autenticado y texto no vacío.
6. `IntelligentReceptionService -> PolicyService`: `getStartablePoliciesForUser(user)`.
7. `PolicyService -> PolicyRepository`: consulta policies iniciables según empresa/carril/usuario.
8. `IntelligentReceptionService -> IntelligentReceptionService`: transforma policies a payload IA con requisitos iniciales.
9. `IntelligentReceptionService -> FastAPI Policy Intent Agent`: `POST /api/v1/agent/policy-intent`.
10. `FastAPI Policy Intent Agent -> LLM Provider`: solicita clasificación de intención contra catálogo.
11. `LLM Provider --> FastAPI Policy Intent Agent`: devuelve candidatos.
12. `FastAPI Policy Intent Agent --> IntelligentReceptionService`: `{ candidates: [...] }`.
13. `IntelligentReceptionService -> IntelligentReceptionService`: valida candidatos contra `policyById`.
14. `IntelligentReceptionService --> IntelligentReceptionController`: `IntelligentReceptionResponseDto`.
15. `IntelligentReceptionController --> DashboardService`: respuesta JSON.
16. `DashboardService --> DashboardScreen`: `IntelligentReceptionResult`.
17. `DashboardScreen -> Cliente móvil`: muestra bottom sheet con candidatos, confianza y requisitos.
18. `Cliente móvil -> DashboardScreen`: selecciona trámite candidato.
19. `DashboardScreen -> StartProcessRequirementsScreen`: navega a pantalla de requisitos iniciales.

## Fragmentos UML 2.5 Recomendados

- `alt [texto vacío]`: Spring responde `400` o UI solicita descripción.
- `alt [no hay policies iniciables]`: Spring responde `404`.
- `alt [IA no disponible]`: `IntelligentReceptionService` usa fallback local.
- `alt [IA devuelve candidatos inválidos]`: se aplica fallback local.
- `opt [cliente selecciona candidato]`: se navega a requisitos iniciales.
- `opt [cliente cierra bottom sheet]`: no se inicia trámite.

## Diagrama de Comunicación - Mensajes Sugeridos

```text
1 Cliente -> DashboardScreen: buscarTramites(texto)
1.1 DashboardScreen -> DashboardService: recommendPoliciesWithAgent(texto)
1.2 DashboardService -> IntelligentReceptionController: POST /recommend
1.3 IntelligentReceptionController -> IntelligentReceptionService: recommendPolicies(request,user)
1.3.1 IntelligentReceptionService -> PolicyService: getStartablePoliciesForUser(user)
1.3.2 PolicyService -> PolicyRepository: find policies iniciables
1.3.3 IntelligentReceptionService -> FastAPI Agent: POST /api/v1/agent/policy-intent
1.3.3.1 FastAPI Agent -> LLM Provider: chat.completions.create()
1.3.4 FastAPI Agent --> IntelligentReceptionService: candidates
1.3.5 IntelligentReceptionService -> IntelligentReceptionService: parseCandidates()
1.4 IntelligentReceptionController --> DashboardService: IntelligentReceptionResponseDto
1.5 DashboardService --> DashboardScreen: IntelligentReceptionResult
2 DashboardScreen -> Cliente: mostrar candidatos
3 Cliente -> DashboardScreen: seleccionar candidato
3.1 DashboardScreen -> StartProcessRequirementsScreen: navegar(policy)
```

## Nota de Modelado

No modeles el inicio del trámite dentro de este CU si el objetivo es recomendación. El resultado correcto es elegir un candidato y pasar al CU de requisitos iniciales.

---

# CU-08: Consultar Análisis Predictivo Operativo

## Objetivo del Diagrama

Representar cómo el administrador de empresa consulta el dashboard predictivo, cómo Spring extrae event logs filtrados por empresa y cómo FastAPI calcula anomalías, prioridades, rutas probables y cuellos de botella.

## Código Relacionado

- `frontend/src/app/features/predictive-analytics/components/predictive-analytics/predictive-analytics.component.ts`
- `frontend/src/app/features/predictive-analytics/services/predictive-analytics.service.ts`
- `backend/src/main/java/com/politicanegocio/core/controller/PredictiveAnalyticsController.java`
- `backend/src/main/java/com/politicanegocio/core/service/PredictiveAnalyticsService.java`
- `backend/src/main/java/com/politicanegocio/core/repository/PolicyRepository.java`
- `backend/src/main/java/com/politicanegocio/core/repository/ProcessInstanceRepository.java`
- `backend/src/main/java/com/politicanegocio/core/repository/TaskInstanceRepository.java`
- `backend/src/main/java/com/politicanegocio/core/repository/DocumentRecordRepository.java`
- `bpmn-ai-engine/app/main.py`

## Participantes para Diagrama de Secuencia

- `Administrador de Empresa <<actor>>`
- `PredictiveAnalyticsComponent <<boundary>>`
- `PredictiveAnalyticsService Angular <<control>>`
- `PredictiveAnalyticsController <<boundary>>`
- `PredictiveAnalyticsService Spring <<control>>`
- `PolicyRepository <<entity>>`
- `ProcessInstanceRepository <<entity>>`
- `TaskInstanceRepository <<entity>>`
- `DocumentRecordRepository <<entity>>`
- `FastAPI Prediction Engine <<external>>`
- `TensorFlow/Keras <<external>>`

## Flujo Principal para Diagrama de Secuencia

1. `Administrador de Empresa -> PredictiveAnalyticsComponent`: entra a `/admin/predictions`.
2. `PredictiveAnalyticsComponent -> PredictiveAnalyticsService Angular`: `getCompanyAnalysis()`.
3. `PredictiveAnalyticsService Angular -> PredictiveAnalyticsController`: `GET /api/predictions/analysis`.
4. `PredictiveAnalyticsController -> PredictiveAnalyticsService Spring`: `analyze(user)`.
5. `PredictiveAnalyticsService Spring -> PredictiveAnalyticsService Spring`: valida `COMPANY_ADMIN`.
6. `PredictiveAnalyticsService Spring -> PolicyRepository`: `findByCompanyId(user.company)`.
7. `PredictiveAnalyticsService Spring -> ProcessInstanceRepository`: `findAll()` y filtra por `policyIds`.
8. `PredictiveAnalyticsService Spring -> TaskInstanceRepository`: `findByProcessInstanceIdIn(processIds)`.
9. `PredictiveAnalyticsService Spring -> DocumentRecordRepository`: `findByPolicyIdInOrderByCreatedAtDesc(policyIds)`.
10. `PredictiveAnalyticsService Spring -> PredictiveAnalyticsService Spring`: construye payload con `processes`, `tasks`, `documents`.
11. `PredictiveAnalyticsService Spring -> FastAPI Prediction Engine`: `POST /api/v1/prediction/analyze`.
12. `FastAPI Prediction Engine -> TensorFlow/Keras`: intenta autoencoder si hay datos suficientes y TensorFlow está disponible.
13. `FastAPI Prediction Engine -> FastAPI Prediction Engine`: calcula anomalías, prioridades, rutas y cuellos de botella.
14. `FastAPI Prediction Engine --> PredictiveAnalyticsService Spring`: devuelve `PredictiveAnalysisResponse`.
15. `PredictiveAnalyticsService Spring -> PredictiveAnalyticsService Spring`: parsea y enriquece nombres legibles.
16. `PredictiveAnalyticsController --> PredictiveAnalyticsService Angular`: retorna `PredictiveAnalysisResponseDto`.
17. `PredictiveAnalyticsService Angular --> PredictiveAnalyticsComponent`: entrega modelo.
18. `PredictiveAnalyticsComponent -> Administrador de Empresa`: muestra tarjetas predictivas.

## Fragmentos UML 2.5 Recomendados

- `alt [usuario no autenticado]`: respuesta `401`.
- `alt [usuario no es COMPANY_ADMIN]`: respuesta `403`.
- `alt [sin policies de empresa]`: respuesta vacía con warning.
- `alt [TensorFlow disponible y >= 8 tareas]`: usa autoencoder.
- `alt [TensorFlow no disponible o pocos datos]`: usa fallback heurístico.
- `alt [FastAPI no disponible]`: Spring devuelve `503`.
- `alt [FastAPI responde inválido]`: Spring devuelve `502`.

## Diagrama de Comunicación - Mensajes Sugeridos

```text
1 AdminEmpresa -> PredictiveAnalyticsComponent: abrirPredicciones()
1.1 PredictiveAnalyticsComponent -> PredictiveAnalyticsServiceAngular: getCompanyAnalysis()
1.2 PredictiveAnalyticsServiceAngular -> PredictiveAnalyticsController: GET /api/predictions/analysis
1.3 PredictiveAnalyticsController -> PredictiveAnalyticsServiceSpring: analyze(user)
1.3.1 PredictiveAnalyticsServiceSpring -> PolicyRepository: findByCompanyId(company)
1.3.2 PredictiveAnalyticsServiceSpring -> ProcessInstanceRepository: findAll()
1.3.3 PredictiveAnalyticsServiceSpring -> TaskInstanceRepository: findByProcessInstanceIdIn(ids)
1.3.4 PredictiveAnalyticsServiceSpring -> DocumentRecordRepository: findByPolicyIdIn(...)
1.3.5 PredictiveAnalyticsServiceSpring -> FastAPI Prediction Engine: POST /prediction/analyze
1.3.5.1 FastAPI Prediction Engine -> TensorFlow/Keras: autoencoder()
1.3.5.2 FastAPI Prediction Engine -> FastAPI Prediction Engine: calcular prioridades/rutas/cuellos
1.3.6 FastAPI Prediction Engine --> PredictiveAnalyticsServiceSpring: response
1.3.7 PredictiveAnalyticsServiceSpring -> PredictiveAnalyticsServiceSpring: enrichResponse()
1.4 PredictiveAnalyticsController --> PredictiveAnalyticsComponent: predictive DTO
2 PredictiveAnalyticsComponent -> AdminEmpresa: mostrar dashboard
```

## Nota de Modelado

El diagrama debe remarcar el aislamiento por empresa. La consulta a `PolicyRepository.findByCompanyId(user.company)` es el punto clave de seguridad y debe aparecer antes de extraer procesos, tareas y documentos.

---

# CU-09: Generar Reporte Dinámico

## Objetivo del Diagrama

Representar cómo el administrador solicita un reporte en lenguaje natural, cómo el agente valida datos, criterios y formato, y cómo Spring genera un archivo PDF, Excel o CSV.

## Código Relacionado

- `frontend/src/app/features/predictive-analytics/components/predictive-analytics/predictive-analytics.component.ts`
- `frontend/src/app/features/predictive-analytics/services/predictive-analytics.service.ts`
- `backend/src/main/java/com/politicanegocio/core/controller/DynamicReportController.java`
- `backend/src/main/java/com/politicanegocio/core/service/DynamicReportService.java`
- `backend/src/main/java/com/politicanegocio/core/dto/DynamicReportPlanDto.java`
- `backend/src/main/java/com/politicanegocio/core/dto/GeneratedReportFileDto.java`
- `bpmn-ai-engine/app/main.py`

## Participantes para Diagrama de Secuencia

- `Administrador de Empresa <<actor>>`
- `PredictiveAnalyticsComponent <<boundary>>`
- `PredictiveAnalyticsService Angular <<control>>`
- `DynamicReportController <<boundary>>`
- `DynamicReportService <<control>>`
- `PolicyRepository <<entity>>`
- `ProcessInstanceRepository <<entity>>`
- `TaskInstanceRepository <<entity>>`
- `DocumentRecordRepository <<entity>>`
- `FastAPI Analytics Agent <<external>>`
- `Report Renderer Spring <<control>>`

## Flujo Principal para Diagrama de Secuencia

El CU tiene dos fases: validar la solicitud y descargar el archivo.

### Fase A: Validar Solicitud

1. `Administrador de Empresa -> PredictiveAnalyticsComponent`: escribe solicitud de reporte.
2. `Administrador de Empresa -> PredictiveAnalyticsComponent`: presiona `Validar solicitud`.
3. `PredictiveAnalyticsComponent -> PredictiveAnalyticsService Angular`: `planDynamicReport(prompt)`.
4. `PredictiveAnalyticsService Angular -> DynamicReportController`: `POST /api/reports/dynamic/plan`.
5. `DynamicReportController -> DynamicReportService`: `plan(user, request)`.
6. `DynamicReportService -> DynamicReportService`: valida `COMPANY_ADMIN` y prompt no vacío.
7. `DynamicReportService -> PolicyRepository`: `findByCompanyId(user.company)`.
8. `DynamicReportService -> ProcessInstanceRepository`: `findAll()` y filtra por policies.
9. `DynamicReportService -> TaskInstanceRepository`: `findByProcessInstanceIdIn(processIds)`.
10. `DynamicReportService -> DocumentRecordRepository`: `findByPolicyIdInOrderByCreatedAtDesc(policyIds)`.
11. `DynamicReportService -> FastAPI Analytics Agent`: `POST /api/v1/analytics/report-plan`.
12. `FastAPI Analytics Agent -> FastAPI Analytics Agent`: detecta datos, criterios y formato.
13. `FastAPI Analytics Agent --> DynamicReportService`: devuelve `DynamicReportPlanResponse`.
14. `DynamicReportService -> DynamicReportService`: parsea y enriquece filas con nombres legibles.
15. `DynamicReportController --> PredictiveAnalyticsService Angular`: retorna `DynamicReportPlanDto`.
16. `PredictiveAnalyticsService Angular --> PredictiveAnalyticsComponent`: entrega plan.
17. `PredictiveAnalyticsComponent -> Administrador de Empresa`: muestra plan o pregunta de aclaración.

### Fase B: Descargar Reporte

1. `Administrador de Empresa -> PredictiveAnalyticsComponent`: presiona `Descargar reporte`.
2. `PredictiveAnalyticsComponent -> PredictiveAnalyticsService Angular`: `downloadDynamicReport(prompt)`.
3. `PredictiveAnalyticsService Angular -> DynamicReportController`: `POST /api/reports/dynamic/download`.
4. `DynamicReportController -> DynamicReportService`: `generate(user, request)`.
5. `DynamicReportService -> DynamicReportService`: vuelve a ejecutar `plan(user, request)` para asegurar consistencia.
6. `DynamicReportService -> Report Renderer Spring`: renderiza según `format`.
7. `Report Renderer Spring --> DynamicReportService`: `GeneratedReportFileDto`.
8. `DynamicReportController --> PredictiveAnalyticsService Angular`: retorna bytes con `Content-Disposition`.
9. `PredictiveAnalyticsService Angular -> Browser`: crea `Blob`, URL temporal y dispara descarga.
10. `Browser -> Administrador de Empresa`: descarga PDF/Excel/CSV.

## Fragmentos UML 2.5 Recomendados

- `alt [prompt vacío]`: Spring devuelve plan incompleto.
- `alt [falta datos]`: FastAPI devuelve `complete=false` y pregunta.
- `alt [falta criterios]`: FastAPI devuelve `complete=false` y pregunta.
- `alt [falta formato]`: FastAPI devuelve `complete=false` y pregunta.
- `alt [complete=true]`: se habilita botón de descarga.
- `alt [formato=pdf]`: Spring ejecuta `renderPdf(plan)`.
- `alt [formato=excel]`: Spring ejecuta `renderExcel(plan)`.
- `alt [formato=csv]`: Spring ejecuta `renderCsv(plan)`.
- `break [complete=false en download]`: Spring responde `422`.

## Diagrama de Comunicación - Mensajes Sugeridos

```text
1 AdminEmpresa -> PredictiveAnalyticsComponent: validarSolicitud(prompt)
1.1 PredictiveAnalyticsComponent -> PredictiveAnalyticsServiceAngular: planDynamicReport(prompt)
1.2 PredictiveAnalyticsServiceAngular -> DynamicReportController: POST /reports/dynamic/plan
1.3 DynamicReportController -> DynamicReportService: plan(user,request)
1.3.1 DynamicReportService -> PolicyRepository: findByCompanyId(company)
1.3.2 DynamicReportService -> ProcessInstanceRepository: findAll()
1.3.3 DynamicReportService -> TaskInstanceRepository: findByProcessInstanceIdIn(ids)
1.3.4 DynamicReportService -> DocumentRecordRepository: findByPolicyIdIn(...)
1.3.5 DynamicReportService -> FastAPI Analytics Agent: POST /analytics/report-plan
1.3.5.1 FastAPI Analytics Agent -> FastAPI Analytics Agent: detectar datos/criterios/formato
1.3.5.2 FastAPI Analytics Agent -> FastAPI Analytics Agent: construir rows
1.3.6 FastAPI Analytics Agent --> DynamicReportService: DynamicReportPlanResponse
1.3.7 DynamicReportService -> DynamicReportService: enrichPlan()
1.4 DynamicReportController --> PredictiveAnalyticsComponent: DynamicReportPlanDto
2 AdminEmpresa -> PredictiveAnalyticsComponent: descargarReporte()
2.1 PredictiveAnalyticsComponent -> PredictiveAnalyticsServiceAngular: downloadDynamicReport(prompt)
2.2 PredictiveAnalyticsServiceAngular -> DynamicReportController: POST /reports/dynamic/download
2.3 DynamicReportController -> DynamicReportService: generate(user,request)
2.3.1 DynamicReportService -> DynamicReportService: plan(user,request)
2.3.2 DynamicReportService -> Report Renderer Spring: renderPdf/renderExcel/renderCsv
2.4 DynamicReportController --> PredictiveAnalyticsServiceAngular: bytes + headers
2.5 PredictiveAnalyticsServiceAngular -> Browser: descargar Blob
```

## Nota de Modelado

Este CU conviene dibujarlo con dos diagramas de secuencia separados:

- `CU-09A: Validar solicitud de reporte`
- `CU-09B: Descargar reporte`

En el diagrama de comunicación sí pueden convivir ambos bloques usando numeración `1.x` para validación y `2.x` para descarga.

---

# Resumen de Complejidad y Por Qué Merecen Diagramas

| CU | Motivo de complejidad | Diagramas recomendados |
|---|---|---|
| `CU-05` | Integra Angular, Spring Boot, permisos, S3, OnlyOffice y callback asíncrono. | Secuencia y comunicación |
| `CU-07` | Usa móvil, backend, catálogo filtrado, FastAPI, LLM y fallback local. | Secuencia y comunicación |
| `CU-08` | Extrae event log por empresa, llama motor IA, usa TensorFlow/fallback y renderiza dashboard. | Secuencia y comunicación |
| `CU-09` | Tiene validación de lenguaje natural, generación de plan, renderizado de archivos y descarga. | Secuencia y comunicación |

# Recomendación Final de Entrega

Si tu documentación requiere priorizar, dibuja:

1. `CU-05` con dos secuencias: apertura del editor y callback de guardado.
2. `CU-08` con un único diagrama de secuencia completo.
3. `CU-09` separado en validación y descarga.
4. `CU-07` como comunicación para mostrar colaboración móvil-backend-IA.

Esto cubre integración externa, IA, análisis predictivo, seguridad y generación documental/reportes.
