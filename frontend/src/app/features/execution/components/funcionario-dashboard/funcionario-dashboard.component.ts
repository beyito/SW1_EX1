import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../../auth.service';
import { ProcessTaskDto, ProcessTaskGroupDto, StartablePolicyDto } from '../../models/execution.models';
import { ExecutionService } from '../../services/execution.service';

@Component({
  selector: 'app-funcionario-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './funcionario-dashboard.component.html',
  styleUrl: './funcionario-dashboard.component.scss'
})
export class FuncionarioDashboardComponent implements OnInit {
  private readonly executionService = inject(ExecutionService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);

  public startablePolicies: StartablePolicyDto[] = [];
  public processGroups: ProcessTaskGroupDto[] = [];
  public selectedProcessId: string | null = null;
  public loading = false;
  public startingPolicyId: string | null = null;
  public message = '';
  public startDrafts: Record<string, { title: string; description: string }> = {};

  public ngOnInit(): void {
    void this.loadDashboardData();
  }

  public get selectedProcess(): ProcessTaskGroupDto | null {
    if (!this.selectedProcessId) {
      return null;
    }
    return this.processGroups.find((group) => group.processInstanceId === this.selectedProcessId) ?? null;
  }

  public async loadDashboardData(): Promise<void> {
    this.loading = true;
    this.message = '';

    const laneId = this.authService.getCurrentLaneId();
    if (!laneId) {
      this.startablePolicies = [];
      this.processGroups = [];
      this.selectedProcessId = null;
      this.loading = false;
      this.message = 'Tu usuario no tiene area/lane asignada.';
      this.cdr.detectChanges();
      return;
    }

    try {
      const [startablePolicies, processGroups] = await Promise.all([
        this.executionService.getStartablePolicies(),
        this.executionService.getMyProcessTaskGroups()
      ]);
      this.startablePolicies = startablePolicies;
      this.processGroups = processGroups;
      this.selectedProcessId = this.resolveSelectedProcessId(processGroups);
      this.seedStartDrafts(startablePolicies);
    } catch (error) {
      this.startablePolicies = [];
      this.processGroups = [];
      this.selectedProcessId = null;
      this.message = error instanceof Error ? error.message : 'No se pudo cargar el dashboard';
    } finally {
      this.loading = false;
      this.cdr.detectChanges();
    }
  }

  public selectProcess(group: ProcessTaskGroupDto): void {
    this.selectedProcessId = group.processInstanceId;
  }

  public draftFor(policy: StartablePolicyDto): { title: string; description: string } {
    const existing = this.startDrafts[policy.id];
    if (existing) {
      return existing;
    }
    const created = {
      title: policy.name ?? '',
      description: ''
    };
    this.startDrafts[policy.id] = created;
    return created;
  }

  public async startProcess(policy: StartablePolicyDto): Promise<void> {
    this.startingPolicyId = policy.id;
    this.message = '';
    this.cdr.detectChanges(); 

    try {
      const draft = this.draftFor(policy);
      const title = (draft.title ?? '').trim() || policy.name;
      const description = (draft.description ?? '').trim();
      await this.executionService.startProcess(policy.id, title, description);
      this.message = `Proceso "${policy.name}" iniciado correctamente.`;
      await this.loadDashboardData();
    } catch (error) {
      this.message = error instanceof Error ? error.message : 'No se pudo iniciar el proceso';
    } finally {
      this.startingPolicyId = null;
      this.cdr.detectChanges(); 
    }
  }

  public openTask(task: ProcessTaskDto): void {
    void this.router.navigate(['/execution/task', task.taskInstanceId]);
  }

  public statusLabel(status: string): string {
    switch (status) {
      case 'IN_PROGRESS':
        return 'En Proceso';
      case 'COMPLETED':
        return 'Hecho';
      case 'REJECTED':
        return 'Rechazado';
      case 'PENDING':
      default:
        return 'Pendiente';
    }
  }

  public statusClass(status: string): string {
    switch (status) {
      case 'IN_PROGRESS':
        return 'badge badge-progress';
      case 'COMPLETED':
        return 'badge badge-completed';
      case 'REJECTED':
        return 'badge badge-rejected';
      case 'PENDING':
      default:
        return 'badge badge-pending';
    }
  }

  private resolveSelectedProcessId(groups: ProcessTaskGroupDto[]): string | null {
    if (groups.length === 0) {
      return null;
    }
    if (!this.selectedProcessId) {
      return groups[0].processInstanceId;
    }
    const stillExists = groups.some((group) => group.processInstanceId === this.selectedProcessId);
    return stillExists ? this.selectedProcessId : groups[0].processInstanceId;
  }

  private seedStartDrafts(policies: StartablePolicyDto[]): void {
    for (const policy of policies) {
      if (!this.startDrafts[policy.id]) {
        this.startDrafts[policy.id] = {
          title: policy.name ?? '',
          description: ''
        };
      }
    }
  }
}
