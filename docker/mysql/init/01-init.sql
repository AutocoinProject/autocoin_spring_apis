-- ====================================
-- Autocoin MySQL 초기화 스크립트
-- ====================================

-- 데이터베이스 생성 (이미 존재한다면 스킵)
CREATE DATABASE IF NOT EXISTS autocoin_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 사용자 생성 및 권한 부여
CREATE USER IF NOT EXISTS 'autocoin_user'@'%' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON autocoin_db.* TO 'autocoin_user'@'%';

-- 권한 새로고침
FLUSH PRIVILEGES;

-- 데이터베이스 선택
USE autocoin_db;

-- 기본 테이블 생성은 JPA/Hibernate가 자동으로 처리합니다.
-- 필요시 여기에 초기 데이터 INSERT 구문을 추가하세요.

-- 예시: 기본 관리자 계정 생성 (필요시 활성화)
-- INSERT INTO users (username, email, password, role, created_at, updated_at)
-- VALUES ('admin', 'admin@autocoin.com', '$2a$10$...', 'ADMIN', NOW(), NOW());
