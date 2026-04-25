export interface PolicyTaskMetricDto {
  taskId: string;
  taskName: string;
  totalCompleted: number;
  avgWaitMinutes: number;
  avgWaitHours: number;
  avgExecutionMinutes: number;
  avgExecutionHours: number;
  avgTotalMinutes: number;
}
