package com.mobilebackend.ungdunglapkehoachdulich.service.flight;

import com.mobilebackend.ungdunglapkehoachdulich.dto.flight.FlightSearchReq;

public interface FlightService {
    Object searchFlights(FlightSearchReq req);

    Object priceFlightOffer(String flightOfferPayload);

    Object bookFlight(String bookingRequest);
}

