/* 家计通交互原型 — 页面路由与交互逻辑 */

const TAB_SCREENS = ['record', 'report', 'savings', 'tools', 'profile']

const SCREENS = {
  login: {
    title: '登录',
    path: 'pages/auth/login',
    tab: false,
    html: `
      <div class="page" style="display:flex;flex-direction:column;align-items:center;justify-content:center;min-height:100%;text-align:center;padding:40px 24px;">
        <div style="font-size:64px;margin-bottom:16px;">💰</div>
        <h2 style="font-size:24px;margin-bottom:8px;">家计通</h2>
        <p class="muted" style="margin-bottom:40px;">记完就知道还能花多少</p>
        <button class="btn btn-primary clickable" data-nav="onboarding">微信一键登录</button>
        <p class="muted" style="margin-top:16px;font-size:12px;">登录即同意隐私政策</p>
      </div>`,
  },
  onboarding: {
    title: '预算引导',
    path: 'pages/onboarding/index',
    tab: false,
    html: `
      <div class="page" style="text-align:center;padding-top:60px;">
        <div style="font-size:48px;">👋</div>
        <h2 style="margin:16px 0 8px;">记完就知道还能花多少</h2>
        <p class="muted" style="margin-bottom:32px;">每笔消费后立刻看到预算影响</p>
        <button class="btn btn-primary clickable" data-action="onboarding-step">开始设置</button>
        <button class="btn btn-ghost clickable" data-nav="record">先逛逛</button>
      </div>`,
  },
  record: {
    title: '记账',
    path: 'pages/record/index',
    tab: true,
    html: `
      <div class="page" style="position:relative;padding-bottom:80px;">
        <div class="gradient-header">
          <div style="display:flex;justify-content:space-between;margin-bottom:12px;">
            <span>👋 吴银龙</span><span style="font-size:13px;opacity:0.9;">6月6日</span>
          </div>
          <div class="label">本月可支配</div>
          <div class="amount">¥3,450</div>
          <div class="progress-track"><div class="progress-bar"></div></div>
          <div class="hint">已花 ¥2,550 · 可选剩余 ¥280</div>
          <div style="font-size:13px;margin-top:6px;">📉 比上月同期少花 ¥320</div>
        </div>
        <div class="card clickable" data-nav="insights">
          <div style="font-size:13px;color:#636e72;">💡 餐饮本周已花 ¥420，比均值高 18%</div>
          <div style="font-size:12px;color:#6c5ce7;margin-top:6px;">查看洞察中心 ›</div>
        </div>
        <div class="section-title">快捷记账</div>
        <div class="grid-3">
          <div class="card cat-item clickable" data-nav="quick" data-param="餐饮"><span class="emoji">🍜</span><span class="name">餐饮</span><span class="spent">¥560</span></div>
          <div class="card cat-item clickable" data-nav="quick" data-param="出行"><span class="emoji">🚕</span><span class="name">出行</span><span class="spent">¥120</span></div>
          <div class="card cat-item clickable" data-nav="quick" data-param="购物"><span class="emoji">🛒</span><span class="name">购物</span><span class="spent">¥380</span></div>
          <div class="card cat-item clickable" data-nav="quick" data-param="账单"><span class="emoji">💡</span><span class="name">账单</span><span class="spent">¥2,100</span></div>
          <div class="card cat-item clickable" data-nav="quick" data-param="娱乐"><span class="emoji">🎮</span><span class="name">娱乐</span><span class="spent">¥90</span></div>
          <div class="card cat-item clickable" data-nav="quick" data-param="其他"><span class="emoji">···</span><span class="name">其他</span><span class="spent">¥50</span></div>
        </div>
        <div class="section-title" style="margin-top:16px;">最近记录</div>
        <div class="card clickable" data-nav="quick" data-action="edit" style="display:flex;justify-content:space-between;">
          <span>🛒 购物 · 可选</span><span class="expense" style="font-weight:600;">¥128</span>
        </div>
        <div class="card clickable" data-nav="quick" data-action="edit" style="display:flex;justify-content:space-between;">
          <span>🍜 餐饮 · 可选</span><span class="expense" style="font-weight:600;">¥38</span>
        </div>
        <div class="card clickable" data-action="delete-hint" style="display:flex;justify-content:space-between;">
          <span>🚕 出行 · 刚需</span><span class="expense" style="font-weight:600;">¥22</span>
        </div>
        <button class="fab clickable" data-nav="quick">＋ 记一笔</button>
      </div>`,
  },
  quick: {
    title: '记一笔',
    path: 'pages/record/quick',
    tab: false,
    back: 'record',
    html: `
      <div class="page">
        <div class="nav-bar">
          <button class="nav-back" data-nav="record">← 返回</button>
          <span class="nav-title">记一笔</span>
          <span class="clickable" style="color:#6c5ce7;" data-nav="import">导入</span>
        </div>
        <div class="amount-display"><small>¥</small>38.00</div>
        <p class="muted" style="text-align:center;margin-bottom:16px;">餐饮 · 上次</p>
        <div class="chip-row">
          <span class="chip active">🍜 餐饮</span>
          <span class="chip">🚕 出行</span>
          <span class="chip">🛒 购物</span>
          <span class="chip">··· 更多</span>
        </div>
        <div class="card clickable" data-action="voice" style="text-align:center;border:2px dashed #dfe6e9;">
          🎤 按住说话记账
        </div>
        <div class="keypad">
          <div class="key clickable" data-action="key">1</div><div class="key clickable" data-action="key">2</div><div class="key clickable" data-action="key">3</div>
          <div class="key clickable" data-action="key">4</div><div class="key clickable" data-action="key">5</div><div class="key clickable" data-action="key">6</div>
          <div class="key clickable" data-action="key">7</div><div class="key clickable" data-action="key">8</div><div class="key clickable" data-action="key">9</div>
          <div class="key clickable" data-action="key">.</div><div class="key clickable" data-action="key">0</div>
          <div class="key ok clickable" data-nav="feedback">✓</div>
        </div>
      </div>`,
  },
  feedback: {
    title: '记账反馈',
    path: 'pages/record/feedback',
    tab: false,
    back: 'record',
    html: `
      <div class="page" style="text-align:center;">
        <div style="font-size:24px;font-weight:700;margin:20px 0;">✅ 已记录</div>
        <div class="card" style="text-align:left;">
          <div style="font-size:18px;font-weight:600;margin-bottom:12px;">🍜 餐饮 · 可选  ¥38</div>
          <div class="muted" style="margin-bottom:8px;">餐饮预算剩余 ¥1,042 · 已用 35%</div>
          <div class="progress-track" style="background:#dfe6e9;"><div class="progress-bar" style="width:35%;background:#6c5ce7;"></div></div>
          <div class="muted" style="margin-top:12px;">🟡 可选预算剩余 ¥280</div>
          <div style="margin-top:12px;padding:10px;background:#f5f6fa;border-radius:8px;font-size:13px;">💡 本周餐饮比均值高 18%，建议控制外卖频次</div>
        </div>
        <div class="card" style="text-align:left;">
          <div class="muted" style="margin-bottom:8px;">修改性质</div>
          <div class="chip-row">
            <span class="chip">🔴 刚需</span>
            <span class="chip active">🟡 可选</span>
            <span class="chip">🟢 奢侈</span>
          </div>
        </div>
        <button class="btn btn-ghost clickable" data-nav="record">完成</button>
        <button class="btn btn-primary clickable" data-nav="quick">再记一笔</button>
        <p class="muted" style="font-size:12px;margin-top:12px;" data-action="subscribe">📩 尝试订阅记账提醒</p>
      </div>`,
  },
  import: {
    title: '导入账单',
    path: 'pages/record/import',
    tab: false,
    back: 'quick',
    html: `
      <div class="page">
        <div class="chip-row">
          <span class="chip active">粘贴文本</span>
          <span class="chip">微信 CSV</span>
          <span class="chip">支付宝 CSV</span>
        </div>
        <textarea class="input" rows="6" placeholder="05-23 麦当劳 -38.00&#10;05-24 滴滴出行 -22.00" style="resize:none;"></textarea>
        <button class="btn btn-primary clickable" data-nav="import-confirm">解析预览</button>
      </div>`,
  },
  'import-confirm': {
    title: '确认导入',
    path: 'pages/record/import-confirm',
    tab: false,
    back: 'import',
    html: `
      <div class="page">
        <p class="muted" style="margin-bottom:12px;">共 3 条，去重后 2 条可导入</p>
        <div class="card" style="display:flex;justify-content:space-between;"><span>麦当劳 ¥38</span><span style="color:#6c5ce7;">餐饮 ›</span></div>
        <div class="card" style="display:flex;justify-content:space-between;opacity:0.5;"><span>麦当劳 ¥38</span><span style="color:#fdcb6e;">重复</span></div>
        <div class="card" style="display:flex;justify-content:space-between;"><span>滴滴出行 ¥22</span><span style="color:#6c5ce7;">出行 ›</span></div>
        <button class="btn btn-primary clickable" data-nav="record" data-action="import-done">确认导入 2 条</button>
      </div>`,
  },
  report: {
    title: '报表',
    path: 'pages/report/index',
    tab: true,
    html: `
      <div class="page">
        <div class="card" style="display:flex;justify-content:space-between;align-items:center;">
          <span class="clickable">‹</span><span style="font-weight:600;">2026年6月</span><span class="clickable">›</span>
        </div>
        <div style="display:flex;gap:12px;margin-bottom:12px;">
          <span class="clickable" style="color:#6c5ce7;font-size:13px;" data-nav="weekly">📊 周报</span>
          <span class="clickable" style="color:#6c5ce7;font-size:13px;" data-nav="monthly">📅 月报热力图</span>
        </div>
        <div class="card" style="display:flex;justify-content:space-between;text-align:center;">
          <div><div class="muted">收入</div><div class="income" style="font-weight:600;">¥8,000</div></div>
          <div><div class="muted">支出</div><div class="expense" style="font-weight:600;">¥2,550</div></div>
          <div><div class="muted">结余</div><div style="font-weight:600;">¥5,450</div></div>
        </div>
        <div class="card">
          <div class="section-title">支出占比</div>
          <div class="ring-chart"></div>
          <div class="clickable" data-nav="detail" style="display:flex;align-items:center;gap:8px;margin:8px 0;font-size:13px;">
            <span>🍜 餐饮</span><div style="flex:1;height:6px;background:#dfe6e9;border-radius:99px;"><div style="width:35%;height:100%;background:#ff6b6b;border-radius:99px;"></div></div>
            <span>35%</span>
          </div>
          <div class="clickable" data-action="longpress-budget" style="display:flex;align-items:center;gap:8px;margin:8px 0;font-size:13px;">
            <span>💡 账单</span><div style="flex:1;height:6px;background:#dfe6e9;border-radius:99px;"><div style="width:45%;height:100%;background:#96ceb4;border-radius:99px;"></div></div>
            <span>45% ⓘ长按设预算</span>
          </div>
        </div>
        <div class="card">
          <div class="section-title">近 6 月趋势</div>
          <div class="line-chart"></div>
        </div>
        <div class="card" style="font-size:13px;">
          <div style="font-weight:600;margin-bottom:8px;">🔮 本月预测</div>
          <div class="muted">已花 ¥2,550，预计全月 ¥3,800</div>
        </div>
      </div>`,
  },
  detail: {
    title: '支出明细',
    path: 'pages/report/detail',
    tab: false,
    back: 'report',
    html: `
      <div class="page">
        <div class="card">
          <div style="font-weight:600;font-size:16px;">🍜 餐饮 详情</div>
          <div class="muted">已花 ¥560 / 预算 ¥1,600</div>
          <div class="progress-track" style="background:#dfe6e9;margin-top:8px;"><div class="progress-bar" style="width:35%;background:#6c5ce7;"></div></div>
        </div>
        <div class="chip-row"><span class="chip active">全部</span><span class="chip">刚需</span><span class="chip">可选</span><span class="chip">奢侈</span></div>
        <div class="card clickable" data-nav="quick" data-action="edit" style="display:flex;justify-content:space-between;">
          <div><div class="muted" style="font-size:12px;">6/5 12:30</div><div>麦当劳</div></div>
          <div style="text-align:right;"><div class="expense" style="font-weight:600;">¥38</div><div class="muted" style="font-size:11px;">可选</div></div>
        </div>
        <div class="card clickable" data-action="delete-hint" style="display:flex;justify-content:space-between;">
          <div><div class="muted" style="font-size:12px;">6/3 18:00</div><div>外卖</div></div>
          <div style="text-align:right;"><div class="expense" style="font-weight:600;">¥52</div></div>
        </div>
        <p class="muted" style="text-align:center;font-size:12px;margin-top:16px;">点击编辑 · 长按删除</p>
      </div>`,
  },
  weekly: {
    title: '周报',
    path: 'pages/report/weekly',
    tab: false,
    back: 'report',
    html: `
      <div class="page">
        <div class="card" style="display:flex;justify-content:space-between;"><span>‹ 上周</span><span style="font-weight:600;">6.2 - 6.8</span><span style="opacity:0.3;">下周 ›</span></div>
        <div class="card" style="display:flex;text-align:center;">
          <div style="flex:1;"><div class="muted">本周</div><div style="font-weight:600;">¥680</div></div>
          <div style="flex:1;"><div class="muted">上周</div><div>¥820</div></div>
          <div style="flex:1;"><div class="income">↓17%</div></div>
        </div>
        <div style="display:flex;gap:8px;margin-bottom:12px;">
          <button class="btn btn-primary btn-sm clickable" data-nav="coach" data-action="ai-weekly">🤖 AI 周报解读</button>
          <button class="btn btn-ghost btn-sm clickable" data-action="share">📤 分享</button>
        </div>
        <div class="card"><div class="section-title">每日支出</div>
          <div style="display:flex;align-items:center;gap:8px;margin:6px 0;font-size:13px;"><span style="width:40px;">周一</span><div style="flex:1;height:8px;background:#dfe6e9;border-radius:99px;"><div style="width:80%;height:100%;background:#6c5ce7;border-radius:99px;"></div></div><span>¥120</span></div>
          <div style="display:flex;align-items:center;gap:8px;margin:6px 0;font-size:13px;"><span style="width:40px;">周二</span><div style="flex:1;height:8px;background:#dfe6e9;border-radius:99px;"><div style="width:40%;height:100%;background:#6c5ce7;border-radius:99px;"></div></div><span>¥60</span></div>
        </div>
      </div>`,
  },
  monthly: {
    title: '月报',
    path: 'pages/report/monthly',
    tab: false,
    back: 'report',
    html: `
      <div class="page">
        <div class="card" style="display:flex;justify-content:space-between;"><span>‹</span><span style="font-weight:600;">2026年6月</span><span>›</span></div>
        <div class="card">
          <div class="section-title">📅 支出日历</div>
          <div class="heatmap">
            ${Array.from({length:28}, (_,i) => `<div class="heat-cell heat-${i%4}">${i+1}</div>`).join('')}
          </div>
        </div>
        <button class="btn btn-primary clickable" data-nav="coach" data-action="ai-monthly">🤖 AI 月报解读</button>
      </div>`,
  },
  savings: {
    title: '洞察',
    path: 'pages/savings/index',
    tab: true,
    html: `
      <div class="page">
        <div class="card">
          <div style="font-size:20px;font-weight:700;">💡 省钱洞察</div>
          <div class="muted" style="font-size:12px;margin-top:4px;">基于账单习惯，无爬虫</div>
        </div>
        <div class="section-title">消费习惯提醒</div>
        <div class="card">
          <div style="font-weight:600;">餐饮支出偏高</div>
          <div class="muted" style="font-size:13px;margin-top:6px;">本月餐饮 ¥560，比近 3 月均值高 22%</div>
          <span class="clickable" style="color:#6c5ce7;font-size:12px;margin-top:8px;display:block;" data-action="deal">查看官方优惠 ›</span>
        </div>
        <div style="display:flex;justify-content:space-between;align-items:center;margin:16px 0 8px;">
          <span class="section-title" style="margin:0;">联盟优惠</span>
          <span class="clickable" style="color:#6c5ce7;font-size:13px;" data-nav="coupons">优惠券中心 ›</span>
        </div>
        <div class="card clickable" data-action="deal" style="display:flex;align-items:center;gap:12px;">
          <span style="font-size:28px;">🍜</span>
          <div style="flex:1;"><div style="font-weight:600;">美团外卖</div><div class="muted" style="font-size:12px;">餐饮消费后可领平台优惠</div></div>
          <span class="arrow">›</span>
        </div>
        <div class="card clickable" data-action="deal" style="display:flex;align-items:center;gap:12px;">
          <span style="font-size:28px;">🛒</span>
          <div style="flex:1;"><div style="font-weight:600;">京东购物</div><div class="muted" style="font-size:12px;">官方活动入口</div></div>
          <span class="arrow">›</span>
        </div>
        <div class="card">
          <div class="menu-item clickable" data-nav="insights">📬 洞察中心 Inbox <span class="badge">2</span><span class="arrow">›</span></div>
          <div class="menu-item clickable" data-nav="coach">🤖 AI 财务教练<span class="arrow">›</span></div>
        </div>
      </div>`,
  },
  coupons: {
    title: '优惠券中心',
    path: 'pages/coupons/index',
    tab: false,
    back: 'savings',
    html: `
      <div class="page">
        <div class="chip-row"><span class="chip active">未使用 (3)</span><span class="chip">已用 (1)</span><span class="chip">过期 (1)</span></div>
        <div class="card" style="display:flex;gap:12px;">
          <span style="font-size:32px;">🍜</span>
          <div style="flex:1;"><div style="font-weight:600;">美团满30减8</div><div class="muted" style="font-size:12px;">有效期至 6/13</div></div>
          <div style="text-align:right;"><div class="expense" style="font-size:18px;font-weight:700;">¥8</div><button class="btn btn-primary btn-sm clickable" data-action="deal" style="margin-top:4px;">去使用</button></div>
        </div>
        <div class="card" style="display:flex;gap:12px;">
          <span style="font-size:32px;">🛒</span>
          <div style="flex:1;"><div style="font-weight:600;">京东满99减15</div><div class="muted" style="font-size:12px;">有效期至 6/20</div></div>
          <div style="text-align:right;"><div class="expense" style="font-size:18px;font-weight:700;">¥15</div></div>
        </div>
        <p class="muted" style="text-align:center;font-size:11px;margin-top:20px;">优惠券来自官方联盟 API，不承诺价格</p>
      </div>`,
  },
  tools: {
    title: '工具',
    path: 'pages/tools/index',
    tab: true,
    html: `
      <div class="page">
        <div class="chip-row"><span class="chip active" data-action="loan-mortgage">房贷</span><span class="chip" data-action="loan-car">车贷</span></div>
        <div class="card">
          <label class="muted">贷款总额（万）</label><input class="input" value="100" />
          <label class="muted">年利率（%）</label><input class="input" value="3.85" />
          <label class="muted">贷款年限</label><input class="input" value="30" />
          <div class="chip-row"><span class="chip active">等额本息</span><span class="chip">等额本金</span></div>
          <button class="btn btn-primary clickable" data-action="calc-loan">计算</button>
        </div>
        <div class="card" id="loan-result" style="display:none;">
          <div class="section-title">计算结果</div>
          <div style="display:flex;justify-content:space-between;margin:8px 0;"><span>月均还款</span><span class="expense" style="font-size:20px;font-weight:700;">¥4,685</span></div>
          <div style="display:flex;justify-content:space-between;margin:8px 0;font-size:13px;"><span>还款总额</span><span>¥168万</span></div>
          <button class="btn btn-ghost clickable" data-action="sync-recurring">同步为固定支出</button>
        </div>
      </div>`,
  },
  profile: {
    title: '我的',
    path: 'pages/profile/index',
    tab: true,
    html: `
      <div class="page">
        <div class="card">
          <div style="font-size:22px;font-weight:700;">吴银龙</div>
          <div class="muted" style="font-size:13px;margin-top:4px;">138****8000 · 连续 7 天</div>
          <div class="muted" style="font-size:12px;margin-top:8px;">同步：已同步 6/6</div>
        </div>
        <div class="card" style="padding:0 14px;">
          <div class="menu-item clickable" data-nav="budget">⚙️ 预算设置<span class="arrow">›</span></div>
          <div class="menu-item clickable" data-nav="categories">📂 分类管理<span class="arrow">›</span></div>
          <div class="menu-item clickable" data-nav="recurring">📌 固定收支<span class="arrow">›</span></div>
          <div class="menu-item clickable" data-nav="import">📥 导入账单<span class="arrow">›</span></div>
          <div class="menu-item clickable" data-nav="insights">💡 洞察中心<span class="badge">2</span><span class="arrow">›</span></div>
          <div class="menu-item clickable" data-nav="reminder">🔔 记账提醒<span class="arrow">›</span></div>
          <div class="menu-item clickable" data-nav="coach">🤖 AI 财务教练<span class="arrow">›</span></div>
          <div class="menu-item clickable" data-nav="family">👨‍👩‍👧 家庭共享<span class="arrow">›</span></div>
          <div class="menu-item clickable" data-nav="subscription">⭐ 高级版<span class="arrow">›</span></div>
          <div class="menu-item clickable" data-nav="privacy">🔒 隐私与数据<span class="arrow">›</span></div>
        </div>
        <div class="card clickable" data-nav="achievements">
          <div style="font-weight:600;">🏆 成就徽章（8/20）</div>
          <div style="color:#6c5ce7;font-size:13px;margin-top:6px;">查看全部 ›</div>
        </div>
      </div>`,
  },
  budget: {
    title: '预算设置',
    path: 'pages/settings/budget',
    tab: false,
    back: 'profile',
    html: `
      <div class="page">
        <div class="card">
          <div class="section-title">分类月预算</div>
          <div style="display:flex;justify-content:space-between;margin:8px 0;font-size:14px;"><span>🍜 餐饮</span><input class="input" value="1600" style="width:100px;text-align:right;margin:0;" /></div>
          <div style="display:flex;justify-content:space-between;margin:8px 0;font-size:14px;"><span>🚕 出行</span><input class="input" value="500" style="width:100px;text-align:right;margin:0;" /></div>
          <div style="display:flex;justify-content:space-between;margin:8px 0;font-size:14px;"><span>💡 账单</span><input class="input" value="2500" style="width:100px;text-align:right;margin:0;" /></div>
        </div>
        <div class="card">
          <div class="section-title">🎯 月度存款目标</div>
          <input class="input" value="2000" placeholder="如 2000" />
        </div>
        <div class="card">
          <div class="section-title">🟡 年度可选预算</div>
          <input class="input" value="6000" /><div class="muted">月均约 ¥500</div>
        </div>
        <button class="btn btn-primary clickable" data-action="save">保存</button>
      </div>`,
  },
  categories: { title: '分类管理', path: 'pages/settings/categories', tab: false, back: 'profile',
    html: `<div class="page"><div class="card"><div style="display:flex;justify-content:space-between;padding:8px 0;"><span>🍜 餐饮</span><span class="muted">显示</span></div><div style="display:flex;justify-content:space-between;padding:8px 0;"><span>👕 服饰</span><span class="muted">隐藏</span></div></div><button class="btn btn-ghost">+ 添加分类</button></div>` },
  recurring: { title: '固定收支', path: 'pages/settings/recurring', tab: false, back: 'profile',
    html: `<div class="page"><div class="card"><div style="font-weight:600;">房租</div><div class="muted">每月1日 · ¥2,100 支出</div></div><div class="card"><div style="font-weight:600;">工资</div><div class="muted">每月10日 · ¥8,000 收入</div></div><button class="btn btn-primary">+ 添加固定项</button></div>` },
  privacy: { title: '隐私与数据', path: 'pages/settings/privacy', tab: false, back: 'profile',
    html: `<div class="page"><div class="card"><div style="font-weight:600;margin-bottom:8px;">AI 财务教练</div><div style="display:flex;justify-content:space-between;align-items:center;"><span class="muted" style="font-size:13px;">仅上传聚合统计</span><span style="color:#6c5ce7;">ON</span></div></div><div class="card" style="padding:0 14px;"><div class="menu-item clickable" data-action="export">📋 导出账单 CSV<span class="arrow">›</span></div><div class="menu-item clickable" data-action="delete-account" style="color:#d63031;">🗑️ 注销账号<span class="arrow">›</span></div></div></div>` },
  reminder: { title: '记账提醒', path: 'pages/settings/reminder', tab: false, back: 'profile',
    html: `<div class="page"><div class="card" style="display:flex;justify-content:space-between;"><div><div style="font-weight:600;">支付后提醒补记</div><div class="muted" style="font-size:12px;">通过微信订阅消息提醒</div></div><span style="color:#6c5ce7;">ON</span></div><button class="btn btn-ghost clickable" data-action="subscribe">测试订阅授权</button></div>` },
  family: { title: '家庭共享', path: 'pages/settings/family', tab: false, back: 'profile',
    html: `<div class="page"><div class="card"><div style="font-weight:600;">小两口</div><div class="muted">角色：户主</div><div class="muted">成员 2 人</div><div style="margin-top:12px;padding:12px;background:#f5f6fa;border-radius:8px;"><div class="muted" style="font-size:12px;">邀请码</div><div style="font-size:24px;font-weight:700;color:#6c5ce7;letter-spacing:4px;">A3K9X2</div><button class="btn btn-ghost btn-sm clickable" data-action="copy">复制邀请码</button></div><button class="btn btn-primary clickable" data-nav="family-summary">查看家庭汇总</button></div></div>` },
  'family-summary': { title: '家庭汇总', path: 'pages/family/summary', tab: false, back: 'family',
    html: `<div class="page"><div class="card"><div style="font-weight:600;">小两口 · 家庭汇总</div><div class="muted">只读 · 2 位成员</div></div><div class="card" style="display:flex;justify-content:space-between;text-align:center;"><div style="flex:1;"><div class="muted">支出</div><div class="expense" style="font-weight:600;">¥3,200</div></div><div style="flex:1;"><div class="muted">结余</div><div style="font-weight:600;">¥4,800</div></div></div><div class="card"><div class="section-title">成员支出</div><div style="display:flex;justify-content:space-between;font-size:14px;margin:6px 0;"><span>吴银龙</span><span>¥2,100</span></div><div style="display:flex;justify-content:space-between;font-size:14px;margin:6px 0;"><span>伴侣</span><span>¥1,100</span></div></div></div>` },
  subscription: { title: '高级版', path: 'pages/settings/subscription', tab: false, back: 'profile',
    html: `<div class="page"><div class="card" style="text-align:center;padding:24px;background:linear-gradient(135deg,rgba(108,92,231,0.1),rgba(255,193,7,0.08));"><div style="font-size:20px;font-weight:700;">免费版</div><div class="muted" style="margin-top:8px;">升级解锁更多能力</div></div><div class="card"><div class="section-title">高级版权益</div><div class="muted" style="font-size:13px;line-height:2;">✓ AI 教练问答不限次<br>✓ 报表大数据加速<br>✓ 专属成就徽章</div></div><button class="btn btn-primary clickable" data-action="subscribe-premium">开通高级版（演示）</button></div>` },
  insights: { title: '洞察中心', path: 'pages/profile/insights', tab: false, back: 'profile',
    html: `<div class="page"><div style="display:flex;justify-content:space-between;margin-bottom:12px;"><span style="font-weight:700;font-size:18px;">洞察中心</span><span style="color:#6c5ce7;font-size:13px;">全部已读</span></div><div class="card" style="border-left:4px solid #6c5ce7;"><div style="display:flex;justify-content:space-between;"><span style="font-size:11px;background:#ffe0e0;color:#d63031;padding:2px 8px;border-radius:99px;">重要</span><span class="muted" style="font-size:11px;">今天</span></div><div style="font-weight:600;margin-top:8px;">可选预算即将用尽</div><div class="muted" style="font-size:13px;margin-top:4px;">已用 85%，建议控制非必要消费</div></div><div class="card"><div style="font-weight:600;">比上月少花 ¥320</div><div class="muted" style="font-size:13px;margin-top:4px;">继续保持！</div></div></div>` },
  coach: { title: 'AI 财务教练', path: 'pages/profile/coach', tab: false, back: 'profile',
    html: `<div class="page"><div class="card"><button class="btn btn-primary btn-sm clickable" data-action="ai-weekly" style="margin:4px 0;">📊 周报解读</button><button class="btn btn-primary btn-sm clickable" data-action="ai-monthly" style="margin:4px 0;">📅 月报解读</button><div class="muted" style="font-size:12px;margin-top:8px;">问答本周剩余 3 次</div></div><div class="card" style="display:flex;gap:8px;"><input class="input" style="flex:1;margin:0;" placeholder="为什么本月花多了？" /><button class="btn btn-primary btn-sm">提问</button></div><div class="card" style="border-left:4px solid #6c5ce7;"><div class="muted" style="font-size:12px;">🤖 AI 教练</div><div style="font-size:13px;margin-top:8px;line-height:1.6;">【周报解读】本周共支出 ¥680，比上周少 17%。支出最高分类是餐饮（占 35%）。</div></div></div>` },
  achievements: { title: '成就徽章', path: 'pages/profile/achievements', tab: false, back: 'profile',
    html: `<div class="page"><div class="muted" style="margin-bottom:12px;">已解锁 8 / 20</div><div class="grid-2">${['🎊 记账新秀','✏️ 第一笔','🔥 初来乍到','🔥 一周坚持','🔥 半月坚持','🔥 月度满勤','💎 精打细算','📉 越来越省'].map((a,i)=>`<div class="card achievement unlocked"><span class="emoji">${a.split(' ')[0]}</span><div style="font-size:12px;font-weight:600;margin-top:4px;">${a.split(' ').slice(1).join(' ')}</div></div>`).join('')}${['🎯 说到做到','📊 数据觉醒'].map(a=>`<div class="card achievement"><span class="emoji">${a.split(' ')[0]}</span><div style="font-size:12px;margin-top:4px;color:#b2bec3;">${a.split(' ').slice(1).join(' ')}</div></div>`).join('')}</div></div>` },
  'bind-phone': { title: '绑定手机号', path: 'pages/auth/bind-phone', tab: false, back: 'profile',
    html: `<div class="page" style="padding-top:40px;text-align:center;"><div class="card"><div class="section-title">绑定手机号</div><input class="input" placeholder="请输入手机号" /><button class="btn btn-primary clickable" data-action="bind">确认绑定</button><button class="btn btn-ghost" data-nav="profile">暂时跳过</button></div></div>` },
}

