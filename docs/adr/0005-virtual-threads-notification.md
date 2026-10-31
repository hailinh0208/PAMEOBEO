# ADR-0005: notification-service xử lý bất đồng bộ bằng Java 21 virtual threads

**Trạng thái:** Chấp nhận

## Bối cảnh
Đề bài yêu cầu ít nhất 1 service xử lý bất đồng bộ (WebFlux hoặc Java 21). Gửi email là I/O chậm (vài chục đến vài trăm ms) và không được làm chậm việc đặt hàng.

## Quyết định
Bất đồng bộ ở **hai tầng**:
1. **Giao tiếp:** order-service chỉ phát `OrderStatusChanged` lên Kafka rồi trả lời ngay. notification-service xử lý độc lập.
2. **Thực thi:** các email của một event (khách + shop) được gửi **song song** bằng `CompletableFuture.supplyAsync(..., Executors.newVirtualThreadPerTaskExecutor())`. `spring.threads.virtual.enabled=true` cũng chuyển thread của Tomcat và Kafka listener sang virtual thread.

Bằng chứng: log `Sending ... on VirtualThread[#83]/runnable@ForkJoinPool-1-worker-2 (virtual=true)`, cột `thread` trong `notification_logs`, endpoint `GET /api/notifications/admin/recent`.

Ngoài ra **api-gateway chạy trên WebFlux** (Reactor Netty, non-blocking).

## Phương án đã cân nhắc
- **WebFlux cho notification**: code phải viết theo kiểu reactive (Mono/Flux), JavaMail lại là blocking nên vẫn phải đẩy sang scheduler riêng.
- **`@Async` với thread pool thường**: phải tự chọn kích thước pool. Virtual thread không cần pool, mỗi task một thread, bị "park" khi chờ I/O.

## Hệ quả
- (+) Code vẫn viết kiểu tuần tự dễ đọc, nhưng xử lý được rất nhiều tác vụ I/O đồng thời.
- (−) Cần Java 21+. Không nên dùng `synchronized` quanh I/O vì sẽ "ghim" (pin) carrier thread.
