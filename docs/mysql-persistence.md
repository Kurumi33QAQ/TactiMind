# TactiMind MySQL 持久化说明

## 1. 本阶段做了什么

后端已经支持把比赛事件和 Agent 分析结果写入 MySQL。

默认配置下，MySQL 持久化是关闭的：

```properties
tactimind.mysql.enabled=false
```

这样即使你本地没有 MySQL，项目也能继续用 JSON、内存、Redis 和 Agent 正常演示。

## 2. 数据库表

后端在 MySQL 持久化开启后会自动创建三张表：

```text
match_info
match_event
match_analysis
```

含义：

```text
match_info      比赛基本信息
match_event     比赛事件明细
match_analysis  Agent 战术分析结果
```

## 3. 启用方式

先创建数据库：

```sql
CREATE DATABASE tactimind DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

然后修改 `tactimind-backend/src/main/resources/application.properties`：

```properties
tactimind.mysql.enabled=true
tactimind.mysql.url=jdbc:mysql://127.0.0.1:3306/tactimind?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
tactimind.mysql.username=root
tactimind.mysql.password=你的密码
```

重启 Java 后端即可。

## 4. 验证接口

查看持久化模式：

```text
GET /match/persistence/status
```

查看历史分析：

```text
GET /match/analysis
```

查看赛后报告：

```text
GET /match/report
```

## 5. 当前设计取舍

第一版没有使用 JPA，而是用 JDBC 手写 SQL。

原因：

```text
1. 表结构更直观
2. 更适合教学和面试讲解
3. 当前写入逻辑简单，不需要复杂 ORM
4. 后续可以平滑演进到 MyBatis 或 JPA
```
