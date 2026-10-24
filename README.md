# 迷你 URL 短链接服务

一个用 Java 写的 URL 短链接服务：创建短链接、重定向访问者、查询链接详情、读取按天统计的访问量。
并发访问下访问计数准确。

- Java 17、Spring Boot 3.3.5、Spring Data JPA
- 嵌入式 H2（**file 模式**）—— 数据在重启后仍在，评估者无需安装数据库
- 302 重定向、原子计数、append-only 访问日志支撑每日统计

---

## 1. 运行

**前置条件：** JDK 17 或更高版本。**不需要预装 Maven** —— 仓库自带 Maven Wrapper。

```bash
# macOS / Linux
./mvnw spring-boot:run

# Windows（PowerShell / CMD）
mvnw.cmd spring-boot:run
```

首次执行时 wrapper 会把 Maven 3.9.9 下载到 `~/.m2/wrapper`，之后离线可用。
如果本机已经装了 Maven 3.9+，`mvn spring-boot:run` 效果相同。

到此为止，没有别的安装步骤。服务监听 <http://localhost:8080>，首次启动时创建
`./data/shortener.mv.db`，并自动建表。

验证：

```bash
curl -i -X POST http://localhost:8080/api/links \
     -H "Content-Type: application/json" \
     -d '{"url":"https://example.com/some/very/long/path?query=1"}'
# → 201 Created {"code":"aB3xK9","short_url":"http://localhost:8080/aB3xK9", ...}
```

打开 <http://localhost:8080> 可以看到一个极简页面，用于创建链接并列出已有链接。

### 没有本地 JDK 时

```bash
docker compose up --build
```

同样是 8080 端口，`./data` 以卷的方式挂载，所以 `docker compose down` 之后数据仍在。

### 配置项

| 环境变量 | 默认值 | 含义 |
|---|---|---|
| `PORT` | `8080` | HTTP 端口 |
| `BASE_URL` | `http://localhost:8080` | 拼接响应中 `short_url` 的前缀 |
| `STATS_ZONE` | `UTC` | 统计接口里用来定义「一天」的时区 |

其余可调项（`app.code-length`、`app.code-max-attempts`、JDBC URL）都在
`src/main/resources/application.yml` 中。

### 测试

```bash
./mvnw test        # Windows: mvnw.cmd test
```

36 个用例，全部在内存 H2 上运行（`src/test/resources/application-test.yml`），
不会碰真实数据库。

---

## 2. API

基础 URL 为 `http://localhost:8080`，所有请求体与响应体均为 JSON。

### 创建短链接

```bash
curl -i -X POST http://localhost:8080/api/links \
     -H "Content-Type: application/json" \
     -d '{"url":"https://example.com/some/very/long/path?query=1"}'
```

```http
HTTP/1.1 201 Created
Location: http://localhost:8080/aB3xK9
Content-Type: application/json

{"code":"aB3xK9","short_url":"http://localhost:8080/aB3xK9",
 "url":"https://example.com/some/very/long/path?query=1",
 "created_at":"2026-10-15T08:30:00Z"}
```

校验规则：`url` 必须存在且非空白，scheme 必须是 `http` 或 `https`，长度不超过 2048 个字符。
违反任一条返回 `400`。

### 重定向

```bash
curl -i http://localhost:8080/aB3xK9
```

```http
HTTP/1.1 302 Found
Location: https://example.com/some/very/long/path?query=1
Cache-Control: no-store
```

每一次成功的重定向都会让访问计数加一。

### 链接详情

```bash
curl -i http://localhost:8080/api/links/aB3xK9
```

```json
{"code":"aB3xK9","url":"https://example.com/some/very/long/path?query=1",
 "created_at":"2026-10-15T08:30:00Z","visit_count":128}
```

### 每日统计

```bash
curl -i http://localhost:8080/api/links/aB3xK9/stats
```

