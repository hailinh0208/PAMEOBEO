import { Pipe, PipeTransform } from '@angular/core';
import { AdminMenuItem, Meat, MenuItem, OrderStatus, PublicMenuItem } from '../core/models';

@Pipe({ name: 'vnd' })
export class VndPipe implements PipeTransform {
  transform(amount: number | null | undefined): string {
    return amount == null ? '' : new Intl.NumberFormat('vi-VN').format(amount) + 'đ';
  }
}

/** "2026-10-29" -> "Thứ Năm, 29/10" */
@Pipe({ name: 'cookingDay' })
export class CookingDayPipe implements PipeTransform {
  transform(isoDate: string | null | undefined): string {
    if (!isoDate) return '';
    const [y, m, d] = isoDate.split('-').map(Number);
    const date = new Date(y, m - 1, d);
    const weekday = date.toLocaleDateString('vi-VN', { weekday: 'long' });
    return `${weekday.charAt(0).toUpperCase()}${weekday.slice(1)}, ${String(d).padStart(2, '0')}/${String(m).padStart(2, '0')}`;
  }
}

export const STATUS_LABEL: Record<OrderStatus, string> = {
  PENDING: 'Đang xử lý',
  CONFIRMED: 'Đã xác nhận',
  COOKING: 'Đang nấu',
  DELIVERING: 'Đang giao',
  COMPLETED: 'Hoàn thành',
  CANCELLED: 'Đã hủy',
};

export const REASON_LABEL: Record<string, string> = {
  SOLD_OUT: 'Ngày nấu đã đủ suất',
  CUTOFF_PASSED: 'Đã qua giờ chốt đơn',
  CANCELLED_BY_CUSTOMER: 'Bạn đã hủy đơn',
};

export const MEATS: Meat[] = ['CHICKEN', 'TUNA', 'BEEF', 'SALMON'];

export const MEAT_LABEL: Record<Meat, string> = { CHICKEN: 'Gà', TUNA: 'Cá ngừ', BEEF: 'Bò', SALMON: 'Cá hồi' };

export function meatEmoji(meat: string): string {
  return ({ CHICKEN: '🐔', TUNA: '🐟', BEEF: '🐄', SALMON: '🍣' } as Record<string, string>)[meat] ?? '🐾';
}

export function menuEmoji(item: MenuItem): string {
  return item.meats.map(meatEmoji).join('');
}

/** Product photos are public and cached by the browser (an image id is never reused). */
export function imageUrl(menuItemId: string, imageId: string): string {
  return `/api/kitchen/menu/${menuItemId}/images/${imageId}`;
}

/** Most photos a product can have (checked again by the server). */
export const MAX_IMAGES = 3;

/** The 4 kinds of pate, in display order. */
export interface PateType {
  key: string;
  label: string;
  mix: boolean;
  withLiver: boolean;
}

export const PATE_TYPES: PateType[] = [
  { key: 'single-liver', label: '1 vị có gan', mix: false, withLiver: true },
  { key: 'single-no-liver', label: '1 vị không gan', mix: false, withLiver: false },
  { key: 'mix-liver', label: 'Mix 2 vị có gan', mix: true, withLiver: true },
  { key: 'mix-no-liver', label: 'Mix 2 vị không gan', mix: true, withLiver: false },
];

/** Admin only: "100g thịt gà · 100g gan · 50g cà rốt · 50g bí đỏ" */
export function recipeText(item: AdminMenuItem): string {
  return item.recipe.map((r) => `${r.grams}g ${r.label.toLowerCase()}`).join(' · ');
}

/** Customers see what is in the jar, not how much: "Thịt gà, gan, cà rốt, bí đỏ" */
export function ingredientsText(item: PublicMenuItem): string {
  const text = item.ingredients.map((x) => x.toLowerCase()).join(', ');
  return text.charAt(0).toUpperCase() + text.slice(1);
}

/** 1500 -> "1,5 kg", 300 -> "300 g" */
export function gramsText(grams: number): string {
  return grams >= 1000 ? `${new Intl.NumberFormat('vi-VN').format(grams / 1000)} kg` : `${grams} g`;
}

export function newIdempotencyKey(): string {
  return crypto.randomUUID();
}
