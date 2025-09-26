package com.odontoPrev.odontoPrev.domain.service;

import com.odontoPrev.odontoPrev.infrastructure.repository.entity.ControleSync;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprev;

/**
 * Interface responsável pelo gerenciamento do controle de sincronização.
 */
public interface  GerenciadorControleSyncService {
    
    /**
     * Cria um novo controle de sincronização.
     * 
     * @param codigoEmpresa código da empresa
     * @param dados dados da integração
     * @return controle de sincronização criado
     */
    ControleSync criarControle(String codigoEmpresa, IntegracaoOdontoprev dados);
    
    /**
     * Cria um controle de sincronização para operação específica.
     * 
     * @param codigoEmpresa código da empresa
     * @param dados dados da integração
     * @param tipoOperacao tipo da operação (CREATE, UPDATE, DELETE)
     * @return controle de sincronização criado
     */
    ControleSync criarControle(String codigoEmpresa, IntegracaoOdontoprev dados, ControleSync.TipoOperacao tipoOperacao);
    
    /**
     * Cria um controle de sincronização com tipo de controle específico.
     * 
     * @param codigoEmpresa código da empresa
     * @param dados dados da integração
     * @param tipoOperacao tipo da operação (CREATE, UPDATE, DELETE)
     * @param tipoControle tipo de controle (1=Adição, 2=Alteração, 3=Exclusão)
     * @return controle de sincronização criado
     */
    ControleSync criarControle(String codigoEmpresa, IntegracaoOdontoprev dados, ControleSync.TipoOperacao tipoOperacao, ControleSync.TipoControle tipoControle);
    
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