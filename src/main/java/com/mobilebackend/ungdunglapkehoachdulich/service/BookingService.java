package com.mobilebackend.ungdunglapkehoachdulich.service;

import com.mobilebackend.ungdunglapkehoachdulich.dto.booking.*;
import com.mobilebackend.ungdunglapkehoachdulich.model.*;
import com.mobilebackend.ungdunglapkehoachdulich.repo.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BookingService {
    private final TripRepo tripRepo;
    private final BookingMasterRepo bookingMasterRepo;
    private final BookingFlightRepo bookingFlightRepo;
    private final BookingHotelRepo bookingHotelRepo;
    private final BookingCoachRepo bookingCoachRepo;
    private final BookingRestaurantRepo bookingRestaurantRepo;
    private final PaymentRepo paymentRepo;

    @Transactional
    public BookingMaster createFlightBooking(Integer tripId, FlightBookingReq req) {
        validateCommonBooking(tripId, req.getUserId(), req.getTotalAmount());
        if (isBlank(req.getFlightNumber()) || isBlank(req.getDepartureAirport()) || isBlank(req.getArrivalAirport())) {
            throw new IllegalArgumentException("Thong tin chuyen bay khong hop le");
        }

        BookingMaster bookingMaster = createBookingMaster(tripId, req.getUserId(), req.getTotalAmount(), req.getPaymentStatus());

        BookingFlight bookingFlight = BookingFlight.builder()
                .pnrCode(req.getPnrCode())
                .flightNumber(req.getFlightNumber())
                .departureAirport(req.getDepartureAirport())
                .arrivalAirport(req.getArrivalAirport())
                .departureTime(req.getDepartureTime())
                .arrivalTime(req.getArrivalTime())
                .bookingMasterId(bookingMaster.getId())
                .build();
        bookingFlightRepo.save(bookingFlight);

        savePaymentIfProvided(bookingMaster.getId(), req.getPayment(), req.getTotalAmount());
        return bookingMaster;
    }

    @Transactional
    public BookingMaster createHotelBooking(Integer tripId, HotelBookingReq req) {
        validateCommonBooking(tripId, req.getUserId(), req.getTotalAmount());
        if (isBlank(req.getRoomType()) || req.getCheckInDate() == null || req.getCheckOutDate() == null) {
            throw new IllegalArgumentException("Thong tin khach san khong hop le");
        }
        if (req.getCheckOutDate().isBefore(req.getCheckInDate())) {
            throw new IllegalArgumentException("Ngay check-out phai lon hon hoac bang ngay check-in");
        }

        BookingMaster bookingMaster = createBookingMaster(tripId, req.getUserId(), req.getTotalAmount(), req.getPaymentStatus());

        BookingHotel bookingHotel = BookingHotel.builder()
                .roomType(req.getRoomType())
                .checkInDate(req.getCheckInDate())
                .checkOutDate(req.getCheckOutDate())
                .bookingMasterId(bookingMaster.getId())
                .build();
        bookingHotelRepo.save(bookingHotel);

        savePaymentIfProvided(bookingMaster.getId(), req.getPayment(), req.getTotalAmount());
        return bookingMaster;
    }

    @Transactional
    public BookingMaster createCoachBooking(Integer tripId, CoachBookingReq req) {
        validateCommonBooking(tripId, req.getUserId(), req.getTotalAmount());
        if (isBlank(req.getName()) || isBlank(req.getPickUp()) || isBlank(req.getDropOff())
                || req.getDepartureDate() == null || req.getDepartureTime() == null) {
            throw new IllegalArgumentException("Thong tin ve xe lien tinh khong hop le (can name, pickUp, dropOff, departureDate, departureTime)");
        }

        BookingMaster bookingMaster = createBookingMaster(tripId, req.getUserId(), req.getTotalAmount(), req.getPaymentStatus());

        BookingCoach bookingCoach = BookingCoach.builder()
                .name(req.getName())
                .seat(req.getSeat())
                .pickUp(req.getPickUp())
                .dropOff(req.getDropOff())
                .departureDate(req.getDepartureDate())
                .departureTime(req.getDepartureTime())
                .bookingMasterId(bookingMaster.getId())
                .build();
        bookingCoach.setPlateNumber(req.getPlateNumber());
        bookingCoachRepo.save(bookingCoach);

        savePaymentIfProvided(bookingMaster.getId(), req.getPayment(), req.getTotalAmount());
        return bookingMaster;
    }

    @Transactional
    public BookingMaster createRestaurantBooking(Integer tripId, RestaurantBookingReq req) {
        validateCommonBooking(tripId, req.getUserId(), req.getTotalAmount());
        if (isBlank(req.getAddress()) || req.getReservationTime() == null || req.getNumberOfGuests() == null || req.getNumberOfGuests() <= 0) {
            throw new IllegalArgumentException("Thong tin nha hang khong hop le");
        }

        BookingMaster bookingMaster = createBookingMaster(tripId, req.getUserId(), req.getTotalAmount(), req.getPaymentStatus());

        BookingRestaurant bookingRestaurant = BookingRestaurant.builder()
                .address(req.getAddress())
                .reservationTime(req.getReservationTime())
                .numberOfGuests(req.getNumberOfGuests())
                .bookingMasterId(bookingMaster.getId())
                .build();
        bookingRestaurantRepo.save(bookingRestaurant);

        savePaymentIfProvided(bookingMaster.getId(), req.getPayment(), req.getTotalAmount());
        return bookingMaster;
    }

    public List<BookingMaster> getTripBookings(Integer tripId) {
        if (tripId == null || !tripRepo.existsById(tripId)) {
            throw new IllegalArgumentException("Trip khong ton tai");
        }
        return bookingMasterRepo.findByTripId(tripId);
    }

    @Transactional
    public BookingMaster updatePaymentStatus(Integer tripId, Integer bookingId, String status) {
        if (isBlank(status)) {
            throw new IllegalArgumentException("paymentStatus khong hop le");
        }
        BookingMaster bookingMaster = bookingMasterRepo.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking khong ton tai"));

        if (!Objects.equals(bookingMaster.getTripId(), tripId)) {
            throw new IllegalArgumentException("Booking khong thuoc trip nay");
        }

        bookingMaster.setPaymentStatus(status.toUpperCase(Locale.ROOT));
        return bookingMasterRepo.save(bookingMaster);
    }

    private void validateCommonBooking(Integer tripId, Integer userId, Float totalAmount) {
        if (tripId == null || !tripRepo.existsById(tripId)) {
            throw new IllegalArgumentException("Trip khong ton tai");
        }
        if (userId == null) {
            throw new IllegalArgumentException("Thieu userId");
        }
        if (totalAmount == null || totalAmount <= 0F) {
            throw new IllegalArgumentException("totalAmount phai lon hon 0");
        }
    }

    private BookingMaster createBookingMaster(Integer tripId, Integer userId, Float totalAmount, String paymentStatus) {
        return bookingMasterRepo.save(BookingMaster.builder()
                .tripId(tripId)
                .userId(userId)
                .totalAmount(totalAmount)
                .paymentStatus(isBlank(paymentStatus) ? "PENDING" : paymentStatus.toUpperCase(Locale.ROOT))
                .build());
    }

    private void savePaymentIfProvided(Integer bookingMasterId, BookingPaymentReq paymentReq, Float fallbackAmount) {
        if (paymentReq == null) {
            return;
        }
        Float amount = paymentReq.getAmount();
        if (amount == null || amount <= 0F) {
            amount = fallbackAmount;
        }

        Payment payment = Payment.builder()
                .bookingMasterId(bookingMasterId)
                .transactionNo(paymentReq.getTransactionNo())
                .amount(amount)
                .paymentDate(paymentReq.getPaymentDate() == null ? LocalDate.now() : paymentReq.getPaymentDate())
                .build();
        paymentRepo.save(payment);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}


