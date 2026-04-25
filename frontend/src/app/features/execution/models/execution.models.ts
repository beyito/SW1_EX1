export type ProcessStatus = 'ACTIVE' | 'COMPLETED' | 'CANCELLED';
export type TaskStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'REJECTED';

export interface ProcessInstance {
  id: string;
  policyId: string;
  title?: string;
  description?: string;
  status: ProcessStatus;
  startedBy: string;
  startedAt: string;
  completedAt?: string | null;
}

export interface TaskInstance {
  id: string;
  processInstanceId: string;
  taskId: string;
  laneId: string;
  status: TaskStatus;
  assignedTo?: string | null;
  formData?: string | null;
  createdAt: string;
  completedAt?: string | null;
}

export interface PendingTaskDto {
  taskInstanceId: string;
  processInstanceId: string;
  policyId: string;
  processName: string;
  taskId: string;
  taskName: string;
  laneId: string;
  status: TaskStatus;
  createdAt: string;
}

export interface StartablePolicyDto {
  id: string;
  name: string;
  description?: string;
}

export interface ProcessTaskDto {
  taskInstanceId: string;
  taskId: string;
  taskName: string;
  laneId?: string;
  status: TaskStatus;
  createdAt?: string | null;
  completedAt?: string | null;
}

export interface ProcessTaskGroupDto {
  processInstanceId: string;
  policyId: string;
  processTitle: string;
  processDescription?: string;
  processStatus: ProcessStatus;
  startedAt?: string | null;
  completedAt?: string | null;
  tasks: ProcessTaskDto[];
}

export interface TaskFormField {
  id?: string;
  name?: string;
  type: 'text' | 'number' | 'select' | 'checkbox' | 'boolean' | 'date' | 'textarea' | 'file';
  label: string;
  required?: boolean;
  options?: string[];
  placeholder?: string;
}

export interface TaskDetailDto {
  id: string;
  policyId?: string;
  taskId?: string;
  taskName: string;
  processName: string;
  status: TaskStatus;
  description: string;
  formSchema: string;
  formData?: string | null;
  diagramJson?: string;
}
