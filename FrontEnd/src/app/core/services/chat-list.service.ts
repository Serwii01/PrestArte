import { inject, Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { LoanStatus } from '../models/loan.models';
import { AuthService } from './auth.service';
import { LoanService } from './loan.service';
import { ShipmentService } from './shipment.service';

/** Resumen mostrado en el dashboard y en la barra lateral del chat. */
export interface ChatSummary {
  /** id del préstamo cuyo chat está vinculado (ruta /app/loans/{id}/chat). */
  loanId: number;
  artworkTitle: string;
  artworkArtist?: string;
  /** Otra parte: fundación si soy coleccionista, coleccionista si soy fundación. */
  counterpartyName?: string;
  status: LoanStatus | string;
}

/**
 * Servicio que devuelve la lista de chats accesibles para el usuario actual,
 * consultando los endpoints que ya existen (préstamos por coleccionista /
 * fundación, envíos por empresa de transporte, listado global del admin).
 *
 * Los chats están 1:1 con préstamos, así que basta con conocer en qué
 * préstamos participa el usuario para tener todos sus chats.
 */
@Injectable({ providedIn: 'root' })
export class ChatListService {
  private readonly auth = inject(AuthService);
  private readonly loanService = inject(LoanService);
  private readonly shipmentService = inject(ShipmentService);

  getMyChats(): Observable<ChatSummary[]> {
    const role = this.auth.role();
    const userId = this.auth.userId();
    if (!role || userId == null) return of([]);

    switch (role) {
      case 'COLLECTOR':
        return this.loanService.getByCollector(userId).pipe(
          map((loans) =>
            loans.map((l) => ({
              loanId: l.id,
              artworkTitle: l.artworkTitle,
              artworkArtist: l.artworkArtist,
              counterpartyName: l.foundationName,
              status: l.status,
            })),
          ),
          catchError(() => of<ChatSummary[]>([])),
        );

      case 'FOUNDATION':
        return this.loanService.getByFoundation(userId).pipe(
          map((loans) =>
            loans.map((l) => ({
              loanId: l.id,
              artworkTitle: l.artworkTitle,
              artworkArtist: l.artworkArtist,
              counterpartyName: l.collectorName,
              status: l.status,
            })),
          ),
          catchError(() => of<ChatSummary[]>([])),
        );

      case 'TRANSPORT':
        // Para transporte, partimos de sus envíos: deduplicamos por préstamo
        // (un préstamo puede tener OUTBOUND + RETURN).
        return this.shipmentService.getByCompany(userId).pipe(
          map((ships) => {
            const seen = new Set<number>();
            const unique: ChatSummary[] = [];
            for (const s of ships) {
              if (s.loanRequestId == null) continue;
              if (seen.has(s.loanRequestId)) continue;
              seen.add(s.loanRequestId);
              unique.push({
                loanId: s.loanRequestId,
                artworkTitle: s.artworkTitle,
                status: s.status,
              });
            }
            return unique;
          }),
          catchError(() => of<ChatSummary[]>([])),
        );

      case 'ADMIN':
        return this.loanService.getAll().pipe(
          map((loans) =>
            loans.map((l) => ({
              loanId: l.id,
              artworkTitle: l.artworkTitle,
              artworkArtist: l.artworkArtist,
              counterpartyName: `${l.foundationName} · ${l.collectorName}`,
              status: l.status,
            })),
          ),
          catchError(() => of<ChatSummary[]>([])),
        );

      default:
        return of([]);
    }
  }
}
