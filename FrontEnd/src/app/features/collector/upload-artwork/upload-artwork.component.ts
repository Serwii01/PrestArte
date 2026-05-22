import { CommonModule } from '@angular/common';
import { Component, computed, inject, Input, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { of, switchMap } from 'rxjs';

import { AuthService } from '../../../core/services/auth.service';
import { ArtworkService } from '../../../core/services/artwork.service';
import { TransportCompanyService } from '../../../core/services/transport-company.service';
import {
  Condition,
  CONDITION_OPTIONS,
  CreateArtworkRequest,
} from '../../../core/models/artwork.models';
import { TransportCompanyProfile } from '../../../core/models/transport-company.models';

@Component({
  selector: 'app-upload-artwork',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './upload-artwork.component.html',
  styleUrl: './upload-artwork.component.scss',
})
export class UploadArtworkComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly artworkService = inject(ArtworkService);
  private readonly transportCompanyService = inject(TransportCompanyService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly conditions = CONDITION_OPTIONS;

  /** Vinculado al param `:editId` por withComponentInputBinding(). Si llega, modo edición. */
  @Input() editId?: string;

  protected readonly isEditMode = computed(() => !!this.editId);

  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly fieldErrors = signal<Record<string, string>>({});
  protected readonly success = signal(false);

  /** Empresas de transporte disponibles para el desplegable. */
  protected readonly companies = signal<TransportCompanyProfile[]>([]);

  /** Estándar mínimo de resolución (por debajo, aviso amarillo). */
  protected readonly MIN_WIDTH = 1200;
  protected readonly MIN_HEIGHT = 900;
  /** Resolución recomendada (objetivo de calidad para contrato y zoom). */
  protected readonly RECOMMENDED_WIDTH = 2000;
  protected readonly RECOMMENDED_HEIGHT = 1500;
  /** Tamaño máximo aceptado por el back (multipart limit). */
  protected readonly MAX_FILE_MB = 10;

  /**
   * Archivos seleccionados (preview en cliente antes de subir).
   * Usamos signal para que las dimensiones leídas asíncronamente disparen
   * recompute del template (avisos low-res, etc.).
   */
  protected readonly files = signal<
    Array<{ file: File; previewUrl: string; width?: number; height?: number; oversized?: boolean }>
  >([]);

  protected readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    artist: ['', [Validators.required, Validators.maxLength(200)]],
    year: [null as number | null, [Validators.min(0)]],
    widthCm: [null as number | null, [Validators.min(0)]],
    heightCm: [null as number | null, [Validators.min(0)]],
    depthCm: [null as number | null, [Validators.min(0)]],
    condition: ['GOOD' as Condition, [Validators.required]],
    estimatedValue: [null as number | null, [Validators.required, Validators.min(1)]],
    description: [''],
    loanConditions: [''],
    location: ['', [Validators.maxLength(150)]],
    /** Si está marcado, se exige una empresa de transporte concreta. */
    requirePreferredTransport: [false],
    preferredTransportCompanyId: [null as number | null],
    preferredTransportMandatory: [false],
  });

  ngOnInit(): void {
    this.transportCompanyService.getAll().subscribe({
      next: (cs) => this.companies.set(cs),
    });

    // Modo edición: precargar valores de la obra.
    const editIdNum = Number(this.editId);
    if (editIdNum) {
      this.artworkService.getById(editIdNum).subscribe({
        next: (a) => {
          this.form.patchValue({
            title: a.title,
            artist: a.artist,
            year: a.year ?? null,
            widthCm: a.widthCm ?? null,
            heightCm: a.heightCm ?? null,
            depthCm: a.depthCm ?? null,
            condition: (a.condition as Condition) ?? 'GOOD',
            estimatedValue: a.estimatedValue ?? null,
            description: a.description ?? '',
            loanConditions: a.loanConditions ?? '',
            location: a.location ?? '',
            requirePreferredTransport: !!a.preferredTransportCompanyId,
            preferredTransportCompanyId: a.preferredTransportCompanyId ?? null,
            preferredTransportMandatory: a.preferredTransportMandatory ?? false,
          });
        },
      });
    }

    // Cuando el toggle "exigir empresa" cambia, ajustamos validators.
    this.form.controls.requirePreferredTransport.valueChanges.subscribe((on) => {
      const tc = this.form.controls.preferredTransportCompanyId;
      if (on) {
        tc.setValidators([Validators.required]);
      } else {
        tc.clearValidators();
        tc.setValue(null);
        this.form.controls.preferredTransportMandatory.setValue(false);
      }
      tc.updateValueAndValidity();
    });
  }

  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files) return;
    const list = Array.from(input.files).filter((f) => f.type.startsWith('image/'));
    const maxBytes = this.MAX_FILE_MB * 1024 * 1024;

    list.forEach((f) => {
      const url = URL.createObjectURL(f);
      const oversized = f.size > maxBytes;
      // Insertamos primero el placeholder; las dimensiones se rellenan asíncronamente.
      this.files.update((arr) => [...arr, { file: f, previewUrl: url, oversized }]);

      const img = new Image();
      img.onload = () => {
        const w = img.naturalWidth;
        const h = img.naturalHeight;
        this.files.update((arr) =>
          arr.map((e) => (e.previewUrl === url ? { ...e, width: w, height: h } : e)),
        );
      };
      img.src = url;
    });
    input.value = '';
  }

  removeFile(index: number): void {
    const target = this.files()[index];
    if (target) URL.revokeObjectURL(target.previewUrl);
    this.files.update((arr) => arr.filter((_, i) => i !== index));
  }

  /** ¿Esta imagen está por debajo del mínimo recomendado? */
  isLowRes(f: { width?: number; height?: number }): boolean {
    if (f.width == null || f.height == null) return false;
    return f.width < this.MIN_WIDTH || f.height < this.MIN_HEIGHT;
  }

  /** Tamaño humano: "2.4 MB", "780 KB". */
  humanSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  submit(): void {
    if (this.form.invalid) return;
    const collectorId = this.auth.userId();
    if (collectorId == null) return;

    this.loading.set(true);
    this.errorMessage.set(null);
    this.fieldErrors.set({});
    this.success.set(false);

    const v = this.form.getRawValue();
    const editIdNum = Number(this.editId);

    // Modo edición: PUT con solo los campos visibles del form.
    if (editIdNum) {
      this.artworkService
        .update(editIdNum, {
          title: v.title,
          artist: v.artist,
          year: v.year ?? undefined,
          widthCm: v.widthCm ?? undefined,
          heightCm: v.heightCm ?? undefined,
          depthCm: v.depthCm ?? undefined,
          condition: v.condition,
          estimatedValue: v.estimatedValue ?? undefined,
          description: v.description || undefined,
          loanConditions: v.loanConditions || undefined,
          location: v.location || undefined,
          preferredTransportCompanyId: v.requirePreferredTransport
            ? v.preferredTransportCompanyId
            : null,
          preferredTransportMandatory: v.requirePreferredTransport
            ? v.preferredTransportMandatory
            : false,
        })
        .subscribe({
          next: () => {
            this.loading.set(false);
            this.success.set(true);
            setTimeout(() => this.router.navigate(['/app/artworks', editIdNum]), 1200);
          },
          error: (err) => this.handleSubmitError(err, 'No se pudo guardar la obra.'),
        });
      return;
    }

    // Modo creación: POST + upload de archivos.
    const body: CreateArtworkRequest = {
      title: v.title,
      artist: v.artist,
      year: v.year ?? undefined,
      widthCm: v.widthCm ?? undefined,
      heightCm: v.heightCm ?? undefined,
      depthCm: v.depthCm ?? undefined,
      condition: v.condition,
      estimatedValue: v.estimatedValue!,
      description: v.description || undefined,
      loanConditions: v.loanConditions || undefined,
      location: v.location || undefined,
      preferredTransportCompanyId: v.requirePreferredTransport
        ? v.preferredTransportCompanyId
        : null,
      preferredTransportMandatory: v.requirePreferredTransport
        ? v.preferredTransportMandatory
        : false,
      collectorId,
    };

    const filesToUpload = this.files().map((f) => f.file);

    this.artworkService
      .create(body)
      .pipe(switchMap((created) => this.artworkService.uploadFiles(created.id, filesToUpload)))
      .subscribe({
        next: () => {
          this.loading.set(false);
          this.success.set(true);
          setTimeout(() => this.router.navigate(['/app/collector']), 1500);
        },
        error: (err) => this.handleSubmitError(err, 'No se pudo crear la obra. Revisa los datos.'),
      });
  }

  private handleSubmitError(err: any, fallback: string): void {
    this.loading.set(false);
    const apiErrors = err?.error?.fieldErrors;
    if (apiErrors && typeof apiErrors === 'object') {
      this.fieldErrors.set(apiErrors);
    }
    this.errorMessage.set(err?.error?.message ?? fallback);
  }

  fieldErr(name: string): string | null {
    return this.fieldErrors()[name] ?? null;
  }

  /** Selecciona/deselecciona la empresa preferida desde el grid de tarjetas. */
  selectCompany(id: number): void {
    const current = this.form.controls.preferredTransportCompanyId.value;
    this.form.controls.preferredTransportCompanyId.setValue(current === id ? null : id);
  }

  /** Convierte "lienzo, escultura" en ["lienzo", "escultura"] (máx. 3 chips). */
  splitSpecialties(text?: string): string[] {
    return (text ?? '')
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean)
      .slice(0, 3);
  }
}
