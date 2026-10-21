import { Component, computed, effect, input, model, OnDestroy, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { DamageMarkInput } from '../../../core/models/shipment.models';

/**
 * Selector de fotos de un informe de estado con marcado de daños: al
 * pulsar sobre una foto se coloca una marca (posición en %) con su nota.
 */
@Component({
  selector: 'app-damage-photo-picker',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div>
      <p class="text-text-main text-sm font-medium pb-1">
        Fotografías de la obra
        <span class="text-text-secondary font-normal">(mín. 1, máx. {{ max() }})</span>
      </p>
      <p class="text-xs text-text-secondary pb-2">
        Pulsa sobre una foto para señalar un daño exactamente donde está.
      </p>
      <div class="grid grid-cols-2 gap-2 items-start">
        @for (url of previews(); track $index; let i = $index) {
          <div class="relative rounded-lg border border-border bg-gray-soft overflow-hidden">
            <div class="relative cursor-crosshair" (click)="addMark(i, $event)">
              <img [src]="url" alt="Foto del informe" class="block w-full h-auto select-none" draggable="false" />
              @for (m of marksOf(i); track m.index) {
                <span class="absolute -translate-x-1/2 -translate-y-1/2 size-5 rounded-full bg-red-600 border-2 border-white
                             text-white text-[10px] font-bold flex items-center justify-center shadow"
                      [style.left.%]="m.mark.x" [style.top.%]="m.mark.y">{{ m.index + 1 }}</span>
              }
            </div>
            <button type="button" (click)="removePhoto(i)" title="Quitar foto"
              class="absolute top-1 right-1 size-6 rounded-full bg-white/90 border border-border shadow
                     flex items-center justify-center text-text-secondary hover:text-red-600">
              <span class="material-symbols-outlined" style="font-size:14px;">close</span>
            </button>
          </div>
        }
        @if (photos().length < max()) {
          <label class="min-h-32 rounded-lg border-2 border-dashed border-border hover:border-primary
                        flex flex-col items-center justify-center cursor-pointer text-text-secondary hover:text-primary">
            <span class="material-symbols-outlined" style="font-size:22px;">add_a_photo</span>
            <span class="text-[10px] font-medium mt-1">Añadir</span>
            <input type="file" multiple accept="image/jpeg,image/png,image/webp" class="hidden"
                   (change)="onSelected($event)" />
          </label>
        }
      </div>
      @if (photos().length === 0) {
        <p class="text-xs text-amber-700 mt-2">Añade al menos una foto: sin fotos el informe no sirve como prueba.</p>
      }
      @if (marks().length > 0) {
        <ol class="mt-3 space-y-2">
          @for (m of marks(); track $index; let i = $index) {
            <li class="flex items-center gap-2">
              <span class="size-5 shrink-0 rounded-full bg-red-600 text-white text-[10px] font-bold flex items-center justify-center">{{ i + 1 }}</span>
              <input type="text" maxlength="500" placeholder="Describe el daño (arañazo, craquelado, golpe en el marco…)"
                     [ngModel]="m.note ?? ''" (ngModelChange)="setNote(i, $event)"
                     class="flex-1 rounded-lg border border-border bg-white h-9 px-3 text-sm
                            focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary" />
              <button type="button" (click)="removeMark(i)" title="Quitar marca"
                      class="text-text-secondary hover:text-red-600">
                <span class="material-symbols-outlined" style="font-size:18px;">delete</span>
              </button>
            </li>
          }
        </ol>
      }
    </div>
  `,
})
export class DamagePhotoPickerComponent implements OnDestroy {
  readonly photos = model.required<File[]>();
  readonly marks = model<DamageMarkInput[]>([]);
  readonly max = input(10);

  private readonly urls = signal<string[]>([]);
  protected readonly previews = this.urls.asReadonly();

  constructor() {
    // Regenera las vistas previas cuando cambia la lista de fotos.
    effect(() => {
      const files = this.photos();
      untracked(() => this.urls()).forEach((u) => URL.revokeObjectURL(u));
      this.urls.set(files.map((f) => URL.createObjectURL(f)));
    });
  }

  protected readonly indexed = computed(() => this.marks().map((mark, index) => ({ mark, index })));

  marksOf(photoIndex: number) {
    return this.indexed().filter((m) => m.mark.photoIndex === photoIndex);
  }

  onSelected(event: Event): void {
    const el = event.target as HTMLInputElement;
    const picked = Array.from(el.files ?? []);
    el.value = '';
    this.photos.update((current) => [...current, ...picked].slice(0, this.max()));
  }

  removePhoto(index: number): void {
    this.photos.update((current) => current.filter((_, i) => i !== index));
    // Las marcas de esa foto desaparecen y las posteriores se reindexan.
    this.marks.update((ms) => ms
      .filter((m) => m.photoIndex !== index)
      .map((m) => (m.photoIndex > index ? { ...m, photoIndex: m.photoIndex - 1 } : m)));
  }

  addMark(photoIndex: number, event: MouseEvent): void {
    const box = (event.currentTarget as HTMLElement).getBoundingClientRect();
    const x = Math.min(100, Math.max(0, ((event.clientX - box.left) / box.width) * 100));
    const y = Math.min(100, Math.max(0, ((event.clientY - box.top) / box.height) * 100));
    this.marks.update((ms) => [...ms, { photoIndex, x: round(x), y: round(y), note: '' }]);
  }

  setNote(index: number, note: string): void {
    this.marks.update((ms) => ms.map((m, i) => (i === index ? { ...m, note } : m)));
  }

  removeMark(index: number): void {
    this.marks.update((ms) => ms.filter((_, i) => i !== index));
  }

  ngOnDestroy(): void {
    this.urls().forEach((u) => URL.revokeObjectURL(u));
  }
}

function round(n: number): number {
  return Math.round(n * 10) / 10;
}
