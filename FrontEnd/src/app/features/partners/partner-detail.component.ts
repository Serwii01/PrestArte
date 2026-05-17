import { CommonModule } from '@angular/common';
import { Component, HostListener, inject, Input, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';
import { TransportCompanyService } from '../../core/services/transport-company.service';
import { TransportCompanyProfile } from '../../core/models/transport-company.models';

@Component({
  selector: 'app-partner-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './partner-detail.component.html',
  styleUrl: './partner-detail.component.scss',
})
export class PartnerDetailComponent implements OnInit {
  private readonly companyService = inject(TransportCompanyService);
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  /** Vinculado al param `:id` por withComponentInputBinding(). */
  @Input() id?: string;

  protected readonly partner = signal<TransportCompanyProfile | null>(null);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly year = new Date().getFullYear();
  protected readonly scrolled = signal(false);

  @HostListener('window:scroll')
  onScroll(): void {
    this.scrolled.set(window.scrollY > 8);
  }

  ngOnInit(): void {
    const numericId = Number(this.id);
    if (!numericId) {
      this.errorMessage.set('Identificador no válido.');
      this.loading.set(false);
      return;
    }
    this.companyService.getById(numericId).subscribe({
      next: (p) => {
        this.partner.set(p);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('No se pudo cargar el partner.');
        this.loading.set(false);
      },
    });
  }

  goToApp(): void {
    this.router.navigate(['/app']);
  }

  splitLocations(locations?: string): string[] {
    return (locations ?? '').split(/\r?\n/).map((s) => s.trim()).filter(Boolean);
  }

  splitSpecialties(specialties?: string): string[] {
    return (specialties ?? '').split(',').map((s) => s.trim()).filter(Boolean);
  }
}
