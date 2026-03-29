package com.mobilebackend.ungdunglapkehoachdulich.controller;

import com.mobilebackend.ungdunglapkehoachdulich.model.Post;
import com.mobilebackend.ungdunglapkehoachdulich.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping("/list/{location}")
    public ResponseEntity<?> getListPost(@PathVariable String location) {
        try {
            List<Post> postList = postService.listPosts(location);
            if (postList == null || postList.isEmpty()) {
                return ResponseEntity
                        .badRequest()
                        .body("khong tim thay dia diem voi vi tri tren");
            }
            return ResponseEntity.ok(postList);

        } catch (Exception e) {
            return ResponseEntity
                    .internalServerError()
                    .body("loi he thong");
        }
    }
}
