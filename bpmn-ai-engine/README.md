# BPMN AI Engine (FastAPI)

Microservicio IA para soporte BPMN en dos modos:

1. Generacion/modificacion de diagrama (`/api/v1/agent/diagram`)
2. Copilot conversacional contextual (`/api/ai/copilot-chat`)

## Stack

- Python + FastAPI
- OpenAI SDK (`openai`)
- `python-dotenv` para configuracion de entorno

## Estructura

```text
bpmn-ai-engine/
|-- app/
|   |-- __init__.py
|   |-- main.py
|   |-- config.py
|   |-- models.py
|   |-- diagram_tools.py
|   `-- agent_service.py
|-- requirements.txt
|-- .env
`-- README.md
```

## Variables de entorno (`.env`)

- `AI_ENGINE_HOST`, `AI_ENGINE_PORT`
- `AI_ENGINE_RELOAD`
- `AI_ENGINE_LOG_LEVEL`
- `AI_ENGINE_ALLOWED_ORIGINS`
- `AI_API_KEY`
- `AI_MODEL` (por defecto sugerido: `gemini-3.1-flash-lite`)
- `AI_MODELS` (lista separada por comas para fallback automatico)
- `AI_TIMEOUT_SECONDS`
- `AI_BASE_URL` (para Gemini OpenAI-compatible: `https://generativelanguage.googleapis.com/v1beta/openai/`)

Nota: el servicio mantiene compatibilidad con variables antiguas `OPENAI_*` si todavia existen en tu entorno.

## Ejecucion local

```bash
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8010 --reload
```

## Docker

### Build imagen

```bash
docker build -f bpmn-ai-engine/Dockerfile -t bpmn-ai-engine:local .
```

### Run standalone

```bash
docker run --rm -p 8010:8010 \
  -e AI_API_KEY=your-key \
  -e AI_MODEL=gemini-3.1-flash-lite \
  -e AI_BASE_URL=https://generativelanguage.googleapis.com/v1beta/openai/ \
  bpmn-ai-engine:local
```

### Run integrado con stack

Desde `infra/`:

```bash
docker compose up --build
```

Backend se conecta internamente al microservicio usando:

- `AI_ENGINE_BASE_URL=http://bpmn-ai-engine:8010`

## Endpoints

### `GET /health`

Estado del servicio y proveedor IA. Incluye:

- `model`: modelo primario
- `available_models`: lista completa de modelos configurados

### `POST /api/v1/agent/diagram`

Asistente de creacion/modificacion de diagramas.

### `POST /api/ai/copilot-chat`

Chat contextual con diagrama actual.

Request:

```json
{
  "userMessage": "Analiza mi diagrama y dime errores",
  "models": ["gemini-3.1-flash-lite-preview", "gemini-2.5-flash"],
  "currentDiagram": {
    "cells": []
  }
}
```

Response:

```json
{
  "message": "Resumen de observaciones BPMN...",
  "suggested_actions": [
    "Conecta la tarea X con un nodo FIN",
    "Agrega salida por defecto al gateway de decision"
  ]
}
```

## Fallback de modelos

El microservicio intenta responder usando una lista ordenada de modelos:

1. `models` enviados en la request (si existen)
2. `AI_MODELS`
3. `AI_MODEL` como ultimo respaldo

Si un modelo falla por cuota/alta demanda, intenta el siguiente automaticamente.

## Sobre las llamadas frecuentes a `/health`

Si usas Docker Compose, esas llamadas las hace el `healthcheck` del contenedor.
Ahora quedaron configurables con:

- `AI_ENGINE_HEALTHCHECK_INTERVAL` (default `60s`)
- `AI_ENGINE_HEALTHCHECK_TIMEOUT` (default `5s`)
- `AI_ENGINE_HEALTHCHECK_RETRIES` (default `3`)
- `AI_ENGINE_HEALTHCHECK_START_PERIOD` (default `25s`)

## Nota de integracion

En el proyecto principal, Angular no llama directo al microservicio.
La ruta recomendada es:

- Angular -> `POST /api/copilot/chat` (Spring)
- Spring Gateway -> `POST http://localhost:8010/api/ai/copilot-chat`
