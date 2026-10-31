# ADR-0007: Optimistic locking để không nhận quá 50 hộp

**Trạng thái:** Chấp nhận

## Bối cảnh
Kitchen nghe `order.events` với 3 consumer song song (3 partition). Nhiều đơn của cùng một ngày nấu có thể được xử lý cùng lúc, dẫn tới nguy cơ "lost update" và nhận quá công suất.

## Quyết định
- `cooking_days.version` + `@Version` (optimistic locking). Lưu bằng `saveAndFlush` để xung đột lộ ra ngay trong use case.
- Khi xung đột: transaction rollback (cả bản ghi `processed_events`), Kafka `DefaultErrorHandler` retry với exponential backoff. Lần sau đọc dữ liệu mới.
- Lưới an toàn ở DB: `CHECK (reserved_count <= capacity)`.

## Phương án đã cân nhắc
- **Pessimistic lock** (`SELECT ... FOR UPDATE`): an toàn, nhưng giữ khóa lâu hơn và giảm thông lượng khi tải bình thường.
- **Redis DECR**: nhanh nhất cho lúc mở suất có nhiều người tranh. Để dành cho giai đoạn sau 6/11.

## Hệ quả
- (+) Không khóa khi không có tranh chấp (đa số thời gian).
- (−) Tranh chấp cao thì nhiều lần retry. Với 50 hộp mỗi ngày thì không đáng kể.
