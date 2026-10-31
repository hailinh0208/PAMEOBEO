# Events — Pameobeo

Mọi message trên Kafka đều là **JSON string** có chung một lớp vỏ (envelope):

```json
{
  "eventId": "6f1c2a5e-0b7d-4a43-9c55-3f0e2f1b8d21",
  "eventType": "OrderCreated",
  "occurredAt": "2026-10-28T03:45:12.345Z",
  "payload": { "...": "..." }
}
```

- `eventId`: consumer dùng để chống xử lý trùng (bảng `processed_events`).
- `eventType`: consumer dùng để biết cách đọc `payload`. Cũng được gửi kèm trong Kafka header `eventType`.
- **Key** của message luôn là `orderId`. Mọi event của cùng một đơn nằm trên cùng partition, nên giữ đúng thứ tự.
- Mỗi topic có 3 partition. Message lỗi sau khi đã retry được chuyển sang `<topic>.DLT`.

## Topics

| Topic | Producer | Consumer (group) | Events |
|---|---|---|---|
| `order.events` | order-service | kitchen-service (`kitchen-service`) | `OrderCreated`, `OrderCancelled` |
| `kitchen.events` | kitchen-service | order-service (`order-service`) | `SlotReserved`, `SlotFull`, `SlotReleased` |
| `order.status-changed` | order-service | notification-service (`notification-service`) | `OrderStatusChanged` |

---

## `OrderCreated` (order → kitchen)

Bước 1 của Saga: kitchen hãy giữ suất cho đơn này.

```json
{
  "orderId": "40b8de2c-47ea-47f4-b38a-5b8a5b550ea9",
  "customerId": "ac908905-c663-4618-b6fc-7a69edfe6b12",
  "cookingDate": "2026-10-29",
  "totalQuantity": 2,
  "orderedAt": "2026-10-28T03:45:12.300Z",
  "items": [{ "menuItemId": "11111111-1111-1111-1111-111111111111", "quantity": 2 }]
}
```

`items` cho bếp biết các món của đơn, để cộng công thức thành nguyên liệu cần mua cho ngày nấu (bếp là nơi giữ công thức). Event cũ không có `items` thì bếp vẫn giữ suất bình thường, chỉ không tính được nguyên liệu.

`orderedAt` được gửi kèm để kitchen kiểm tra giờ chốt theo **thời điểm khách bấm đặt**, không theo thời điểm kitchen nhận event (UC02 - BR4).

## `OrderCancelled` (order → kitchen)

Khách hủy đơn, kitchen trả lại suất.

```json
{
  "orderId": "6700c8a4-e298-45c4-a51c-7bd062285b3e",
  "cookingDate": "2026-10-29",
  "reason": "CANCELLED_BY_CUSTOMER"
}
```

## `SlotReserved` (kitchen → order)

Đã giữ suất. order-service chuyển đơn sang `CONFIRMED`.

```json
{ "orderId": "40b8de2c-...", "cookingDate": "2026-10-29", "quantity": 2, "remainingSlots": 45 }
```

## `SlotFull` (kitchen → order)

Không giữ được suất. order-service **bù trừ**: chuyển đơn sang `CANCELLED`.

```json
{ "orderId": "8a3e...", "cookingDate": "2026-10-29", "requestedQuantity": 1, "remainingSlots": 0, "reason": "SOLD_OUT" }
```

`reason`: `SOLD_OUT` (hết suất) hoặc `CUTOFF_PASSED` (đặt sau 18:00 của ngày trước ngày nấu).

## `SlotReleased` (kitchen → order)

Thông tin: đã trả suất sau khi hủy. Hiện order-service bỏ qua event này. Nó được giữ lại để sau này có thể theo dõi.

```json
{ "orderId": "6700c8a4-...", "cookingDate": "2026-10-29", "quantity": 3, "remainingSlots": 47 }
```

## `OrderStatusChanged` (order → notification)

Phát ra mỗi khi đơn đổi trạng thái. Payload chứa đủ dữ liệu để notification-service gửi email mà **không phải gọi lại** order-service.

```json
{
  "orderId": "40b8de2c-47ea-47f4-b38a-5b8a5b550ea9",
  "customerId": "ac908905-c663-4618-b6fc-7a69edfe6b12",
  "customerEmail": "linh@test.vn",
  "customerName": "Linh",
  "previousStatus": "PENDING",
  "status": "CONFIRMED",
  "reason": null,
  "cookingDate": "2026-10-29",
  "deliveryWindow": "18:00-22:00",
  "deliveryAddress": "12 Nguyen Hue, Q1",
  "totalVnd": 90000,
  "items": [ { "name": "Pate gà bí đỏ", "quantity": 2, "unitPriceVnd": 45000 } ]
}
```

---

## Xem event thật

- **Kafka UI:** http://localhost:8090 → Topics → chọn topic → Messages
- **Outbox chưa gửi:** `SELECT * FROM outbox_events WHERE published_at IS NULL;` (trong `order_db` / `kitchen_db`)
