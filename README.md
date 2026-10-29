---

> [!NOTE]
> **本仓库已迁移至 Gitee**：<https://gitee.com/Everett5678/stock-idle-game>
> GitHub 端仅作只读归档，新代码请推送到 Gitee。

# 股海拾金

一款以股票买卖为主题的**单机增量/放置手游**（Android APK）。市场 24 小时持续模拟运行，你每次只玩 1-10 分钟：看行情、买卖、收分红、做升级，然后关掉。离线时持仓继续产生分红，回来一次性结算——总资产在你离开的时候也在增长。

> 核心循环：**碎片化操作 + 实时模拟市场**。不盯盘、无失败惩罚、没有服务器、不需要账号。

## 特色

- **真实感市场模拟**：12 只虚拟股票，几何布朗运动（随机游走 + 漂移率 + 波动率）+ 均值回归 + 三类事件冲击（个股 / 板块联动 / 财报季），**±10% 涨跌停**、模拟成交量与五档盘口，走势像真的，但纯本地模拟
- **专业图表**：详情页「分时 / K 线」双模式（15 秒/根蜡烛），网格 + 价格刻度 + 基准虚线 + 成交量柱 + 按住十字线读数；大盘指数实时分时
- **限价委托**：挂单冻结资金/持仓，跌到目标价自动买、涨到自动卖，一键撤单
- **放置收益**：持仓每秒产生分红；离线收益按 `离线时长 × 持仓市值 × 分红率` 计算（8 小时上限），上线全屏报告结算；「自动复投」升级让分红自动再买入
- **增量成长**：7 项永久升级（手续费折扣/分红倍率/资讯通/量化机器人/止损保险/内幕雷达/自动复投）、每日任务、12 项成就、每日波动翻倍时段、转生系统（无限循环 + 经理等级永久加成）
- **动画即灵魂**：滚动行情条、卡片迷你走势图、全量数字滚动动画、金币雨、成交粒子、BREAKING 新闻横幅、解锁碎链特效、转生全屏仪式、随牛熊缓慢变色的氛围背景
- **硬朗终端风 + 双主题**：全应用直角金融终端设计、自绘矢量图标（无 emoji）、等宽数字；暗色黑金终端 / 亮色专业行情两种主题，跟随系统自动切换，也可手动切换；红涨绿跌、单手操作
- **新手友好**：首次启动 5 步图形化教学（发 ¥3000 启动补贴），设置里可重看；市场活跃度三档可调（悠闲/标准/疾速）

## 安装

前往 [Releases](../../releases) 下载最新的 `StockIdle-vX.Y.Z.apk`（release 签名），安装即可。支持 Android 8.0（API 26）及以上。

## 怎么玩

1. 开局 ¥10,000（完成新手引导再 +¥3,000），在「行情」页挑一只蓝筹，进详情页点「买入」
2. 想等低价？切到「限价委托」挂个目标价，跌到自动成交；卖单同理
3. 持仓每秒累计分红，到「资产」页收取；关掉游戏也照常累计（上限 8 小时）
4. 价格有 ±10% 涨跌停：涨停买不进、跌停卖不出，留意卡片角标
5. 每天 12:00-13:00 / 20:00-21:00 波动翻倍，差价更好做；「内幕雷达」升级可提前预警事件
6. 「成长」页做任务、买升级、领成就；总资产 ¥100 万后**转生**换永久加成
7. 节奏太快或太慢？设置里调「市场活跃度」：悠闲挂机 / 疾速短线

数值与公式的完整设计文档见 [docs/GDD.md](docs/GDD.md)。

## 技术选型

**Kotlin + Jetpack Compose（原生 Android）**，没有网页套壳。选择理由：

| 维度 | 说明 |
|------|------|
| 性能 | 1Hz 全局刷新 + 多路无限动画，原生渲染毫无压力 |
| 动画 | Compose 的 Animatable / InfiniteTransition / Canvas 覆盖本项目全部动画需求 |
| 主题 | 双主题（亮/暗/跟随系统）是 Compose Material 3 的原生能力 |
| 体积 | APK 约 7MB，无额外运行时 |
| CI | 构建只需 JDK + Android SDK，GitHub Actions 原生支持 |