```json
{"code":"aB3xK9","days":[
  {"date":"2026-10-09","count":0},
  {"date":"2026-10-10","count":3},
  {"date":"2026-10-11","count":0},
  {"date":"2026-10-12","count":17},
  {"date":"2026-10-13","count":42},
  {"date":"2026-10-14","count":51},
  {"date":"2026-10-15","count":15}]}
```

最近 7 个自然日（含今天），从旧到新，缺失的日期补 0。默认按 **UTC** 自然日切分
（可用 `STATS_ZONE` 修改）。

### 错误响应

所有错误都是同一个结构，包括未匹配的路径和方法不允许：

```json
{"status":400,"error":"INVALID_URL","message":"url must be a valid http/https URL"}
```

| `error` | HTTP | 触发条件 |
|---|---|---|
| `INVALID_URL` | 400 | 空值、非 http(s)、无 host、无法解析、超长 |
| `VALIDATION_FAILED` | 400 | Bean Validation 拒绝请求体（`url` 缺失/空白/超长） |
| `MALFORMED_JSON` | 400 | 请求体不是合法 JSON |
| `INVALID_PARAMETER` | 400 | 参数类型错误 |
| `NOT_FOUND` | 404 | 短码不存在，或路径不存在 |
| `METHOD_NOT_ALLOWED` | 405 | 该路径不支持此 HTTP 方法 |
| `UNSUPPORTED_MEDIA_TYPE` | 415 | `Content-Type` 不是 `application/json` |
| `SHORT_CODE_UNAVAILABLE` | 503 | 每次生成的短码都冲突 |
| `INTERNAL_ERROR` | 500 | 其他未预期异常（详情只写日志，不回传） |

### 附加接口

- `GET /api/links` —— 最近 20 条链接（前端页面用）。
- `GET /db/console` —— 浏览数据库（JDBC URL `jdbc:h2:file:./data/shortener`，
  用户 `sa`，密码为空）。仅供演示，对外部署前请关闭。

---

## 3. 设计决策

### 3.1 短码生成

**`[a-zA-Z0-9]` 中随机取 7 位，唯一性由数据库唯一索引保证，每次创建最多重试 5 次。**

- 字母表 62 个字符，7 位共 `62^7 ≈ 3.5 × 10^12` 种。链接数为 1 万时，单次插入撞码概率约为
  `10^4 / 3.5 × 10^12 ≈ 3 × 10^{-9}`；重试是兜底，不是常规路径。
- 唯一性**不是**靠插入前 `existsByCode(code)` 判断的。那是一个竞态：两个并发创建可能都看到
  「可用」然后都插入。唯一可信的检查是数据库拒绝第二次插入，所以由 `uk_links_code` 兜住，
  捕获 `DataIntegrityViolationException` 后重试。
- 每次尝试都在**独立事务**（`REQUIRES_NEW`）中执行。这一点很关键：Hibernate 在约束冲突后会把
  当前事务标记为 rollback-only，如果重试循环写在同一事务里，第二次尝试会以
  「Transaction marked as rollback-only」失败，而不是成功。
- 用 `SecureRandom` 而不是 `Random`：`java.util.Random` 的输出可以从少量样本推算出来，
  那等于让人能枚举站内所有短码。

被否方案 —— *自增 ID + base62*：唯一性有数学保证、不需要重试，但短码变成有序的
（`/0000001`、`/0000002`…），任何人都能把整张链接表走一遍，还顺带暴露了业务量。
我选择不可猜的短码，代价是一段几乎不会执行到的重试分支。若要彻底消除重试，可以加一条
「重试耗尽则回退到 ID 派生码」的兜底；在 `62^7` 的量级下这基本不会发生。

### 3.2 重定向状态码：302，而不是 301

`302 Found`，并带 `Cache-Control: no-store`。

- `301` 是**永久**重定向：浏览器和中间层会缓存它，往往缓存到配置文件的整个生命周期。之后
  的重复访问根本到不了服务器，`visit_count` 和每日统计会静默少计 —— 少的正是本题要衡量的部分。
