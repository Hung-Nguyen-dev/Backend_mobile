package com.mobilebackend.ungdunglapkehoachdulich.repo;

import com.mobilebackend.ungdunglapkehoachdulich.model.TripMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripMemberRepo extends JpaRepository<TripMember, Integer> {

    /**
     * Tìm thành viên theo chuyến đi và user (dùng để kiểm tra trùng lặp)
     */
    Optional<TripMember> findByTripIdAndUserId(Integer tripId, Integer userId);

    /**
     * Lấy danh sách tất cả thành viên của một chuyến đi
     */
    List<TripMember> findByTripId(Integer tripId);

    /**
     * Lấy danh sách lời mời đang chờ xác nhận của một user (status = 0)
     */
    List<TripMember> findByUserIdAndStatus(Integer userId, Integer status);
}
