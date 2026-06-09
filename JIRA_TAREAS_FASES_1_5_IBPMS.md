# Plantilla Jira - Tareas Implementadas en Fases 1 a 5 iBPMS

## Objetivo

Este documento divide el trabajo realizado en las fases 1 a 5 en épicas, historias y tareas técnicas para registrarlas en una herramienta como Jira.

Formato sugerido por ticket:

- **Tipo:** Épica, Historia, Tarea técnica, Subtarea.
- **Prioridad:** Alta, Media, Baja.
- **Componentes:** Backend, Frontend, Mobile, IA, Infraestructura, Documentación.
- **Estado sugerido:** Done, si se registra como trabajo ya realizado.
- **Criterios de aceptación:** condiciones verificables para cerrar el ticket.

---

# EPIC-01: Fase 1 - Reestructuración Core y Almacenamiento

## IBPMS-F1-01: Modelar requisitos iniciales en políticas BPMN

| Campo | Valor |
|---|---|
| **Tipo** | Historia |
| **Prioridad** | Alta |
| **Componentes** | Backend, Base de Datos |
| **Descripción** | Agregar soporte para requisitos iniciales obligatorios u opcionales dentro de las políticas BPMN. |
| **Alcance** | Crear `PolicyInitialRequirement` y agregar `initialRequirements` en `Policy`. |
| **Criterios de aceptación** | La entidad `Policy` permite almacenar requisitos iniciales. Cada requisito contiene nombre, descripción, obligatoriedad y extensiones permitidas. |
| **Estado sugerido** | Done |

## IBPMS-F1-02: Actualizar GraphQL y DTOs de políticas iniciables

| Campo | Valor |
|---|---|
| **Tipo** | Tarea técnica |
| **Prioridad** | Alta |
| **Componentes** | Backend, GraphQL |
| **Descripción** | Exponer los requisitos iniciales en las consultas y respuestas usadas por el modelador y por la ejecución de procesos. |
| **Alcance** | Actualizar `schema.graphqls`, DTOs como `StartablePolicyDto` y controladores/servicios relacionados. |
| **Criterios de aceptación** | Las políticas iniciables devuelven la lista de requisitos iniciales. El frontend y móvil pueden consumirlos sin lógica adicional. |
| **Estado sugerido** | Done |

## IBPMS-F1-03: Adaptar modelador Angular para definir requisitos iniciales

| Campo | Valor |
|---|---|
| **Tipo** | Historia |
| **Prioridad** | Alta |
| **Componentes** | Frontend |
| **Descripción** | Permitir que el administrador defina requisitos iniciales al crear o editar una política BPMN. |
| **Alcance** | Actualizar modelos, servicio de políticas, componente y template del diseñador. |
| **Criterios de aceptación** | El administrador puede agregar, editar y guardar requisitos iniciales desde el modelador. Los datos persisten al recargar la política. |
| **Estado sugerido** | Done |

## IBPMS-F1-04: Refactorizar almacenamiento S3 con estructura jerárquica

| Campo | Valor |
|---|---|
| **Tipo** | Tarea técnica |
| **Prioridad** | Alta |
| **Componentes** | Backend, AWS S3 |
| **Descripción** | Cambiar el almacenamiento de archivos para no guardar documentos en la raíz del bucket. |
| **Alcance** | Actualizar `S3Service` y lógica de upload para usar la ruta `clientes/{clientId}/tramites/{processInstanceId}/{documentId}_{fileName}`. |
| **Criterios de aceptación** | Todo archivo nuevo se almacena bajo la ruta jerárquica definida. La lectura y descarga usan el `s3Key` correcto. |
| **Estado sugerido** | Done |

## IBPMS-F1-05: Validar requisitos iniciales antes de iniciar trámites desde móvil

| Campo | Valor |
|---|---|
| **Tipo** | Historia |
| **Prioridad** | Alta |
| **Componentes** | Mobile, Backend |
| **Descripción** | Asegurar que el cliente móvil visualice y cargue los requisitos iniciales antes de crear una instancia de trámite. |
| **Alcance** | Crear/ajustar pantalla de requisitos, validación de obligatorios, inicio de proceso y subida de documentos. |
| **Criterios de aceptación** | El trámite no se inicia si falta un requisito obligatorio. Al iniciar correctamente, los documentos quedan asociados a la instancia. |
| **Estado sugerido** | Done |