- 这个映射在设计上也不是永久的：以后可能变化（过期、自定义别名、换目标地址），这又是一个
  不该承诺「永久」的理由。
- `no-store` 是补强措施：有些代理连 302 也缓存。它不花任何成本，同时把意图写清楚。

接受的代价：每次访问都要多一次 HTTP 往返，而不是用缓存的跳转。这正是「要有统计」的题中之义。

### 3.3 重复 URL：每次都给新短码

同一个 URL 提交两次会得到两个不同的短码。这一点在文档里明确写死，行为保持一致。

- `url` 上不建唯一约束。这一列最长 2048 字符，在它上面建唯一索引既贵，可行性又因数据库而异
  （索引键长度上限不同），换来的好处很小。
- 创建路径因此保持为「纯单行插入」，没有先读再写的竞态。
- 调用方常常**就是要**为同一个目标地址准备多个短码：不同渠道、不同活动、A/B 变体，
  各自独立统计。
- 需要稳定标识的人可以用自定义别名（一个自然的扩展方向，本次未实现）。

被否方案 —— *已知 URL 直接返回已有短码*：会把每次创建变成「先读再插」，还要处理竞态；
并且会把调用方可能想分开的统计悄悄合并。

### 3.4 每日统计：append-only 事件日志，而不是按天聚合表

`visits(link_id, visit_day, visited_at)` 每次重定向写入**一行**，总数放在
`links.visit_count` 这个冗余计数器里。

- **写入便宜且无争用。** 往 append-only 表插入不会和并发插入冲突。按天聚合表（每个链接每天
  一行）会让热门链接的所有访问去抢**今天那一行**：自增被单行锁串行化，而「不存在则插入」
  这一步还需要方言相关的 upsert（PostgreSQL 的 `ON CONFLICT`、MySQL 的
  `ON DUPLICATE KEY`、H2 的 `MERGE`）以及相应的重试处理。
- **两条写操作在同一个事务里。** 计数器更新和访问记录插入一起提交，所以 `visit_count`
  与每日计数之和永远不会漂移。
- **7 天查询仍然是 O(天数)。** 有了 `idx_visits_link_day(link_id, visit_day)`，窗口查询就是
  在已排序索引上的范围扫描；缺失的日期在 Java 侧补零（7 次循环），而不是靠日历表或递归 CTE
  —— 那又是一种方言相关构造。
- **可扩展。** 日志以后还能回答聚合表回答不了的问题：去重访客、按小时、来源、User-Agent。

接受的取舍：存储随访问量线性增长（这个量级下无所谓，真实部署可用保留策略控制），
以及窗口内聚合查询要扫描「每次访问一行」而不是「每天一行」。如果流量到了每天百万级重定向，
我会加一张 `link_daily_stats` 汇总表由定时任务写入、日志保留用于取证查询，或者用 Redis
吸收计数增量再批量落库。

### 3.5 数据库索引

| 索引 | 作用 |
|---|---|
| `uk_links_code (code)` **唯一** | 每次重定向、每次详情/统计读取的查找入口，*同时*是短码唯一性的真正保证。反正查找必须有索引，做成唯一的不额外花钱。 |
| `idx_visits_link_day (link_id, visit_day)` | 复合索引。左前缀 `link_id` 按链接过滤，第二列有序，所以 7 天的 `BETWEEN` 是有序范围扫描，无排序、无全表扫描。同时服务于外键检查。 |
| `fk_visits_link (link_id)` | 引用完整性：访问记录不可能指向不存在的链接。 |
| 两张表的主键 | 都用单调递增的 `bigint` 作主键，插入追加在聚簇索引末尾。 |

`links.url` 上什么索引都没有：原因见 3.3。

### 3.6 其他决策

