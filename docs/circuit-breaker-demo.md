# Demo Circuit Breaker bằng JMeter

> Yêu cầu đề bài: *"Cấu hình Circuit Breaker phù hợp để tái hiện trường hợp quá tải, dùng tool gửi 1 lượng lớn request khi các microservice làm việc với nhau và chứng minh Circuit Breaker có hoạt động."*

---

## 1. Kịch bản

```
JMeter (50 users) ──POST /api/orders──► api-gateway ──► order-service ──REST /api/kitchen/quotes──► kitchen-service
                                                         └─ CircuitBreaker "kitchen"                (chaos: chậm 3s)
```

| Giai đoạn | Thời gian (mặc định) | Kitchen | Breaker mong đợi | Khách nhận |
|---|---|---|---|---|
| 1. Khỏe | 0–10 s | trả lời ~50 ms | `CLOSED` | `202 Accepted` |
| 2. Chậm / quá tải | 10–35 s | trả lời sau **3 s** (chaos), order-service timeout sau **2 s** | `CLOSED → OPEN` sau khoảng 10 lời gọi lỗi; thử `HALF_OPEN → OPEN` vì kitchen vẫn chậm | `503 SERVICE_BUSY`, đầu tiên mất 2 s, sau đó **vài ms** |
| 3. Hồi phục | 35 s → hết | trả lời bình thường | `OPEN → HALF_OPEN → CLOSED` | `202 Accepted` trở lại |

## 2. Cấu hình Circuit Breaker

File `order-service/src/main/resources/application.yml`:

| Thuộc tính | Giá trị | Ý nghĩa |
|---|---|---|
| `sliding-window-type / size` | `COUNT_BASED / 20` | Đánh giá trên 20 lời gọi gần nhất |
| `minimum-number-of-calls` | `10` | Chưa đủ 10 lời gọi thì chưa kết luận |
| `failure-rate-threshold` | `50` | ≥ 50% lỗi thì OPEN |
| `slow-call-duration-threshold` | `1s` | Lời gọi > 1 s bị tính là chậm |
| `slow-call-rate-threshold` | `50` | ≥ 50% chậm cũng OPEN (bắt được trường hợp quá tải chứ chưa chết hẳn) |
| `wait-duration-in-open-state` | `10s` | OPEN 10 s rồi tự sang HALF_OPEN |
| `permitted-number-of-calls-in-half-open-state` | `5` | Cho 5 lời gọi thử |
| `record-exceptions` | timeout, connection refused, 5xx | Lỗi hạ tầng |
| `ignore-exceptions` | 4xx | Lỗi nghiệp vụ không làm mở breaker |
| HTTP client `read-timeout` | `2s` | Không bao giờ chờ quá 2 s |

## 3. Chạy demo

### Chuẩn bị
```bash
docker compose up -d          # Postgres, Kafka, Kafka UI, Mailpit
scripts/start-all.sh          # build + chạy 5 service
```

### Cách 1: JMeter GUI (dùng khi quay video)
```bash
jmeter -t jmeter/circuit-breaker-demo.jmx
```
1. Bấm ▶ **Start**.
2. Mở **Aggregate Report**: các dòng `Circuit breaker = CLOSED / OPEN / HALF_OPEN` cho thấy breaker đổi trạng thái theo thời gian. Dòng `POST /api/orders` cho thấy thời gian phản hồi.
3. Song song, mở `http://localhost:9002/actuator/circuitbreakers` hoặc xem log `.run/order-service.log`:
   ```
   Circuit breaker 'kitchen': CLOSED -> OPEN
   Circuit breaker 'kitchen': OPEN -> HALF_OPEN
   Circuit breaker 'kitchen': HALF_OPEN -> CLOSED
   ```

Test plan gồm các thread group sau:

| Thread group | Việc |
|---|---|
| `0 - setUp` | Đăng ký và đăng nhập khách, đăng nhập admin, nâng công suất ngày nấu lên 100.000 (để không bị hết suất), **warm-up 20 đơn**, reset breaker về CLOSED, tắt chaos |
| `1 - Chaos switch` | Sau `healthySeconds` (10 s) bật chậm 3 s; sau thêm `slowSeconds` (25 s) tắt |
| `2 - Customers` | `threads` (50) user, mỗi user đặt 1 hộp, nghỉ 500 ms, chạy trong `duration` (60 s) |
| `3 - Breaker monitor` | Mỗi giây đọc trạng thái breaker từ actuator, đặt tên sample theo trạng thái |
| `9 - tearDown` | Tắt chaos |

### Cách 2: JMeter CLI + báo cáo HTML
```bash
jmeter/run-cb-test.sh                                  # mặc định 50 user, 60 s
jmeter/run-cb-test.sh -Jthreads=200 -Jduration=90      # nhiều user hơn
# → jmeter/results/<timestamp>-report/index.html  (biểu đồ "Response Times Over Time")
```

