# FASE 5: Analitica y Reportes Dinamicos

## Plan de Accion

- [archivo: bpmn-ai-engine/app/main.py] Se agrego el endpoint del agente analitico para interpretar solicitudes de reportes.
- [archivo: backend/src/main/java/com/politicanegocio/core/controller/DynamicReportController.java] Se agregaron endpoints REST para preparar y descargar reportes.
- [archivo: backend/src/main/java/com/politicanegocio/core/service/DynamicReportService.java] Se implemento extraccion de historico por empresa, llamada a FastAPI y generacion de archivos.
- [archivo: backend/src/main/java/com/politicanegocio/core/dto/DynamicReportRequestDto.java] Se agrego el contrato de solicitud textual.
- [archivo: backend/src/main/java/com/politicanegocio/core/dto/DynamicReportPlanDto.java] Se agrego el contrato del plan analitico.
- [archivo: backend/src/main/java/com/politicanegocio/core/dto/GeneratedReportFileDto.java] Se agrego el contrato interno de archivo generado.
- [archivo: frontend/src/app/features/predictive-analytics/services/predictive-analytics.service.ts] Se agregaron metodos para validar y descargar reportes dinamicos.
- [archivo: frontend/src/app/features/predictive-analytics/components/predictive-analytics/predictive-analytics.component.ts] Se agrego estado de UI para el agente analitico.
- [archivo: frontend/src/app/features/predictive-analytics/components/predictive-analytics/predictive-analytics.component.html] Se agrego la tarjeta visual de reportes dinamicos.
- [archivo: frontend/src/app/features/predictive-analytics/components/predictive-analytics/predictive-analytics.component.scss] Se agregaron estilos del bloque de reportes.

## Objetivo

Permitir que el administrador de empresa solicite reportes en lenguaje natural, por ejemplo:

```text
Quiero un reporte de los cuellos de botella de RRHH del ultimo mes en PDF
```

El sistema debe validar obligatoriamente tres elementos:

- **Datos:** que informacion se quiere reportar.
- **Criterios:** filtros o condiciones del reporte.
- **Formato:** tipo de archivo final.

Si falta uno de esos elementos, el asistente no genera el archivo y pregunta lo que falta.

## Capa Python - Agente Analitico

### [archivo: bpmn-ai-engine/app/main.py]

Se agrego:

```text
POST /api/v1/analytics/report-plan
```

Responsabilidades:

- Recibir el texto del usuario.
- Detectar el tipo de datos solicitado.
- Detectar criterios como periodo, area/carril o estado.
- Detectar formato (`PDF`, `Excel`, `CSV`).
- Validar que existan `datos`, `criterios` y `formato`.
- Si falta algo, devolver una pregunta clara.
- Si esta completo, estructurar un JSON con filas, resumen y advertencias.

### Tipos de datos soportados

- `bottlenecks`: cuellos de botella.
- `anomalies`: anomalias.
- `priorities`: prioridades.
- `routes`: rutas probables.
- `documents`: documentos.
- `tasks`: tareas.
- `processes`: tramites o instancias.

### Criterios soportados

- Periodo: `hoy`, `ultima semana`, `ultimo mes`, `ultimos 7 dias`, `ultimos 30 dias`.
- Area/carril: por ejemplo `RRHH`, `Legal`, `Cliente`.
- Estado: `pendientes`, `completadas`, `rechazadas`, `activas`.

### Formatos soportados

- `pdf`
- `excel`
- `csv`

## Capa Spring Boot - Gateway y Generacion de Archivos

### [archivo: backend/src/main/java/com/politicanegocio/core/controller/DynamicReportController.java]

Se agregaron:

```text
POST /api/reports/dynamic/plan
POST /api/reports/dynamic/download
```

Ambos endpoints estan restringidos a:

```text
COMPANY_ADMIN
```

### Endpoint `/plan`

Recibe:

```json
{
  "prompt": "Quiero un reporte de los cuellos de botella de RRHH del ultimo mes en PDF"
}
```

Devuelve un plan:

