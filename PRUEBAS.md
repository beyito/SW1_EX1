# Flujo de Trabajo: Pruebas

## 1. Tipos de prueba que se aplicarán

### 1.1 Pruebas de Caja Negra
Las pruebas de caja negra validan el comportamiento observable del sistema sin analizar su implementación interna.  
Se enfocan en entradas, salidas, reglas de negocio y manejo de errores desde la perspectiva del usuario o consumidor de API.

### 1.2 Pruebas de Caja Blanca
Las pruebas de caja blanca validan la lógica interna del código (ramas, condiciones, validaciones, excepciones, flujos alternos).  
Se enfocan en métodos, decisiones de control y cobertura de caminos de ejecución.

---

## 2. Tabla de Casos de Prueba - Caja Negra

| ID de Prueba | Caso de Uso | Objetivo | Precondiciones | Pasos de Ejecución | Resultado Esperado | Resultado Obtenido | Estado |
|---|---|---|---|---|---|---|---|
| CP-01 | CU06 | Verificar rechazo de guardado con versión obsoleta en edición colaborativa. | UserA y UserB editan la misma política en paralelo. | 1) UserA guarda cambio (nueva versión). 2) UserB intenta guardar snapshot antiguo. | El sistema rechaza el guardado obsoleto y obliga a sincronización/recarga de estado. | Pendiente de ejecución | ⏳ |
| CP-02 | CU06 | Verificar que no se creen enlaces duplicados entre los mismos nodos. | Política abierta en diseñador. | 1) Crear enlace A->B. 2) Intentar crear nuevamente A->B. | El sistema bloquea el segundo enlace y muestra mensaje de validación. | Pendiente de ejecución | ⏳ |
| CP-03 | CU06 | Verificar auto-guardado exitoso de cambios del lienzo. | Política activa y usuario con rol COMPANY_ADMIN. | 1) Mover nodo. 2) Esperar ventana de auto-guardado. 3) Recargar política. | El cambio persiste en `policies.diagramJson` y se visualiza tras recarga. | Pendiente de ejecución | ⏳ |
| CP-04 | CU07 | Verificar aplicación automática de cambios por Copilot cuando hay intención de mutación. | Servicio Copilot disponible y política cargada. | 1) Enviar instrucción de cambio (“agrega tarea…”). 2) Esperar respuesta apply. | El diagrama se actualiza, normaliza y guarda sin confirmación manual obligatoria. | Pendiente de ejecución | ⏳ |
| CP-05 | CU07 | Verificar manejo de timeout/fallo del servicio IA. | Forzar indisponibilidad de `bpmn-ai-engine`. | 1) Enviar instrucción al Copilot. | El frontend recibe error controlado y no se corrompe el diagrama actual. | Pendiente de ejecución | ⏳ |
| CP-06 | CU11 | Verificar validación de campos obligatorios en completar tarea. | Tarea `IN_PROGRESS` con formulario requerido. | 1) Enviar formulario incompleto. | El sistema rechaza completar tarea y devuelve error de validación. | Pendiente de ejecución | ⏳ |
| CP-07 | CU11 | Verificar enrutamiento por decisión BPMN. | Tarea con nodo DECISION y rutas condicionadas. | 1) Completar tarea con variable que activa rama válida. | Se crean tareas de la ruta correspondiente y avanza el proceso. | Pendiente de ejecución | ⏳ |
| CP-08 | CU19 | Verificar convergencia visual por full-sync en colaboradores remotos. | Dos clientes suscritos al mismo `policyId`. | 1) UserA hace cambio estructural de carriles. 2) UserB observa actualización remota. | UserB recibe evento y ambos clientes convergen al mismo estado visual. | Pendiente de ejecución | ⏳ |
| CP-09 | CU12 | Verificar subida de adjunto válida y almacenamiento de URL. | Usuario autenticado y archivo permitido. | 1) Subir archivo desde formulario de tarea. | El sistema retorna metadatos/URL y permite asociarlo al formData. | Pendiente de ejecución | ⏳ |
| CP-10 | CU21/CU22 | Verificar registro de token y notificación push de nueva tarea. | Cliente móvil autenticado, FCM configurado. | 1) Registrar token. 2) Generar tarea pendiente para el usuario. | Se guarda token y se emite notificación push al dispositivo objetivo. | Pendiente de ejecución | ⏳ |

---

## 3. Tabla de Casos de Prueba - Caja Blanca

