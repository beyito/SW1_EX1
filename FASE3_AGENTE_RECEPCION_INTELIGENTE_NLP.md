# FASE 3: Agente de Recepcion Inteligente (NLP)

## Plan de Accion

- [archivo: bpmn-ai-engine/app/main.py] Se ajusto el endpoint NLP para devolver candidatos de tramites ordenados por confianza.
- [archivo: backend/src/main/java/com/politicanegocio/core/controller/IntelligentReceptionController.java] Se expuso el gateway REST para recomendar tramites desde lenguaje natural.
- [archivo: backend/src/main/java/com/politicanegocio/core/service/IntelligentReceptionService.java] Se implemento la coreografia Spring Boot -> FastAPI sin iniciar automaticamente el tramite.
- [archivo: backend/src/main/java/com/politicanegocio/core/dto/IntelligentReceptionRequestDto.java] Se mantiene el contrato de entrada del agente.
- [archivo: backend/src/main/java/com/politicanegocio/core/dto/IntelligentReceptionCandidateDto.java] Se agrego el contrato de cada candidato recomendado.
- [archivo: backend/src/main/java/com/politicanegocio/core/dto/IntelligentReceptionResponseDto.java] Se cambio la salida para devolver una lista de candidatos.
- [archivo: mobile/lib/features/dashboard/data/dashboard_service.dart] Se agrego el consumo movil del endpoint de recomendacion.
- [archivo: mobile/lib/features/dashboard/models/intelligent_reception_result.dart] Se actualizo el modelo movil para manejar varios candidatos.
- [archivo: mobile/lib/features/dashboard/presentation/dashboard_screen.dart] Se ajusto la tarjeta del agente para que el cliente elija un tramite recomendado.

## Modificaciones

### [archivo: bpmn-ai-engine/app/main.py]

- El endpoint `POST /api/v1/agent/policy-intent` ahora recibe el texto del cliente y el catalogo de Policies disponibles.
- Devuelve hasta 3 candidatos dentro de `candidates`.
- Cada candidato contiene:
  - `policyId`
  - `policyName`
  - `confidence`
  - `missing_requirements`
  - `reason`
- Si `AI_API_KEY` esta configurado, usa el proveedor LLM compatible con OpenAI.
- Si no hay proveedor IA o falla la llamada, usa fallback lexico local.
- La respuesta se normaliza para evitar que la IA invente IDs de Policies que no existen.

Ejemplo de respuesta:

```json
{
  "candidates": [
    {
      "policyId": "123",
      "policyName": "Reposicion de tarjeta",
      "confidence": 0.95,
      "missing_requirements": ["Carnet de Identidad"],
      "reason": "La solicitud menciona perdida de tarjeta."
    }
  ]
}
```

### [archivo: backend/src/main/java/com/politicanegocio/core/service/IntelligentReceptionService.java]

- Consulta las Policies iniciables para el usuario autenticado.
- Construye un catalogo compacto con nombre, descripcion y requisitos iniciales.
- Envia el texto y catalogo al motor FastAPI.
- Valida que cada candidato devuelto pertenezca al catalogo permitido del usuario.
- Ordena candidatos por confianza.
- No inicia ningun proceso automaticamente.
- Devuelve candidatos para que el cliente elija manualmente.
- Si FastAPI no responde, responde con candidatos calculados por fallback local para evitar errores `Bad Gateway` al usuario.

### [archivo: backend/src/main/java/com/politicanegocio/core/controller/IntelligentReceptionController.java]

- Se agrego:

```text
POST /api/execution/intelligent-reception/recommend
```

- Se mantiene `/start` como alias compatible, pero el comportamiento ya no inicia automaticamente.
- Permitido para roles:

```text
CLIENT, FUNCTIONARY, COMPANY_ADMIN, SOFTWARE_ADMIN
```

### [archivo: mobile/lib/features/dashboard/presentation/dashboard_screen.dart]

- La tarjeta `Agente de recepcion inteligente` ahora busca tramites candidatos.
- El cliente escribe o dicta su necesidad usando el teclado del celular.
- La app muestra una lista de candidatos recomendados.
- El cliente elige el tramite que vea mas conveniente.
- Al elegir un candidato, la app abre `StartProcessRequirementsScreen`.
- Desde ahi el flujo es igual al inicio manual: se muestran requisitos iniciales y se adjuntan archivos antes de iniciar.

## Casos de Uso Generados

### CU-F3-01: Solicitar Recomendacion de Tramite en Lenguaje Natural

- **Actor principal:** Cliente movil.
- **Objetivo:** encontrar tramites candidatos sin conocer el nombre exacto de la Policy.
- **Precondicion:** el cliente inicio sesion y existen Policies iniciables para su usuario.
- **Flujo principal:** el cliente escribe o dicta una necesidad, la app envia el texto al backend, Spring Boot consulta el catalogo permitido y pide recomendaciones al motor IA.
- **Resultado:** el sistema devuelve una lista de tramites candidatos.

### CU-F3-02: Clasificar Intencion contra Catalogo de Policies

- **Actor principal:** Motor IA FastAPI.
- **Objetivo:** comparar el texto del cliente contra las Policies disponibles.
- **Precondicion:** Spring Boot envio texto y catalogo de Policies.
- **Flujo principal:** FastAPI evalua la intencion con LLM o fallback lexico, calcula confianza por candidato y devuelve hasta 3 opciones.
- **Resultado:** Spring Boot recibe candidatos con `policyId`, `confidence` y `missing_requirements`.

### CU-F3-03: Elegir Tramite Recomendado

- **Actor principal:** Cliente movil.
- **Objetivo:** seleccionar el tramite mas conveniente entre los candidatos sugeridos.
- **Precondicion:** el agente devolvio uno o mas candidatos.
- **Flujo principal:** la app muestra los candidatos con confianza, razon y requisitos. El cliente selecciona uno.
- **Resultado:** la app navega al flujo normal de inicio del tramite seleccionado.

### CU-F3-04: Completar Requisitos Iniciales antes de Iniciar

- **Actor principal:** Cliente movil.
- **Objetivo:** adjuntar documentos requeridos antes de crear la instancia del tramite.
- **Precondicion:** el cliente eligio un tramite candidato.
- **Flujo principal:** la app abre la pantalla de requisitos iniciales, valida obligatorios, crea la instancia y sube documentos.
- **Resultado:** el tramite se inicia solo despues de completar los requisitos obligatorios.

## Reglas de Negocio

- El agente solo recomienda Policies que el usuario puede iniciar.
- El backend no confia ciegamente en los `policyId` devueltos por IA.
- El agente no crea instancias de proceso.
- El cliente siempre decide que tramite iniciar.
- El flujo de requisitos iniciales se mantiene como punto obligatorio antes de iniciar.
- Si el proveedor LLM no esta disponible, el motor IA usa fallback lexico para mantener continuidad operativa.
- Si el microservicio IA completo no esta disponible o devuelve una respuesta invalida, Spring Boot usa fallback local y mantiene la experiencia de recomendacion.

## Validacion

- Backend Spring Boot compila con Maven.
- App movil Flutter pasa `flutter analyze`.
- `bpmn-ai-engine/app/main.py` pasa validacion de sintaxis con `py_compile`.
- No se detectaron caracteres corruptos en `bpmn-ai-engine/app`, `backend/src` ni `mobile/lib`.
