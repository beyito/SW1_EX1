import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

declare global {
  interface Window {
    webkitSpeechRecognition?: any;
    SpeechRecognition?: any;
  }
}

@Injectable({ providedIn: 'root' })
export class VoiceRecognitionService {
  private recognition: any;

  public isSupported(): boolean {
    return !!(window.SpeechRecognition || window.webkitSpeechRecognition);
  }

  public createSession(lang = 'es-ES'): Observable<string> {
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
        const transcript = event?.results?.[0]?.[0]?.transcript ?? '';
        observer.next(transcript);
        observer.complete();
      };

      this.recognition.onerror = (event: any) => {
        const code = String(event?.error || '').trim();
        observer.error(new Error(this.mapRecognitionError(code)));
      };

      this.recognition.start();

      return () => {
        try {
          this.recognition?.stop();
        } catch {
          // noop
        }
      };
    });
  }

  public stop(): void {
    try {
      this.recognition?.stop();
    } catch {
      // noop
    }
  }

  private mapRecognitionError(code: string): string {
    switch (code) {
      case 'network':
        return 'Error de red del reconocimiento de voz. Verifica conexión, permisos de micrófono y uso en HTTPS.';
      case 'not-allowed':
      case 'service-not-allowed':
        return 'Permiso de micrófono denegado por el navegador.';
      case 'no-speech':
        return 'No se detectó voz. Intenta hablar más cerca del micrófono.';
      case 'audio-capture':
        return 'No se pudo capturar audio del micrófono.';
      case 'aborted':
        return 'Reconocimiento de voz cancelado.';
      default:
        return code ? `Error de reconocimiento de voz: ${code}` : 'Error de reconocimiento de voz.';
    }
  }
}
