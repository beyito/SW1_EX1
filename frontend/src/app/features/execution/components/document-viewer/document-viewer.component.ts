import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { DocumentDto, OnlyOfficeConfigDto } from '../../models/execution.models';
import { ExecutionService } from '../../services/execution.service';

declare global {
  interface Window {
    DocsAPI?: {
      DocEditor: new (elementId: string, config: Record<string, unknown>) => unknown;
    };
  }
}

@Component({
  selector: 'app-document-viewer',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './document-viewer.component.html',
  styleUrl: './document-viewer.component.scss'
})
export class DocumentViewerComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly executionService = inject(ExecutionService);
  private readonly cdr = inject(ChangeDetectorRef);

  public document: DocumentDto | null = null;
  public loading = false;
  public onlyOfficeLoading = false;
  public onlyOfficeOpen = false;
  public message = '';

  public ngOnInit(): void {
    void this.loadDocument();
  }

  public async openOnlyOfficeEditor(): Promise<void> {
    if (!this.document) {
      return;
    }

    if (!this.document.canEdit) {
      this.message = 'No tienes privilegios para editar este documento.';
      return;
    }

    this.onlyOfficeLoading = true;
    this.onlyOfficeOpen = true;
    this.message = '';
    this.cdr.detectChanges();

    try {
      const editorConfig = await this.executionService.getOnlyOfficeConfig(this.document.id);
      await this.ensureOnlyOfficeScript(editorConfig);
      await new Promise((resolve) => setTimeout(resolve, 80));

      const placeholder = window.document.getElementById('onlyoffice-placeholder');
      if (!placeholder) {
        throw new Error('No se encontró el contenedor de OnlyOffice.');
      }
      placeholder.innerHTML = '';

      if (!window.DocsAPI?.DocEditor) {
        throw new Error('OnlyOffice Document Server no está disponible.');
      }

      new window.DocsAPI.DocEditor('onlyoffice-placeholder', editorConfig.config);
    } catch (error) {
      this.onlyOfficeOpen = false;
      this.message = error instanceof Error ? error.message : 'No se pudo abrir OnlyOffice.';
    } finally {
      this.onlyOfficeLoading = false;
      this.cdr.detectChanges();
    }
  }

  public closeOnlyOfficeEditor(): void {
    this.onlyOfficeOpen = false;
    const placeholder = window.document.getElementById('onlyoffice-placeholder');
    if (placeholder) {
      placeholder.innerHTML = '';
    }
  }

  public canTryEditor(): boolean {
    if (!this.document?.fileName) {
      return false;
    }
    return /(\.docx?|\.xlsx?)$/i.test(this.document.fileName);
  }

  private async loadDocument(): Promise<void> {
    const documentId = this.route.snapshot.paramMap.get('id') ?? '';
    if (!documentId) {
      this.message = 'No se encontró el documento.';
      return;
    }

    this.loading = true;
    this.cdr.detectChanges();
    try {
      this.document = await this.executionService.getDocument(documentId);
      this.tryOpenEditorFromQueryParam();
    } catch (error) {
      this.message = error instanceof Error ? error.message : 'No se pudo cargar el documento.';
    } finally {
      this.loading = false;
      this.cdr.detectChanges();
    }
  }

  private tryOpenEditorFromQueryParam(): void {
    if (this.route.snapshot.queryParamMap.get('edit') !== '1') {
      return;
    }

    if (!this.document?.canEdit) {
      this.message = 'No tienes privilegios para editar este documento.';
      return;
    }

    if (!this.canTryEditor()) {
      this.message = 'Este tipo de documento no se puede editar con OnlyOffice.';
      return;
    }

    setTimeout(() => void this.openOnlyOfficeEditor(), 0);
  }

  private async ensureOnlyOfficeScript(editorConfig: OnlyOfficeConfigDto): Promise<void> {
    if (window.DocsAPI?.DocEditor) {
      return;
    }

    const baseUrl = String(editorConfig.documentServerUrl ?? '').replace(/\/+$/, '');
    if (!baseUrl) {
      throw new Error('No se configuró la URL de OnlyOffice Document Server.');
    }

    const existingScript = window.document.querySelector<HTMLScriptElement>('script[data-onlyoffice-api="true"]');
    if (existingScript) {
      await this.waitForOnlyOfficeApi();
      return;
    }

    await new Promise<void>((resolve, reject) => {
      const script = window.document.createElement('script');
      script.src = `${baseUrl}/web-apps/apps/api/documents/api.js`;
      script.async = true;
      script.dataset['onlyofficeApi'] = 'true';
      script.onload = () => resolve();
      script.onerror = () => reject(new Error('No se pudo cargar el script de OnlyOffice.'));
      window.document.body.appendChild(script);
    });
  }

  private async waitForOnlyOfficeApi(): Promise<void> {
    for (let attempt = 0; attempt < 20; attempt++) {
      if (window.DocsAPI?.DocEditor) {
        return;
      }
      await new Promise((resolve) => setTimeout(resolve, 100));
    }
    throw new Error('OnlyOffice tardó demasiado en inicializar.');
  }
}
