import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import {
  PredictiveAnalysis,
  PredictionAnomaly,
  PredictionBottleneck,
  PredictionPriority,
  PredictionRoute
} from '../../models/predictive-analysis.models';
import { PredictiveAnalyticsService } from '../../services/predictive-analytics.service';

@Component({
  selector: 'app-predictive-analytics',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './predictive-analytics.component.html',
  styleUrl: './predictive-analytics.component.scss'
})
export class PredictiveAnalyticsComponent implements OnInit {
  private readonly predictiveAnalyticsService = inject(PredictiveAnalyticsService);
  private readonly cdr = inject(ChangeDetectorRef);

  public analysis: PredictiveAnalysis | null = null;
  public loading = false;
  public message = '';

  public async ngOnInit(): Promise<void> {
    await this.loadAnalysis();
  }

  public async loadAnalysis(): Promise<void> {
    this.loading = true;
    this.message = '';
    this.cdr.detectChanges();

    try {
      this.analysis = await this.predictiveAnalyticsService.getCompanyAnalysis();
    } catch (error) {
      this.analysis = null;
      this.message = error instanceof Error ? error.message : 'No se pudo cargar el análisis predictivo.';
    } finally {
      this.loading = false;
      this.cdr.detectChanges();
    }
  }

  public get hasData(): boolean {
    if (!this.analysis) {
      return false;
    }
    return this.totalFindings > 0;
  }

  public get totalFindings(): number {
    if (!this.analysis) {
      return 0;
    }
    return this.analysis.anomalies.length
      + this.analysis.priorities.length
      + this.analysis.routePredictions.length
      + this.analysis.bottlenecks.length;
  }

  public priorityClass(priority?: string): string {
    const normalized = (priority || '').toLowerCase();
    if (normalized.includes('critica') || normalized.includes('crítica')) {
      return 'critical';
    }
    if (normalized.includes('alta')) {
      return 'high';
    }
    if (normalized.includes('media')) {
      return 'medium';
    }
    return 'low';
  }

  public asPercent(value?: number): string {
    if (typeof value !== 'number' || Number.isNaN(value)) {
      return 'N/D';
    }
    const normalized = value > 1 ? value : value * 100;
    return `${Math.round(normalized)}%`;
  }

  public modelStrategyLabel(strategy?: string): string {
    if (strategy === 'tensorflow_autoencoder') {
      return 'Autoencoder TensorFlow';
    }
    if (strategy === 'heuristic') {
      return 'Cálculo heurístico';
    }
    if (strategy === 'empty') {
      return 'Sin datos suficientes';
    }
    return strategy || 'Sin estrategia';
  }

  public anomalyTypeLabel(type?: string): string {
    if (type === 'TASK_DURATION_ANOMALY') {
      return 'Tiempo fuera de patrón';
    }
    if (type === 'DOCUMENT_BURST') {
      return 'Carga documental inusual';
    }
    return type ? type.replaceAll('_', ' ').toLowerCase() : 'Anomalía';
  }

  public trackAnomaly(index: number, item: PredictionAnomaly): string {
    return item.documentId || item.taskId || item.processInstanceId || `anomaly-${index}`;
  }

  public trackPriority(index: number, item: PredictionPriority): string {
    return item.processInstanceId || `priority-${index}`;
  }

  public trackRoute(index: number, item: PredictionRoute): string {
    return `${item.currentTaskId || 'task'}-${item.predictedNextTaskId || index}`;
  }

  public trackBottleneck(index: number, item: PredictionBottleneck): string {
    return item.laneId || `bottleneck-${index}`;
  }
}
