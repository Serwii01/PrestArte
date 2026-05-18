import { CommonModule } from '@angular/common';
import { Component, computed, HostListener, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { ArtworkService } from '../../core/services/artwork.service';
import { AuthService } from '../../core/services/auth.service';
import {
  ArtworkResponse,
  CONDITION_LABEL,
  Condition,
  isArtworkImage,
} from '../../core/models/artwork.models';

/**
 * Catálogo público de obras. Accesible sin sesión: cualquier visitante puede
 * navegar por las obras subidas. Las acciones (solicitar préstamo, subir, etc.)
 * permanecen restringidas a usuarios autenticados.
 */
@Component({
  selector: 'app-public-catalog',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './public-catalog.component.html',
  styleUrl: './public-catalog.component.scss',
})
export class PublicCatalogComponent implements OnInit {
  protected readonly artworkService = inject(ArtworkService);
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly artworks = signal<ArtworkResponse[]>([]);
  protected readonly loading = signal(true);
  protected readonly year = new Date().getFullYear();

  /** Filtros (en cliente). */
  protected query = '';
  protected conditionFilter: Condition | '' = '';

  /** Toggle de navbar al hacer scroll (mismo patrón que la landing). */
  protected readonly scrolled = signal(false);

  @HostListener('window:scroll')
  onScroll(): void {
    this.scrolled.set(window.scrollY > 8);
  }

  protected readonly filtered = computed(() => {
    const q = this.query.trim().toLowerCase();
    const cond = this.conditionFilter;
    return this.artworks().filter((a) => {
      const matchQ =
        !q ||
        a.title.toLowerCase().includes(q) ||
        a.artist.toLowerCase().includes(q) ||
        (a.location ?? '').toLowerCase().includes(q);
      const matchCond = !cond || a.condition === cond;
      return matchQ && matchCond;
    });
  });

  protected readonly conditionOptions: Array<{ value: Condition; label: string }> = (
    Object.keys(CONDITION_LABEL) as Condition[]
  ).map((v) => ({ value: v, label: CONDITION_LABEL[v] }));

  ngOnInit(): void {
    this.artworkService.getAll().subscribe({
      next: (list) => {
        this.artworks.set(list);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  onFilterChange(): void {
    // Forzar recompute del filtered signal cuando cambia query/condition.
    this.artworks.set([...this.artworks()]);
  }

  conditionLabel(c: string): string {
    return CONDITION_LABEL[c as Condition] ?? c;
  }

  fileUrl(fileId: string): string {
    return this.artworkService.fileUrl(fileId);
  }

  /** URL de la primera imagen (excluyendo documentos adjuntos). */
  mainImageUrl(a: ArtworkResponse): string | null {
    const first = (a.files ?? []).find(isArtworkImage);
    return first ? this.artworkService.fileUrl(first.id) : null;
  }

  goToApp(): void {
    this.router.navigate(['/app']);
  }
}
