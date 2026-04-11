# Backend Mobile - Booking & Finance APIs

This project now includes backend APIs for Huy's module:

- Booking: flight, hotel, coach (xe/tau/xe buyt), restaurant
- Trip finance: budget, expenses, expense split
- Balance remaining
- Category analytics data for charting

## Base URL

`/api/v1`

## Setup API (for Huy module only)

- `POST /trips`

Example request body:

```json
{
  "userId": 1,
  "tripName": "HCM Trip 2026",
  "destination": "Ho Chi Minh City",
  "startDate": "2026-04-05",
  "endDate": "2026-04-10"
}
```

## Booking APIs

- `POST /trips/{tripId}/bookings/flights`
- `POST /trips/{tripId}/bookings/hotels`
- `POST /trips/{tripId}/bookings/coaches`
- `POST /trips/{tripId}/bookings/restaurants`
- `GET /trips/{tripId}/bookings`
- `PATCH /trips/{tripId}/bookings/{bookingId}/payment-status?status=PAID`

Example request body (`/bookings/flights`):

```json
{
  "userId": 1,
  "totalAmount": 2500000,
  "paymentStatus": "PENDING",
  "pnrCode": "ABCD12",
  "flightNumber": "VN123",
  "departureAirport": "HAN",
  "arrivalAirport": "SGN",
  "departureTime": "09:30:00",
  "arrivalTime": "11:40:00",
  "payment": {
    "transactionNo": "TXN-001",
    "amount": 2500000,
    "paymentDate": "2026-03-30"
  }
}
```

Example request body (`/bookings/coaches` - **Vé xe liên tỉnh**):

```json
{
  "userId": 1,
  "totalAmount": 450000,
  "paymentStatus": "PENDING",
  "name": "Tuyến Hà Nội - TP.HCM",
  "seat": "A12",
  "pickUp": "Bến xe Mỹ Đình",
  "dropOff": "Bến xe Miền Đông",
  "plateNumber": "29A-123.45",
  "departureDate": "2026-04-05",
  "departureTime": "18:00:00",
  "payment": {
    "transactionNo": "TXN-XE-001",
    "amount": 450000,
    "paymentDate": "2026-03-30"
  }
}
```

Example request body (`/bookings/hotels`):

```json
{
  "userId": 1,
  "totalAmount": 2500000,
  "paymentStatus": "PENDING",
  "roomType": "Deluxe Double",
  "checkInDate": "2026-04-05",
  "checkOutDate": "2026-04-07",
  "payment": {
    "transactionNo": "TXN-HOTEL-001",
    "amount": 2500000,
    "paymentDate": "2026-03-30"
  }
}
```

Example request body (`/bookings/restaurants`):

```json
{
  "userId": 1,
  "totalAmount": 500000,
  "paymentStatus": "PENDING",
  "address": "Nhà hàng Tiệp Khắc, Q1, HCM",
  "reservationTime": "19:00:00",
  "numberOfGuests": 4,
  "payment": {
    "transactionNo": "TXN-REST-001",
    "amount": 500000,
    "paymentDate": "2026-03-30"
  }
}
```

## Flight API (Duffel only)

Backend hiện tại chỉ dùng **Duffel** cho luồng tìm chuyến bay.

### Required config

Set these values in `application-dev.yaml` or environment variables:

```yaml
travel:
  flight:
    provider: duffel
    duffel:
      api-key: YOUR_DUFFEL_API_KEY
      base-url: https://api.duffel.com
      timeout-seconds: 30
```

### Endpoints

- `GET /api/v1/flights/search`
- `POST /api/v1/flights/price`
- `POST /api/v1/flights/book`

### Postman flow (Duffel)

1. `GET /api/v1/flights/search`
   - Query params tối thiểu: `originLocationCode`, `destinationLocationCode`, `departureDate`, `adults`
2. Copy `offer id` từ response search
3. `POST /api/v1/flights/price`
   - Body raw JSON: `{ "id": "..." }`
4. `POST /api/v1/flights/book`
   - Body raw JSON: `{ "id": "..." }`

Nếu thiếu `DUFFEL_API_KEY`, API sẽ trả `503` với message: `Chua cau hinh travel.flight.duffel.api-key`.

