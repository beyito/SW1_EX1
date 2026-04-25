import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { LinkCondition, LinkConditionUpdate } from '../../models/policy-designer.models';

type RouteType = 'default' | 'expression';

@Component({
  selector: 'app-link-properties-panel',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './link-properties-panel.component.html',
  styleUrl: './link-properties-panel.component.scss'
})
export class LinkPropertiesPanelComponent implements OnChanges {
  @Input() public visible = false;
  @Input() public isDecisionSource = false;
  @Input() public currentCondition: LinkCondition | null = null;
  @Input() public currentConditionLabel = '';

  @Output() public conditionChanged = new EventEmitter<LinkConditionUpdate>();

  private readonly formBuilder = inject(FormBuilder);
  protected readonly form = this.formBuilder.group({
    routeType: this.formBuilder.control<RouteType>('default', { validators: [Validators.required], nonNullable: true }),
    conditionLabel: this.formBuilder.control<string>('')
  });

  public constructor() {
    this.form.controls.routeType.valueChanges.subscribe((routeType) => {
      this.applyConditionalValidators(routeType);
      this.emitUpdateIfValid();
    });

    this.form.valueChanges.subscribe(() => {
      this.emitUpdateIfValid();
    });
  }

  public ngOnChanges(changes: SimpleChanges): void {
    if (!changes['currentCondition'] && !changes['currentConditionLabel']) {
      return;
    }
    this.patchFromInputs(this.currentCondition, this.currentConditionLabel);
  }

  private applyConditionalValidators(routeType: RouteType): void {
    const labelControl = this.form.controls.conditionLabel;
    if (routeType === 'expression') {
      labelControl.setValidators([Validators.required]);
    } else {
      labelControl.clearValidators();
    }
    labelControl.updateValueAndValidity({ emitEvent: false });
  }

  private patchFromInputs(condition: LinkCondition | null, conditionLabel: string): void {
    const routeType: RouteType = condition?.type === 'expression' ? 'expression' : 'default';
    this.form.patchValue(
      {
        routeType,
        conditionLabel: conditionLabel ?? ''
      },
      { emitEvent: false }
    );
    this.applyConditionalValidators(routeType);
  }

  private emitUpdateIfValid(): void {
    const routeType = this.form.controls.routeType.value;
    const label = (this.form.controls.conditionLabel.value ?? '').trim();

    if (routeType === 'default') {
      this.conditionChanged.emit({
        conditionLabel: label || 'Default',
        condition: { type: 'default' }
      });
      return;
    }

    if (this.form.invalid || !label) {
      return;
    }

    this.conditionChanged.emit({
      conditionLabel: label,
      condition: {
        type: 'expression',
        script: `#_decisionTomada == '${this.escapeSingleQuotes(label)}'`
      }
    });
  }

  private escapeSingleQuotes(value: string): string {
    return value.replace(/'/g, "\\'");
  }
}
