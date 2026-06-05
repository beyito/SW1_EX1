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
  startedAt?: string | null;
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
  initialRequirements?: PolicyInitialRequirement[];
}

export interface PolicyInitialRequirement {
  id: string;
  name: string;
  description?: string;
  required: boolean;
  allowedExtensions?: string[];
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
  requiresAttachment?: boolean;
  attachmentLabel?: string;
  options?: string[];
  placeholder?: string;
}

export interface TaskDetailDto {
  id: string;
  processInstanceId?: string;
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

export interface DocumentDto {
  id: string;
  processInstanceId: string;
  documentCode?: string;
  fileName: string;
  contentType: string;
  size: number;
  url: string;
  createdAt?: string;
  updatedAt?: string;
  canEdit?: boolean;
  canDelete?: boolean;
}

export interface OnlyOfficeConfigDto {
  documentServerUrl: string;
  config: Record<string, unknown>;
}
