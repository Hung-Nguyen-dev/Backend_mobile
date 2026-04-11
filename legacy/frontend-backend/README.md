# Backend (Spring Boot + MySQL) — UC-04

## Chạy nhanh

1) Tạo database trong MySQL:

```sql
CREATE DATABASE travel_app CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2) Sửa `src/main/resources/application.yml` cho đúng username/password MySQL.

3) Chạy backend:

```bash
mvn spring-boot:run
```

## Test nhanh

- Sync dữ liệu địa điểm (Đà Nẵng) vào MySQL:

`POST http://localhost:8080/api/places/sync?city=Đà%20Nẵng`

- Gợi ý lịch (frontend gọi endpoint này):

`POST http://localhost:8080/api/v1/ai/itinerary`

Body:

```json
{
  "destination": "Đà Nẵng",
  "dayCount": 5,
  "preferences": ["Ẩm thực", "Biển"],
  "budgetTier": "medium",
  "startDate": "2026-04-10"
}
```

