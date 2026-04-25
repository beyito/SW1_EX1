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
- `AI_TIMEOUT_SECONDS`
- `AI_BASE_URL` (para Gemini OpenAI-compatible: `https://generativelanguage.googleapis.com/v1beta/openai/`)

Nota: el servicio mantiene compatibilidad con variables antiguas `OPENAI_*` si todavia existen en tu entorno.

## Ejecucion local

```bash
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8010 --reload
```

## Endpoints

### `GET /health`

Estado del servicio y proveedor IA.

### `POST /api/v1/agent/diagram`

Asistente de creacion/modificacion de diagramas.

### `POST /api/ai/copilot-chat`

Chat contextual con diagrama actual.

Request:

```json
{
  "userMessage": "Analiza mi diagrama y dime errores",
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

## Nota de integracion

En el proyecto principal, Angular no llama directo al microservicio.
La ruta recomendada es:

- Angular -> `POST /api/copilot/chat` (Spring)
- Spring Gateway -> `POST http://localhost:8010/api/ai/copilot-chat`
