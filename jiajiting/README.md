# 家计通（jiajiting）

> 记完就知道还能花多少的预算记账 App  
> 技术栈：UniApp + Vue 3 + Pinia + TypeScript

## v2.1 功能清单（完整版）

- [x] 高级版订阅（本地演示，AI 问答不限次）
- [x] 成就 20 个（设计文档上限）
- [x] 记录编辑 / 删除
- [x] 大数据分页 + 图表防抖
- [x] 月度存款目标

## v2.0 功能清单

### 5 Tab 导航
- [x] 记账 / 报表 / **洞察** / **工具** / 我的

### 洞察 Tab
- [x] 消费习惯提醒（基于账单聚合，无爬虫）
- [x] 联盟优惠 Deep Link 入口
- [x] 优惠券中心（local Mock / cloud API）

### 工具 Tab
- [x] 房贷 / 车贷计算器（等额本息、等额本金）
- [x] 计算结果同步为固定支出

### 家庭协作
- [x] 成员各自记账（`memberId` + 家庭账本）
- [x] 家庭汇总含成员支出 breakdown

## v1.2 功能清单

### AI 财务教练
- [x] 可插拔 AI 层（`local` 模板解读 / `cloud` API）
- [x] 周报 / 月报解读（各每周/每月 1 次）
- [x] 异常问答（每周 ≤3 次，仅传聚合数据）
- [x] 隐私设置中可关闭 AI

### 成就
- [x] 扩展至 15 个徽章（半月/季度坚持、数据觉醒、导入/语音/分享/家庭等）

### 家庭共享
- [x] 户主创建家庭 + 邀请码
- [x] 成员只读加入 + 家庭汇总页
- [x] 聚合快照（不传明细）

## v1.1 功能清单

### 录入增强
- [x] 语音一句话记账（WechatSI 插件 + 文本降级）
- [x] 支付后订阅消息提醒（记账反馈页触发）

### 报表
- [x] 周报（周对比、每日柱状、分类占比）
- [x] 月报热力图（日历热力图 + 摘要）
- [x] 环形图图例长按 → 跳转分类预算设置

### 洞察
- [x] 洞察中心 Inbox（未读角标、全部已读、关闭）
- [x] App 启动 / Tab 展示时自动生成洞察

## MVP 功能清单（v0.1.0）

### 记账
- [x] 首页 Dashboard（预算卡片、比上月对比、洞察卡片、快捷分类）
- [x] 记一笔（数字键盘、10 分类、性质三档、关键词预判、超支确认）
- [x] 记账完成反馈页（预算剩余、改性质、再记一笔）
- [x] 预算 Onboarding 三屏引导
- [x] 预算设置 / 分类管理 / 固定收支

### 录入增强
- [x] 账单导入（粘贴文本 / 微信·支付宝 CSV）
- [x] 导入去重 + 待确认列表
- [x] 固定收支自动入账（App 启动触发）

### 报表
- [x] 收支三栏、月份切换
- [x] 环形图 + 分类下钻明细
- [x] 近 6 月折线图
- [x] 月度支出预测

### 账号与同步
- [x] 微信登录 + 手机号绑定
- [x] 离线队列 + 冲突合并
- [x] 云端 API 可配置（`local` / `cloud`）

### 留存与合规
- [x] 洞察规则引擎 + 首页卡片
- [x] 8 个成就徽章 + 解锁逻辑
- [x] 隐私政策、数据导出、账号注销
- [x] 核心埋点

## 开发

```bash
npm install
npm run dev:mp-weixin    # 微信小程序
npm run dev:h5           # H5 预览
npm run type-check
```

微信开发者工具导入 `dist/dev/mp-weixin`。

## 交互原型

无需编译，浏览器打开 [`prototype/index.html`](prototype/index.html) 可体验全部 27 个页面的可点击原型（含 5 Tab 跳转与 Toast 反馈）。详见 [`prototype/README.md`](prototype/README.md)。

## 目录结构

```
src/
├── pages/
│   ├── auth/           # 登录、绑定手机
│   ├── record/         # 记账、反馈、导入
│   ├── report/         # 报表、下钻
│   ├── profile/        # 我的、成就
│   ├── settings/       # 预算、分类、固定收支、隐私
│   └── onboarding/     # 新用户引导
├── components/         # 键盘、环形图、折线图、语音按钮、热力图
├── services/           # auth、sync、voice、http
├── stores/             # Pinia 状态
├── composables/        # 洞察 hooks
├── constants/          # 分类、成就、规则
└── utils/              # 工具函数
```

## 环境变量

```bash
cp .env.example .env.local
```

| 变量 | 说明 |
|------|------|
| `VITE_AUTH_MODE` | `local`（默认）或 `cloud` |
| `VITE_API_BASE` | 云端 API 地址（cloud 模式） |

云端 API 规范见 [`docs/cloud-api.md`](docs/cloud-api.md)。

## 设计文档

仓库根目录 `家计通-完整产品设计文档-v2.md`
