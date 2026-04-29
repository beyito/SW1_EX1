# ANALISIS

## Paquete 1: Gestion de Acceso y Administracion
Se encarga de autenticacion, sesion, autorizacion por roles y administracion de empresas, areas y usuarios.

**CU que participan**: `CU01`, `CU02`, `CU03`, `CU04`, `CU15`, `CU16`.

## Paquete 2: Diseno Colaborativo de Politicas BPMN
Se encarga de crear/editar politicas BPMN, guardado del diagrama, colaboracion en tiempo real y consulta de orden de ejecucion.

**CU que participan**: `CU05`, `CU06`, `CU08`, `CU18`, `CU19`.

## Paquete 3: Ejecucion Operativa de Procesos
Se encarga de iniciar procesos, gestionar tareas (tomar, iniciar, completar) y enrutar el flujo segun reglas BPMN.

**CU que participan**: `CU09`, `CU10`, `CU11`, `CU13`, `CU23`.

## Paquete 4: Servicios de Soporte
Se encarga de adjuntos, metricas de politicas y notificaciones push (registro de token y aviso de nueva tarea).

**CU que participan**: `CU12`, `CU14`, `CU21`, `CU22`.

## Paquete 5: Integracion Inteligente Copilot IA
Se encarga de la asistencia conversacional, aplicacion automatica de cambios sobre el diagrama y normalizacion del resultado IA para mantener consistencia estructural y visual.

**CU que participan**: `CU07`, `CU17`, `CU20`.

## Diagramas de Colaboracion (Guia)
Objetivo: para cada CU representar como colaboran actor, frontend (web o mobile), backend (GraphQL/REST), motor BPMN/IA y base de datos.

Participantes recomendados por defecto:
- `Actor` (Admin, Diseñador, Cliente, RRHH, etc.)
- `Frontend` (Angular Web o Flutter Mobile)
- `Backend API` (Spring Boot: controllers/services)
- `Motor BPMN/IA` (bpmn-ai-engine cuando aplique)
- `Persistencia` (MongoDB y/o almacenamiento de archivos)
- `Push Provider` (Firebase cuando aplique)

Reglas de modelado:
- Mostrar mensaje numerado (`1`, `1.1`, `2`, etc.) entre participantes.
- Marcar decisiones de negocio relevantes (validacion de rol, estado de tarea, gateway BPMN).
- Incluir respuestas de error esperadas (401/403/404/409/500) en pasos alternos.
- Si hay evento asincrono (notificacion push, colaboracion en tiempo real), marcarlo como `async`.

## Prompts por Caso de Uso (Diagramas de Comunicacion)
Usa estos prompts en otra IA para generar el diagrama de comunicacion de cada CU sin perder contexto tecnico.

### Prompt base reutilizable
```text
Genera un Diagrama de Comunicacion UML (no secuencia) para el caso de uso {CU_ID} del sistema BPMN.
Incluye participantes: Actor, Frontend, Backend API, Servicio de Dominio, Persistencia, y servicios externos si aplica (IA, Firebase, S3).
Describe mensajes numerados (1, 1.1, 1.2...) con:
- nombre del metodo/endpoint
- payload principal
- validaciones
- respuesta esperada
- flujo alterno de error
Al final agrega: precondiciones, postcondiciones y reglas de negocio del CU.
Devuelvelo en formato PlantUML listo para renderizar.
```

### CU01-CU04, CU15, CU16 (Acceso y Administracion)
```text
Genera diagrama de comunicacion para {CU_ID} de gestion de acceso/administracion.
Contexto:
- Frontend Angular llama APIs de autenticacion y administracion.
- Backend Spring valida JWT, rol y pertenencia de empresa/area.
- Persistencia MongoDB para usuarios, empresas y areas.
Incluye operaciones tipicas: login, refresco de sesion, CRUD usuario/empresa/area, asignacion de rol, validacion de permisos.
Marca claramente respuestas 401/403/409.
```

### CU05-CU08, CU17-CU20 (Diseno BPMN Colaborativo + IA)
```text
Genera diagrama de comunicacion para {CU_ID} de diseno BPMN colaborativo.
Contexto:
- Angular Policy Designer manipula nodos, links, lanes y propiedades.
- Backend guarda politica/version y sincroniza cambios colaborativos.
- Copilot (Spring -> bpmn-ai-engine) propone/aplica cambios de diagrama.
- Persistencia guarda estructura completa (nodes, links, lanes con x,y,width,height).
Incluye: normalizacion de coordenadas, validacion de referencias huerfanas, merge de cambios y manejo de conflictos.
Marca flujos alternos de timeout/error IA y rollback seguro.
```

### CU09-CU11, CU13 (Ejecucion de procesos y tareas)
```text
Genera diagrama de comunicacion para {CU_ID} de ejecucion de procesos.
Contexto:
- Cliente mobile/web inicia tramite y consulta tareas por instancia.
- Backend crea ProcessInstance y TaskInstance segun BPMN.
- Servicios de ejecucion evaluan gateways, responsables y transiciones.
Incluye: iniciar proceso, tomar tarea, completar tarea con formData, avance de flujo, creacion de nuevas tareas.
Marca validaciones de estado (PENDING/IN_PROGRESS/COMPLETED/REJECTED) y errores de concurrencia.
```

### CU12, CU14, CU21, CU22 (Adjuntos, metricas, notificaciones)
```text
Genera diagrama de comunicacion para {CU_ID} de servicios de soporte.
Contexto:
- Frontend sube adjuntos y registra token push.
- Backend delega carga de archivos (S3 u otro storage) y persiste URL.
- Backend envia push via Firebase solo cuando corresponde por regla de negocio.
- Backend calcula metricas por politica/instancia/tarea.
Incluye: registro/actualizacion de token, filtro de destinatario, envio push async, y manejo de fallos externos.
Marca reintentos y logs de auditoria.
```

## Plantilla minima de salida esperada (PlantUML)
```text
@startuml
title CUxx - Nombre del caso de uso
left to right direction
actor Actor
participant Frontend
participant "Backend API" as API
participant "Servicio Dominio" as SVC
database MongoDB

Actor -> Frontend : 1. Accion de usuario
Frontend -> API : 1.1 endpoint(payload)
API -> SVC : 1.2 metodoDominio()
SVC -> MongoDB : 1.3 query/update
MongoDB --> SVC : 1.4 resultado
SVC --> API : 1.5 respuesta de negocio
API --> Frontend : 1.6 HTTP 200/4xx/5xx
Frontend --> Actor : 1.7 feedback UI
@enduml
```
