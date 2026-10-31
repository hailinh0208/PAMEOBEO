import { Component, computed, input } from '@angular/core';
import { OrderStatus } from '../core/models';
import { STATUS_LABEL } from './format';

@Component({
  selector: 'app-status-badge',
  template: `<span class="badge" [class]="'badge badge-' + status().toLowerCase()">{{ label() }}</span>`,
})
export class StatusBadge {
  readonly status = input.required<OrderStatus>();
  protected readonly label = computed(() => STATUS_LABEL[this.status()]);
}
