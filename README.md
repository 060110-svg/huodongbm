# 校园活动报名系统 (huodongbm)

一个基于 **Spring Boot 4 + Thymeleaf + MyBatis-Plus + MySQL** 的校园活动报名管理系统。支持活动发布与管理、学生在线报名、管理员审核，以及活动状态的自动流转。

## 功能特性

- **双角色体系**：管理员（admin）与学生（student）差异化登录入口与操作权限
- **活动管理**：管理员可发布、编辑、下线、删除活动，支持上传封面图片（jpg/jpeg/png/gif/webp，≤10MB）
- **在线报名**：学生报名活动，系统自动校验活动状态、报名截止时间、重复报名与名额限制
- **报名审核**：管理员对待审核报名进行通过 / 拒绝 / 重新审核，并实时维护活动参与人数
- **我的报名**：学生可查看自己的报名记录、撤销待审核申请或取消已通过报名
- **首页数据看板**：管理员端统计活动数、已发布数、待审核报名、用户数；学生端展示活动统计与热门活动
- **活动状态自动流转**：定时任务根据起止时间自动将活动切换为 进行中(ongoing) / 已结束(ended)
- **活动检索**：按标题模糊搜索活动列表

## 技术栈

| 分类 | 技术 |
| --- | --- |
| 后端框架 | Spring Boot 4.0.6 |
| JDK | Java 25 |
| ORM | MyBatis-Plus 3.5.16 |
| 模板引擎 | Thymeleaf |
| 数据库 | MySQL 8.x |
| 构建工具 | Maven |
| 其他 | Lombok、Spring Actuator |

## 项目结构

```
huodongbm/
├── pom.xml                              # Maven 配置
├── xiaoyuanhdbmxt.sql                   # 数据库建表 + 初始化数据脚本
├── src/main/java/com/stu/huodongbm/
│   ├── HuodongbmApplication.java        # 启动类
│   ├── config/                          # 配置类（MyBatis-Plus、Web、全局模型）
│   ├── controller/                      # 控制器（登录、首页、活动、报名）
│   ├── entity/                          # 实体类（User、Activity、Registration）
│   ├── interceptor/                     # 登录拦截器
│   ├── mapper/                          # MyBatis-Plus Mapper
│   ├── service/                         # 业务层
│   └── task/                            # 定时任务（活动状态流转）
├── src/main/resources/
│   ├── application.yml                  # 应用配置
│   ├── static/                          # 静态资源（CSS、JS）
│   └── templates/                       # Thymeleaf 模板
└── src/test/java                         # 测试
```

## 快速开始

### 环境要求

- JDK 25+
- Maven 3.9+
- MySQL 8.x

### 1. 初始化数据库

使用 MySQL 客户端执行根目录的建表脚本（会自动建库、建表并写入初始化数据）：

```bash
mysql -u root -p < xiaoyuanhdbmxt.sql
```

### 2. 配置数据库连接

数据库连接默认读取环境变量，未配置时使用以下本地默认值（与脚本一致）：

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `DB_HOST` | `127.0.0.1` | 数据库地址 |
| `DB_PORT` | `3306` | 数据库端口 |
| `DB_NAME` | `xiaoyuanhdbmxt` | 数据库名 |
| `DB_USERNAME` | `root` | 数据库用户名 |
| `DB_PASSWORD` | `1234` | 数据库密码 |

如需修改，可直接编辑 `src/main/resources/application.yml`，或设置环境变量覆盖。

### 3. 启动项目

```bash
mvn spring-boot:run
```

启动成功后访问：<http://localhost:8080/login>

### 默认账号

| 角色 | 用户名 | 密码 |
| --- | --- | --- |
| 管理员 | `admin` | `123456` |
| 学生 | `student01` | `123456` |

> 密码为明文存储，仅供学习演示使用，生产环境请使用加密方案（如 BCrypt）。

## 数据模型

核心表：`user`（用户）、`activity`（活动）、`registration`（报名），另含 `clazz`（班级）、`student`（学生）、`score`（成绩）、`dept`（部门）等扩展表。详见 `xiaoyuanhdbmxt.sql`。

活动状态：`published`(已发布) → `ongoing`(进行中) → `ended`(已结束)，`offline`(已下线)。

报名状态：`registered`(待审核) → `approved`(已通过) / `rejected`(已拒绝)，`cancelled`(已取消)。

## 开源许可

本项目采用 [MIT](./LICENSE) 许可证开源。
