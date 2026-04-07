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



