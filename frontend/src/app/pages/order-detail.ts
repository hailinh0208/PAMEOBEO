import { DatePipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { errorText } from '../core/errors';
import { Order } from '../core/models';
import { ToastService } from '../core/toast.service';
import { CookingDayPipe, VndPipe } from '../shared/format';
import { OrderTimeline } from '../shared/order-timeline';
import { StatusBadge } from '../shared/status-badge';

/**
 * The API answers 202 + PENDING: the kitchen confirms asynchronously (Saga).
 * This page polls every second until the order leaves PENDING.
 */
@Component({
  selector: 'app-order-detail',
  imports: [RouterLink, DatePipe, VndPipe, CookingDayPipe, OrderTimeline, StatusBadge],
  template: `
    <a routerLink="/orders" class="muted">← Đơn của tôi</a>
    @if (order(); as o) {
      <section class="card order-detail">
        <div class="page-header">
          <h1>Đơn #{{ o.id.slice(0, 8).toUpperCase() }}</h1>
          <app-status-badge [status]="o.status" />
        </div>
        <app-order-timeline [order]="o" />

        @if (o.status === 'PENDING') {
          <p class="info pulse">⏳ Bếp đang giữ suất cho đơn của bạn… (xử lý bất đồng bộ qua Kafka)</p>
        }
        @if (o.status === 'CONFIRMED') {
          <p class="success">✅ Đơn đã được xác nhận. Pate sẽ được giao {{ o.cookingDate | cookingDay }}, 18:00–22:00.</p>
        }

        <div class="detail-grid">
          <div>
            <h3>Món</h3>
            @for (i of o.items; track i.menuItemId) {
              <div class="cart-line"><span>{{ i.name }} × {{ i.quantity }}</span><span>{{ i.unitPriceVnd * i.quantity | vnd }}</span></div>
            }
            <div class="cart-total"><span>Tổng</span><strong>{{ o.totalVnd | vnd }}</strong></div>
          </div>
          <div>
            <h3>Giao hàng</h3>
            <p>{{ o.deliveryAddress }}</p>
            <p class="muted">{{ o.cookingDate | cookingDay }} · 18:00–22:00</p>
            <p class="muted small">Đặt lúc {{ o.createdAt | date: 'HH:mm:ss dd/MM/yyyy' }}</p>
            @if (canCancel()) {
              <p class="muted small">Có thể hủy trước {{ o.cancellableUntil | date: 'HH:mm dd/MM' }}</p>
              <button class="btn danger" (click)="cancel()" [disabled]="busy()">Hủy đơn</button>
            }
          </div>
        </div>
        @if (error()) { <p class="error">{{ error() }}</p> }
      </section>
    } @else if (error()) {
      <p class="error">{{ error() }}</p>
    }
  `,
})
export class OrderDetailPage {
  readonly id = input.required<string>();

  private readonly api = inject(ApiService);
  private readonly toast = inject(ToastService);
  protected readonly order = signal<Order | null>(null);
  protected readonly error = signal('');
  protected readonly busy = signal(false);

  protected readonly canCancel = computed(() => {
    const o = this.order();
    return !!o && (o.status === 'PENDING' || o.status === 'CONFIRMED') && Date.now() < new Date(o.cancellableUntil).getTime();
  });

  constructor() {
    const poll = setInterval(() => {
      const o = this.order();
      if (!o || o.status === 'PENDING') this.load();
    }, 1000);
    inject(DestroyRef).onDestroy(() => clearInterval(poll));
  }

  ngOnInit(): void {
    this.load();
  }

  protected cancel(): void {
    if (!confirm('Bạn chắc chắn muốn hủy đơn này?')) return;
    this.busy.set(true);
    this.api.cancel(this.id()).subscribe({
      next: (o) => {
        this.order.set(o);
        this.busy.set(false);
        this.toast.success('Đã hủy đơn. Suất đã được trả lại cho bếp.');
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(errorText(e));
      },
    });
  }

  private load(): void {
    this.api.order(this.id()).subscribe({
      next: (o) => {
        const previous = this.order()?.status;
        this.order.set(o);
        if (previous === 'PENDING' && o.status === 'CONFIRMED') this.toast.success('Bếp đã xác nhận đơn của bạn!');
        if (previous === 'PENDING' && o.status === 'CANCELLED') this.toast.error('Rất tiếc, ngày nấu đã đủ suất. Đơn đã bị hủy.');
      },
      error: (e) => this.error.set(errorText(e)),
    });
  }
}
