import { CommonModule } from '@angular/common';
import { Component, computed, inject, Input, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { catchError, forkJoin, of } from 'rxjs';

import { ArtworkService } from '../../core/services/artwork.service';
import { AuthService } from '../../core/services/auth.service';
import { LoanService } from '../../core/services/loan.service';
import { ShipmentService } from '../../core/services/shipment.service';
import { TransportCompanyService } from '../../core/services/transport-company.service';
import {
  ArtworkResponse,
  Condition,
  CONDITION_LABEL,
  CONDITION_OPTIONS,
  isArtworkImage,
} from '../../core/models/artwork.models';
import {
  COVERAGE_LABEL,
  CoverageType,
  LoanDocumentsResponse,
  LoanEventResponse,
  LoanResponse,
  LOAN_STATUS_LABEL,
  LoanStatus,
} from '../../core/models/loan.models';
import {
  ConditionReportResponse,
  ShipmentResponse,
  SHIPMENT_STATUS_LABEL,
} from '../../core/models/shipment.models';
import { TransportCompanyProfile } from '../../core/models/transport-company.models';
import { StatusPillComponent } from '../../shared/components/status-pill/status-pill.component';
import { SecureFilePipe } from '../../shared/pipes/secure-file.pipe';
import { CompatibilityReport } from '../../core/models/facility.models';
import {
  CostEstimate,
  SUPERVISION_STAGE_LABEL,
  SupervisionCheckpoint,
  SupervisionStage,
} from '../../core/models/cost.models';
import { CostEstimatePanelComponent } from '../../shared/components/cost-estimate-panel/cost-estimate-panel.component';
import { LenderTermsComponent } from '../../shared/components/lender-terms/lender-terms.component';
import { RatingBadgeComponent } from '../../shared/components/rating-badge/rating-badge.component';
import { LoanReviews, Reviewable } from '../../core/models/review.models';
import { CompatibilityPanelComponent } from '../../shared/components/compatibility-panel/compatibility-panel.component';
import { RequirementsSummaryComponent } from '../../shared/components/requirements-summary/requirements-summary.component';

/** Máximo de fotos por informe de estado (igual que en el backend). */
const MAX_REPORT_PHOTOS = 10;

const ROLE_LABEL: Record<string, string> = {
  COLLECTOR: 'Coleccionista',
  FOUNDATION: 'Fundación',
  TRANSPORT: 'Transporte',
  ADMIN: 'Administración',
};

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
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    StatusPillComponent,
    SecureFilePipe,
    CompatibilityPanelComponent,
    RequirementsSummaryComponent,
    CostEstimatePanelComponent,
    LenderTermsComponent,
    RatingBadgeComponent,
  ],
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
  private readonly router = inject(Router);

  /** Vinculado a `:id` por withComponentInputBinding(). */
  @Input() id?: string;

  protected readonly loan = signal<LoanResponse | null>(null);
  protected readonly artwork = signal<ArtworkResponse | null>(null);
  protected readonly shipment = signal<ShipmentResponse | null>(null);
  protected readonly returnShipment = signal<ShipmentResponse | null>(null);
  protected readonly companies = signal<TransportCompanyProfile[]>([]);
  protected readonly history = signal<LoanEventResponse[]>([]);
  protected readonly conditionReports = signal<ConditionReportResponse[]>([]);
  protected readonly compatibility = signal<CompatibilityReport | null>(null);
  protected readonly documents = signal<LoanDocumentsResponse | null>(null);
  protected readonly costEstimate = signal<CostEstimate | null>(null);

  /* ===== Ampliación, certificado y valoraciones ===== */

  protected readonly reviews = signal<LoanReviews | null>(null);
  protected readonly showExtensionDialog = signal(false);
  protected readonly extensionRejectReason = signal('');
  protected readonly reviewTarget = signal<Reviewable | null>(null);

  protected readonly extensionForm = this.fb.nonNullable.group({
    newEndDate: ['', [Validators.required]],
    reason: ['', [Validators.maxLength(500)]],
  });

  protected readonly reviewForm = this.fb.nonNullable.group({
    rating: [5, [Validators.required, Validators.min(1), Validators.max(5)]],
    comment: ['', [Validators.maxLength(1000)]],
  });
  protected readonly supervision = signal<SupervisionCheckpoint[]>([]);
  protected readonly stageLabel = SUPERVISION_STAGE_LABEL;
  protected readonly stages = Object.keys(SUPERVISION_STAGE_LABEL) as SupervisionStage[];

  /* ===== Supervisión remota (correo virtual) ===== */

  protected readonly showCheckpointDialog = signal(false);
  protected readonly showCheckpointChangesDialog = signal<SupervisionCheckpoint | null>(null);

  protected readonly checkpointForm = this.fb.nonNullable.group({
    stage: ['PACKING' as SupervisionStage, [Validators.required]],
    note: ['', [Validators.maxLength(2000)]],
  });

  protected readonly checkpointChangesForm = this.fb.nonNullable.group({
    reason: ['', [Validators.required, Validators.maxLength(500)]],
  });
  protected readonly coverageLabel = COVERAGE_LABEL;

  /* ===== Seguro y permiso de exportación ===== */

  protected readonly showInsuranceDialog = signal(false);
  protected readonly showRejectInsuranceDialog = signal(false);
  protected readonly showPermitDialog = signal(false);
  protected readonly documentFile = signal<File | null>(null);

  protected readonly insuranceForm = this.fb.nonNullable.group({
    coverageType: ['BORROWER_POLICY' as CoverageType, [Validators.required]],
    insurer: ['', [Validators.required, Validators.maxLength(200)]],
    policyNumber: ['', [Validators.required, Validators.maxLength(100)]],
    insuredAmount: [null as number | null, [Validators.required, Validators.min(1)]],
    coverageStart: ['', [Validators.required]],
    coverageEnd: ['', [Validators.required]],
  });

  protected readonly rejectInsuranceForm = this.fb.nonNullable.group({
    reason: ['', [Validators.required, Validators.maxLength(500)]],
  });

  protected readonly permitForm = this.fb.nonNullable.group({
    resolutionNumber: ['', [Validators.required, Validators.maxLength(100)]],
    issuedBy: ['Ministerio de Cultura', [Validators.required, Validators.maxLength(200)]],
    issueDate: ['', [Validators.required]],
    returnDeadline: ['', [Validators.required]],
  });

  /** Fotos elegidas para el informe de estado del diálogo abierto. */
  protected readonly reportPhotos = signal<File[]>([]);
  protected readonly conditionOptions = CONDITION_OPTIONS;
  protected readonly maxReportPhotos = MAX_REPORT_PHOTOS;

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
  protected readonly forbidden = signal(false);
  protected readonly actionMessage = signal<string | null>(null);
  protected readonly busy = signal(false);

  /** Diálogos inline. */
  protected readonly showAcceptDialog = signal(false);
  protected readonly showCancelDialog = signal(false);
  protected readonly showQuoteDialog = signal(false);
  protected readonly showRejectQuoteDialog = signal(false);
  protected readonly showDeliveryDialog = signal(false);
  protected readonly showReassignDialog = signal(false);
  protected readonly showRejectDialog = signal(false);
  protected readonly showPickupDialog = signal(false);

  /* ===== Forms para los diálogos ===== */

  protected readonly acceptForm = this.fb.nonNullable.group({
    transportCompanyId: [null as number | null, [Validators.required]],
    transportCompanyMandatory: [false],
    /** Valor asegurado acordado; se precarga con el valor estimado de la obra. */
    agreedValue: [null as number | null, [Validators.required, Validators.min(1)]],
  });

  protected readonly cancelForm = this.fb.nonNullable.group({
    reason: [''],
  });

  protected readonly quoteForm = this.fb.nonNullable.group({
    price: [null as number | null, [Validators.required, Validators.min(1)]],
    returnPrice: [null as number | null, [Validators.required, Validators.min(0)]],
    insuranceCost: [null as number | null, [Validators.required, Validators.min(0)]],
    insurancePolicy: [''],
  });

  protected readonly rejectQuoteForm = this.fb.nonNullable.group({
    reason: [''],
  });

  protected readonly rejectForm = this.fb.nonNullable.group({
    reason: ['', [Validators.maxLength(500)]],
  });

  /** Informe de estado de salida (lo rellena el transportista al recoger). */
  protected readonly pickupForm = this.fb.nonNullable.group({
    condition: [null as Condition | null, [Validators.required]],
    damageReported: [false],
    notes: ['', [Validators.maxLength(5000)]],
  });

  /** Entrega + informe de estado de llegada (lo rellena quien recibe). */
  protected readonly deliveryForm = this.fb.nonNullable.group({
    receivedBy: ['', [Validators.required, Validators.maxLength(200)]],
    condition: [null as Condition | null, [Validators.required]],
    damageReported: [false],
    notes: ['', [Validators.maxLength(5000)]],
  });

  protected readonly reassignForm = this.fb.nonNullable.group({
    transportCompanyId: [null as number | null, [Validators.required]],
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

  /**
   * El museo o el coleccionista pueden buscar OTRA empresa cuando:
   *  - el préstamo está en QUOTE_PENDING,
   *  - el último envío OUTBOUND está REJECTED (el museo lo descartó),
   *  - la empresa NO era obligatoria (si lo fuera, el préstamo ya estaría CANCELLED).
   */
  protected readonly canReassignTransport = computed(() => {
    const l = this.loan();
    const s = this.shipment();
    if (!l || !s) return false;
    if (l.status !== 'QUOTE_PENDING') return false;
    if (s.status !== 'REJECTED') return false;
    if (l.transportCompanyMandatory) return false;
    return this.isFoundation() || this.isCollector();
  });
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
    const first = a.files.find(isArtworkImage);
    return first ? this.artworkService.fileUrl(first.id) : null;
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
          // Cargamos el listado de empresas cuando:
          //  - el coleccionista está aceptando la solicitud, o
          //  - el préstamo está en QUOTE_PENDING (por si toca reasignar transporte).
          companies:
            (loan.status === 'REQUESTED' && this.role() === 'COLLECTOR')
            || loan.status === 'QUOTE_PENDING'
              ? this.companyService.getAll()
              : of([] as TransportCompanyProfile[]),
          // El historial y los informes no deben bloquear la ficha si fallan.
          history: this.loanService
            .getHistory(id)
            .pipe(catchError(() => of([] as LoanEventResponse[]))),
          reports: this.loanService
            .getConditionReports(id)
            .pipe(catchError(() => of([] as ConditionReportResponse[]))),
          compatibility: this.loanService
            .getCompatibility(id)
            .pipe(catchError(() => of(null as CompatibilityReport | null))),
          documents: this.loanService
            .getDocuments(id)
            .pipe(catchError(() => of(null as LoanDocumentsResponse | null))),
          cost: this.loanService
            .getCostEstimate(id)
            .pipe(catchError(() => of(null as CostEstimate | null))),
          supervision: this.loanService
            .getSupervision(id)
            .pipe(catchError(() => of([] as SupervisionCheckpoint[]))),
          reviews: loan.status === 'RETURNED'
            ? this.loanService.getReviews(id).pipe(catchError(() => of(null as LoanReviews | null)))
            : of(null as LoanReviews | null),
        });
        tasks.subscribe({
          next: ({ art, ship, ret, companies, history, reports, compatibility, documents, cost, supervision, reviews }) => {
            this.reviews.set(reviews);
            this.compatibility.set(compatibility);
            this.documents.set(documents);
            this.costEstimate.set(cost);
            this.supervision.set(supervision);
            this.artwork.set(art);
            this.shipment.set(ship);
            this.returnShipment.set(ret);
            this.companies.set(companies);
            this.history.set([...history].reverse());
            this.conditionReports.set(reports);
            this.applyPreferredCompanyToAcceptForm();
            this.loading.set(false);
          },
          error: () => {
            this.errorMessage.set('No se pudieron cargar los datos del préstamo.');
            this.loading.set(false);
          },
        });
      },
      error: (err: any) => {
        this.loading.set(false);
        if (err?.status === 403) {
          this.forbidden.set(true);
        } else {
          this.errorMessage.set('No se pudo cargar el préstamo.');
        }
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

  /**
     * Vuelve al panel del usuario (HomeRedirect lo lleva al dashboard
     * concreto del rol). Evitamos Location.back() porque después de visitar
     * el chat el "Volver" rebotaría al chat de nuevo.
     */
  goBack(): void {
    this.router.navigate(['/app']);
  }

  fileUrl(fileId: string): string {
    return this.artworkService.fileUrl(fileId);
  }

  openAccept(): void {
    this.acceptForm.reset({
      transportCompanyId: null,
      transportCompanyMandatory: false,
      agreedValue: this.artwork()?.estimatedValue ?? null,
    });
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
      agreedValue: v.agreedValue ?? undefined,
    }), 'Préstamo aceptado.', () => this.showAcceptDialog.set(false));
  }

  /* ===== Documentación: seguro y permiso de exportación ===== */

  /** Entre la aceptación y la recogida se puede gestionar la documentación. */
  protected readonly inDocumentPhase = computed(() => {
    const s = this.loan()?.status;
    const phase: LoanStatus[] = ['ACCEPTED', 'QUOTE_PENDING', 'QUOTE_PROPOSED', 'PAID', 'READY_FOR_PICKUP'];
    return !!s && phase.includes(s);
  });

  /** Se puede subir un seguro si no hay ninguno vivo (pendiente o aprobado). */
  protected readonly canUploadInsurance = computed(() => {
    const ins = this.documents()?.insurance;
    const free = !ins || ins.status === 'REJECTED';
    return this.inDocumentPhase() && free && (this.isFoundation() || this.isCollector());
  });

  protected readonly canReviewInsurance = computed(
    () => this.isCollector() && this.documents()?.insurance?.status === 'PENDING',
  );

  protected readonly canUploadPermit = computed(
    () => this.inDocumentPhase() && !!this.documents()?.permitRequired && (this.isFoundation() || this.isCollector()),
  );

  /** La obra no puede salir mientras falte documentación. */
  protected readonly shipmentBlocked = computed(() => {
    const d = this.documents();
    return !!d && !d.readyForShipment;
  });

  openInsurance(): void {
    const l = this.loan();
    const d = this.documents();
    if (!l) return;
    this.insuranceForm.reset({
      coverageType: this.isCollector() ? 'LENDER_POLICY' : 'BORROWER_POLICY',
      insurer: '',
      policyNumber: '',
      insuredAmount: d?.agreedValue ?? null,
      coverageStart: l.startDate,
      coverageEnd: l.endDate,
    });
    // Cada parte solo puede subir su tipo de póliza.
    if (this.isCollector()) this.insuranceForm.controls.coverageType.disable();
    else this.insuranceForm.controls.coverageType.enable();
    this.documentFile.set(null);
    this.showInsuranceDialog.set(true);
  }

  confirmInsurance(): void {
    const id = this.loan()?.id;
    const file = this.documentFile();
    if (!id || !file || this.insuranceForm.invalid) return;
    const v = this.insuranceForm.getRawValue();
    this.run(
      this.loanService.uploadInsurance(id, {
        coverageType: v.coverageType,
        insurer: v.insurer.trim(),
        policyNumber: v.policyNumber.trim(),
        insuredAmount: v.insuredAmount!,
        coverageStart: v.coverageStart,
        coverageEnd: v.coverageEnd,
      }, file),
      'Certificado de seguro subido.',
      () => this.showInsuranceDialog.set(false),
    );
  }

  approveInsurance(): void {
    const id = this.loan()?.id;
    if (!id) return;
    this.run(this.loanService.approveInsurance(id), 'Certificado de seguro aprobado.');
  }

  openRejectInsurance(): void {
    this.rejectInsuranceForm.reset({ reason: '' });
    this.showRejectInsuranceDialog.set(true);
  }

  confirmRejectInsurance(): void {
    const id = this.loan()?.id;
    if (!id || this.rejectInsuranceForm.invalid) return;
    this.run(
      this.loanService.rejectInsurance(id, this.rejectInsuranceForm.controls.reason.value.trim()),
      'Certificado rechazado. La fundación deberá subir otro.',
      () => this.showRejectInsuranceDialog.set(false),
    );
  }

  openPermit(): void {
    this.permitForm.reset({
      resolutionNumber: '',
      issuedBy: 'Ministerio de Cultura',
      issueDate: '',
      returnDeadline: '',
    });
    this.documentFile.set(null);
    this.showPermitDialog.set(true);
  }

  confirmPermit(): void {
    const id = this.loan()?.id;
    const file = this.documentFile();
    if (!id || !file || this.permitForm.invalid) return;
    const v = this.permitForm.getRawValue();
    this.run(
      this.loanService.uploadExportPermit(id, {
        resolutionNumber: v.resolutionNumber.trim(),
        issuedBy: v.issuedBy.trim(),
        issueDate: v.issueDate,
        returnDeadline: v.returnDeadline,
      }, file),
      'Permiso de exportación adjuntado.',
      () => this.showPermitDialog.set(false),
    );
  }

  // ----- Ampliación -----

  protected readonly canRequestExtension = computed(
    () => this.loan()?.status === 'ON_LOAN' && this.isFoundation() && !this.loan()?.requestedEndDate,
  );

  protected readonly canReviewExtension = computed(
    () => this.isCollector() && !!this.loan()?.requestedEndDate,
  );

  openExtension(): void {
    this.extensionForm.reset({ newEndDate: '', reason: '' });
    this.showExtensionDialog.set(true);
  }

  confirmExtension(): void {
    const id = this.loan()?.id;
    if (!id || this.extensionForm.invalid) return;
    const v = this.extensionForm.getRawValue();
    this.run(
      this.loanService.requestExtension(id, v.newEndDate, v.reason.trim() || undefined),
      'Ampliación solicitada. El coleccionista la revisará.',
      () => this.showExtensionDialog.set(false),
    );
  }

  approveExtension(): void {
    const id = this.loan()?.id;
    if (!id) return;
    this.run(this.loanService.approveExtension(id), 'Ampliación aprobada.');
  }

  rejectExtension(): void {
    const id = this.loan()?.id;
    if (!id) return;
    this.run(
      this.loanService.rejectExtension(id, this.extensionRejectReason().trim() || undefined),
      'Ampliación rechazada.',
      () => this.extensionRejectReason.set(''),
    );
  }

  // ----- Certificado de exposición -----

  protected readonly canDownloadCertificate = computed(() => {
    const l = this.loan();
    const exhibited: LoanStatus[] = ['DELIVERED', 'ON_LOAN', 'RETURNING', 'RETURNED'];
    return !!l?.exhibitionId && exhibited.includes(l.status)
      && (this.isCollector() || this.isFoundation() || this.isAdmin());
  });

  downloadCertificate(): void {
    const id = this.loan()?.id;
    if (!id) return;
    this.loanService.downloadExhibitionCertificate(id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `certificado_exposicion_PA-${id}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.errorMessage.set('No se pudo descargar el certificado.'),
    });
  }

  // ----- Valoraciones -----

  openReview(target: Reviewable): void {
    this.reviewForm.reset({ rating: 5, comment: '' });
    this.reviewTarget.set(target);
  }

  confirmReview(): void {
    const id = this.loan()?.id;
    const target = this.reviewTarget();
    if (!id || !target || this.reviewForm.invalid) return;
    const v = this.reviewForm.getRawValue();
    this.busy.set(true);
    this.loanService.createReview(id, target.userId, v.rating, v.comment.trim() || undefined).subscribe({
      next: (r) => {
        this.reviews.set(r);
        this.reviewTarget.set(null);
        this.busy.set(false);
        this.actionMessage.set('¡Gracias por tu valoración!');
      },
      error: (err) => {
        this.busy.set(false);
        this.errorMessage.set(err?.error?.message ?? 'No se pudo guardar la valoración.');
      },
    });
  }

  protected stars(n: number): string {
    return '★'.repeat(n) + '☆'.repeat(5 - n);
  }

  /** Supervisión que exige la obra. */
  protected readonly courierMode = computed(() => this.artwork()?.requirements?.courierMode ?? 'NONE');

  /** Documentan las fases quien manipula la obra: transportista o institución. */
  protected readonly canAddCheckpoint = computed(() => {
    const s = this.loan()?.status;
    const active: LoanStatus[] = ['PAID', 'READY_FOR_PICKUP', 'IN_TRANSIT', 'DELIVERED', 'ON_LOAN', 'RETURNING'];
    const isCarrier = this.role() === 'TRANSPORT' && this.shipment()?.transportCompanyId === this.userId();
    return !!s && active.includes(s) && (this.isFoundation() || isCarrier);
  });

  /**
   * Con correo virtual, la recogida exige el embalaje aprobado (de ida o de
   * vuelta según el estado del préstamo).
   */
  protected readonly packingApprovalMissing = computed(() => {
    if (this.courierMode() !== 'VIRTUAL') return false;
    const stage: SupervisionStage = this.loan()?.status === 'RETURNING' ? 'REPACKING' : 'PACKING';
    return !this.supervision().some((c) => c.stage === stage && c.status === 'APPROVED');
  });

  openCheckpoint(): void {
    const status = this.loan()?.status;
    const suggested: SupervisionStage =
      status === 'RETURNING' ? 'REPACKING'
        : status === 'ON_LOAN' || status === 'DELIVERED' ? 'INSTALLATION'
          : 'PACKING';
    this.checkpointForm.reset({ stage: suggested, note: '' });
    this.reportPhotos.set([]);
    this.showCheckpointDialog.set(true);
  }

  confirmCheckpoint(): void {
    const id = this.loan()?.id;
    const photos = this.reportPhotos();
    if (!id || this.checkpointForm.invalid || photos.length === 0) return;
    const v = this.checkpointForm.getRawValue();
    this.run(
      this.loanService.addCheckpoint(id, v.stage, v.note.trim(), photos),
      'Fase documentada. El coleccionista la revisará.',
      () => this.showCheckpointDialog.set(false),
    );
  }

  approveCheckpoint(c: SupervisionCheckpoint): void {
    const id = this.loan()?.id;
    if (!id) return;
    this.run(this.loanService.approveCheckpoint(id, c.id), `${this.stageLabel[c.stage]}: visto bueno dado.`);
  }

  openCheckpointChanges(c: SupervisionCheckpoint): void {
    this.checkpointChangesForm.reset({ reason: '' });
    this.showCheckpointChangesDialog.set(c);
  }

  confirmCheckpointChanges(): void {
    const id = this.loan()?.id;
    const c = this.showCheckpointChangesDialog();
    if (!id || !c || this.checkpointChangesForm.invalid) return;
    this.run(
      this.loanService.requestCheckpointChanges(id, c.id, this.checkpointChangesForm.controls.reason.value.trim()),
      'Cambios solicitados.',
      () => this.showCheckpointChangesDialog.set(null),
    );
  }

  onDocumentFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.documentFile.set(input.files?.[0] ?? null);
  }

  openReject(): void {
    this.rejectForm.reset({ reason: '' });
    this.showRejectDialog.set(true);
  }

  confirmReject(): void {
    const id = this.loan()?.id;
    if (!id || this.rejectForm.invalid) return;
    const reason = this.rejectForm.controls.reason.value.trim() || undefined;
    this.run(this.loanService.reject(id, { reason }), 'Solicitud rechazada.', () =>
      this.showRejectDialog.set(false),
    );
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
    this.quoteForm.reset({ price: null, returnPrice: null, insuranceCost: null, insurancePolicy: '' });
    this.showQuoteDialog.set(true);
  }

  confirmQuote(): void {
    const sId = this.shipment()?.id;
    if (!sId || this.quoteForm.invalid) return;
    const v = this.quoteForm.getRawValue();
    this.run(this.shipmentService.proposeQuote(sId, {
      price: v.price!,
      returnPrice: v.returnPrice!,
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

  openReassign(): void {
    const previous = this.shipment()?.transportCompanyId ?? null;
    // Preseleccionamos la primera empresa disponible que NO sea la que acaba
    // de rechazar el presupuesto, para evitar el error del back.
    const first = this.companies().find((c) => c.id !== previous)?.id ?? null;
    this.reassignForm.reset({ transportCompanyId: first });
    this.showReassignDialog.set(true);
  }

  confirmReassign(): void {
    const id = this.loan()?.id;
    const newCompanyId = this.reassignForm.controls.transportCompanyId.value;
    if (!id || !newCompanyId) return;
    this.run(
      this.loanService.reassignTransport(id, newCompanyId),
      'Nueva empresa de transporte asignada. Esperando presupuesto.',
      () => this.showReassignDialog.set(false),
    );
  }

  /** Lista de empresas válidas para reasignar: descarta la que acaba de rechazar. */
  protected readonly reassignableCompanies = computed(() => {
    const previous = this.shipment()?.transportCompanyId ?? null;
    return this.companies().filter((c) => c.id !== previous);
  });

  openPickup(): void {
    this.pickupForm.reset({
      condition: this.lastKnownCondition(),
      damageReported: false,
      notes: '',
    });
    this.reportPhotos.set([]);
    this.showPickupDialog.set(true);
  }

  confirmPickup(): void {
    const sId = this.activeShipment()?.id;
    const photos = this.reportPhotos();
    if (!sId || this.pickupForm.invalid || photos.length === 0) return;
    const v = this.pickupForm.getRawValue();
    this.run(
      this.shipmentService.markPickedUp(sId, {
        condition: v.condition!,
        damageReported: v.damageReported,
        notes: v.notes.trim() || undefined,
      }, photos),
      'Recogida confirmada con su informe de estado.',
      () => this.showPickupDialog.set(false),
    );
  }

  markInTransit(): void {
    const sId = this.activeShipment()?.id;
    if (!sId) return;
    this.run(this.shipmentService.markInTransit(sId), 'En tránsito.');
  }

  openDelivery(): void {
    this.deliveryForm.reset({
      receivedBy: '',
      condition: this.lastKnownCondition(),
      damageReported: false,
      notes: '',
    });
    this.reportPhotos.set([]);
    this.showDeliveryDialog.set(true);
  }

  confirmDelivery(): void {
    const sId = this.activeShipment()?.id;
    const photos = this.reportPhotos();
    if (!sId || this.deliveryForm.invalid || photos.length === 0) return;
    const isReturn = this.loan()?.status === 'RETURNING';
    const v = this.deliveryForm.getRawValue();
    this.run(this.shipmentService.confirmDelivery(sId, {
      receivedBy: v.receivedBy.trim(),
      condition: v.condition!,
      damageReported: v.damageReported,
      notes: v.notes.trim() || undefined,
    }, photos),
    isReturn ? 'Retorno completado. Préstamo cerrado.' : 'Entrega confirmada.',
    () => this.showDeliveryDialog.set(false));
  }

  /* ===== Fotos de los informes de estado ===== */

  onReportPhotosSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const picked = Array.from(input.files ?? []);
    input.value = '';
    this.reportPhotos.update((current) => [...current, ...picked].slice(0, MAX_REPORT_PHOTOS));
  }

  removeReportPhoto(index: number): void {
    this.reportPhotos.update((current) => current.filter((_, i) => i !== index));
  }

  /**
   * Estado con el que se precarga el informe: el del último informe
   * levantado o, si no hay ninguno, el declarado en la ficha de la obra.
   */
  private lastKnownCondition(): Condition | null {
    const reports = this.conditionReports();
    if (reports.length > 0) return reports[reports.length - 1].condition;
    return (this.artwork()?.condition as Condition | undefined) ?? null;
  }

  /* ===== Etiquetas para la vista ===== */

  conditionLabel(c: string): string {
    return CONDITION_LABEL[c as Condition] ?? c;
  }

  reportTitle(r: ConditionReportResponse): string {
    const leg = r.direction === 'RETURN' ? 'vuelta' : 'ida';
    return r.stage === 'PICKUP' ? `Recogida (${leg})` : `Entrega (${leg})`;
  }

  roleLabel(role?: string | null): string {
    return role ? (ROLE_LABEL[role] ?? role) : '';
  }

  loanStatusLabel(s?: LoanStatus | null): string {
    return s ? LOAN_STATUS_LABEL[s] : '';
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
