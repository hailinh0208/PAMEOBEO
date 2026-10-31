import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { errorText } from '../core/errors';

@Component({
  selector: 'app-login',
  imports: [FormsModule, RouterLink],
  template: `
    <section class="auth-card card">
      <img class="auth-logo" src="logo.jpg" alt="Pameobeo — since 2025" width="220" height="220" />
      <h1>Đăng nhập</h1>
      <p class="muted center">Pate tươi nấu theo đơn cho bé mèo, giao 18:00–22:00 hôm sau.</p>
      <form (ngSubmit)="submit()" #f="ngForm">
        <label>Email <input name="email" type="email" [(ngModel)]="email" required autocomplete="username" /></label>
        <label>Mật khẩu <input name="password" type="password" [(ngModel)]="password" required autocomplete="current-password" /></label>
        @if (error()) { <p class="error">{{ error() }}</p> }
        <button class="btn primary block" [disabled]="loading() || f.invalid">{{ loading() ? 'Đang đăng nhập…' : 'Đăng nhập' }}</button>
      </form>
      <p class="muted center">Chưa có tài khoản? <a routerLink="/register">Đăng ký</a></p>
      <details class="hint">
        <summary>Tài khoản demo</summary>
        <button type="button" class="btn ghost small" (click)="fill('admin@pameobeo.vn', 'Admin@123')">Admin: admin&#64;pameobeo.vn / Admin&#64;123</button>
      </details>
    </section>
  `,
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected email = '';
  protected password = '';
  protected readonly loading = signal(false);
  protected readonly error = signal('');

  protected fill(email: string, password: string): void {
    this.email = email;
    this.password = password;
  }

  protected submit(): void {
    this.loading.set(true);
    this.error.set('');
    this.auth.login(this.email, this.password).subscribe({
      next: (user) => {
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
        this.router.navigateByUrl(returnUrl ?? (user.roles.includes('ADMIN') ? '/admin/orders' : '/'));
      },
      error: (err) => {
        this.error.set(errorText(err));
        this.loading.set(false);
      },
    });
  }
}
