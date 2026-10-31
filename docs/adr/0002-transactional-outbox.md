# ADR-0002: Transactional Outbox + Polling Publisher

**Trạng thái:** Chấp nhận

## Bối cảnh
`orderRepository.save()` rồi `kafkaTemplate.send()` có 2 cách hỏng: lưu xong nhưng gửi lỗi thì **mất event** (saga kẹt ở `PENDING` mãi); gửi xong nhưng DB rollback thì **event ma**.

## Quyết định
- Ghi event vào bảng `outbox_events` **trong cùng transaction** với thay đổi nghiệp vụ (`OutboxWriter`, `Propagation.MANDATORY` để bắt buộc phải có transaction).
- `OutboxRelay` chạy mỗi 300 ms, lấy tối đa 100 dòng chưa gửi bằng `FOR UPDATE SKIP LOCKED`, gửi Kafka, chờ ack rồi mới đánh dấu `published_at`.
- Consumer luôn **idempotent** (`processed_events`), vì relay có thể gửi trùng.

## Phương án đã cân nhắc
- **Transaction Log Tailing (Debezium)**: gần realtime, không tốn query, nhưng phải dựng thêm Kafka Connect. Ghi lại như hướng nâng cấp.
- **Kafka transactions**: không bao được transaction của PostgreSQL.

## Hệ quả
- (+) Không mất event, không có event ma.
- (−) Độ trễ tối đa khoảng 300 ms. Có thể gửi trùng, đã xử lý bằng idempotent consumer.
