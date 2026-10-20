import { inject, Pipe, PipeTransform } from '@angular/core';
import { Observable, of } from 'rxjs';

import { SecureFileService } from '../../core/services/secure-file.service';

/**
 * Convierte el id de un archivo protegido en una URL `blob:` autenticada.
 * Uso: `[src]="fileId | secureFile | async"`.
 */
@Pipe({ name: 'secureFile' })
export class SecureFilePipe implements PipeTransform {
  private readonly files = inject(SecureFileService);

  transform(fileId: string | null | undefined): Observable<string | null> {
    return fileId ? this.files.objectUrl(fileId) : of(null);
  }
}
