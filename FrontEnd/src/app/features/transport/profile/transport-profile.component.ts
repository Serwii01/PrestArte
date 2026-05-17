import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';
import { TransportCompanyService } from '../../../core/services/transport-company.service';
import { TransportCompanyProfile } from '../../../core/models/transport-company.models';

/**
 * Pantalla donde la empresa de transporte edita su propio perfil público.
 * Solo accesible al usuario con rol TRANSPORT (ya protegido por roleGuard).
 */
@Component({
  selector: 'app-transport-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './transport-profile.component.html',
  styleUrl: './transport-profile.component.scss',
})
export class TransportProfileComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly companyService = inject(TransportCompanyService);
  protected readonly auth = inject(AuthService);

  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly success = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    companyName: ['', [Validators.maxLength(200)]],
    contactEmail: ['', [Validators.maxLength(150)]],
    website: ['', [Validators.maxLength(200)]],
    description: [''],
    specialties: ['', [Validators.maxLength(500)]],
    locations: [''],
    coverageArea: ['', [Validators.maxLength(50)]],
  });

  ngOnInit(): void {
    const id = this.auth.userId();
    if (id == null) return;
    this.companyService.getById(id).subscribe({
      next: (p) => {
        this.form.patchValue({
          companyName: p.companyName ?? '',
          contactEmail: p.contactEmail ?? '',
          website: p.website ?? '',
          description: p.description ?? '',
          specialties: p.specialties ?? '',
          locations: p.locations ?? '',
          coverageArea: p.coverageArea ?? '',
        });
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('No se pudo cargar el perfil.');
        this.loading.set(false);
      },
    });
  }

  submit(): void {
    const id = this.auth.userId();
    if (!id || this.form.invalid) return;
    this.saving.set(true);
    this.success.set(false);
    this.errorMessage.set(null);

    this.companyService.updateProfile(id, this.form.getRawValue()).subscribe({
      next: () => {
        this.saving.set(false);
        this.success.set(true);
      },
      error: (err) => {
        this.saving.set(false);
        this.errorMessage.set(err?.error?.message ?? 'No se pudo guardar el perfil.');
      },
    });
  }
}
