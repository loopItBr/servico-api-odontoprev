-- Script para verificar se as views existem no banco
SELECT 'VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS' as view_name, COUNT(*) as exists_count
FROM user_views 
WHERE view_name = 'VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS'

UNION ALL

SELECT 'VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_ALT' as view_name, COUNT(*) as exists_count
FROM user_views 
WHERE view_name = 'VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_ALT'

UNION ALL

SELECT 'VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_EXC' as view_name, COUNT(*) as exists_count
FROM user_views 
WHERE view_name = 'VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_EXC';

-- Verificar todas as views que começam com VW_INTEGRACAO_ODONTOPREV
SELECT view_name, status
FROM user_views 
WHERE view_name LIKE 'VW_INTEGRACAO_ODONTOPREV%'
ORDER BY view_name;
