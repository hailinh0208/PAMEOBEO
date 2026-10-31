# Pameobeo — Frontend (Angular 22)

Giao diện cho khách hàng và admin. Gọi API qua **api-gateway**.

| Trang | Đường dẫn | Ai dùng | Use case |
|---|---|---|---|
| Đăng nhập / Đăng ký | `#/login`, `#/register` | Tất cả | UC01 |
| Đặt pate (menu, số suất còn lại, giỏ hàng) | `#/` | Khách | UC02 |
| Đơn của tôi, chi tiết đơn (timeline, tự cập nhật PENDING → CONFIRMED, hủy đơn) | `#/orders`, `#/orders/:id` | Khách | UC02, UC03 |
| Quản lý đơn (kế hoạch nấu, chốt đơn, giao hàng) | `#/admin/orders` | Admin | UC04 |
| Bếp (công suất ngày nấu, giá, bật/tắt món) | `#/admin/kitchen` | Admin | UC05 |
| Hệ thống (trạng thái Circuit Breaker theo thời gian thực, chaos, bắn tải, email đã gửi) | `#/admin/system` | Admin | UC06 + demo CB |

## Chạy

**Không cần Node:** bản build đã nằm sẵn trong `api-gateway/src/main/resources/static`. Chạy `scripts/start-all.sh` rồi mở http://localhost:9000.

**Phát triển (cần Node 20.19+ / 22 / 24, ví dụ `brew install node`):**
```bash
cd frontend
npm install
npm start            # http://localhost:4200, /api được proxy sang gateway :9000 (proxy.conf.json)
npm run build        # build production thẳng vào api-gateway/src/main/resources/static
```
Sau `npm run build` thì build lại và khởi động lại gateway (`scripts/start-all.sh` tự làm việc này).

## Kỹ thuật

- Standalone component, **signals**, zoneless, control flow `@if` / `@for`, lazy-load từng trang.
- `core/auth.interceptor.ts`: gắn `Authorization: Bearer`. Khi gặp 401 thì gọi `/api/auth/refresh` một lần (refresh token rotation) rồi gửi lại request; refresh thất bại thì đăng xuất.
- `core/guards.ts`: chặn các trang cần đăng nhập và các trang ADMIN. Server vẫn kiểm tra lại quyền, guard chỉ để trải nghiệm người dùng.
- Mỗi lần bấm **Đặt hàng** gửi kèm header `Idempotency-Key` (UUID), nên bấm 2 lần cũng chỉ tạo 1 đơn.
- Gặp lỗi `CUTOFF_PASSED` thì hỏi khách có đồng ý chuyển sang ngày nấu sau không, đồng ý thì gửi lại với `confirmNextCookingDay=true`.
- Dùng hash routing (`#/...`), nên gateway chỉ cần phục vụ file tĩnh, không cần fallback phía server.
- Style viết bằng CSS thuần, không dùng SCSS: trình biên dịch Sass native cần macOS 14+.
- Token được lưu ở `localStorage` cho đơn giản. Ứng dụng thật nên để refresh token trong cookie httpOnly.
