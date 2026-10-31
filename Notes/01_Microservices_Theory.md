# Microservices Patterns

> Ghi chú lý thuyết dựa theo [microservices.io](https://microservices.io) — Chris Richardson

---

## Mục lục

1. [Architecture Style](#1-architecture-style)
2. [Service Boundaries](#2-service-boundaries)
3. [Chuyển đổi từ Monolith sang Microservice](#3-chuyển-đổi-từ-monolith-sang-microservice)
4. [Các service phối hợp với nhau như thế nào?](#4-các-service-phối-hợp-với-nhau-như-thế-nào)
5. [Transactional Messaging](#5-transactional-messaging)
6. [Testing](#6-testing)
7. [Deployment](#7-deployment)
8. [Cross-cutting Concerns](#8-cross-cutting-concerns)
9. [Communication Styles](#9-communication-styles)
10. [External API](#10-external-api)
11. [Service Discovery](#11-service-discovery)
12. [Reliability](#12-reliability)
13. [Security](#13-security)
14. [Observability](#14-observability)
15. [UI Design](#15-ui-design)

---

## 1. Architecture Style

### 1.1. Monolithic Architecture

**Ý tưởng:** Toàn bộ ứng dụng được build và deploy thành **một khối duy nhất** (ví dụ: 1 file `.jar` / `.war`), dùng chung một database.

| ✅ Ưu điểm | ❌ Nhược điểm |
|---|---|
| Dễ code, dễ test, dễ debug lúc đầu | Code càng lớn càng khó hiểu, khó sửa |
| Deploy đơn giản (chỉ 1 file) | Sửa 1 dòng cũng phải build & deploy lại cả hệ thống |
| Gọi hàm trong cùng process → nhanh, không lo lỗi mạng | Chỉ scale được cả khối, không scale riêng từng phần |
| | Bị khóa chặt vào một công nghệ |
| | Một module lỗi (ví dụ memory leak) có thể làm sập cả hệ thống |

> 💡 Monolith **không xấu**. Với dự án nhỏ hoặc team nhỏ, đây thường là lựa chọn hợp lý nhất để bắt đầu.

### 1.2. Microservice Architecture

**Ý tưởng:** Chia ứng dụng thành **nhiều service nhỏ**, mỗi service:
- Lo một phần nghiệp vụ
- Có database riêng
- Deploy độc lập
- Do một team nhỏ sở hữu

| ✅ Ưu điểm | ❌ Nhược điểm |
|---|---|
| Deploy độc lập, release nhanh | Hệ thống phân tán → phức tạp hơn nhiều (lỗi mạng, timeout, ...) |
| Scale riêng từng service | Khó đảm bảo dữ liệu nhất quán giữa các service |
| Mỗi service có thể dùng công nghệ khác nhau | Khó test, khó debug hơn |
| Một service lỗi không nhất thiết làm sập cả hệ thống | Cần đầu tư hạ tầng: CI/CD, monitoring, logging, ... |

> 💡 Gần như toàn bộ các pattern từ phần 2 trở đi sinh ra để **giải quyết các nhược điểm** ở bảng trên.

---

## 2. Service Boundaries

**Hai cách xác định service boundary:**

- Chia theo **chức năng nghiệp vụ** của doanh nghiệp (Business Capability). _Ví dụ: Quản lý đơn hàng, Thanh toán, Giao hàng_
- Chia theo **subdomain** của DDD

**Nguyên tắc sau khi xác định:**

- Một service nên tự xử lý được phần lớn nghiệp vụ của mình, không phụ thuộc quá nhiều vào service khác
- Một team chịu trách nhiệm và sở hữu service đó
- Những thứ hay thay đổi cùng nhau thì nên nằm chung một service

> ⚠️ Chia quá nhỏ (nano-service) cũng là sai lầm: các service phải gọi nhau liên tục, vừa chậm vừa khó quản lý.

---

## 3. Chuyển đổi từ Monolith sang Microservice

Hai pattern thường được **dùng cùng nhau** khi chuyển đổi:

### 3.1. Strangler Application — _Thay thế từng phần_

> ❌ Không xóa bỏ hoàn toàn hệ thống cũ để viết lại.

**Nên:**

1. Giữ nguyên hệ thống đang chạy
2. Tách từng chức năng thành microservice
3. Lưu lượng request dần chuyển sang service mới
4. Hệ thống cũ dần bị thay thế

### 3.2. Anti-corruption Layer (ACL) — _Lớp chống ô nhiễm_

- Tạo một lớp trung gian giữa hệ thống cũ và mới
- Chuyển đổi API, dữ liệu, nghiệp vụ
- Bảo vệ hệ thống mới khỏi thiết kế xấu của Legacy System

> **Ví dụ:** Hệ thống cũ gọi khách hàng là `CUSTOMER_INFO` với các cột viết tắt khó hiểu `CUST_NM`, `PH_NO`. Service mới dùng model `Customer(name, phone)`. ACL đứng giữa để "dịch" qua lại, nhờ đó service mới không bị dính model cũ.

---

## 4. Các service phối hợp với nhau như thế nào?

### 4.1. Database per Service

- Mỗi service sở hữu DB riêng
- Service khác **không được** truy cập trực tiếp DB đó
- Muốn lấy dữ liệu phải gọi API hoặc nhận Event từ service sở hữu dữ liệu

> ⚠️ **Hệ quả:** Không còn JOIN hay transaction xuyên service được nữa → đây chính là lý do cần các pattern 4.3 → 4.6 bên dưới.

### 4.2. Shared Database

- Các service cùng sử dụng một DB
- Dễ triển khai nhưng coupling cao
- Không phải cách tiếp cận microservice lý tưởng

### 4.3. Saga Pattern — _Giao dịch phân tán_

| Kiến trúc | Transaction |
|---|---|
| **Monolith** | Một transaction dùng chung cho toàn bộ nghiệp vụ |
| **Microservice** | Mỗi service có DB riêng → không thể dùng một transaction chung xuyên nhiều service |
| **Saga** | Chia thành nhiều local transaction. Nếu một bước lỗi → chạy **Compensating Transaction** để hoàn tác các bước đã làm |

> 💡 Compensating Transaction **không phải** rollback của DB. Đó là một hành động nghiệp vụ ngược lại. _Ví dụ: đã trừ tiền thì hoàn tiền, đã giữ hàng thì nhả hàng._

**Ví dụ đặt hàng:**
```
Create Order (PENDING) → Charge Payment → Reserve Stock → Order APPROVED
                                              ✗ Out of stock
                          ← Refund Payment ← Reject Order (REJECTED)
```

**Hai cách điều phối Saga:**

| Cách | Hoạt động | Phù hợp |
|---|---|---|
| **Choreography** — _Tự nhảy theo nhạc_ | Mỗi service xong việc thì phát Event, service kế tiếp nghe Event rồi làm tiếp. Không có ai chỉ huy | Saga ngắn, ít bước |
| **Orchestration** — _Có nhạc trưởng_ | Một **Orchestrator** ra lệnh cho từng service làm gì, và xử lý khi có lỗi | Saga dài, nhiều bước, dễ theo dõi hơn |

> ⚠️ **Lưu ý:** Trong lúc Saga đang chạy, dữ liệu ở các service tạm thời chưa khớp nhau (**Eventual Consistency** — cuối cùng sẽ nhất quán).

### 4.4. Command-side Replica

- Một service giữ bản sao dữ liệu cần thiết từ service khác
- Giảm số lần gọi API giữa các service
- Bản sao được cập nhật bằng cách **subscribe Event** từ service sở hữu dữ liệu

> **Ví dụ:** `OrderService` lưu _Customer Snapshot_ (Customer Name, Customer Level) thay vì gọi `CustomerService` liên tục. Khi `CustomerService` phát Event `CustomerUpdated`, `OrderService` cập nhật lại bản sao.

> ⚠️ Bản sao có thể bị cũ trong một khoảng thời gian ngắn.

### 4.5. API Composition — _Ghép dữ liệu từ nhiều API_

- Dùng khi một màn hình cần dữ liệu từ nhiều service
- Một service tổng hợp (**Aggregator**) gọi nhiều service rồi ghép kết quả. Aggregator có thể là API Gateway hoặc một service riêng

> **Ví dụ:** Màn hình _Order Detail_ cần Order + Customer + Product
> → API Composition gọi các service tương ứng và trả về **một JSON duy nhất** cho FE.

| ✅ Ưu điểm | ❌ Nhược điểm |
|---|---|
| Đơn giản, dễ làm | Phải ghép (JOIN) dữ liệu trong bộ nhớ → chậm với dữ liệu lớn |
| | Một service con lỗi là cả response có thể lỗi |

### 4.6. CQRS — _Command Query Responsibility Segregation_

- Tách riêng phần **ghi** (Command) và **đọc** (Query) dữ liệu
- Cho phép tối ưu riêng cho đọc và ghi

**Cách hoạt động trong microservice:**
```
Order Service    ──Event──┐
Customer Service ──Event──┼──► Query Service ──► View DB (pre-joined) ──► FE
Product Service  ──Event──┘
```
- Phía Query lắng nghe Event từ các service, rồi lưu sẵn dữ liệu đã ghép vào một **View DB** riêng
- Khi FE cần đọc thì chỉ query View DB, không phải gọi nhiều service

> 💡 **Khi nào dùng:** Khi API Composition quá chậm, hoặc cần query phức tạp (search, filter, thống kê) trên dữ liệu của nhiều service.
>
> ⚠️ View DB có độ trễ nhỏ so với dữ liệu gốc (Eventual Consistency).

### 4.7. Domain Event

- Khi dữ liệu thay đổi, service phát sinh Event
- Service phát Event → **Producer**
- Service nhận Event → **Consumer**
- Event thường đi qua Message Broker (Kafka, RabbitMQ, ...)
- Tên Event thường ở **thì quá khứ**: `OrderCreated`, `PaymentFailed`, ...

### 4.8. Event Sourcing

- Không lưu trạng thái cuối cùng
- Lưu **toàn bộ chuỗi sự kiện** dẫn tới trạng thái hiện tại
- Muốn biết trạng thái hiện tại → **replay** (chạy lại) các event từ đầu

> **Ví dụ — Tài khoản ngân hàng:**
> Thay vì lưu `balance = 70`, ta lưu: `Deposited(100)` → `Withdrawn(50)` → `Deposited(20)` → cộng lại ra 70.

| ✅ Ưu điểm | ❌ Nhược điểm |
|---|---|
| Có sẵn lịch sử đầy đủ (dùng luôn cho audit) | Khó học, cách nghĩ khác hẳn CRUD thông thường |
| Lưu event = publish event → không cần Outbox | Query trạng thái hiện tại khó → thường phải kết hợp CQRS |
| | Nhiều event thì replay chậm → cần **Snapshot** |

---

## 5. Transactional Messaging

> 🎯 **Mục tiêu:** Đảm bảo việc cập nhật Database và gửi Message/Event diễn ra an toàn, tránh mất dữ liệu.

> ❓ **Vì sao cần?** Nếu code là `save(order)` rồi `kafka.send(event)`:
> - Lưu DB xong mà gửi Kafka lỗi → **mất Event**
> - Gửi Kafka xong mà DB rollback → **Event "ma"** (báo có đơn hàng nhưng thực tế không có)

### 5.1. Transactional Outbox (Application Events)

**Mục đích:**
- Đảm bảo việc cập nhật Database và Publish Event diễn ra đáng tin cậy
- Tránh sử dụng 2PC (Distributed Transaction)

**Cách hoạt động:**
- Lưu dữ liệu nghiệp vụ và Event trong **cùng một transaction**
- Transaction commit → Event được lưu trong Outbox
- Transaction rollback → Event cũng rollback theo
- Một **Message Relay** riêng đọc Outbox và gửi Event tới Message Broker (Message Relay triển khai theo 5.2 hoặc 5.3)

```
BEGIN TX
  ├─ Update Business Data
  └─ Insert Event into Outbox
COMMIT
   ↓
Message Relay
   ↓
Kafka / RabbitMQ
```

**Ưu điểm:**
- Không cần 2PC
- Không mất Event khi transaction commit thành công
- Giữ được thứ tự Event

> ⚠️ **Lưu ý:** Consumer nên **idempotent** (xem 9.4) vì Event có thể bị publish nhiều hơn một lần.

### 5.2. Transaction Log Tailing

**Mục đích:** Một cách triển khai Message Relay trong pattern Transactional Outbox.

**Cách hoạt động:**
- Theo dõi (tail) transaction log của database
- Khi phát hiện record mới được ghi vào Outbox → publish sang Message Broker

> 💡 **Transaction Log:** Mỗi database đều lưu lại các thay đổi dữ liệu trong một file log nội bộ (MySQL: _binlog_, PostgreSQL: _WAL_).

| ✅ Ưu điểm | ❌ Nhược điểm |
|---|---|
| Không cần 2PC | Phụ thuộc DB |
| Gần realtime | Khó triển khai hơn Polling Publisher |
| Chính xác | Có thể publish trùng |

**Ví dụ:** Debezium (công cụ CDC phổ biến)

### 5.3. Polling Publisher

**Cách hoạt động:**
1. Định kỳ query (poll) bảng Outbox
2. Lấy các event chưa gửi
3. Publish sang Message Broker
4. Đánh dấu đã xử lý hoặc xóa khỏi Outbox

| ✅ Ưu điểm | ❌ Nhược điểm |
|---|---|
| Đơn giản | Có độ trễ do polling |
| Hoạt động với mọi SQL Database | Tốn query DB |
| | Khó đảm bảo thứ tự Event |
| | Có thể phát sinh Event trùng lặp |

---

## 6. Testing

### 6.1. Consumer-Driven Contract Test

**Mục đích:** Kiểm tra API của service vẫn đúng như những gì service khác mong đợi.

**Cách làm:**
- Consumer viết bộ test mô tả những gì nó kỳ vọng từ Provider
- Provider phải chạy được bộ test này để xác nhận đáp ứng đúng contract

> **Ví dụ:** `OrderService` (consumer) cần `GET /customers/1` trả về có field `name`. Nếu team `CustomerService` (provider) lỡ đổi `name` → `fullName`, test fail ngay trên CI của provider, trước khi lên Production.

**Công cụ:** Spring Cloud Contract, Pact

### 6.2. Consumer-side Contract Test

**Mục đích:** Chiều ngược lại của 6.1 — kiểm tra **phía Consumer** gọi và xử lý response của Provider đúng.

**Cách làm:** Dùng chính contract ở 6.1 để sinh ra một **stub** (Provider giả) cho Consumer test, khỏi phải chạy Provider thật.

### 6.3. Service Component Test

**Mục đích:** Kiểm tra service trong trạng thái độc lập.

**Cách làm:**
- Mock / Test Double các service phụ thuộc
- Chỉ kiểm tra logic của service hiện tại

---

## 7. Deployment

> 🧭 **Hướng phát triển:** Server vật lý → VM → **Container** → **Platform (Kubernetes) / Serverless**.
> Hiện nay phần lớn hệ thống microservice chạy bằng **Container + Kubernetes**, nên tập trung học kỹ phần đó.
>
> ⭐ Cần nắm chắc · 👍 Nên biết · 📖 Đọc qua

### 7.1. Service Instance per Container ⭐

**Mục đích:** Đóng gói mỗi service thành một container image (thường là Docker) và deploy mỗi service instance dưới dạng một container độc lập.

**Container là gì?** Một "hộp" chứa code + runtime (JDK, ...) + thư viện, chạy ở đâu cũng giống nhau. Hết cảnh _"máy em chạy được mà"_.

**Container khác VM thế nào?**

```
        Virtual Machine                     Container
┌──────────┬──────────┐          ┌──────────┬──────────┐
│ Service A│ Service B│          │ Service A│ Service B│
│ Guest OS │ Guest OS │          ├──────────┴──────────┤
├──────────┴──────────┤          │   Container Engine  │
│     Hypervisor      │          │   (Docker, ...)     │
├─────────────────────┤          ├─────────────────────┤
│       Host OS       │          │       Host OS       │
└─────────────────────┘          └─────────────────────┘
```

- Mỗi VM có **cả một hệ điều hành riêng** → nặng (GB), khởi động vài phút
- Các container **dùng chung kernel** của Host OS → nhẹ (MB), khởi động vài giây

**Quy trình:**
```
Code → Build Image (Dockerfile) → Push to Registry → Pull & Run Container
                                   (Docker Hub, ECR, Harbor, ...)
```

> **Ví dụ Dockerfile cho một Spring Boot service:**
> ```dockerfile
> FROM eclipse-temurin:21-jre
> COPY target/order-service.jar app.jar
> EXPOSE 8080
> ENTRYPOINT ["java", "-jar", "app.jar"]
> ```

**Khái niệm cần nhớ:**
- **Image:** bản đóng gói (giống class), không thay đổi
- **Container:** một instance đang chạy từ image (giống object). 1 image chạy được nhiều container
- **Registry:** nơi lưu trữ image
- **Image tag:** version của image, ví dụ `order-service:1.2.0`. Nên dùng version cụ thể, tránh dùng `latest` trên Production

| ✅ Ưu điểm | ❌ Nhược điểm |
|---|---|
| Dễ scale (chạy thêm container) | Chạy nhiều container bằng tay rất khó quản lý → cần 7.2 |
| Cô lập tốt giữa các service | Cần học thêm Docker, networking, ... |
| Giới hạn được RAM/CPU cho từng container | |
| Mỗi service dùng công nghệ khác nhau thoải mái | |
| Khởi động nhanh (vài giây) | |
| Môi trường dev / test / prod giống nhau | |

**Thường dùng:** Docker (đóng gói) + Kubernetes (quản lý, xem 7.2)

### 7.2. Service Deployment Platform ⭐

**Bài toán:** Có hàng chục service, mỗi service vài container. Container chết thì ai bật lại? Traffic tăng thì ai thêm container? Deploy bản mới thế nào để không downtime?

**Giải pháp:** Deploy service lên một **Platform**. Ta chỉ cần khai báo _"tôi muốn gì"_ (ví dụ: chạy 3 instance của `order-service:1.2.0`), Platform tự lo phần còn lại:
- **Scale:** tự tăng/giảm số container theo tải (autoscaling)
- **Load Balancing:** chia đều traffic cho các container
- **Recovery (Self-healing):** container chết thì tự bật container mới
- **Rolling Update:** deploy bản mới bằng cách thay dần từng container → không downtime, lỗi thì rollback được
- **Monitoring:** theo dõi trạng thái container (qua Health Check API, xem 14.2.2)

**Kubernetes (K8s) — các khái niệm cơ bản cần biết:**

| Khái niệm | Dễ hiểu là |
|---|---|
| **Pod** | Đơn vị nhỏ nhất, bọc 1 (hoặc vài) container |
| **Deployment** | Khai báo "chạy N Pod của image X", tự thay Pod chết, hỗ trợ rolling update |
| **Service** | Một địa chỉ cố định đứng trước nhóm Pod, kiêm load balancer nội bộ (chính là Server-side Discovery ở 11.2) |
| **Ingress** | Cửa ngõ cho traffic từ bên ngoài vào (gần giống API Gateway đơn giản) |
| **ConfigMap / Secret** | Lưu config và mật khẩu ngoài code (chính là Externalized Configuration ở 8.2) |
| **HPA** (Horizontal Pod Autoscaler) | Tự tăng/giảm số Pod theo CPU, RAM, ... |

> **Ví dụ khai báo Deployment:**
> ```yaml
> apiVersion: apps/v1
> kind: Deployment
> metadata:
>   name: order-service
> spec:
>   replicas: 3                    # always keep 3 Pods running
>   selector:
>     matchLabels:
>       app: order-service
>   template:
>     metadata:
>       labels:
>         app: order-service
>     spec:
>       containers:
>         - name: order-service
>           image: order-service:1.2.0
>           ports:
>             - containerPort: 8080
> ```

> 💡 Nhiều pattern ở các phần khác được Kubernetes **làm sẵn**: Service Discovery (11), Externalized Configuration (8.2), Health Check (14.2.2).

**Ví dụ platform:**
- Container: **Kubernetes** (phổ biến nhất), AWS EKS / Google GKE / Azure AKS (Kubernetes do Cloud quản lý), Docker Swarm
- Serverless: AWS Lambda
- PaaS: Cloud Foundry, AWS Elastic Beanstalk

### 7.3. Serverless Deployment 👍

**Cách làm:**
- Upload code lên Cloud
- Cloud tự chạy và tự scale. Không có request thì không chạy, không tốn tiền

**Ví dụ:** AWS Lambda, Azure Functions, Google Cloud Functions

| ✅ Ưu điểm | ❌ Nhược điểm |
|---|---|
| Không quản lý hạ tầng | Cold Start (lần gọi đầu chậm) |
| Tự động scale | Giới hạn thời gian chạy (ví dụ: AWS Lambda tối đa 15 phút) |
| Trả tiền theo request | Không hợp với service chạy lâu hoặc phải giữ kết nối/trạng thái |
| | Phụ thuộc vào nhà cung cấp Cloud (vendor lock-in) |

**Thích hợp cho:** Event Processing, Scheduled Job, API nhỏ

### 7.4. Các cách deploy truyền thống 📖

> Ngày nay ít dùng cho microservice. Đọc qua để hiểu vì sao Container ra đời.

| Pattern | Ý tưởng | Vì sao ít dùng |
|---|---|---|
| **Multiple Service Instances per Host** | Chạy nhiều service trên cùng một server (ví dụ: nhiều JVM / Tomcat) | Các service tranh CPU/RAM, một service lỗi kéo theo service khác |
| **Service Instance per Host** | Mỗi server chỉ chạy đúng một service | Cô lập tốt nhưng lãng phí tài nguyên, tốn tiền |
| **Service Instance per VM** | Mỗi service đóng gói thành một VM image (ví dụ: AWS AMI) | VM nặng, build image lâu, khởi động chậm → Container làm cùng việc này nhẹ hơn nhiều |

---

## 8. Cross-cutting Concerns

> Là những thứ mà service nào cũng cần, lặp đi lặp lại ở mọi microservice.

### 8.1. Microservice Chassis

**Ý tưởng:**
- Tạo sẵn một framework dùng chung
- Service mới kế thừa framework đó

**Chứa:** Logging, Security, Health Check, Metrics, ...

**Ví dụ:** Spring Boot, Spring Cloud, Company Starter

### 8.2. Externalized Configuration

**Ý tưởng:**
- Đưa toàn bộ config ra ngoài ứng dụng
- Service đọc config lúc startup từ:
  - Environment Variables
  - Property Files
  - Command Line Arguments
  - Config Server (Spring Cloud Config, Consul, Kubernetes ConfigMap / Secret)

> 💡 **Lợi ích:** Cùng một bản build chạy được ở mọi môi trường (dev / test / prod), chỉ cần đổi config, không phải build lại. Password, secret cũng không nằm trong code.

### 8.3. Service Template

**Ý tưởng:** Một project mẫu để tạo service mới.

> 💡 **Khác Chassis thế nào?**
> - **Template:** _copy_ một lần lúc tạo service. Sau này template sửa thì phải tự sửa tay vào từng service.
> - **Chassis:** là _thư viện / dependency_. Nâng version là mọi service được cập nhật.

### 8.4. Service Mesh & Sidecar

**Ý tưởng:** Thay vì nhét code xử lý mạng (retry, timeout, mTLS, tracing, ...) vào từng service, ta đặt một **proxy (sidecar)** chạy cạnh mỗi service. Mọi traffic vào/ra service đều đi qua proxy này.

```
[Service A] ⇄ [Proxy] ⇄ ─── network ─── ⇄ [Proxy] ⇄ [Service B]
```

| ✅ Ưu điểm | ❌ Nhược điểm |
|---|---|
| Không phải sửa code service | Thêm một hệ thống phức tạp cần vận hành |
| Dùng được với mọi ngôn ngữ | Thêm một chút độ trễ |

**Ví dụ:** Istio, Linkerd

---

## 9. Communication Styles

### 9.1. Remote Procedure Invocation (RPI)

**Mục đích:** Giao tiếp giữa các service qua Request/Response.

**Công nghệ:** REST, gRPC, Thrift

**Đặc điểm:** Synchronous communication

| ✅ Ưu điểm | ❌ Nhược điểm |
|---|---|
| Đơn giản | Runtime coupling |
| Dễ hiểu | Client phải chờ response |
| Không cần Message Broker | Service đích bị down thì request fail |

> 💡 Khi dùng RPI nên kết hợp **Timeout + Circuit Breaker** (xem 12) để tránh lỗi dây chuyền.

### 9.2. Messaging

**Mục đích:** Giao tiếp bất đồng bộ giữa các service.

**Công nghệ:** Kafka, RabbitMQ

**Các kiểu giao tiếp:**
- **Request/Response:** gửi yêu cầu và chờ phản hồi (nhưng qua message)
- **Notification:** gửi một chiều, không cần phản hồi
- **Request/Async Response:** gửi yêu cầu, phản hồi sẽ đến sau, không ngồi chờ
- **Publish/Subscribe:** phát một message, nhiều service cùng nhận
- **Publish/Async Response:** phát cho nhiều service, rồi thu thập phản hồi của chúng sau

| ✅ Ưu điểm | ❌ Nhược điểm |
|---|---|
| Loose Coupling | Cần Message Broker |
| Availability cao (service nhận tạm chết thì message vẫn nằm chờ trong broker) | Phức tạp hơn REST |
| Hỗ trợ nhiều kiểu giao tiếp | Khó debug hơn vì luồng xử lý không tuần tự |

### 9.3. Domain-Specific Protocol

**Mục đích:** Giao tiếp bằng protocol chuyên biệt của domain.

**Ví dụ:**
- FIX (Tài chính)
- HL7 / FHIR (Y tế)
- RFC / BAPI (SAP)

| ✅ Ưu điểm | ❌ Nhược điểm |
|---|---|
| Phù hợp với chuẩn ngành | Khó học hơn |
| Tối ưu cho nghiệp vụ cụ thể | Ít phổ biến hơn REST/Kafka |

### 9.4. Idempotent Consumer

**Mục đích:** Xử lý đúng ngay cả khi nhận cùng một Message nhiều lần.

**Nguyên nhân:** Message Broker thường dùng _At-Least-Once Delivery_ — đảm bảo message đến **ít nhất** một lần, nên có thể đến 2–3 lần.

**Giải pháp:**
- Lưu Message ID đã xử lý (phổ biến: bảng `PROCESSED_MESSAGES`)
- Nếu Message đã xử lý rồi thì bỏ qua

> ⚠️ Việc ghi Message ID và cập nhật dữ liệu nghiệp vụ phải nằm trong **cùng một transaction**. Nếu không, vẫn có thể xử lý trùng.

**Ưu điểm:**
- Tránh xử lý trùng lặp (ví dụ: trừ tiền 2 lần)
- Đảm bảo tính đúng đắn của dữ liệu

---

## 10. External API

> Client bên ngoài gọi vào hệ thống Microservice qua 2 pattern sau.

### 10.1. API Gateway

**Mục đích:** Điểm vào duy nhất cho Client.

**Chức năng:**
- Routing
- Authentication
- Load Balancing
- Rate Limiting
- API Aggregation
- Chuyển đổi protocol (ví dụ: client gọi REST, bên trong dùng gRPC)

| ✅ Ưu điểm | ❌ Nhược điểm |
|---|---|
| Client chỉ cần biết 1 địa chỉ | Thêm một thành phần cần vận hành |
| Giấu cấu trúc bên trong hệ thống | Nếu Gateway chết là cả hệ thống không truy cập được → phải chạy nhiều instance |

**Ví dụ:** Spring Cloud Gateway, Kong, Nginx, AWS API Gateway

### 10.2. Backend For Frontend (BFF)

**Mục đích:** Tối ưu API cho từng loại Client (Web, Mobile, ...).

**Cách làm:** Mỗi loại client có **một API Gateway riêng**, thường do chính team làm client đó sở hữu.

```
Web App    → Web BFF    ─┐
Mobile App → Mobile BFF ─┼─→ Microservices
3rd Party  → Public API ─┘
```

> **Ví dụ:** Màn hình Web hiển thị nhiều thông tin, Mobile chỉ cần vài field để tiết kiệm băng thông. Mỗi BFF trả về đúng thứ client của nó cần.

---

## 11. Service Discovery

> ❓ Làm thế nào để một service tìm được service khác?

### 11.1. Client-Side Service Discovery

**Bài toán:**
- `OrderService` muốn gọi tới `CustomerService`
- `CustomerService` có nhiều instance — instance có thể chết, mới tạo, IP có thể thay đổi
- → Không thể hard-code URL

**Giải pháp:** Client trực tiếp hỏi **Service Registry** để lấy danh sách địa chỉ, rồi tự chọn một instance để gọi.

**Ví dụ:** Eureka (registry) + Spring Cloud LoadBalancer / Ribbon (load balancing phía client) + Feign (HTTP client)

> 💡 Ribbon đã ngừng phát triển, bản Spring Cloud mới dùng **Spring Cloud LoadBalancer**.

| ✅ Ưu điểm | ❌ Nhược điểm |
|---|---|
| Không cần Router trung gian | Client phức tạp hơn |
| Client tự Load Balance | Phụ thuộc Registry |
| | Mỗi ngôn ngữ phải có thư viện discovery riêng |

### 11.2. Server-Side Service Discovery

**Bài toán:** Tương tự Client-Side.

**Giải pháp:**
- Đặt một tầng trung gian giữa Client và Service
- Tầng trung gian này giao tiếp với Registry

**Ví dụ:** Kubernetes Service, AWS Load Balancer, API Gateway

| ✅ Ưu điểm | ❌ Nhược điểm |
|---|---|
| Client đơn giản | Cần thêm tầng trung gian |
| Discovery tập trung | Thêm một bước nhảy mạng (network hop) |
| Dễ quản lý | |

### 11.3. Service Registry

**Mục đích:** Lưu địa chỉ các Service Instance.

**Lưu trữ:** Service Name, Host, Port, Status

**Chức năng:**
- Register khi service start
- Deregister khi service stop
- Health Check

> ⚠️ Registry là trái tim của discovery. Registry chết là các service không tìm thấy nhau → phải chạy nhiều instance (cluster).

**Ví dụ:** Eureka, Consul, ZooKeeper, etcd, Kubernetes Service

### 11.4. Self Registration

**Mục đích:** Service tự đăng ký vào Registry.

```
Start → Register → Heartbeat → Shutdown → Unregister
```

| ✅ Ưu điểm | ❌ Nhược điểm |
|---|---|
| Service tự quản lý trạng thái | Gắn chặt với Registry |
| | Phải cài logic đăng ký trong service |

**Ví dụ:** Eureka Client, Spring Cloud Netflix

### 11.5. 3rd Party Registration

**Mục đích:** Một thành phần bên ngoài đăng ký service vào Registry.

```
Service → Registration Component → Service Registry
```

**Ưu điểm:**
- Service không cần biết Registry
- Giảm coupling
- Quản lý tập trung

**Ví dụ:** Kubernetes, Cloud Platform

> 💡 Dùng **Kubernetes** thì gần như có sẵn cả 11.2, 11.3 và 11.5, không cần tự dựng Eureka.

---

## 12. Reliability

### 12.1. Circuit Breaker

**Mục đích:** Ngăn lỗi của một service lan sang service khác.

> **Hình dung:** Giống cầu dao điện trong nhà. Chập điện thì cầu dao tự ngắt, không để cháy lan ra cả nhà.

**Trạng thái:**

| Trạng thái | Ý nghĩa |
|---|---|
| 🟢 `CLOSED` | Hoạt động bình thường, request đi qua. Đồng thời đếm số lỗi. Lỗi vượt ngưỡng (ví dụ 50%) → chuyển sang `OPEN` |
| 🔴 `OPEN` | Chặn request, trả lỗi **ngay lập tức** (hoặc trả dữ liệu dự phòng — **fallback**) mà không gọi service đích. Sau một khoảng thời gian → chuyển sang `HALF-OPEN` |
| 🟡 `HALF-OPEN` | Cho thử vài request đi qua. Thành công → `CLOSED`, vẫn lỗi → quay lại `OPEN` |

```
CLOSED ──(too many failures)──► OPEN ──(timeout expired)──► HALF-OPEN
   ▲                              ▲                            │
   └──────(trial OK)──────────────┼────────────────────────────┤
                                  └──────(trial failed)────────┘
```

**Ưu điểm:**
- Tránh timeout dây chuyền
- Tránh cạn kiệt thread pool
- Tăng độ ổn định hệ thống
- Cho service đích thời gian "thở" để hồi phục

**Ví dụ:** Resilience4j (Netflix Hystrix đã ngừng phát triển)

### 12.2. Các kỹ thuật hay đi kèm

> Phần này không có trên microservices.io nhưng thường dùng cùng Circuit Breaker.

- **Timeout:** Không bao giờ chờ vô hạn. Đặt thời gian tối đa cho mỗi lần gọi
- **Retry:** Lỗi tạm thời thì thử lại vài lần, mỗi lần chờ lâu hơn (_exponential backoff_). Chỉ retry những thao tác idempotent
- **Bulkhead:** Chia riêng thread pool / connection cho từng service đích. Một service chậm chỉ chiếm hết pool của nó, không kéo theo các phần khác
- **Rate Limiter:** Giới hạn số request trong một khoảng thời gian để bảo vệ service khỏi quá tải

---

## 13. Security

### 13.1. Access Token

**Bài toán:** User gửi request tới `PaymentService`. Service cần biết:
- User là ai?
- Role là gì?
- Có quyền thanh toán không?

**Giải pháp:**
- Sau khi đăng nhập thành công, hệ thống cấp cho user một **Access Token**
- Token chứa thông tin về người dùng
- **API Gateway** kiểm tra token, rồi truyền token kèm theo request tới các service phía sau
- Mỗi service tự đọc token để biết user là ai và có quyền gì

```
Client ──(token)──► API Gateway (verify) ──(token)──► Order Service ──(token)──► Payment Service
```

**Công nghệ:** OAuth2, JWT

> 💡 **JWT** có chữ ký số → service tự kiểm tra token hợp lệ được, không cần gọi lại Auth Server mỗi lần.
>
> ⚠️ JWT chỉ được **mã hóa Base64, không phải mã hóa bí mật**. Ai có token cũng đọc được nội dung → **không** để password hay dữ liệu nhạy cảm trong token.
>
> ⚠️ Nên để Access Token hết hạn nhanh (vài phút đến 1 giờ), kết hợp **Refresh Token** để lấy token mới.

**Lợi ích:**
- Login một lần
- Service vẫn biết user là ai
- Hỗ trợ Authentication và Authorization

---

## 14. Observability

> ❓ Khi hệ thống đang chạy, làm sao biết nó hoạt động thế nào, và khi lỗi xảy ra thì tìm nguyên nhân ở đâu?

### 14.1. Theo dõi Log

#### 14.1.1. Log Aggregation — _Log tập trung_

**Bài toán:**
- Hệ thống phân tán trên nhiều máy chủ / instance
- Khi xảy ra sự cố, truy cập từng instance riêng lẻ để xem log là bất khả thi và cực kỳ tốn thời gian

**Giải pháp:**
- Dùng một **dịch vụ log tập trung** để aggregate log từ mọi service instance
- Người dùng có thể:
  - Tìm kiếm và phân tích log
  - Cấu hình Alert tự động kích hoạt khi log xuất hiện những chuỗi tin nhắn nhất định

**Kỹ thuật bổ trợ nên có:**
- **Correlation ID / External Request ID:** luôn gắn ID này vào từng dòng log để truy vết luồng đi của request qua nhiều service
- **Exception Tracking Service:** dùng dịch vụ chuyên biệt để quản lý ngoại lệ / lỗi ứng dụng hiệu quả hơn thay vì chỉ log thuần túy
- **Structured Logging:** ghi log dạng JSON thay vì text tự do để dễ search, filter

> ⚖️ **Trade-off:** Cần đầu tư infrastructure đủ lớn để xử lý lượng log khổng lồ.

**Ví dụ:** ELK Stack (Elasticsearch + Logstash + Kibana), Grafana Loki, AWS CloudWatch

#### 14.1.2. Audit Logging

**Mục đích:**
- Bắt buộc với các hệ thống có yêu cầu cao về Security & Compliance
- Truy vết hành động của user để hỗ trợ người dùng khi cần thiết

**Giải pháp:** Ghi nhận mọi user action vào một audit log lưu trong database. Mỗi bản ghi thường gồm:
- **Identity** — Ai đã thực hiện
- **Action** — Hành động là gì
- **Target / Data** — Đối tượng nghiệp vụ nào bị tác động
- **Timestamp** — Thời điểm thực hiện

**Các cách triển khai:**
- **AOP / Interceptor:** tự động chèn mã audit ở tầng framework trước/sau khi gọi hàm nghiệp vụ
- **Event Sourcing** pattern
- **DB Trigger / Change Data Capture (CDC)**

| ✅ Ưu điểm | ❌ Nhược điểm |
|---|---|
| Bản ghi lịch sử rõ ràng, chính xác về hành động của người dùng | Nếu viết thủ công, audit code đan xen với business logic → mã nguồn phức tạp, khó bảo trì |
| Đáp ứng yêu cầu khắt khe về compliance, security logging để giải quyết tranh chấp / khiếu nại | Tốn thêm chi phí lưu trữ do audit log tăng dần theo thời gian |

> 💡 **Audit Log khác Application Log:** Application log dành cho dev debug, có thể xóa sau vài ngày. Audit log là bằng chứng nghiệp vụ, phải giữ lâu và **không được sửa/xóa**.

#### 14.1.3. Log Deployments and Changes

**Mục đích:** Trả lời câu hỏi _"Ai vừa deploy hay thay đổi gì trên Production?"_

**Cơ chế hoạt động:**
1. CI/CD Pipeline deploy thành công → tự động gọi API gửi sự kiện tới công cụ giám sát
2. Công cụ giám sát hiển thị mốc deploy đè lên Dashboard Metrics / Logs / Exceptions

> **Ví dụ:** Biểu đồ error rate tăng vọt lúc 14:05, đúng ngay mốc "Deploy order-service v2.3 lúc 14:03" → nghi ngờ bản deploy này đầu tiên, có thể rollback ngay.

### 14.2. Theo dõi hiệu năng

#### 14.2.1. Application Metrics

**Mục đích:** Đo lường hiệu năng và sức khỏe hệ thống theo thời gian thực (Real-time Monitoring) qua dữ liệu số.

**Cơ chế hoạt động:**
1. **Instrumentation:** chèn mã đo đạc vào service để tự động ghi nhận dữ liệu (CPU/RAM, Latency, Error Rate, ...)
2. **Aggregation:** tập hợp chỉ số từ tất cả service instance về một **Centralized Metrics Service**
3. **Visualization & Alerting:** trực quan hóa lên Dashboard và cài Alert khi chỉ số vượt ngưỡng an toàn

**Hai mô hình thu thập:**

| Mô hình | Cách hoạt động | Ví dụ |
|---|---|---|
| **Pull** | Metrics Service định kỳ gọi API kéo dữ liệu từ service → service cần expose một metrics endpoint | Prometheus |
| **Push** | Service chủ động gửi dữ liệu về Metrics Service | AWS CloudWatch, StatsD |

> ⚖️ **Trade-off:** Cần infrastructure ghi và xử lý ổn định lượng lớn dữ liệu chuỗi thời gian (time-series), đồng thời không gây runtime overhead cho ứng dụng chính.

**Ví dụ (Spring):** Micrometer + Spring Boot Actuator → Prometheus → Grafana

#### 14.2.2. Health Check API

**Mục đích:**
- Tự động hóa việc giám sát và khôi phục hệ thống
- Trả lời câu hỏi _"Instance này còn sống không, và có nhận request được lúc này không?"_

**Khái niệm:**

| Probe | Câu hỏi | Nếu fail |
|---|---|---|
| **Liveness** | Còn sống không? | Restart container / instance |
| **Readiness** | Sẵn sàng nhận request chưa? | Tạm ngắt traffic khỏi Load Balancer |

**Best practice:**
- Giữ logic kiểm tra trong Health Check API thật nhẹ và phản hồi nhanh
- Phân biệt rõ lỗi nội bộ nghiêm trọng với lỗi tạm thời để tránh restart dịch vụ hàng loạt
- Liveness **không nên** kiểm tra DB hay service khác. Nếu DB chết mà Liveness fail thì mọi instance bị restart liên tục, trong khi restart cũng không sửa được DB. Những kiểm tra này nên đặt ở Readiness

**Ví dụ:** Spring Boot Actuator (`/actuator/health`)

### 14.3. Debug lỗi

#### 14.3.1. Distributed Tracing

**Mục đích:** Trả lời câu hỏi _"Request này đã đi qua những service nào, mất bao lâu ở mỗi nơi, bị nghẽn / lỗi ở đâu?"_

**Giải pháp:**
- Gán cho mỗi external request một mã định danh duy nhất gọi là **Trace ID**
- Trace ID được truyền qua tất cả các service tham gia xử lý request (thường qua HTTP header)
- Mỗi service ghi nhận thông tin và gửi về một **Centralized Distributed Tracing Service**

**Khái niệm cốt lõi:**
- **Trace:** toàn bộ hành trình của một request
- **Span:** một đơn vị công việc nhỏ trong Trace (ví dụ: gọi 1 API, chạy 1 câu query DB)

```
Trace ID: abc123                                  (total 350ms)
├─ Span: API Gateway         ████████████████████ 350ms
│  ├─ Span: Order Service      ██████████████     250ms
│  │  ├─ Span: Query DB          ███              50ms
│  │  └─ Span: Payment Service      ████████      180ms  ← slowest
```

> 💡 **Tối ưu — Sampling:** Không cần trace 100% request trên Production để tránh quá tải infrastructure. Chỉ trace 1–5% traffic bình thường, nhưng trace 100% request gặp lỗi.

**Ví dụ:** OpenTelemetry (chuẩn thu thập), Zipkin, Jaeger (lưu trữ & hiển thị)

#### 14.3.2. Exception Tracking

**Bài toán:**
- Chỉ ghi exception vào file log thông thường thì khó phát hiện kịp thời, vì log trôi nhanh hoặc bị lẫn giữa hàng nghìn dòng bình thường
- Cần gom các exception giống nhau thành từng nhóm issue thay vì báo cáo rời rạc
- Cần nhận cảnh báo ngay khi có lỗi mới hoặc lỗi nghiêm trọng

**Giải pháp:** Dùng **Centralized Exception Tracking Service**. Khi exception xảy ra, ngoài việc ghi log:
1. Tự động thu thập exception cùng toàn bộ stack trace và context metadata
2. Tự động gửi về Exception Tracking Service
3. Hệ thống loại bỏ trùng lặp, gom các lỗi giống nhau thành **issue** để đội kỹ sư dễ theo dõi, phân công xử lý và đánh giá mức độ ảnh hưởng

**Ví dụ:** Sentry, Rollbar, Bugsnag

---

## 15. UI Design

> ❓ Làm thế nào để một screen / page hiển thị dữ liệu từ nhiều service?

### 15.1. Server-side Page Fragment Composition

**Mục đích:**
- Micro-frontends ở tầng server
- Ghép trang web từ nhiều HTML fragment do các microservice độc lập cung cấp, ngay trên server

**Giải pháp:** Dùng một thành phần phía server gọi là **Page Router / Aggregator** (thường là một service hoặc API Gateway / Edge Service). Khi có request từ trình duyệt:
1. Page Router đóng vai trò là khung trang
2. Các fragment được tải song song từ các microservice tương ứng
3. Page Router ghép các fragment thành một trang HTML hoàn chỉnh phía server
4. Trả về trình duyệt một trang HTML duy nhất đã đầy đủ nội dung

```
Browser → Page Router ─┬─ Fragment A (Service A)
                       ├─ Fragment B (Service B)
                       └─ Fragment C (Service C)
             ↓
       Complete HTML → Browser
```

**Ví dụ:**
- SSI (Server Side Includes)
- ESI (Edge Side Includes) — ghép trang ngay tại CDN / proxy như Varnish
- Tailor (Zalando)

### 15.2. Client-side UI Composition

**Mục đích:** Micro-frontends ở tầng **trình duyệt**. Mỗi team tự làm phần UI của service mình, rồi ghép lại ngay trên trình duyệt.

**Giải pháp:**
- Có một ứng dụng khung (**Shell / Container App**) chứa layout chung: header, menu, ...
- Mỗi vùng trên màn hình là một **UI component** do một team sở hữu
- Mỗi component tự gọi API của service tương ứng để lấy dữ liệu

```
┌──────────── Shell App (Browser) ────────────┐
│  [Header]                                   │
│  [Product Widget] → calls Product Service   │
│  [Cart Widget]    → calls Cart Service      │
│  [Review Widget]  → calls Review Service    │
└─────────────────────────────────────────────┘
```

| ✅ Ưu điểm | ❌ Nhược điểm |
|---|---|
| Mỗi team tự làm và deploy UI của mình độc lập | Khó giữ giao diện thống nhất giữa các team |
| Trang phản hồi nhanh, tương tác tốt | Trình duyệt phải tải nhiều JS → nặng nếu không tối ưu |
| | Không tốt cho SEO bằng render phía server |

**Ví dụ:** Webpack Module Federation, single-spa, Web Components