let currentScreen = 'login'
let history = []
const logEl = () => document.getElementById('interaction-log')

function log(msg) {
  const el = logEl()
  if (!el) return
  const item = document.createElement('div')
  item.className = 'log-item'
  item.innerHTML = `<strong>${new Date().toLocaleTimeString()}</strong> ${msg}`
  el.prepend(item)
  if (el.children.length > 20) el.lastChild.remove()
}

function showToast(msg) {
  const t = document.getElementById('toast')
  t.textContent = msg
  t.classList.add('show')
  setTimeout(() => t.classList.remove('show'), 2000)
  log(`Toast: ${msg}`)
}

function navigate(id, pushHistory = true) {
  if (!SCREENS[id]) return
  if (pushHistory && currentScreen !== id) history.push(currentScreen)
  currentScreen = id
  render()
  log(`导航 → <strong>${SCREENS[id].title}</strong> (${SCREENS[id].path})`)
}

function goBack() {
  const screen = SCREENS[currentScreen]
  if (screen.back) {
    navigate(screen.back, false)
    history.pop()
  } else if (history.length) {
    navigate(history.pop(), false)
  }
}

function render() {
  const screen = SCREENS[currentScreen]
  document.getElementById('screen-name').textContent = screen.title
  document.getElementById('screen-path').textContent = screen.path
  document.getElementById('notch-title').textContent = screen.tab ? screen.title : ''
  document.getElementById('screen-area').innerHTML = screen.html

  const tabBar = document.getElementById('tab-bar')
  tabBar.style.display = screen.tab ? 'flex' : 'none'

  document.querySelectorAll('.tab-btn').forEach((btn) => {
    btn.classList.toggle('active', btn.dataset.tab === currentScreen)
  })

  document.querySelectorAll('.nav-item').forEach((btn) => {
    btn.classList.toggle('active', btn.dataset.screen === currentScreen)
  })

  bindClicks()
}

