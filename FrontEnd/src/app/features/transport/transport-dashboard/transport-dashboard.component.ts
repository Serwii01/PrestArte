import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';

import { AuthService } from '../../../core/services/auth.service';
import { ShipmentService } from '../../../core/services/shipment.service';
import {
  SHIPMENT_STATUS_LABEL,
  ShipmentResponse,
} from '../../../core/models/shipment.models';

@Component({
  selector: 'app-transport-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <h1 class="text-2xl font-semibold mb-6">Servicios de transporte</h1>

    <section class="bg-white rounded-xl shadow p-6">
      <h2 class="text-lg font-medium mb-4">Mis envíos</h2>

      @if (loading()) {
        <p class="text-slate-500">Cargando...</p>
      } @else if (shipments().length === 0) {
        <p class="text-slate-500">No tienes envíos asignados.</p>
      } @else {
        <table class="w-full text-sm">
          <thead class="text-left text-slate-500 border-b border-slate-100">
            <tr>
              <th class="py-2">Tracking</th>
              <th class="py-2">Obra</th>
              <th class="py-2">Estado</th>
              <th class="py-2">Precio</th>
            </tr>
          </thead>
          <tbody>
            @for (s of shipments(); track s.id) {
              <tr class="border-b border-slate-50">
                <td class="py-3 font-mono text-xs">{{ s.trackingNumber }}</td>
                <td class="py-3">{{ s.artworkTitle }}</td>
                <td class="py-3">
                  <span class="inline-block px-2 py-0.5 rounded bg-slate-100 text-slate-700">
                    {{ statusLabel(s.status) }}
                  </span>
                </td>
                <td class="py-3 text-slate-600">
                  {{ s.price ? (s.price | number:'1.2-2') + ' €' : '—' }}
                </td>
              </tr>
            }
          </tbody>
        </table>
      }
    </section>
  `,
})
export class TransportDashboardComponent implements OnInit {
  private readonly shipmentService = inject(ShipmentService);
  private readonly auth = inject(AuthService);

  protected readonly shipments = signal<ShipmentResponse[]>([]);
  protected readonly loading = signal(true);

  ngOnInit(): void {
    const companyId = this.auth.userId();
    if (companyId == null) return;

    this.shipmentService.getByCompany(companyId).subscribe({
      next: (shipments) => {
        this.shipments.set(shipments);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  statusLabel(s: ShipmentResponse['status']): string {
    return SHIPMENT_STATUS_LABEL[s] ?? s;
  }
}
