package com.mobilebackend.ungdunglapkehoachdulich.service;

import com.mobilebackend.ungdunglapkehoachdulich.model.Post;
import com.mobilebackend.ungdunglapkehoachdulich.repo.PostRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepo postRepo;

    List<Post> listPosts(String location){
        if(location == null || location.trim().isEmpty()){
            throw new IllegalArgumentException("location cannot be empty");
        }
        return postRepo.findByLocation(location);
    }
}
