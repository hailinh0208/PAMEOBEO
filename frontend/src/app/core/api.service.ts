import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AdminMenuItem, Availability, ChaosSettings, CircuitBreakerState, CookingPlan, Highlight, NotificationEntry, Order,
  OrderStatus,
  PlaceOrderRequest, PublicMenuItem,
} from './models';

/** All calls go to the API gateway (same origin, or proxied to :9000 by `ng serve`). */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  // ---------------------------------------------------------------- kitchen (public)
  menu(): Observable<PublicMenuItem[]> {
    return this.http.get<PublicMenuItem[]>('/api/kitchen/menu');
  }

  highlights(): Observable<Highlight[]> {
    return this.http.get<Highlight[]>('/api/kitchen/menu/highlights');
  }

  availability(date?: string): Observable<Availability> {
    return this.http.get<Availability>('/api/kitchen/availability', { params: date ? { date } : {} });
  }

  // ---------------------------------------------------------------- orders (customer)
  placeOrder(body: PlaceOrderRequest, idempotencyKey: string): Observable<Order> {
    return this.http.post<Order>('/api/orders', body, { headers: new HttpHeaders({ 'Idempotency-Key': idempotencyKey }) });
  }

  myOrders(): Observable<Order[]> {
    return this.http.get<Order[]>('/api/orders');
  }

  order(id: string): Observable<Order> {
    return this.http.get<Order>(`/api/orders/${id}`);
  }

  cancel(id: string): Observable<Order> {
    return this.http.post<Order>(`/api/orders/${id}/cancel`, {});
  }

  // ---------------------------------------------------------------- admin
  allOrders(cookingDate?: string): Observable<Order[]> {
    return this.http.get<Order[]>('/api/orders/admin', { params: cookingDate ? { cookingDate } : {} });
  }

  startCooking(date: string): Observable<{ cookingDate: string; ordersMovedToCooking: number }> {
    return this.http.post<{ cookingDate: string; ordersMovedToCooking: number }>(
      `/api/orders/admin/cooking-days/${date}/start-cooking`, {});
  }

  advance(id: string, status: OrderStatus): Observable<Order> {
    return this.http.patch<Order>(`/api/orders/admin/${id}/status`, { status });
  }

  changeCapacity(date: string, capacity: number): Observable<Availability> {
    return this.http.put<Availability>(`/api/kitchen/admin/cooking-days/${date}/capacity`, { capacity });
  }

  cookingPlan(date: string): Observable<CookingPlan> {
    return this.http.get<CookingPlan>(`/api/kitchen/admin/cooking-days/${date}/plan`);
  }

  fullMenu(): Observable<AdminMenuItem[]> {
    return this.http.get<AdminMenuItem[]>('/api/kitchen/admin/menu');
  }

  updateMenuItem(id: string, patch: { priceVnd?: number; active?: boolean; featured?: boolean }): Observable<AdminMenuItem> {
    return this.http.patch<AdminMenuItem>(`/api/kitchen/admin/menu/${id}`, patch);
  }

  uploadImage(menuItemId: string, photo: Blob): Observable<{ imageIds: string[] }> {
    const form = new FormData();
    form.append('file', photo, 'photo.jpg');
    return this.http.post<{ imageIds: string[] }>(`/api/kitchen/admin/menu/${menuItemId}/images`, form);
  }

  deleteImage(menuItemId: string, imageId: string): Observable<{ imageIds: string[] }> {
    return this.http.delete<{ imageIds: string[] }>(`/api/kitchen/admin/menu/${menuItemId}/images/${imageId}`);
  }

  makeCover(menuItemId: string, imageId: string): Observable<{ imageIds: string[] }> {
    return this.http.put<{ imageIds: string[] }>(`/api/kitchen/admin/menu/${menuItemId}/images/${imageId}/cover`, {});
  }

  chaos(): Observable<ChaosSettings> {
    return this.http.get<ChaosSettings>('/api/kitchen/admin/chaos');
  }

  setChaos(settings: ChaosSettings): Observable<ChaosSettings> {
    return this.http.put<ChaosSettings>('/api/kitchen/admin/chaos', settings);
  }

  /** Proxied by the gateway to order-service's /actuator/circuitbreakers (ADMIN only). */
  circuitBreaker(): Observable<{ circuitBreakers: { kitchen: CircuitBreakerState } }> {
    return this.http.get<{ circuitBreakers: { kitchen: CircuitBreakerState } }>('/api/system/circuit-breakers');
  }

  recentNotifications(limit = 10): Observable<NotificationEntry[]> {
    return this.http.get<NotificationEntry[]>('/api/notifications/admin/recent', { params: { limit } });
  }
}
