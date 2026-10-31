import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, finalize, map, shareReplay, tap } from 'rxjs';
import { CurrentUser, Role, TokenResponse } from './models';

const STORAGE_KEY = 'pameobeo.tokens';

/**
 * Keeps the access/refresh tokens and the current user.
 * Demo note: tokens are kept in localStorage for simplicity; a production app would prefer an
 * httpOnly cookie for the refresh token.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly tokens = signal<TokenResponse | null>(this.load());
  private refreshing$: Observable<string> | null = null;

  readonly user = computed<CurrentUser | null>(() => {
    const t = this.tokens();
    return t ? decode(t.accessToken) : null;
  });
  readonly isLoggedIn = computed(() => this.user() !== null);
  readonly isAdmin = computed(() => this.user()?.roles.includes('ADMIN') ?? false);

  accessToken(): string | null {
    return this.tokens()?.accessToken ?? null;
  }

  login(email: string, password: string): Observable<CurrentUser> {
    return this.http.post<TokenResponse>('/api/auth/login', { email, password }).pipe(
      tap((t) => this.store(t)),
      map(() => this.user()!),
    );
  }

  register(email: string, password: string, fullName: string): Observable<unknown> {
    return this.http.post('/api/auth/register', { email, password, fullName });
  }

  /** Refresh token rotation: one request at a time, concurrent 401s share it. */
  refresh(): Observable<string> {
    const refreshToken = this.tokens()?.refreshToken;
    if (!refreshToken) {
      return new Observable((s) => s.error(new Error('no refresh token')));
    }
    if (!this.refreshing$) {
      this.refreshing$ = this.http.post<TokenResponse>('/api/auth/refresh', { refreshToken }).pipe(
        tap((t) => this.store(t)),
        map((t) => t.accessToken),
        finalize(() => (this.refreshing$ = null)),
        shareReplay(1),
      );
    }
    return this.refreshing$;
  }

  logout(redirect = true): void {
    const refreshToken = this.tokens()?.refreshToken;
    this.store(null);
    if (refreshToken) {
      this.http.post('/api/auth/logout', { refreshToken }).subscribe({ error: () => undefined });
    }
    if (redirect) {
      this.router.navigateByUrl('/login');
    }
  }

  hasRole(role: Role): boolean {
    return this.user()?.roles.includes(role) ?? false;
  }

  private store(t: TokenResponse | null): void {
    this.tokens.set(t);
    try {
      if (t) localStorage.setItem(STORAGE_KEY, JSON.stringify(t));
      else localStorage.removeItem(STORAGE_KEY);
    } catch {
      /* storage unavailable (private mode): keep tokens in memory only */
    }
  }

  private load(): TokenResponse | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? (JSON.parse(raw) as TokenResponse) : null;
    } catch {
      return null;
    }
  }
}

/** Reads the claims of a JWT (no verification: the gateway and services verify the signature). */
function decode(token: string): CurrentUser | null {
  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const json = new TextDecoder().decode(Uint8Array.from(atob(base64), (c) => c.charCodeAt(0)));
    const claims = JSON.parse(json) as { sub: string; email: string; name: string; roles: Role[]; exp: number };
    return { id: claims.sub, email: claims.email, name: claims.name, roles: claims.roles ?? [], expiresAt: claims.exp * 1000 };
  } catch {
    return null;
  }
}

