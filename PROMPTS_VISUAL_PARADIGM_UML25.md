# Prompts para Visual Paradigm AI - UML 2.5

Regla de uso:

- **Diagramas de secuencia:** deben respetar el código, clases y nombres de métodos reales.
- **Diagramas de comunicación:** son de análisis, por eso usan componentes conceptuales en español y mensajes de negocio.

---

## CU-05: Editar Documentos - Diagrama de Secuencia

```text
Crea un diagrama de secuencia UML 2.5 para el caso de uso "Editar Documentos".

Usa nombres de clases y métodos reales del código.

Participantes:
- Funcionario autorizado <<actor>>
- DocumentViewerComponent <<boundary>>
- ExecutionService <<control>>
- OnlyOfficeController <<boundary>>
- OnlyOfficeService <<control>>
- DocumentAccessService <<control>>
- DocumentRecordRepository <<entity>>
- S3Service <<control>>
- AWS S3 <<external>>
- OnlyOffice Document Server <<external>>
- DocumentAuditLogRepository <<entity>>

Flujo principal:
1. Funcionario autorizado -> DocumentViewerComponent: openOnlyOfficeEditor()
2. DocumentViewerComponent -> DocumentViewerComponent: validar document.canEdit
3. DocumentViewerComponent -> ExecutionService: getOnlyOfficeConfig(document.id)
4. ExecutionService -> OnlyOfficeController: getEditorConfig(documentId, authentication)
5. OnlyOfficeController -> OnlyOfficeService: buildEditorConfig(documentId, user)
6. OnlyOfficeService -> DocumentRecordRepository: findById(documentId)
7. OnlyOfficeService -> DocumentAccessService: assertCanAccessDocument(user, record, EDIT)
8. OnlyOfficeService -> OnlyOfficeService: extension(record.fileName)
9. OnlyOfficeService -> S3Service: createPresignedReadUrl(record.s3Key, presignedHours)
10. S3Service -> AWS S3: presignGetObject()
11. OnlyOfficeService -> OnlyOfficeService: buildDocumentKey(record)
12. OnlyOfficeService -> DocumentAuditLogRepository: save(auditLog)
13. OnlyOfficeService --> OnlyOfficeController: OnlyOfficeConfigDto
14. OnlyOfficeController --> ExecutionService: OnlyOfficeConfigDto
15. ExecutionService --> DocumentViewerComponent: OnlyOfficeConfigDto
16. DocumentViewerComponent -> DocumentViewerComponent: ensureOnlyOfficeScript(editorConfig)
17. DocumentViewerComponent -> OnlyOffice Document Server: new DocsAPI.DocEditor("onlyoffice-placeholder", config)
18. OnlyOffice Document Server -> AWS S3: descargar document.url
19. Funcionario autorizado -> OnlyOffice Document Server: editar documento colaborativamente
20. OnlyOffice Document Server -> OnlyOfficeController: callback(documentId, rawPayload)
21. OnlyOfficeController -> OnlyOfficeService: handleSaveCallback(documentId, payload)
22. OnlyOfficeService -> OnlyOfficeService: validar status 2 o 6
23. OnlyOfficeService -> OnlyOffice Document Server: descargar payload.url
24. OnlyOfficeService -> S3Service: replaceObject(record.s3Key, updatedBytes, record.contentType)
25. S3Service -> AWS S3: putObject()
26. OnlyOfficeService -> DocumentRecordRepository: save(record)
27. OnlyOfficeService -> DocumentAuditLogRepository: save(auditLog)
28. OnlyOfficeController --> OnlyOffice Document Server: responder error=0

Fragmentos importantes:
- alt [sin permiso de edición]: DocumentViewerComponent muestra mensaje y termina.
- alt [callback no corresponde a guardado]: OnlyOfficeService ignora el callback.
```

## CU-05: Editar Documentos - Diagrama de Comunicación

