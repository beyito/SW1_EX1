import { Injectable, inject } from '@angular/core';
import { AuthService } from '../../../auth.service';
import {
  PendingTaskDto,
  ProcessTaskGroupDto,
  ProcessInstance,
  StartablePolicyDto,
  TaskDetailDto,
  TaskInstance
} from '../models/execution.models';

@Injectable({
  providedIn: 'root'
})
export class ExecutionService {
  private readonly authService = inject(AuthService);
  private readonly baseUrl = '/api/execution';

  public async startProcess(policyId: string, title: string, description: string): Promise<ProcessInstance> {
    const response = await fetch(`${this.baseUrl}/process/start`, {
      method: 'POST',
      headers: this.authHeaders,
      body: JSON.stringify({ policyId, title, description })
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || 'No se pudo iniciar el proceso');
    }

    return response.json();
  }

  public async getMyPendingTasks(laneId: string): Promise<PendingTaskDto[]> {
    const response = await fetch(`${this.baseUrl}/tasks/pending/${encodeURIComponent(laneId)}`, {
      headers: this.authHeaders
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || 'No se pudieron cargar las tareas pendientes');
    }

    return response.json();
  }

  public async getStartablePolicies(): Promise<StartablePolicyDto[]> {
    const response = await fetch(`${this.baseUrl}/startable-policies`, {
      headers: this.authHeaders
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || 'No se pudieron cargar los procesos disponibles');
    }

    return response.json();
  }

  public async getMyTasks(): Promise<PendingTaskDto[]> {
    const response = await fetch(`${this.baseUrl}/my-tasks`, {
      headers: this.authHeaders
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || 'No se pudieron cargar tus tareas pendientes');
    }

    return response.json();
  }

  public async getMyProcessTaskGroups(): Promise<ProcessTaskGroupDto[]> {
    const response = await fetch(`${this.baseUrl}/my-processes/tasks`, {
      headers: this.authHeaders
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || 'No se pudieron cargar tus procesos activos');
    }

    return response.json();
  }

  public async getTaskDetails(taskInstanceId: string): Promise<TaskDetailDto> {
    const response = await fetch(`${this.baseUrl}/tasks/${encodeURIComponent(taskInstanceId)}`, {
      headers: this.authHeaders
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || 'No se pudo cargar el detalle de la tarea');
    }

    return response.json();
  }

  public async takeTask(taskInstanceId: string): Promise<TaskInstance> {
    const response = await fetch(`${this.baseUrl}/tasks/${encodeURIComponent(taskInstanceId)}/take`, {
      method: 'POST',
      headers: this.authHeaders
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || 'No se pudo tomar la tarea');
    }

    return response.json();
  }

  public async completeTask(taskInstanceId: string, formData: unknown): Promise<TaskInstance> {
    const payload = typeof formData === 'string' ? formData : JSON.stringify(formData);
    const response = await fetch(`${this.baseUrl}/tasks/${encodeURIComponent(taskInstanceId)}/complete`, {
      method: 'POST',
      headers: this.authHeaders,
      body: JSON.stringify({ formData: payload })
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || 'No se pudo completar la tarea');
    }

    return response.json();
  }

  public async voiceFill(voiceTranscript: string, formFields: string[]): Promise<Record<string, unknown>> {
    const response = await fetch('/api/copilot/voice-fill', {
      method: 'POST',
      headers: this.authHeaders,
      body: JSON.stringify({ voiceTranscript, formFields })
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || 'No se pudo autocompletar por voz.');
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

  // ... tus otros métodos ...

  public async uploadFile(file: File, policyId?: string): Promise<any> {
    const formData = new FormData();
    formData.append('file', file);
    if (policyId) {
      formData.append('policyId', policyId);
    }

    // Copiamos los headers de autenticación pero ELIMINAMOS el Content-Type
    // para que el navegador ponga automáticamente 'multipart/form-data'
    const headers: Record<string, string> = { ...this.authHeaders };
    delete headers['Content-Type'];

    const response = await fetch(`${this.baseUrl.replace('/execution', '/files')}/upload`, {
      method: 'POST',
      headers: headers,
      body: formData
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || 'No se pudo subir el archivo a S3');
    }

    return response.json(); // Esto devolverá tu UploadResponseDto (con la URL)
  }
  
}