---

# EPIC-02: Fase 2 - Gestión Documental, Privilegios, Auditoría y OnlyOffice

## IBPMS-F2-01: Crear modelo documental DMS

| Campo | Valor |
|---|---|
| **Tipo** | Historia |
| **Prioridad** | Alta |
| **Componentes** | Backend, Base de Datos |
| **Descripción** | Crear las entidades y repositorios base para gestionar documentos, permisos y auditoría. |
| **Alcance** | Implementar `DocumentRecord`, `DocumentPermission`, `DocumentAuditLog`, `DocumentAction` y repositorios. |
| **Criterios de aceptación** | MongoDB cuenta con registros documentales, permisos por usuario/documento y eventos de auditoría. |
| **Estado sugerido** | Done |

## IBPMS-F2-02: Implementar control de acceso documental

| Campo | Valor |
|---|---|
| **Tipo** | Tarea técnica |
| **Prioridad** | Alta |
| **Componentes** | Backend, Seguridad |
| **Descripción** | Centralizar la validación de permisos `VIEW`, `EDIT` y `DELETE` sobre documentos. |
| **Alcance** | Implementar `DocumentAccessService` y usarlo en servicios/controladores documentales. |
| **Criterios de aceptación** | Un funcionario sin permiso no puede visualizar, editar ni eliminar documentos protegidos. El sistema devuelve mensajes entendibles. |
| **Estado sugerido** | Done |

## IBPMS-F2-03: Implementar servicios y endpoints de administración documental

| Campo | Valor |
|---|---|
| **Tipo** | Historia |
| **Prioridad** | Alta |
| **Componentes** | Backend |
| **Descripción** | Proveer APIs para listar documentos, asignar permisos y consultar auditoría. |
| **Alcance** | Implementar `DocumentAdminController`, `DocumentAdminService`, DTOs de documentos, permisos y auditoría. |
| **Criterios de aceptación** | El administrador puede consultar documentos, modificar privilegios y revisar auditoría desde APIs protegidas. |
| **Estado sugerido** | Done |

## IBPMS-F2-04: Implementar visor documental con control de acceso

| Campo | Valor |
|---|---|
| **Tipo** | Historia |
| **Prioridad** | Alta |
| **Componentes** | Frontend, Backend |
| **Descripción** | Permitir visualizar documentos sin descarga automática y respetando permisos de acceso. |
| **Alcance** | Ajustar `DocumentController`, `DocumentService`, `DocumentViewerComponent` y `ExecutionService`. |
| **Criterios de aceptación** | El documento se muestra en la interfaz si el usuario tiene permiso. Si no lo tiene, se muestra un mensaje claro sin error técnico. |
| **Estado sugerido** | Done |

## IBPMS-F2-05: Crear panel de privilegios documentales para administrador de empresa

| Campo | Valor |
|---|---|
| **Tipo** | Historia |
| **Prioridad** | Alta |
| **Componentes** | Frontend |
| **Descripción** | Agregar en el dashboard de empresa una interfaz para asignar permisos documentales. |
| **Alcance** | Implementar `DocumentPrivilegesComponent`, rutas y menú de administración. |
| **Criterios de aceptación** | El administrador selecciona documento, funcionario y permisos. Los cambios se guardan y se reflejan al consultar nuevamente. |
| **Estado sugerido** | Done |

## IBPMS-F2-06: Crear panel de auditoría documental

| Campo | Valor |
|---|---|
| **Tipo** | Historia |
| **Prioridad** | Media |
| **Componentes** | Frontend, Backend |
| **Descripción** | Permitir al administrador consultar quién vio, editó o eliminó documentos. |
| **Alcance** | Implementar `DocumentAuditComponent` y endpoints de auditoría documental. |
| **Criterios de aceptación** | La vista muestra eventos con usuario, acción, documento y fecha, filtrados por empresa. |
| **Estado sugerido** | Done |

## IBPMS-F2-07: Integrar OnlyOffice Document Server

