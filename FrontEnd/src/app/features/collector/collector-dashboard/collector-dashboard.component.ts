import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ArtworkService } from '../../../core/services/artwork.service';
import { AuthService } from '../../../core/services/auth.service';
import { LoanService } from '../../../core/services/loan.service';
import { LoanResponse } from '../../../core/models/loan.models';
import { ArtworkResponse, isArtworkImage } from '../../../core/models/artwork.models';
import { StatCardComponent } from '../../../shared/components/stat-card/stat-card.component';
import { StatusPillComponent } from '../../../shared/components/status-pill/status-pill.component';

@Component({
  selector: 'app-collector-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, StatCardComponent, StatusPillComponent],
  templateUrl: './collector-dashboard.component.html',
  styleUrl: './collector-dashboard.component.scss',
})
export class CollectorDashboardComponent implements OnInit {
  private readonly loanService = inject(LoanService);
  protected readonly artworkService = inject(ArtworkService);
  protected readonly auth = inject(AuthService);

  protected readonly loans = signal<LoanResponse[]>([]);
  protected readonly artworks = signal<ArtworkResponse[]>([]);
  protected readonly loading = signal(true);

  /* ===== Stats derivados ===== */

  /** Préstamos pendientes de respuesta del coleccionista. */
  protected readonly pendingRequests = computed(() =>
    this.loans().filter((l) => l.status === 'REQUESTED'),
  );

  protected readonly pendingRequestsCount = computed(() => this.pendingRequests().length);

  /** Préstamos activos (no terminales). */
  protected readonly activeLoansCount = computed(
    () =>
      this.loans().filter(
        (l) => !['REJECTED', 'CANCELLED', 'RETURNED'].includes(l.status),
      ).length,
  );

  protected readonly totalArtworks = computed(() => this.artworks().length);

  /** Suma de valor estimado de todas las obras del coleccionista, formateado en €. */
  protected readonly totalValueLabel = computed(() => {
    const total = this.artworks().reduce((acc, a) => acc + (a.estimatedValue ?? 0), 0);
    if (total === 0) return '—';
    return new Intl.NumberFormat('es-ES', {
      style: 'currency',
      currency: 'EUR',
      maximumFractionDigits: 0,
    }).format(total);
  });

  ngOnInit(): void {
    const collectorId = this.auth.userId();
    if (collectorId == null) return;

    this.loanService.getByCollector(collectorId).subscribe({
      next: (loans) => {
        this.loans.set(loans);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });

    // Endpoint dedicado del backend: /api/artworks/collector/{id}
    this.artworkService.getByCollector(collectorId).subscribe({
      next: (mine) => this.artworks.set(mine),
    });
  }

  /** URL de la primera imagen (excluye documentos adjuntos). */
  mainImageUrl(a: ArtworkResponse): string | null {
    const first = (a.files ?? []).find(isArtworkImage);
    return first ? this.artworkService.fileUrl(first.id) : null;
  }
}
