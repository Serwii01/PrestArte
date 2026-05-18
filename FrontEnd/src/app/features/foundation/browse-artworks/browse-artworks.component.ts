import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { ArtworkService } from '../../../core/services/artwork.service';
import {
  ArtworkResponse,
  CONDITION_LABEL,
  Condition,
  isArtworkImage,
} from '../../../core/models/artwork.models';

@Component({
  selector: 'app-browse-artworks',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './browse-artworks.component.html',
  styleUrl: './browse-artworks.component.scss',
})
export class BrowseArtworksComponent implements OnInit {
  protected readonly artworkService = inject(ArtworkService);

  protected readonly artworks = signal<ArtworkResponse[]>([]);
  protected readonly loading = signal(true);

  /** Filtros (cliente). */
  protected query = '';
  protected conditionFilter: Condition | '' = '';

  /** Aplicación de filtros sobre el catálogo. */
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

  /** Hack para forzar recompute al cambiar query/condition (signals + ngModel). */
  onFilterChange(): void {
    // No-op: el computed `filtered` se reactiva si artworks() cambia.
    // Como query/conditionFilter son fields normales, forzamos recompute con un set artificial.
    this.artworks.set([...this.artworks()]);
  }

  conditionLabel(c: string): string {
    return CONDITION_LABEL[c as Condition] ?? c;
  }

  fileUrl(fileId: string): string {
    return this.artworkService.fileUrl(fileId);
  }

  /** URL de la primera imagen (excluye documentos). */
  mainImageUrl(a: ArtworkResponse): string | null {
    const first = (a.files ?? []).find(isArtworkImage);
    return first ? this.artworkService.fileUrl(first.id) : null;
  }
}
