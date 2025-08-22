package com.odontoPrev.odontoPrev.infrastructure.repository.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_controle_sync_odontoprev")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ControleSync {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "codigo_empresa", length = 6, nullable = false)
    private String codigoEmpresa;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_operacao", nullable = false)
    private TipoOperacao tipoOperacao;

    @Column(name = "endpoint_destino", length = 200, nullable = false)
    private String endpointDestino;

    @Lob
    @Column(name = "dados_json")
    private String dadosJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_sync")
    private StatusSync statusSync;

    @Column(name = "tentativas")
    private Integer tentativas;

    @Column(name = "max_tentativas")
    private Integer maxTentativas;

    @Column(name = "data_criacao")
    private LocalDateTime dataCriacao;

    @Column(name = "data_ultima_tentativa")
    private LocalDateTime dataUltimaTentativa;

    @Column(name = "data_sucesso")
    private LocalDateTime dataSucesso;

    @Lob
    @Column(name = "erro_mensagem")
    private String erroMensagem;

    @Lob
    @Column(name = "response_api")
    private String responseApi;

    // Novo campo adicionado para o tempo de resposta
    @Column(name = "tempo_resposta_ms")
    private Long tempoRespostaMs;

    public enum TipoOperacao {
        CREATE, UPDATE, DELETE, GET
    }

    public enum StatusSync {
        PENDING, SUCCESS, ERROR, RETRY
    }
}