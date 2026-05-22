import { CommonModule, Location } from '@angular/common';
import {
  Component,
  computed,
  HostListener,
  inject,
  Input,
  OnInit,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { ArtworkService } from '../../core/services/artwork.service';
import { AuthService } from '../../core/services/auth.service';
import {
  ArtworkResponse,
  CONDITION_LABEL,
  Condition,
  FileDto,
  isArtworkDocument,
  isArtworkImage,
} from '../../core/models/artwork.models';

@Component({
  selector: 'app-artwork-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './artwork-detail.component.html',
  styleUrl: './artwork-detail.component.scss',
})
export class ArtworkDetailComponent implements OnInit {
  private readonly artworkService = inject(ArtworkService);
  protected readonly auth = inject(AuthService);
  private readonly location = inject(Location);
  private readonly router = inject(Router);

  /** Vinculado al param `:id` por withComponentInputBinding(). */
  @Input() id?: string;

  protected readonly artwork = signal<ArtworkResponse | null>(null);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly selectedFileIndex = signal(0);

  /** Año actual para el footer del modo público. */
  protected readonly year = new Date().getFullYear();

  /** Toggle de navbar al hacer scroll. */
  protected readonly scrolled = signal(false);

  /** ¿Estamos en el catálogo público (`/catalog/:id`)? */
  protected readonly isPublic = signal(this.router.url.startsWith('/catalog'));

  @HostListener('window:scroll')
  onScroll(): void {
    this.scrolled.set(window.scrollY > 8);
  }

  /** ¿La obra pertenece al coleccionista que mira la pantalla? */
  protected readonly isMine = computed(() => {
    const a = this.artwork();
    if (!a || !a.collectorId) return false;
    return a.collectorId === this.auth.userId();
  });

  /** ¿La obra está dada de baja por el coleccionista? Visible para todos. */
  protected readonly isDisabled = computed(() => this.artwork()?.availableForLoan === false);

  /**
   * Para mostrar el botón "solicitar préstamo" solo a museos y solo si la obra
   * está activa. Si el coleccionista la ha deshabilitado, no se puede pedir.
   */
  protected readonly canRequestLoan = computed(
    () => this.auth.role() === 'FOUNDATION' && !this.isMine() && !this.isDisabled(),
  );

  /** Imágenes (gallery): excluye documentos. */
  protected readonly images = computed<FileDto[]>(() => {
    return (this.artwork()?.files ?? []).filter(isArtworkImage);
  });

  /** Documentos adjuntos (seguro, certificado, informe...). */
  protected readonly documents = computed<FileDto[]>(() => {
    return (this.artwork()?.files ?? []).filter(isArtworkDocument);
  });

  /** URL de la imagen actualmente mostrada como principal. */
  protected readonly mainImageUrl = computed(() => {
    const imgs = this.images();
    if (imgs.length === 0) return null;
    const file = imgs[this.selectedFileIndex()] ?? imgs[0];
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

  protected readonly busy = signal(false);
  protected readonly actionMessage = signal<string | null>(null);

  /** Vuelve a la pantalla anterior preservando el historial. */
  goBack(): void {
    this.location.back();
  }

  goToApp(): void {
    this.router.navigate(['/app']);
  }

  fileUrl(fileId: string): string {
    return this.artworkService.fileUrl(fileId);
  }

  /** ¿El documento es una imagen renderizable inline? */
  isImageDoc(d: FileDto): boolean {
    return (d.fileType ?? '').startsWith('image/');
  }

  /** "1.2 MB", "340 KB"… */
  humanSize(bytes?: number): string {
    if (bytes == null || isNaN(bytes)) return '';
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  // ===== Subida de documentos =====

  protected pendingDoc: File | null = null;
  protected pendingDocDescription = '';
  protected pendingDocConfidential = false;
  protected readonly uploadingDoc = signal(false);
  protected readonly docError = signal<string | null>(null);
  protected readonly showUploader = signal(false);

  onDocSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.pendingDoc = input.files?.[0] ?? null;
    input.value = '';
  }

  clearPendingDoc(): void {
    this.pendingDoc = null;
    this.pendingDocDescription = '';
    this.pendingDocConfidential = false;
    this.docError.set(null);
  }

  toggleUploader(): void {
    this.showUploader.update((v) => !v);
    if (!this.showUploader()) this.clearPendingDoc();
  }

  uploadDocument(): void {
    const a = this.artwork();
    if (!a || !this.pendingDoc) {
      this.docError.set('Selecciona un archivo antes de subirlo.');
      return;
    }
    this.uploadingDoc.set(true);
    this.docError.set(null);
    this.artworkService
      .addDocument(a.id, this.pendingDocDescription, this.pendingDocConfidential, this.pendingDoc)
      .subscribe({
        next: (updated) => {
          this.uploadingDoc.set(false);
          this.artwork.set(updated);
          this.clearPendingDoc();
          this.showUploader.set(false);
          this.actionMessage.set('Documento subido correctamente.');
        },
        error: (err) => {
          this.uploadingDoc.set(false);
          this.docError.set(err?.error?.message ?? 'No se pudo subir el documento.');
        },
      });
  }

  deleteDocument(d: FileDto): void {
    const a = this.artwork();
    if (!a || d.artworkFileId == null) return;
    const label = d.description || d.fileName;
    if (!confirm(`¿Eliminar "${label}"? Esta acción no se puede deshacer.`)) return;
    this.busy.set(true);
    this.errorMessage.set(null);
    this.artworkService.deleteDocument(a.id, d.artworkFileId).subscribe({
      next: (updated) => {
        this.busy.set(false);
        this.artwork.set(updated);
        this.actionMessage.set('Documento eliminado.');
      },
      error: (err) => {
        this.busy.set(false);
        this.errorMessage.set(err?.error?.message ?? 'No se pudo eliminar el documento.');
      },
    });
  }

  // ======================================================================

  toggleAvailability(): void {
    const a = this.artwork();
    if (!a) return;
    const next = !(a.availableForLoan ?? true);
    this.busy.set(true);
    this.errorMessage.set(null);
    this.artworkService.setAvailability(a.id, next).subscribe({
      next: (updated) => {
        this.busy.set(false);
        this.artwork.set(updated);
        this.actionMessage.set(next
          ? 'Obra publicada para préstamo.'
          : 'Obra dada de baja. Sigue visible en el catálogo pero no se podrán pedir nuevos préstamos.');
      },
      error: (err) => {
        this.busy.set(false);
        this.errorMessage.set(err?.error?.message ?? 'No se pudo cambiar la disponibilidad.');
      },
    });
  }

  deleteArtwork(): void {
    const a = this.artwork();
    if (!a) return;
    if (!confirm(`¿Eliminar "${a.title}"? Esta acción no se puede deshacer.`)) return;
    this.busy.set(true);
    this.errorMessage.set(null);
    this.artworkService.delete(a.id).subscribe({
      next: () => {
        this.busy.set(false);
        this.location.back();
      },
      error: (err) => {
        this.busy.set(false);
        this.errorMessage.set(err?.error?.message ?? 'No se pudo eliminar la obra.');
      },
    });
  }
}
