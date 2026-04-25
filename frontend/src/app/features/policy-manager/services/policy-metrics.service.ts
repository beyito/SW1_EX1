import { Injectable, inject } from '@angular/core';
import { AuthService } from '../../../auth.service';
import { PolicyTaskMetricDto } from '../models/policy-metrics.model';

@Injectable({
  providedIn: 'root'
})
export class PolicyMetricsService {
  private readonly authService = inject(AuthService);
  private readonly baseUrl = '/api/metrics';

  public async getPolicyMetrics(policyId: string): Promise<PolicyTaskMetricDto[]> {
    const response = await fetch(`${this.baseUrl}/policy/${encodeURIComponent(policyId)}`, {
      headers: this.authHeaders
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || 'No se pudieron cargar las metricas de la politica');
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
