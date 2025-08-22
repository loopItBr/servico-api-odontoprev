package com.odontoPrev.odontoPrev.infrastructure.client;

import com.odontoPrev.odontoPrev.domain.model.Post;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "postClient", url = "https://jsonplaceholder.typicode.com")
public interface PostClient {

    @GetMapping("/posts")
    List<Post> getPosts();
}