| Campo | Valor |
|---|---|
| **Tipo** | Historia técnica |
| **Prioridad** | Alta |
| **Componentes** | Backend, Frontend, Infraestructura |
| **Descripción** | Integrar edición colaborativa real para documentos Word y Excel mediante OnlyOffice. |
| **Alcance** | Implementar `OnlyOfficeController`, `OnlyOfficeService`, generación de configuración, URL prefirmada de S3, callback de guardado y carga dinámica del editor en Angular. |
| **Criterios de aceptación** | Un usuario con permiso `EDIT` abre el editor, modifica el documento y los cambios se guardan en S3 mediante callback. |
| **Estado sugerido** | Done |

## IBPMS-F2-08: Listar documentos desde procesos activos

| Campo | Valor |
|---|---|
| **Tipo** | Historia |
| **Prioridad** | Media |
| **Componentes** | Frontend, Backend |
| **Descripción** | Mostrar documentos relacionados con una instancia de trámite desde la vista de procesos activos. |
| **Alcance** | Agregar botón/listado de documentos en dashboard de funcionario y exponer permisos `canEdit`, `canDelete`. |
| **Criterios de aceptación** | El funcionario puede abrir el listado de documentos del trámite y solo ve acciones permitidas según privilegios. |
| **Estado sugerido** | Done |

## IBPMS-F2-09: Configurar Docker/Nginx para OnlyOffice

| Campo | Valor |
|---|---|
| **Tipo** | Tarea técnica |
| **Prioridad** | Alta |
| **Componentes** | Infraestructura |
| **Descripción** | Ajustar infraestructura para ejecutar OnlyOffice junto al backend y frontend. |
| **Alcance** | Actualizar `docker-compose.yml`, variables de entorno y documentación de Nginx local/productivo. |
| **Criterios de aceptación** | OnlyOffice corre en Docker y puede comunicarse con backend y S3 para lectura/guardado documental. |
| **Estado sugerido** | Done |

---

# EPIC-03: Fase 3 - Agente de Recepción Inteligente

## IBPMS-F3-01: Implementar endpoint IA para recomendación de trámites

| Campo | Valor |
|---|---|
| **Tipo** | Historia técnica |
| **Prioridad** | Alta |
| **Componentes** | IA |
| **Descripción** | Crear endpoint FastAPI que reciba texto del cliente y catálogo de políticas para devolver candidatos. |
| **Alcance** | Implementar `PolicyIntentRequest`, `PolicyIntentResponse`, clasificación con LLM y fallback léxico. |
| **Criterios de aceptación** | FastAPI devuelve hasta tres candidatos con `policyId`, confianza y requisitos faltantes. |
| **Estado sugerido** | Done |

## IBPMS-F3-02: Crear gateway Spring Boot para recepción inteligente

| Campo | Valor |
|---|---|
| **Tipo** | Historia |
| **Prioridad** | Alta |
| **Componentes** | Backend |
| **Descripción** | Permitir que Spring Boot reciba solicitudes del móvil y consulte el motor IA con políticas filtradas. |
| **Alcance** | Implementar `IntelligentReceptionController`, `IntelligentReceptionService` y DTOs de request/response. |
| **Criterios de aceptación** | El backend envía solo políticas permitidas al motor IA y devuelve candidatos al móvil. |
| **Estado sugerido** | Done |

## IBPMS-F3-03: Agregar fallback local ante fallas del motor IA

| Campo | Valor |
|---|---|
| **Tipo** | Tarea técnica |
| **Prioridad** | Media |
| **Componentes** | Backend |
| **Descripción** | Evitar errores visibles tipo Bad Gateway cuando el motor IA no responde correctamente. |
| **Alcance** | Implementar fallback local por coincidencia léxica y manejo defensivo de timeout/respuestas inválidas. |
| **Criterios de aceptación** | Si FastAPI falla, el backend devuelve candidatos locales o mensaje controlado sin romper la experiencia móvil. |
| **Estado sugerido** | Done |

## IBPMS-F3-04: Implementar experiencia móvil de agente inteligente

| Campo | Valor |
|---|---|
| **Tipo** | Historia |
| **Prioridad** | Alta |
| **Componentes** | Mobile |
| **Descripción** | Permitir que el cliente escriba una necesidad y seleccione un trámite recomendado. |
| **Alcance** | Actualizar `DashboardScreen`, `DashboardService` y modelos de resultado inteligente. |
| **Criterios de aceptación** | La app muestra candidatos y permite elegir uno sin iniciar automáticamente el trámite. |
| **Estado sugerido** | Done |

