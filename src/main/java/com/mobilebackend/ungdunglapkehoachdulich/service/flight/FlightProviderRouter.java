package com.mobilebackend.ungdunglapkehoachdulich.service.flight;

import com.mobilebackend.ungdunglapkehoachdulich.dto.flight.FlightSearchReq;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
@RequiredArgsConstructor
public class FlightProviderRouter implements FlightService {
    private final DuffelFlightService duffelFlightService;

    @Override
    public Object searchFlights(FlightSearchReq req) {
        return provider().searchFlights(req);
    }

    @Override
    public Object priceFlightOffer(String flightOfferPayload) {
        return provider().priceFlightOffer(flightOfferPayload);
    }

    @Override
    public Object bookFlight(String bookingRequest) {
        return duffelFlightService.bookFlight(bookingRequest);
    }

    private FlightService provider() {
        return duffelFlightService;
    }
}


