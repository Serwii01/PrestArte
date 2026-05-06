import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';

import { environment } from '../../../../environments/environment';
import { UserResponse } from '../../../core/models/user.models';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <h1 class="text-2xl font-semibold mb-6">Panel de administración</h1>

    <section class="bg-white rounded-xl shadow p-6">
      <h2 class="text-lg font-medium mb-4">Usuarios pendientes de aprobación</h2>

      @if (loading()) {
        <p class="text-slate-500">Cargando...</p>
      } @else if (pending().length === 0) {
        <p class="text-slate-500">No hay usuarios pendientes.</p>
      } @else {
        <ul class="divide-y divide-slate-100">
          @for (u of pending(); track u.id) {
            <li class="py-3 flex items-center justify-between">
              <div>
                <p class="font-medium">{{ u.name }} <span class="text-xs text-slate-400">({{ u.role }})</span></p>
                <p class="text-sm text-slate-500">{{ u.email }} · {{ u.taxId }}</p>
              </div>
              <div class="flex gap-2">
                <button (click)="approve(u.id)"
                        class="px-3 py-1.5 rounded bg-emerald-600 text-white text-sm hover:bg-emerald-700">
                  Aprobar
                </button>
                <button (click)="reject(u.id)"
                        class="px-3 py-1.5 rounded bg-red-600 text-white text-sm hover:bg-red-700">
                  Rechazar
                </button>
              </div>
            </li>
          }
        </ul>
      }
    </section>
  `,
})
export class AdminDashboardComponent implements OnInit {
  private readonly http = inject(HttpClient);

  protected readonly pending = signal<UserResponse[]>([]);
  protected readonly loading = signal(true);

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
}
