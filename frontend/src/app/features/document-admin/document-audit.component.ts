import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../auth.service';

interface AdminDocument {
  id: string;
  fileName: string;
  processInstanceId: string;
}

interface AuditLog {
  id: string;
  username: string;
  action: 'VIEW' | 'EDIT' | 'DELETE';
  documentId: string;
  processInstanceId: string;
  httpMethod: string;
  path: string;
  timestamp: string;
}

@Component({
  selector: 'app-document-audit',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './document-audit.component.html',
  styleUrl: './document-admin.scss'
})
export class DocumentAuditComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly cdr = inject(ChangeDetectorRef);

  public documents: AdminDocument[] = [];
  public logs: AuditLog[] = [];
  public selectedDocumentId = 'ALL';
  public loading = false;
  public message = '';

  public async ngOnInit(): Promise<void> {
    await this.loadDocuments();
    await this.loadAudit();
  }

  public documentName(documentId: string): string {
    return this.documents.find((document) => document.id === documentId)?.fileName ?? documentId;
  }

  public async loadAudit(): Promise<void> {
    this.loading = true;
    this.message = '';
    this.cdr.detectChanges();
    try {
      const url = this.selectedDocumentId === 'ALL'
        ? '/api/admin/documents/audit'
        : `/api/admin/documents/${encodeURIComponent(this.selectedDocumentId)}/audit`;
      const response = await fetch(url, { headers: this.authHeaders });
      if (!response.ok) {
        throw new Error(await response.text() || 'No se pudo cargar la auditoría');
      }
      this.logs = await response.json();
    } catch (error) {
      this.message = error instanceof Error ? error.message : 'Error al cargar auditoría';
    } finally {
      this.loading = false;
      this.cdr.detectChanges();
    }
  }

  private async loadDocuments(): Promise<void> {
    const response = await fetch('/api/admin/documents', { headers: this.authHeaders });
    if (!response.ok) {
      this.message = 'No se pudieron cargar documentos';
      return;
    }
    this.documents = await response.json();
  }

  private get authHeaders(): Record<string, string> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    const token = this.authService.getToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
    return headers;
  }
}
