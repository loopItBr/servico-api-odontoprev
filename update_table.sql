-- Script para atualizar a tabela TB_CONTROLE_SYNC_ODONTOPREV (Oracle)
-- Adiciona o campo tipo_controle para identificar se foi adição, alteração ou exclusão

-- Adiciona a coluna tipo_controle (Oracle não suporta DEFAULT em ALTER TABLE)
ALTER TABLE TB_CONTROLE_SYNC_ODONTOPREV 
ADD tipo_controle NUMBER(1) DEFAULT 1;

-- Adiciona comentário na coluna
COMMENT ON COLUMN TB_CONTROLE_SYNC_ODONTOPREV.tipo_controle IS '1=Adição, 2=Alteração, 3=Exclusão';

-- Adiciona índice para melhor performance
CREATE INDEX idx_tipo_controle ON TB_CONTROLE_SYNC_ODONTOPREV (tipo_controle);

-- Atualiza registros existentes para tipo_controle = 1 (Adição)
UPDATE TB_CONTROLE_SYNC_ODONTOPREV 
SET tipo_controle = 1 
WHERE tipo_operacao = 'CREATE';

-- Atualiza registros existentes para tipo_controle = 2 (Alteração)
UPDATE TB_CONTROLE_SYNC_ODONTOPREV 
SET tipo_controle = 2 
WHERE tipo_operacao = 'UPDATE';

-- Atualiza registros existentes para tipo_controle = 3 (Exclusão)
UPDATE TB_CONTROLE_SYNC_ODONTOPREV 
SET tipo_controle = 3 
WHERE tipo_operacao = 'DELETE';

-- Torna a coluna NOT NULL após popular os dados
ALTER TABLE TB_CONTROLE_SYNC_ODONTOPREV 
MODIFY tipo_controle NOT NULL;

-- Verifica se a atualização foi bem-sucedida
SELECT 
    tipo_operacao,
    tipo_controle,
    COUNT(*) as total_registros
FROM TB_CONTROLE_SYNC_ODONTOPREV 
GROUP BY tipo_operacao, tipo_controle
ORDER BY tipo_operacao, tipo_controle;
