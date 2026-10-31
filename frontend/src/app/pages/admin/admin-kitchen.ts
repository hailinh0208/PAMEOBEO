import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { errorText } from '../../core/errors';
import { AdminMenuItem, Availability, CookingPlan } from '../../core/models';
import { ToastService } from '../../core/toast.service';
import { CookingDayPipe, MAX_IMAGES, MEAT_LABEL, PATE_TYPES, gramsText, imageUrl, menuEmoji } from '../../shared/format';
import { resizeForUpload } from '../../shared/image-resize';

/** One row of the price table: a flavor or a pair of flavors, with its "with liver" and "without liver" items. */
interface PriceRow {
  label: string;
  emoji: string;
  withLiver?: AdminMenuItem;
  withoutLiver?: AdminMenuItem;
}

/** UC05: capacity per cooking day, ingredients to buy, full price table (flavor(s) x liver). */
@Component({
  selector: 'app-admin-kitchen',
  imports: [FormsModule, CookingDayPipe],
  template: `
    <h1>Bếp</h1>
    <div class="admin-grid">
      <section class="card">
        <h2>Công suất ngày nấu</h2>
        <label>Ngày nấu <input type="date" [(ngModel)]="date" (change)="loadDay()" /></label>
        @if (day(); as d) {
          <p><strong>{{ d.cookingDate | cookingDay }}</strong> — đã nhận {{ d.reserved }}/{{ d.capacity }} hộp, còn {{ d.remaining }}</p>
          <div class="progress"><span [style.width.%]="(d.reserved / (d.capacity || 1)) * 100"></span></div>
          <div class="row">
            <input type="number" min="0" [(ngModel)]="capacity" />
            <button class="btn primary" (click)="saveCapacity()">Lưu công suất</button>
          </div>
          <div class="row">
            <button class="btn ghost small" (click)="capacity = 50; saveCapacity()">Mặc định 50</button>
            <button class="btn ghost small" (click)="capacity = d.reserved + 1; saveCapacity()">Chỉ còn 1 suất (demo Saga)</button>
            <button class="btn ghost small" (click)="capacity = d.reserved + 100000; saveCapacity()">Rất lớn (demo tải)</button>
          </div>
        }

        <h2 class="section-gap">Nguyên liệu cần chuẩn bị</h2>
        @if (plan(); as p) {
          @if (p.items.length === 0) {
            <p class="muted">Chưa có hộp nào được giữ suất cho ngày này.</p>
          } @else {
            <table class="table">
              <tbody>
                @for (i of p.ingredients; track i.ingredient) {
                  <tr><td>{{ i.label }}</td><td class="num"><strong>{{ grams(i.grams) }}</strong></td></tr>
                }
              </tbody>
            </table>
            <h3>Theo món ({{ p.reservedJars }} hộp)</h3>
            <table class="table">
              <tbody>
                @for (i of p.items; track i.menuItemId) {
                  <tr><td>{{ i.name }}</td><td class="num">{{ i.jars }} hộp</td></tr>
                }
              </tbody>
            </table>
          }
          @if (p.jarsWithoutDetails > 0) {
            <p class="muted small">{{ p.jarsWithoutDetails }} hộp đặt trước khi có công thức, chưa tính vào nguyên liệu.</p>
          }
          <p class="muted small">Chỉ tính các đơn đã được bếp giữ suất (đơn bị hủy được trừ ra).</p>
        }
      </section>

      <section class="card">
        <h2>Bảng giá</h2>
        <table class="table">
          <thead><tr><th>Vị</th><th>Có gan</th><th>Không gan</th></tr></thead>
          <tbody>
            @for (r of rows(); track r.label) {
              <tr>
                <td>{{ r.emoji }} {{ r.label }}</td>
                @for (m of [r.withLiver, r.withoutLiver]; track $index) {
                  <td>
                    @if (m) {
                      <div class="price-cell">
                        <input type="number" min="1000" step="1000" [(ngModel)]="prices[m.id]" class="narrow" />
                        <button class="btn small" (click)="savePrice(m)" [disabled]="prices[m.id] === m.priceVnd">Lưu</button>
                        <label class="inline" title="Đang bán">
                          <input type="checkbox" [checked]="m.active" (change)="toggle(m)" /> bán
                        </label>
                        <button class="btn ghost small star" [class.on]="m.featured" (click)="toggleFeatured(m)"
                                [title]="m.featured ? 'Bỏ khỏi Gợi ý của tiệm' : 'Thêm vào Gợi ý của tiệm'">
                          {{ m.featured ? '★' : '☆' }}
                        </button>
                        <button class="btn ghost small" (click)="managing.set(m.id)" title="Ảnh sản phẩm">
                          📷 {{ m.imageIds.length }}/{{ maxImages }}
                        </button>
                      </div>
                    }
                  </td>
                }
              </tr>
            }
          </tbody>
        </table>
        <p class="muted small">Đổi giá không ảnh hưởng đơn đã đặt (giá được chụp lại lúc đặt).</p>
        <p class="muted small">
          ★ = <strong>Gợi ý của tiệm</strong>. Đầu trang đặt hàng hiện 5 món bán chạy nhất trong 30 ngày;
          khi chưa đủ dữ liệu bán thì các món ★ được hiện thay vào chỗ trống.
        </p>

        <h2 class="section-gap">Công thức mỗi hộp 400g</h2>
        <table class="table">
          <tbody>
            @for (r of recipes(); track r.label) {
              <tr><td>{{ r.label }}</td><td>{{ r.text }}</td></tr>
            }
          </tbody>
        </table>
        <p class="muted small">Chỉ admin thấy số lượng. Khách chỉ thấy tên nguyên liệu.</p>
      </section>
    </div>

    @if (managed(); as m) {
      <div class="lightbox" (click)="managing.set(null)" role="dialog" aria-modal="true">
        <section class="card image-manager" (click)="$event.stopPropagation()">
          <div class="page-header">
            <h2>📷 Ảnh · {{ m.name }}</h2>
            <button class="btn ghost small" (click)="managing.set(null)" aria-label="Đóng">✕</button>
          </div>
          <div class="image-grid">
            @for (id of m.imageIds; track id; let first = $first) {
              <figure>
                <img [src]="imageUrl(m.id, id)" [alt]="m.name" />
                <figcaption>
                  @if (first) {
                    <span class="badge badge-pick">Ảnh bìa</span>
                  } @else {
                    <button class="btn ghost small" (click)="makeCover(m, id)" [disabled]="uploading()">Làm ảnh bìa</button>
                  }
                  <button class="btn danger small" (click)="deleteImage(m, id)" [disabled]="uploading()">Xóa</button>
                </figcaption>
              </figure>
            }
            @if (m.imageIds.length < maxImages) {
              <label class="image-add" [class.busy]="uploading()">
                <input type="file" accept="image/jpeg,image/png,image/webp" multiple hidden
                       [disabled]="uploading()" (change)="upload(m, $event)" />
                <span>{{ uploading() ? 'Đang tải lên…' : '+ Thêm ảnh' }}</span>
                <span class="muted small">còn {{ maxImages - m.imageIds.length }} chỗ</span>
              </label>
            }
          </div>
          <p class="muted small">
            Tối đa {{ maxImages }} ảnh, JPG / PNG / WebP. Ảnh được tự thu nhỏ (cạnh dài tối đa 1600px) trước khi tải lên.
            Ảnh bìa hiện trên trang đặt hàng; khách bấm vào ảnh để xem tất cả.
          </p>
        </section>
      </div>
    }
  `,
})
export class AdminKitchenPage {
  private readonly api = inject(ApiService);
  private readonly toast = inject(ToastService);

