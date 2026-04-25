import { Injectable, inject } from '@angular/core';
import { AuthService } from '../../../auth.service';

export interface CopilotResponse {
  message: string;
  suggestedActions: string[];
}

export interface CopilotApplyResponse {
  summary: string;
  changes: string[];
  warnings: string[];
  diagram: Record<string, unknown>;
}

@Injectable({
  providedIn: 'root'
})
export class CopilotService {
  private readonly authService = inject(AuthService);
  private readonly baseUrl = '/api/copilot';

  public async sendMessage(userText: string, currentDiagram: unknown): Promise<CopilotResponse> {
    const response = await fetch(`${this.baseUrl}/chat`, {
      method: 'POST',
      headers: this.authHeaders,
      body: JSON.stringify({
        userMessage: userText,
        currentDiagram
      })
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(`Copilot HTTP ${response.status}: ${error || 'No se pudo obtener respuesta del copilot.'}`);
    }

    return response.json();
  }

  public async applyChange(
    instruction: string,
    currentDiagram: unknown,
    lanes: unknown[],
    context: Record<string, unknown>
  ): Promise<CopilotApplyResponse> {
    const response = await fetch(`${this.baseUrl}/apply`, {
      method: 'POST',
      headers: this.authHeaders,
      body: JSON.stringify({
        instruction,
        currentDiagram,
        lanes,
        context
      })
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(`Copilot Apply HTTP ${response.status}: ${error || 'No se pudo aplicar el cambio.'}`);
    }
    return response.json();
  }

  private get authHeaders(): Record<string, string> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    const token = this.authService.getToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
    return headers;
  }
}
