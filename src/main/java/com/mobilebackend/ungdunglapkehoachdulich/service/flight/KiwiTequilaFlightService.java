package com.mobilebackend.ungdunglapkehoachdulich.service.flight;

import com.mobilebackend.ungdunglapkehoachdulich.dto.flight.FlightSearchReq;
import org.springframework.stereotype.Service;

@Service
public class KiwiTequilaFlightService implements FlightService {
    private static final String MESSAGE = "Kiwi da bi loai bo. Hien tai backend chi ho tro Duffel.";

    @Override
    public Object searchFlights(FlightSearchReq req) {
        throw new IllegalStateException(MESSAGE);
    }

    @Override
    public Object priceFlightOffer(String flightOfferPayload) {
        throw new IllegalStateException(MESSAGE);
    }

    @Override
    public Object bookFlight(String bookingRequest) {
        throw new IllegalStateException(MESSAGE);
    }
}

