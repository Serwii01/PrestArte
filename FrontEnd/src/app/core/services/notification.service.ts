import { HttpClient } from '@angular/common/http';
import { computed, effect, inject, Injectable, signal } from '@angular/core';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import { LoanResponse } from '../models/loan.models';
import { ShipmentResponse } from '../models/shipment.models';
import { UserResponse } from '../models/user.models';
import { AuthService } from './auth.service';
import { LoanService } from './loan.service';
import { ShipmentService } from './shipment.service';

export type NotificationSeverity = 'info' | 'warning' | 'success';

export interface NotificationItem {
  id: string;
  icon: string;
  title: string;
  subtitle?: string;
  link: any[];
  severity: NotificationSeverity;
}

/**
 * Notificaciones derivadas: no hay tabla de notificaciones en BD; agregamos
 * acciones pendientes a partir de los datos que cada rol ya consulta
 * (préstamos, envíos, registros pendientes). Refresca al iniciar sesión,
 * al cambiar de rol, y cada 60 segundos mientras la pestaña está abierta.
 */
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly loanService = inject(LoanService);
  private readonly shipmentService = inject(ShipmentService);

  readonly notifications = signal<NotificationItem[]>([]);
  readonly loading = signal(false);
  readonly unreadCount = computed(() => this.notifications().length);

  private pollHandle?: ReturnType<typeof setInterval>;

  constructor() {
    // Cuando hay sesión, refrescamos y arrancamos el polling.
    // Cuando se cierra, vaciamos y paramos.
    effect(() => {
      const role = this.auth.role();
      const userId = this.auth.userId();
      this.stopPolling();
      if (role && userId != null) {
        this.refresh();
        this.pollHandle = setInterval(() => this.refresh(), 60_000);
      } else {
        this.notifications.set([]);
      }
    });
  }

  private stopPolling(): void {
    if (this.pollHandle) {
      clearInterval(this.pollHandle);
      this.pollHandle = undefined;
    }
  }

  /** Fuerza una recarga de las notificaciones para el usuario actual. */
  refresh(): void {
    const role = this.auth.role();
    const userId = this.auth.userId();
    if (!role || userId == null) return;

    this.loading.set(true);
    switch (role) {
      case 'COLLECTOR':
        this.loadCollector(userId);
        break;
      case 'FOUNDATION':
        this.loadFoundation(userId);
        break;
      case 'TRANSPORT':
        this.loadTransport(userId);
        break;
      case 'ADMIN':
        this.loadAdmin();
        break;
    }
  }

  // ===== Por rol =====

  private loadCollector(collectorId: number): void {
    this.loanService
      .getByCollector(collectorId)
      .pipe(catchError(() => of<LoanResponse[]>([])))
      .subscribe((loans) => {
        const items: NotificationItem[] = [];
        for (const l of loans) {
          if (l.status === 'REQUESTED') {
            items.push({
              id: `loan-${l.id}-requested`,
              icon: 'mark_email_unread',
              title: 'Nueva solicitud de préstamo',
              subtitle: `${l.foundationName} pide "${l.artworkTitle}"`,
              link: ['/app/loans', l.id],
              severity: 'warning',
            });
          } else if (l.status === 'PAID') {
            items.push({
              id: `loan-${l.id}-paid`,
              icon: 'inventory_2',
              title: 'Lista para preparar',
              subtitle: `Marca "${l.artworkTitle}" como lista para recoger`,
              link: ['/app/loans', l.id],
              severity: 'info',
            });
          } else if (l.status === 'RETURNING') {
            items.push({
              id: `loan-${l.id}-returning`,
              icon: 'undo',
              title: 'Obra en devolución',
              subtitle: `Confirma la entrega de "${l.artworkTitle}" cuando llegue`,
              link: ['/app/loans', l.id],
              severity: 'info',
            });
          }
        }
        this.notifications.set(items);
        this.loading.set(false);
      });
  }

  private loadFoundation(foundationId: number): void {
    this.loanService
      .getByFoundation(foundationId)
      .pipe(catchError(() => of<LoanResponse[]>([])))
      .subscribe((loans) => {
        // Para cada préstamo en estados con shipment activo, miramos el envío.
        const candidates = loans.filter(
          (l) =>
            l.shipmentId != null &&
            ['QUOTE_PENDING', 'QUOTE_PROPOSED', 'IN_TRANSIT'].includes(l.status),
        );
        if (candidates.length === 0) {
          this.notifications.set([]);
          this.loading.set(false);
          return;
        }
        const ships = candidates.map((l) =>
          this.shipmentService
            .getById(l.shipmentId!)
            .pipe(catchError(() => of<ShipmentResponse | null>(null))),
        );
        forkJoin(ships).subscribe((shipments) => {
          const items: NotificationItem[] = [];
          candidates.forEach((l, idx) => {
            const s = shipments[idx];
            if (!s) return;
            if (s.status === 'QUOTED') {
              items.push({
                id: `ship-${s.id}-quoted`,
                icon: 'receipt_long',
                title: 'Presupuesto recibido',
                subtitle: `"${l.artworkTitle}" · ${s.transportCompanyName}`,
                link: ['/app/loans', l.id],
                severity: 'warning',
              });
            } else if (s.status === 'REJECTED' && !l.transportCompanyMandatory) {
              items.push({
                id: `ship-${s.id}-reassign`,
                icon: 'swap_horiz',
                title: 'Asigna otra empresa de transporte',
                subtitle: `Presupuesto de ${s.transportCompanyName} rechazado para "${l.artworkTitle}"`,
                link: ['/app/loans', l.id],
                severity: 'warning',
              });
            } else if (s.status === 'IN_TRANSIT') {
              items.push({
                id: `ship-${s.id}-arriving`,
                icon: 'local_shipping',
                title: 'Obra en camino',
                subtitle: `Confirma la recepción de "${l.artworkTitle}"`,
                link: ['/app/loans', l.id],
                severity: 'info',
              });
            }
          });
          this.notifications.set(items);
          this.loading.set(false);
        });
      });
  }

  private loadTransport(companyId: number): void {
    this.shipmentService
      .getByCompany(companyId)
      .pipe(catchError(() => of<ShipmentResponse[]>([])))
      .subscribe((shipments) => {
        const items: NotificationItem[] = [];
        for (const s of shipments) {
          if (s.status === 'REQUESTED') {
            items.push({
              id: `ship-${s.id}-requested`,
              icon: 'request_quote',
              title: 'Sube tu presupuesto',
              subtitle: `${s.artworkTitle} · tracking ${s.trackingNumber}`,
              link: ['/app/loans', s.loanRequestId],
              severity: 'warning',
            });
          } else if (s.status === 'APPROVED') {
            items.push({
              id: `ship-${s.id}-approved`,
              icon: 'inventory',
              title: 'Servicio aprobado',
              subtitle: `Recoge "${s.artworkTitle}" cuando esté listo`,
              link: ['/app/loans', s.loanRequestId],
              severity: 'info',
            });
          } else if (s.status === 'PICKED_UP') {
            items.push({
              id: `ship-${s.id}-pickedup`,
              icon: 'local_shipping',
              title: 'Marca como en tránsito',
              subtitle: `"${s.artworkTitle}" recogida, falta el siguiente paso`,
              link: ['/app/loans', s.loanRequestId],
              severity: 'info',
            });
          }
        }
        this.notifications.set(items);
        this.loading.set(false);
      });
  }

  private loadAdmin(): void {
    this.http
      .get<UserResponse[]>(`${environment.apiBaseUrl}/admin/pending-users`)
      .pipe(catchError(() => of<UserResponse[]>([])))
      .subscribe((users) => {
        const items: NotificationItem[] = users.map((u) => ({
          id: `user-${u.id}-pending`,
          icon: 'how_to_reg',
          title: `Aprobar a ${u.name}`,
          subtitle: `${u.email} · ${this.roleLabel(u.role)}`,
          link: ['/app/admin'],
          severity: 'warning' as NotificationSeverity,
        }));
        this.notifications.set(items);
        this.loading.set(false);
      });
  }

  private roleLabel(role: UserResponse['role']): string {
    return (
      {
        ADMIN: 'Administrador',
        COLLECTOR: 'Coleccionista',
        FOUNDATION: 'Fundación',
        TRANSPORT: 'Transporte',
      } as const
    )[role];
  }
}
