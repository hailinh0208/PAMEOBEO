import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { apiError, errorText } from '../../core/errors';
import { ChaosSettings, CircuitBreakerState, NotificationEntry } from '../../core/models';
import { ToastService } from '../../core/toast.service';
import { newIdempotencyKey } from '../../shared/format';

interface Transition { at: Date; from: string; to: string; }
interface LoadResult { code: string; count: number; avgMs: number; maxMs: number; }

/**
 * Live demo board for the Circuit Breaker: chaos switch for the kitchen, breaker state polled every
 * second, a small in-browser load generator, and the last emails (sent on virtual threads).
 */
@Component({
  selector: 'app-admin-system',
  imports: [FormsModule, DatePipe, DecimalPipe],
  template: `
    <h1>Hệ thống &amp; Circuit Breaker</h1>
    <div class="admin-grid">
      <section class="card">
        <h2>Circuit breaker <code>kitchen</code></h2>
        <p class="muted small">order-service → kitchen-service (POST /api/kitchen/quotes)</p>
        @if (cb(); as s) {
          <div class="cb-state" [class]="'cb-state cb-' + s.state.toLowerCase()">{{ s.state }}</div>
          <div class="cb-metrics">
            <div><span class="muted">Tỉ lệ lỗi</span><strong>{{ rateText(s.failureRate) }}</strong></div>
            <div><span class="muted">Tỉ lệ chậm</span><strong>{{ rateText(s.slowCallRate) }}</strong></div>
            <div><span class="muted">Lời gọi gần đây</span><strong>{{ s.bufferedCalls }}</strong></div>
            <div><span class="muted">Bị chặn (OPEN)</span><strong>{{ s.notPermittedCalls }}</strong></div>
          </div>
          @if (isUnknownRate(s.failureRate)) {
            <p class="muted small">Chưa đủ 10 lời gọi gần đây nên breaker chưa tính tỉ lệ.</p>
          }
        } @else { <p class="muted">Đang tải…</p> }
        <h3>Lịch sử chuyển trạng thái</h3>
        <ul class="transitions">
          @for (t of transitions(); track t.at) {
            <li><span class="muted">{{ t.at | date: 'HH:mm:ss' }}</span> {{ t.from }} → <strong>{{ t.to }}</strong></li>
          } @empty { <li class="muted">Chưa có thay đổi.</li> }
        </ul>
      </section>

      <section class="card">
        <h2>Chaos: làm kitchen chậm / lỗi</h2>
        <div class="row">
          <label>Độ trễ (ms) <input type="number" min="0" step="500" [(ngModel)]="chaos.delayMs" class="narrow" /></label>
          <label>Lỗi (%) <input type="number" min="0" max="100" [(ngModel)]="chaos.failurePercent" class="narrow" /></label>
          <button class="btn primary" (click)="applyChaos()">Áp dụng</button>
        </div>
        <div class="row">
          <button class="btn ghost small" (click)="preset(0, 0)">✅ Tắt chaos</button>
          <button class="btn ghost small" (click)="preset(3000, 0)">🐢 Chậm 3s</button>
          <button class="btn ghost small" (click)="preset(0, 60)">💥 Lỗi 60%</button>
        </div>
        <p class="muted small">Đang áp dụng: trễ {{ currentChaos().delayMs }} ms, lỗi {{ currentChaos().failurePercent }}%.
          order-service timeout sau 2s; breaker mở khi ≥50% lỗi hoặc chậm (&gt;1s) trong 20 lời gọi gần nhất.</p>

        <h2>Bắn tải từ trình duyệt</h2>
        <div class="row">
          <label>Số đơn <input type="number" min="1" max="500" [(ngModel)]="loadCount" class="narrow" /></label>
          <label>Song song <input type="number" min="1" max="50" [(ngModel)]="loadParallel" class="narrow" /></label>
          <button class="btn primary" (click)="fireLoad()" [disabled]="loading()">{{ loading() ? 'Đang gửi… ' + progress() : 'Gửi đơn' }}</button>
        </div>
        <p class="muted small">Mỗi đơn 1 hộp, đặt bằng tài khoản admin. Nên nâng công suất ở trang Bếp trước. Demo đầy đủ: JMeter.</p>
        @if (loadResults().length) {
          <table class="table">
            <thead><tr><th>Kết quả</th><th>Số request</th><th>TB (ms)</th><th>Max (ms)</th></tr></thead>
            <tbody>
              @for (r of loadResults(); track r.code) {
                <tr><td>{{ r.code }}</td><td>{{ r.count }}</td><td>{{ r.avgMs | number: '1.0-0' }}</td><td>{{ r.maxMs | number: '1.0-0' }}</td></tr>
              }
            </tbody>
          </table>
        }
      </section>

      <section class="card wide">
        <h2>Email gần đây (notification-service, Java 21 virtual threads)</h2>
        <table class="table">
          <thead><tr><th>Lúc</th><th>Gửi cho</th><th>Tiêu đề</th><th>Thread</th></tr></thead>
          <tbody>
            @for (n of notifications(); track $index) {
              <tr>
                <td>{{ n.sentAt | date: 'HH:mm:ss' }}</td>
                <td>{{ n.recipient === 'SHOP' ? '🏪' : '👤' }} {{ n.toAddress }}</td>
                <td>{{ n.subject }}</td>
                <td><code class="small">{{ n.thread }}</code></td>
              </tr>
            } @empty { <tr><td colspan="4" class="muted">Chưa có email.</td></tr> }
          </tbody>
        </table>
        <p class="muted small">Xem nội dung email tại Mailpit: <a href="http://localhost:8025" target="_blank" rel="noopener">localhost:8025</a></p>
      </section>
    </div>
  `,
})
export class AdminSystemPage {
  private readonly api = inject(ApiService);
  private readonly toast = inject(ToastService);

