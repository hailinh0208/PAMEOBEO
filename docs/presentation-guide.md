# Hướng dẫn thuyết trình & quay video

> Đề bài cho phép dùng AI nhưng **phải thuyết trình được hiểu biết của mình**. Tài liệu này giúp bạn nắm dự án trong khoảng 2–3 buổi tối: đọc theo thứ tự, mở đúng file, tự trả lời các câu hỏi ở mục 5 thành lời trước khi quay.

---

## 1. Lộ trình tự nắm dự án (trước khi quay)

| Buổi | Đọc / làm | Mục tiêu |
|---|---|---|
| 1 | `docs/use-cases.md` (UC02, UC03) → `docs/architecture.md` mục 1, 4, 5 | Kể được luồng đặt hàng từ đầu tới cuối mà không nhìn tài liệu |
| 1 | Chạy `scripts/demo.sh`, mở Kafka UI (http://localhost:8090) và Mailpit (http://localhost:8025) | Thấy event và email thật |
| 2 | Đọc code theo **thứ tự ở mục 3** | Biết mỗi tiêu chí nằm ở file nào |
| 2 | Chạy `scripts/cb-demo.sh` và JMeter GUI; thử nút **Chậm 3s** + **Gửi đơn** ở trang web Hệ thống | Tự giải thích được 3 trạng thái của breaker |
| 3 | Tự trả lời **mục 5 (Q&A)** thành tiếng, quay thử 1 lần | Nói trôi chảy |

---

## 2. Kịch bản video (12–15 phút)

| Phút | Nội dung | Màn hình |
|---|---|---|
| 0:00–1:00 | **Bài toán:** shop pate làm theo đơn, chốt 18:00, nấu rồi giao 18–22h hôm sau, tối đa 50 hộp/ngày. Bài toán khó: tranh suất cuối, giờ chốt, bếp chậm | Giao diện web http://localhost:9000 (logo, số suất còn lại, giờ chốt) |
| 1:00–1:30 | **Demo nhanh trên web:** đăng ký, chọn pate, đặt hàng. Màn hình chi tiết đơn tự chuyển *Đang xử lý → Đã xác nhận* (Saga bất đồng bộ) | Web `#/` → `#/orders/:id` |
| 1:00–3:00 | **Kiến trúc:** 5 service, mỗi service một DB, gateway, Kafka. Vì sao REST cho "hỏi" và Kafka cho "giữ suất" | `docs/architecture.md` sơ đồ mục 1 + bảng mục 4 |
| 3:00–4:30 | **Tiêu chí 1 – Security:** `scripts/demo.sh security` → 401 / 200 / 403. Giải thích RS256 + JWKS, gateway và service đều kiểm tra. Mở nhanh `JwtAccessTokenIssuer`, `SecurityConfig` của gateway | Terminal + IDE |
| 4:30–7:00 | **Tiêu chí 2, 3 – Giao tiếp + Saga:** `scripts/demo.sh order` → PENDING rồi CONFIRMED. `scripts/demo.sh saga` → 2 khách tranh 1 hộp: một CONFIRMED, một CANCELLED. Mở Kafka UI xem `OrderCreated` → `SlotFull`. Mở `KitchenApplicationService.reserve()` và `KitchenReplyService` | Terminal + Kafka UI + IDE |
| 7:00–8:00 | **Outbox + Idempotency:** vì sao không `save()` rồi `send()`. Mở `OrderTransactions`, `OutboxRelay`, `JdbcProcessedEventStore`. `scripts/demo.sh idempotency` | IDE + Terminal |
| 8:00–9:30 | **Tiêu chí 5 – Clean Architecture + DDD:** mở cây package của order-service, chỉ chiều phụ thuộc. Mở `Order` (state machine) và `CookingDay` (luật 50 hộp). Chạy `CleanArchitectureTest` (ArchUnit) | IDE |
| 9:30–10:30 | **Tiêu chí 6 – Bất đồng bộ:** `scripts/demo.sh async` → cột thread hiện `VirtualThread[...]`. Mở `NotificationService`. Mở Mailpit | Terminal + Mailpit |
| 10:30–13:30 | **Tiêu chí 4 – Circuit Breaker + JMeter:** mở trang web **Hệ thống** (`#/admin/system`) ở một bên màn hình, JMeter ở bên kia. Chạy JMeter: thẻ trạng thái trên web đổi màu CLOSED → OPEN → HALF_OPEN → CLOSED và lịch sử chuyển trạng thái hiện ngay. Đưa ra bảng so sánh có / không có breaker | Web + JMeter |
| 13:30–14:30 | **Tổng kết:** bảng đối chiếu tiêu chí (README). Một điều học được: *"với 50 hộp mỗi ngày thì monolith là đủ, microservice ở đây để học; tôi hiểu cái giá: Saga, outbox, idempotency, eventual consistency"* | README |

> 💡 Trước khi quay: `scripts/stop-all.sh && scripts/start-all.sh`, chạy JMeter một lần để warm-up, rồi tăng font chữ của terminal và IDE.

---

## 3. Đối chiếu tiêu chí với code

| # | Tiêu chí đề bài | Áp dụng thế nào | File chính | Bằng chứng khi demo |
|---|---|---|---|---|
| 1 | Service xử lý security, dùng Spring Security | **auth-service** cấp JWT RS256, JWKS, refresh token rotation, khóa sau 5 lần sai. **api-gateway** và **mọi service** là OAuth2 Resource Server | `auth-service/.../application/service/AuthApplicationService.java`, `.../infrastructure/security/JwtAccessTokenIssuer.java`, `api-gateway/.../SecurityConfig.java`, `*/infrastructure/config/SecurityConfig.java` | `scripts/demo.sh security`: 401 / 200 / 403 / 401 (token bị sửa) |
| 2 | ≥ 2 microservice giao tiếp với nhau | order → kitchen qua **REST** (quote); order ↔ kitchen ↔ notification qua **Kafka** | `order-service/.../infrastructure/client/KitchenRestClient.java`, `*/infrastructure/messaging/*Listener.java` | `scripts/demo.sh order`, Kafka UI |
| 3 | ≥ 1 pattern CQRS / Event Sourcing / Saga | **Saga choreography** có bù trừ, kèm **Outbox** và **Idempotent Consumer** | `kitchen-service/.../application/service/KitchenApplicationService.java#reserve`, `order-service/.../application/service/KitchenReplyService.java`, `*/infrastructure/messaging/Outbox*.java` | `scripts/demo.sh saga` (một CONFIRMED, một CANCELLED) |
| 4 | Circuit Breaker cho API giữa các service | Resilience4j `@CircuitBreaker("kitchen")`, timeout ở HTTP client, fallback 503, bỏ qua 4xx, không retry | `KitchenRestClient.java`, `KitchenClientConfig.java`, `order-service/src/main/resources/application.yml` | JMeter, `scripts/cb-demo.sh`, `/actuator/circuitbreakers`, log `CLOSED -> OPEN` |
| 5 | Clean Architecture + DDD | `domain / application (ports) / infrastructure / interfaces`; aggregate `Order`, `CookingDay`, `User`; value object `Money`, `CutoffPolicy`; JPA entity tách riêng | `*/domain/model/*`, `*/infrastructure/config/BeanConfig.java`, `*/architecture/CleanArchitectureTest.java` | Cây package; `mvn verify` chạy test ArchUnit |
| 6 | ≥ 1 service bất đồng bộ (WebFlux hoặc Java 21) | **notification-service**: Kafka + gửi email song song trên **virtual threads** (Java 21). **api-gateway** chạy WebFlux | `notification-service/.../application/service/NotificationService.java`, `.../infrastructure/config/BeanConfig.java` | `scripts/demo.sh async`: `VirtualThread[#..]`; log `virtual=true` |
| — | Demo CB bằng JMeter, tái hiện quá tải | Test plan có chaos switch, theo dõi breaker, warm-up | `jmeter/circuit-breaker-demo.jmx`, `docs/circuit-breaker-demo.md` | Báo cáo HTML + bảng số liệu |

### Thứ tự đọc code gợi ý

1. `order-service/.../domain/model/Order.java` + `OrderStatus.java`: state machine, đọc là hiểu vòng đời đơn
2. `kitchen-service/.../domain/model/CookingDay.java` + `CutoffPolicy.java`: luật 50 hộp và 18:00
3. `order-service/.../application/service/PlaceOrderService.java`: use case đặt hàng (Ex1–Ex8 có comment)
4. `order-service/.../application/service/OrderTransactions.java`: vì sao tách transaction
5. `kitchen-service/.../application/service/KitchenApplicationService.java#reserve`: bước Saga ở kitchen
6. `order-service/.../application/service/KitchenReplyService.java`: bước cuối Saga + bù trừ
7. `*/infrastructure/messaging/OutboxWriter.java`, `OutboxRelay.java`, `JdbcProcessedEventStore.java`
8. `order-service/.../infrastructure/client/KitchenRestClient.java` + `application.yml` (resilience4j)
9. `auth-service/.../domain/model/User.java`, `RefreshToken.java`, rồi `AuthApplicationService.java`
10. `api-gateway/.../SecurityConfig.java`, `CorrelationIdFilter.java`
11. `notification-service/.../application/service/NotificationService.java`

---

## 4. Những câu "chốt" nên nói

- *"Đơn được tạo ở trạng thái **PENDING** và API trả **202 Accepted**, vì việc giữ suất xảy ra **bất đồng bộ** ở kitchen. Đây là eventual consistency."*
- *"Số suất mà REST trả về chỉ để **báo lỗi sớm**. Quyết định cuối cùng là ở kitchen khi xử lý `OrderCreated`, có optimistic locking, nên không bao giờ nhận quá 50 hộp."*
- *"Tôi không gọi `save()` rồi `kafka.send()`, vì lưu xong mà gửi lỗi thì mất event. Event được ghi vào bảng outbox **trong cùng transaction**."*
- *"Kafka đảm bảo **at-least-once**, nên mọi consumer đều idempotent: `processed_events` có primary key là `eventId`."*
- *"Lời gọi REST tới kitchen nằm **ngoài** transaction DB, nên kitchen chậm không giữ connection DB của order-service."*
- *"Tôi **không bật retry** cho lời gọi đồng bộ, vì retry khi kitchen đang quá tải sẽ làm nó quá tải hơn (retry storm)."*
- *"Với quy mô 50 hộp mỗi ngày, monolith là đủ. Tôi làm microservice để học, và hiểu cái giá của nó."*

---

## 5. Câu hỏi thường gặp (tự trả lời thành tiếng)

**Kiến trúc**
1. **Vì sao vừa REST vừa Kafka giữa order và kitchen?** REST để hỏi và báo lỗi ngay cho khách (hết suất, qua giờ chốt, giá). Kafka để giữ suất một cách chắc chắn (không mất khi kitchen chết) và tuần tự hóa an toàn.
2. **Database per service: muốn hiện tên món trong đơn thì sao?** Tên và giá được chụp lại vào `order_items` lúc đặt (snapshot), không JOIN sang kitchen.
3. **Vì sao Saga choreography mà không orchestration?** Saga chỉ có 2 bên và 1 bước. Thêm orchestrator là phức tạp không cần thiết. Nếu có thêm thanh toán và giao hàng thì nên chuyển sang orchestration.
4. **Khi nào không nên dùng microservice?** Team nhỏ, nghiệp vụ chưa ổn định, không có nhu cầu scale hay deploy độc lập. Pameobeo thật sự chỉ cần monolith.

**Saga & messaging**
5. **Kitchen chết lúc khách đặt hàng thì sao?** Nếu chết ở bước quote (REST) thì breaker / fallback trả 503, không tạo đơn. Nếu kitchen chết sau khi đơn đã PENDING thì `OrderCreated` nằm chờ trong Kafka, kitchen sống lại sẽ xử lý tiếp, đơn được xác nhận muộn hơn.
6. **Hai khách cùng đặt hộp cuối thì sao?** Cả hai qua bước quote. Kitchen xử lý `OrderCreated`: người tới trước giữ được suất. Người sau nhận `SlotFull`, đơn bị hủy (bù trừ) và nhận email. Nếu xử lý đúng cùng lúc thì optimistic lock làm một transaction thất bại và retry.
7. **Outbox relay gửi trùng thì sao?** Consumer ghi `eventId` vào `processed_events` trong cùng transaction. Nếu đã có thì bỏ qua.
8. **Thứ tự event có được đảm bảo không?** Có, trong phạm vi một đơn: key = `orderId`, nên các event của một đơn nằm trên cùng partition.
9. **Khách hủy lúc kitchen đang giữ suất?** `SlotReserved` tới sau thì order bỏ qua (`confirm()` chỉ chạy khi đơn còn PENDING). Kitchen nhận `OrderCancelled` ngay sau (cùng partition) và trả suất.
10. **Message lỗi mãi thì sao?** Retry với backoff, sau đó chuyển sang `<topic>.DLT`, không chặn các message phía sau.

**Circuit Breaker**
11. **3 trạng thái?** CLOSED: gọi bình thường và đếm lỗi. OPEN: chặn ngay, trả fallback. HALF_OPEN: cho vài lời gọi thử; thành công thì CLOSED, lỗi thì OPEN lại.
12. **Vì sao bỏ qua 4xx?** 4xx là lỗi nghiệp vụ (ví dụ món không tồn tại), kitchen vẫn khỏe. Tính 4xx vào sẽ làm breaker mở sai.
13. **Vì sao không dùng `@TimeLimiter`?** Nó chỉ áp dụng cho kiểu trả về bất đồng bộ (`CompletableFuture`). Lời gọi này đồng bộ nên timeout đặt ở HTTP client (read timeout 2 s).
14. **Fallback trả gì?** 503 `SERVICE_BUSY` + `Retry-After: 10`, không tạo đơn. Không đoán số suất vì đoán sai sẽ nhận quá công suất.

**Security**
15. **RS256 khác HS256?** RS256 dùng cặp khóa: chỉ auth-service có private key để ký, các service khác chỉ có public key để kiểm tra. HS256 dùng chung một secret, ai giữ secret cũng ký được token.
16. **Gateway đã kiểm tra rồi, sao service còn kiểm tra?** Defense in depth: nếu gọi thẳng vào service, bỏ qua gateway (ví dụ từ trong mạng nội bộ), request vẫn bị chặn.
17. **JWT stateless thì đăng xuất / hết session thế nào?** Access token sống ngắn (15 phút). Session được quản lý bằng refresh token lưu DB: idle 30 phút, tối đa 7 ngày, logout thì thu hồi refresh token.
18. **Chống brute force?** Đếm số lần sai trong bảng `users`; sai 5 lần thì `locked_until = now + 15 phút`, kể cả nhập đúng mật khẩu cũng bị từ chối.

**Clean Architecture / DDD / Async**
19. **Aggregate là gì, ví dụ?** Một cụm đối tượng có một gốc, mọi thay đổi đi qua gốc để giữ luật bất biến. `CookingDay` giữ luật "không vượt công suất"; `Order` giữ state machine.
20. **Vì sao tách JPA entity khỏi domain?** Để domain không phụ thuộc framework, test không cần DB, và đổi persistence không ảnh hưởng nghiệp vụ. ArchUnit kiểm tra luật này.
21. **Virtual thread khác platform thread?** Virtual thread rất nhẹ, do JVM quản lý; khi chờ I/O thì bị "park" và nhả carrier thread. Vì vậy có thể chạy hàng nghìn tác vụ I/O đồng thời mà không cần thread pool lớn.
22. **Gọi `@Transactional` trên chính `this` thì sao?** Không qua proxy nên không có transaction. Đó là lý do `OrderTransactions` là một bean riêng.

---

## 6. Phạm vi và hướng phát triển (nói ở phần kết)

| Đã làm | Để sau 6/11 |
|---|---|
| Saga + Outbox + Idempotency + DLT | CQRS (`order_view` từ event) |
| Circuit Breaker + JMeter | Redis: rate limiting ở gateway, blacklist JWT, mở suất bằng `DECR` |
| JWT RS256 + refresh rotation + lockout | Thanh toán VNPay (cọc 50%, COD từ đơn thứ 2) |
| Clean Architecture + ArchUnit | Docker image cho từng service, Kubernetes, Prometheus/Grafana, distributed tracing |
