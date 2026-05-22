import { CommonModule } from '@angular/common';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { environment } from '../../../../environments/environment';
import { AuthService } from '../../../core/services/auth.service';
import { Role, UserResponse } from '../../../core/models/user.models';
import { ChatsShortcutComponent } from '../../../shared/components/chats-shortcut/chats-shortcut.component';
import { StatCardComponent } from '../../../shared/components/stat-card/stat-card.component';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, StatCardComponent, ChatsShortcutComponent],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.scss',
})
export class AdminDashboardComponent implements OnInit {
  private readonly http = inject(HttpClient);
  protected readonly auth = inject(AuthService);

  protected readonly pending = signal<UserResponse[]>([]);
  protected readonly loading = signal(true);

  /** Listado completo + filtro de rol. */
  protected readonly allUsers = signal<UserResponse[]>([]);
  protected readonly loadingAll = signal(true);
  protected readonly deleteError = signal<string | null>(null);
  protected roleFilter: Role | '' = '';

  protected readonly roleFilters: Array<{ value: Role | ''; label: string }> = [
    { value: '', label: 'Tod@s' },
    { value: 'COLLECTOR', label: 'Coleccionistas' },
    { value: 'FOUNDATION', label: 'Fundaciones' },
    { value: 'TRANSPORT', label: 'Transportistas' },
    { value: 'ADMIN', label: 'Administración' },
  ];

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
    this.refreshAll();
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

  refreshAll(): void {
    this.loadingAll.set(true);
    let params = new HttpParams();
    if (this.roleFilter) params = params.set('role', this.roleFilter);
    this.http
      .get<UserResponse[]>(`${environment.apiBaseUrl}/admin/users`, { params })
      .subscribe({
        next: (users) => {
          this.allUsers.set(users);
          this.loadingAll.set(false);
        },
        error: () => this.loadingAll.set(false),
      });
  }

  approve(id: number): void {
    this.http
      .post(`${environment.apiBaseUrl}/admin/approve/${id}`, {}, { responseType: 'text' })
      .subscribe(() => {
        this.refresh();
        this.refreshAll();
      });
  }

  reject(id: number): void {
    this.http
      .post(`${environment.apiBaseUrl}/admin/reject/${id}`, {}, { responseType: 'text' })
      .subscribe(() => {
        this.refresh();
        this.refreshAll();
      });
  }

  deleteUser(u: UserResponse): void {
    if (u.id === this.auth.userId()) {
      this.deleteError.set('No puedes eliminar tu propia cuenta de administración.');
      return;
    }
    if (!confirm(`¿Eliminar la cuenta de "${u.name}" (${u.email})? Esta acción no se puede deshacer.`)) {
      return;
    }
    this.deleteError.set(null);
    this.http
      .delete(`${environment.apiBaseUrl}/admin/users/${u.id}`, { responseType: 'text' })
      .subscribe({
        next: () => this.refreshAll(),
        error: (err) => {
          this.deleteError.set(err?.error?.message ?? 'No se pudo eliminar la cuenta.');
        },
      });
  }

  /** Inicial para el avatar circular. */
  initial(name: string): string {
    return name?.charAt(0).toUpperCase() ?? '?';
  }

  /** URL pública del adjunto de verificación (GET /api/files/{id} es permitAll). */
  fileUrl(fileId: string): string {
    return `${environment.apiBaseUrl}/files/${fileId}`;
  }

  /** Detecta si el adjunto es imagen para renderizar miniatura inline. */
  isImage(mime?: string | null): boolean {
    return !!mime && mime.startsWith('image/');
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
