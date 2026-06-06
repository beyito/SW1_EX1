# FASE 4: Deep Learning y Predicción con TensorFlow

## Plan de Acción

- [archivo: bpmn-ai-engine/app/main.py] Se agregó el endpoint predictivo para anomalías, prioridades, rutas probables y cuellos de botella.
- [archivo: bpmn-ai-engine/requirements-tensorflow.txt] Se agregaron dependencias opcionales para activar TensorFlow/Keras en entornos compatibles.
- [archivo: backend/src/main/java/com/politicanegocio/core/controller/PredictiveAnalyticsController.java] Se expuso el endpoint REST del gateway predictivo restringido a administradores de empresa.
- [archivo: backend/src/main/java/com/politicanegocio/core/service/PredictiveAnalyticsService.java] Se implementó la extracción de event log desde Spring Boot filtrando únicamente trámites/policies de la empresa autenticada.
- [archivo: backend/src/main/java/com/politicanegocio/core/dto/PredictiveAnalysisResponseDto.java] Se agregó el contrato de respuesta predictiva.
- [archivo: frontend/src/app/features/predictive-analytics/components/predictive-analytics/predictive-analytics.component.ts] Se agregó el apartado visual de predicción para el dashboard del administrador de empresa.
- [archivo: frontend/src/app/features/predictive-analytics/services/predictive-analytics.service.ts] Se agregó el servicio Angular que consulta el gateway predictivo con token de sesión.
- [archivo: frontend/src/app/app.routes.ts] Se agregó la ruta `/admin/predictions`.
- [archivo: frontend/src/app/features/admin-layout/admin-layout.component.html] Se agregó el enlace `Predicción Operativa` al menú del panel de empresa.

## Objetivo

Implementar el núcleo predictivo del iBPMS usando el histórico real que guarda Spring Boot:

- Procesos instanciados.
- Tareas ejecutadas o pendientes.
- Carriles responsables.
- Tiempos de espera y ejecución.
- Documentos cargados por instancia.

El motor IA procesa estos eventos para detectar anomalías, estimar prioridades, predecir rutas probables y anticipar cuellos de botella.

El apartado está orientado al `COMPANY_ADMIN`. Cada administrador ve predicciones generadas solo con procesos, tareas, documentos y policies pertenecientes a su empresa.

## Capa Python - Motor IA

### [archivo: bpmn-ai-engine/app/main.py]

Se agregó:

```text
POST /api/v1/prediction/analyze
```

### Entrada esperada

```json
{
  "processes": [
    {
      "processInstanceId": "proc-1",
      "policyId": "policy-1",
      "title": "Reposición de tarjeta",
      "description": "Cliente reporta pérdida urgente",
      "status": "ACTIVE",
      "startedBy": "cliente1",
      "startedAt": "2026-06-05T10:00:00",
      "completedAt": null
    }
  ],
  "tasks": [
    {
      "processInstanceId": "proc-1",
      "policyId": "policy-1",
      "taskId": "validar_datos",
      "laneId": "rrhh",
      "status": "PENDING",
      "createdAt": "2026-06-05T10:01:00",
      "startedAt": null,
      "completedAt": null
    }
  ],
  "documents": [
    {
      "processInstanceId": "proc-1",
      "policyId": "policy-1",
      "documentId": "doc-1",
      "createdBy": "cliente1",
      "createdAt": "2026-06-05T10:02:00",
      "size": 12000
    }
  ]
}
```

### Salida

```json
{
  "model_strategy": "tensorflow_autoencoder",
  "anomalies": [],
  "priorities": [],
  "route_predictions": [],
  "bottlenecks": [],
  "warnings": []
}
```

## Modelos Predictivos Implementados

### 1. Anomalías - Autoencoder TensorFlow/Keras

- Si TensorFlow está instalado y hay suficiente histórico, el motor entrena un autoencoder pequeño en runtime.
- Las features usadas por tarea son: minutos de espera, minutos de ejecución, edad de tarea pendiente, estado numérico, longitud del carril y longitud del taskId.
- Se calcula error de reconstrucción.
- Si el error supera el umbral estadístico, se marca como anomalía.

### Fallback de anomalías

Si TensorFlow no está disponible o hay pocos datos, se usa un detector heurístico:

