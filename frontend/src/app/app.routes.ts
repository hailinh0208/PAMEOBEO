import { Routes } from '@angular/router';
import { adminGuard, authGuard } from './core/guards';

export const routes: Routes = [
  { path: '', loadComponent: () => import('./pages/shop').then((m) => m.ShopPage), title: 'Pameobeo · Đặt pate' },
  { path: 'login', loadComponent: () => import('./pages/login').then((m) => m.LoginPage), title: 'Đăng nhập · Pameobeo' },
  { path: 'register', loadComponent: () => import('./pages/register').then((m) => m.RegisterPage), title: 'Đăng ký · Pameobeo' },
  {
    path: 'orders', canActivate: [authGuard], title: 'Đơn của tôi · Pameobeo',
    loadComponent: () => import('./pages/orders').then((m) => m.OrdersPage),
  },
  {
    path: 'orders/:id', canActivate: [authGuard], title: 'Chi tiết đơn · Pameobeo',
    loadComponent: () => import('./pages/order-detail').then((m) => m.OrderDetailPage),
  },
  {
    path: 'admin', canActivate: [authGuard, adminGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'orders' },
      { path: 'orders', title: 'Quản lý đơn · Pameobeo', loadComponent: () => import('./pages/admin/admin-orders').then((m) => m.AdminOrdersPage) },
      { path: 'kitchen', title: 'Bếp · Pameobeo', loadComponent: () => import('./pages/admin/admin-kitchen').then((m) => m.AdminKitchenPage) },
      { path: 'system', title: 'Hệ thống · Pameobeo', loadComponent: () => import('./pages/admin/admin-system').then((m) => m.AdminSystemPage) },
    ],
  },
  { path: '**', redirectTo: '' },
];
