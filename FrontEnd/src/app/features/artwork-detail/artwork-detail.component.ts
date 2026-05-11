import { CommonModule, Location } from '@angular/common';
import { Component, computed, inject, Input, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ArtworkService } from '../../core/services/artwork.service';
import { AuthService } from '../../core/services/auth.service';
import { ArtworkResponse, CONDITION_LABEL, Condition } from '../../core/models/artwork.models';

@Component({
  selector: 'app-artwork-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './artwork-detail.component.html',
  styleUrl: './artwork-detail.component.scss',
})
export class ArtworkDetailComponent implements OnInit {
  private readonly artworkService = inject(ArtworkService);
  protected readonly auth = inject(AuthService);
  private readonly location = inject(Location);

  /** Vinculado al param `:id` por withComponentInputBinding(). */
  @Input() id?: string;

  protected readonly artwork = signal<ArtworkResponse | null>(null);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly selectedFileIndex = signal(0);

  /** ¿La obra pertenece al coleccionista que mira la pantalla? */
  protected readonly isMine = computed(() => {
    const a = this.artwork();
    if (!a || !a.collectorId) return false;
    return a.collectorId === this.auth.userId();
  });

  /** Para mostrar el botón "solicitar préstamo" solo a museos. */
  protected readonly canRequestLoan = computed(
    () => this.auth.role() === 'FOUNDATION' && !this.isMine(),
  );

  /** URL de la imagen actualmente mostrada como principal. */
  protected readonly mainImageUrl = computed(() => {
    const a = this.artwork();
    if (!a?.files || a.files.length === 0) return null;
    const file = a.files[this.selectedFileIndex()] ?? a.files[0];
    return this.artworkService.fileUrl(file.id);
  });

  protected readonly conditionLabel = computed(() => {
    const c = this.artwork()?.condition as Condition | undefined;
    return c ? CONDITION_LABEL[c] ?? c : '—';
  });

  protected readonly formattedValue = computed(() => {
    const v = this.artwork()?.estimatedValue;
    if (v == null) return '—';
    return new Intl.NumberFormat('es-ES', {
      style: 'currency',
      currency: 'EUR',
      maximumFractionDigits: 0,
    }).format(v);
  });

  /** Dimensiones formateadas como "120 × 80 × 4 cm" o "—". */
  protected readonly dimensionsLabel = computed(() => {
    const a = this.artwork();
    if (!a) return '—';
    const parts = [a.widthCm, a.heightCm, a.depthCm].filter(
      (n): n is number => typeof n === 'number',
    );
    if (parts.length === 0) return '—';
    return parts.map((n) => `${n}`).join(' × ') + ' cm';
  });

  ngOnInit(): void {
    const numericId = Number(this.id);
    if (!numericId) {
      this.errorMessage.set('Identificador de obra inválido.');
      this.loading.set(false);
      return;
    }

    this.artworkService.getById(numericId).subscribe({
      next: (a) => {
        this.artwork.set(a);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err?.error?.message ?? 'No se pudo cargar la obra.');
      },
    });
  }

  selectImage(index: number): void {
    this.selectedFileIndex.set(index);
  }

  /** Vuelve a la pantalla anterior preservando el historial. */
  goBack(): void {
    this.location.back();
  }

  fileUrl(fileId: string): string {
    return this.artworkService.fileUrl(fileId);
  }
}