```text
Crea un diagrama de comunicación UML 2.5 en nivel de ANÁLISIS para el caso de uso "Editar Documentos".

Usa componentes conceptuales en español. No uses nombres de clases, métodos ni endpoints.

Componentes:
- Funcionario autorizado
- Interfaz de documentos
- Sistema de gestión documental
- Control de acceso
- Servicio de edición colaborativa
- Almacenamiento documental
- Registro de auditoría

Mensajes numerados:
1 Funcionario autorizado -> Interfaz de documentos: solicita editar documento
1.1 Interfaz de documentos -> Sistema de gestión documental: solicita preparar edición
1.2 Sistema de gestión documental -> Control de acceso: verifica permiso de edición
1.3 Sistema de gestión documental -> Almacenamiento documental: obtiene acceso seguro al documento
1.4 Sistema de gestión documental -> Servicio de edición colaborativa: habilita sesión de edición
2 Servicio de edición colaborativa -> Almacenamiento documental: abre documento original
3 Funcionario autorizado -> Servicio de edición colaborativa: modifica documento
4 Servicio de edición colaborativa -> Sistema de gestión documental: informa cambios guardables
4.1 Sistema de gestión documental -> Almacenamiento documental: guarda nueva versión
4.2 Sistema de gestión documental -> Registro de auditoría: registra edición
5 Sistema de gestión documental -> Interfaz de documentos: confirma edición guardada

Fragmentos importantes:
- alt [sin permiso]: se rechaza la edición.
- alt [fallo de guardado]: se informa que no se pudo guardar.
```

---

## CU-07: Recomendar Trámite en Lenguaje Natural - Diagrama de Secuencia

```text
Crea un diagrama de secuencia UML 2.5 para el caso de uso "Recomendar Trámite en Lenguaje Natural".

Usa nombres de clases y métodos reales del código.

Participantes:
- Cliente móvil <<actor>>
- DashboardScreen <<boundary>>
- DashboardService <<control>>
- IntelligentReceptionController <<boundary>>
- IntelligentReceptionService <<control>>
- PolicyService <<control>>
- PolicyRepository <<entity>>
- FastAPI Policy Intent Agent <<external>>
- LLM Provider <<external>>
- StartProcessRequirementsScreen <<boundary>>

Flujo principal:
1. Cliente móvil -> DashboardScreen: _startWithAgent(policies)
2. DashboardScreen -> DashboardService: recommendPoliciesWithAgent(text)
3. DashboardService -> IntelligentReceptionController: startFromNaturalLanguage(request, authentication)
4. IntelligentReceptionController -> IntelligentReceptionService: recommendPolicies(request, user)
5. IntelligentReceptionService -> IntelligentReceptionService: validar usuario y texto
6. IntelligentReceptionService -> PolicyService: getStartablePoliciesForUser(user)
7. PolicyService -> PolicyRepository: consultar policies iniciables
8. IntelligentReceptionService -> IntelligentReceptionService: toAiPolicy(policy)
9. IntelligentReceptionService -> IntelligentReceptionService: toAiRequirement(requirement)
10. IntelligentReceptionService -> FastAPI Policy Intent Agent: classify_policy_intent(payload)
11. FastAPI Policy Intent Agent -> LLM Provider: chat.completions.create(...)
12. LLM Provider --> FastAPI Policy Intent Agent: candidates
13. FastAPI Policy Intent Agent --> IntelligentReceptionService: PolicyIntentResponse
14. IntelligentReceptionService -> IntelligentReceptionService: parseCandidates(result, policyById)
15. IntelligentReceptionService -> IntelligentReceptionService: requiredRequirementNames(policy)
16. IntelligentReceptionService --> IntelligentReceptionController: IntelligentReceptionResponseDto
17. IntelligentReceptionController --> DashboardService: IntelligentReceptionResponseDto
18. DashboardService --> DashboardScreen: IntelligentReceptionResult
19. DashboardScreen -> DashboardScreen: _showAgentCandidates(candidates, policies)
20. Cliente móvil -> DashboardScreen: selecciona trámite candidato
21. DashboardScreen -> StartProcessRequirementsScreen: _startPolicy(selectedPolicy)

Fragmentos importantes:
- alt [texto vacío]: DashboardScreen solicita descripción.
- alt [IA no disponible o respuesta inválida]: IntelligentReceptionService usa localRecommendations(text, policies, reason).
- opt [cliente selecciona candidato]: navega a StartProcessRequirementsScreen.
```

