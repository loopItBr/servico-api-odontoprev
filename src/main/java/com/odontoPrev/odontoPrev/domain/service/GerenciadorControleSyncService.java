package com.odontoPrev.odontoPrev.domain.service;

import com.odontoPrev.odontoPrev.infrastructure.repository.entity.ControleSync;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprev;

/**
 * Interface responsável pelo gerenciamento do controle de sincronização.
 */
public interface GerenciadorControleSyncService {
    
    /**
     * Cria um novo controle de sincronização.
     * 
     * @param codigoEmpresa código da empresa
     * @param dados dados da integração
     * @return controle de sincronização criado
     */
    ControleSync criarControle(String codigoEmpresa, IntegracaoOdontoprev dados);
    
    /**
     * Atualiza o controle com sucesso.
     * 
     * @param controle controle a ser atualizado
     * @param responseJson resposta da API
     * @param tempoResposta tempo de resposta em millisegundos
     */
    void atualizarSucesso(ControleSync controle, String responseJson, long tempoResposta);
    
    /**
     * Atualiza o controle com erro.
     * 
     * @param controle controle a ser atualizado
     * @param mensagemErro mensagem de erro
     */
    void atualizarErro(ControleSync controle, String mensagemErro);
    
    /**
     * Salva o controle de sincronização.
     * 
     * @param controle controle a ser salvo
     * @return controle salvo
     */
    ControleSync salvar(ControleSync controle);
}