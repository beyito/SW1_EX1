# Reestructuración BPMN AI Engine

## Objetivo

Separar responsabilidades del microservicio FastAPI y preparar el motor para defender el punto:

```text
Deep Learning sin internet mediante modelos locales.
```

## Nueva Organización

```text
app/
  main.py
  config.py
  models.py
  agent_service.py
  diagram_tools.py
  local_ml/
    __init__.py
    datasets.py
    offline_models.py
  routers/
    __init__.py
    local_ml.py
  schemas/
    __init__.py
  services/
    __init__.py
    predictive_orchestrator.py
  models/
    local/

datasets/
  predictive_task_history.csv
  document_activity_history.csv
  policy_intents.json
```

## Responsabilidades

- `main.py`
  - Crea la aplicación FastAPI.
  - Configura CORS.
  - Registra routers por tipo de IA.
  - No contiene lógica de negocio.

- `app/services/predictive_orchestrator.py`
  - Orquesta el análisis predictivo.
  - Fusiona el cálculo existente con el servicio Deep Learning offline.
  - Evita que `main.py` concentre la lógica de combinación de modelos.

- `app/routers/local_ml.py`
  - Expone `GET /api/v1/local-ml/status`.
  - Permite verificar datasets locales y disponibilidad de TensorFlow.

- `app/local_ml/datasets.py`
  - Carga datasets locales desde archivos del repositorio.
  - No usa internet.
  - Expone histórico de tareas, actividad documental e intenciones de trámites.

- `app/local_ml/offline_models.py`
  - Contiene modelos locales para predicción.
  - Usa TensorFlow/Keras si está disponible.
  - Carga modelos `.keras` desde disco cuando existen.
  - Si no existen, los entrena con datasets locales y los guarda.
  - Si TensorFlow no está instalado, cae a heurísticas locales.

- `datasets/predictive_task_history.csv`
  - Histórico base de tareas.
  - Sirve como entrenamiento offline cuando el proyecto todavía tiene pocos datos reales.

- `datasets/document_activity_history.csv`
  - Histórico base de actividad documental.
  - Ayuda a detectar cargas anómalas de documentos.

- `datasets/policy_intents.json`
  - Ejemplos locales de intención para recepción inteligente.
  - Sirve como base offline/fallback para clasificación semántica futura.

## Modelos Locales Incorporados

### 1. Anomalías

El endpoint predictivo mantiene el autoencoder TensorFlow existente y ahora lo complementa con datasets locales.

Cuando TensorFlow está disponible:

- Se usa autoencoder para detectar desviaciones en tiempos y rutas.

Cuando TensorFlow no está disponible:

- Se usa fallback heurístico local.

### 2. Prioridad

Se agregó un clasificador local Keras de prioridad.

Artefactos:

```text
app/models/local/priority_classifier.keras
app/models/local/priority_classifier.json
```

Entradas usadas:

- Cantidad de tareas del proceso.
- Cantidad de tareas pendientes o en progreso.
- Duración promedio.
- Estado del proceso.

Salidas:

- `BAJA`
- `MEDIA`
- `ALTA`
- `CRITICA`

### 3. Rutas Probables

Se agregó un clasificador local Keras para predecir la siguiente tarea probable.

Artefactos:

```text
app/models/local/route_classifier.keras
app/models/local/route_classifier.json
```

El modelo usa histórico local y datos reales enviados por Spring Boot para entrenar pares:

- tarea actual
- siguiente tarea probable
- probabilidad

Si TensorFlow no está disponible, usa fallback estadístico por transiciones históricas.

### 4. Cuellos de Botella

Se agregó un regresor local Keras de riesgo por carril.

Artefactos:

```text
app/models/local/bottleneck_regressor.keras
app/models/local/bottleneck_regressor.json
```

El modelo aprende a partir de:

- duración histórica promedio
- cantidad de tareas pendientes
- carril BPMN

Si TensorFlow no está disponible, usa fórmula heurística local.

### 5. Anomalías Documentales

Se agregó detección offline de cargas documentales inusuales.

El umbral se calcula con histórico local:

```text
promedio + 2 * desviación estándar
```

## Uso de Datos Locales + Datos Reales

El motor no reemplaza los datos reales del sistema. Los combina:

```text
Predicción final = Event log real enviado por Spring Boot + datasets locales offline
```

Esto permite:

- mejores predicciones cuando hay poco histórico real;
- defensa académica de modelos locales sin internet;
- transición futura hacia entrenamiento con histórico real exportado desde MongoDB.

## Limitaciones Actuales

- Los datasets iniciales son sintéticos y deben crecer con datos reales.
- Los modelos se entrenan en memoria durante la ejecución.
- La clasificación NLP principal todavía puede usar proveedor externo si `AI_API_KEY` está configurado.
- Para defensa estricta offline, se debe ejecutar sin `AI_API_KEY` o agregar un clasificador NLP local completo.

## Recomendación de Defensa

Frase sugerida:

> El proyecto aplica Deep Learning local en el módulo predictivo mediante TensorFlow/Keras y datasets offline incluidos en el repositorio. El sistema combina histórico real enviado por Spring Boot con datasets locales para entrenar modelos de anomalías, prioridad, rutas y cuellos de botella sin depender de internet. Las funciones NLP pueden usar proveedor externo, pero el núcleo predictivo opera localmente con fallback heurístico si TensorFlow no está disponible.

## Verificación de Modelos Locales

```text
GET /api/v1/local-ml/status
```

Este endpoint muestra:

- disponibilidad de TensorFlow;
- cantidad de registros en datasets locales;
- ruta de modelos locales;
- artefactos `.keras` y `.json` encontrados en disco.