function bindClicks() {
  document.getElementById('screen-area').querySelectorAll('[data-nav]').forEach((el) => {
    el.addEventListener('click', (e) => {
      e.stopPropagation()
      navigate(el.dataset.nav)
    })
  })

  document.getElementById('screen-area').querySelectorAll('[data-action]').forEach((el) => {
    el.addEventListener('click', (e) => {
      e.stopPropagation()
      handleAction(el.dataset.action)
    })
  })
}

function handleAction(action) {
  const actions = {
    'onboarding-step': () => { showToast('进入收入与模板设置…'); setTimeout(() => navigate('record'), 800) },
    voice: () => showToast('🎤 语音识别：午饭38 → 已填入'),
    edit: () => showToast('进入编辑模式'),
    'delete-hint': () => showToast('长按删除记录（演示）'),
    subscribe: () => showToast('请求订阅消息授权'),
    'import-done': () => showToast('已导入 2 条记录'),
    'longpress-budget': () => { showToast('长按 → 跳转预算设置'); setTimeout(() => navigate('budget'), 600) },
    share: () => showToast('已记录分享'),
    'ai-weekly': () => { showToast('生成周报解读…'); setTimeout(() => navigate('coach'), 500) },
    'ai-monthly': () => { showToast('生成月报解读…'); setTimeout(() => navigate('coach'), 500) },
    deal: () => showToast('链接已复制，请在浏览器打开官方页面'),
    'calc-loan': () => { document.getElementById('loan-result').style.display = 'block'; showToast('计算完成') },
    'sync-recurring': () => showToast('已添加固定支出：房贷月供 ¥4,685'),
    save: () => showToast('已保存'),
    export: () => showToast('账单 CSV 已导出'),
    'delete-account': () => showToast('账号已注销（演示）'),
    'subscribe-premium': () => showToast('已开通高级版 ⭐'),
    copy: () => showToast('邀请码 A3K9X2 已复制'),
    bind: () => { showToast('绑定成功'); setTimeout(() => navigate('profile'), 600) },
    key: () => showToast('输入数字'),
    'loan-mortgage': () => showToast('切换：房贷'),
    'loan-car': () => showToast('切换：车贷'),
  }
  if (actions[action]) actions[action]()
  else log(`动作: ${action}`)
}

function init() {
  document.querySelectorAll('.nav-item').forEach((btn) => {
    btn.addEventListener('click', () => navigate(btn.dataset.screen))
  })

  document.querySelectorAll('.tab-btn').forEach((btn) => {
    btn.addEventListener('click', () => {
      history = []
      navigate(btn.dataset.tab)
    })
  })

  document.getElementById('btn-back').addEventListener('click', goBack)

  render()
  log('原型已加载，点击左侧页面列表或手机内可交互元素开始体验')
}

document.addEventListener('DOMContentLoaded', init)
