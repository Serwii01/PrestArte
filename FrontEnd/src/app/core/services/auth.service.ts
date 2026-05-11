import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AuthResponse, LoginRequest } from '../models/auth.models';
import { Role } from '../models/user.models';

const STORAGE_KEY = 'prestarte.auth';

interface StoredSession {
  token: string;
  userId: number;
  email: string;
  name: string;
  role: Role;
  expiresAt: number; // epoch ms
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly _session = signal<StoredSession | null>(this.loadFromStorage());

  /** Sesión actual (signal). null si no hay sesión válida. */
  readonly session = this._session.asReadonly();

  /** Conveniencias derivadas. */
  readonly isLoggedIn = computed(() => this._session() !== null);
  readonly role = computed(() => this._session()?.role ?? null);
  readonly userId = computed(() => this._session()?.userId ?? null);
  readonly displayName = computed(() => this._session()?.name ?? '');

  login(req: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${environment.apiBaseUrl}/auth/login`, req)
      .pipe(tap((res) => this.persist(res)));
  }

  forgotPassword(email: string): Observable<void> {
    return this.http.post<void>(`${environment.apiBaseUrl}/auth/forgot-password`, { email });
  }

  resetPassword(token: string, newPassword: string): Observable<void> {
    return this.http.post<void>(`${environment.apiBaseUrl}/auth/reset-password`, {
      token,
      newPassword,
    });
  }

  logout(): void {
    localStorage.removeItem(STORAGE_KEY);
    this._session.set(null);
    this.router.navigate(['/login']);
  }

  /** Devuelve el token vigente o null si expiró / no hay sesión. */
  getToken(): string | null {
    const s = this._session();
    if (!s) return null;
    if (Date.now() >= s.expiresAt) {
      this.logout();
      return null;
    }
    return s.token;
  }

  private persist(res: AuthResponse): void {
    const stored: StoredSession = {
      token: res.token,
      userId: res.userId,
      email: res.email,
      name: res.name,
      role: res.role,
      expiresAt: Date.now() + res.expiresInMs,
    };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(stored));
    this._session.set(stored);
  }

  private loadFromStorage(): StoredSession | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) return null;
      const parsed = JSON.parse(raw) as StoredSession;
      if (Date.now() >= parsed.expiresAt) {
        localStorage.removeItem(STORAGE_KEY);
        return null;
      }
      return parsed;
    } catch {
      return null;
    }
  }
}
