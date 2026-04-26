# Dockerización `bpmn-ai-engine`

## Objetivo

Preparar el microservicio FastAPI `bpmn-ai-engine` para ejecutarse en Docker y quedar integrado al stack principal.

## Cambios realizados

### 1) Imagen Docker del microservicio

Archivo creado:

- `bpmn-ai-engine/Dockerfile`

Incluye:

- Base `python:3.11-slim`
- Instalación de dependencias desde `requirements.txt`
- Copia de `app/`
- Exposición de puerto `8010`
- Comando de arranque:
  - `uvicorn app.main:app --host 0.0.0.0 --port 8010`

### 2) Exclusiones de build

Archivo creado:

- `bpmn-ai-engine/.dockerignore`

Excluye:

- `venv`, caches Python, `__pycache__`
- `.env*` para no inyectar secretos al build context

### 3) Integración en Docker Compose

Archivo actualizado:

- `infra/docker-compose.yml`

Se agregó servicio:

- `bpmn-ai-engine`
  - build con `bpmn-ai-engine/Dockerfile`
  - puerto `8010:8010`
  - variables de entorno IA
  - `healthcheck` sobre `GET /health`

Se actualizó backend para consumir IA vía red interna:

- `AI_ENGINE_BASE_URL=${AI_ENGINE_BASE_URL}`
- `depends_on` con healthcheck de `bpmn-ai-engine`

### 4) Variables de entorno para compose

Archivo actualizado:

- `infra/.env`

Nuevas variables:

- `AI_ENGINE_BASE_URL=http://bpmn-ai-engine:8010`
- `AI_ENGINE_APP_NAME`, `AI_ENGINE_APP_VERSION`
- `AI_ENGINE_HOST`, `AI_ENGINE_PORT`
- `AI_ENGINE_RELOAD`, `AI_ENGINE_LOG_LEVEL`
- `AI_ENGINE_ALLOWED_ORIGINS`
- `AI_API_KEY`, `AI_MODEL`, `AI_TIMEOUT_SECONDS`, `AI_BASE_URL`

### 5) Documentación

Archivos actualizados:

- `bpmn-ai-engine/README.md` (sección Docker: build/run/compose)
- `ARCHITECTURE.md` (servicio y routing interno en Docker)

## Uso

Desde `infra/`:

```bash
docker compose up --build
```

Con esto:

- FastAPI IA queda en `http://localhost:8010`
- Backend consume IA por `http://bpmn-ai-engine:8010` dentro de la red Docker