## IBPMS-F3-05: Redirigir trámite recomendado al flujo de requisitos iniciales

| Campo | Valor |
|---|---|
| **Tipo** | Historia |
| **Prioridad** | Alta |
| **Componentes** | Mobile |
| **Descripción** | Asegurar que una recomendación IA siga el mismo flujo que la selección manual de trámite. |
| **Alcance** | Navegar desde candidato seleccionado hacia `StartProcessRequirementsScreen`. |
| **Criterios de aceptación** | Si el trámite recomendado tiene requisitos obligatorios, la app solicita los archivos antes de iniciar el proceso. |
| **Estado sugerido** | Done |

---

# EPIC-04: Fase 4 - Deep Learning y Predicción Operativa

## IBPMS-F4-01: Crear endpoint predictivo en FastAPI

| Campo | Valor |
|---|---|
| **Tipo** | Historia técnica |
| **Prioridad** | Alta |
| **Componentes** | IA |
| **Descripción** | Implementar análisis predictivo a partir del event log enviado por Spring Boot. |
| **Alcance** | Crear endpoint `/api/v1/prediction/analyze`, modelos de entrada/salida y cálculos de anomalías, prioridades, rutas y cuellos de botella. |
| **Criterios de aceptación** | FastAPI responde con listas de anomalías, prioridades, rutas probables, cuellos de botella y advertencias. |
| **Estado sugerido** | Done |

## IBPMS-F4-02: Implementar detección de anomalías con TensorFlow/fallback

| Campo | Valor |
|---|---|
| **Tipo** | Tarea técnica |
| **Prioridad** | Media |
| **Componentes** | IA |
| **Descripción** | Detectar desviaciones operativas usando autoencoder cuando TensorFlow está disponible o heurística de respaldo. |
| **Alcance** | Implementar `_autoencoder_anomalies`, `_heuristic_task_anomalies` y `_document_burst_anomalies`. |
| **Criterios de aceptación** | El motor identifica tareas o cargas documentales sospechosas y explica el motivo de la anomalía. |
| **Estado sugerido** | Done |

## IBPMS-F4-03: Construir event log por empresa desde Spring Boot

| Campo | Valor |
|---|---|
| **Tipo** | Historia técnica |
| **Prioridad** | Alta |
| **Componentes** | Backend |
| **Descripción** | Extraer histórico de procesos, tareas y documentos filtrado por la empresa autenticada. |
| **Alcance** | Implementar `PredictiveAnalyticsService.analyze`, `resolvePolicies`, `toProcessEvent`, `toTaskEvent` y `toDocumentEvent`. |
| **Criterios de aceptación** | El payload enviado a FastAPI no contiene datos de otras empresas. |
| **Estado sugerido** | Done |

## IBPMS-F4-04: Crear gateway predictivo para administradores de empresa

| Campo | Valor |
|---|---|
| **Tipo** | Historia |
| **Prioridad** | Alta |
| **Componentes** | Backend, Seguridad |
| **Descripción** | Exponer análisis predictivo solo para usuarios `COMPANY_ADMIN`. |
| **Alcance** | Implementar `PredictiveAnalyticsController` con autorización por rol. |
| **Criterios de aceptación** | Solo el administrador de empresa puede consultar `/api/predictions/analysis`. |
| **Estado sugerido** | Done |

## IBPMS-F4-05: Crear dashboard Angular de predicciones

| Campo | Valor |
|---|---|
| **Tipo** | Historia |
| **Prioridad** | Alta |
| **Componentes** | Frontend |
| **Descripción** | Mostrar predicciones operativas en el dashboard del administrador de empresa. |
| **Alcance** | Implementar `PredictiveAnalyticsComponent`, modelos, servicio Angular y ruta `/admin/predictions`. |
| **Criterios de aceptación** | El dashboard muestra anomalías, prioridades, rutas probables y cuellos de botella. |
| **Estado sugerido** | Done |

## IBPMS-F4-06: Enriquecer predicciones con nombres legibles

