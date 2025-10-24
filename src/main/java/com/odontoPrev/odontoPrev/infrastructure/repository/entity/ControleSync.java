package com.odontoPrev.odontoPrev.infrastructure.repository.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * ENTIDADE DE CONTROLE E AUDITORIA DAS SINCRONIZAÇÕES
 * 
 * FUNÇÃO PRINCIPAL:
 * Esta classe representa uma tabela que funciona como um "diário" de todas
 * as sincronizações realizadas com a OdontoPrev. Para cada empresa processada,
 * cria-se um registro aqui com todos os detalhes da operação.
 * 
 * PARA QUE SERVE:
 * 1. AUDITORIA: registrar o que foi feito, quando e por quem
 * 2. RASTREAMENTO: identificar qual empresa teve problema
 * 3. REPROCESSAMENTO: permitir retentar empresas com erro
 * 4. PERFORMANCE: medir tempo de resposta das APIs
 * 5. DEBUG: analisar dados enviados e respostas recebidas
 * 
 * CICLO DE VIDA DE UM REGISTRO:
 * 1. CRIAÇÃO: registro criado com status PENDING
 * 2. PROCESSAMENTO: sistema tenta sincronizar empresa
 * 3. SUCESSO: status vira SUCCESS, salva resposta da API
 * 4. ERRO: status vira ERROR, salva mensagem de erro
 * 
 * EXEMPLO PRÁTICO:
 * Empresa "A001" vai ser sincronizada:
 * 1. Cria registro: codigo="A001", status=PENDING, dataCriacao=agora
 * 2. Envia para API: dadosJson="{dados da empresa}"
 * 3. Se API responde OK: status=SUCCESS, dataSucesso=agora, responseApi="resposta"
 * 4. Se API falha: status=ERROR, erroMensagem="falha na autenticação"
 * 
 * CAMPOS IMPORTANTES:
 * - codigoEmpresa: qual empresa foi processada
 * - dadosJson: exatamente o que foi enviado para OdontoPrev
 * - responseApi: exatamente o que OdontoPrev respondeu
 * - status: PENDING/SUCCESS/ERROR
 * - datas: quando foi criado, quando terminou
 * - erro: se deu problema, qual foi
 * 
 * BENEFÍCIOS PARA OPERAÇÃO:
 * - Pode reprocessar apenas empresas com erro
 * - Identifica padrões de falha
 * - Comprova que sincronização foi feita
 * - Permite análise de performance
 */
@Entity
@Table(name = "TB_CONTROLE_SYNC_ODONTOPREV", schema = "TASY")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ControleSync {

    /**
     * ID ÚNICO DO REGISTRO (CHAVE PRIMÁRIA)
     * Gerado automaticamente pelo banco a cada novo registro
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * CÓDIGO DA EMPRESA QUE FOI PROCESSADA
     * Identifica qual empresa este registro representa
     * Exemplo: "A001", "EMP123"
     */
    @Column(name = "CODIGO_EMPRESA", length = 6, nullable = false)
    private String codigoEmpresa;

    /**
     * TIPO DE OPERAÇÃO REALIZADA
     * Indica se foi criação, atualização ou exclusão na OdontoPrev
     * Valores: CREATE, UPDATE, DELETE
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_OPERACAO", nullable = false)
    private TipoOperacao tipoOperacao;

    /**
     * TIPO DE CONTROLE PARA IDENTIFICAÇÃO
     * 1 = Adição (empresas novas)
     * 2 = Alteração (empresas modificadas)
     * 3 = Exclusão (empresas inativadas/excluídas)
     */
    @Column(name = "TIPO_CONTROLE", nullable = false)
    private Integer tipoControle;

    /**
     * URL DA API QUE FOI CHAMADA
     * Registra exatamente qual endpoint da OdontoPrev foi utilizado
     * Exemplo: "/api/v1/empresas/consultar"
     */
    @Column(name = "ENDPOINT_DESTINO", length = 200, nullable = false)
    private String endpointDestino;

    /**
     * DADOS ENVIADOS PARA A ODONTOPREV (FORMATO JSON)
     * Armazena exatamente quais dados foram enviados na requisição.
     * @Lob permite textos grandes (JSON pode ser extenso)
     */
    @Lob
    @Column(name = "DADOS_JSON")
    private String dadosJson;

    /**
     * STATUS ATUAL DA SINCRONIZAÇÃO
     * PENDING = criado mas ainda não processado
     * SUCCESS = processado com sucesso
     * ERROR = processado mas deu erro
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS_SYNC")
    private StatusSync statusSync;

    /**
     * QUANDO O REGISTRO FOI CRIADO
     * Timestamp de quando a sincronização foi iniciada
     */
    @Column(name = "DATA_CRIACAO")
    private LocalDateTime dataCriacao;

    /**
     * QUANDO A SINCRONIZAÇÃO DEU CERTO
     * Só é preenchido se statusSync = SUCCESS
     * Usado para calcular tempo total de processamento
     */
    @Column(name = "DATA_SUCESSO")
    private LocalDateTime dataSucesso;

    /**
     * MENSAGEM DE ERRO (SE HOUVE FALHA)
     * Só é preenchida se statusSync = ERROR
     * Contém detalhes do que deu errado para investigação
     */
    @Lob
    @Column(name = "ERRO_MENSAGEM")
    private String erroMensagem;

    /**
     * RESPOSTA COMPLETA DA API DA ODONTOPREV
     * Armazena a resposta exata que foi recebida da OdontoPrev
     * Útil para debug e auditoria do que foi retornado
     */
    @Lob
    @Column(name = "RESPONSE_API")
    private String responseApi;

    /**
     * ENUM PARA TIPOS DE OPERAÇÃO
     * 
     * CREATE: Criação de nova empresa na OdontoPrev
     * UPDATE: Atualização de empresa existente
     * DELETE: Exclusão de empresa (raro)
     */
    public enum TipoOperacao {
        CREATE, UPDATE, DELETE
    }

    /**
     * ENUM PARA STATUS DE SINCRONIZAÇÃO
     * 
     * PENDING: Registro criado, aguardando processamento
     * SUCCESS: Processamento concluído com sucesso
     * ERROR: Processamento falhou, ver campo erroMensagem
     */
    public enum StatusSync {
        PENDING, SUCCESS, ERROR
    }

    /**
     * ENUM PARA TIPOS DE CONTROLE
     * 
     * ADICAO: Empresas novas (valor 1)
     * ALTERACAO: Empresas modificadas (valor 2)
     * EXCLUSAO: Empresas inativadas/excluídas (valor 3)
     * PLANOS: Cadastro PME de planos (valor 4)
     */
    public enum TipoControle {
        ADICAO(1),
        ALTERACAO(2),
        EXCLUSAO(3),
        PLANOS(4);

        private final Integer codigo;

        TipoControle(Integer codigo) {
            this.codigo = codigo;
        }

        public Integer getCodigo() {
            return codigo;
        }

        public static TipoControle fromCodigo(Integer codigo) {
            for (TipoControle tipo : values()) {
                if (tipo.codigo.equals(codigo)) {
                    return tipo;
                }
            }
            throw new IllegalArgumentException("Código de tipo de controle inválido: " + codigo);
        }
    }

}