import { CommonModule } from '@angular/common';
import { Component, HostListener, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';
import { TransportCompanyService } from '../../core/services/transport-company.service';
import { TransportCompanyProfile } from '../../core/models/transport-company.models';

/**
 * Página pública con todas las empresas de transporte aprobadas (partners).
 */
@Component({
  selector: 'app-public-partners',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './public-partners.component.html',
  styleUrl: './public-partners.component.scss',
})
export class PublicPartnersComponent implements OnInit {
  private readonly companyService = inject(TransportCompanyService);
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly partners = signal<TransportCompanyProfile[]>([]);
  protected readonly loading = signal(true);
  protected readonly year = new Date().getFullYear();
  protected readonly scrolled = signal(false);

  @HostListener('window:scroll')
  onScroll(): void {
    this.scrolled.set(window.scrollY > 8);
  }

  ngOnInit(): void {
    this.companyService.getAll().subscribe({
      next: (list) => {
        this.partners.set(list);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  goToApp(): void {
    this.router.navigate(['/app']);
  }

  /** Convierte "Madrid (HQ)\nBarcelona\nLisboa" en array para renderizar. */
  splitLocations(locations?: string): string[] {
    return (locations ?? '').split(/\r?\n/).map((s) => s.trim()).filter(Boolean);
  }

  /** "Pintura, Escultura, Gran formato" → array de chips. */
  splitSpecialties(specialties?: string): string[] {
    return (specialties ?? '').split(',').map((s) => s.trim()).filter(Boolean);
  }
}
