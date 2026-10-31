import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';

const PUBLIC = ['/api/auth/login', '/api/auth/register', '/api/auth/refresh', '/api/auth/logout'];

function withToken(req: HttpRequest<unknown>, token: string | null): HttpRequest<unknown> {
  return token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;
}

/**
 * Adds "Authorization: Bearer <access token>" to API calls. When the access token has expired
 * (401), it refreshes once using the refresh token and retries; if that fails the user is logged out.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (PUBLIC.some((p) => req.url.startsWith(p))) {
    return next(req);
  }
  const auth = inject(AuthService);
  return next(withToken(req, auth.accessToken())).pipe(
    catchError((err: unknown) => {
      if (!(err instanceof HttpErrorResponse) || err.status !== 401 || !auth.accessToken()) {
        return throwError(() => err);
      }
      return auth.refresh().pipe(
        switchMap((token) => next(withToken(req, token))),
        catchError((refreshErr: unknown) => {
          auth.logout();
          return throwError(() => refreshErr);
        }),
      );
    }),
  );
};
