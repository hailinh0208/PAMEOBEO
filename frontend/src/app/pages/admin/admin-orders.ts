import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { errorText } from '../../core/errors';
import { Order, OrderStatus } from '../../core/models';
import { ToastService } from '../../core/toast.service';
import { CookingDayPipe, STATUS_LABEL, VndPipe } from '../../shared/format';
import { StatusBadge } from '../../shared/status-badge';

/** UC04: orders of a cooking day, cooking plan, cut-off, delivery. */
@Component({
  selector: 'app-admin-orders',
  imports: [FormsModule, DatePipe, VndPipe, CookingDayPipe, StatusBadge],
  template: `
    <div class="page-header">
      <h1>Đơn hàng theo ngày nấu</h1>
      <div class="row">
        <input type="date" [(ngModel)]="date" (change)="load()" />
        <button class="btn ghost" (click)="load()">↻</button>
      </div>
    </div>

    <div class="stats">
      @for (s of statuses; track s) {
        <div class="card stat"><div class="stat-number">{{ countBy()[s] ?? 0 }}</div><div class="muted small">{{ labels[s] }}</div></div>
      }
    </div>

    <div class="admin-grid">
      <section class="card">
        <h2>Kế hoạch nấu {{ date | cookingDay }}</h2>
        <p class="muted small">Đơn đã xác nhận / đang nấu</p>
        @for (line of cookingPlan(); track line.name) {
          <div class="cart-line"><span>{{ line.name }}</span><strong>{{ line.jars }} hộp</strong></div>
        } @empty { <p class="muted">Chưa có đơn.</p> }
        <div class="cart-total"><span>Tổng</span><strong>{{ totalJars() }} hộp</strong></div>
        <button class="btn primary block" (click)="startCooking()" [disabled]="!countBy()['CONFIRMED']">
          Chốt đơn &amp; bắt đầu nấu
        </button>
        <p class="muted small">Job tự động làm việc này lúc 18:00 hằng ngày.</p>
      </section>

      <section class="card">
        <h2>Danh sách đơn</h2>
        <table class="table">
          <thead><tr><th>Đơn</th><th>Món</th><th>Tổng</th><th>Trạng thái</th><th></th></tr></thead>
          <tbody>
            @for (o of visibleOrders(); track o.id) {
              <tr>
                <td><strong>#{{ o.id.slice(0, 8).toUpperCase() }}</strong><div class="muted small">{{ o.createdAt | date: 'HH:mm dd/MM' }}</div></td>
                <td class="small">{{ summary(o) }}<div class="muted">{{ o.deliveryAddress }}</div></td>
                <td>{{ o.totalVnd | vnd }}</td>
                <td><app-status-badge [status]="o.status" /></td>
                <td>
                  @if (o.status === 'COOKING') { <button class="btn small" (click)="advance(o, 'DELIVERING')">🛵 Giao</button> }
                  @if (o.status === 'DELIVERING') { <button class="btn small primary" (click)="advance(o, 'COMPLETED')">✓ Xong</button> }
                </td>
              </tr>
            } @empty {
              <tr><td colspan="5" class="muted">Không có đơn cho ngày này.</td></tr>
            }
          </tbody>
        </table>
        @if (orders().length > visibleOrders().length) {
          <p class="muted small">Hiển thị {{ visibleOrders().length }}/{{ orders().length }} đơn mới nhất.</p>
        }
      </section>
    </div>
    @if (error()) { <p class="error">{{ error() }}</p> }
  `,
})
export class AdminOrdersPage {
  private readonly api = inject(ApiService);
  private readonly toast = inject(ToastService);

  protected readonly statuses: OrderStatus[] = ['PENDING', 'CONFIRMED', 'COOKING', 'DELIVERING', 'COMPLETED', 'CANCELLED'];
  protected readonly labels = STATUS_LABEL;
  protected date = '';
  protected readonly orders = signal<Order[]>([]);
  protected readonly error = signal('');

  protected readonly visibleOrders = computed(() => this.orders().slice(0, 200));

  protected readonly countBy = computed(() =>
    this.orders().reduce<Record<string, number>>((acc, o) => ({ ...acc, [o.status]: (acc[o.status] ?? 0) + 1 }), {}));

  protected readonly cookingPlan = computed(() => {
    const jars = new Map<string, number>();
    for (const o of this.orders().filter((x) => x.status === 'CONFIRMED' || x.status === 'COOKING')) {
      for (const i of o.items) jars.set(i.name, (jars.get(i.name) ?? 0) + i.quantity);
    }
    return [...jars.entries()].map(([name, n]) => ({ name, jars: n }));
  });
  protected readonly totalJars = computed(() => this.cookingPlan().reduce((s, l) => s + l.jars, 0));

  constructor() {
    this.api.availability().subscribe((a) => {
      this.date = a.cookingDate;
      this.load();
    });
  }

  protected load(): void {
    this.api.allOrders(this.date || undefined).subscribe({
      next: (o) => this.orders.set(o),
      error: (e) => this.error.set(errorText(e)),
    });
  }

  protected startCooking(): void {
    this.api.startCooking(this.date).subscribe({
      next: (r) => { this.toast.success(`${r.ordersMovedToCooking} đơn chuyển sang Đang nấu`); this.load(); },
      error: (e) => this.toast.error(errorText(e)),
    });
  }

  protected advance(o: Order, status: OrderStatus): void {
    this.api.advance(o.id, status).subscribe({
      next: () => { this.toast.success(`Đơn #${o.id.slice(0, 8).toUpperCase()}: ${STATUS_LABEL[status]}`); this.load(); },
      error: (e) => this.toast.error(errorText(e)),
    });
  }

  protected summary(o: Order): string {
    return o.items.map((i) => `${i.name} × ${i.quantity}`).join(', ');
  }
}
