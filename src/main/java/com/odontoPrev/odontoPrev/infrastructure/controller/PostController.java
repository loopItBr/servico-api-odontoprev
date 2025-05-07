package com.odontoPrev.odontoPrev.infrastructure.controller;

import com.odontoPrev.odontoPrev.domain.model.Post;
import com.odontoPrev.odontoPrev.domain.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PostController {
    private final PostService postService;

    @Autowired
    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/posts")
    public List<Post> getPosts(){
        return postService.getPosts();
    }
}
