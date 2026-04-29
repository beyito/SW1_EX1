# Autocompletado por Voz con IA (Angular + Spring Boot + Python)

## 1) Capa Motor IA (Python)

### 1.1 System Prompt propuesto
```text
Eres un extractor estructurado de datos para formularios BPMN.
Recibirás:
1) form_schema: lista de campos con nombre y tipo.
2) voice_transcript: texto transcrito desde voz.

Tu tarea:
- Extraer solo la información explícita o inferible con alta confianza del voice_transcript.
- Mapearla a los nombres exactos de campos del form_schema.
- Respetar tipos:
  - string: texto
  - number: número
  - boolean: true/false
  - date: formato YYYY-MM-DD si es posible
  - email: correo válido
  - phone: texto del número
  - select: valor más cercano a opciones permitidas si fueron provistas

Reglas estrictas:
- Devuelve ÚNICAMENTE un JSON válido (objeto clave-valor).
- No incluyas markdown, comentarios, explicación ni texto adicional.
- No inventes campos que no existan en form_schema.
- Si un campo no puede inferirse con confianza, omítelo.
- Las claves deben coincidir exactamente con los nombres de campo del schema.
```

### 1.2 Payload sugerido hacia Python
```json
{
  "form_schema": [
    { "name": "nombreCompleto", "type": "string" },
    { "name": "edad", "type": "number" },
    { "name": "fechaNacimiento", "type": "date" }
  ],
  "voice_transcript": "Me llamo Ana Perez, tengo 32 años y nací el 10 de febrero de 1994"
}
```

### 1.3 Respuesta esperada del motor IA
```json
{
  "nombreCompleto": "Ana Perez",
  "edad": 32,
  "fechaNacimiento": "1994-02-10"
}
```

---

## 2) Capa Backend (Spring Boot Gateway)

### 2.1 DTO de entrada
`backend/src/main/java/com/politicanegocio/core/dto/VoiceFillRequestDto.java`
```java
package com.politicanegocio.core.dto;

import java.util.List;

public record VoiceFillRequestDto(
        String voiceTranscript,
        List<String> formFields
) {}
```

### 2.2 DTO de salida opcional (si quieres tipar respuesta)
`backend/src/main/java/com/politicanegocio/core/dto/VoiceFillResponseDto.java`
```java
package com.politicanegocio.core.dto;

import java.util.Map;

public record VoiceFillResponseDto(
        Map<String, Object> values
) {}
```

### 2.3 Método en `CopilotService` (RestClient)
Agregar en `backend/src/main/java/com/politicanegocio/core/service/CopilotService.java`:

```java
public Map<String, Object> fillFormFromVoice(VoiceFillRequestDto request) {
    if (request == null || request.voiceTranscript() == null || request.voiceTranscript().isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "voiceTranscript es obligatorio.");
    }
    if (request.formFields() == null || request.formFields().isEmpty()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "formFields es obligatorio.");
    }

    String requestId = UUID.randomUUID().toString();

    Map<String, Object> payload = new HashMap<>();
    payload.put("voice_transcript", request.voiceTranscript().trim());
    payload.put("form_fields", request.formFields());

    try {
        String raw = restClient.post()
                .uri("/api/v1/agent/voice-fill")
                .header("X-Request-Id", requestId)
                .body(payload)
                .retrieve()
                .body(String.class);

        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Respuesta vacía del motor IA.");
        }

        Map<String, Object> parsed = objectMapper.readValue(raw, new com.fasterxml.jackson.core.type.TypeReference<>() {});

        // Filtro defensivo: solo claves permitidas por formFields
        Set<String> allowed = new HashSet<>(request.formFields());
        parsed.keySet().removeIf(k -> !allowed.contains(k));
        return parsed;

    } catch (ResourceAccessException ex) {
        throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Timeout/fallo conectando con motor IA.", ex);
    } catch (RestClientResponseException ex) {
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Error del motor IA: " + ex.getStatusCode(), ex);
    } catch (Exception ex) {
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno al procesar voice-fill.", ex);
    }
}
```

### 2.4 Endpoint recomendado en `CopilotController`
```java
@PostMapping("/voice-fill")
@PreAuthorize("hasAnyAuthority('COMPANY_ADMIN','FUNCTIONARY','CLIENT','SOFTWARE_ADMIN')")
public ResponseEntity<Map<String, Object>> voiceFill(@RequestBody VoiceFillRequestDto request) {
    return ResponseEntity.ok(copilotService.fillFormFromVoice(request));
}
```

---

## 3) Capa Frontend (Angular)

### 3.1 Servicio Web Speech API nativa
`frontend/src/app/shared/services/voice-recognition.service.ts`
```ts
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

declare global {
  interface Window {
    webkitSpeechRecognition: any;
    SpeechRecognition: any;
  }
}

@Injectable({ providedIn: 'root' })
export class VoiceRecognitionService {
  private recognition: any;

  isSupported(): boolean {
    return !!(window.SpeechRecognition || window.webkitSpeechRecognition);
  }

  createSession(lang = 'es-ES'): Observable<string> {
    return new Observable<string>((observer) => {
      const SR = window.SpeechRecognition || window.webkitSpeechRecognition;
      if (!SR) {
        observer.error(new Error('SpeechRecognition no soportado en este navegador.'));
        return;
      }

      this.recognition = new SR();
      this.recognition.lang = lang;
      this.recognition.continuous = false;
      this.recognition.interimResults = false;

      this.recognition.onresult = (event: any) => {
        const text = event?.results?.[0]?.[0]?.transcript ?? '';
        observer.next(text);
        observer.complete();
      };
      this.recognition.onerror = (event: any) => observer.error(new Error(event?.error || 'Error de reconocimiento'));
      this.recognition.onend = () => {};

      this.recognition.start();

      return () => {
        try { this.recognition?.stop(); } catch {}
      };
    });
  }

  stop(): void {
    try { this.recognition?.stop(); } catch {}
  }
}
```

