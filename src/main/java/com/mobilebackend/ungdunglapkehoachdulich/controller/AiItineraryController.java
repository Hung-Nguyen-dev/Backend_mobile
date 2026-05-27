package com.mobilebackend.ungdunglapkehoachdulich.controller;

import com.mobilebackend.ungdunglapkehoachdulich.dto.ai.AiItineraryDtos.AiItineraryRequest;
import com.mobilebackend.ungdunglapkehoachdulich.dto.ai.AiItineraryDtos.AiItineraryResponse;
import com.mobilebackend.ungdunglapkehoachdulich.service.AiItineraryService;
import com.mobilebackend.ungdunglapkehoachdulich.service.CityDataService;
import com.mobilebackend.ungdunglapkehoachdulich.service.CityDataService.CityPlace;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@CrossOrigin(origins = "*")
/**
 * Controller xử lý các API liên quan đến chatbot sinh lịch trình AI.
 * Lớp này chỉ nhận request từ client và chuyển xuống service để xử lý nghiệp vụ.
 */
public class AiItineraryController {

    private final AiItineraryService aiItineraryService;
    private final CityDataService cityDataService;

    public AiItineraryController(AiItineraryService aiItineraryService, CityDataService cityDataService) {
        this.aiItineraryService = aiItineraryService;
        this.cityDataService = cityDataService;
    }

    /** Nhận yêu cầu sinh lịch trình từ frontend và trả về kết quả gợi ý theo ngày. */
    @PostMapping("/itinerary")
    public AiItineraryResponse suggest(@RequestBody AiItineraryRequest req) {
        return aiItineraryService.generate(req);
    }

    /** Cung cấp dữ liệu địa điểm theo thành phố và danh mục để chatbot tra cứu nhanh. */
    @GetMapping("/city-data")
    public List<CityPlace> cityData(
            @RequestParam String destination,
            @RequestParam String category,
            @RequestParam(required = false) String query
    ) {
        return cityDataService.searchPlaces(destination, category, query);
    }
}
