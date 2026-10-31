import { DatePipe } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { errorText } from '../core/errors';
import { Order } from '../core/models';
import { CookingDayPipe, VndPipe } from '../shared/format';
import { StatusBadge } from '../shared/status-badge';

@Component({
  selector: 'app-orders',
  imports: [RouterLink, DatePipe, VndPipe, CookingDayPipe, StatusBadge],
  template: `
    <div class="page-header">
      <h1>Đơn của tôi</h1>
      <button class="btn ghost" (click)="load()">↻ Làm mới</button>
    </div>
    @if (error()) { <p class="error">{{ error() }}</p> }
    <div class="order-list">
      @for (o of orders(); track o.id) {
        <a class="card order-row" [routerLink]="['/orders', o.id]">
          <div>
            <strong>#{{ o.id.slice(0, 8).toUpperCase() }}</strong>
            <div class="muted small">Đặt lúc {{ o.createdAt | date: 'HH:mm dd/MM' }} · giao {{ o.cookingDate | cookingDay }}</div>
            <div class="small">{{ summary(o) }}</div>
          </div>
          <div class="right">
            <app-status-badge [status]="o.status" />
            <div><strong>{{ o.totalVnd | vnd }}</strong></div>
          </div>
        </a>
      } @empty {
        <p class="muted">Bạn chưa có đơn nào. <a routerLink="/">Đặt pate ngay</a></p>
      }
    </div>
  `,
})
export class OrdersPage {
  private readonly api = inject(ApiService);
  protected readonly orders = signal<Order[]>([]);
  protected readonly error = signal('');

  constructor() {
    this.load();
    // keep refreshing while some order is still being confirmed by the kitchen
    const poll = setInterval(() => { if (this.orders().some((o) => o.status === 'PENDING')) this.load(); }, 2000);
    inject(DestroyRef).onDestroy(() => clearInterval(poll));
  }

  protected load(): void {
    this.api.myOrders().subscribe({ next: (o) => this.orders.set(o), error: (e) => this.error.set(errorText(e)) });
  }

  protected summary(o: Order): string {
    return o.items.map((i) => `${i.name} × ${i.quantity}`).join(', ');
  }
}
