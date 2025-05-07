package com.odontoPrev.odontoPrev.infrastructure.client;


import com.odontoPrev.odontoPrev.domain.model.Post;
import com.odontoPrev.odontoPrev.infrastructure.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "jsonplaceholder", url = "${api.url}",configuration = FeignConfig.class)
public interface PostClient {

    @GetMapping("/posts")
    List<Post> getPosts();
}
