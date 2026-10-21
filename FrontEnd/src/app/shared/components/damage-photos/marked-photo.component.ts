import { AsyncPipe } from '@angular/common';
import { Component, input } from '@angular/core';

import { DamageMark } from '../../../core/models/shipment.models';
import { SecureFilePipe } from '../../pipes/secure-file.pipe';

/** Foto protegida de un informe de estado con sus marcas de daño encima. */
@Component({
  selector: 'app-marked-photo',
  standalone: true,
  imports: [AsyncPipe, SecureFilePipe],
  template: `
    <a [href]="photoId() | secureFile | async" target="_blank" rel="noopener"
       class="relative block rounded-lg overflow-hidden border border-border bg-gray-soft">
      <img [src]="photoId() | secureFile | async" alt="Foto del informe de estado" class="block w-full h-auto" />
      @for (m of marks(); track $index) {
        <span class="absolute -translate-x-1/2 -translate-y-1/2 size-5 rounded-full bg-red-600 border-2 border-white
                     text-white text-[10px] font-bold flex items-center justify-center shadow"
              [style.left.%]="m.x" [style.top.%]="m.y" [title]="m.note ?? ''">{{ m.n }}</span>
      }
    </a>
  `,
})
export class MarkedPhotoComponent {
  readonly photoId = input.required<string>();
  /** Marcas de esta foto, con su número dentro del informe. */
  readonly marks = input<(DamageMark & { n: number })[]>([]);
}
