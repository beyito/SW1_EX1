import { Injectable, inject } from '@angular/core';
import { AuthService } from '../../../auth.service';
import { DynamicReportPlan, PredictiveAnalysis } from '../models/predictive-analysis.models';

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

  public async planDynamicReport(prompt: string): Promise<DynamicReportPlan> {
    const response = await fetch('/api/reports/dynamic/plan', {
      method: 'POST',
      headers: this.authHeaders,
      body: JSON.stringify({ prompt })
    });

    if (!response.ok) {
      throw new Error(await this.errorMessage(response, 'No se pudo preparar el reporte dinamico.'));
    }

    return (await response.json()) as DynamicReportPlan;
  }

  public async downloadDynamicReport(prompt: string): Promise<void> {
    const response = await fetch('/api/reports/dynamic/download', {
      method: 'POST',
      headers: this.authHeaders,
      body: JSON.stringify({ prompt })
    });

    if (!response.ok) {
      throw new Error(await this.errorMessage(response, 'No se pudo descargar el reporte dinamico.'));
    }

    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = this.fileNameFrom(response.headers.get('Content-Disposition')) || 'reporte-dinamico';
    link.click();
    window.URL.revokeObjectURL(url);
  }

  private async errorMessage(response: Response, fallback: string): Promise<string> {
    if (response.status === 401) {
      return 'Tu sesion expiro. Vuelve a iniciar sesion.';
    }
    if (response.status === 403) {
      return 'Solo los administradores de empresa pueden generar reportes.';
    }
    const errorText = await response.text();
    return errorText || fallback;
  }

  private fileNameFrom(disposition: string | null): string {
    const match = /filename="?([^"]+)"?/i.exec(disposition || '');
    return match?.[1] || '';
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
