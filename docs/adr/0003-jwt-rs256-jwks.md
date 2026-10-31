# ADR-0003: auth-service cấp JWT RS256, công bố JWKS; gateway và service đều kiểm tra

**Trạng thái:** Chấp nhận

## Bối cảnh
Đề bài yêu cầu một service chịu trách nhiệm security cho các microservice, dùng Spring Security.

## Quyết định
- **auth-service** (Spring Security + `NimbusJwtEncoder`) ký access token bằng **private key RSA** (RS256, 15 phút), công bố public key tại `/.well-known/jwks.json`.
- **api-gateway** (WebFlux, OAuth2 Resource Server) kiểm tra chữ ký và hạn của token, trả 401 / 403 sớm.
- **Từng service** cũng là Resource Server và kiểm tra lại token (defense in depth). Claim `roles` được map sang `ROLE_*`.
- **Refresh token** là chuỗi opaque, DB chỉ lưu SHA-256, có rotation, idle timeout 30 phút, absolute timeout 7 ngày. Sai mật khẩu 5 lần thì khóa 15 phút (đếm trong DB).

## Phương án đã cân nhắc
- **HS256 (shared secret)**: đơn giản hơn, nhưng service nào giữ secret cũng có thể tự ký token.
- **Session + Redis**: stateful. Redis đã bị cắt khỏi phạm vi trước 6/11.
- **Keycloak**: mạnh nhưng không thể hiện được hiểu biết về Spring Security.

## Hệ quả
- (+) Không gọi auth-service mỗi request. Service khác không thể giả token.
- (−) Access token không thu hồi được trước khi hết hạn. Đã giảm thiểu bằng TTL 15 phút. Blacklist bằng Redis để sau 6/11.
- (−) Key được sinh ra và lưu ở `.keys/` khi chạy local. Production nên dùng secret manager.
