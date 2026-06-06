# 家计通云端 API 约定

`VITE_AUTH_MODE=cloud` 时，前端将请求以下接口。可用云函数 / LeanCloud / 自建 Node 服务实现。

## POST /auth/wx-login

**请求：**

```json
{ "code": "微信 wx.login 返回的 code" }
```

**响应：**

```json
{
  "user": {
    "id": "user_xxx",
    "wxOpenId": "oXXXX",
    "nickname": "微信用户",
    "avatar": "",
    "onboardingCompleted": false,
    "createdAt": 1717660800000
  },
  "token": "jwt_or_session_token",
  "expiresAt": 1718265600000,
  "isNewUser": true
}
```

服务端需用 `code` 调用微信 `jscode2session` 换取 `openid`。

## POST /auth/bind-phone

**Header：** `Authorization: Bearer <token>`

**请求（微信授权）：**

```json
{
  "code": "getPhoneNumber 返回的 code"
}
```

或本地测试：

```json
{ "phone": "13800138000" }
```

**响应：**

```json
{
  "user": { "...": "更新后的用户对象，含 phone" }
}
```

## GET /auth/me

校验 token，返回当前用户。

## GET /sync/records?userId=xxx

拉取用户全部记账记录。

## POST /sync/records/batch

**请求：**

```json
{
  "operations": [
    {
      "id": "sync_xxx",
      "entity": "record",
      "type": "upsert",
      "payload": { "...RecordItem" },
      "createdAt": 1717660800000,
      "retryCount": 0
    }
  ]
}
```

**冲突策略：** 服务端按 `updatedAt` 较新者覆盖，与客户端 `mergeRecords` 一致。

## POST /ai/coach

`VITE_AI_MODE=cloud` 时调用。请求体为聚合统计（见 `services/ai/types.ts`），**禁止**上传商家明细与备注原文。

**响应：**

```json
{
  "content": "解读文案…",
  "scene": "weekly | monthly | qa",
  "generatedAt": 1717660800000
}
```

## 家庭共享（v1.2）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/family/create` | 创建家庭，返回 `inviteCode` |
| POST | `/family/join` | `{ "inviteCode": "ABC123" }` 只读加入 |
| GET | `/family/summary` | 家庭聚合汇总（成员只读） |
| POST | `/family/snapshot` | 户主刷新汇总快照 |

本地模式（`VITE_AUTH_MODE=local`）使用 `family_registry` 存储模拟，跨设备需云端 API。

## GET /alliance/coupons

`VITE_ALLIANCE_MODE=cloud` 时拉取联盟优惠券列表。响应为 `Coupon[]`（见 `types/coupon.ts`）。

## 家庭协作账本（v2.0）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/family/records` | 成员提交记账（含 `memberId`） |
| GET | `/family/records?familyId=` | 户主拉取家庭账本聚合 |