### Search example

```powershell
curl "http://localhost:8080/api/v1/flights/search?originLocationCode=HAN&destinationLocationCode=SGN&departureDate=2026-04-20&adults=1&currencyCode=VND"
```

### Price example (Duffel)

```json
{
  "id": "PUT_OFFER_ID_HERE"
}
```

### Book example (Duffel)

```json
{
  "id": "PUT_ORDER_ID_HERE"
}
```

Response sẽ trả `orderId`/`status` theo dữ liệu Duffel.

## Payment APIs (VNPay + MoMo sandbox)

### Config

```yaml
travel:
  payment:
    vnpay:
      tmn-code: YOUR_VNPAY_TMN_CODE
      hash-secret: YOUR_VNPAY_HASH_SECRET
      return-url: http://localhost:8080/api/v1/payments/vnpay/callback
    momo:
      partner-code: YOUR_MOMO_PARTNER_CODE
      access-key: YOUR_MOMO_ACCESS_KEY
      secret-key: YOUR_MOMO_SECRET_KEY
      redirect-url: http://localhost:3000/payment-result
      ipn-url: http://localhost:8080/api/v1/payments/momo/webhook
```

### Endpoints

- `POST /api/v1/trips/{tripId}/bookings/{bookingId}/payments/initiate`
- `GET /api/v1/payments/vnpay/callback`
- `GET /api/v1/payments/vnpay/ipn`
- `POST /api/v1/payments/momo/webhook`

### Initiate payment request

```json
{
  "provider": "vnpay",
  "amount": 2500000,
  "orderInfo": "Thanh toan booking flight",
  "returnUrl": "http://localhost:8080/api/v1/payments/vnpay/callback"
}
```

```json
{
  "provider": "momo",
  "amount": 2500000,
  "orderInfo": "Thanh toan booking flight",
  "returnUrl": "http://localhost:3000/payment-result",
  "ipnUrl": "http://localhost:8080/api/v1/payments/momo/webhook"
}
```

Response sẽ chứa `paymentUrl`, frontend redirect user sang URL này để thanh toán sandbox.

### Payment status mapping

- `payments.status`: `PENDING | SUCCESS | FAILED`
- `booking_masters.payment_status`: `PENDING | PAID | FAILED`

Khi callback/webhook hợp lệ và thành công, booking sẽ được cập nhật thành `PAID`.

## Finance APIs

- `PUT /trips/{tripId}/budget`
- `GET /trips/{tripId}/budget`
- `POST /trips/{tripId}/expenses`
- `GET /trips/{tripId}/expenses`
- `POST /trips/{tripId}/expenses/{expenseId}/splits`
- `GET /trips/{tripId}/expenses/{expenseId}/splits`
- `GET /trips/{tripId}/finance/balance`
- `GET /trips/{tripId}/expenses/analytics?groupBy=category`

Example request body (`/budget`):

```json
{
  "category": "food",
  "limitAmount": 3000000
}
```

Example request body (`/expenses`):

```json
{
  "userId": 1,
  "amount": 250000,
  "category": "food",
  "description": "Bua toi ngay 1"
}
```

Example request body (`/expenses/{expenseId}/splits`):

```json
[
  { "userId": 1, "owedAmount": 125000, "isSettled": 1 },
  { "userId": 2, "owedAmount": 125000, "isSettled": 0 }
]
```

## Validation notes

- `tripId` must exist in `trips`
- `userId` is required in booking/expense requests for tracking purposes
- `totalAmount`, `limitAmount`, `amount`, `owedAmount` must be greater than 0
- **Coach booking (xe liên tỉnh)**: requires `name`, `pickUp`, `dropOff`, `departureDate`, `departureTime`
- Expense split total must equal expense amount
- Hotel `checkOutDate` must be after or equal to `checkInDate`

## Run

```powershell
.\mvnw.cmd spring-boot:run
```

## Verify compile

```powershell
.\mvnw.cmd -DskipTests compile
```

## Verify tests

```powershell
.\mvnw.cmd test
```

`src/test/resources/application.yaml` is configured to use H2 in-memory DB for tests.



