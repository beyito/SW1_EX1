export type PredictionPriorityLevel = 'BAJA' | 'MEDIA' | 'ALTA' | 'CRITICA' | string;

export interface PredictionAnomaly {
  type?: string;
  processInstanceId?: string;
  processTitle?: string;
  taskId?: string;
  taskLabel?: string;
  laneId?: string;
  laneName?: string;
  documentId?: string;
  documentName?: string;
  score?: number;
  reason?: string;
}

export interface PredictionPriority {
  processInstanceId?: string;
  processTitle?: string;
  policyId?: string;
  policyName?: string;
  priority?: PredictionPriorityLevel;
  score?: number;
  reason?: string;
}

export interface PredictionRoute {
  currentTaskId?: string;
  currentTaskLabel?: string;
  predictedNextTaskId?: string;
  predictedNextTaskLabel?: string;
  probability?: number;
  support?: number;
  reason?: string;
}

export interface PredictionBottleneck {
  laneId?: string;
  laneName?: string;
  risk?: number;
  pendingTasks?: number;
  avgWaitMinutes?: number;
  reason?: string;
}

export interface PredictiveAnalysis {
  modelStrategy: string;
  anomalies: PredictionAnomaly[];
  priorities: PredictionPriority[];
  routePredictions: PredictionRoute[];
  bottlenecks: PredictionBottleneck[];
  warnings: string[];
}

export interface DynamicReportPlan {
  complete: boolean;
  missingFields: string[];
  question: string;
  dataScope: string;
  criteria: Record<string, unknown>;
  format: string;
  title: string;
  summary: string;
  rows: Record<string, unknown>[];
  warnings: string[];
}
