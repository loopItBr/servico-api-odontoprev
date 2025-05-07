package com.odontoPrev.odontoPrev.domain.service;

import com.odontoPrev.odontoPrev.domain.model.Post;
import com.odontoPrev.odontoPrev.infrastructure.client.PostClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostService {

    private PostClient postClient;

    @Autowired
    public PostService(PostClient jsonClient){
        this.postClient = jsonClient;
    }

    public List<Post> getPosts(){
        return postClient.getPosts();
    }
}
