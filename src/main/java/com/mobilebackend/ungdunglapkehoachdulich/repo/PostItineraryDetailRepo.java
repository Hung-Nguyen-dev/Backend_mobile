package com.mobilebackend.ungdunglapkehoachdulich.repo;

import com.mobilebackend.ungdunglapkehoachdulich.model.PostItineraryDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostItineraryDetailRepo extends JpaRepository<PostItineraryDetail, Integer> {

    /** Lấy tất cả địa điểm (post) liên kết với một hoạt động */
    List<PostItineraryDetail> findByItineraryDetailId(Integer itineraryDetailId);

    /** Tìm bản ghi theo id và userId (dùng để kiểm tra quyền cập nhật) */
    Optional<PostItineraryDetail> findByIdAndUserId(Integer id, Integer userId);
}
