-- Criação da tabela de controle de sincronização com OdontoPrev
CREATE TABLE tb_controle_sync_odontoprev (
    id INT PRIMARY KEY AUTO_INCREMENT,
    codigo_empresa VARCHAR(6) NOT NULL,
    tipo_operacao ENUM('CREATE', 'UPDATE', 'DELETE', 'GET') NOT NULL,
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
    tempo_resposta_ms BIGINT NULL,
    
    INDEX idx_empresa (codigo_empresa),
    INDEX idx_status (status_sync),
    INDEX idx_data_criacao (data_criacao)
);

-- Criação de tabelas base para demonstração (estas seriam suas tabelas reais)
CREATE TABLE IF NOT EXISTS empresas (
    codigo_empresa VARCHAR(6) PRIMARY KEY,
    cnpj VARCHAR(14),
    codigo_cliente_operadora VARCHAR(20),
    nome_fantasia VARCHAR(255),
    data_inicio_contrato DATE,
    data_fim_contrato DATE,
    data_vigencia DATE,
    empresa_pf BOOLEAN,
    codigo_grupo_gerencial VARCHAR(10),
    codigo_marca VARCHAR(10),
    codigo_celula VARCHAR(10),
    vidas_ativas INT,
    valor_ultimo_faturamento DECIMAL(15,2),
    sinistralidade DECIMAL(5,2),
    ativo BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS planos (
    codigo_plano VARCHAR(10) PRIMARY KEY,
    codigo_empresa VARCHAR(6),
    descricao_plano VARCHAR(255),
    nome_fantasia_plano VARCHAR(255),
    numero_registro_ans VARCHAR(20),
    sigla_plano VARCHAR(10),
    valor_titular DECIMAL(10,2),
    valor_dependente DECIMAL(10,2),
    data_inicio_plano DATE,
    data_fim_plano DATE,
    co_participacao BOOLEAN,
    tipo_negociacao VARCHAR(50),
    
    FOREIGN KEY (codigo_empresa) REFERENCES empresas(codigo_empresa)
);

CREATE TABLE IF NOT EXISTS tipos_cobranca (
    codigo_tipo_cobranca VARCHAR(10) PRIMARY KEY,
    codigo_empresa VARCHAR(6),
    nome_tipo_cobranca VARCHAR(100),
    sigla_tipo_cobranca VARCHAR(5),
    numero_banco VARCHAR(10),
    nome_banco VARCHAR(100),
    numero_parcelas INT,
    
    FOREIGN KEY (codigo_empresa) REFERENCES empresas(codigo_empresa)
);

-- Criação da view de integração
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
WHERE e.ativo = 1;