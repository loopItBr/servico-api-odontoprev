package com.odontoPrev.odontoPrev.infrastructure.client.service;

import com.odontoPrev.odontoPrev.infrastructure.client.domain.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * SERVIÇO PARA GERENCIAMENTO DE HEADERS DA API ODONTOPREV
 *
 * FUNÇÃO PRINCIPAL:
 * Esta classe centraliza a criação e configuração de headers HTTP necessários
 * para comunicação com todas as APIs da OdontoPrev (empresa e beneficiário).
 *
 * HEADERS OBRIGATÓRIOS:
 * 1. Authorization: Token de autenticação
 * 2. empresa: Código da empresa contratante
 * 3. usuario: Usuário do sistema
 * 4. senha: Senha de acesso
 * 5. app-id: Identificador da aplicação
 *
 * CONFIGURAÇÃO:
 * Todos os valores são obtidos do application.yml através de @Value.
 * Permite diferentes configurações por ambiente (dev, hml, prod).
 *
 * USO:
 * Injetado nos serviços que fazem chamadas para OdontoPrev para
 * obter headers padronizados e centralizados.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OdontoprevApiHeaderService {

    private final TokenService tokenService;

    @Value("${odontoprev.api.empresa:}")
    private String empresa;

    @Value("${odontoprev.api.usuario:}")
    private String usuario;

    @Value("${odontoprev.api.senha:}")
    private String senha;

    @Value("${odontoprev.api.app-id:}")
    private String appId;

    /**
     * OBTÉM TOKEN DE AUTORIZAÇÃO
     *
     * Header Authorization necessário para autenticação na API.
     * Obtém automaticamente um token válido através do TokenService.
     *
     * @return token de autorização formatado como "Bearer {token}"
     */
    public String getAuthorization() {
        try {
            String token = tokenService.obterTokenValido();
            if (token == null || token.trim().isEmpty()) {
                log.error("Token de autorização está vazio ou nulo");
                return "";
            }
            return "Bearer " + token;
        } catch (Exception e) {
            log.error("Erro ao obter token de autorização: {}", e.getMessage(), e);
            return "";
        }
    }

    /**
     * OBTÉM CÓDIGO DA EMPRESA
     *
     * Header empresa identifica a empresa contratante no sistema OdontoPrev.
     *
     * @return código da empresa
     */
    public String getEmpresa() {
        if (empresa == null || empresa.trim().isEmpty()) {
            log.warn("Empresa header não configurado - verificar application.yml");
            return "";
        }
        return empresa;
    }

    /**
     * OBTÉM USUÁRIO DO SISTEMA
     *
     * Header usuario identifica quem está fazendo a requisição.
     *
     * @return nome do usuário
     */
    public String getUsuario() {
        if (usuario == null || usuario.trim().isEmpty()) {
            log.warn("Usuario header não configurado - verificar application.yml");
            return "";
        }
        return usuario;
    }

    /**
     * OBTÉM SENHA DE ACESSO
     *
     * Header senha para autenticação na API.
     *
     * @return senha de acesso
     */
    public String getSenha() {
        if (senha == null || senha.trim().isEmpty()) {
            log.warn("Senha header não configurado - verificar application.yml");
            return "";
        }
        return senha;
    }

    /**
     * OBTÉM ID DA APLICAÇÃO
     *
     * Header app-id identifica a aplicação que está consumindo a API.
     *
     * @return ID da aplicação
     */
    public String getAppId() {
        if (appId == null || appId.trim().isEmpty()) {
            log.warn("App-Id header não configurado - verificar application.yml");
            return "";
        }
        return appId;
    }

    /**
     * VALIDA SE TODOS OS HEADERS OBRIGATÓRIOS ESTÃO CONFIGURADOS
     *
     * Verifica se não há headers faltando para evitar erros de autenticação.
     *
     * @return true se todos os headers estão configurados, false caso contrário
     */
    public boolean validarConfiguracao() {
        boolean valido = true;

        try {
            String token = getAuthorization();
            if (token == null || token.trim().isEmpty()) {
                log.error("CONFIGURAÇÃO FALTANDO: Token de autorização não pôde ser obtido");
                valido = false;
            }
        } catch (Exception e) {
            log.error("CONFIGURAÇÃO FALTANDO: Erro ao obter token de autorização", e);
            valido = false;
        }

        if (empresa == null || empresa.trim().isEmpty()) {
            log.error("CONFIGURAÇÃO FALTANDO: odontoprev.api.empresa");
            valido = false;
        }

        if (usuario == null || usuario.trim().isEmpty()) {
            log.error("CONFIGURAÇÃO FALTANDO: odontoprev.api.usuario");
            valido = false;
        }

        if (senha == null || senha.trim().isEmpty()) {
            log.error("CONFIGURAÇÃO FALTANDO: odontoprev.api.senha");
            valido = false;
        }

        if (appId == null || appId.trim().isEmpty()) {
            log.error("CONFIGURAÇÃO FALTANDO: odontoprev.api.app-id");
            valido = false;
        }

        if (valido) {
            log.debug("Configuração de headers da API OdontoPrev validada com sucesso");
        } else {
            log.error("Configuração de headers da API OdontoPrev INVÁLIDA - verificar application.yml");
        }

        return valido;
    }
}
