package com.odontoPrev.odontoPrev.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Post {

    private Integer id;
    private Integer userId;
    private String title;
    private String body;

}