### Cách 3: Trang web Hệ thống (dễ nhìn nhất khi quay video)
Đăng nhập admin → **Hệ thống** (`http://localhost:9000/#/admin/system`):
- Thẻ trạng thái breaker cập nhật mỗi giây (xanh CLOSED / đỏ OPEN / vàng HALF_OPEN), kèm tỉ lệ lỗi, tỉ lệ chậm, số lời gọi bị chặn và lịch sử chuyển trạng thái.
- Nút **🐢 Chậm 3s** / **💥 Lỗi 60%** / **✅ Tắt chaos**.
- **Gửi đơn**: bắn N đơn với M request song song ngay từ trình duyệt. Đo thật: 100 đơn khi kitchen chậm thì cả 100 trả 503 SERVICE_BUSY, breaker OPEN, 85 lời gọi bị chặn; tắt chaos thì OPEN → HALF_OPEN → CLOSED và 100 đơn đều 202.
- Trình duyệt chỉ mở khoảng 6 kết nối cùng lúc tới một host, nên đây là demo trực quan. Còn **tải lớn thì dùng JMeter** (yêu cầu của đề). Cách tốt nhất khi quay: chạy JMeter và để trang này mở bên cạnh.

### Cách 4: Không có JMeter
```bash
scripts/cb-demo.sh                       # kitchen chậm 3s
MODE=compare scripts/cb-demo.sh          # so sánh CÓ / KHÔNG có breaker
MODE=overload scripts/cb-demo.sh         # quá tải thật (xem mục 5)
```

> ⚠️ Groovy đi kèm JMeter 5.6.3 không chạy trên Java 24+. Test plan cố ý chỉ dùng hàm có sẵn của JMeter và BeanShell, nên chạy được với mọi phiên bản Java.

## 4. Kết quả đo thật (máy dev, 28/10/2026, 50 user, 60 s)

**Trạng thái breaker theo thời gian** (tính từ lúc bắt đầu tải):

| Thời điểm | Trạng thái | Ghi chú |
|---|---|---|
| t+0,5 s | `CLOSED` | Kitchen khỏe |
| t+12,7 s | `OPEN` | Chaos bật ở t+10 s, khoảng 30 lời gọi timeout 2 s thì breaker mở |
| t+21,7 s | `HALF_OPEN` | Hết 10 s chờ |
| t+24,8 s | `OPEN` | Lời gọi thử vẫn chậm (kitchen chưa hồi phục) |
| t+33,8 s → t+35,8 s | `HALF_OPEN` → `OPEN` | Chaos vừa tắt ở t+35 s, lời gọi thử đang chạy dở vẫn chậm |
| t+45,9 s | `HALF_OPEN` | |
| t+46,9 s | `CLOSED` | 5 lời gọi thử thành công |

**Đơn hàng theo từng khoảng 5 giây:**

| Khoảng | HTTP | Số request | Thời gian TB |
|---|---|---|---|
| 0–5 s | 202 | 127 | 70 ms |
| 5–10 s | 202 / 503 | 222 / 32 | 298 ms / **2.016 ms** (timeout) |
| 10–15 s | 503 | 297 | 129 ms |
| 15–45 s | 503 | ~2.800 | **6–44 ms** (breaker OPEN, kitchen không bị gọi) |
| 45–50 s | 202 / 503 | 166 / 117 | 800 ms / 40 ms |
| 50–60 s | 202 | 408 | 300–1.100 ms (xử lý phần tồn đọng) |

**So sánh có / không có breaker** (`MODE=compare scripts/cb-demo.sh`, 90 request × 30 song song, kitchen chậm 3 s):

| | Thời gian phản hồi TB | Điều gì xảy ra |
|---|---|---|
| Breaker **DISABLED** | **2.031 ms** | Mọi request chờ đủ 2 s timeout, thread của order-service bị chặn suốt thời gian đó |
| Breaker **bật** | **752 ms** (và giảm dần về vài ms khi tải kéo dài) | Khoảng 10–30 lời gọi đầu chờ 2 s, sau đó breaker mở và các request còn lại bị từ chối ngay |

## 5. Quá tải "thật" (không cần độ trễ giả 3 s)

```bash
scripts/stop-all.sh && KITCHEN_MAX_THREADS=5 scripts/start-all.sh
MODE=overload scripts/cb-demo.sh       # chaos chỉ 300 ms = thời gian "nấu" một request
```

Kitchen chỉ có 5 thread Tomcat. 30 request song song phải xếp hàng, thời gian chờ vượt 1 s (slow call) rồi 2 s (timeout). Kết quả đo được: 22 đơn thành công nhưng chậm (TB 1.127 ms, tối đa 2.430 ms), sau đó breaker **OPEN** và 128 request còn lại bị từ chối nhanh (TB 374 ms). Đây đúng là trường hợp *quá tải* mà đề bài yêu cầu.

## 6. Nói gì trong video (khoảng 2 phút)

1. *"order-service gọi kitchen-service qua REST để lấy số suất còn lại. Nếu kitchen chậm, thread của order-service bị chặn, request dồn lại, và order-service sập theo. Đó là lỗi dây chuyền."*
2. Chỉ vào cấu hình: cửa sổ 20, ngưỡng 50%, slow call 1 s, chờ 10 s; timeout 2 s đặt ở HTTP client.
3. Chạy JMeter, chỉ vào giai đoạn CLOSED với các response 202.
4. Chaos bật: vài request 2 s, rồi **OPEN**, response 503 chỉ còn vài ms. *"Lúc này order-service không gọi kitchen nữa, nên kitchen có thời gian hồi phục."*
5. HALF_OPEN → OPEN: *"breaker thử lại, kitchen vẫn chậm nên mở lại"*. Sau khi chaos tắt: HALF_OPEN → **CLOSED**.
6. Kết luận: *"Không có breaker thì mọi request đều chờ 2 s (bảng so sánh). Có breaker thì hệ thống báo bận ngay và tự phục hồi."*
