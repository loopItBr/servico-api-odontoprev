-- Inserção de dados de exemplo para teste da sincronização

-- Empresas de exemplo
INSERT INTO empresas (codigo_empresa, cnpj, codigo_cliente_operadora, nome_fantasia, 
                     data_inicio_contrato, data_fim_contrato, data_vigencia, empresa_pf,
                     codigo_grupo_gerencial, codigo_marca, codigo_celula, vidas_ativas,
                     valor_ultimo_faturamento, sinistralidade, ativo) 
VALUES 
('EMP001', '12345678901234', 'CLI001', 'Empresa Alpha Ltda', 
 '2024-01-01', '2024-12-31', '2024-01-01', FALSE,
 'GG01', 'MAR01', 'CEL01', 150,
 25000.00, 0.85, TRUE),

('EMP002', '98765432109876', 'CLI002', 'Beta Corporação S.A.', 
 '2024-02-01', '2024-12-31', '2024-02-01', FALSE,
 'GG02', 'MAR02', 'CEL02', 300,
 45000.00, 0.72, TRUE),

('EMP003', '11111111111111', 'CLI003', 'Gamma Negócios ME', 
 '2024-03-01', '2024-12-31', '2024-03-01', TRUE,
 'GG01', 'MAR01', 'CEL03', 50,
 8000.00, 0.90, TRUE);

-- Planos de exemplo
INSERT INTO planos (codigo_plano, codigo_empresa, descricao_plano, nome_fantasia_plano,
                   numero_registro_ans, sigla_plano, valor_titular, valor_dependente,
                   data_inicio_plano, data_fim_plano, co_participacao, tipo_negociacao)
VALUES 
('PLN001', 'EMP001', 'Plano Básico Odontológico', 'Básico Plus', 
 'ANS123456', 'BP', 89.90, 45.50,
 '2024-01-01', '2024-12-31', FALSE, 'Pré-pagamento'),

('PLN002', 'EMP001', 'Plano Premium Odontológico', 'Premium Gold', 
 'ANS789012', 'PG', 149.90, 85.50,
 '2024-01-01', '2024-12-31', TRUE, 'Pré-pagamento'),

('PLN003', 'EMP002', 'Plano Empresarial Standard', 'Standard Corp', 
 'ANS345678', 'SC', 120.00, 60.00,
 '2024-02-01', '2024-12-31', FALSE, 'Pós-pagamento'),

('PLN004', 'EMP003', 'Plano Micro Empresa', 'Micro Dental', 
 'ANS901234', 'MD', 75.00, 35.00,
 '2024-03-01', '2024-12-31', FALSE, 'Pré-pagamento');

-- Tipos de cobrança de exemplo
INSERT INTO tipos_cobranca (codigo_tipo_cobranca, codigo_empresa, nome_tipo_cobranca,
                           sigla_tipo_cobranca, numero_banco, nome_banco, numero_parcelas)
VALUES 
('TC001', 'EMP001', 'Débito Automático Mensal', 'DAM', '341', 'Itaú', 12),
('TC002', 'EMP001', 'Boleto Bancário', 'BOL', '237', 'Bradesco', 6),
('TC003', 'EMP002', 'Transferência Eletrônica', 'TEF', '001', 'Banco do Brasil', 12),
('TC004', 'EMP003', 'Cartão de Crédito', 'CC', '104', 'Caixa Econômica', 1);