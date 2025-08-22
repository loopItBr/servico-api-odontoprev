package com.odontoPrev.odontoPrev.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class EmpresaTokenResponse {
    private String accessToken;
    private String codigoUsuario;
    private String nomeOperador;
    private String email;
    private String empresa;
    private String grupoEmpresa;
    private List<String> acessos;
    private String usuarioMatriz;
    @JsonProperty("isUsuarioMaster")
    private boolean isUsuarioMaster;
}