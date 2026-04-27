import { Injectable, inject } from '@angular/core';
import { AuthService } from '../../../auth.service';

export interface CopilotResponse {
  message: string;
  suggestedActions: string[];
  conversationId?: string | null;
}

export interface CopilotApplyResponse {
  summary: string;
  changes: string[];
  warnings: string[];
  diagram: Record<string, unknown>;
  lanes?: Array<{
    id?: string;
    _id?: string;
    name?: string;
    color?: string;
    x?: number;
    width?: number;
    height?: number;
  }>;
}

export interface CopilotConversationMessage {
  role: 'user' | 'assistant' | 'system';
  text: string;
  timestamp?: string | null;
  suggestedActions?: string[];
}

export interface CopilotConversation {
  conversationId?: string | null;
  policyId?: string | null;
  policyName?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
  messages: CopilotConversationMessage[];
}

export interface CopilotChatOptions {
  conversationId?: string | null;
  policyId?: string | null;
  policyName?: string | null;
  context?: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class CopilotService {
  private readonly authService = inject(AuthService);
  private readonly baseUrl = '/api/copilot';
public async sendMessage(
    userText: string,
    currentDiagram: unknown,
    lanes: any[], // <--- 1. RECIBIMOS LOS CARRILES AQUÍ
    options: CopilotChatOptions = {}
  ): Promise<CopilotResponse> {
    const response = await fetch(`${this.baseUrl}/chat`, {
      method: 'POST',
      headers: this.authHeaders,
      body: JSON.stringify({
        userMessage: userText,
        currentDiagram,
        lanes, // <--- 2. LOS METEMOS EN EL JSON PARA SPRING BOOT
        context: options.context ?? null, // <--- 3. ENVIAMOS EL CONTEXTO DE ÁREAS DISPONIBLES
        conversationId: options.conversationId ?? null,
        policyId: options.policyId ?? null,
        policyName: options.policyName ?? null
      })
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(`Copilot HTTP ${response.status}: ${error || 'No se pudo obtener respuesta del copilot.'}`);
    }

    return response.json();
  }

  public async getHistoryByPolicy(policyId: string): Promise<CopilotConversation | null> {
    const safePolicyId = (policyId ?? '').trim();
    if (!safePolicyId) {
      return null;
    }

    const response = await fetch(`${this.baseUrl}/history?policyId=${encodeURIComponent(safePolicyId)}`, {
      method: 'GET',
      headers: this.authHeaders
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(`Copilot History HTTP ${response.status}: ${error || 'No se pudo cargar el historial.'}`);
    }

    const payload = await response.json() as CopilotConversation;
    return payload;
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
