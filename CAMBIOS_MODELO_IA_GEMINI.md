# Cambio de modelo IA a Gemini 3.1 Flash Lite

## Cambios aplicados

Se actualizo `bpmn-ai-engine` para usar configuracion de proveedor IA generica (`AI_*`) y dejar como modelo por defecto:

- `gemini-3.1-flash-lite`

### Archivos modificados

- `bpmn-ai-engine/app/config.py`
  - Se renombro configuracion a:
    - `ai_api_key`
    - `ai_model`
    - `ai_timeout_seconds`
    - `ai_base_url`
  - Compatibilidad retroactiva:
    - Si no existe `AI_*`, usa fallback `OPENAI_*`.

- `bpmn-ai-engine/app/agent_service.py`
  - Usa `settings.ai_*`.
  - Mensajes de fallback actualizados a `AI_API_KEY`.

- `bpmn-ai-engine/app/main.py`
  - Cliente IA usa `settings.ai_*`.
  - Health expone `settings.ai_model`.
  - Copilot usa `settings.ai_model`.

- `bpmn-ai-engine/.env`
  - Nuevas variables:
    - `AI_API_KEY`
    - `AI_MODEL=gemini-3.1-flash-lite`
    - `AI_TIMEOUT_SECONDS`
    - `AI_BASE_URL=https://generativelanguage.googleapis.com/v1beta/openai/`

- `bpmn-ai-engine/README.md`
  - Documentacion de nuevas variables `AI_*`.
  - Nota de compatibilidad con `OPENAI_*`.

## Verificacion

- Compilacion de modulos Python (`compileall`) OK.
