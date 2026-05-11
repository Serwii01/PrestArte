import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, computed, inject, OnInit, signal } from '@angular/core';

import { environment } from '../../../../environments/environment';
import { AuthService } from '../../../core/services/auth.service';
import { UserResponse } from '../../../core/models/user.models';
import { StatCardComponent } from '../../../shared/components/stat-card/stat-card.component';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, StatCardComponent],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.scss',
})
export class AdminDashboardComponent implements OnInit {
  private readonly http = inject(HttpClient);
  protected readonly auth = inject(AuthService);

  protected readonly pending = signal<UserResponse[]>([]);
  protected readonly loading = signal(true);

  /** Reparto por rol para los stat cards. */
  protected readonly pendingByRole = computed(() => {
    const all = this.pending();
    return {
      collectors: all.filter((u) => u.role === 'COLLECTOR').length,
      foundations: all.filter((u) => u.role === 'FOUNDATION').length,
      transport: all.filter((u) => u.role === 'TRANSPORT').length,
    };
  });

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.http.get<UserResponse[]>(`${environment.apiBaseUrl}/admin/pending-users`).subscribe({
      next: (users) => {
        this.pending.set(users);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  approve(id: number): void {
    this.http
      .post(`${environment.apiBaseUrl}/admin/approve/${id}`, {}, { responseType: 'text' })
      .subscribe(() => this.refresh());
  }

  reject(id: number): void {
    this.http
      .post(`${environment.apiBaseUrl}/admin/reject/${id}`, {}, { responseType: 'text' })
      .subscribe(() => this.refresh());
  }

  /** Inicial para el avatar circular. */
  initial(name: string): string {
    return name?.charAt(0).toUpperCase() ?? '?';
  }

  /** Etiqueta amigable del rol. */
  roleLabel(role: UserResponse['role']): string {
    return {
      ADMIN: 'Administrador',
      COLLECTOR: 'Coleccionista',
      FOUNDATION: 'Fundación',
      TRANSPORT: 'Transporte',
    }[role];
  }
}
