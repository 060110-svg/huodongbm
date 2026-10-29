-- 创建数据库
CREATE DATABASE IF NOT EXISTS student_manage DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE student_manage;

-- 用户表
CREATE TABLE IF NOT EXISTS user (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(50) NOT NULL,
    real_name VARCHAR(50) NOT NULL
);

-- 班级表
CREATE TABLE IF NOT EXISTS clazz (
    id INT AUTO_INCREMENT PRIMARY KEY,
    clazz_name VARCHAR(50) NOT NULL
);

-- 学生表
CREATE TABLE IF NOT EXISTS student (
    id INT AUTO_INCREMENT PRIMARY KEY,
    stu_name VARCHAR(50) NOT NULL,
    stu_no VARCHAR(50) NOT NULL,
    age INT,
    gender VARCHAR(10),
    clazz_id INT,
    create_time DATETIME,
    FOREIGN KEY (clazz_id) REFERENCES clazz(id)
);

-- 成绩表
CREATE TABLE IF NOT EXISTS score (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    subject VARCHAR(50) NOT NULL,
    score DOUBLE,
    exam_date DATE,
    FOREIGN KEY (student_id) REFERENCES student(id)
);

-- 插入默认管理员账号（用户名：admin，密码：123456）
INSERT INTO user (username, password, real_name) VALUES ('admin', '123456', '管理员');

-- 插入测试数据
INSERT INTO clazz (clazz_name) VALUES ('计算机1班'), ('计算机2班'), ('软件1班');

INSERT INTO student (stu_name, stu_no, age, gender, clazz_id, create_time) VALUES 
('张三', '2024001', 20, '男', 1, NOW()),
('李四', '2024002', 21, '女', 1, NOW()),
('王五', '2024003', 19, '男', 2, NOW());

INSERT INTO score (student_id, subject, score, exam_date) VALUES 
(1, 'Java程序设计', 85.5, '2024-06-15'),
(1, '数据库原理', 90.0, '2024-06-16'),
(2, 'Java程序设计', 92.0, '2024-06-15'),
(3, '数据结构', 88.5, '2024-06-17');