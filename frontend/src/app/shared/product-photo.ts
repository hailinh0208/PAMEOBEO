import { Component, HostListener, computed, input, signal } from '@angular/core';
import { MenuItem } from '../core/models';
import { imageUrl, menuEmoji } from './format';

/**
 * Cover photo of a product (or its emoji while the shop has not uploaded any). Clicking it opens the photos
 * full screen, to see the colour and texture of the pate: arrows / ‹ › to browse, Esc or a click outside to close.
 */
@Component({
  selector: 'app-product-photo',
  template: `
    @if (urls().length) {
      <button type="button" [class]="'photo photo-' + size()" (click)="open($event, 0)"
              [title]="'Xem ảnh ' + item().name">
        <img [src]="urls()[0]" [alt]="item().name" loading="lazy" />
        @if (urls().length > 1) { <span class="photo-count">📷 {{ urls().length }}</span> }
      </button>
    } @else {
      <div [class]="'photo-emoji photo-' + size()">{{ emoji() }}</div>
    }

    @if (index() !== null) {
      <div class="lightbox" (click)="close()" role="dialog" aria-modal="true" [attr.aria-label]="item().name">
        <figure (click)="$event.stopPropagation()">
          <img [src]="urls()[index()!]" [alt]="item().name + ' — ảnh ' + (index()! + 1)" />
          <figcaption>
            {{ item().name }}
            @if (urls().length > 1) { <span class="muted">· {{ index()! + 1 }}/{{ urls().length }}</span> }
          </figcaption>
          @if (urls().length > 1) {
            <button type="button" class="lightbox-nav prev" (click)="step(-1)" aria-label="Ảnh trước">‹</button>
            <button type="button" class="lightbox-nav next" (click)="step(1)" aria-label="Ảnh sau">›</button>
            <div class="lightbox-thumbs">
              @for (u of urls(); track u; let i = $index) {
                <button type="button" [class.on]="i === index()" (click)="index.set(i)">
                  <img [src]="u" [alt]="'Ảnh ' + (i + 1)" />
                </button>
              }
            </div>
          }
        </figure>
        <button type="button" class="lightbox-close" (click)="close()" aria-label="Đóng">✕</button>
      </div>
    }
  `,
})
export class ProductPhoto {
  readonly item = input.required<MenuItem>();
  /** card: carousel / chosen product, thumb: cart line */
  readonly size = input<'card' | 'thumb'>('card');

  protected readonly urls = computed(() => this.item().imageIds.map((id) => imageUrl(this.item().id, id)));
  protected readonly emoji = computed(() => menuEmoji(this.item()));
  /** index of the photo shown full screen, null when closed */
  protected readonly index = signal<number | null>(null);

  protected open(event: Event, i: number): void {
    event.stopPropagation();
    this.index.set(i);
  }

  protected close(): void {
    this.index.set(null);
  }

  protected step(delta: number): void {
    const n = this.urls().length;
    this.index.update((i) => (i === null ? null : (i + delta + n) % n));
  }

  @HostListener('document:keydown', ['$event'])
  protected onKey(e: KeyboardEvent): void {
    if (this.index() === null) return;
    if (e.key === 'Escape') this.close();
    if (e.key === 'ArrowLeft') this.step(-1);
    if (e.key === 'ArrowRight') this.step(1);
  }
}
