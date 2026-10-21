import { CommonModule } from '@angular/common';
import { Component, computed, HostListener, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { ArtworkService } from '../../core/services/artwork.service';
import { AuthService } from '../../core/services/auth.service';
import {
  ArtworkPage,
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

  protected readonly page = signal<ArtworkPage | null>(null);
  protected readonly loading = signal(true);
  protected readonly year = new Date().getFullYear();

  /** Filtros: se aplican en el servidor (búsqueda paginada). */
  protected query = '';
  protected conditionFilter: Condition | '' = '';
  protected city = '';
  protected yearFrom: number | null = null;
  protected yearTo: number | null = null;
  protected verifiedOnly = false;
  protected lendableOnly = false;
  protected availableFrom = '';
  protected availableTo = '';
  protected sort: 'recent' | 'title' | 'artist' | 'year' = 'recent';
  protected readonly showAdvanced = signal(false);
  protected readonly pageIndex = signal(0);
  private readonly pageSize = 12;
  private debounce?: ReturnType<typeof setTimeout>;

  /** Toggle de navbar al hacer scroll (mismo patrón que la landing). */
  protected readonly scrolled = signal(false);

  @HostListener('window:scroll')
  onScroll(): void {
    this.scrolled.set(window.scrollY > 8);
  }

  /** Obras de la página actual. */
  protected readonly filtered = computed(() => this.page()?.items ?? []);
  protected readonly total = computed(() => this.page()?.total ?? 0);
  protected readonly totalPages = computed(() => this.page()?.totalPages ?? 0);

  protected readonly conditionOptions: Array<{ value: Condition; label: string }> = (
    Object.keys(CONDITION_LABEL) as Condition[]
  ).map((v) => ({ value: v, label: CONDITION_LABEL[v] }));

  ngOnInit(): void {
    this.load();
  }

  /** Cualquier cambio de filtro vuelve a la primera página (con un pequeño retardo al escribir). */
  onFilterChange(): void {
    clearTimeout(this.debounce);
    this.debounce = setTimeout(() => {
      this.pageIndex.set(0);
      this.load();
    }, 300);
  }

  clearFilters(): void {
    this.query = '';
    this.conditionFilter = '';
    this.city = '';
    this.yearFrom = null;
    this.yearTo = null;
    this.verifiedOnly = false;
    this.lendableOnly = false;
    this.availableFrom = '';
    this.availableTo = '';
    this.sort = 'recent';
    this.pageIndex.set(0);
    this.load();
  }

  goToPage(i: number): void {
    if (i < 0 || i >= this.totalPages()) return;
    this.pageIndex.set(i);
    this.load();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  private load(): void {
    this.loading.set(true);
    this.artworkService
      .search({
        q: this.query.trim() || undefined,
        condition: this.conditionFilter || undefined,
        city: this.city.trim() || undefined,
        yearFrom: this.yearFrom,
        yearTo: this.yearTo,
        verifiedOnly: this.verifiedOnly,
        lendableOnly: this.lendableOnly,
        availableFrom: this.availableFrom && this.availableTo ? this.availableFrom : undefined,
        availableTo: this.availableFrom && this.availableTo ? this.availableTo : undefined,
        sort: this.sort,
        page: this.pageIndex(),
        size: this.pageSize,
      })
      .subscribe({
        next: (p) => {
          this.page.set(p);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
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
