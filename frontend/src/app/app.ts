import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';
import { ToastService } from './core/toast.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <header class="topbar">
      <a class="brand" routerLink="/">
        <img src="logo-128.jpg" alt="" width="44" height="44" />
        <span>Pameobeo</span>
      </a>
      <nav>
        <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">Đặt pate</a>
        @if (auth.isLoggedIn()) {
          <a routerLink="/orders" routerLinkActive="active">Đơn của tôi</a>
        }
        @if (auth.isAdmin()) {
          <span class="divider"></span>
          <a routerLink="/admin/orders" routerLinkActive="active">Quản lý đơn</a>
          <a routerLink="/admin/kitchen" routerLinkActive="active">Bếp</a>
          <a routerLink="/admin/system" routerLinkActive="active">Hệ thống</a>
        }
      </nav>
      <div class="user">
        @if (auth.user(); as u) {
          <span class="muted">{{ u.name }} @if (auth.isAdmin()) { <span class="tag">ADMIN</span> }</span>
          <button class="btn ghost small" (click)="auth.logout()">Đăng xuất</button>
        } @else {
          <a class="btn small" routerLink="/login">Đăng nhập</a>
        }
      </div>
    </header>

    <main class="container">
      <router-outlet />
    </main>

    <footer class="footer muted small">Pameobeo · since 2025 · Chốt đơn 18:00 · Giao 18:00–22:00 hôm sau · Tối đa 50 hộp/ngày</footer>

    <div class="toasts">
      @for (t of toast.toasts(); track t.id) {
        <div class="toast" [class]="'toast toast-' + t.kind" (click)="toast.dismiss(t.id)">{{ t.text }}</div>
      }
    </div>
  `,
})
export class App {
  protected readonly auth = inject(AuthService);
  protected readonly toast = inject(ToastService);
}
