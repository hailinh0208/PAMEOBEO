import { Component, computed, input } from '@angular/core';
import { Order, OrderStatus } from '../core/models';
import { REASON_LABEL, STATUS_LABEL } from './format';

const STEPS: OrderStatus[] = ['PENDING', 'CONFIRMED', 'COOKING', 'DELIVERING', 'COMPLETED'];

/** Visual order state machine: PENDING -> CONFIRMED -> COOKING -> DELIVERING -> COMPLETED (or CANCELLED). */
@Component({
  selector: 'app-order-timeline',
  template: `
    @if (order().status === 'CANCELLED') {
      <div class="timeline-cancelled">
        ✖ Đơn đã hủy
        @if (reason()) { <span>— {{ reason() }}</span> }
      </div>
    } @else {
      <ol class="timeline">
        @for (step of steps; track step; let i = $index) {
          <li [class.done]="i < current()" [class.active]="i === current()">
            <span class="dot">{{ i < current() ? '✓' : i + 1 }}</span>
            <span class="label">{{ labels[step] }}</span>
          </li>
        }
      </ol>
    }
  `,
})
export class OrderTimeline {
  readonly order = input.required<Order>();
  protected readonly steps = STEPS;
  protected readonly labels = STATUS_LABEL;
  protected readonly current = computed(() => STEPS.indexOf(this.order().status));
  protected readonly reason = computed(() => {
    const r = this.order().statusReason;
    return r ? (REASON_LABEL[r] ?? r) : '';
  });
}
