package com.odontoPrev.odontoPrev.infrastructure.client.service;

import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.EmpresaPmeRequest;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprev;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * IMPLEMENTAÇÃO DO SERVIÇO PME
 * 
 * Converte dados da view VW_INTEGRACAO_ODONTOPREV para o formato
 * necessário do endpoint PME da OdontoPrev.
 * 
 * FUNCIONALIDADES PRINCIPAIS:
 * - Mapeamento de campos da view para request PME
 * - Suporte a múltiplos planos (CODIGO_PLANO_1, 2, 3)
 * - Valores padrão para campos obrigatórios
 * - Tratamento de datas e formatação
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmpresaPmeServiceImpl implements EmpresaPmeService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    @Override
    public EmpresaPmeRequest converterParaRequestPme(IntegracaoOdontoprev dadosEmpresa) {
        log.info("🔄 [CONVERSÃO PME] Iniciando conversão para request PME - Empresa: {}", dadosEmpresa.getNomeFantasia());
        
        // CAMPOS BÁSICOS DA EMPRESA
        String cgc = dadosEmpresa.getCnpj();
        String razaoSocial = dadosEmpresa.getNomeFantasia(); // Usando nome fantasia como razão social
        String nomeFantasia = dadosEmpresa.getNomeFantasia();
        
        // DATAS - Converter para formato ISO
        String dataInicioContrato = formatarDataLocalDate(dadosEmpresa.getDataInicioContrato());
        String dataVigencia = formatarDataLocalDate(dadosEmpresa.getDataVigencia());
        
        log.info("📋 [CONVERSÃO PME] Dados básicos - CNPJ: {}, Nome: {}, Data Início: {}", 
                cgc, nomeFantasia, dataInicioContrato);
        
        // PLANOS - Suporte a múltiplos planos
        List<EmpresaPmeRequest.PlanoPme> planos = criarPlanos(dadosEmpresa);
        log.info("📋 [CONVERSÃO PME] Planos criados: {} planos", planos.size());
        
        // CONTATOS
        List<EmpresaPmeRequest.ContatoPme> contatos = criarContatos();
        List<EmpresaPmeRequest.ContatoFatura> contatosDaFatura = criarContatosDaFatura();
        
        // GRUPOS E GRAUS DE PARENTESCO
        List<EmpresaPmeRequest.GrupoPme> grupos = criarGrupos();
        List<EmpresaPmeRequest.GrauParentesco> grausParentesco = criarGrausParentesco();
        
        // ENDEREÇO E COBRANÇA
        EmpresaPmeRequest.EnderecoPme endereco = criarEndereco();
        EmpresaPmeRequest.CobrancaPme cobranca = criarCobranca(cgc, razaoSocial);
        
        // COMISSIONAMENTOS
        List<EmpresaPmeRequest.Comissionamento> comissionamentos = criarComissionamentos();
        
        // CONSTRUIR REQUEST
        EmpresaPmeRequest request = EmpresaPmeRequest.builder()
                .cgc(cgc)
                .razaoSocial(razaoSocial)
                .nomeFantasia(nomeFantasia)
                .dataInicioContrato(dataInicioContrato)
                .dataVigencia(dataVigencia)
                .planos(planos)
                .contatos(contatos)
                .contatosDaFatura(contatosDaFatura)
                .grupos(grupos)
                .grausParentesco(grausParentesco)
                .endereco(endereco)
                .cobranca(cobranca)
                .comissionamentos(comissionamentos)
                .build();
        
        log.info("✅ [CONVERSÃO PME] Request PME criado com sucesso - {} planos, {} contatos", 
                planos.size(), contatos.size());
        
        return request;
    }

    /**
     * CRIA LISTA DE PLANOS COM BASE NOS DADOS DA VIEW
     * 
     * Suporta múltiplos planos baseado nos campos CODIGO_PLANO_1, 2, 3
     * da view VW_INTEGRACAO_ODONTOPREV.
     */
    private List<EmpresaPmeRequest.PlanoPme> criarPlanos(IntegracaoOdontoprev dadosEmpresa) {
        List<EmpresaPmeRequest.PlanoPme> planos = new ArrayList<>();
        
        // PLANO 1 - Sempre presente
        if (dadosEmpresa.getCodigoPlano1() != null) {
            EmpresaPmeRequest.PlanoPme plano1 = EmpresaPmeRequest.PlanoPme.builder()
                    .codigoPlano(String.valueOf(dadosEmpresa.getCodigoPlano1()))
                    .dataInicioPlano(formatarDataLocalDate(dadosEmpresa.getDataInicioPlano1()))
                    .valorTitular(converterStringParaDouble(dadosEmpresa.getValorTitular1()))
                    .valorDependente(converterStringParaDouble(dadosEmpresa.getValorDependente1()))
                    .redes(criarRedes())
                    .build();
            planos.add(plano1);
            log.info("📋 [PLANOS] Plano 1 adicionado: {}", dadosEmpresa.getCodigoPlano1());
        }
        
        // PLANO 2 - Se presente
        if (dadosEmpresa.getCodigoPlano2() != null) {
            EmpresaPmeRequest.PlanoPme plano2 = EmpresaPmeRequest.PlanoPme.builder()
                    .codigoPlano(String.valueOf(dadosEmpresa.getCodigoPlano2()))
                    .dataInicioPlano(formatarDataLocalDate(dadosEmpresa.getDataInicioPlano2()))
                    .valorTitular(converterStringParaDouble(dadosEmpresa.getValorTitular2()))
                    .valorDependente(converterStringParaDouble(dadosEmpresa.getValorDependente2()))
                    .redes(criarRedes())
                    .build();
            planos.add(plano2);
            log.info("📋 [PLANOS] Plano 2 adicionado: {}", dadosEmpresa.getCodigoPlano2());
        }
        
        // PLANO 3 - Se presente
        if (dadosEmpresa.getCodigoPlano3() != null) {
            EmpresaPmeRequest.PlanoPme plano3 = EmpresaPmeRequest.PlanoPme.builder()
                    .codigoPlano(String.valueOf(dadosEmpresa.getCodigoPlano3()))
                    .dataInicioPlano(formatarDataLocalDate(dadosEmpresa.getDataInicioPlano3()))
                    .valorTitular(converterStringParaDouble(dadosEmpresa.getValorTitular3()))
                    .valorDependente(converterStringParaDouble(dadosEmpresa.getValorDependente3()))
                    .redes(criarRedes())
                    .build();
            planos.add(plano3);
            log.info("📋 [PLANOS] Plano 3 adicionado: {}", dadosEmpresa.getCodigoPlano3());
        }
        
        // Se nenhum plano foi encontrado, criar um plano padrão
        if (planos.isEmpty()) {
            log.warn("⚠️ [PLANOS] Nenhum plano encontrado na view - criando plano padrão");
            EmpresaPmeRequest.PlanoPme planoPadrao = EmpresaPmeRequest.PlanoPme.builder()
                    .codigoPlano("9972") // Plano padrão
                    .dataInicioPlano(formatarData(LocalDateTime.now()))
                    .valorTitular(0.0)
                    .valorDependente(0.0)
                    .redes(criarRedes())
                    .build();
            planos.add(planoPadrao);
        }
        
        return planos;
    }

    /**
     * CRIA REDES PADRÃO PARA OS PLANOS
     */
    private List<EmpresaPmeRequest.RedePme> criarRedes() {
        List<EmpresaPmeRequest.RedePme> redes = new ArrayList<>();
        redes.add(EmpresaPmeRequest.RedePme.builder().codigoRede("1").build());
        return redes;
    }

    /**
     * CRIA CONTATOS PADRÃO
     */
    private List<EmpresaPmeRequest.ContatoPme> criarContatos() {
        List<EmpresaPmeRequest.ContatoPme> contatos = new ArrayList<>();
        
        contatos.add(EmpresaPmeRequest.ContatoPme.builder()
                .cargo("Gerente")
                .nome("Contato Principal")
                .email("contato@empresa.com")
                .idCorretor("N")
                .telefone(EmpresaPmeRequest.TelefonePme.builder()
                        .telefone1("(32) 99999-9999")
                        .celular("(32) 99999-9999")
                        .build())
                .build());
        
        return contatos;
    }

    /**
     * CRIA CONTATOS DA FATURA
     */
    private List<EmpresaPmeRequest.ContatoFatura> criarContatosDaFatura() {
        List<EmpresaPmeRequest.ContatoFatura> contatos = new ArrayList<>();
        
        contatos.add(EmpresaPmeRequest.ContatoFatura.builder()
                .codSequencial(1)
                .email("fatura@empresa.com")
                .nomeContato("Contato Fatura")
                .relatorio(true)
                .build());
        
        return contatos;
    }

    /**
     * CRIA GRUPOS PADRÃO
     */
    private List<EmpresaPmeRequest.GrupoPme> criarGrupos() {
        List<EmpresaPmeRequest.GrupoPme> grupos = new ArrayList<>();
        grupos.add(EmpresaPmeRequest.GrupoPme.builder().codigoGrupo(1).build());
        return grupos;
    }

    /**
     * CRIA GRAUS DE PARENTESCO PADRÃO
     */
    private List<EmpresaPmeRequest.GrauParentesco> criarGrausParentesco() {
        List<EmpresaPmeRequest.GrauParentesco> graus = new ArrayList<>();
        graus.add(EmpresaPmeRequest.GrauParentesco.builder().codigoGrauParentesco("1").build());
        graus.add(EmpresaPmeRequest.GrauParentesco.builder().codigoGrauParentesco("2").build());
        graus.add(EmpresaPmeRequest.GrauParentesco.builder().codigoGrauParentesco("3").build());
        return graus;
    }

    /**
     * CRIA ENDEREÇO PADRÃO
     */
    private EmpresaPmeRequest.EnderecoPme criarEndereco() {
        return EmpresaPmeRequest.EnderecoPme.builder()
                .cidade(EmpresaPmeRequest.CidadePme.builder().build())
                .build();
    }

    /**
     * CRIA DADOS DE COBRANÇA
     */
    private EmpresaPmeRequest.CobrancaPme criarCobranca(String cgc, String razaoSocial) {
        return EmpresaPmeRequest.CobrancaPme.builder()
                .nome(razaoSocial)
                .cgc(cgc)
                .endereco(criarEndereco())
                .build();
    }

    /**
     * CRIA COMISSIONAMENTOS PADRÃO
     */
    private List<EmpresaPmeRequest.Comissionamento> criarComissionamentos() {
        List<EmpresaPmeRequest.Comissionamento> comissionamentos = new ArrayList<>();
        comissionamentos.add(EmpresaPmeRequest.Comissionamento.builder()
                .cnpjCorretor("05281511000142")
                .codigoRegra(0)
                .build());
        return comissionamentos;
    }

    /**
     * FORMATA DATA PARA FORMATO ISO
     */
    private String formatarData(LocalDateTime data) {
        if (data == null) {
            return formatarData(LocalDateTime.now());
        }
        return data.format(DATE_FORMATTER);
    }

    /**
     * FORMATA LOCALDATE PARA FORMATO ISO
     */
    private String formatarDataLocalDate(LocalDate data) {
        if (data == null) {
            return formatarData(LocalDateTime.now());
        }
        return data.atStartOfDay().format(DATE_FORMATTER);
    }

    /**
     * CONVERTE STRING PARA DOUBLE
     */
    private Double converterStringParaDouble(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(valor.replace(",", "."));
        } catch (NumberFormatException e) {
            log.warn("⚠️ [CONVERSÃO] Erro ao converter valor '{}' para double: {}", valor, e.getMessage());
            return 0.0;
        }
    }
}