- **单实例部署。** 假设单进程，因此没有分布式锁、没有多节点协调。水平扩展时最先出问题的会是
  计数器（`UPDATE … + 1` 在**同一个数据库内**仍是原子的，而在常见的扩展方案里数据库是共享的，
  所以它其实比看上去更能扩展）。
- **用 H2 file 模式**而不是 MySQL/PostgreSQL：题目的硬要求是评估者不装任何东西就能启动。
  同时仍提供 `docker-compose.yml` 以便容器化运行；schema 与 JPA 映射是可移植的，换成
  PostgreSQL 只需改 datasource URL 并加驱动。
- **用 JPA `ddl-auto: update`** 而不是 Flyway：这个规模的练习服务换取零依赖启动，而且索引就
  声明在实体注解里，读者一眼能看到。长期维护的项目我会改用版本化迁移。
- **`open-in-view: false`**，避免数据访问悄悄泄漏到视图层。

---

## 4. 并发与正确性

短链接服务最容易丢数据的三处，以及这里的做法：

1. **访问计数。** 自增是一条语句 ——
   `update links set visit_count = visit_count + 1 where id = ?`。数据库在行锁保护下完成
   读和写，所以两个同时发生的重定向不可能都读到 128 再都写回 129。实体上刻意没有
   `setVisitCount(...)`，因为 `find → +1 → save` 就是那个 bug。
2. **短码唯一性。** 由唯一索引保证，而不是靠预检；每次尝试在独立事务里重试（3.1）。
3. **计数器与每日统计。** 两条写操作共用一个短事务，要么都成功，要么都回滚。

`src/test/java/.../ConcurrencyTest.java` 用 16 个线程对同一个短码发起 320 次重定向，
并断言计数器、访问日志、每日统计三者都精确等于 320。

---

## 5. 测试

| 测试类 | 证明了什么 |
|---|---|
| `LinkApiIntegrationTest` | 创建 → 重定向 → 详情 → 统计的端到端流程；`302` + `Location`；7 天补零；重复 URL 得到新短码；各链接计数互不影响 |
| `ValidationTest` | 8 种非法请求体都返回 `400` 且 `error` 码符合约定 |
| `ErrorShapeTest` | 404/405/415 都能反序列化成同一个 `ApiError` 记录；未匹配路径也一样 |
| `LinkServiceTest` | 撞码 → 重试 → 成功；连续 5 次撞码 → `503`；非法 URL 根本不会碰数据库 |
| `UrlValidatorTest` | `new URI("not-a-url")` 本身是合法的，所以真正拦下它的是 scheme/host 检查；2048 边界 |
| `ConcurrencyTest` | 并发重定向不丢计数；并发创建得到互不相同的短码 |

`./mvnw test` 用内存 H2（`src/test/resources/application-test.yml`）跑全部 36 个用例，
不会碰真实数据库。

---

## 6. 目录结构

```
src/main/java/com/example/shortener/
├── config/       AppProperties（带校验的配置）、AppConfig（Clock、ZoneId）
├── domain/       Link、Visit            —— 实体，索引声明在这里
├── repo/         LinkRepository、VisitRepository
├── service/      LinkService、LinkInserter、RedirectService、StatsService、
│                 ShortCodeGenerator、UrlValidator
├── error/        ApiException + 3 个子类（携带 HTTP 状态与稳定的错误码）
└── web/          LinkController、RedirectController、
                  GlobalExceptionHandler、FallbackErrorController、dto/
```

Controller 不写业务逻辑，Service 是唯一的事务边界，Repository 只负责查询。

---

## 7. 后续我会做什么

- 自定义别名、链接过期（`410 Gone`）、创建接口限流，以及用 Caffeine 缓存热点短码
  （安全，因为短码 → URL 的映射不可变；计数仍然走数据库）。
- 把 `ddl-auto` 换成 Flyway 迁移，并在保持同一套 JPA 映射的前提下把 H2 换成 PostgreSQL。
- 如果访问日志大到一台机器放不下，再加按天预聚合。
