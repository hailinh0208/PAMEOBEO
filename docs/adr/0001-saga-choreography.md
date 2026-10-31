# ADR-0001: Saga kiểu choreography cho luồng đặt hàng

**Trạng thái:** Chấp nhận

## Bối cảnh
Đơn hàng nằm ở `order_db`, suất nấu nằm ở `kitchen_db`. Không có transaction chung giữa hai database. Đề bài yêu cầu ít nhất 1 pattern trong CQRS / Event Sourcing / Saga.

## Quyết định
Dùng **Saga choreography** qua Kafka:
`OrderCreated` → kitchen giữ suất → `SlotReserved` (đơn `CONFIRMED`) hoặc `SlotFull` (bù trừ: đơn `CANCELLED`). Khi khách hủy: `OrderCancelled` → kitchen trả suất.

## Phương án đã cân nhắc
- **Orchestration** (có một orchestrator điều phối): rõ ràng hơn khi saga có nhiều bước. Ở đây saga chỉ có 2 bên, 1 bước, nên thêm orchestrator là phức tạp không cần thiết.
- **2PC / XA**: khóa tài nguyên lâu, Kafka và PostgreSQL không hỗ trợ tốt, đi ngược tinh thần microservice.
- **Event Sourcing**: quá nặng cho quy mô 50 hộp mỗi ngày.

## Hệ quả
- (+) Hai service độc lập. Kitchen chết thì đơn vẫn được tạo (`PENDING`) và được xác nhận khi kitchen sống lại.
- (−) **Eventual consistency**: có khoảng vài trăm ms đơn ở `PENDING`. API trả `202 Accepted`, client hỏi lại trạng thái (hoặc nhận email).
- (−) Luồng nằm rải rác ở 2 service. Bù lại có sequence diagram trong `docs/architecture.md`.
