import { Injectable, inject } from '@angular/core';
import { AuthService } from '../../../auth.service';
import { PredictiveAnalysis } from '../models/predictive-analysis.models';

@Injectable({ providedIn: 'root' })
export class PredictiveAnalyticsService {
  private readonly authService = inject(AuthService);

  public async getCompanyAnalysis(): Promise<PredictiveAnalysis> {
    const response = await fetch('/api/predictions/analysis', {
      method: 'GET',
      headers: this.authHeaders
    });

    if (!response.ok) {
      if (response.status === 401) {
        throw new Error('Tu sesión expiró. Vuelve a iniciar sesión para consultar predicciones.');
      }
      if (response.status === 403) {
        throw new Error('Solo los administradores de empresa pueden consultar estas predicciones.');
      }
      const errorText = await response.text();
      throw new Error(errorText || 'No se pudo cargar el análisis predictivo.');
    }

    return (await response.json()) as PredictiveAnalysis;
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
