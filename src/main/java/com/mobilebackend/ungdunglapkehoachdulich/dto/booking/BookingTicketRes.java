package com.mobilebackend.ungdunglapkehoachdulich.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Booking kèm loại dịch vụ + mã khách đưa cho nhà hàng / khách sạn / xe để đối soát (demo).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingTicketRes {
    private Integer id;
    private Float totalAmount;
    private String paymentStatus;
    private Integer tripId;
    private Integer userId;
    /** FLIGHT, HOTEL, RESTAURANT, COACH */
    private String category;
    /** Vi du ETKT-00000012, HTL-00000012, RST-00000012, BUS-00000012 */
    private String verifyCode;
    private String summaryTitle;
    private String summaryDetail;
}
