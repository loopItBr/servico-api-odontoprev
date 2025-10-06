package com.odontoPrev.odontoPrev.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * ENTIDADE PARA CONTROLE DE SINCRONIZAÇÃO DE BENEFICIÁRIOS COM ODONTOPREV
 *
 * Esta classe armazena o histórico e controle de todas as tentativas
 * de sincronização (inclusão, alteração, exclusão) de beneficiários
 * com a API da OdontoPrev.
 *
 * QUANDO É USADA:
 * - Ao registrar tentativa de envio de beneficiário para OdontoPrev
 * - Ao armazenar resposta da API (sucesso ou erro)
 * - Ao fazer retry de operações que falharam
 * - Para auditoria e rastreabilidade das integrações
 *
 * CAMPOS PRINCIPAIS:
 * - statusSync: status da sincronização (PENDING, SUCCESS, ERROR)
 * - tentativas: número de tentativas já realizadas
 * - tipoOperacao: tipo de operação (INCLUSAO, ALTERACAO, EXCLUSAO)
 * - dadosJson: payload JSON enviado para a API
 * - responseApi: resposta retornada pela API da OdontoPrev
 */
@Entity
@Table(name = "TB_CONTROLE_SYNC_ODONTOPREV_BENEF", schema = "TASY")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ControleSyncBeneficiario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    /**
     * Código da empresa (vinculação do beneficiário)
     */
    @Column(name = "CODIGO_EMPRESA", length = 6, nullable = false)
    private String codigoEmpresa;

    /**
     * Código do beneficiário (matrícula)
     */
    @Column(name = "CODIGO_BENEFICIARIO", length = 15, nullable = false)
    private String codigoBeneficiario;

    /**
     * Tipo de log (I = Inclusão, A = Alteração, E = Exclusão)
     */
    @Column(name = "TIPO_LOG", length = 1, nullable = false)
    private String tipoLog;

    /**
     * Tipo da operação realizada
     * Valores possíveis: INCLUSAO, ALTERACAO, EXCLUSAO
     */
    @Column(name = "TIPO_OPERACAO", length = 10, nullable = false)
    private String tipoOperacao;

    /**
     * Endpoint da API OdontoPrev chamado
     */
    @Column(name = "ENDPOINT_DESTINO", length = 200)
    private String endpointDestino;

    /**
     * Dados JSON enviados para a API (payload completo)
     */
    @Lob
    @Column(name = "DADOS_JSON", columnDefinition = "CLOB")
    private String dadosJson;

    /**
     * Status da sincronização
     * Valores possíveis: PENDING, SUCCESS, ERROR
     */
    @Column(name = "STATUS_SYNC", length = 10, nullable = false)
    @Builder.Default
    private String statusSync = "PENDING";

    /**
     * Número de tentativas já realizadas
     */
    @Column(name = "TENTATIVAS", nullable = false)
    @Builder.Default
    private Integer tentativas = 0;

    /**
     * Número máximo de tentativas permitidas
     */
    @Column(name = "MAX_TENTATIVAS", nullable = false)
    @Builder.Default
    private Integer maxTentativas = 3;

    /**
     * Data e hora de criação do registro
     */
    @CreationTimestamp
    @Column(name = "DATA_CRIACAO", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    /**
     * Data e hora da última tentativa de sincronização
     */
    @Column(name = "DATA_ULTIMA_TENTATIVA")
    private LocalDateTime dataUltimaTentativa;

    /**
     * Data e hora do sucesso da sincronização
     */
    @Column(name = "DATA_SUCESSO")
    private LocalDateTime dataSucesso;

    /**
     * Mensagem de erro (quando houver falha)
     */
    @Lob
    @Column(name = "ERRO_MENSAGEM", columnDefinition = "CLOB")
    private String erroMensagem;

    /**
     * Resposta completa da API OdontoPrev
     */
    @Lob
    @Column(name = "RESPONSE_API", columnDefinition = "CLOB")
    private String responseApi;
}
