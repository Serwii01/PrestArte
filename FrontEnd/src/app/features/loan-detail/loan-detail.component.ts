import { CommonModule, Location } from '@angular/common';
import { Component, computed, inject, Input, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';

import { ArtworkService } from '../../core/services/artwork.service';
import { AuthService } from '../../core/services/auth.service';
import { LoanService } from '../../core/services/loan.service';
import { ShipmentService } from '../../core/services/shipment.service';
import { TransportCompanyService } from '../../core/services/transport-company.service';
import { ArtworkResponse } from '../../core/models/artwork.models';
import { LoanResponse, LOAN_STATUS_LABEL, LoanStatus } from '../../core/models/loan.models';
import { ShipmentResponse, SHIPMENT_STATUS_LABEL } from '../../core/models/shipment.models';
import { TransportCompanyResponse } from '../../core/models/transport-company.models';
import { StatusPillComponent } from '../../shared/components/status-pill/status-pill.component';

/** Pasos del timeline en el orden visible al usuario. */
const TIMELINE: { status: LoanStatus; label: string; icon: string }[] = [
  { status: 'REQUESTED', label: 'Solicitud enviada', icon: 'forum' },
  { status: 'ACCEPTED', label: 'Coleccionista acepta', icon: 'handshake' },
  { status: 'QUOTE_PENDING', label: 'Esperando presupuesto', icon: 'request_quote' },
  { status: 'QUOTE_PROPOSED', label: 'Presupuesto propuesto', icon: 'receipt_long' },
  { status: 'PAID', label: 'Pagado', icon: 'paid' },
  { status: 'READY_FOR_PICKUP', label: 'Listo para recoger', icon: 'inventory_2' },
  { status: 'IN_TRANSIT', label: 'En tránsito', icon: 'local_shipping' },
  { status: 'DELIVERED', label: 'Entregado', icon: 'check_circle' },
  { status: 'ON_LOAN', label: 'En préstamo', icon: 'museum' },
  { status: 'RETURNING', label: 'En devolución', icon: 'undo' },
  { status: 'RETURNED', label: 'Devuelto', icon: 'task_alt' },
];

/** Estados terminales que cortan el timeline en seco. */
const TERMINAL: LoanStatus[] = ['REJECTED', 'CANCELLED', 'RETURNED'];

@Component({
  selector: 'app-loan-detail',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, StatusPillComponent],
  templateUrl: './loan-detail.component.html',
  styleUrl: './loan-detail.component.scss',
})
export class LoanDetailComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly loanService = inject(LoanService);
  private readonly artworkService = inject(ArtworkService);
  private readonly shipmentService = inject(ShipmentService);
  private readonly companyService = inject(TransportCompanyService);
  protected readonly auth = inject(AuthService);
  private readonly location = inject(Location);

  /** Vinculado a `:id` por withComponentInputBinding(). */
  @Input() id?: string;

  protected readonly loan = signal<LoanResponse | null>(null);
  protected readonly artwork = signal<ArtworkResponse | null>(null);
  protected readonly shipment = signal<ShipmentResponse | null>(null);
  protected readonly returnShipment = signal<ShipmentResponse | null>(null);
  protected readonly companies = signal<TransportCompanyResponse[]>([]);

  /**
   * Devuelve el shipment "activo" según el estado del préstamo: si el loan está
   * en RETURNING, las acciones del transportista y la confirmación de entrega
   * operan sobre el shipment de retorno.
   */
  protected readonly activeShipment = computed(() => {
    const l = this.loan();
    if (l?.status === 'RETURNING') return this.returnShipment();
    return this.shipment();
  });

  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly actionMessage = signal<string | null>(null);
  protected readonly busy = signal(false);

  /** Diálogos inline. */
  protected readonly showAcceptDialog = signal(false);
  protected readonly showCancelDialog = signal(false);
  protected readonly showQuoteDialog = signal(false);
  protected readonly showRejectQuoteDialog = signal(false);
  protected readonly showDeliveryDialog = signal(false);

  /* ===== Forms para los diálogos ===== */

  protected readonly acceptForm = this.fb.nonNullable.group({
    transportCompanyId: [null as number | null, [Validators.required]],
    transportCompanyMandatory: [false],
  });

  protected readonly cancelForm = this.fb.nonNullable.group({
    reason: [''],
  });

  protected readonly quoteForm = this.fb.nonNullable.group({
    price: [null as number | null, [Validators.required, Validators.min(1)]],
    insuranceCost: [null as number | null, [Validators.required, Validators.min(0)]],
    insurancePolicy: [''],
  });

  protected readonly rejectQuoteForm = this.fb.nonNullable.group({
    reason: [''],
  });

  protected readonly deliveryForm = this.fb.nonNullable.group({
    receivedBy: ['', [Validators.required]],
    notes: [''],
  });

  /* ===== Roles y permisos derivados ===== */

  protected readonly role = computed(() => this.auth.role());
  protected readonly userId = computed(() => this.auth.userId());

  protected readonly isCollector = computed(() => {
    const a = this.artwork();
    return this.role() === 'COLLECTOR' && a?.collectorId === this.userId();
  });
  protected readonly isFoundation = computed(
    () => this.role() === 'FOUNDATION' && this.loan()?.foundationId === this.userId(),
  );
  protected readonly isTransport = computed(() => {
    const s = this.activeShipment();
    if (!s || this.role() !== 'TRANSPORT') return false;
    return s.transportCompanyId === this.userId();
  });
  protected readonly isAdmin = computed(() => this.role() === 'ADMIN');

  /* ===== Acciones disponibles según estado ===== */

  protected readonly canAccept = computed(() => this.loan()?.status === 'REQUESTED' && this.isCollector());
  protected readonly canReject = computed(() => this.loan()?.status === 'REQUESTED' && this.isCollector());
  protected readonly canCancel = computed(() => {
    const s = this.loan()?.status;
    if (!s) return false;
    const cancellable: LoanStatus[] = [
      'REQUESTED',
      'ACCEPTED',
      'QUOTE_PENDING',
      'QUOTE_PROPOSED',
      'PAID',
      'READY_FOR_PICKUP',
    ];
    return cancellable.includes(s) && (this.isCollector() || this.isFoundation());
  });
  protected readonly canMarkReady = computed(
    () => this.loan()?.status === 'PAID' && this.isCollector(),
  );
  protected readonly canStartReturn = computed(
    () => this.loan()?.status === 'ON_LOAN' && this.isFoundation(),
  );
  protected readonly canQuote = computed(
    () => this.shipment()?.status === 'REQUESTED' && this.isTransport(),
  );
  protected readonly canApproveQuote = computed(
    () => this.shipment()?.status === 'QUOTED' && this.isFoundation(),
  );
  protected readonly canRejectQuote = computed(
    () => this.shipment()?.status === 'QUOTED' && this.isFoundation(),
  );
  protected readonly canMarkPickedUp = computed(
    () => this.activeShipment()?.status === 'APPROVED' && this.isTransport(),
  );
  protected readonly canMarkInTransit = computed(
    () => this.activeShipment()?.status === 'PICKED_UP' && this.isTransport(),
  );
  /** En OUTBOUND la confirma el museo; en RETURN la confirma el coleccionista. */
  protected readonly canConfirmDelivery = computed(() => {
    const s = this.activeShipment();
    if (!s || s.status !== 'IN_TRANSIT') return false;
    
    return this.loan()?.status === 'RETURNING' 
      ? this.isCollector() 
      : this.isFoundation();
});
  protected readonly canDownloadContract = computed(() => {
    const s = this.loan()?.status;
    if (!s) return false;
    const after: LoanStatus[] = [
      'PAID',
      'READY_FOR_PICKUP',
      'IN_TRANSIT',
      'DELIVERED',
      'ON_LOAN',
      'RETURNING',
      'RETURNED',
    ];
    return after.includes(s);
  });

  /* ===== Timeline ===== */

  protected readonly timelineSteps = computed(() => {
    const current = this.loan()?.status;
    const terminal = current && TERMINAL.includes(current) ? current : null;
    const currentIndex = current ? TIMELINE.findIndex((t) => t.status === current) : -1;

    return TIMELINE.map((step, i) => ({
      ...step,
      state:
        terminal === 'CANCELLED' || terminal === 'REJECTED'
          ? (i === 0 ? 'past' : 'aborted')
          : i < currentIndex
            ? 'past'
            : i === currentIndex
              ? 'current'
              : 'future',
    }));
  });

  protected readonly heroImageUrl = computed(() => {
    const a = this.artwork();
    if (!a?.files || a.files.length === 0) return null;
    return this.artworkService.fileUrl(a.files[0].id);
  });

  protected readonly statusLabel = computed(() => {
    const s = this.loan()?.status;
    return s ? LOAN_STATUS_LABEL[s] : '';
  });

  protected readonly shipmentStatusLabel = computed(() => {
    const s = this.shipment()?.status;
    return s ? SHIPMENT_STATUS_LABEL[s] : '';
  });

  ngOnInit(): void {
    this.reload();
  }

  /** Recarga el loan + sus dependencias (artwork, shipment). */
  private reload(): void {
    const id = Number(this.id);
    if (!id) {
      this.errorMessage.set('Identificador de préstamo inválido.');
      this.loading.set(false);
      return;
    }

    this.loading.set(true);
    this.loanService.getById(id).subscribe({
      next: (loan) => {
        this.loan.set(loan);
        const tasks = forkJoin({
          art: this.artworkService.getById(loan.artworkId),
          ship: loan.shipmentId
            ? this.shipmentService.getById(loan.shipmentId)
            : of(null),
          ret: loan.returnShipmentId
            ? this.shipmentService.getById(loan.returnShipmentId)
            : of(null),
          companies:
            loan.status === 'REQUESTED' && this.role() === 'COLLECTOR'
              ? this.companyService.getAll()
              : of([] as TransportCompanyResponse[]),
        });
        tasks.subscribe({
          next: ({ art, ship, ret, companies }) => {
            this.artwork.set(art);
            this.shipment.set(ship);
            this.returnShipment.set(ret);
            this.companies.set(companies);
            this.applyPreferredCompanyToAcceptForm();
            this.loading.set(false);
          },
          error: () => {
            this.errorMessage.set('No se pudieron cargar los datos del préstamo.');
            this.loading.set(false);
          },
        });
      },
      error: () => {
        this.errorMessage.set('No se pudo cargar el préstamo.');
        this.loading.set(false);
      },
    });
  }

  /** Si la obra tiene empresa preferida, la pre-selecciona en el form de aceptar. */
  private applyPreferredCompanyToAcceptForm(): void {
    const a = this.artwork();
    if (!a?.preferredTransportCompanyId) return;
    this.acceptForm.controls.transportCompanyId.setValue(a.preferredTransportCompanyId);
    this.acceptForm.controls.transportCompanyMandatory.setValue(
      a.preferredTransportMandatory ?? false,
    );
    if (a.preferredTransportMandatory) {
      // Si es obligatoria, no se puede cambiar.
      this.acceptForm.controls.transportCompanyId.disable();
      this.acceptForm.controls.transportCompanyMandatory.disable();
    }
  }

  /* ===== Acciones ===== */

  goBack(): void {
    this.location.back();
  }

  fileUrl(fileId: string): string {
    return this.artworkService.fileUrl(fileId);
  }

  openAccept(): void {
    this.acceptForm.reset({ transportCompanyId: null, transportCompanyMandatory: false });
    this.applyPreferredCompanyToAcceptForm();
    this.showAcceptDialog.set(true);
  }

  confirmAccept(): void {
    const id = this.loan()?.id;
    if (!id || this.acceptForm.invalid) return;
    const v = this.acceptForm.getRawValue();
    this.run(this.loanService.accept(id, {
      transportCompanyId: v.transportCompanyId!,
      transportCompanyMandatory: v.transportCompanyMandatory,
    }), 'Préstamo aceptado.', () => this.showAcceptDialog.set(false));
  }

  rejectLoan(): void {
    const id = this.loan()?.id;
    if (!id) return;
    if (!confirm('¿Seguro que quieres rechazar esta solicitud? La acción no se puede deshacer.')) return;
    this.run(this.loanService.reject(id), 'Solicitud rechazada.');
  }

  openCancel(): void {
    this.cancelForm.reset({ reason: '' });
    this.showCancelDialog.set(true);
  }

  confirmCancel(): void {
    const id = this.loan()?.id;
    if (!id) return;
    const reason = this.cancelForm.controls.reason.value || undefined;
    this.run(this.loanService.cancel(id, { reason }), 'Préstamo cancelado.', () =>
      this.showCancelDialog.set(false),
    );
  }

  markReady(): void {
    const id = this.loan()?.id;
    if (!id) return;
    this.run(this.loanService.markReadyForPickup(id), 'Marcada como lista para recoger.');
  }

  startReturn(): void {
    const id = this.loan()?.id;
    if (!id) return;
    if (!confirm('¿Iniciar el retorno? Se generará un envío de vuelta con la misma empresa de transporte.')) return;
    this.run(this.loanService.startReturn(id), 'Retorno iniciado. La empresa de transporte recogerá la obra.');
  }

  openQuote(): void {
    this.quoteForm.reset({ price: null, insuranceCost: null, insurancePolicy: '' });
    this.showQuoteDialog.set(true);
  }

  confirmQuote(): void {
    const sId = this.shipment()?.id;
    if (!sId || this.quoteForm.invalid) return;
    const v = this.quoteForm.getRawValue();
    this.run(this.shipmentService.proposeQuote(sId, {
      price: v.price!,
      insuranceCost: v.insuranceCost!,
      insurancePolicy: v.insurancePolicy || undefined,
    }), 'Presupuesto enviado.', () => this.showQuoteDialog.set(false));
  }

  approveQuote(): void {
    const sId = this.shipment()?.id;
    if (!sId) return;
    this.run(this.shipmentService.approveQuote(sId), 'Presupuesto aprobado.');
  }

  openRejectQuote(): void {
    this.rejectQuoteForm.reset({ reason: '' });
    this.showRejectQuoteDialog.set(true);
  }

  confirmRejectQuote(): void {
    const sId = this.shipment()?.id;
    if (!sId) return;
    const reason = this.rejectQuoteForm.controls.reason.value || undefined;
    this.run(this.shipmentService.rejectQuote(sId, { reason }), 'Presupuesto rechazado.', () =>
      this.showRejectQuoteDialog.set(false),
    );
  }

  markPickedUp(): void {
    const sId = this.activeShipment()?.id;
    if (!sId) return;
    this.run(this.shipmentService.markPickedUp(sId), 'Recogida confirmada.');
  }

  markInTransit(): void {
    const sId = this.activeShipment()?.id;
    if (!sId) return;
    this.run(this.shipmentService.markInTransit(sId), 'En tránsito.');
  }

  openDelivery(): void {
    this.deliveryForm.reset({ receivedBy: '', notes: '' });
    this.showDeliveryDialog.set(true);
  }

  confirmDelivery(): void {
    const sId = this.activeShipment()?.id;
    if (!sId || this.deliveryForm.invalid) return;
    const isReturn = this.loan()?.status === 'RETURNING';
    const v = this.deliveryForm.getRawValue();
    this.run(this.shipmentService.confirmDelivery(sId, {
      receivedBy: v.receivedBy,
      notes: v.notes || undefined,
    }),
    isReturn ? 'Retorno completado. Préstamo cerrado.' : 'Entrega confirmada.',
    () => this.showDeliveryDialog.set(false));
  }

  downloadContract(): void {
    const id = this.loan()?.id;
    if (!id) return;
    this.loanService.downloadContract(id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `contrato_prestamo_${id}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.errorMessage.set('No se pudo descargar el contrato.'),
    });
  }

  /** Wrapper que ejecuta una acción del flujo, recarga y muestra feedback. */
  private run(obs: { subscribe: Function }, successMsg: string, onAfter?: () => void): void {
    this.busy.set(true);
    this.errorMessage.set(null);
    this.actionMessage.set(null);
    (obs as any).subscribe({
      next: () => {
        this.busy.set(false);
        this.actionMessage.set(successMsg);
        onAfter?.();
        this.reload();
      },
      error: (err: any) => {
        this.busy.set(false);
        this.errorMessage.set(err?.error?.message ?? 'No se pudo completar la acción.');
      },
    });
  }
}
