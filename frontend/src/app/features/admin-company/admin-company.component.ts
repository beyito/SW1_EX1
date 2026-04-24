import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core'; // 🚩 Importamos ChangeDetectorRef
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { AuthService, AuthProfile } from '../../auth.service';

interface AreaInfo {
  id: string;
  name: string;
}

interface UserInfo {
  id: string;
  username: string;
  company: string;
  area: string;
  roles: string[];
}

@Component({
  selector: 'app-admin-company',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './admin-company.component.html',
  styleUrls: ['../panel.scss', '../../../../src/styles.scss']
})
export class AdminCompanyComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly cdr = inject(ChangeDetectorRef); // 🚩 Inyectamos el detector

  public areas: AreaInfo[] = [];
  public functionaries: UserInfo[] = [];

  public newAreaName = '';
  public areaMessage = '';
  public areaLoading = false;
  public editingAreaId: string | null = null;
  public editingAreaName = '';

  public funcUsername = '';
  public funcPassword = '';
  public funcMessage = '';
  public funcLoading = false;
  public editingFunctionaryId: string | null = null;
  public editingFunctionaryUsername = '';
  public editingFunctionaryPassword = '';

  public async ngOnInit(): Promise<void> {
    await this.loadData();
  }

  public get profile(): AuthProfile | null {
    return this.authService.getProfile();
  }

  public get token(): string | null {
    return this.authService.getToken();
  }

  public async loadData(): Promise<void> {
    await Promise.all([this.loadAreas(), this.loadFunctionaries()]);
    this.cdr.detectChanges(); // 🚩
  }

  public async loadAreas(): Promise<void> {
    try {
      const response = await fetch('/api/admin/areas', { headers: this.authHeaders });
      if (!response.ok) {
        throw new Error('No se pudieron cargar las areas');
      }
      this.areas = await response.json();
      this.cdr.detectChanges(); // 🚩
    } catch (error) {
      this.areaMessage = error instanceof Error ? error.message : 'Error al cargar areas';
      this.cdr.detectChanges(); // 🚩
    }
  }

  public async loadFunctionaries(): Promise<void> {
    try {
      const response = await fetch('/api/admin/functionaries', { headers: this.authHeaders });
      if (!response.ok) {
        throw new Error('No se pudieron cargar los funcionarios');
      }
      this.functionaries = await response.json();
      this.cdr.detectChanges(); // 🚩
    } catch (error) {
      this.funcMessage = error instanceof Error ? error.message : 'Error al cargar funcionarios';
      this.cdr.detectChanges(); // 🚩
    }
  }

  public async createArea(): Promise<void> {
    this.areaLoading = true;
    this.areaMessage = '';
    this.cdr.detectChanges(); // 🚩

    try {
      const response = await fetch('/api/admin/areas', {
        method: 'POST',
        headers: this.authHeaders,
        body: JSON.stringify({ name: this.newAreaName.trim() })
      });
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || 'No se pudo crear el area');
      }
      this.newAreaName = '';
      this.areaMessage = 'Area creada correctamente';
      await this.loadAreas();
    } catch (error) {
      this.areaMessage = error instanceof Error ? error.message : 'Error al crear area';
    } finally {
      this.areaLoading = false;
      this.cdr.detectChanges(); // 🚩
    }
  }

  public startEditArea(area: AreaInfo): void {
    this.editingAreaId = area.id;
    this.editingAreaName = area.name;
    this.cdr.detectChanges(); // 🚩
  }

  public cancelEditArea(): void {
    this.editingAreaId = null;
    this.editingAreaName = '';
    this.cdr.detectChanges(); // 🚩
  }

  public async saveArea(areaId: string): Promise<void> {
    this.areaLoading = true;
    this.areaMessage = '';
    this.cdr.detectChanges(); // 🚩

    try {
      const response = await fetch(`/api/admin/areas/${areaId}`, {
        method: 'PUT',
        headers: this.authHeaders,
        body: JSON.stringify({ name: this.editingAreaName.trim() })
      });
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || 'No se pudo actualizar el area');
      }
      this.areaMessage = 'Area actualizada correctamente';
      this.cancelEditArea();
      await this.loadData();
    } catch (error) {
      this.areaMessage = error instanceof Error ? error.message : 'Error al actualizar area';
    } finally {
      this.areaLoading = false;
      this.cdr.detectChanges(); // 🚩
    }
  }

  public async deleteArea(areaId: string): Promise<void> {
    this.areaLoading = true;
    this.areaMessage = '';
    this.cdr.detectChanges(); // 🚩

    try {
      const response = await fetch(`/api/admin/areas/${areaId}`, {
        method: 'DELETE',
        headers: this.authHeaders
      });
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || 'No se pudo eliminar el area');
      }
      this.areaMessage = 'Area eliminada correctamente';
      await this.loadData();
    } catch (error) {
      this.areaMessage = error instanceof Error ? error.message : 'Error al eliminar area';
    } finally {
      this.areaLoading = false;
      this.cdr.detectChanges(); // 🚩
    }
  }

  public async createFunctionary(): Promise<void> {
    this.funcLoading = true;
    this.funcMessage = '';
    this.cdr.detectChanges(); // 🚩

    try {
      const response = await fetch('/api/admin/functionaries', {
        method: 'POST',
        headers: this.authHeaders,
        body: JSON.stringify({
          username: this.funcUsername.trim(),
          password: this.funcPassword
        })
      });
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || 'No se pudo crear el funcionario');
      }
      this.funcUsername = '';
      this.funcPassword = '';
      this.funcMessage = 'Funcionario creado correctamente en el area Cliente';
      await this.loadFunctionaries();
    } catch (error) {
      this.funcMessage = error instanceof Error ? error.message : 'Error al crear funcionario';
    } finally {
      this.funcLoading = false;
      this.cdr.detectChanges(); // 🚩
    }
  }

  public startEditFunctionary(func: UserInfo): void {
    this.editingFunctionaryId = func.id;
    this.editingFunctionaryUsername = func.username;
    this.editingFunctionaryPassword = '';
    this.cdr.detectChanges(); // 🚩
  }

  public cancelEditFunctionary(): void {
    this.editingFunctionaryId = null;
    this.editingFunctionaryUsername = '';
    this.editingFunctionaryPassword = '';
    this.cdr.detectChanges(); // 🚩
  }

  public async saveFunctionary(userId: string): Promise<void> {
    this.funcLoading = true;
    this.funcMessage = '';
    this.cdr.detectChanges(); // 🚩

    try {
      const response = await fetch(`/api/admin/functionaries/${userId}`, {
        method: 'PUT',
        headers: this.authHeaders,
        body: JSON.stringify({
          username: this.editingFunctionaryUsername.trim(),
          password: this.editingFunctionaryPassword.trim() || null
        })
      });
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || 'No se pudo actualizar el funcionario');
      }
      this.funcMessage = 'Funcionario actualizado correctamente';
      this.cancelEditFunctionary();
      await this.loadFunctionaries();
    } catch (error) {
      this.funcMessage = error instanceof Error ? error.message : 'Error al actualizar funcionario';
    } finally {
      this.funcLoading = false;
      this.cdr.detectChanges(); // 🚩
    }
  }

  public async deleteFunctionary(userId: string): Promise<void> {
    this.funcLoading = true;
    this.funcMessage = '';
    this.cdr.detectChanges(); // 🚩

    try {
      const response = await fetch(`/api/admin/functionaries/${userId}`, {
        method: 'DELETE',
        headers: this.authHeaders
      });
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || 'No se pudo eliminar el funcionario');
      }
      this.funcMessage = 'Funcionario eliminado correctamente';
      await this.loadFunctionaries();
    } catch (error) {
      this.funcMessage = error instanceof Error ? error.message : 'Error al eliminar funcionario';
    } finally {
      this.funcLoading = false;
      this.cdr.detectChanges(); // 🚩
    }
  }

  private get authHeaders(): Record<string, string> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (this.token) {
      headers['Authorization'] = `Bearer ${this.token}`;
    }
    return headers;
  }
}