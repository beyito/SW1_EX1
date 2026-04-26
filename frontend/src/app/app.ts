import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from './auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterModule],
  styleUrl: './app.scss',
  template: `
    <header class="enterprise-header">
      <div class="brand-block">
        <div class="brand-logo" aria-hidden="true">EB</div>
        <div class="brand-text">
          <strong>Enterprise BPMN Suite</strong>
          <small>Orquestacion y automatizacion empresarial</small>
        </div>
      </div>

      <div class="header-actions">
        <ng-container *ngIf="isAuthenticated; else loginAction">
          <div class="profile-block">
            <strong>{{ profile?.username }}</strong>
            <small>{{ profile?.company || 'Empresa' }}</small>
          </div>
          <button class="logout-btn" (click)="logout()">Cerrar sesion</button>
        </ng-container>
        <ng-template #loginAction>
          <button class="login-btn" (click)="goToLogin()">Iniciar sesion</button>
        </ng-template>
      </div>
    </header>

    <main class="app-content with-header">
      <router-outlet></router-outlet>
    </main>
  `
})
export class App {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  public get profile() {
    return this.authService.getProfile();
  }

  public get isAuthenticated(): boolean {
    return this.authService.isAuthenticated();
  }

  public logout(): void {
    this.authService.logout();
    window.location.reload();
  }

  public goToLogin(): void {
    void this.router.navigate(['/login']);
  }
}