| ID de Prueba | Módulo/Clase | Objetivo de Código | Método(s) bajo prueba | Caminos internos a cubrir | Resultado Esperado | Resultado Obtenido | Estado |
|---|---|---|---|---|---|---|---|
| WB-01 | `PolicyService` | Validar recalculo de `laneId` al guardar diagrama. | `updatePolicyGraph(...)`, `determineLaneId(...)` | Nodo dentro de carril A, nodo dentro de carril B, fallback `default`. | Los nodos quedan con `laneId` consistente según geometría. | Pendiente | ⏳ |
| WB-02 | `PolicyService` | Validar control de acceso por empresa en política. | `getPolicyById(...)`, `updatePolicyGraph(...)` | Empresa coincide / empresa no coincide. | Si no coincide, lanza excepción de acceso denegado. | Pendiente | ⏳ |
| WB-03 | `CopilotService` backend | Validar parseo robusto de respuesta IA. | `parseChatResponse(...)`, `parseApplyResponse(...)` | JSON válido, JSON vacío, JSON malformado. | Solo JSON válido continúa; casos inválidos generan error controlado. | Pendiente | ⏳ |
| WB-04 | `CopilotService` backend | Validar manejo de errores de conectividad IA. | `chat(...)`, `apply(...)` | Timeout, `ResourceAccessException`, `RestClientResponseException`. | Se transforma a `GATEWAY_TIMEOUT` o `BAD_GATEWAY` según corresponda. | Pendiente | ⏳ |
| WB-05 | `DiagramAgentService` (Python) | Verificar merge destructivo/no destructivo del diagrama. | `process(...)`, `_is_destructive_instruction(...)`, `_merge_diagrams(...)` | Instrucción destructiva y no destructiva. | Destructiva reemplaza base; no destructiva fusiona celdas por ID. | Pendiente | ⏳ |
| WB-06 | `DiagramAgentService` (Python) | Verificar fallback cuando IA falla. | `process(...)`, `_fallback_result(...)` | Excepción en `_run_llm(...)`. | Retorna diagrama base saneado con warning de error IA. | Pendiente | ⏳ |
| WB-07 | `WorkflowEngine` | Verificar resolución de rutas de decisión. | `getNextNodes(...)`, `resolveDecisionTargets(...)` | Match por expresión, fallback `default`, sin match. | Se retorna la(s) ruta(s) esperada(s) por reglas BPMN. | Pendiente | ⏳ |
| WB-08 | `WebSocketService` + `DesignerSocketController` | Verificar contrato de publicación/suscripción STOMP. | `sendMessage(...)`, `handlePolicyChange(...)` | Destino publish correcto y topic correcto. | Evento publicado a `/app/policy/{id}/change` y reenviado a `/topic/policy.{id}`. | Pendiente | ⏳ |
| WB-09 | `ProcessExecutionService` | Verificar transiciones de estado de tarea. | `takeTask(...)`, `startTask(...)`, `completeTask(...)` | `PENDING -> IN_PROGRESS -> COMPLETED` y casos inválidos. | Transiciones válidas avanzan; inválidas se rechazan con error. | Pendiente | ⏳ |
| WB-10 | `FirebaseMessagingService` | Verificar envío condicional de push. | `sendTaskAssignedNotification(...)` | Token vacío, Firebase no inicializado, envío exitoso/fallido. | Sin token/no init: no rompe flujo; éxito/fallo queda en log sin interrumpir BPMN. | Pendiente | ⏳ |

---

## 4. Plantilla de Registro de Evidencia

Para cada prueba ejecutada se debe adjuntar:
- Fecha y entorno de ejecución.
- Datos de entrada utilizados.
- Capturas/logs (frontend, backend, IA, broker).
- Resultado obtenido real.
- Comparación contra resultado esperado.
- Estado final: `✅ EXITOSO`, `❌ FALLIDO`, `⚠️ OBSERVADO`.

---

## 5. Criterios de Aceptación Global

Se considera que el sistema cumple la fase de pruebas cuando:
1. Los casos críticos CU06, CU07, CU11 y CU19 estén en estado `✅ EXITOSO`.
2. No existan defectos bloqueantes en colaboración en tiempo real, guardado y ejecución de workflow.
3. Los errores de IA y conectividad se manejen de forma controlada sin pérdida de consistencia del diagrama.

---

## 6. Fichas de Casos de Prueba (Formato Atributo/Descripción)

### Caso de Prueba CP-01 (Caja Negra)
| Atributo | Descripción |
|---|---|
| ID de Caso de Prueba | CP-01 (Asociado a CU06) |
| Objetivo | Verificar rechazo de guardado con versión obsoleta en edición colaborativa. |
| Precondiciones | UserA y UserB editan la misma política y UserA guarda antes que UserB. |
| Pasos de Ejecución | 1) UserA modifica y guarda. 2) UserB intenta guardar snapshot obsoleto. |
| Resultado Esperado | El sistema rechaza el guardado obsoleto y fuerza resincronización/recarga. |
| Resultado Obtenido | Pendiente de ejecución. |
| Estado | ⏳ PENDIENTE |

### Caso de Prueba CP-02 (Caja Negra)
| Atributo | Descripción |
|---|---|
| ID de Caso de Prueba | CP-02 (Asociado a CU06) |
| Objetivo | Validar que no se creen enlaces duplicados entre los mismos nodos. |
| Precondiciones | Política abierta en diseñador con nodos A y B existentes. |
| Pasos de Ejecución | 1) Crear enlace A->B. 2) Repetir creación A->B. |
| Resultado Esperado | El segundo enlace se rechaza y se informa validación en UI. |
| Resultado Obtenido | Pendiente de ejecución. |
| Estado | ⏳ PENDIENTE |

