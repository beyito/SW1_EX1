import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ExecutionService } from '../../services/execution.service';
import { TaskDetailDto, TaskFormField, TaskStatus } from '../../models/execution.models';

type DynamicFormGroup = FormGroup<Record<string, FormControl>>;

@Component({
  selector: 'app-task-execution',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './task-execution.component.html',
  styleUrl: './task-execution.component.scss'
})
export class TaskExecutionComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly executionService = inject(ExecutionService);
  private readonly cdr = inject(ChangeDetectorRef);

  public loading = false;
  public takingTask = false;
  public completingTask = false;
  public message = '';
  public taskDetail: TaskDetailDto | null = null;
  public formSchemaFields: TaskFormField[] = [];
  public formGroup: DynamicFormGroup = this.fb.group({}) as DynamicFormGroup;
  public formReady = false;
  public savedAnswers: Record<string, unknown> = {};
  public decisionOptions: string[] = [];

  // 🚩 AGREGADO PARA S3: Control de estado de subida por cada campo
  public uploadingFiles: Record<string, boolean> = {};

  public ngOnInit(): void {
    void this.loadTaskDetails();
  }

  public get taskId(): string {
    return this.route.snapshot.paramMap.get('id') ?? '';
  }

  public get status(): TaskStatus | '' {
    return this.taskDetail?.status ?? '';
  }

  public async loadTaskDetails(): Promise<void> {
    if (!this.taskId) {
      this.message = 'No se encontro una tarea para ejecutar.';
      this.cdr.detectChanges(); 
      return;
    }

    this.loading = true;
    this.message = '';
    this.formReady = false;
    this.cdr.detectChanges(); 

    try {
      const detail = await this.executionService.getTaskDetails(this.taskId);
      this.taskDetail = detail;
      this.formSchemaFields = this.parseSchema(detail.formSchema);
      this.formGroup = this.buildForm(this.formSchemaFields);
      this.formReady = true;
      this.savedAnswers = this.parseFormData(detail.formData);
      this.decisionOptions = this.getDecisionOptions();
    } catch (error) {
      this.message = error instanceof Error ? error.message : 'No se pudo cargar el detalle de la tarea';
    } finally {
      this.loading = false;
      this.cdr.detectChanges(); 
    }
  }

  public async markAsInProgress(): Promise<void> {
    if (!this.taskDetail || this.taskDetail.status !== 'PENDING') {
      return;
    }
    this.takingTask = true;
    this.message = '';
    this.cdr.detectChanges(); 

    try {
      await this.executionService.takeTask(this.taskDetail.id);
      await this.loadTaskDetails(); 
    } catch (error) {
      this.message = error instanceof Error ? error.message : 'No se pudo tomar la tarea';
    } finally {
      this.takingTask = false;
      this.cdr.detectChanges(); 
    }
  }

  // 🚩 AGREGADO PARA S3: Método para interceptar archivos y subirlos
  public async onFileSelected(event: Event, controlName: string): Promise<void> {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const file = input.files[0];
    this.uploadingFiles[controlName] = true;
    this.cdr.detectChanges(); // Despertar UI para mostrar "Subiendo..."

    try {
      const policyId = this.taskDetail?.policyId || 'general'; 
      const uploadResult = await this.executionService.uploadFile(file, policyId);
      
      this.formGroup.get(controlName)?.setValue(uploadResult.url);
      this.formGroup.get(controlName)?.markAsDirty();
    } catch (error) {
      console.error('Error subiendo archivo a S3:', error);
      alert('Error al subir el archivo. Intente nuevamente.');
      input.value = ''; 
    } finally {
      this.uploadingFiles[controlName] = false;
      this.cdr.detectChanges(); // Despertar UI para ocultar "Subiendo..."
    }
  }

  public async submitTask(selectedDecision?: string): Promise<void> {
    if (this.formGroup.invalid) {
      this.formGroup.markAllAsTouched(); 
      this.message = 'Existen campos obligatorios sin completar.';
      this.cdr.detectChanges();
      return;
    }

    this.completingTask = true;
    this.message = '';
    this.cdr.detectChanges();

    try {
      const formValues = this.formGroup.getRawValue() as Record<string, unknown>;
      const payload: Record<string, unknown> = {
        ...formValues
      };
      if (selectedDecision && selectedDecision.trim()) {
        payload['_decisionTomada'] = selectedDecision.trim();
      }
      await this.executionService.completeTask(this.taskDetail!.id, payload);
      await this.router.navigate(['/funcionario-dashboard']);
    } catch (error) {
      this.message = error instanceof Error ? error.message : 'Error al finalizar la tarea';
    } finally {
      this.completingTask = false;
      this.cdr.detectChanges();
    }
  }

  public statusLabel(status: string): string { 
    const labels: Record<string, string> = { 'PENDING': 'Pendiente', 'IN_PROGRESS': 'En Progreso', 'COMPLETED': 'Completada' };
    return labels[status] || status; 
  }

  public statusClass(status: string): string { 
    return `status-badge ${status.toLowerCase()}`;
  }

  public fieldName(field: TaskFormField, index: number): string {
    if (!field) return `field_${index}`;
    const nameStr = field.name || field.id || `field_${index}`;
    return nameStr.trim();
  }

  public getFieldControl(field: TaskFormField, index: number): FormControl | null { 
    return this.formGroup.get(this.fieldName(field, index)) as FormControl | null;
  }

  public shouldShowError(field: TaskFormField, index: number): boolean { 
    const control = this.getFieldControl(field, index);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  private buildForm(fields: TaskFormField[]): DynamicFormGroup {
    const controls: Record<string, FormControl> = {};

    fields.forEach((field, index) => {
      const controlName = this.fieldName(field, index);
      // Aquí usas la propiedad 'required' real que viene del JSON, o false por defecto
      const isRequired = field.required ?? false; 
      const isBooleanField = field.type === 'checkbox' || field.type === 'boolean';

      controls[controlName] = this.fb.control(
        isBooleanField ? false : '',
        isRequired
          ? [isBooleanField ? Validators.requiredTrue : Validators.required]
          : []
      ) as FormControl;
    });

    return this.fb.group(controls) as DynamicFormGroup;
  }

  private parseSchema(rawSchema: string): TaskFormField[] {
    if (!rawSchema || !rawSchema.trim()) return [];
    try {
      let parsed = JSON.parse(rawSchema);
      if (typeof parsed === 'string') parsed = JSON.parse(parsed);
      
      // Manejamos si viene como objeto { fields: [...] } o array directo
      if (parsed && Array.isArray(parsed.fields)) return parsed.fields;
      return Array.isArray(parsed) ? parsed : [];
    } catch (error) {
      console.error('Error al parsear formSchema:', error);
      return [];
    }
  }

  private parseFormData(rawFormData?: string | null): Record<string, unknown> {
    if (!rawFormData || !rawFormData.trim()) return {};
    try {
      let parsed = JSON.parse(rawFormData);
      if (typeof parsed === 'string') parsed = JSON.parse(parsed);
      return parsed && typeof parsed === 'object' ? parsed : {};
    } catch (error) {
      console.error('Error al parsear formData:', error);
      return {};
    }
  }

  public getDecisionOptions(): string[] {
    const diagramJson = this.taskDetail?.diagramJson;
    const currentNodeId = this.taskDetail?.taskId;
    if (!diagramJson || !currentNodeId) {
      return [];
    }

    const cells = this.parseDiagramCells(diagramJson);
    if (!cells.length) {
      return [];
    }

    const linkFromCurrent = cells.find((cell) => {
      if (cell.type !== 'standard.Link') {
        return false;
      }
      return cell.source?.id === currentNodeId;
    });
    if (!linkFromCurrent?.target?.id) {
      return [];
    }

    const nextNode = cells.find((cell) => cell.id === linkFromCurrent.target?.id);
    if (!nextNode || nextNode.nodeType !== 'DECISION') {
      return [];
    }

    return cells
      .filter((cell) => cell.type === 'standard.Link' && cell.source?.id === nextNode.id)
      .map((cell) => (cell.conditionLabel ?? '').toString().trim())
      .filter((label) => !!label);
  }

  private parseDiagramCells(rawDiagramJson: string): Array<{
    id?: string;
    type?: string;
    nodeType?: string;
    source?: { id?: string };
    target?: { id?: string };
    conditionLabel?: string;
  }> {
    try {
      const parsed = JSON.parse(rawDiagramJson) as { cells?: unknown };
      if (!parsed || !Array.isArray(parsed.cells)) {
        return [];
      }
      return parsed.cells as Array<{
        id?: string;
        type?: string;
        nodeType?: string;
        source?: { id?: string };
        target?: { id?: string };
        conditionLabel?: string;
      }>;
    } catch (error) {
      console.error('Error al parsear diagramJson para lookahead:', error);
      return [];
    }
  }
}
