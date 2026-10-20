import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, map, Observable, of, shareReplay } from 'rxjs';

import { environment } from '../../../environments/environment';

/**
 * Descarga archivos protegidos (documentos de verificación, adjuntos del chat,
 * documentos confidenciales) a través de HttpClient para que el interceptor
 * añada el JWT, y los expone como URL `blob:` utilizable en `<img src>` o
 * `<a href>`. Un `<img>` o un enlace normal no pueden enviar la cabecera
 * Authorization, por eso el backend respondería 403.
 *
 * Las URLs se cachean por id durante la vida de la aplicación.
 */
@Injectable({ providedIn: 'root' })
export class SecureFileService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/files`;
  private readonly cache = new Map<string, Observable<string | null>>();

  objectUrl(fileId: string): Observable<string | null> {
    let url$ = this.cache.get(fileId);
    if (!url$) {
      url$ = this.http.get(`${this.base}/${fileId}`, { responseType: 'blob' }).pipe(
        map((blob) => URL.createObjectURL(blob)),
        catchError(() => {
          this.cache.delete(fileId);
          return of(null);
        }),
        shareReplay(1),
      );
      this.cache.set(fileId, url$);
    }
    return url$;
  }
}
