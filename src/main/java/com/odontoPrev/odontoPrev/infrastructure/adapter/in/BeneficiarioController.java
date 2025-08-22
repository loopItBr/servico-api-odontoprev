package com.odontoPrev.odontoPrev.infrastructure.adapter.in;

import com.odontoPrev.odontoPrev.application.dto.DadosBeneficiarioResponse;
import com.odontoPrev.odontoPrev.domain.service.OdontoPrevService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/beneficiarios")
@RequiredArgsConstructor
public class BeneficiarioController {

    private final OdontoPrevService odontoPrevService;

    @GetMapping("/{codigoAssociado}")
    public ResponseEntity<DadosBeneficiarioResponse> getBeneficiario(
            @PathVariable String codigoAssociado) {

        DadosBeneficiarioResponse response = odontoPrevService.getDadosBeneficiario(codigoAssociado);
        return ResponseEntity.ok(response);
    }
}