import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { PolicyTaskMetricDto } from '../models/policy-metrics.model';
import { PolicyMetricsService } from '../services/policy-metrics.service';

@Component({
  selector: 'app-policy-metrics',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './policy-metrics.component.html',
  styleUrl: './policy-metrics.component.scss'
})
export class PolicyMetricsComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly metricsService = inject(PolicyMetricsService);
  private readonly cdr = inject(ChangeDetectorRef); // <-- INYECTAMOS EL DETECTOR DE CAMBIOS

  public policyId = '';
  public metrics: PolicyTaskMetricDto[] = [];
  public loading = false;
  public message = '';

  // El cuello de botella será siempre el primero gracias a que los ordenamos al recibirlos
  public get bottleneckTaskId(): string {
    if (this.metrics.length === 0) {
      return '';
    }
    return this.metrics[0].taskId;
  }

  public async ngOnInit(): Promise<void> {
    this.policyId = this.route.snapshot.paramMap.get('policyId') ?? '';
    await this.loadMetrics();
  }

  public async loadMetrics(): Promise<void> {
    if (!this.policyId) {
      this.metrics = [];
      this.message = 'No se encontro la politica solicitada.';
      this.cdr.detectChanges(); // Forzamos actualización visual
      return;
    }

    this.loading = true;
    this.message = '';
    this.cdr.detectChanges(); // Avisamos que empezó a cargar

    try {
      this.metrics = await this.metricsService.getPolicyMetrics(this.policyId);
      
      if (this.metrics.length === 0) {
        this.message = 'No hay datos suficientes para calcular metricas en esta politica.';
      } else {
        // MEJORA: Ordenamos de mayor a menor tiempo total para asegurar que el [0] sea el cuello de botella
        this.metrics.sort((a, b) => b.avgTotalMinutes - a.avgTotalMinutes);
      }
    } catch (error) {
      this.metrics = [];
      this.message = error instanceof Error ? error.message : 'No se pudieron cargar las metricas.';
    } finally {
      this.loading = false;
      this.cdr.detectChanges(); // <-- LA SOLUCIÓN: Obligamos a Angular a repintar la pantalla sin el loader
    }
  }

  public isBottleneck(metric: PolicyTaskMetricDto): boolean {
    return metric.taskId === this.bottleneckTaskId;
  }

  public waitBarWidth(metric: PolicyTaskMetricDto): string {
    return `${this.normalizedPercent(metric.avgWaitMinutes)}%`;
  }

  public executionBarWidth(metric: PolicyTaskMetricDto): string {
    return `${this.normalizedPercent(metric.avgExecutionMinutes)}%`;
  }

  public async goBack(): Promise<void> {
    await this.router.navigate(['/admin/policies']);
  }

  private normalizedPercent(value: number): number {
    const max = Math.max(1, ...this.metrics.map((metric) => metric.avgTotalMinutes));
    return Math.max(4, Math.round((value / max) * 100));
  }
}