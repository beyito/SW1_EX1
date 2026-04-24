import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core'; // 🚩 Importamos ChangeDetectorRef
import { FormsModule } from '@angular/forms';
import { AuthService, AuthProfile } from '../../auth.service';

interface CompanyInfo {
  id: string;
  name: string;
}

interface CompanyAdminInfo {
  id: string;
  username: string;
  company: string;
  roles: string[];
}

@Component({
  selector: 'app-admin-software',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-software.component.html',
  styleUrls: ['../panel.scss', '../../../../src/styles.scss']
})
export class AdminSoftwareComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly cdr = inject(ChangeDetectorRef); // 🚩 Inyectamos el detector

  public companies: CompanyInfo[] = [];
  public companyAdmins: CompanyAdminInfo[] = [];

  public newCompanyName = '';
  public companyMessage = '';
  public companyLoading = false;

  public adminUsername = '';
  public adminPassword = '';
  public adminCompany = '';
  public adminMessage = '';
  public adminLoading = false;

  public editingAdminId: string | null = null;
  public editingAdminUsername = '';
  public editingAdminPassword = '';

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
    await Promise.all([this.loadCompanies(), this.loadCompanyAdmins()]);
    this.cdr.detectChanges(); // 🚩
  }

  public async loadCompanies(): Promise<void> {
    this.companyMessage = '';
    try {
      const response = await fetch('/api/admin/companies', {
        headers: this.authHeaders
      });
      if (!response.ok) {
        throw new Error('No se pudieron cargar las empresas');
      }
      this.companies = await response.json();
      if (this.companies.length > 0 && !this.adminCompany) {
        this.adminCompany = this.companies[0].name;
      }
      this.cdr.detectChanges(); // 🚩
    } catch (error) {
      this.companyMessage = error instanceof Error ? error.message : 'Error al cargar empresas';
      this.cdr.detectChanges(); // 🚩
    }
  }

  public async loadCompanyAdmins(): Promise<void> {
    this.adminMessage = '';
    try {
      const response = await fetch('/api/admin/company-admins', {
        headers: this.authHeaders
      });
      if (!response.ok) {
        throw new Error('No se pudieron cargar los administradores de empresa');
      }
      this.companyAdmins = await response.json();
      this.cdr.detectChanges(); // 🚩
    } catch (error) {
      this.adminMessage = error instanceof Error ? error.message : 'Error al cargar administradores';
      this.cdr.detectChanges(); // 🚩
    }
  }

  public async createCompany(): Promise<void> {
    this.companyLoading = true;
    this.companyMessage = '';
    this.cdr.detectChanges(); // 🚩

    try {
      const response = await fetch('/api/admin/companies', {
        method: 'POST',
        headers: this.authHeaders,
        body: JSON.stringify({ name: this.newCompanyName.trim() })
      });
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || 'No se pudo crear la empresa');
      }
      this.newCompanyName = '';
      this.companyMessage = 'Empresa creada correctamente (con area Cliente por defecto)';
      await this.loadData();
    } catch (error) {
      this.companyMessage = error instanceof Error ? error.message : 'Error al crear empresa';
    } finally {
      this.companyLoading = false;
      this.cdr.detectChanges(); // 🚩
    }
  }

  public async createCompanyAdmin(): Promise<void> {
    this.adminLoading = true;
    this.adminMessage = '';
    this.cdr.detectChanges(); // 🚩

    try {
      const response = await fetch('/api/admin/company-admins', {
        method: 'POST',
        headers: this.authHeaders,
        body: JSON.stringify({
          username: this.adminUsername.trim(),
          password: this.adminPassword,
          company: this.adminCompany
        })
      });
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || 'No se pudo crear el administrador');
      }
      this.adminUsername = '';
      this.adminPassword = '';
      this.adminMessage = 'Administrador de empresa creado correctamente';
      await this.loadCompanyAdmins();
    } catch (error) {
      this.adminMessage = error instanceof Error ? error.message : 'Error al crear administrador';
    } finally {
      this.adminLoading = false;
      this.cdr.detectChanges(); // 🚩
    }
  }

  public startEditAdmin(admin: CompanyAdminInfo): void {
    this.editingAdminId = admin.id;
    this.editingAdminUsername = admin.username;
    this.editingAdminPassword = '';
    this.cdr.detectChanges(); // 🚩
  }

  public cancelEditAdmin(): void {
    this.editingAdminId = null;
    this.editingAdminUsername = '';
    this.editingAdminPassword = '';
    this.cdr.detectChanges(); // 🚩
  }

  public async saveAdmin(userId: string): Promise<void> {
    this.adminLoading = true;
    this.adminMessage = '';
    this.cdr.detectChanges(); // 🚩

    try {
      const response = await fetch(`/api/admin/company-admins/${userId}`, {
        method: 'PUT',
        headers: this.authHeaders,
        body: JSON.stringify({
          username: this.editingAdminUsername.trim(),
          password: this.editingAdminPassword.trim() || null
        })
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || 'No se pudo actualizar el administrador');
      }

      this.adminMessage = 'Administrador actualizado correctamente';
      this.cancelEditAdmin();
      await this.loadCompanyAdmins();
    } catch (error) {
      this.adminMessage = error instanceof Error ? error.message : 'Error al actualizar administrador';
    } finally {
      this.adminLoading = false;
      this.cdr.detectChanges(); // 🚩
    }
  }

  public async deleteAdmin(userId: string): Promise<void> {
    this.adminLoading = true;
    this.adminMessage = '';
    this.cdr.detectChanges(); // 🚩

    try {
      const response = await fetch(`/api/admin/company-admins/${userId}`, {
        method: 'DELETE',
        headers: this.authHeaders
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || 'No se pudo eliminar el administrador');
      }

      this.adminMessage = 'Administrador eliminado correctamente';
      await this.loadCompanyAdmins();
    } catch (error) {
      this.adminMessage = error instanceof Error ? error.message : 'Error al eliminar administrador';
    } finally {
      this.adminLoading = false;
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