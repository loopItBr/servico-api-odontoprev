package com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para requisição de login empresa na OdontoPrev
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginEmpresaRequest {
    
    @JsonProperty("appId")
    private String appId;
    
    @JsonProperty("login")
    private String login;
    
    @JsonProperty("senha")
    private String senha;
}
