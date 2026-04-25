# Diagnostico 502 Copilot (Local)

## Contexto observado

- Frontend mostraba:
  - `Copilot HTTP 502: No se pudo conectar con el microservicio IA...`
- FastAPI registraba requests exitosos.

Esto sugiere problema intermitente de transporte/resolucion de host en gateway o timeout puntual, no un fallo fijo del endpoint IA.

## Cambios aplicados

### Backend (Spring)

Archivo: `backend/src/main/java/com/politicanegocio/core/service/CopilotService.java`

- Se agrego trazabilidad completa por request:
  - `requestId` (UUID)
  - `aiBaseUrl`
  - `elapsedMs`
  - `rootCause` en errores de conectividad
- Se envia header `X-Request-Id` al FastAPI para correlacion de logs.
- Se mejoraron mensajes de error devueltos a frontend con:
  - `requestId`
  - `status`
  - `body` (recortado) del microservicio IA.
- Se registra log de inicializacion con la URL efectiva del microservicio.

### Config backend

Archivo: `backend/src/main/resources/application.properties`

- Se cambio default:
  - `app.ai-engine.base-url` de `http://localhost:8010`
  - a `http://127.0.0.1:8010`

Esto evita posibles problemas IPv4/IPv6 en local.

### FastAPI

Archivo: `bpmn-ai-engine/app/main.py`

- Se lee `X-Request-Id` y se incluye en logs.
- Se agrego `request_id` en errores `HTTPException detail` de etapas criticas:
  - llamada al proveedor IA
  - lectura de respuesta
  - parseo JSON

## Resultado esperado

Cuando vuelva a fallar, frontend y logs mostraran exactamente:

- `requestId`
- URL que uso backend para llamar al microservicio
- Causa raiz (`ConnectException`, timeout, etc.)
- Estado/body del error remoto (si aplica)

## Verificacion

- Backend compila OK (`mvnw -q -DskipTests compile`)
- Python microservicio compila OK (`compileall`)
