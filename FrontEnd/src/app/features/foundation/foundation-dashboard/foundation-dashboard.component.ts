import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';
import { LoanService } from '../../../core/services/loan.service';
import { LoanResponse } from '../../../core/models/loan.models';
import { ChatsShortcutComponent } from '../../../shared/components/chats-shortcut/chats-shortcut.component';
import { StatCardComponent } from '../../../shared/components/stat-card/stat-card.component';
import { StatusPillComponent } from '../../../shared/components/status-pill/status-pill.component';

@Component({
  selector: 'app-foundation-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, StatCardComponent, StatusPillComponent, ChatsShortcutComponent],
  templateUrl: './foundation-dashboard.component.html',
  styleUrl: './foundation-dashboard.component.scss',
})
export class FoundationDashboardComponent implements OnInit {
  private readonly loanService = inject(LoanService);
  protected readonly auth = inject(AuthService);

  protected readonly loans = signal<LoanResponse[]>([]);
  protected readonly loading = signal(true);

  /* ===== Stats ===== */

  /** Solicitudes que requieren acción del museo (presupuesto a aprobar). */
  protected readonly awaitingMyApproval = computed(() =>
    this.loans().filter((l) => l.status === 'QUOTE_PROPOSED'),
  );

  protected readonly awaitingMyApprovalCount = computed(
    () => this.awaitingMyApproval().length,
  );

  /** Préstamos activos en el museo (obra ya en posesión). */
  protected readonly inMuseumCount = computed(
    () => this.loans().filter((l) => ['DELIVERED', 'ON_LOAN'].includes(l.status)).length,
  );

  /** Préstamos en tránsito hacia el museo. */
  protected readonly incomingCount = computed(
    () =>
      this.loans().filter((l) =>
        ['ACCEPTED', 'QUOTE_PENDING', 'QUOTE_PROPOSED', 'PAID', 'READY_FOR_PICKUP', 'IN_TRANSIT'].includes(l.status),
      ).length,
  );

  protected readonly totalLoansCount = computed(() => this.loans().length);

  ngOnInit(): void {
    const foundationId = this.auth.userId();
    if (foundationId == null) return;

    this.loanService.getByFoundation(foundationId).subscribe({
      next: (loans) => {
        this.loans.set(loans);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
