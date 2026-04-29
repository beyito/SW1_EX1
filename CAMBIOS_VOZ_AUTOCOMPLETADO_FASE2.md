# Cambios Fase 2 - Voz a Formulario (Control Manual de Envío IA)

## Objetivo del ajuste
Se corrigió el flujo de autocompletado por voz para:
1. Evitar envío automático a IA inmediatamente después de grabar.
2. Permitir revisión/edición de la transcripción antes de consumir tokens IA.
3. Mejorar manejo de errores de reconocimiento (caso frecuente: `network`).

## Problema reportado
- Independientemente de lo dicho al micrófono, aparecía el error/valor asociado a `network`.
- Se necesitaba un control explícito para no enviar texto a IA hasta confirmación del usuario.

## Cambios implementados

### 1) Frontend - Componente de ejecución de tarea
Archivo:
- `frontend/src/app/features/execution/components/task-execution/task-execution.component.ts`
- `frontend/src/app/features/execution/components/task-execution/task-execution.component.html`

Cambios:
- Se separó el proceso en dos acciones:
  - `startVoiceAutofill()` => solo captura transcripción.
  - `sendTranscriptToAi()` => envía manualmente transcripción a IA.
- Se agregó estado:
  - `pendingTranscript` (texto editable previo al envío IA).
  - `isRecording`, `isAiProcessing`.
- Se mantiene `patchValue(...)` para rellenar formulario sin submit automático.
- Se bloquea el formulario únicamente durante el procesamiento IA.

UI agregada:
- Botón `Autocompletar por voz`.
- Botón `Detener`.
- Botón `Enviar a la IA`.
- Textarea editable para revisar transcripción antes de envío.

### 2) Frontend - Servicio de reconocimiento de voz
Archivo:
- `frontend/src/app/features/execution/services/voice-recognition.service.ts`

Cambios:
- Mapeo de errores del Web Speech API a mensajes comprensibles:
  - `network`, `not-allowed`, `no-speech`, `audio-capture`, `aborted`.
- Mensajes orientados a diagnóstico (conexión, permisos y HTTPS).

## Impacto funcional
- Se reduce gasto de tokens IA: ya no hay invocación automática tras grabar.
- El usuario mantiene control:
  1) graba,
  2) revisa/edita texto,
  3) decide enviar a IA.
- Se conserva el principio de validación manual:
  - IA solo hace `patchValue`.
  - El envío final del formulario sigue siendo manual.

## Archivos modificados en esta fase
- `frontend/src/app/features/execution/components/task-execution/task-execution.component.ts`
- `frontend/src/app/features/execution/components/task-execution/task-execution.component.html`
- `frontend/src/app/features/execution/services/voice-recognition.service.ts`