| Campo | Valor |
|---|---|
| **Tipo** | Tarea técnica |
| **Prioridad** | Media |
| **Componentes** | Backend, Frontend |
| **Descripción** | Evitar que el dashboard muestre IDs técnicos como identificadores UUID. |
| **Alcance** | Enriquecer respuestas predictivas con nombres de políticas, procesos, tareas y documentos. |
| **Criterios de aceptación** | El dashboard muestra nombres legibles en lugar de claves internas cuando existen datos disponibles. |
| **Estado sugerido** | Done |

---

# EPIC-05: Fase 5 - Analítica y Reportes Dinámicos

## IBPMS-F5-01: Implementar agente analítico de reportes en FastAPI

| Campo | Valor |
|---|---|
| **Tipo** | Historia técnica |
| **Prioridad** | Alta |
| **Componentes** | IA |
| **Descripción** | Interpretar solicitudes de reportes en lenguaje natural y validar datos, criterios y formato. |
| **Alcance** | Implementar `dynamic_report_plan`, `_detect_report_scope`, `_detect_report_criteria`, `_detect_report_format`, `_missing_report_fields` y `_build_report_rows`. |
| **Criterios de aceptación** | El agente devuelve un plan completo si la solicitud contiene datos, criterios y formato; si falta algo, devuelve pregunta de aclaración. |
| **Estado sugerido** | Done |

## IBPMS-F5-02: Implementar backend de planificación y descarga de reportes

| Campo | Valor |
|---|---|
| **Tipo** | Historia |
| **Prioridad** | Alta |
| **Componentes** | Backend |
| **Descripción** | Permitir que Spring Boot consulte el agente analítico y genere archivos descargables. |
| **Alcance** | Implementar `DynamicReportController`, `DynamicReportService`, `DynamicReportRequestDto`, `DynamicReportPlanDto` y `GeneratedReportFileDto`. |
| **Criterios de aceptación** | El backend expone endpoints para planificar y descargar reportes dinámicos protegidos para `COMPANY_ADMIN`. |
| **Estado sugerido** | Done |

## IBPMS-F5-03: Generar reportes PDF, Excel y CSV

| Campo | Valor |
|---|---|
| **Tipo** | Tarea técnica |
| **Prioridad** | Alta |
| **Componentes** | Backend |
| **Descripción** | Renderizar archivos en los formatos solicitados por el usuario. |
| **Alcance** | Implementar `renderPdf`, `renderExcel` y `renderCsv`. Ajustar PDF para evitar contenido superpuesto. |
| **Criterios de aceptación** | El usuario puede descargar reportes en PDF, Excel o CSV y el contenido se visualiza de forma legible. |
| **Estado sugerido** | Done |

## IBPMS-F5-04: Crear UI Angular para reportes dinámicos

| Campo | Valor |
|---|---|
| **Tipo** | Historia |
| **Prioridad** | Alta |
| **Componentes** | Frontend |
| **Descripción** | Permitir que el administrador escriba solicitudes de reportes y descargue archivos generados. |
| **Alcance** | Ampliar `PredictiveAnalyticsComponent` y `PredictiveAnalyticsService` con planificación y descarga de reportes. |
| **Criterios de aceptación** | La interfaz muestra el plan o pregunta de aclaración y permite descargar el archivo si la solicitud está completa. |
| **Estado sugerido** | Done |

## IBPMS-F5-05: Aplicar aislamiento multiempresa en reportes

| Campo | Valor |
|---|---|
| **Tipo** | Tarea técnica |
| **Prioridad** | Alta |
| **Componentes** | Backend, Seguridad |
| **Descripción** | Asegurar que los reportes dinámicos usen solo información de la empresa autenticada. |
| **Alcance** | Filtrar políticas, procesos, tareas y documentos por empresa en `DynamicReportService`. |
| **Criterios de aceptación** | Un administrador de empresa no puede generar reportes con datos de otra empresa. |
| **Estado sugerido** | Done |

---

# EPIC-06: Documentación, Diseño y Evidencias

## IBPMS-DOC-01: Consolidar casos de uso de fases 1 a 5

| Campo | Valor |
|---|---|
| **Tipo** | Tarea documental |
| **Prioridad** | Media |
| **Componentes** | Documentación |
| **Descripción** | Agrupar los casos de uso generados durante las fases y eliminar duplicidades. |
| **Alcance** | Crear `CASOS_USO_FASES_1_5_IBPMS.md`. |
| **Criterios de aceptación** | El documento contiene CU-01 a CU-08 con actor, objetivo, precondición, flujo, postcondición y excepción. |
| **Estado sugerido** | Done |