## CU-07: Recomendar Trámite en Lenguaje Natural - Diagrama de Comunicación

```text
Crea un diagrama de comunicación UML 2.5 en nivel de ANÁLISIS para el caso de uso "Recomendar Trámite en Lenguaje Natural".

Usa componentes conceptuales en español. No uses clases, métodos ni endpoints.

Componentes:
- Cliente móvil
- Aplicación móvil
- Sistema iBPMS
- Catálogo de trámites
- Agente inteligente de recepción
- Motor de lenguaje natural
- Pantalla de requisitos iniciales

Mensajes numerados:
1 Cliente móvil -> Aplicación móvil: describe necesidad
1.1 Aplicación móvil -> Sistema iBPMS: solicita recomendación
1.2 Sistema iBPMS -> Catálogo de trámites: obtiene trámites iniciables
1.3 Sistema iBPMS -> Agente inteligente de recepción: envía necesidad y catálogo permitido
1.4 Agente inteligente de recepción -> Motor de lenguaje natural: interpreta intención
1.5 Motor de lenguaje natural -> Agente inteligente de recepción: devuelve candidatos
1.6 Agente inteligente de recepción -> Sistema iBPMS: entrega candidatos y requisitos
1.7 Sistema iBPMS -> Aplicación móvil: muestra trámites recomendados
2 Cliente móvil -> Aplicación móvil: selecciona trámite
2.1 Aplicación móvil -> Pantalla de requisitos iniciales: abre requisitos del trámite elegido

Fragmentos importantes:
- alt [necesidad ambigua]: pedir descripción más clara.
- alt [motor IA no disponible]: usar recomendación local.
```

---

## CU-08: Consultar Análisis Predictivo Operativo - Diagrama de Secuencia

```text
Crea un diagrama de secuencia UML 2.5 para el caso de uso "Consultar Análisis Predictivo Operativo".

Usa nombres de clases y métodos reales del código.

Participantes:
- Administrador de Empresa <<actor>>
- PredictiveAnalyticsComponent <<boundary>>
- PredictiveAnalyticsService Angular <<control>>
- PredictiveAnalyticsController <<boundary>>
- PredictiveAnalyticsService Spring <<control>>
- Repositorios de datos <<entity>>
- FastAPI Prediction Engine <<external>>
- TensorFlow/Keras <<external>>

Flujo principal simplificado:
1. Administrador de Empresa -> PredictiveAnalyticsComponent: loadAnalysis()
2. PredictiveAnalyticsComponent -> PredictiveAnalyticsService Angular: getCompanyAnalysis()
3. PredictiveAnalyticsService Angular -> PredictiveAnalyticsController: analysis(authentication)
4. PredictiveAnalyticsController -> PredictiveAnalyticsService Spring: analyze(user)
5. PredictiveAnalyticsService Spring -> PredictiveAnalyticsService Spring: resolvePolicies(user)
6. PredictiveAnalyticsService Spring -> Repositorios de datos: consultar policies, procesos, tareas y documentos de la empresa
7. PredictiveAnalyticsService Spring -> PredictiveAnalyticsService Spring: construir event log con toProcessEvent(), toTaskEvent(), toDocumentEvent()
8. PredictiveAnalyticsService Spring -> FastAPI Prediction Engine: predictive_analysis(payload)
9. FastAPI Prediction Engine -> TensorFlow/Keras: _autoencoder_anomalies(tasks)
10. FastAPI Prediction Engine -> FastAPI Prediction Engine: _predict_priorities(), _predict_routes(), _predict_bottlenecks()
11. FastAPI Prediction Engine --> PredictiveAnalyticsService Spring: PredictiveAnalysisResponse
12. PredictiveAnalyticsService Spring -> PredictiveAnalyticsService Spring: parseResponse() y enrichResponse()
13. PredictiveAnalyticsController --> PredictiveAnalyticsService Angular: PredictiveAnalysisResponseDto
14. PredictiveAnalyticsService Angular --> PredictiveAnalyticsComponent: PredictiveAnalysis
15. PredictiveAnalyticsComponent -> Administrador de Empresa: mostrar dashboard predictivo

Fragmentos importantes:
- alt [usuario no es COMPANY_ADMIN]: rechazar acceso.
- alt [sin datos suficientes]: mostrar advertencia.
- alt [TensorFlow no disponible]: usar fallback heurístico.
```