  protected date = '';
  protected capacity = 50;
  protected readonly day = signal<Availability | null>(null);
  protected readonly plan = signal<CookingPlan | null>(null);
  protected readonly menu = signal<AdminMenuItem[]>([]);
  protected prices: Record<string, number> = {};
  protected readonly grams = gramsText;
  protected readonly imageUrl = imageUrl;
  protected readonly maxImages = MAX_IMAGES;
  /** Product whose photos are being managed (dialog open), by id so it follows menu reloads */
  protected readonly managing = signal<string | null>(null);
  protected readonly managed = computed(() => this.menu().find((m) => m.id === this.managing()) ?? null);
  protected readonly uploading = signal(false);

  /** Singles first, then mixes; the server already returns the menu in that order. */
  protected readonly rows = computed(() => {
    const rows = new Map<string, PriceRow>();
    for (const m of this.menu()) {
      const key = m.meats.join('+');
      const row = rows.get(key) ?? {
        label: (m.mix ? 'Mix ' : '') + m.meats.map((x) => MEAT_LABEL[x]).join(' + '),
        emoji: menuEmoji(m),
      };
      if (m.withLiver) row.withLiver = m; else row.withoutLiver = m;
      rows.set(key, row);
    }
    return [...rows.values()].sort((a, b) => Number(a.label.startsWith('Mix')) - Number(b.label.startsWith('Mix')));
  });

