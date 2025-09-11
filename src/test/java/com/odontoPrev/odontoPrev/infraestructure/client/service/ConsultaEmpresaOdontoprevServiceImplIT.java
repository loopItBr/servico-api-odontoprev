package com.odontoPrev.odontoPrev.infraestructure.client.service;

import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.EmpresaResponse;
import com.odontoPrev.odontoPrev.infrastructure.client.domain.service.TokenService;
import com.odontoPrev.odontoPrev.infrastructure.client.service.ConsultaEmpresaOdontoprevServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ConsultaEmpresaOdontoprevServiceImplIT {

    @Autowired
    private ConsultaEmpresaOdontoprevServiceImpl consultaEmpresaOdontoprevService;

    @Autowired
    private TokenService tokenService;

    @Test
    void deveChamarApiRealEBuscarEmpresaPorCodigo() {
        String codigoEmpresa = "12345";

        EmpresaResponse response = consultaEmpresaOdontoprevService.buscarEmpresa(codigoEmpresa);

        assertThat(response).isNull();
    }
}
