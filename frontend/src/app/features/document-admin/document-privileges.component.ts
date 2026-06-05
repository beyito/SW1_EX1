import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../auth.service';

interface AdminDocument {
  id: string;
  fileName: string;
  documentCode?: string;
  processInstanceId: string;
  createdBy?: string;
  updatedAt?: string;
}

interface UserInfo {
  id: string;
  username: string;
  area: string;
}

interface DocumentPermission {
  id?: string;
  documentId: string;
  userId: string;
  username: string;
  canView: boolean;
  canEdit: boolean;
  canDelete: boolean;
}

@Component({
  selector: 'app-document-privileges',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './document-privileges.component.html',
  styleUrl: './document-admin.scss'
})
export class DocumentPrivilegesComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly cdr = inject(ChangeDetectorRef);

  public documents: AdminDocument[] = [];
  public functionaries: UserInfo[] = [];
  public permissions: Record<string, DocumentPermission> = {};
  public selectedDocumentId = '';
  public loading = false;
  public savingUserId: string | null = null;
  public message = '';

  public async ngOnInit(): Promise<void> {
    await this.loadInitialData();
  }

  public get selectedDocument(): AdminDocument | undefined {
    return this.documents.find((document) => document.id === this.selectedDocumentId);
  }

  public permissionFor(user: UserInfo): DocumentPermission {
    if (!this.permissions[user.id]) {
      this.permissions[user.id] = {
        documentId: this.selectedDocumentId,
        userId: user.id,
        username: user.username,
        canView: false,
        canEdit: false,
        canDelete: false
      };
    }
    return this.permissions[user.id];
  }

  public async onDocumentChange(): Promise<void> {
    await this.loadPermissions();
  }

  public async savePermission(user: UserInfo): Promise<void> {
    if (!this.selectedDocumentId) {
      return;
    }
    const permission = this.permissionFor(user);
    this.savingUserId = user.id;
    this.message = '';
    this.cdr.detectChanges();

    try {
      const response = await fetch(`/api/admin/documents/${encodeURIComponent(this.selectedDocumentId)}/permissions`, {
        method: 'PUT',
        headers: this.authHeaders,
        body: JSON.stringify({
          userId: user.id,
          canView: permission.canView,
          canEdit: permission.canEdit,
          canDelete: permission.canDelete
        })
      });
      if (!response.ok) {
        throw new Error(await response.text() || 'No se pudo guardar el privilegio');
      }
      const saved = (await response.json()) as DocumentPermission;
      this.permissions[user.id] = saved;
      this.message = 'Privilegios actualizados.';
    } catch (error) {
      this.message = error instanceof Error ? error.message : 'Error al guardar privilegios';
    } finally {
      this.savingUserId = null;
      this.cdr.detectChanges();
    }
  }

  private async loadInitialData(): Promise<void> {
    this.loading = true;
    this.cdr.detectChanges();
    try {
      const [documentsResponse, usersResponse] = await Promise.all([
        fetch('/api/admin/documents', { headers: this.authHeaders }),
        fetch('/api/admin/functionaries', { headers: this.authHeaders })
      ]);
      if (!documentsResponse.ok) {
        throw new Error('No se pudieron cargar los documentos');
      }
      if (!usersResponse.ok) {
        throw new Error('No se pudieron cargar los funcionarios');
      }
      this.documents = await documentsResponse.json();
      this.functionaries = await usersResponse.json();
      this.selectedDocumentId = this.documents[0]?.id ?? '';
      await this.loadPermissions();
    } catch (error) {
      this.message = error instanceof Error ? error.message : 'Error al cargar privilegios';
    } finally {
      this.loading = false;
      this.cdr.detectChanges();
    }
  }

  private async loadPermissions(): Promise<void> {
    this.permissions = {};
    if (!this.selectedDocumentId) {
      return;
    }
    const response = await fetch(`/api/admin/documents/${encodeURIComponent(this.selectedDocumentId)}/permissions`, {
      headers: this.authHeaders
    });
    if (!response.ok) {
      throw new Error('No se pudieron cargar los privilegios');
    }
    const permissions = (await response.json()) as DocumentPermission[];
    this.permissions = permissions.reduce<Record<string, DocumentPermission>>((acc, permission) => {
      acc[permission.userId] = permission;
      return acc;
    }, {});
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
