# ADR-0006: Clean Architecture + DDD trong từng service

**Trạng thái:** Chấp nhận

## Quyết định
- Mỗi service có 4 package: `domain` / `application` (`port/in`, `port/out`, `service`) / `infrastructure` / `interfaces`. Phụ thuộc chỉ hướng vào trong.
- `domain` là Java thuần, chứa aggregate và luật bất biến:
  - `Order`: state machine, tối đa 10 hộp, chỉ hủy trước giờ chốt
  - `CookingDay`: không vượt công suất, kiểm tra giờ chốt
  - `User`: khóa tài khoản
  - `RefreshToken`: idle / absolute timeout, rotation
  - Value object: `Money`, `OrderItem`, `CutoffPolicy`, `LoginPolicy`, `SessionPolicy`
- JPA entity riêng ở `infrastructure.persistence`, map qua `fromDomain` / `toDomain`.
- Lớp application không có annotation Spring trừ `@Transactional`. Bean được ghép ở `BeanConfig`.
- Luật phụ thuộc được kiểm tra tự động bằng **ArchUnit** (`CleanArchitectureTest`) trong `mvn verify`.

## Hệ quả
- (+) Test domain và use case không cần Spring, DB hay Kafka (`OrderTest`, `PlaceOrderServiceTest` dùng fake), chạy rất nhanh.
- (+) Đổi hạ tầng (ví dụ Kafka sang RabbitMQ) chỉ đụng tới `infrastructure`.
- (−) Nhiều class hơn (entity + domain + mapper). Chấp nhận được vì mục tiêu là học.
