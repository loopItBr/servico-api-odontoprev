package com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenRequest {
    
    private String grantType = "client_credentials";
}