- 市场引擎：Kotlin 协程，前台每秒演化一次（GBM + 均值回归 + 事件漂移 + 涨跌停 + 指数 + 委托撮合）
- 存档：本地 JSON 原子写入（`filesDir/save.json`，v2 格式，兼容读取 v1），包含持仓/现金/价格/12 分钟分时历史/委托单/成就/离线时间戳
- 图标：18 种自绘 24dp Canvas 矢量线条图标，零第三方依赖
- 音效：Python 程序合成的 WAV（`scripts/gen_sounds.py`），无版权问题

## 目录结构

```
├── app/src/main/java/com/stockidle/game/
│   ├── game/          # 引擎：市场模拟/交易/委托/分红/升级/任务/成就/转生/存档
│   ├── ui/            # Compose 界面：主题/组件(图表/图标/特效/引导)/页面
│   ├── sound/         # SoundPool 音效管理
│   └── util/          # 格式化工具
├── docs/GDD.md        # 游戏设计文档（全部规则与数值公式）
├── scripts/           # 音效生成 / 签名密钥配置脚本
├── version.txt        # 版本号唯一来源（SemVer）
└── .github/workflows/ # CI 与 Release 流水线
```

## 本地运行

要求：Android Studio（Hedgehog 以上）或命令行 JDK 17 + Android SDK（API 35）。

```bash
# Android Studio：File → Open 打开仓库根目录，直接 Run

# 命令行：
./gradlew installDebug          # 安装到连接的设备/模拟器
./gradlew assembleDebug         # 生成 app/build/outputs/apk/debug/app-debug.apk
```

## 构建签名 APK（本地）

```bash
# 生成密钥（仅本地构建需要）
keytool -genkeypair -v -keystore release.keystore -alias stockidle \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass 你的密码 -keypass 你的密码 -dname "CN=StockIdle"

export SIGNING_KEY_PATH=release.keystore
export KEYSTORE_PASSWORD=你的密码
export KEY_ALIAS=stockidle
export KEY_PASSWORD=你的密码

./gradlew assembleRelease
# 产物：app/build/outputs/apk/release/app-release.apk
```

## 发布流程（自动构建签名 APK）

1. 更新 `version.txt`（如 `0.2.0`）并在 `CHANGELOG.md` 写清本次改动
2. 提交并打 tag：

```bash
git commit -am "release: v0.2.0"
git tag v0.2.0
git push origin main v0.2.0
```

3. GitHub Actions 的 **Release** 工作流会自动：
   - 校验 tag 与 `version.txt` 一致（不一致直接失败，保证三者版本统一）
   - 从 Secrets 解出签名密钥 → 构建签名 APK
   - 提取 CHANGELOG 对应段落作为 Release 说明
   - 创建 GitHub Release 并上传 `StockIdle-v0.2.0.apk`

### 一次性配置签名 Secrets

```bash
pip install pynacl
export GH_TOKEN=你的GitHubToken     # 需要 repo 权限，不要写进任何文件
./scripts/setup-signing-secrets.sh <你的GitHub用户名> stock-idle-game
```

脚本会生成密钥库、Base64 编码后写入 4 个 Secrets：`SIGNING_KEY`、`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD`。**密钥文件不进仓库**（已在 .gitignore 排除）。

## 版本号一致性

| 位置 | 来源 |
|------|------|
| 游戏内「设置」显示版本 | `version.txt` → `versionName` |
| APK 产物版本 | 同上 |
| GitHub Release 标签 | tag `vX.Y.Z`（CI 强制校验与 `version.txt` 相同） |

## Roadmap

- [ ] 龙虎榜 / 换手率统计
- [ ] 分红除权除息日机制
- [ ] 股票合并/拆股事件
- [ ] 云备份导出（本地文件分享）

## License

[MIT](LICENSE)
