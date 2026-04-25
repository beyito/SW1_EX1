# Fix WebSocket NXDOMAIN en local

## Problema

El backend intentaba resolver `rabbitmq` como host de relay STOMP:

- En Docker: funciona (DNS interno de la red `bpmn-net`)
- En local sin Docker: falla con `NXDOMAIN`

Esto rompía WebSocket y aparecían trazas de Netty DNS.

## Cambios aplicados

### 1) Modo de broker configurable

Archivo:
- `backend/src/main/java/com/politicanegocio/core/config/WebSocketConfig.java`

Se agregó:
- `app.websocket.use-relay` (boolean)
- Si `true`: usa `enableStompBrokerRelay(...)` (RabbitMQ relay)
- Si `false`: usa `enableSimpleBroker(...)` (sin RabbitMQ, ideal local)

### 2) Defaults para local

Archivo:
- `backend/src/main/resources/application.properties`

Cambios:
- `spring.rabbitmq.host` default ahora `127.0.0.1`
- Nuevo:
  - `app.websocket.use-relay=${WEBSOCKET_USE_RELAY:false}`

Con esto local arranca sin depender de DNS `rabbitmq`.

### 3) Docker mantiene relay

Archivos:
- `infra/.env`
- `infra/docker-compose.yml`

Cambios:
- `WEBSOCKET_USE_RELAY=true` en `infra/.env`
- Variable inyectada al servicio backend en `docker-compose.yml`

## Resultado

- Local (sin Docker): WebSocket funciona con broker simple.
- Docker/producción: WebSocket sigue funcionando con relay RabbitMQ.

## Verificación

- Backend compila OK (`mvnw -q -DskipTests compile`).
