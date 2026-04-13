# Hướng dẫn Cấu hình Email OTP Verification

## Tổng quan về hệ thống
Hệ thống đăng ký mới có 2 bước:
1. **Bước 1**: Người dùng nhập thông tin đăng ký và email
2. **Bước 2**: Hệ thống gửi OTP (One-Time Password) đến email
3. **Bước 3**: Người dùng nhập OTP để xác thực email
4. **Bước 4**: Tài khoản được tạo với email đã xác thực

## Cấu hình Email (Gmail)

### Yêu cầu:
- Gmail hoặc các SMTP server khác

### Hướng dẫn sử dụng Gmail:

1. **Bật 2-Factor Authentication:**
   - Truy cập https://myaccount.google.com/
   - Vào mục "Security" trong thanh bên
   - Bật "2-Step Verification"

2. **Tạo App Password:**
   - Quay lại Security settings
   - Tìm mục "App passwords" (chỉ hiển thị nếu 2FA đã bật)
   - Chọn "Mail" và "Windows Computer" (hoặc Device tương ứng)
   - Google sẽ cung cấp password 16 ký tự

3. **Cấu hình trong application-local.yaml:**

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: xxxx xxxx xxxx xxxx  # App password từ bước 2
    from: noreply@travelapp.com
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
          connectiontimeout: 5000
          timeout: 5000
          writetimeout: 5000
```

## Các Endpoint mới

### 1. Gửi OTP
```
POST /api/v1/auth/send-otp
Body: { "email": "user@example.com" }
Response: { 
  "message": "OTP đã được gửi đến email của bạn",
  "email": "user@example.com",
  "expiryMinutes": 10 
}
```

### 2. Xác thực OTP
```
POST /api/v1/auth/verify-otp
Body: { 
  "email": "user@example.com",
  "otpCode": "123456"
}
Response: { "message": "OTP xác thực thành công" }
```

### 3. Hoàn tất Đăng ký (sau khi OTP được xác thực)
```
POST /api/v1/auth/complete-registration
Body: {
  "username": "user123",
  "password": "password123",
  "email": "user@example.com",
  "fullName": "Ngô Minh Phúc",
  "otpCode": "123456"
}
Response: {
  "id": 1,
  "username": "user123",
  "email": "user@example.com",
  "fullName": "Ngô Minh Phúc",
  "avatarUrl": null,
  "role": "USER"
}
```

## Cấy hình OTP

Trong `application.yaml`:
```yaml
app:
  otp:
    length: 6              # Độ dài OTP (mặc định: 6 chữ số)
    expiry-minutes: 10     # Thời gian hết hạn OTP (mặc định: 10 phút)
  mail:
    otp-template: "Mã xác thực của bạn là: %s. Mã này sẽ hết hạn trong 10 phút."
```

## Cơ sở dữ liệu

### Bảng users (cập nhật):
- `is_email_verified` (boolean) - Cho biết email có được xác thực
- `email_verified_at` (long) - Timestamp khi email được xác thực

### Bảng email_otps (mới):
- `id` - Primary key
- `email` - Email nhận mã OTP
- `otp_code` - Mã OTP
- `expires_at` - Thời gian hết hạn
- `used` - Đã sử dụng hay chưa
- `created_at` - Thời gian tạo
- `verified_at` - Thời gian xác thực

## Héc hành

Quá trình Đăng ký mới:

```
Frontend: Nhập thông tin → Gửi POST /api/v1/auth/send-otp
Backend: Tạo OTP seed, lưu vào database, gửi email
Frontend: Nhân OTP từ email → Gửi POST /api/v1/auth/complete-registration
Backend: Xác thực OTP → Tạo user với is_email_verified = true
Frontend: Lưu session user → Chuyển đến home page
```

## Kiểm tra

1. Chạy backend:
```bash
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```

2. Kiểm tra database:
```sql
SELECT * FROM email_otps;
SELECT id, email, is_email_verified FROM users;
```

3. Test bằng Postman/cURL

## Troubleshooting

### Email không gửi được
- Kiểm tra username/password chính xác
- Cho phép "Less secure apps" nếu không sử dụng App Password
- Kiểm tra SMTP host/port đúng
- Xem logs backend để tìm lỗi

### OTP hết hạn
- Tăng giá trị `app.otp.expiry-minutes` trong application.yaml

### OTP không đúng
- Mã được so sánh không phân biệt tráng lế

