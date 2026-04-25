import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../../auth.service';
import { ProcessTaskDto, ProcessTaskGroupDto } from '../../models/execution.models';
import { ExecutionService } from '../../services/execution.service';

@Component({
  selector: 'app-task-inbox',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './task-inbox.component.html',
  styleUrl: './task-inbox.component.scss'
})
export class TaskInboxComponent implements OnInit {
  private readonly executionService = inject(ExecutionService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  public processGroups: ProcessTaskGroupDto[] = [];
  public selectedProcess: ProcessTaskGroupDto | null = null;
  public selectedTask: ProcessTaskDto | null = null;
  public loading = false;
  public message = '';

  public ngOnInit(): void {
    void this.loadMyTasks();
  }

  public async loadMyTasks(): Promise<void> {
    this.loading = true;
    this.message = '';

    try {
      const laneId = this.authService.getCurrentLaneId();
      if (!laneId) {
        this.processGroups = [];
        this.selectedProcess = null;
        this.selectedTask = null;
        this.message = 'Tu perfil no tiene area asignada para buscar tareas.';
        return;
      }

      this.processGroups = await this.executionService.getMyProcessTaskGroups();
      this.selectedProcess = this.processGroups.length > 0 ? this.processGroups[0] : null;
      this.selectedTask = this.selectedProcess?.tasks[0] ?? null;

      if (this.processGroups.length === 0) {
        this.message = 'No tienes procesos activos con tareas.';
      }
    } catch (error) {
      this.processGroups = [];
      this.selectedProcess = null;
      this.selectedTask = null;
      this.message = error instanceof Error ? error.message : 'Error al cargar tareas';
    } finally {
      this.loading = false;
    }
  }

  public selectProcess(group: ProcessTaskGroupDto): void {
    this.selectedProcess = group;
    this.selectedTask = group.tasks[0] ?? null;
    this.message = '';
  }

  public selectTask(task: ProcessTaskDto): void {
    this.selectedTask = task;
    this.message = '';
  }

  public openSelectedTask(): void {
    if (!this.selectedTask) {
      return;
    }
    void this.router.navigate(['/execution/task', this.selectedTask.taskInstanceId]);
  }
}