### Caso de Prueba CP-03 (Caja Negra)
| Atributo | Descripción |
|---|---|
| ID de Caso de Prueba | CP-03 (Asociado a CU06) |
| Objetivo | Verificar auto-guardado de cambios del lienzo BPMN. |
| Precondiciones | Usuario COMPANY_ADMIN con política activa. |
| Pasos de Ejecución | 1) Mover nodo. 2) Esperar auto-guardado. 3) Recargar política. |
| Resultado Esperado | El cambio persiste en `policies.diagramJson`. |
| Resultado Obtenido | Pendiente de ejecución. |
| Estado | ⏳ PENDIENTE |

### Caso de Prueba CP-04 (Caja Negra)
| Atributo | Descripción |
|---|---|
| ID de Caso de Prueba | CP-04 (Asociado a CU07) |
| Objetivo | Verificar aplicación automática de cambios por Copilot IA. |
| Precondiciones | Servicio IA disponible y política cargada. |
| Pasos de Ejecución | 1) Enviar instrucción de mutación. 2) Esperar apply + guardado. |
| Resultado Esperado | Se aplica, normaliza, persiste y sincroniza el nuevo diagrama. |
| Resultado Obtenido | Pendiente de ejecución. |
| Estado | ⏳ PENDIENTE |

### Caso de Prueba CP-05 (Caja Negra)
| Atributo | Descripción |
|---|---|
| ID de Caso de Prueba | CP-05 (Asociado a CU07) |
| Objetivo | Verificar manejo de timeout/fallo del servicio IA. |
| Precondiciones | `bpmn-ai-engine` no disponible o con latencia extrema. |
| Pasos de Ejecución | 1) Enviar instrucción Copilot. |
| Resultado Esperado | Error controlado en UI, sin corromper diagrama actual. |
| Resultado Obtenido | Pendiente de ejecución. |
| Estado | ⏳ PENDIENTE |

### Caso de Prueba CP-06 (Caja Negra)
| Atributo | Descripción |
|---|---|
| ID de Caso de Prueba | CP-06 (Asociado a CU11) |
| Objetivo | Verificar validación de campos obligatorios en completar tarea. |
| Precondiciones | Tarea `IN_PROGRESS` con formulario requerido. |
| Pasos de Ejecución | 1) Enviar formulario incompleto. |
| Resultado Esperado | Se rechaza `completeTask` con error de validación. |
| Resultado Obtenido | Pendiente de ejecución. |
| Estado | ⏳ PENDIENTE |

### Caso de Prueba CP-07 (Caja Negra)
| Atributo | Descripción |
|---|---|
| ID de Caso de Prueba | CP-07 (Asociado a CU11) |
| Objetivo | Verificar enrutamiento por decisión BPMN. |
| Precondiciones | Nodo DECISION con rutas condicionales configuradas. |
| Pasos de Ejecución | 1) Completar tarea con variable que activa una rama. |
| Resultado Esperado | El proceso avanza por la rama correcta y crea tareas siguientes. |
| Resultado Obtenido | Pendiente de ejecución. |
| Estado | ⏳ PENDIENTE |

### Caso de Prueba CP-08 (Caja Negra)
| Atributo | Descripción |
|---|---|
| ID de Caso de Prueba | CP-08 (Asociado a CU19) |
| Objetivo | Verificar convergencia visual en full-sync colaborativo. |
| Precondiciones | Dos clientes suscritos al mismo `policyId`. |
| Pasos de Ejecución | 1) UserA cambia layout de carriles. 2) UserB recibe sync. |
| Resultado Esperado | Ambos clientes convergen al mismo estado visual. |
| Resultado Obtenido | Pendiente de ejecución. |
| Estado | ⏳ PENDIENTE |

### Caso de Prueba CP-09 (Caja Negra)
| Atributo | Descripción |
|---|---|
| ID de Caso de Prueba | CP-09 (Asociado a CU12) |
| Objetivo | Verificar carga de adjunto y asociación en formulario. |
| Precondiciones | Usuario autenticado y archivo válido. |
| Pasos de Ejecución | 1) Subir archivo en tarea. 2) Guardar referencia en formulario. |
| Resultado Esperado | URL/metadato disponible y vinculada al `formData`. |
| Resultado Obtenido | Pendiente de ejecución. |
| Estado | ⏳ PENDIENTE |

### Caso de Prueba CP-10 (Caja Negra)
| Atributo | Descripción |
|---|---|
| ID de Caso de Prueba | CP-10 (Asociado a CU21/CU22) |
| Objetivo | Verificar registro de token FCM y recepción de push por nueva tarea. |
| Precondiciones | Cliente móvil autenticado con permisos de notificación. |
| Pasos de Ejecución | 1) Registrar token. 2) Generar nueva tarea para el usuario. |
| Resultado Esperado | Token persistido y push enviado al dispositivo objetivo. |
| Resultado Obtenido | Pendiente de ejecución. |
| Estado | ⏳ PENDIENTE |