### 3.2 Cliente Angular para endpoint backend
Ejemplo en tu `CopilotService` (frontend) o un servicio dedicado:
```ts
public async voiceFill(voiceTranscript: string, formFields: string[]): Promise<Record<string, unknown>> {
  const response = await fetch('/api/copilot/voice-fill', {
    method: 'POST',
    headers: this.authHeaders,
    body: JSON.stringify({ voiceTranscript, formFields })
  });
  if (!response.ok) {
    const err = await response.text();
    throw new Error(`VoiceFill HTTP ${response.status}: ${err}`);
  }
  return response.json();
}
```

### 3.3 Integración en componente de formulario dinámico
```ts
isRecording = false;
isAiProcessing = false;
lastTranscript = '';

constructor(
  private voiceSvc: VoiceRecognitionService,
  private copilotSvc: CopilotService // frontend
) {}

async onMicClick(): Promise<void> {
  if (!this.voiceSvc.isSupported()) {
    this.infoMessage = 'Tu navegador no soporta reconocimiento de voz.';
    return;
  }

  this.isRecording = true;
  this.miFormulario.disable({ emitEvent: false }); // bloqueo temporal visual

  this.voiceSvc.createSession('es-ES').subscribe({
    next: async (text) => {
      this.lastTranscript = text;
      this.isRecording = false;
      this.isAiProcessing = true;
      try {
        const formFields = Object.keys(this.miFormulario.controls);
        const patch = await this.copilotSvc.voiceFill(text, formFields);
        this.miFormulario.patchValue(patch, { emitEvent: false }); // SOLO autocompleta
      } catch (e) {
        this.infoMessage = `No se pudo autocompletar por voz: ${e}`;
      } finally {
        this.isAiProcessing = false;
        this.miFormulario.enable({ emitEvent: false });
      }
    },
    error: (err) => {
      this.isRecording = false;
      this.isAiProcessing = false;
      this.miFormulario.enable({ emitEvent: false });
      this.infoMessage = `Error de voz: ${err?.message || err}`;
    }
  });
}

onStopMic(): void {
  this.voiceSvc.stop();
  this.isRecording = false;
}
```

### 3.4 HTML mínimo
```html
<button type="button" (click)="onMicClick()" [disabled]="isRecording || isAiProcessing">
  {{ isRecording ? 'Escuchando...' : 'Hablar para autocompletar' }}
</button>

<button type="button" (click)="onStopMic()" [disabled]="!isRecording">Detener</button>

<div *ngIf="isAiProcessing">Procesando con IA...</div>
```

> Importante: `patchValue` no envía formulario. El botón Guardar/Enviar sigue siendo manual.

---

## 4) Impacto en Casos de Uso y Paquetes (para documentación)

### 4.1 Nuevos CU sugeridos
- **CU23 - Autocompletar formulario de tarea por voz con IA**
  - Actor: `Functionary` / `Client`
  - Resultado: formulario rellenado visualmente (sin submit automático).
- **CU24 - Confirmar y enviar formulario autocompletado**
  - Actor: `Functionary` / `Client`
  - Resultado: envío manual tras validación visual del usuario.

### 4.2 Paquete recomendado
- **Paquete 3: Ejecución Operativa de Procesos**
  - Agregar: `CU23`, `CU24`
  - Justificación: la funcionalidad ocurre en la ejecución de tareas y afecta captura de `formData`.

### 4.3 Componentes impactados
- Frontend: componente de ejecución de tarea + servicio de voz.
- Backend: `CopilotController`, `CopilotService`.
- IA: nuevo endpoint de extracción estructurada (`voice-fill`).
- Persistencia: sin cambio de esquema; reutiliza `task_instances.formData` al guardar manualmente.

---

## 5) Reglas funcionales recomendadas
1. Nunca auto-enviar el formulario tras voice-fill.
2. Filtrar en backend claves no incluidas en `formFields`.
3. Registrar trazas (`requestId`) para depuración de fallos IA.
4. Aplicar timeout estricto al motor IA.
5. Mostrar siempre al usuario el texto transcrito antes o después del patch.

---

## 6) Estado de Implementacion Real (aplicado en el codigo)

### 6.1 Archivos backend modificados
- `backend/src/main/java/com/politicanegocio/core/dto/VoiceFillRequestDto.java`
- `backend/src/main/java/com/politicanegocio/core/controller/CopilotController.java`
- `backend/src/main/java/com/politicanegocio/core/service/CopilotService.java`

### 6.2 Archivos IA (Python) modificados
- `bpmn-ai-engine/app/main.py`
  - endpoint nuevo: `POST /api/v1/agent/voice-fill`

### 6.3 Archivos frontend modificados
- `frontend/src/app/features/execution/services/voice-recognition.service.ts`
- `frontend/src/app/features/execution/services/execution.service.ts`
- `frontend/src/app/features/execution/components/task-execution/task-execution.component.ts`
- `frontend/src/app/features/execution/components/task-execution/task-execution.component.html`

### 6.4 Casos de uso y paquete
- **CU23 - Autocompletar formulario de tarea por voz con IA** (nuevo)
- **CU24 - Confirmar y enviar formulario autocompletado** (nuevo)
- **Paquete**: `Paquete 3 - Ejecucion Operativa de Procesos`
