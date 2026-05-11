import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';

import { AuthService } from '../../../core/services/auth.service';
import { ArtworkService } from '../../../core/services/artwork.service';
import { TransportCompanyService } from '../../../core/services/transport-company.service';
import {
  Condition,
  CONDITION_OPTIONS,
  CreateArtworkRequest,
} from '../../../core/models/artwork.models';
import { TransportCompanyResponse } from '../../../core/models/transport-company.models';

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

  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly fieldErrors = signal<Record<string, string>>({});
  protected readonly success = signal(false);

  /** Empresas de transporte disponibles para el desplegable. */
  protected readonly companies = signal<TransportCompanyResponse[]>([]);

  /** Archivos seleccionados (preview en cliente antes de subir). */
  protected files: Array<{ file: File; previewUrl: string }> = [];

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
    list.forEach((f) => {
      const url = URL.createObjectURL(f);
      this.files.push({ file: f, previewUrl: url });
    });
    input.value = '';
  }

  removeFile(index: number): void {
    URL.revokeObjectURL(this.files[index].previewUrl);
    this.files.splice(index, 1);
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

    const filesToUpload = this.files.map((f) => f.file);

    this.artworkService
      .create(body)
      .pipe(switchMap((created) => this.artworkService.uploadFiles(created.id, filesToUpload)))
      .subscribe({
        next: () => {
          this.loading.set(false);
          this.success.set(true);
          setTimeout(() => this.router.navigate(['/app/collector']), 1500);
        },
        error: (err) => {
          this.loading.set(false);
          const apiErrors = err?.error?.fieldErrors;
          if (apiErrors && typeof apiErrors === 'object') {
            this.fieldErrors.set(apiErrors);
          }
          this.errorMessage.set(err?.error?.message ?? 'No se pudo crear la obra. Revisa los datos.');
        },
      });
  }

  fieldErr(name: string): string | null {
    return this.fieldErrors()[name] ?? null;
  }
}
