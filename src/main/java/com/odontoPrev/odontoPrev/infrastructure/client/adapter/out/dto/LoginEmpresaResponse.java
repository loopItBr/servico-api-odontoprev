package com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para resposta de login empresa na OdontoPrev
 * 
 * Estrutura real da API:
 * {
 *     "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
 *     "codigoUsuario": "13433638",
 *     "nomeOperador": "SABIN SINAI",
 *     "email": "feodpv01583@odontoprev.com.br",
 *     "empresa": "787392",
 *     "grupoEmpresa": "787392",
 *     "acessos": [],
 *     "usuarioMatriz": "S",
 *     "cpf": "481.402.358-81",
 *     "perfilUsuario": "Master"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginEmpresaResponse {
    
    @JsonProperty("accessToken")
    private String accessToken;
    
    @JsonProperty("codigoUsuario")
    private String codigoUsuario;
    
    @JsonProperty("nomeOperador")
    private String nomeOperador;
    
    @JsonProperty("email")
    private String email;
    
    @JsonProperty("empresa")
    private String empresa;
    
    @JsonProperty("grupoEmpresa")
    private String grupoEmpresa;
    
    @JsonProperty("acessos")
    private List<String> acessos;
    
    @JsonProperty("usuarioMatriz")
    private String usuarioMatriz;
    
    @JsonProperty("cpf")
    private String cpf;
    
    @JsonProperty("perfilUsuario")
    private String perfilUsuario;
}
