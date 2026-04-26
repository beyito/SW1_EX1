# Cambios: Fallback de Modelos y Healthcheck

Fecha: 2026-04-25

## Objetivo

1. Permitir enviar/usar una lista de modelos IA para tolerar alta demanda.
2. Reducir la frecuencia de llamadas al endpoint `/health` en Docker.

## Cambios implementados

### 1) Lista de modelos con fallback automatico

- Archivo: `app/config.py`
  - Nueva variable `AI_MODELS` (CSV).
  - Normalizacion y deduplicacion de modelos.
  - `ai_model` queda como primer modelo efectivo (compatibilidad).

- Archivo: `app/main.py`
  - `CopilotRequest` ahora acepta `models: List[str]` opcional.
  - `copilot-chat` intenta cada modelo en orden hasta obtener respuesta.
  - Logs por modelo fallido y log de modelo exitoso.
  - `/health` ahora expone `available_models`.

- Archivo: `app/agent_service.py`
  - Soporte para `models` en `AgentRequest`.
  - Fallback de modelos para creacion/modificacion de diagramas.

- Archivo: `app/models.py`
  - `AgentRequest` incluye `models`.
  - `HealthResponse` incluye `available_models`.

### 2) Healthcheck Docker menos agresivo

- Archivo: `infra/docker-compose.yml`
  - Healthcheck del `bpmn-ai-engine` ahora configurable por variables:
    - `AI_ENGINE_HEALTHCHECK_INTERVAL` (default `60s`)
    - `AI_ENGINE_HEALTHCHECK_TIMEOUT` (default `5s`)
    - `AI_ENGINE_HEALTHCHECK_RETRIES` (default `3`)
    - `AI_ENGINE_HEALTHCHECK_START_PERIOD` (default `25s`)

- Archivo: `infra/.env`
  - Se agregaron valores de healthcheck.
  - Se agrego `AI_MODELS`.

- Archivo: `bpmn-ai-engine/.env`
  - Se agrego `AI_MODELS` para ejecucion local.

## Resultado esperado

- Si el primer modelo falla por saturacion/cuota, el microservicio prueba automaticamente el siguiente.
- Menos ruido en logs por `/health` al usar Docker Compose.
