# Use Cases — Pameobeo

> Mô tả hệ thống làm **gì** dưới góc nhìn người dùng, chưa bàn tới công nghệ.
> Viết xong thì gạch chân các **chỗ khó**, rồi đưa vào bảng [Chỗ khó → Khái niệm](#chỗ-khó--khái-niệm) ở cuối file.

---

## Actors

| Actor | Mô tả |
|---|---|
| **Guest** | Khách chưa đăng nhập |
| **Customer** | Khách đã đăng nhập, đặt pate cho mèo |
| **Admin** | Chủ shop, quản lý menu, ngày nấu, đơn hàng |
| **System** | Các tác vụ tự động (job, xử lý event, gửi email) |

---

## Danh sách Use Case

| ID | Tên | Actor | Service liên quan | Ưu tiên |
|---|---|---|---|---|
| UC01 | Đăng ký / Đăng nhập | Guest | auth | ⭐ |
| UC02 | Đặt hàng | Customer | order, kitchen, notification | ⭐ |
| UC03 | Hủy đơn | Customer | order, kitchen, notification | ⭐ |
| UC04 | Chốt đơn, nấu và giao hàng | System, Admin | order, notification | 👍 |
| UC05 | Quản lý ngày nấu và menu | Admin | kitchen | 👍 |
| UC06 | Gửi thông báo email | System | notification | ⭐ |

> 💡 Ưu tiên: ⭐ bắt buộc cho bài tập · 👍 nên có · 💤 để sau 6/11
>
> ✅ Trong phần **Tiêu chí chấp nhận**: `[x]` = đã kiểm chứng bằng test tự động (`mvn verify`) hoặc bằng `scripts/demo.sh` / `scripts/cb-demo.sh`; `[ ]` = logic đã có nhưng chưa có test riêng.

---

## Template

> Copy khối dưới đây cho mỗi Use Case mới.

```markdown
## UCxx. <Tên use case>

**Actor:** <Ai làm?>
**Mục tiêu:** <Muốn đạt được gì?>
**Service liên quan:** <auth / order / kitchen / notification>

**Điều kiện trước:**
- <Cần gì trước khi bắt đầu? Ví dụ: đã đăng nhập>

**Luồng chính:**
1. <Actor làm gì>
2. <Hệ thống phản hồi thế nào>
3. ...

**Trường hợp lỗi / luồng phụ:**
- **Ex1 — <tên>:** <xảy ra khi nào> → <hệ thống xử lý ra sao>
- **Ex2 — <tên>:** ...

**Luật nghiệp vụ:**
- **BR1:** <quy tắc bất biến, ví dụ: tối đa 50 hộp mỗi ngày nấu>
- **BR2:** ...

**Kết quả sau cùng:**
- <Dữ liệu / trạng thái thay đổi thế nào>

**Tiêu chí chấp nhận (dùng để viết test):**
- [ ] Given <bối cảnh>, When <hành động>, Then <kết quả mong đợi>
- [ ] ...

**Chỗ khó:** <ghi lại phần nào khó, sẽ đưa vào bảng cuối file>
```

---

## UC01. Đăng ký / Đăng nhập

> ✍️ Ví dụ mẫu đã điền sẵn để tham khảo cách viết.

**Actor:** Guest
**Mục tiêu:** Có tài khoản và đăng nhập để đặt hàng
**Service liên quan:** auth

**Điều kiện trước:**
- Chưa đăng nhập

**Luồng chính:**
1. Guest nhập email, mật khẩu, tên để đăng ký
2. Hệ thống tạo tài khoản với vai trò `CUSTOMER`
3. Guest đăng nhập bằng email và mật khẩu
4. Hệ thống trả về **access token** (JWT, sống 15 phút) và **refresh token** (lưu trong DB)
5. Các request sau gửi kèm access token để gọi API qua gateway
6. Access token hết hạn → client gửi refresh token để lấy cặp token mới, người dùng không phải đăng nhập lại

**Trường hợp lỗi / luồng phụ:**
- **Ex1 — Email đã tồn tại:** → báo lỗi `EMAIL_ALREADY_EXISTS`, không tạo tài khoản
- **Ex2 — Sai email hoặc mật khẩu:** → trả `401`, không nói rõ sai email hay sai mật khẩu
- **Ex3 — Access token hết hạn:** → gateway trả `401`, client dùng refresh token để lấy token mới (bước 6)
- **Ex4 — Customer gọi API của Admin:** → trả `403`
- **Ex5 — Spam đăng nhập (brute force):** sai mật khẩu **5 lần liên tiếp** → khóa đăng nhập tài khoản đó **15 phút**, trả lỗi `ACCOUNT_TEMPORARILY_LOCKED`. Trong thời gian khóa, nhập đúng mật khẩu cũng bị từ chối
- **Ex6 — Treo máy quá lâu (idle timeout):** không có request nào trong **30 phút** → refresh token hết hạn → lần refresh tiếp theo trả `401`, bắt đăng nhập lại
- **Ex7 — Đăng nhập quá lâu (absolute timeout):** dù vẫn hoạt động liên tục, sau **7 ngày** kể từ lần đăng nhập đầu cũng phải đăng nhập lại

**Luật nghiệp vụ:**
- **BR1:** Email là duy nhất
- **BR2:** Mật khẩu tối thiểu 8 ký tự, lưu dạng hash (BCrypt), không bao giờ lưu plain text
- **BR3:** Tài khoản đăng ký mới luôn là `CUSTOMER`. `ADMIN` được tạo sẵn bằng seed data
- **BR4:** Sai mật khẩu 5 lần liên tiếp → khóa 15 phút. Đăng nhập thành công thì reset bộ đếm về 0
- **BR5:** Thời gian sống của token:

  | Loại | Thời gian | Ý nghĩa |
  |---|---|---|
  | Access token | 15 phút | Ngắn, để token bị lộ cũng nhanh vô dụng |
  | Refresh token — idle | 30 phút | Không hoạt động quá 30 phút → hết session |
  | Refresh token — absolute | 7 ngày | Tối đa 7 ngày phải đăng nhập lại |

- **BR6:** Mỗi lần refresh thì cấp **refresh token mới** và vô hiệu hóa token cũ (refresh token rotation). Thời hạn idle được tính lại từ lúc refresh

> 💡 **Vì sao cần refresh token để làm idle timeout?**
> JWT là **stateless**: server không lưu session nên không biết user "đang treo" hay không. Access token chỉ có hạn cố định, không tự gia hạn được.
> → Thời gian treo được đo qua **refresh token** (lưu trong DB): mỗi lần client hoạt động và refresh thì gia hạn thêm 30 phút. Treo quá 30 phút thì refresh token hết hạn, session kết thúc.

> ⚠️ Các con số (5 lần, 15 phút, 30 phút, 7 ngày) là giá trị đề xuất, nên để trong config (`application.yml`) để dễ chỉnh khi demo.

**Kết quả sau cùng:**
- Bảng `users` có thêm 1 bản ghi (đăng ký)
- Bảng `refresh_tokens` có thêm 1 bản ghi (đăng nhập)
- Client giữ access token + refresh token

**Tiêu chí chấp nhận (dùng để viết test):**
- [x] Given email chưa tồn tại, When đăng ký, Then tạo user với role `CUSTOMER`
- [ ] Given email đã tồn tại, When đăng ký, Then trả lỗi `EMAIL_ALREADY_EXISTS`
- [x] Given đúng email/mật khẩu, When đăng nhập, Then nhận access token + refresh token
- [x] Given không có token, When gọi `POST /orders` qua gateway, Then trả `401`
- [x] Given token `CUSTOMER`, When gọi API admin, Then trả `403`
- [x] Given đã sai mật khẩu 4 lần, When sai lần thứ 5, Then tài khoản bị khóa 15 phút
- [x] Given tài khoản đang bị khóa, When nhập đúng mật khẩu, Then vẫn trả `ACCOUNT_TEMPORARILY_LOCKED`
- [x] Given đã sai 3 lần, When đăng nhập đúng, Then bộ đếm reset về 0
- [x] Given refresh token còn hạn, When refresh, Then nhận cặp token mới và token cũ không dùng lại được
- [x] Given không hoạt động 30 phút, When refresh, Then trả `401`
- [x] Given đã đăng nhập 7 ngày, When refresh, Then trả `401` dù vẫn đang hoạt động

**Chỗ khó:**
- Gateway kiểm tra token thế nào mà không cần gọi auth-service mỗi request?
- JWT là stateless, vậy làm sao biết user treo bao lâu để hết session?
- Chống spam đăng nhập: đếm số lần sai ở đâu, khi auth-service chạy nhiều instance?

---

## UC02. Khách hàng đặt hàng

**Actor:** Customer
**Mục tiêu:** Đặt pate để nhận vào tối ngày hôm sau
**Service liên quan:** order, kitchen, notification

**Mốc thời gian:**
```
            Day D                              Day D+1
──────────────────────────────┬─────────────────────────────┬──────────────────►
  ... ordering ...            │ buy ingredients + cook      │ 18:00–22:00
                       18:00  │ + cool down + pack          │ delivery
                       cut-off│ (18:00 D → 18:00 D+1)       │
```

**Điều kiện trước:**
- Đã đăng nhập
- Có ít nhất 1 địa chỉ giao hàng

**Luồng chính:**
1. Khách xem menu và **số suất còn lại** của ngày nấu kế tiếp
2. Khách chọn **loại pate** (1 vị có gan, 1 vị không gan, mix 2 vị có gan, mix 2 vị không gan), chọn **vị** (1 vị, hoặc 2 vị khác nhau nếu là mix), số lượng và thêm vào giỏ hàng
3. Khách mở giỏ hàng, chọn địa chỉ giao, bấm **Đặt hàng**
4. Hệ thống kiểm tra: còn trước giờ chốt không, số lượng có hợp lệ không, ngày nấu còn đủ suất không
5. Hệ thống tạo đơn ở trạng thái `PENDING`, lưu lại giá tại thời điểm đặt. Màn hình hiện *"Đơn hàng đang được xử lý"*
6. Bếp **giữ suất** cho đơn ở phía sau (bất đồng bộ)
7. Giữ suất thành công → đơn chuyển `CONFIRMED`
8. Khách nhận email xác nhận, trong email có ngày và giờ giao (18:00–22:00 ngày D+1)

**Trường hợp lỗi / luồng phụ:**
- **Ex1 — Hết suất khi bấm đặt:** Khách để hàng trong giỏ chưa đặt, trong lúc đó người khác đặt hết suất → ở bước 4 hệ thống báo *"Ngày nấu này đã hết suất, bạn có muốn đặt cho ngày nấu tiếp theo không?"*. Không tạo đơn
- **Ex2 — Quá giờ chốt đơn:** Khách bấm đặt sau 18:00 → hệ thống báo *"Đã qua giờ chốt đơn, đơn sẽ được nấu và giao vào ngày DD/MM. Bạn có đồng ý không?"* → khách đồng ý thì đơn được gán cho ngày nấu kế tiếp
- **Ex3 — Tranh suất cuối:** Ở bước 4 vẫn còn suất, nhưng tới bước 6 thì người khác đã lấy mất → đơn chuyển `CANCELLED`, khách nhận email *"Rất tiếc, ngày nấu đã đủ suất"*
- **Ex4 — Đặt nhiều hơn số suất còn lại:** Muốn đặt 5 hộp nhưng chỉ còn 3 → từ chối cả đơn, báo *"Chỉ còn 3 suất"*. Không tự cắt bớt số lượng
- **Ex5 — Hệ thống bếp chậm hoặc không phản hồi:** Ở bước 4 không kiểm tra được suất → báo ngay *"Hệ thống đang bận, vui lòng thử lại sau ít phút"*. Không tạo đơn, không bắt khách chờ lâu
- **Ex6 — Bấm đặt hàng 2 lần liên tiếp:** Do mạng chậm hoặc bấm đúp → chỉ tạo **1 đơn**
- **Ex7 — Số lượng không hợp lệ:** 0 hộp, số âm, hoặc quá giới hạn mỗi đơn → báo lỗi ngay, không tạo đơn
- **Ex8 — Món đã ngừng bán:** Món trong giỏ bị admin tắt trước khi khách bấm đặt → báo *"Món X hiện không còn bán"*

**Luật nghiệp vụ:**
- **BR1:** Mỗi ngày nấu tối đa **50 hộp**, tính chung cho mọi loại pate. Admin chỉnh được công suất theo từng ngày
- **BR2:** Giờ chốt đơn là **18:00** mỗi ngày (giờ Việt Nam). Đơn đặt trước 18:00 ngày D được chuẩn bị và nấu từ 18:00 ngày D đến 18:00 ngày D+1, rồi giao vào **18:00–22:00 ngày D+1**
- **BR3:** Đơn đặt sau 18:00 ngày D được gán cho ngày nấu kế tiếp (D+2)
- **BR4:** Thời điểm tính giờ chốt là **lúc khách bấm đặt** (`createdAt` của đơn), không phải lúc bếp xử lý
- **BR5:** **Giỏ hàng không giữ suất.** Chỉ khi bấm đặt hàng và bếp xác nhận thì suất mới bị trừ
- **BR6:** Giá được **chụp lại lúc đặt hàng**. Admin đổi giá sau đó thì đơn đã đặt không bị ảnh hưởng
- **BR7:** Mỗi đơn tối đa **10 hộp** _(đề xuất, có thể chỉnh)_
- **BR8:** Khách chỉ được hủy đơn **trước 18:00 ngày D** (trước giờ chốt). Hủy thì trả lại suất cho ngày nấu
- **BR9:** Có 4 vị: **gà, cá ngừ, bò, cá hồi**. Mỗi tổ hợp (loại × vị) là một món có **giá riêng** (bảng giá đầy đủ: 4 vị × 2 + 6 cặp mix × 2 = **20 món**), admin bật/tắt từng món
- **BR10:** Mỗi hộp **400g**, luôn có 50g cà rốt + 50g bí đỏ. Công thức phần còn lại:

  | Loại | Thịt | Gan |
  |---|---|---|
  | 1 vị có gan | 200g | 100g |
  | 1 vị không gan | 300g (gan thay bằng thịt) | — |
  | Mix 2 vị có gan | 100g vị này + 100g vị kia | 100g |
  | Mix 2 vị không gan | 150g vị này + 150g vị kia | — |

- **BR11:** Khách chỉ thấy **tên** nguyên liệu, không thấy số lượng. Số gram của công thức chỉ admin thấy
- **BR12:** Mỗi món hiện **số hộp đã bán thực tế trong 30 ngày** (đơn đã được bếp giữ suất, không tính đơn hủy). Đầu trang có carousel tối đa **5 món bán chạy nhất**; nếu chưa đủ 5 món có lượt bán thì bù bằng các món admin chọn làm **"Gợi ý của tiệm"**
- **BR13:** Mỗi sản phẩm có **tối đa 3 ảnh** do admin tải lên (JPG / PNG / WebP, tối đa 2 MB; hệ thống kiểm tra nội dung thật của file, không tin đuôi file). Ảnh đầu tiên là **ảnh bìa**; khách bấm vào ảnh để xem phóng to tất cả ảnh. Chưa có ảnh thì hiện biểu tượng của vị


**Luật thanh toán — 💤 để sau 6/11:**
- **BR-P1:** Đơn lớn (từ 10 hộp) cần cọc 50% giá trị đơn
- **BR-P2:** Khách đặt lần đầu cần thanh toán 100%
- **BR-P3:** Từ lần đặt thứ 2, khách được chọn thanh toán khi nhận hàng (COD)

> Trong bài tập, đơn hàng chưa có bước thanh toán. Đơn đi thẳng từ `PENDING` sang `CONFIRMED` / `CANCELLED`.

**Kết quả sau cùng:**
- Đơn được tạo với trạng thái `CONFIRMED` (hoặc `CANCELLED` nếu hết suất ở Ex3)
- Số suất đã nhận của ngày nấu tăng đúng bằng số hộp trong đơn
- Khách nhận email xác nhận hoặc email hủy

**Vòng đời của đơn (để tham khảo, các bước sau thuộc use case khác):**

| Trạng thái | Khi nào | Ai chuyển | Use case |
|---|---|---|---|
| `PENDING` | Vừa bấm đặt | Customer | UC02 |
| `CONFIRMED` / `CANCELLED` | Bếp giữ được suất hay không | System | UC02 |
| `CANCELLED` | Khách tự hủy trước 18:00 ngày D | Customer | _UC hủy đơn_ |
| `COOKING` | 18:00 ngày D, ngay sau khi chốt đơn | Admin / System | _UC chốt đơn & nấu_ |
| `DELIVERING` | Bắt đầu đi giao | Admin | _UC giao hàng_ |
| `COMPLETED` | Giao xong | Admin | _UC giao hàng_ |

**Tiêu chí chấp nhận (dùng để viết test):**
- [x] Given ngày nấu còn 10 suất và trước 18:00, When đặt 3 hộp, Then đơn `PENDING` rồi chuyển `CONFIRMED`, số suất còn lại là 7
- [ ] Given ngày nấu đã đủ 50 hộp, When đặt hàng, Then báo hết suất và không tạo đơn
- [x] Given đặt lúc 18:01 ngày D, When đặt hàng, Then đơn được gán cho ngày nấu D+2
- [x] Given khách bấm đặt lúc 17:59:59 nhưng bếp xử lý lúc 18:00:02, When bếp giữ suất, Then đơn vẫn được nhận cho ngày D+1
- [x] Given còn 3 suất, When đặt 5 hộp, Then từ chối cả đơn
- [x] Given còn 1 suất và 2 khách cùng đặt, When cả hai bấm đặt cùng lúc, Then chỉ 1 đơn `CONFIRMED`, đơn còn lại `CANCELLED`
- [ ] Given còn 3 suất và 1.000 request đặt cùng lúc, When xử lý xong, Then tổng số hộp đã nhận không vượt 50
- [x] Given bếp không phản hồi, When đặt hàng, Then trả lỗi "Hệ thống đang bận" trong vòng 2 giây, không tạo đơn
- [x] Given khách bấm đặt 2 lần với cùng một yêu cầu, When xử lý, Then chỉ có 1 đơn
- [ ] Given admin đổi giá sau khi khách đặt, When xem lại đơn, Then đơn vẫn giữ giá cũ
- [x] Given đặt 0 hộp hoặc 11 hộp, When đặt hàng, Then báo lỗi số lượng không hợp lệ

**Chỗ khó:**
- Hai người cùng tranh suất cuối thì làm sao không nhận quá 50 hộp? (Ex3, BR1)
- Đơn tạo ở order-service nhưng suất nằm ở kitchen-service. Hết suất sau khi đơn đã tạo thì hủy đơn thế nào? (Ex3)
- Bếp chậm thì làm sao không kéo sập luôn chức năng đặt hàng? (Ex5)
- Bấm đặt 2 lần, hoặc message tới bếp 2 lần, thì làm sao chỉ giữ suất 1 lần? (Ex6)
- Đặt sát giờ chốt nhưng bếp xử lý sau giờ chốt thì tính thế nào? (BR4)
- Vì sao đơn phải đi qua trạng thái `PENDING` mà không xác nhận ngay?

---

## UC03. Khách hàng hủy đơn

**Actor:** Customer
**Mục tiêu:** Hủy đơn đã đặt khi đổi ý
**Service liên quan:** order, kitchen, notification

**Điều kiện trước:**
- Đã đăng nhập, là chủ của đơn hàng
- Đơn đang ở trạng thái `PENDING` hoặc `CONFIRMED`

**Luồng chính:**
1. Khách mở danh sách đơn của mình, chọn một đơn, bấm **Hủy đơn**
2. Hệ thống kiểm tra: đơn thuộc về khách này, trạng thái cho phép hủy, còn trước giờ chốt (18:00 ngày D)
3. Đơn chuyển sang `CANCELLED` với lý do `CANCELLED_BY_CUSTOMER`
4. Bếp nhận event `OrderCancelled` và **trả lại suất** cho ngày nấu (bất đồng bộ)
5. Khách nhận email xác nhận đã hủy

**Trường hợp lỗi / luồng phụ:**
- **Ex1 — Quá giờ chốt:** Hủy sau 18:00 ngày D → báo *"Đã qua giờ chốt, bếp đang chuẩn bị đơn này"*, không hủy được (`CANCELLATION_NOT_ALLOWED`)
- **Ex2 — Đơn đã nấu / đang giao / đã giao / đã hủy:** → báo không thể hủy ở trạng thái hiện tại
- **Ex3 — Hủy đơn của người khác:** → `403 ORDER_ACCESS_DENIED`
- **Ex4 — Hủy khi đơn còn `PENDING`, bếp trả lời sau:** Bếp vẫn có thể gửi `SlotReserved` sau khi đơn đã hủy → order bỏ qua (đơn không "sống lại"). Bếp sẽ nhận `OrderCancelled` ngay sau đó (cùng key `orderId` nên đúng thứ tự) và trả suất
- **Ex5 — Event `OrderCancelled` tới bếp 2 lần:** Chỉ trả suất **1 lần**

**Luật nghiệp vụ:**
- **BR1:** Chỉ hủy được trước 18:00 ngày D (UC02 - BR8)
- **BR2:** Hủy thì trả đúng số hộp đơn đó đã giữ, không bao giờ trả 2 lần
- **BR3:** Đơn đã hủy không thể chuyển sang trạng thái nào khác

**Kết quả sau cùng:**
- Đơn ở `CANCELLED`
- Số suất đã nhận của ngày nấu giảm đúng bằng số hộp của đơn

**Tiêu chí chấp nhận (dùng để viết test):**
- [x] Given đơn `CONFIRMED` 3 hộp, trước 18:00, When khách hủy, Then đơn `CANCELLED` và ngày nấu được trả lại 3 suất
- [x] Given đã qua 18:00 ngày D, When khách hủy, Then trả `CANCELLATION_NOT_ALLOWED`
- [x] Given đơn đã `CANCELLED`, When bếp gửi `SlotReserved` muộn, Then đơn vẫn `CANCELLED`
- [ ] Given đơn của khách A, When khách B hủy, Then trả `403`
- [ ] Given `OrderCancelled` bị gửi 2 lần, When bếp xử lý, Then chỉ trả suất 1 lần

**Chỗ khó:**
- Khách hủy đúng lúc bếp đang giữ suất (2 event "đua" nhau)
- Trả suất 2 lần nếu message bị gửi lặp

---

## UC04. Chốt đơn, nấu và giao hàng

**Actor:** System (job lúc 18:00), Admin
**Mục tiêu:** Đóng nhận đơn cho ngày nấu, theo dõi đơn tới khi giao xong
**Service liên quan:** order, kitchen, notification

**Điều kiện trước:**
- Có đơn `CONFIRMED` cho ngày nấu D+1

**Luồng chính:**
1. **18:00 ngày D**, job tự động chạy: mọi đơn `CONFIRMED` của ngày nấu D+1 chuyển sang `COOKING`
2. Từ 18:00 bếp không nhận thêm đơn cho ngày D+1 (đơn mới tự sang D+2, xem UC02 - BR3)
3. Admin xem danh sách đơn theo ngày nấu để biết nấu bao nhiêu hũ mỗi vị
4. **18:00 ngày D+1**, admin bắt đầu đi giao → chuyển từng đơn sang `DELIVERING` → khách nhận email "đang giao"
5. Giao xong → admin chuyển `COMPLETED` → khách nhận email cảm ơn

**Trường hợp lỗi / luồng phụ:**
- **Ex1 — Job chạy 2 lần (restart, nhiều instance):** chỉ đơn `CONFIRMED` mới được chuyển → chạy lại không gây sai
- **Ex2 — Demo / chạy tay:** admin gọi `POST /api/orders/admin/cooking-days/{date}/start-cooking` thay vì chờ 18:00
- **Ex3 — Chuyển sai thứ tự** (ví dụ `CONFIRMED` → `DELIVERING`): bị từ chối `INVALID_STATUS_TRANSITION`
- **Ex4 — Đơn còn `PENDING` lúc 18:00** (bếp chưa kịp trả lời): không chuyển sang `COOKING`; khi bếp trả lời thì đi tiếp theo UC02

**Luật nghiệp vụ:**
- **BR1:** Thứ tự bắt buộc: `CONFIRMED → COOKING → DELIVERING → COMPLETED`
- **BR2:** Admin chỉ được đặt `DELIVERING` hoặc `COMPLETED`, không tự đặt `CONFIRMED`/`CANCELLED`
- **BR3:** Giờ giao cố định 18:00–22:00 ngày D+1

**Kết quả sau cùng:**
- Đơn đi hết vòng đời tới `COMPLETED`, khách nhận email ở các mốc quan trọng

**Tiêu chí chấp nhận (dùng để viết test):**
- [ ] Given 5 đơn `CONFIRMED` cho ngày 29/10, When job chốt đơn chạy, Then cả 5 đơn sang `COOKING`
- [ ] Given đơn `CONFIRMED`, When admin chuyển thẳng sang `DELIVERING`, Then bị từ chối
- [x] Given đơn đi `COOKING → DELIVERING → COMPLETED`, Then mỗi bước hợp lệ
- [ ] Given job đã chạy, When chạy lại, Then không đơn nào bị đổi thêm

**Chỗ khó:**
- Job định kỳ chạy trên nhiều instance cùng lúc
- Không cho chuyển trạng thái sai thứ tự

---

## UC05. Admin quản lý ngày nấu và menu

**Actor:** Admin
**Mục tiêu:** Điều chỉnh công suất từng ngày, giá và món đang bán
**Service liên quan:** kitchen

**Điều kiện trước:**
- Đăng nhập với vai trò `ADMIN`

**Luồng chính:**
1. Admin xem số suất của một ngày nấu: công suất, đã nhận, còn lại
2. Admin đổi công suất của ngày đó (ví dụ hôm nay chỉ nấu được 30 hộp)
3. Admin đổi giá hoặc tạm ngừng bán một món trong bảng giá (vị × có gan / không gan)
4. Admin tải **ảnh sản phẩm** (tối đa 3 ảnh mỗi món), chọn ảnh bìa, xóa ảnh
5. Admin đánh dấu ★ các món làm **"Gợi ý của tiệm"** (hiện ở đầu trang khi chưa đủ dữ liệu bán chạy)
6. Admin xem **nguyên liệu cần chuẩn bị** cho một ngày nấu: số hộp theo từng món và tổng số gram mỗi nguyên liệu (cộng công thức của các hộp đã được giữ suất)

**Trường hợp lỗi / luồng phụ:**
- **Ex1 — Giảm công suất thấp hơn số đã nhận:** đã nhận 40 hộp, đặt công suất 30 → từ chối `INVALID_CAPACITY`
- **Ex2 — Customer gọi API admin:** → `403`
- **Ex3 — Ngày nấu chưa có đơn nào:** hệ thống tự mở ngày đó với công suất mặc định 50

**Luật nghiệp vụ:**
- **BR1:** Công suất mặc định 50 hộp/ngày, admin chỉnh được theo từng ngày
- **BR2:** Công suất không được nhỏ hơn số hộp đã nhận
- **BR3:** Đổi giá không ảnh hưởng đơn đã đặt (đơn giữ giá snapshot - UC02 - BR6)
- **BR4:** Món bị tắt không đặt được nữa (UC02 - Ex8), đơn cũ vẫn giữ nguyên
- **BR5:** Nguyên liệu chỉ tính các hộp **đang giữ suất**; đơn bị hủy (suất đã trả) không tính

**Kết quả sau cùng:**
- `cooking_days` / `menu_items` được cập nhật

**Tiêu chí chấp nhận (dùng để viết test):**
- [x] Given đã nhận 30 hộp, When đặt công suất 20, Then `INVALID_CAPACITY`
- [x] Given đã nhận 30 hộp, When đặt công suất 40, Then còn lại 10
- [ ] Given admin tắt món X, When khách đặt món X, Then `MENU_ITEM_UNAVAILABLE`

**Chỗ khó:**
- Admin giảm công suất đúng lúc khách đang đặt (cùng sửa một `CookingDay`) → optimistic locking

---

## UC06. Gửi thông báo email

**Actor:** System
**Mục tiêu:** Khách (và shop) biết ngay khi đơn đổi trạng thái, mà không làm chậm API đặt hàng
**Service liên quan:** notification (nhận event từ order)

**Điều kiện trước:**
- order-service phát event `OrderStatusChanged`

**Luồng chính:**
1. order-service đổi trạng thái đơn và ghi `OrderStatusChanged` vào outbox (cùng transaction)
2. Relay đẩy event lên Kafka topic `order.status-changed`
3. notification-service nhận event, quyết định gửi email cho ai theo trạng thái
4. Các email của cùng một event (khách + shop) được gửi **song song**, mỗi email trên một **virtual thread** (Java 21)
5. Ghi log đã gửi (kèm tên thread)

| Trạng thái mới | Gửi cho |
|---|---|
| `CONFIRMED` | Khách + shop |
| `CANCELLED` | Khách (kèm lý do) + shop |
| `DELIVERING` | Khách |
| `COMPLETED` | Khách |
| `PENDING`, `COOKING` | Không gửi |

**Trường hợp lỗi / luồng phụ:**
- **Ex1 — Mail server chết:** Kafka retry 5 lần, mỗi lần cách 2 giây; vẫn lỗi thì chuyển message sang topic `order.status-changed.DLT` để xử lý sau. API đặt hàng **không bị ảnh hưởng**
- **Ex2 — Event bị gửi 2 lần:** chỉ gửi email 1 lần (bảng `processed_events`)
- **Ex3 — notification-service tắt một lúc:** event nằm chờ trong Kafka, bật lại thì gửi tiếp, không mất email

**Luật nghiệp vụ:**
- **BR1:** Gửi email không bao giờ chặn luồng đặt hàng (xử lý bất đồng bộ)
- **BR2:** Mỗi event chỉ tạo email đúng 1 lần

**Kết quả sau cùng:**
- Email nằm trong hộp thư (môi trường dev: Mailpit http://localhost:8025)
- Bảng `notification_logs` có bản ghi, cột `thread` cho thấy `VirtualThread[...]`

**Tiêu chí chấp nhận (dùng để viết test):**
- [x] Given đơn `CONFIRMED`, When nhận event, Then gửi 2 email (khách + shop) trên virtual thread
- [x] Given cùng một event tới 2 lần, Then chỉ gửi 1 lần
- [x] Given đơn `CANCELLED` vì `SOLD_OUT`, Then email ghi lý do "ngày nấu đã đủ suất"
- [x] Given đơn `COOKING`, Then không gửi email

**Chỗ khó:**
- Gửi email chậm (I/O) không được làm chậm hệ thống
- Không gửi trùng email khi Kafka giao lại message

---

## Chỗ khó → Khái niệm

> Gom các "chỗ khó" từ những use case trên, rồi ghép với khái niệm / pattern sẽ dùng để giải quyết.
> Bảng này là cầu nối từ nghiệp vụ sang kiến trúc, và cũng là phần nên trình bày trong video.

| # | Chỗ khó (từ UC nào) | Khái niệm / Pattern giải quyết | Tiêu chí đề bài liên quan |
|---|---|---|---|
| 1 | Gateway kiểm tra token mà không gọi auth-service (UC01) | JWT RS256 + JWKS | Security service |
| 2 | Hết session khi user treo quá 30 phút, dù JWT là stateless (UC01) | Access token ngắn + Refresh token (idle / absolute timeout, rotation) | Security service |
| 3 | Chống spam đăng nhập khi auth-service có nhiều instance (UC01) | Đếm số lần sai trong DB (`failed_attempts`, `locked_until`). Sau 6/11 có thể chuyển sang Redis + rate limiting ở gateway | Security service |
| 4 | Hai người tranh suất cuối, không được nhận quá 50 hộp (UC02) | Optimistic locking (`@Version`) trên `CookingDay` ở kitchen-service | DDD (aggregate giữ luật bất biến) |
| 5 | Đơn ở order-service, suất ở kitchen-service, hết suất thì hủy đơn (UC02) | Saga choreography + Compensating action (`OrderCreated` → `SlotReserved` / `SlotFull`) + Eventual consistency | Saga |
| 6 | Tạo đơn và gửi event phải cùng thành công hoặc cùng thất bại (UC02) | Transactional Outbox | Saga |
| 7 | Bếp chậm không được kéo sập chức năng đặt hàng (UC02) | Circuit Breaker + Timeout + Fallback (Resilience4j) | Circuit Breaker |
| 8 | Bấm đặt 2 lần / message tới 2 lần nhưng chỉ giữ suất 1 lần (UC02) | Idempotency key khi tạo đơn + Idempotent Consumer (`processed_events`) | Saga |
| 9 | Đặt sát giờ chốt nhưng bếp xử lý sau giờ chốt (UC02) | Tính theo `createdAt` của đơn, gửi kèm trong event | Saga |
| 10 | Khách hủy đúng lúc bếp đang giữ suất (UC03) | Kafka key = `orderId` giữ thứ tự `OrderCreated` → `OrderCancelled`; order bỏ qua `SlotReserved` tới muộn | Saga |
| 11 | Trả suất / gửi email 2 lần khi message bị giao lặp (UC03, UC06) | Idempotent Consumer (`processed_events`) + `SlotReservation.release()` chỉ trả 1 lần | Saga |
| 12 | Job 18:00 chạy trên nhiều instance (UC04) | Job idempotent (chỉ lấy đơn `CONFIRMED`). Sau 6/11: distributed lock (Redis/ShedLock) | — |
| 13 | Không cho đổi trạng thái sai thứ tự (UC04) | State machine trong aggregate `Order` (`OrderStatus.canMoveTo`) | DDD |
| 14 | Gửi email chậm không làm chậm đặt hàng (UC06) | Event qua Kafka + gửi song song bằng Java 21 virtual threads | Xử lý bất đồng bộ |
| 15 | Mail server chết thì không mất email (UC06) | Retry + Dead Letter Topic | Xử lý bất đồng bộ |