## CU-08: Consultar Análisis Predictivo Operativo - Diagrama de Comunicación

```text
Crea un diagrama de comunicación UML 2.5 en nivel de ANÁLISIS para el caso de uso "Consultar Análisis Predictivo Operativo".

Usa componentes conceptuales en español. No uses clases, métodos ni endpoints.

Componentes:
- Administrador de Empresa
- Panel predictivo
- Sistema iBPMS
- Historial operativo de la empresa
- Motor predictivo
- Modelo de aprendizaje automático
- Resultados predictivos

Mensajes numerados:
1 Administrador de Empresa -> Panel predictivo: solicita análisis operativo
1.1 Panel predictivo -> Sistema iBPMS: pide predicciones de la empresa
1.2 Sistema iBPMS -> Historial operativo de la empresa: extrae procesos, tareas y documentos
1.3 Sistema iBPMS -> Motor predictivo: envía historial filtrado
1.4 Motor predictivo -> Modelo de aprendizaje automático: analiza patrones y anomalías
1.5 Modelo de aprendizaje automático -> Motor predictivo: devuelve señales predictivas
1.6 Motor predictivo -> Sistema iBPMS: entrega anomalías, prioridades, rutas y cuellos de botella
1.7 Sistema iBPMS -> Resultados predictivos: organiza la información
1.8 Sistema iBPMS -> Panel predictivo: muestra resultados consolidados
2 Panel predictivo -> Administrador de Empresa: presenta riesgos operativos

Fragmentos importantes:
- alt [sin histórico suficiente]: mostrar advertencia.
- alt [modelo avanzado no disponible]: usar cálculo heurístico.
```

---

## CU-09: Generar Reporte Dinámico - Diagrama de Secuencia

