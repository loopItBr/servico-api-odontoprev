package com.odontoPrev.odontoPrev.infrastructure.repository.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "TB_CONTROLE_SYNC_ODONTOPREV", schema = "TASY")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ControleSync {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "CODIGO_EMPRESA", length = 6, nullable = false)
    private String codigoEmpresa;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_OPERACAO", nullable = false)
    private TipoOperacao tipoOperacao;

    @Column(name = "ENDPOINT_DESTINO", length = 200, nullable = false)
    private String endpointDestino;

    @Lob
    @Column(name = "DADOS_JSON")
    private String dadosJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS_SYNC")
    private StatusSync statusSync;

    @Column(name = "DATA_CRIACAO")
    private LocalDateTime dataCriacao;

    @Column(name = "DATA_SUCESSO")
    private LocalDateTime dataSucesso;

    @Lob
    @Column(name = "ERRO_MENSAGEM")
    private String erroMensagem;

    @Lob
    @Column(name = "RESPONSE_API")
    private String responseApi;

    public enum TipoOperacao {
        CREATE, UPDATE, DELETE
    }

    public enum StatusSync {
        PENDING, SUCCESS, ERROR
    }
}