  /** Recipe of each of the 4 kinds, from any item of that kind: "100g + 100g thịt · 100g gan · 50g cà rốt · 50g bí đỏ" */
  protected readonly recipes = computed(() => PATE_TYPES.flatMap((t) => {
    const sample = this.menu().find((m) => m.mix === t.mix && m.withLiver === t.withLiver);
    if (!sample) return [];
    // recipe order: the meat(s) first, then liver / vegetables
    const meat = sample.recipe.slice(0, sample.meats.length).map((r) => `${r.grams}g`).join(' + ');
    const rest = sample.recipe.slice(sample.meats.length).map((r) => `${r.grams}g ${r.label.toLowerCase()}`);
    return [{ label: t.label, text: [`${meat} thịt`, ...rest].join(' · ') }];
  }));

  constructor() {
    this.api.availability().subscribe((a) => { this.date = a.cookingDate; this.setDay(a); this.loadPlan(); });
    this.loadMenu();
  }

  protected loadDay(): void {
    this.api.availability(this.date).subscribe((a) => this.setDay(a));
    this.loadPlan();
  }

  protected saveCapacity(): void {
    this.api.changeCapacity(this.date, this.capacity).subscribe({
      next: (a) => { this.setDay(a); this.toast.success(`Công suất ${a.cookingDate}: ${a.capacity} hộp`); },
      error: (e) => this.toast.error(errorText(e)),
    });
  }

  protected toggle(m: AdminMenuItem): void {
    this.api.updateMenuItem(m.id, { active: !m.active }).subscribe({
      next: () => { this.toast.success(`${m.name}: ${m.active ? 'ngừng bán' : 'đang bán'}`); this.loadMenu(); },
      error: (e) => this.toast.error(errorText(e)),
    });
  }

  protected toggleFeatured(m: AdminMenuItem): void {
    this.api.updateMenuItem(m.id, { featured: !m.featured }).subscribe({
      next: () => {
        this.toast.success(`${m.name}: ${m.featured ? 'bỏ khỏi' : 'thêm vào'} Gợi ý của tiệm`);
        this.loadMenu();
      },
      error: (e) => this.toast.error(errorText(e)),
    });
  }

  /** Uploads the chosen files one by one (resized first), up to the free places. */
  protected async upload(m: AdminMenuItem, event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files ?? []);
    input.value = ''; // the same file can be picked again later
    const free = MAX_IMAGES - m.imageIds.length;
    if (files.length > free) this.toast.error(`Chỉ còn ${free} chỗ: tải lên ${free} ảnh đầu tiên.`);
    this.uploading.set(true);
    try {
      for (const file of files.slice(0, free)) {
        const photo = await resizeForUpload(file);
        await firstValueFrom(this.api.uploadImage(m.id, photo));
      }
      this.toast.success(`Đã tải ảnh lên cho ${m.name}`);
    } catch (e) {
      this.toast.error(e instanceof HttpErrorResponse ? errorText(e) : 'Không đọc được ảnh, hãy chọn file JPG, PNG hoặc WebP.');
    } finally {
      this.uploading.set(false);
      this.loadMenu();
    }
  }

  protected makeCover(m: AdminMenuItem, imageId: string): void {
    this.api.makeCover(m.id, imageId).subscribe({
      next: () => { this.toast.success('Đã đổi ảnh bìa'); this.loadMenu(); },
      error: (e) => this.toast.error(errorText(e)),
    });
  }

  protected deleteImage(m: AdminMenuItem, imageId: string): void {
    if (!confirm('Xóa ảnh này?')) return;
    this.api.deleteImage(m.id, imageId).subscribe({
      next: () => { this.toast.success('Đã xóa ảnh'); this.loadMenu(); },
      error: (e) => this.toast.error(errorText(e)),
    });
  }

  protected savePrice(m: AdminMenuItem): void {
    this.api.updateMenuItem(m.id, { priceVnd: this.prices[m.id] }).subscribe({
      next: () => { this.toast.success(`Đã đổi giá ${m.name}`); this.loadMenu(); },
      error: (e) => this.toast.error(errorText(e)),
    });
  }

  private setDay(a: Availability): void {
    this.day.set(a);
    this.capacity = a.capacity;
  }

  private loadPlan(): void {
    if (!this.date) return;
    this.api.cookingPlan(this.date).subscribe({
      next: (p) => this.plan.set(p),
      error: (e) => this.toast.error(errorText(e)),
    });
  }

  private loadMenu(): void {
    this.api.fullMenu().subscribe((m) => {
      this.menu.set(m);
      this.prices = Object.fromEntries(m.map((x) => [x.id, x.priceVnd]));
    });
  }
}