```text
Crea un diagrama de secuencia UML 2.5 para el caso de uso "Generar Reporte Dinámico".

Usa nombres de clases y métodos reales del código.

Participantes:
- Administrador de Empresa <<actor>>
- PredictiveAnalyticsComponent <<boundary>>
- PredictiveAnalyticsService Angular <<control>>
- DynamicReportController <<boundary>>
- DynamicReportService <<control>>
- PolicyRepository <<entity>>
- ProcessInstanceRepository <<entity>>
- TaskInstanceRepository <<entity>>
- DocumentRecordRepository <<entity>>
- FastAPI Analytics Agent <<external>>
- Report Renderer Spring <<control>>
- Browser <<boundary>>

Flujo principal con métodos reales:
1. Administrador de Empresa -> PredictiveAnalyticsComponent: prepareReport()
2. PredictiveAnalyticsComponent -> PredictiveAnalyticsService Angular: planDynamicReport(reportPrompt)
3. PredictiveAnalyticsService Angular -> DynamicReportController: plan(request, authentication)
4. DynamicReportController -> DynamicReportService: plan(user, request)
5. DynamicReportService -> DynamicReportService: validateUser(user)
6. DynamicReportService -> PolicyRepository: findByCompanyId(user.company)
7. DynamicReportService -> ProcessInstanceRepository: findAll()
8. DynamicReportService -> TaskInstanceRepository: findByProcessInstanceIdIn(processIds)
9. DynamicReportService -> DocumentRecordRepository: findByPolicyIdInOrderByCreatedAtDesc(policyIds)
10. DynamicReportService -> DynamicReportService: toProcessEvent(), toTaskEvent(), toDocumentEvent()
11. DynamicReportService -> FastAPI Analytics Agent: dynamic_report_plan(payload)
12. FastAPI Analytics Agent -> FastAPI Analytics Agent: _detect_report_scope(), _detect_report_criteria(), _detect_report_format()
13. FastAPI Analytics Agent -> FastAPI Analytics Agent: _missing_report_fields(), _build_report_rows()
14. FastAPI Analytics Agent --> DynamicReportService: DynamicReportPlanResponse
15. DynamicReportService -> DynamicReportService: parsePlan(), enrichPlan()
16. DynamicReportController --> PredictiveAnalyticsService Angular: DynamicReportPlanDto
17. PredictiveAnalyticsService Angular --> PredictiveAnalyticsComponent: DynamicReportPlan
18. PredictiveAnalyticsComponent -> Administrador de Empresa: mostrar plan o pregunta de aclaración
19. Administrador de Empresa -> PredictiveAnalyticsComponent: downloadReport()
20. PredictiveAnalyticsComponent -> PredictiveAnalyticsService Angular: downloadDynamicReport(reportPrompt)
21. PredictiveAnalyticsService Angular -> DynamicReportController: download(request, authentication)
22. DynamicReportController -> DynamicReportService: generate(user, request)
23. DynamicReportService -> DynamicReportService: ref plan(user, request)
24. DynamicReportService -> Report Renderer Spring: renderPdf(plan) o renderExcel(plan) o renderCsv(plan)
25. Report Renderer Spring --> DynamicReportService: byte[]
26. DynamicReportService --> DynamicReportController: GeneratedReportFileDto
27. DynamicReportController --> PredictiveAnalyticsService Angular: byte[]
28. PredictiveAnalyticsService Angular -> PredictiveAnalyticsService Angular: fileNameFrom(contentDisposition)
29. PredictiveAnalyticsService Angular -> Browser: createObjectURL(blob)
30. Browser -> Administrador de Empresa: descargar archivo

Fragmentos importantes:
- alt [falta datos, criterios o formato]: mostrar pregunta y no descargar.
- alt [solicitud completa]: habilitar descarga y generar archivo.
- alt [PDF, Excel o CSV]: renderizar formato seleccionado.
```

## CU-09: Generar Reporte Dinámico - Diagrama de Comunicación

```text
Crea un diagrama de comunicación UML 2.5 en nivel de ANÁLISIS para el caso de uso "Generar Reporte Dinámico".

Usa componentes conceptuales en español. No uses clases, métodos ni endpoints.

Componentes:
- Administrador de Empresa
- Panel de reportes
- Sistema iBPMS
- Historial operativo de la empresa
- Agente analítico
- Generador de reportes
- Archivo de reporte

Mensajes numerados:
1 Administrador de Empresa -> Panel de reportes: solicita reporte en lenguaje natural
1.1 Panel de reportes -> Sistema iBPMS: envía solicitud textual
1.2 Sistema iBPMS -> Historial operativo de la empresa: obtiene datos permitidos
1.3 Sistema iBPMS -> Agente analítico: envía solicitud y datos disponibles
1.4 Agente analítico -> Agente analítico: identifica datos, criterios y formato
1.5 Agente analítico -> Sistema iBPMS: devuelve plan del reporte
1.6 Sistema iBPMS -> Panel de reportes: muestra plan o pregunta aclaratoria
2 Administrador de Empresa -> Panel de reportes: confirma descarga
2.1 Panel de reportes -> Sistema iBPMS: solicita generación del archivo
2.2 Sistema iBPMS -> Generador de reportes: renderiza reporte solicitado
2.3 Generador de reportes -> Archivo de reporte: crea PDF, Excel o CSV
2.4 Sistema iBPMS -> Panel de reportes: entrega archivo generado
2.5 Panel de reportes -> Administrador de Empresa: descarga reporte

Fragmentos importantes:
- alt [faltan datos, criterios o formato]: el agente solicita aclaración.
- alt [solicitud completa]: el sistema genera el archivo.
```
