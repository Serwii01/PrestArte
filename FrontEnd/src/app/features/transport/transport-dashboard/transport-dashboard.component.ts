import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';
import { ShipmentService } from '../../../core/services/shipment.service';
import { ShipmentResponse } from '../../../core/models/shipment.models';
import { StatCardComponent } from '../../../shared/components/stat-card/stat-card.component';
import { StatusPillComponent } from '../../../shared/components/status-pill/status-pill.component';

@Component({
  selector: 'app-transport-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, StatCardComponent, StatusPillComponent],
  templateUrl: './transport-dashboard.component.html',
  styleUrl: './transport-dashboard.component.scss',
})
export class TransportDashboardComponent implements OnInit {
  private readonly shipmentService = inject(ShipmentService);
  protected readonly auth = inject(AuthService);

  protected readonly shipments = signal<ShipmentResponse[]>([]);
  protected readonly loading = signal(true);

  /* ===== Stats ===== */

  /** Servicios sin presupuesto aún. */
  protected readonly toQuote = computed(() =>
    this.shipments().filter((s) => s.status === 'REQUESTED'),
  );
  protected readonly toQuoteCount = computed(() => this.toQuote().length);

  /** Presupuestos enviados, esperando que el museo los apruebe. */
  protected readonly awaitingApprovalCount = computed(
    () => this.shipments().filter((s) => s.status === 'QUOTED').length,
  );

  /** Servicios en curso (aprobados/recogidos/en tránsito). */
  protected readonly activeCount = computed(
    () =>
      this.shipments().filter((s) =>
        ['APPROVED', 'PICKED_UP', 'IN_TRANSIT'].includes(s.status),
      ).length,
  );

  /** Servicios completados. */
  protected readonly deliveredCount = computed(
    () => this.shipments().filter((s) => s.status === 'DELIVERED').length,
  );

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
}
