# ADR-0004: Circuit Breaker (Resilience4j) cho lời gọi order → kitchen

**Trạng thái:** Chấp nhận

## Bối cảnh
`POST /api/orders` gọi đồng bộ `POST /api/kitchen/quotes`. Nếu kitchen chậm, thread của order-service bị chặn chờ, request dồn lại và order-service sập theo (lỗi dây chuyền).

## Quyết định
- `@CircuitBreaker(name = "kitchen")` trên `KitchenRestClient.quote()`.
- Cấu hình: cửa sổ 20 lời gọi gần nhất, tối thiểu 10; OPEN khi ≥ 50% lỗi **hoặc** ≥ 50% chậm (> 1s); OPEN 10s rồi sang HALF_OPEN, cho 5 lời gọi thử.
- Chỉ tính **lỗi hạ tầng** (`ResourceAccessException`: timeout, connection refused; `HttpServerErrorException`: 5xx). **Bỏ qua 4xx** vì đó là lỗi nghiệp vụ, kitchen vẫn khỏe.
- **Timeout đặt ở HTTP client** (connect 1s, read 2s), vì `@TimeLimiter` của Resilience4j chỉ áp dụng cho kiểu trả về bất đồng bộ.
- **Không retry** lời gọi đồng bộ này: retry khi kitchen đang quá tải sẽ nhân tải lên (retry storm). Retry chỉ dùng ở Kafka consumer.
- **Fallback**: trả `503 SERVICE_BUSY` + `Retry-After: 10`, không tạo đơn. Khi breaker OPEN, fallback chạy ngay, kitchen không bị gọi.
- Lời gọi REST nằm **ngoài** transaction DB (`OrderTransactions` tách riêng), nên kitchen chậm không giữ connection DB.

## Hệ quả
- (+) Kitchen chậm thì order-service vẫn trả lời trong vài ms. Kitchen có thời gian hồi phục.
- (−) Trong lúc OPEN, khách không đặt được hàng (chấp nhận được: thà báo bận ngay còn hơn treo 30 giây).
- Kiểm chứng: `scripts/cb-demo.sh`, `jmeter/circuit-breaker-demo.jmx`, `docs/circuit-breaker-demo.md`.
