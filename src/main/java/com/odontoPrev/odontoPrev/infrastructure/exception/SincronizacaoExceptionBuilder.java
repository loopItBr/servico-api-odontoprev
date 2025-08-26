package com.odontoPrev.odontoPrev.infrastructure.exception;

/**
 * Builder para criar exceções de sincronização de forma fluente e simplificada.
 */
public class SincronizacaoExceptionBuilder {
    
    private ContextoSincronizacao contexto;
    private String detalhes;
    private Throwable causa;
    
    private SincronizacaoExceptionBuilder() {
        this.contexto = ContextoSincronizacao.criar();
    }
    
    public static SincronizacaoExceptionBuilder criar() {
        return new SincronizacaoExceptionBuilder();
    }
    
    public SincronizacaoExceptionBuilder comContexto(ContextoSincronizacao contexto) {
        this.contexto = contexto;
        return this;
    }
    
    public SincronizacaoExceptionBuilder comDetalhes(String detalhes) {
        this.detalhes = detalhes;
        return this;
    }
    
    public SincronizacaoExceptionBuilder comCausa(Throwable causa) {
        this.causa = causa;
        if (causa != null) {
            this.contexto.comErro((Exception) causa);
        }
        return this;
    }
    
    public SincronizacaoExceptionBuilder com(String chave, Object valor) {
        this.contexto.com(chave, valor);
        return this;
    }
    
    // Métodos para criar exceções específicas
    
    public InicializacaoSchedulerException inicializacaoScheduler() {
        return new InicializacaoSchedulerException(
            detalhes != null ? detalhes : "Falha na inicialização",
            contexto.paraMapa(),
            causa
        );
    }
    
    public ControleExecucaoException controleExecucao() {
        return new ControleExecucaoException(
            detalhes != null ? detalhes : "Falha no controle de execução",
            contexto.paraMapa(),
            causa
        );
    }
    
    public ConsultaEmpresasException consultaEmpresas() {
        return new ConsultaEmpresasException(
            detalhes != null ? detalhes : "Falha na consulta de empresas",
            contexto.paraMapa(),
            causa
        );
    }
    
    public ProcessamentoLoteException processamentoLote() {
        return new ProcessamentoLoteException(
            detalhes != null ? detalhes : "Falha no processamento de lote",
            contexto.paraMapa(),
            causa
        );
    }
    
    public ProcessamentoEmpresaException processamentoEmpresa(String codigoEmpresa) {
        contexto.com("codigoEmpresa", codigoEmpresa);
        return new ProcessamentoEmpresaException(
            codigoEmpresa,
            detalhes != null ? detalhes : "Falha no processamento da empresa",
            contexto.paraMapa(),
            causa
        );
    }
    
    public ConfiguracaoSincronizacaoException configuracao(String parametro) {
        contexto.com("parametro", parametro);
        return new ConfiguracaoSincronizacaoException(
            parametro,
            detalhes != null ? detalhes : "Configuração inválida",
            contexto.paraMapa(),
            causa
        );
    }
    
    public RecursosIndisponiveisException recursosIndisponiveis(String recurso) {
        contexto.com("recurso", recurso);
        return new RecursosIndisponiveisException(
            recurso,
            detalhes != null ? detalhes : "Recurso indisponível",
            contexto.paraMapa(),
            causa
        );
    }
    
    public AutenticacaoOdontoprevException autenticacao() {
        return new AutenticacaoOdontoprevException(
            detalhes != null ? detalhes : "Falha na autenticação",
            contexto.paraMapa(),
            causa
        );
    }
    
    public ComunicacaoApiOdontoprevException comunicacaoApi(String endpoint) {
        contexto.com("endpoint", endpoint);
        if (causa instanceof feign.FeignException) {
            feign.FeignException fe = (feign.FeignException) causa;
            return new ComunicacaoApiOdontoprevException(
                endpoint,
                fe.status(),
                detalhes != null ? detalhes : "Falha na comunicação",
                contexto.paraMapa()
            );
        }
        return new ComunicacaoApiOdontoprevException(
            endpoint,
            detalhes != null ? detalhes : "Falha na comunicação",
            contexto.paraMapa(),
            causa
        );
    }
}