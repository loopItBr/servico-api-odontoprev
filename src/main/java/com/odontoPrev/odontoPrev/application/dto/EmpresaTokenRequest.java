package com.odontoPrev.odontoPrev.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaTokenRequest {
    private String appld;
    private String login;
    private String senha;
}