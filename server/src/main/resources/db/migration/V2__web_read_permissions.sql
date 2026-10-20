-- 管理端读取权限明确预置，不让浏览器直连用户库。
INSERT INTO admin_role_permission(role_code,permission_code) VALUES
('security_admin','user:read'),
('security_admin','dashboard:read'),('content_editor','dashboard:read'),
('clinical_reviewer','dashboard:read'),('publisher','dashboard:read');
CREATE INDEX ix_article_created ON admin_knowledge_article(created_at,id);
CREATE INDEX ix_operator_created ON admin_operator(created_at,id);
