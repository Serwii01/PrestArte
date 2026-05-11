import { CommonModule } from '@angular/common';
import { Component, computed, input } from '@angular/core';

import {
  LOAN_STATUS_LABEL,
  LoanStatus,
} from '../../../core/models/loan.models';
import {
  SHIPMENT_STATUS_LABEL,
  ShipmentStatus,
} from '../../../core/models/shipment.models';

type AnyStatus = LoanStatus | ShipmentStatus | string;

interface StatusStyle {
  /** Tailwind classes para fondo y texto del pill. */
  bg: string;
  text: string;
  /** Color del puntito a la izquierda. */
  dot: string;
}

/**
 * Pill de estado con dot + label, alineado al lenguaje visual de Stitch.
 * Acepta cualquier estado de Loan o Shipment y mapea automáticamente etiqueta y color.
 */
@Component({
  selector: 'app-status-pill',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './status-pill.component.html',
  styleUrl: './status-pill.component.scss',
})
export class StatusPillComponent {
  readonly status = input.required<AnyStatus>();

  /** Permite forzar un label custom; si no, mapea por status. */
  readonly customLabel = input<string | undefined>(undefined);

  protected readonly label = computed(() => {
    const custom = this.customLabel();
    if (custom) return custom;
    const s = this.status();
    return (
      LOAN_STATUS_LABEL[s as LoanStatus] ??
      SHIPMENT_STATUS_LABEL[s as ShipmentStatus] ??
      s
    );
  });

  protected readonly style = computed<StatusStyle>(() => STATUS_STYLES[this.status() as AnyStatus] ?? DEFAULT);
}

const DEFAULT: StatusStyle = { bg: 'bg-gray-100', text: 'text-text-main', dot: 'bg-gray-400' };

/** Mapeo coherente de estados (Loan + Shipment) a paleta de pills. */
const STATUS_STYLES: Record<string, StatusStyle> = {
  // ----- Loan -----
  REQUESTED: { bg: 'bg-blue-100', text: 'text-blue-800', dot: 'bg-blue-500' },
  ACCEPTED: { bg: 'bg-emerald-100', text: 'text-emerald-800', dot: 'bg-emerald-500' },
  QUOTE_PENDING: { bg: 'bg-yellow-100', text: 'text-yellow-800', dot: 'bg-yellow-500' },
  QUOTE_PROPOSED: { bg: 'bg-amber-100', text: 'text-amber-800', dot: 'bg-amber-500' },
  PAID: { bg: 'bg-emerald-100', text: 'text-emerald-800', dot: 'bg-emerald-500' },
  READY_FOR_PICKUP: { bg: 'bg-purple-100', text: 'text-purple-800', dot: 'bg-purple-500' },
  IN_TRANSIT: { bg: 'bg-yellow-100', text: 'text-yellow-800', dot: 'bg-yellow-500' },
  DELIVERED: { bg: 'bg-emerald-100', text: 'text-emerald-800', dot: 'bg-emerald-500' },
  ON_LOAN: { bg: 'bg-emerald-100', text: 'text-emerald-800', dot: 'bg-emerald-500' },
  RETURNING: { bg: 'bg-orange-100', text: 'text-orange-800', dot: 'bg-orange-500' },
  RETURNED: { bg: 'bg-slate-100', text: 'text-slate-700', dot: 'bg-slate-500' },
  REJECTED: { bg: 'bg-red-100', text: 'text-red-800', dot: 'bg-red-500' },
  CANCELLED: { bg: 'bg-red-100', text: 'text-red-800', dot: 'bg-red-500' },
  // ----- Shipment exclusivos -----
  QUOTED: { bg: 'bg-amber-100', text: 'text-amber-800', dot: 'bg-amber-500' },
  APPROVED: { bg: 'bg-emerald-100', text: 'text-emerald-800', dot: 'bg-emerald-500' },
  PICKED_UP: { bg: 'bg-purple-100', text: 'text-purple-800', dot: 'bg-purple-500' },
};