## IBPMS-DOC-02: Documentar modelo C4 completo

| Campo | Valor |
|---|---|
| **Tipo** | Tarea documental |
| **Prioridad** | Media |
| **Componentes** | Documentación, Arquitectura |
| **Descripción** | Crear modelo C4 del sistema completo. |
| **Alcance** | Documentar contexto, contenedores, componentes y código crítico en `MODELO_C4_IBPMS.md`. |
| **Criterios de aceptación** | El documento incluye C1, C2, C3 y C4 alineados al proyecto actual. |
| **Estado sugerido** | Done |

## IBPMS-DOC-03: Actualizar diseño de base de datos

| Campo | Valor |
|---|---|
| **Tipo** | Tarea documental |
| **Prioridad** | Media |
| **Componentes** | Documentación, Base de Datos |
| **Descripción** | Actualizar modelo lógico y físico MongoDB con las nuevas colecciones y atributos. |
| **Alcance** | Crear `DISENO_BD_ACTUALIZADO_IBPMS.md`. |
| **Criterios de aceptación** | El diseño incluye `documents`, `document_permissions`, `document_audit_logs` e `initialRequirements`. |
| **Estado sugerido** | Done |

## IBPMS-DOC-04: Crear modelo conceptual actualizado en PlantUML

| Campo | Valor |
|---|---|
| **Tipo** | Tarea documental |
| **Prioridad** | Media |
| **Componentes** | Documentación, UML |
| **Descripción** | Representar el modelo conceptual actualizado del dominio. |
| **Alcance** | Crear `MODELO_CONCEPTUAL_ACTUALIZADO_IBPMS.puml`. |
| **Criterios de aceptación** | El PlantUML coincide con el diseño lógico actualizado. |
| **Estado sugerido** | Done |

## IBPMS-DOC-05: Documentar despliegue productivo

| Campo | Valor |
|---|---|
| **Tipo** | Tarea documental |
| **Prioridad** | Media |
| **Componentes** | Documentación, Infraestructura |
| **Descripción** | Crear diagrama de despliegue productivo con protocolos, puertos y nodos. |
| **Alcance** | Crear `DIAGRAMA_DESPLIEGUE_IBPMS.md`. |
| **Criterios de aceptación** | El documento indica HTTPS, HTTP, WebSocket, STOMP, AMQP, MongoDB TLS, TCP/UDP y reglas de firewall. |
| **Estado sugerido** | Done |

## IBPMS-DOC-06: Documentar pruebas de caja negra y evidencias

| Campo | Valor |
|---|---|
| **Tipo** | Tarea documental |
| **Prioridad** | Media |
| **Componentes** | Documentación, QA |
| **Descripción** | Crear pruebas de caja negra para los casos de uso consolidados y registrar ejecución/evidencia. |
| **Alcance** | Crear `PRUEBAS_CAJA_NEGRA_CU_FASES_1_5.md`. |
| **Criterios de aceptación** | El documento incluye CP-01 a CP-08 y sección 6.3 con estado `EXITOSO`. |
| **Estado sugerido** | Done |

---

# Resumen de Épicas

| Épica | Nombre | Cantidad de tickets |
|---|---|---:|
| EPIC-01 | Fase 1 - Reestructuración Core y Almacenamiento | 5 |
| EPIC-02 | Fase 2 - Gestión Documental, Privilegios, Auditoría y OnlyOffice | 9 |
| EPIC-03 | Fase 3 - Agente de Recepción Inteligente | 5 |
| EPIC-04 | Fase 4 - Deep Learning y Predicción Operativa | 6 |
| EPIC-05 | Fase 5 - Analítica y Reportes Dinámicos | 5 |
| EPIC-06 | Documentación, Diseño y Evidencias | 6 |
| **Total** |  | **36** |

---

# Labels Sugeridos para Jira

- `ibpms`
- `fase-1`
- `fase-2`
- `fase-3`
- `fase-4`
- `fase-5`
- `backend`
- `frontend`
- `mobile`
- `fastapi`
- `dms`
- `onlyoffice`
- `s3`
- `analytics`
- `reports`
- `documentation`

