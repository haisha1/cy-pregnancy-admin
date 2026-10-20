-- 管理端独立数据库，不读取或修改小程序用户库。发布到用户端的消费链路后续单独实现。
CREATE TABLE admin_operator (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  client_uuid CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL UNIQUE,
  username VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  display_name VARCHAR(64) NOT NULL,
  state VARCHAR(16) NOT NULL,
  revision BIGINT NOT NULL DEFAULT 1,
  created_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE admin_role (
  code VARCHAR(40) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  name VARCHAR(64) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE admin_role_permission (
  role_code VARCHAR(40) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  permission_code VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  PRIMARY KEY (role_code, permission_code),
  FOREIGN KEY (role_code) REFERENCES admin_role(code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE admin_operator_role (
  operator_id BIGINT NOT NULL,
  role_code VARCHAR(40) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  PRIMARY KEY (operator_id, role_code),
  FOREIGN KEY (operator_id) REFERENCES admin_operator(id),
  FOREIGN KEY (role_code) REFERENCES admin_role(code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE admin_session (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  client_uuid CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL UNIQUE,
  operator_id BIGINT NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  revoked_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  KEY ix_operator_session(operator_id, revoked_at),
  FOREIGN KEY (operator_id) REFERENCES admin_operator(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 低流量管理写操作使用固定行锁串行化：同时覆盖幂等、最后一名管理员保护和状态流转。
-- 不持锁调用微信/对象存储等外部服务。规模扩大后可按聚合拆锁，不能直接移除锁。
CREATE TABLE admin_write_lock (id INT PRIMARY KEY) ENGINE=InnoDB;
INSERT INTO admin_write_lock VALUES (1);
CREATE TABLE admin_idempotency (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  actor_id BIGINT NOT NULL,
  command_scope VARCHAR(120) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  operation_uuid CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  payload_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  response_json JSON NOT NULL,
  created_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_command(actor_id, command_scope, operation_uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE admin_audit_event (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  actor_id BIGINT NULL,
  resource_type VARCHAR(40) NOT NULL,
  resource_uuid VARCHAR(64) NULL,
  action VARCHAR(64) NOT NULL,
  result_code VARCHAR(40) NOT NULL,
  request_id VARCHAR(64) NULL,
  occurred_at DATETIME(6) NOT NULL,
  KEY ix_audit_time(occurred_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE admin_knowledge_article (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  client_uuid CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL UNIQUE,
  slug VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL UNIQUE,
  published_version_id BIGINT NULL,
  revision BIGINT NOT NULL DEFAULT 1,
  created_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE admin_knowledge_version (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  client_uuid CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL UNIQUE,
  article_id BIGINT NOT NULL,
  version_no INT NOT NULL,
  title VARCHAR(160) NOT NULL,
  category VARCHAR(64) NOT NULL,
  pro_content MEDIUMTEXT NOT NULL,
  plain_content MEDIUMTEXT NOT NULL,
  references_json JSON NOT NULL,
  evidence_note VARCHAR(2000) NOT NULL,
  state VARCHAR(16) NOT NULL,
  author_id BIGINT NOT NULL,
  reviewer_id BIGINT NULL,
  review_note VARCHAR(2000) NULL,
  publisher_id BIGINT NULL,
  published_at DATETIME(6) NULL,
  revision BIGINT NOT NULL DEFAULT 1,
  created_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_article_version(article_id, version_no),
  KEY ix_version_state(state, id),
  FOREIGN KEY (article_id) REFERENCES admin_knowledge_article(id),
  FOREIGN KEY (author_id) REFERENCES admin_operator(id),
  FOREIGN KEY (reviewer_id) REFERENCES admin_operator(id),
  FOREIGN KEY (publisher_id) REFERENCES admin_operator(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
ALTER TABLE admin_knowledge_article ADD CONSTRAINT fk_published_version
  FOREIGN KEY (published_version_id) REFERENCES admin_knowledge_version(id);

INSERT INTO admin_role(code,name) VALUES
('security_admin','安全管理员'),('content_editor','内容编辑'),
('clinical_reviewer','内容审核人员'),('publisher','发布人员');
INSERT INTO admin_role_permission(role_code,permission_code) VALUES
('security_admin','operator:read'),('security_admin','operator:manage'),('security_admin','audit:read'),
('content_editor','knowledge:read'),('content_editor','knowledge:edit'),
('clinical_reviewer','knowledge:read'),('clinical_reviewer','knowledge:review'),
('publisher','knowledge:read'),('publisher','knowledge:publish');
