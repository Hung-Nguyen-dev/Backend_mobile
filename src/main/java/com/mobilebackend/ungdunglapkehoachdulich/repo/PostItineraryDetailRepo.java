package com.mobilebackend.ungdunglapkehoachdulich.repo;

import com.mobilebackend.ungdunglapkehoachdulich.model.PostItineraryDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostItineraryDetailRepo extends JpaRepository<Integer, PostItineraryDetail> {
    void save(PostItineraryDetail postItineraryDetail);
}