```json
{
  "complete": true,
  "missingFields": [],
  "question": "",
  "dataScope": "bottlenecks",
  "criteria": {
    "period": "last_month",
    "lane": "RRHH"
  },
  "format": "pdf",
  "title": "Reporte de cuellos de botella - RRHH (last_month)",
  "summary": "Se generaron 2 filas para el reporte solicitado.",
  "rows": [],
  "warnings": []
}
```

Si falta informacion:

```json
{
  "complete": false,
  "missingFields": ["formato"],
  "question": "Para generar el reporte necesito que indiques en que formato lo necesitas (PDF, Excel o CSV)."
}
```

### Endpoint `/download`

Si la solicitud esta completa, genera y devuelve el archivo final.

Formatos:

- **PDF:** documento simple renderizado desde Spring con fuente monoespaciada, interlineado fijo y tabla textual para evitar contenido superpuesto.
- **Excel:** archivo `.xls` compatible con Excel usando tabla HTML.
- **CSV:** archivo CSV real.

## Seguridad y Aislamiento por Empresa

- Solo `COMPANY_ADMIN` puede generar reportes dinamicos.
- Spring Boot extrae solo policies de `user.company`.
- Los procesos, tareas y documentos enviados a Python ya llegan filtrados por empresa.
- Python no consulta directamente la base de datos.
- Los IDs internos pueden existir en el calculo, pero Spring enriquece las filas con nombres legibles.

## Capa Angular - Dashboard

### [archivo: frontend/src/app/features/predictive-analytics/components/predictive-analytics/predictive-analytics.component.html]

Se agrego la tarjeta `Agente analitico`.

Flujo:

- El usuario escribe la solicitud.
- Presiona `Validar solicitud`.
- Si faltan datos, criterios o formato, la UI muestra la pregunta del asistente.
- Si esta completa, se habilita `Descargar reporte`.
- El archivo se descarga desde Spring Boot.

## Casos de Uso Generados

### CU-F5-01: Validar Solicitud de Reporte

- **Actor principal:** Administrador de Empresa.
- **Objetivo:** verificar si una solicitud textual contiene datos, criterios y formato.
- **Precondicion:** el administrador inicio sesion.
- **Flujo principal:** el usuario escribe una solicitud, Angular la envia a Spring, Spring la envia al agente Python, y Python responde si esta completa.
- **Resultado:** la UI muestra el plan del reporte o una pregunta de aclaracion.

### CU-F5-02: Solicitar Aclaracion por Campos Faltantes

- **Actor principal:** Agente Analitico.
- **Objetivo:** evitar generar reportes ambiguos o incompletos.
- **Precondicion:** falta `datos`, `criterios` o `formato`.
- **Flujo principal:** el agente detecta campos faltantes y devuelve `complete=false` con una pregunta.
- **Resultado:** el administrador sabe exactamente que debe completar.

### CU-F5-03: Generar Reporte PDF

- **Actor principal:** Administrador de Empresa.
- **Objetivo:** descargar un reporte en PDF.
- **Precondicion:** la solicitud incluye datos, criterios y formato `PDF`.
- **Flujo principal:** Spring recibe el plan completo, renderiza un PDF y lo devuelve como archivo.
- **Resultado:** el administrador descarga el PDF.

### CU-F5-04: Generar Reporte Excel

- **Actor principal:** Administrador de Empresa.
- **Objetivo:** descargar un reporte editable en Excel.
- **Precondicion:** la solicitud incluye formato `Excel`.
- **Flujo principal:** Spring renderiza una tabla compatible con Excel.
- **Resultado:** el administrador descarga un archivo `.xls`.

### CU-F5-05: Generar Reporte CSV

- **Actor principal:** Administrador de Empresa.
- **Objetivo:** descargar un reporte liviano para analisis externo.
- **Precondicion:** la solicitud incluye formato `CSV`.
- **Flujo principal:** Spring renderiza filas separadas por comas.
- **Resultado:** el administrador descarga un archivo `.csv`.

## Validacion

- `python -m py_compile bpmn-ai-engine/app/main.py` paso correctamente.
- `mvnw.cmd -q -DskipTests compile` paso correctamente.
- `npm.cmd run build` paso correctamente.
- El build de Angular mantiene warnings preexistentes del proyecto: presupuesto SCSS del disenador, dependencias CommonJS de JointJS/STOMP y selectores `%`.
