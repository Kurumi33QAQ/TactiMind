# TactiMind 公开比赛数据接入说明

## 当前状态

TactiMind 当前默认使用本地模拟事件流，保证项目 0 成本、可离线演示。真实开放数据接入采用“先人工下载，再本地转换”的方式，避免运行时依赖外网，也避免误把模拟数据展示成真实比赛事实。

本阶段已经补上 StatsBomb 本地转换入口：它不会自动下载真实数据，但可以把你放入 raw 目录的 StatsBomb 事件 JSON 转换成 TactiMind 统一事件流。

## 推荐数据源

第一阶段优先预留 StatsBomb Open Data。该类公开事件数据适合用来补充真实比赛事件流，但接入后仍然需要在页面、报告和 Agent 输出中明确标注数据来源。

## 目录约定

原始数据目录：

```text
data/open-data/statsbomb/raw
```

转换后事件目录：

```text
data/open-data/statsbomb/converted
```

转换器会把原始事件映射成 TactiMind 统一事件格式，并生成类似：

```text
data/open-data/statsbomb/converted/{matchCode}.json
```

## 本地转换接口

```http
POST /api/open-data/statsbomb/convert-local
Content-Type: application/json

{
  "matchCode": "adapter-demo",
  "rawEventFile": "adapter-demo-events.json",
  "outputFile": "adapter-demo.json"
}
```

说明：`adapter-demo-events.json` 只是转换器适配测试样例，不是真实比赛数据。真实接入时，请把公开数据源下载到的原始事件 JSON 放入 raw 目录。

## 统一事件格式

```json
{
  "minute": 12,
  "team": "Team A",
  "type": "SHOT",
  "player": "Player 9",
  "description": "球队完成一次禁区内射门。",
  "data": {
    "sourceType": "STATSBOMB_OPEN_DATA",
    "originalType": "Shot",
    "zone": "box",
    "direction": "middle",
    "phase": "build_up",
    "result": "Saved"
  }
}
```

## 当前转换规则

- StatsBomb 的 `Shot` 映射为 `SHOT`、`SHOT_ON_TARGET` 或 `GOAL`
- `Pass` 中的 `shot_assist` / `goal_assist` 映射为 `KEY_PASS`
- `Dribble` / `Carry` 在高位区域发生时映射为 `DANGEROUS_ATTACK`
- `Duel` / `Interception` / `Ball Recovery` / `Block` 映射为 `TACKLE`
- `Dispossessed` / `Miscontrol` 映射为 `TURNOVER`
- 坐标会转换成 `left / middle / right / box`
- `play_pattern` 会转换成 `build_up / transition / set_piece`

## 防幻觉要求

真实开放数据接入后也不能放松防幻觉机制：

- 每条结论仍必须有 `evidence`
- 数据缺失时必须降低 `confidence`
- 模拟数据和真实公开数据必须在页面上明确区分
- 阵容、球员状态、能力标签如果不是来自真实数据源，必须标注为辅助演练资料


## 已接入的真实公开比赛

当前已下载并接入 3 场 StatsBomb Open Data 公开事件数据：

| 比赛 | 日期 | StatsBomb match_id | 本地文件 |
| --- | --- | --- | --- |
| Argentina vs France | 2022-12-18 | 3869685 | `data/open-data/statsbomb/raw/world-cup-2022-argentina-france-statsbomb.json` |
| Argentina vs Croatia | 2022-12-13 | 3869519 | `data/open-data/statsbomb/raw/world-cup-2022-argentina-croatia-statsbomb.json` |
| Netherlands vs Argentina | 2022-12-09 | 3869321 | `data/open-data/statsbomb/raw/world-cup-2022-netherlands-argentina-statsbomb.json` |

这些文件来自 StatsBomb Open Data 的公开 GitHub 仓库。它们是公开事件数据，不是 TactiMind 人工模拟事件。页面和报告仍需要区分：事件流来自公开数据；阵容能力标签如果缺少真实来源，仍属于项目辅助资料。
## 当前接口

查看导入说明：

```http
GET /api/open-data/import-guide?sourceType=STATSBOMB_OPEN_DATA
```

转换本地 StatsBomb 事件：

```http
POST /api/open-data/statsbomb/convert-local
```