- Tareas con duración/espera muy superior a la mediana histórica.
- Procesos con espera excesiva.
- Subidas masivas de documentos por minuto.

Esto evita que el sistema falle en entornos donde TensorFlow no esté instalado.

### 2. Prioridad - Clasificación Multiclase

Se calcula una prioridad por instancia:

- `BAJA`
- `MEDIA`
- `ALTA`
- `CRÍTICA`

Factores considerados:

- Cantidad de tareas pendientes o en progreso.
- Tareas rechazadas.
- Antigüedad del proceso.
- Urgencia detectada en texto (`urgente`, `emergencia`, `crítico`, `perdí`, etc.).

### 3. Rutas Probables

Se predicen transiciones probables a partir del event log:

- Se ordenan tareas por instancia.
- Se calculan transiciones históricas `tarea_actual -> siguiente_tarea`.
- Se devuelve la siguiente tarea más probable y su probabilidad.

### 4. Cuellos de Botella

Se evalúa riesgo por carril:

- Tareas pendientes/en progreso.
- Tareas completadas.
- Espera promedio histórica.
- Presión de carga.

El resultado indica carriles con mayor riesgo de colapso.

## Capa Spring Boot - Gateway Predictivo

### [archivo: backend/src/main/java/com/politicanegocio/core/controller/PredictiveAnalyticsController.java]

Se agregó:

```text
GET /api/predictions/analysis
```

Roles permitidos:

```text
COMPANY_ADMIN
```

La restricción existe en dos capas:

- En el controller con `@PreAuthorize("hasAuthority('COMPANY_ADMIN')")`.
- En el service con validación defensiva de roles antes de extraer el histórico.

### [archivo: backend/src/main/java/com/politicanegocio/core/service/PredictiveAnalyticsService.java]

Responsabilidades:

- Validar usuario autenticado.
- Validar que el usuario sea administrador de empresa.
- Obtener únicamente las policies de `user.company`.
- Extraer procesos históricos asociados a esas policies.
- Extraer tareas relacionadas a esos procesos.
- Extraer documentos asociados a esas policies.
- Construir el event log compacto.
- Enviar el payload a FastAPI.
- Parsear la respuesta predictiva de forma defensiva.

## Capa Angular - Dashboard del Administrador de Empresa

### [archivo: frontend/src/app/features/predictive-analytics/components/predictive-analytics/predictive-analytics.component.ts]

Se agregó el apartado visual:

```text
/admin/predictions
```

Funcionalidades:

- Botón `Actualizar análisis` para consultar Spring Boot.
- Tarjetas resumen con estrategia usada, cantidad de anomalías, prioridades y cuellos de botella.
- Listado de prioridades por instancia con etiqueta `BAJA`, `MEDIA`, `ALTA` o `CRÍTICA`.
- Listado de anomalías operativas por proceso, tarea, carril o documento.
- Listado de rutas probables.
- Listado de cuellos de botella por carril.
- Mensajes entendibles para errores `401`, `403` o fallas del motor predictivo.

### [archivo: frontend/src/app/features/predictive-analytics/services/predictive-analytics.service.ts]

Servicio Angular responsable de:

- Enviar el token JWT al backend.
- Consumir `GET /api/predictions/analysis`.
- Traducir errores de permisos en mensajes legibles para el administrador.

### [archivo: frontend/src/app/features/admin-layout/admin-layout.component.html]

Se agregó el enlace de menú `Predicción Operativa` para que el administrador de empresa acceda desde su dashboard.

## Dependencias TensorFlow

No se agregó TensorFlow al `requirements.txt` principal para evitar romper entornos livianos o versiones de Python no compatibles.

Se agregó:

```text
bpmn-ai-engine/requirements-tensorflow.txt
```

Instalación opcional:

```bash
pip install -r requirements.txt -r requirements-tensorflow.txt
```

Si TensorFlow no está instalado, el endpoint sigue funcionando con fallback heurístico.

## Cómo Interpretar las Predicciones

### Estrategia del Modelo

El campo `modelStrategy` indica qué enfoque usó el motor IA para calcular anomalías:

- `tensorflow_autoencoder`: se usó TensorFlow/Keras con un autoencoder. Es el modo más cercano al requerimiento de Deep Learning.
- `heuristic`: no hubo suficiente histórico o TensorFlow no estaba disponible, así que el sistema usó reglas estadísticas simples.
- `empty`: no existen policies o procesos suficientes para analizar.

En el dashboard estos valores se muestran como etiquetas humanas:

- `Autoencoder TensorFlow`
- `Cálculo heurístico`
- `Sin datos suficientes`

### Qué es un Autoencoder

Un autoencoder es una red neuronal que aprende cómo se ve un trámite "normal" usando datos históricos.

En esta fase no intenta adivinar una etiqueta manual, sino reconstruir el comportamiento típico de las tareas. Si una tarea tiene tiempos, espera o ruta muy distinta a lo aprendido, el error de reconstrucción sube y el sistema la marca como anomalía.

Ejemplo práctico:

- Si normalmente una tarea tarda 20 minutos y ahora tarda 8 horas, el modelo puede marcarla como fuera de patrón.
- Si una tarea queda pendiente demasiado tiempo respecto al histórico, también puede marcarla.
- Si el flujo sigue una ruta rara comparada con ejecuciones anteriores, el motor puede elevar la señal de riesgo.

La frase anterior `Autoencoder TensorFlow detectó desviación de tiempos/ruta` significa:

```text
El tiempo de espera, duración o ruta de esta tarea se aleja mucho del comportamiento normal registrado.
```

### Prioridades

Las prioridades ayudan a ordenar la atención operativa de las instancias activas.

Valores posibles:

- `BAJA`: el trámite no muestra señales urgentes.
- `MEDIA`: hay cierta carga o antigüedad que conviene observar.
- `ALTA`: hay señales importantes de retraso, carga pendiente o urgencia.
- `CRÍTICA`: el trámite debería revisarse pronto porque combina varios factores de riesgo.

Campos principales:

- `processTitle`: nombre legible de la instancia o trámite.
- `policyName`: nombre del trámite/policy.
- `priority`: etiqueta calculada (`BAJA`, `MEDIA`, `ALTA`, `CRÍTICA`).
- `score`: valor entre `0` y `1` que representa la fuerza de la prioridad. En el dashboard se muestra como porcentaje de confianza.
- `reason`: explicación breve del cálculo.

Factores usados:

- Cantidad de tareas pendientes.
- Cantidad de tareas en progreso.
- Rechazos.
- Antigüedad del trámite.
- Palabras de urgencia en título o descripción.

### Anomalías

Las anomalías señalan eventos fuera del comportamiento esperado.

Tipos actuales:

- `TASK_DURATION_ANOMALY`: una tarea tardó, esperó o se comportó distinto al histórico.
- `DOCUMENT_BURST`: se subieron demasiados documentos en un intervalo muy corto.

Campos principales:

- `processTitle`: trámite o instancia donde ocurrió la anomalía.
- `documentName`: documento relacionado, si aplica.
- `taskLabel`: tarea relacionada, si aplica.
- `laneName`: carril o área relacionada, si aplica.
- `score`: intensidad de la anomalía. Mientras más alto, más fuerte es la desviación.
- `reason`: explicación de negocio.

Interpretación:

- Una anomalía no significa necesariamente error o fraude.
- Significa que conviene revisar porque el patrón se aleja del histórico.
- Sirve para detectar atrasos, saturación, cargas documentales inusuales o rutas poco frecuentes.

### Rutas Probables

Las rutas probables anticipan cuál puede ser el siguiente paso del trámite.

Campos principales:

- `currentTaskLabel`: paso actual.
- `predictedNextTaskLabel`: siguiente paso más probable.
- `probability`: probabilidad calculada con transiciones históricas.
- `support`: cantidad de casos similares usados como respaldo cuando el motor lo informa.
- `reason`: explicación del cálculo.

Ejemplo:

```text
Validar documentos -> Aprobar solicitud
80% de probabilidad
```

Esto significa que, según el histórico, cuando una instancia llega a `Validar documentos`, normalmente continúa por `Aprobar solicitud`.

### Cuellos de Botella

Los cuellos de botella indican carriles o áreas con riesgo de saturación.

Campos principales:

- `laneName`: nombre del carril o área.
- `risk`: riesgo calculado entre `0` y `1`. En el dashboard se muestra como porcentaje.
- `pendingTasks`: cantidad de tareas pendientes o en progreso en ese carril.
- `avgWaitMinutes`: espera promedio histórica antes de iniciar tareas en ese carril.
- `reason`: explicación del riesgo.

Interpretación:

- Riesgo bajo: el carril parece operar con normalidad.
- Riesgo medio: hay presión acumulada, conviene observar.
- Riesgo alto: el carril puede estar retrasando varios trámites.

### Por Qué el Dashboard ya no Muestra Claves Técnicas

El motor IA y Spring Boot siguen usando IDs internos para cruzar datos de forma segura:

- `policyId`
- `processInstanceId`
- `documentId`
- `laneId`
- `taskId`

Pero el dashboard en Angular muestra campos enriquecidos:

- `policyName`
- `processTitle`
- `documentName`
- `laneName`
- `taskLabel`

Esto evita mostrar valores como `df112b33-b168-453e-a123-d5939123cb50` al administrador de empresa.

## Casos de Uso Generados

### CU-F4-01: Detectar Anomalías Operativas

- **Actor principal:** Administrador de Empresa.
- **Objetivo:** identificar procesos, tareas o cargas documentales fuera del comportamiento normal de su empresa.
- **Precondición:** existen procesos, tareas o documentos históricos de la empresa autenticada.
- **Flujo principal:** Spring extrae event log filtrado por empresa, FastAPI calcula anomalías con autoencoder o fallback, y devuelve eventos sospechosos.
- **Resultado:** el administrador de empresa puede detectar desviaciones tempranas sin acceder a datos de otras empresas.

### CU-F4-02: Calcular Prioridad de Instancias

- **Actor principal:** Administrador de Empresa.
- **Objetivo:** asignar prioridad a procesos activos de su empresa.
- **Precondición:** existen procesos activos y tareas asociadas en la empresa.
- **Flujo principal:** el motor evalúa carga, antigüedad, rechazos y urgencia textual.
- **Resultado:** cada instancia recibe prioridad `BAJA`, `MEDIA`, `ALTA` o `CRÍTICA`.

### CU-F4-03: Predecir Ruta Probable

- **Actor principal:** Administrador de Empresa.
- **Objetivo:** anticipar la siguiente tarea probable según histórico de la empresa.
- **Precondición:** existen secuencias históricas de tareas.
- **Flujo principal:** el motor calcula transiciones frecuentes por taskId dentro del histórico filtrado.
- **Resultado:** se devuelve la siguiente tarea probable y su probabilidad.

### CU-F4-04: Predecir Cuellos de Botella

- **Actor principal:** Administrador de Empresa.
- **Objetivo:** identificar carriles con riesgo de saturación.
- **Precondición:** existen tareas pendientes/completadas por carril.
- **Flujo principal:** el motor calcula presión por carril usando carga pendiente y tiempos de espera.
- **Resultado:** se obtiene una lista de carriles con riesgo y razones.

### CU-F4-05: Consultar Panel Predictivo de Empresa

- **Actor principal:** Administrador de Empresa.
- **Objetivo:** consultar desde el dashboard una vista consolidada de predicciones operativas.
- **Precondición:** el usuario inició sesión con rol `COMPANY_ADMIN`.
- **Flujo principal:** el administrador entra a `Predicción Operativa`, el frontend consulta el gateway predictivo y muestra tarjetas por categoría.
- **Resultado:** el administrador visualiza riesgos operativos sin salir del panel de empresa.

## Reglas de Seguridad y Arquitectura

- Spring Boot nunca envía histórico de otras empresas.
- `COMPANY_ADMIN` analiza solo policies de su empresa.
- `SOFTWARE_ADMIN` y `FUNCTIONARY` no pueden usar el endpoint predictivo operativo.
- Python no consulta directamente la base de datos; solo recibe event logs filtrados desde Spring Boot.
- TensorFlow es opcional y se activa solo si el entorno lo soporta.
- El sistema mantiene respuesta predictiva aunque TensorFlow no esté disponible.

## Validación

- `python -m py_compile bpmn-ai-engine/app/main.py` pasó correctamente en la implementación base.
- `mvnw -q -DskipTests compile` pasó correctamente en la implementación base.
- La sección Angular queda disponible en `/admin/predictions`.
