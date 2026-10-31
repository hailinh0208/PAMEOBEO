import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { errorText } from '../core/errors';

@Component({
  selector: 'app-register',
  imports: [FormsModule, RouterLink],
  template: `
    <section class="auth-card card">
      <img class="auth-logo small-logo" src="logo.jpg" alt="Pameobeo" width="140" height="140" />
      <h1>Tạo tài khoản</h1>
      <form (ngSubmit)="submit()" #f="ngForm">
        <label>Họ tên <input name="fullName" [(ngModel)]="fullName" required /></label>
        <label>Email <input name="email" type="email" [(ngModel)]="email" required email autocomplete="username" /></label>
        <label>Mật khẩu (tối thiểu 8 ký tự)
          <input name="password" type="password" [(ngModel)]="password" required minlength="8" autocomplete="new-password" />
        </label>
        @if (error()) { <p class="error">{{ error() }}</p> }
        <button class="btn primary block" [disabled]="loading() || f.invalid">{{ loading() ? 'Đang tạo…' : 'Đăng ký' }}</button>
      </form>
      <p class="muted center">Đã có tài khoản? <a routerLink="/login">Đăng nhập</a></p>
    </section>
  `,
})
export class RegisterPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected fullName = '';
  protected email = '';
  protected password = '';
  protected readonly loading = signal(false);
  protected readonly error = signal('');

  protected submit(): void {
    this.loading.set(true);
    this.error.set('');
    this.auth.register(this.email, this.password, this.fullName)
      .pipe(switchMap(() => this.auth.login(this.email, this.password)))
      .subscribe({
        next: () => this.router.navigateByUrl('/'),
        error: (err) => {
          this.error.set(errorText(err));
          this.loading.set(false);
        },
      });
  }
}
