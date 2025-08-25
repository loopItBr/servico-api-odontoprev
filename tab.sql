CREATE TABLE tb_controle_sync_odontoprev (
    id INT PRIMARY KEY AUTO_INCREMENT,
    codigo_empresa VARCHAR(6) NOT NULL,
    tipo_operacao ENUM('CREATE', 'UPDATE', 'DELETE') NOT NULL,
    endpoint_destino VARCHAR(200) NOT NULL,
    dados_json TEXT,
    status_sync ENUM('PENDING', 'SUCCESS', 'ERROR', 'RETRY') DEFAULT 'PENDING',
    tentativas INT DEFAULT 0,
    max_tentativas INT DEFAULT 3,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_ultima_tentativa TIMESTAMP NULL,
    data_sucesso TIMESTAMP NULL,
    erro_mensagem TEXT NULL,
    response_api TEXT NULL,
    
    INDEX idx_empresa (codigo_empresa),
    INDEX idx_status (status_sync),
    INDEX idx_data_criacao (data_criacao)
);

CREATE VIEW vw_integracao_odontoprev AS
SELECT 
    -- Dados da Empresa
    e.codigo_empresa,
    e.cnpj,
    e.codigo_cliente_operadora,
    e.nome_fantasia,
    e.data_inicio_contrato,
    e.data_fim_contrato,
    e.data_vigencia,
    e.empresa_pf,
    e.codigo_grupo_gerencial,
    e.codigo_marca,
    e.codigo_celula,
    e.vidas_ativas,
    e.valor_ultimo_faturamento,
    e.sinistralidade,
    
    -- Dados dos Planos
    p.codigo_plano,
    p.descricao_plano,
    p.nome_fantasia_plano,
    p.numero_registro_ans,
    p.sigla_plano,
    p.valor_titular,
    p.valor_dependente,
    p.data_inicio_plano,
    p.data_fim_plano,
    p.co_participacao,
    p.tipo_negociacao,
    
    -- Dados de Tipos de Cobrança
    tc.codigo_tipo_cobranca,
    tc.nome_tipo_cobranca,
    tc.sigla_tipo_cobranca,
    tc.numero_banco,
    tc.nome_banco,
    tc.numero_parcelas
    
FROM empresas e
LEFT JOIN planos p ON e.codigo_empresa = p.codigo_empresa
LEFT JOIN tipos_cobranca tc ON e.codigo_empresa = tc.codigo_empresa
WHERE e.ativo = 1 -- Apenas empresas ativas