  protected readonly cb = signal<CircuitBreakerState | null>(null);
  protected readonly transitions = signal<Transition[]>([]);
  protected readonly notifications = signal<NotificationEntry[]>([]);
  protected readonly currentChaos = signal<ChaosSettings>({ delayMs: 0, failurePercent: 0 });
  protected chaos: ChaosSettings = { delayMs: 0, failurePercent: 0 };

  protected loadCount = 100;
  protected loadParallel = 20;
  protected readonly loading = signal(false);
  private readonly done = signal(0);
  protected readonly progress = computed(() => `${this.done()}/${this.loadCount}`);
  protected readonly loadResults = signal<LoadResult[]>([]);

  /** Resilience4j reports -1 while fewer calls than minimum-number-of-calls (10) have been recorded. */
  protected isUnknownRate(rate: string): boolean {
    return parseFloat(rate) < 0;
  }

  protected rateText(rate: string): string {
    return this.isUnknownRate(rate) ? '—' : rate;
  }

  constructor() {
    this.api.chaos().subscribe((c) => { this.currentChaos.set(c); this.chaos = { ...c }; });
    this.pollBreaker();
    this.loadNotifications();
    const cbTimer = setInterval(() => this.pollBreaker(), 1000);
    const notifTimer = setInterval(() => this.loadNotifications(), 5000);
    inject(DestroyRef).onDestroy(() => { clearInterval(cbTimer); clearInterval(notifTimer); });
  }

  protected preset(delayMs: number, failurePercent: number): void {
    this.chaos = { delayMs, failurePercent };
    this.applyChaos();
  }

  protected applyChaos(): void {
    this.api.setChaos(this.chaos).subscribe({
      next: (c) => { this.currentChaos.set(c); this.toast.info(`Chaos: trễ ${c.delayMs} ms, lỗi ${c.failurePercent}%`); },
      error: (e) => this.toast.error(errorText(e)),
    });
  }

  /** Fires `loadCount` orders with at most `loadParallel` in flight and groups results by outcome. */
  protected async fireLoad(): Promise<void> {
    this.loading.set(true);
    this.done.set(0);
    const samples: { code: string; ms: number }[] = [];
    let next = 0;
    const worker = async () => {
      while (next < this.loadCount) {
        next++;
        const start = performance.now();
        let code = '202 Accepted';
        try {
          await firstValueFrom(this.api.placeOrder({
            items: [{ menuItemId: '11111111-1111-1111-1111-111111111111', quantity: 1 }],
            deliveryAddress: 'Load test from browser', confirmNextCookingDay: true,
          }, newIdempotencyKey()));
        } catch (err) {
          const e = apiError(err);
          code = e.code === 'SERVICE_BUSY' ? '503 SERVICE_BUSY' : e.code;
        }
        samples.push({ code, ms: performance.now() - start });
        this.done.update((d) => d + 1);
      }
    };
    await Promise.all(Array.from({ length: Math.max(1, this.loadParallel) }, worker));
    const groups = new Map<string, number[]>();
    for (const s of samples) groups.set(s.code, [...(groups.get(s.code) ?? []), s.ms]);
    this.loadResults.set([...groups.entries()].map(([code, ms]) => ({
      code, count: ms.length, avgMs: ms.reduce((a, b) => a + b, 0) / ms.length, maxMs: Math.max(...ms),
    })));
    this.loading.set(false);
  }

  private pollBreaker(): void {
    this.api.circuitBreaker().subscribe({
      next: (r) => {
        const state = r.circuitBreakers.kitchen;
        const previous = this.cb()?.state;
        if (previous && previous !== state.state) {
          this.transitions.update((list) => [{ at: new Date(), from: previous, to: state.state }, ...list].slice(0, 12));
        }
        this.cb.set(state);
      },
      error: () => undefined,
    });
  }

  private loadNotifications(): void {
    this.api.recentNotifications(8).subscribe({ next: (n) => this.notifications.set(n), error: () => undefined });
  }
}
