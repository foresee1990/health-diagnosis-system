# Authentication

# AuthController

## POST 用户注册接口

POST /api/auth/register

> Body 请求参数

```json
{
  "username": "string",
  "password": "string",
  "email": "user@example.com"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[RegisterRequest](#schemaregisterrequest)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "message": "string",
  "data": null,
  "timestamp": 0
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[Result?](#schemaresult?)|

## POST 用户登录接口

POST /api/auth/login

> Body 请求参数

```json
{
  "username": "string",
  "password": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[LoginRequest](#schemaloginrequest)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "message": "string",
  "data": null,
  "timestamp": 0
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[Result?](#schemaresult?)|

## GET 获取用户信息接口

GET /api/users/me

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "message": "string",
  "data": null,
  "timestamp": 0
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[Result?](#schemaresult?)|

# ConsultationController

## POST 创建问诊会话

POST /api/consultations

POST /api/consultations

> Body 请求参数

```json
{
  "chiefComplaint": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[CreateConsultationRequest](#schemacreateconsultationrequest)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "message": "string",
  "data": {
    "consultationId": 0,
    "status": "string",
    "createdAt": "string"
  },
  "timestamp": 0
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ResultConsultationResponse](#schemaresultconsultationresponse)|

## GET 获取问诊会话列表

GET /api/consultations

GET /api/consultations

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|page|query|integer| 是 |none|
|size|query|integer| 是 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "message": "string",
  "data": {
    "total": 0,
    "page": 0,
    "size": 0,
    "pages": 0,
    "consultations": [
      {
        "id": 0,
        "chiefComplaint": "string",
        "status": "string",
        "riskLevel": "string",
        "createdAt": "string",
        "completedAt": "string"
      }
    ]
  },
  "timestamp": 0
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ResultConsultationPageResponse](#schemaresultconsultationpageresponse)|

## POST 发送消息

POST /api/consultations/{consultationId}/messages

POST /api/consultations/{consultationId}/messages

> Body 请求参数

```json
{
  "content": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|consultationId|path|integer| 是 |none|
|body|body|[SendMessageRequest](#schemasendmessagerequest)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "message": "string",
  "data": {
    "userMessage": {
      "id": 0,
      "role": "string",
      "content": "string",
      "createdAt": "string"
    },
    "assistantReply": {
      "id": 0,
      "role": "string",
      "content": "string",
      "createdAt": "string"
    }
  },
  "timestamp": 0
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ResultSendMessageResponse](#schemaresultsendmessageresponse)|

## GET getMessages

GET /api/consultations/{consultationId}/messages

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|consultationId|path|integer| 是 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "message": "string",
  "data": {
    "consultationId": 0,
    "status": "string",
    "riskLevel": "string",
    "messages": [
      {
        "id": 0,
        "role": "string",
        "content": "string",
        "createdAt": "string"
      }
    ]
  },
  "timestamp": 0
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ResultConsultationMessagesResponse](#schemaresultconsultationmessagesresponse)|

## POST 发送消息（SSE 流式）

POST /api/consultations/{consultationId}/messages/stream

POST /api/consultations/{consultationId}/messages/stream

> Body 请求参数

```json
{
  "content": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|consultationId|path|integer| 是 |none|
|body|body|[SendMessageRequest](#schemasendmessagerequest)| 否 |none|

> 返回示例

> 200 Response

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|*anonymous*|[[ServerSentEventString](#schemaserversenteventstring)]|false|none||none|
|» id|string¦null|false|none||none|
|» event|string¦null|false|none||none|
|» retry|[Duration](#schemaduration)¦null|false|none||none|
|»» seconds|integer(int64)|false|none||The number of seconds in the duration.|
|»» nanos|integer|false|none||The number of nanoseconds in the duration, expressed as a fraction of the<br />number of seconds. This is always positive, and never exceeds 999,999,999.|
|» comment|string¦null|false|none||none|
|» data|string¦null|false|none||none|

## PATCH 结束问诊会话

PATCH /api/consultations/{consultationId}/status

PATCH /api/consultations/{consultationId}/status

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|consultationId|path|integer| 是 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "message": "string",
  "data": {
    "id": 0,
    "chiefComplaint": "string",
    "status": "string",
    "riskLevel": "string",
    "createdAt": "string",
    "completedAt": "string"
  },
  "timestamp": 0
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ResultConsultationItemResponse](#schemaresultconsultationitemresponse)|

# ReportController

## POST 生成问诊报告

POST /api/consultations/{consultationId}/report

POST /api/consultations/{consultationId}/report

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|consultationId|path|integer| 是 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "message": "string",
  "data": {
    "reportId": 0,
    "downloadUrl": "string",
    "fileSize": 0,
    "createdAt": "string"
  },
  "timestamp": 0
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ResultReportVO](#schemaresultreportvo)|

## GET 查询问诊报告信息

GET /api/consultations/{consultationId}/report

GET /api/consultations/{consultationId}/report

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|consultationId|path|integer| 是 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "message": "string",
  "data": {
    "reportId": 0,
    "downloadUrl": "string",
    "fileSize": 0,
    "createdAt": "string"
  },
  "timestamp": 0
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ResultReportVO](#schemaresultreportvo)|

## GET 下载 PDF 报告文件

GET /api/reports/{reportId}/file

GET /api/reports/{reportId}/file

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|reportId|path|integer| 是 |none|

> 返回示例

> 200 Response

```json
{}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

# 数据模型

<h2 id="tocS_Result?">Result?</h2>

<a id="schemaresult?"></a>
<a id="schema_Result?"></a>
<a id="tocSresult?"></a>
<a id="tocsresult?"></a>

```json
{
  "code": 0,
  "message": "string",
  "data": null,
  "timestamp": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|false|none||状态码 (HTTP 状态码或业务状态码)|
|message|string|false|none||响应消息|
|data|null|false|none||响应数据|
|timestamp|integer(int64)|false|none||时间戳 (毫秒)<br />默认值：System.currentTimeMillis()|

<h2 id="tocS_RegisterRequest">RegisterRequest</h2>

<a id="schemaregisterrequest"></a>
<a id="schema_RegisterRequest"></a>
<a id="tocSregisterrequest"></a>
<a id="tocsregisterrequest"></a>

```json
{
  "username": "string",
  "password": "string",
  "email": "user@example.com"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|username|string|true|none||用户名：非空，长度3-50|
|password|string|true|none||密码：非空，长度6-20|
|email|string(email)|false|none||邮箱：可选，格式验证|

<h2 id="tocS_LoginRequest">LoginRequest</h2>

<a id="schemaloginrequest"></a>
<a id="schema_LoginRequest"></a>
<a id="tocSloginrequest"></a>
<a id="tocsloginrequest"></a>

```json
{
  "username": "string",
  "password": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|username|string|true|none||用户名|
|password|string|true|none||密码|

<h2 id="tocS_ConsultationResponse">ConsultationResponse</h2>

<a id="schemaconsultationresponse"></a>
<a id="schema_ConsultationResponse"></a>
<a id="tocSconsultationresponse"></a>
<a id="tocsconsultationresponse"></a>

```json
{
  "consultationId": 0,
  "status": "string",
  "createdAt": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|consultationId|integer(int64)|false|none||none|
|status|string|false|none||none|
|createdAt|string|false|none||none|

<h2 id="tocS_ReportVO">ReportVO</h2>

<a id="schemareportvo"></a>
<a id="schema_ReportVO"></a>
<a id="tocSreportvo"></a>
<a id="tocsreportvo"></a>

```json
{
  "reportId": 0,
  "downloadUrl": "string",
  "fileSize": 0,
  "createdAt": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|reportId|integer(int64)|false|none||none|
|downloadUrl|string|false|none||none|
|fileSize|integer|false|none||none|
|createdAt|string|false|none||none|

<h2 id="tocS_ResultConsultationResponse">ResultConsultationResponse</h2>

<a id="schemaresultconsultationresponse"></a>
<a id="schema_ResultConsultationResponse"></a>
<a id="tocSresultconsultationresponse"></a>
<a id="tocsresultconsultationresponse"></a>

```json
{
  "code": 0,
  "message": "string",
  "data": {
    "consultationId": 0,
    "status": "string",
    "createdAt": "string"
  },
  "timestamp": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|false|none||状态码 (HTTP 状态码或业务状态码)|
|message|string|false|none||响应消息|
|data|[ConsultationResponse](#schemaconsultationresponse)|false|none||响应数据|
|timestamp|integer(int64)|false|none||时间戳 (毫秒)<br />默认值：System.currentTimeMillis()|

<h2 id="tocS_ResultReportVO">ResultReportVO</h2>

<a id="schemaresultreportvo"></a>
<a id="schema_ResultReportVO"></a>
<a id="tocSresultreportvo"></a>
<a id="tocsresultreportvo"></a>

```json
{
  "code": 0,
  "message": "string",
  "data": {
    "reportId": 0,
    "downloadUrl": "string",
    "fileSize": 0,
    "createdAt": "string"
  },
  "timestamp": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|false|none||状态码 (HTTP 状态码或业务状态码)|
|message|string|false|none||响应消息|
|data|[ReportVO](#schemareportvo)|false|none||响应数据|
|timestamp|integer(int64)|false|none||时间戳 (毫秒)<br />默认值：System.currentTimeMillis()|

<h2 id="tocS_CreateConsultationRequest">CreateConsultationRequest</h2>

<a id="schemacreateconsultationrequest"></a>
<a id="schema_CreateConsultationRequest"></a>
<a id="tocScreateconsultationrequest"></a>
<a id="tocscreateconsultationrequest"></a>

```json
{
  "chiefComplaint": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|chiefComplaint|string|true|none||主诉内容<br />规则：不能为空，长度 1~500 字符|

<h2 id="tocS_SendMessageResponse">SendMessageResponse</h2>

<a id="schemasendmessageresponse"></a>
<a id="schema_SendMessageResponse"></a>
<a id="tocSsendmessageresponse"></a>
<a id="tocssendmessageresponse"></a>

```json
{
  "userMessage": {
    "id": 0,
    "role": "string",
    "content": "string",
    "createdAt": "string"
  },
  "assistantReply": {
    "id": 0,
    "role": "string",
    "content": "string",
    "createdAt": "string"
  }
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|userMessage|[MessageResponse](#schemamessageresponse)|false|none||none|
|assistantReply|[MessageResponse](#schemamessageresponse)|false|none||none|

<h2 id="tocS_ResultSendMessageResponse">ResultSendMessageResponse</h2>

<a id="schemaresultsendmessageresponse"></a>
<a id="schema_ResultSendMessageResponse"></a>
<a id="tocSresultsendmessageresponse"></a>
<a id="tocsresultsendmessageresponse"></a>

```json
{
  "code": 0,
  "message": "string",
  "data": {
    "userMessage": {
      "id": 0,
      "role": "string",
      "content": "string",
      "createdAt": "string"
    },
    "assistantReply": {
      "id": 0,
      "role": "string",
      "content": "string",
      "createdAt": "string"
    }
  },
  "timestamp": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|false|none||状态码 (HTTP 状态码或业务状态码)|
|message|string|false|none||响应消息|
|data|[SendMessageResponse](#schemasendmessageresponse)|false|none||响应数据|
|timestamp|integer(int64)|false|none||时间戳 (毫秒)<br />默认值：System.currentTimeMillis()|

<h2 id="tocS_SendMessageRequest">SendMessageRequest</h2>

<a id="schemasendmessagerequest"></a>
<a id="schema_SendMessageRequest"></a>
<a id="tocSsendmessagerequest"></a>
<a id="tocssendmessagerequest"></a>

```json
{
  "content": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|content|string|true|none||none|

<h2 id="tocS_MessageResponse">MessageResponse</h2>

<a id="schemamessageresponse"></a>
<a id="schema_MessageResponse"></a>
<a id="tocSmessageresponse"></a>
<a id="tocsmessageresponse"></a>

```json
{
  "id": 0,
  "role": "string",
  "content": "string",
  "createdAt": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer(int64)|false|none||none|
|role|string|false|none||none|
|content|string|false|none||none|
|createdAt|string|false|none||none|

<h2 id="tocS_Duration">Duration</h2>

<a id="schemaduration"></a>
<a id="schema_Duration"></a>
<a id="tocSduration"></a>
<a id="tocsduration"></a>

```json
{
  "seconds": 0,
  "nanos": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|seconds|integer(int64)|false|none||The number of seconds in the duration.|
|nanos|integer|false|none||The number of nanoseconds in the duration, expressed as a fraction of the<br />number of seconds. This is always positive, and never exceeds 999,999,999.|

<h2 id="tocS_ConsultationMessagesResponse">ConsultationMessagesResponse</h2>

<a id="schemaconsultationmessagesresponse"></a>
<a id="schema_ConsultationMessagesResponse"></a>
<a id="tocSconsultationmessagesresponse"></a>
<a id="tocsconsultationmessagesresponse"></a>

```json
{
  "consultationId": 0,
  "status": "string",
  "riskLevel": "string",
  "messages": [
    {
      "id": 0,
      "role": "string",
      "content": "string",
      "createdAt": "string"
    }
  ]
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|consultationId|integer(int64)|false|none||none|
|status|string|false|none||none|
|riskLevel|string|false|none||none|
|messages|[[MessageResponse](#schemamessageresponse)]|false|none||none|

<h2 id="tocS_ServerSentEventString">ServerSentEventString</h2>

<a id="schemaserversenteventstring"></a>
<a id="schema_ServerSentEventString"></a>
<a id="tocSserversenteventstring"></a>
<a id="tocsserversenteventstring"></a>

```json
{
  "id": "string",
  "event": "string",
  "retry": {
    "seconds": 0,
    "nanos": 0
  },
  "comment": "string",
  "data": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|string¦null|false|none||none|
|event|string¦null|false|none||none|
|retry|[Duration](#schemaduration)|false|none||none|
|comment|string¦null|false|none||none|
|data|string¦null|false|none||none|

<h2 id="tocS_ResultConsultationMessagesResponse">ResultConsultationMessagesResponse</h2>

<a id="schemaresultconsultationmessagesresponse"></a>
<a id="schema_ResultConsultationMessagesResponse"></a>
<a id="tocSresultconsultationmessagesresponse"></a>
<a id="tocsresultconsultationmessagesresponse"></a>

```json
{
  "code": 0,
  "message": "string",
  "data": {
    "consultationId": 0,
    "status": "string",
    "riskLevel": "string",
    "messages": [
      {
        "id": 0,
        "role": "string",
        "content": "string",
        "createdAt": "string"
      }
    ]
  },
  "timestamp": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|false|none||状态码 (HTTP 状态码或业务状态码)|
|message|string|false|none||响应消息|
|data|[ConsultationMessagesResponse](#schemaconsultationmessagesresponse)|false|none||响应数据|
|timestamp|integer(int64)|false|none||时间戳 (毫秒)<br />默认值：System.currentTimeMillis()|

<h2 id="tocS_ConsultationItemResponse">ConsultationItemResponse</h2>

<a id="schemaconsultationitemresponse"></a>
<a id="schema_ConsultationItemResponse"></a>
<a id="tocSconsultationitemresponse"></a>
<a id="tocsconsultationitemresponse"></a>

```json
{
  "id": 0,
  "chiefComplaint": "string",
  "status": "string",
  "riskLevel": "string",
  "createdAt": "string",
  "completedAt": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer(int64)|false|none||none|
|chiefComplaint|string|false|none||none|
|status|string|false|none||none|
|riskLevel|string|false|none||none|
|createdAt|string|false|none||none|
|completedAt|string|false|none||none|

<h2 id="tocS_ConsultationPageResponse">ConsultationPageResponse</h2>

<a id="schemaconsultationpageresponse"></a>
<a id="schema_ConsultationPageResponse"></a>
<a id="tocSconsultationpageresponse"></a>
<a id="tocsconsultationpageresponse"></a>

```json
{
  "total": 0,
  "page": 0,
  "size": 0,
  "pages": 0,
  "consultations": [
    {
      "id": 0,
      "chiefComplaint": "string",
      "status": "string",
      "riskLevel": "string",
      "createdAt": "string",
      "completedAt": "string"
    }
  ]
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|total|integer(int64)|false|none||none|
|page|integer(int64)|false|none||none|
|size|integer(int64)|false|none||none|
|pages|integer(int64)|false|none||none|
|consultations|[[ConsultationItemResponse](#schemaconsultationitemresponse)]|false|none||none|

<h2 id="tocS_ResultConsultationPageResponse">ResultConsultationPageResponse</h2>

<a id="schemaresultconsultationpageresponse"></a>
<a id="schema_ResultConsultationPageResponse"></a>
<a id="tocSresultconsultationpageresponse"></a>
<a id="tocsresultconsultationpageresponse"></a>

```json
{
  "code": 0,
  "message": "string",
  "data": {
    "total": 0,
    "page": 0,
    "size": 0,
    "pages": 0,
    "consultations": [
      {
        "id": 0,
        "chiefComplaint": "string",
        "status": "string",
        "riskLevel": "string",
        "createdAt": "string",
        "completedAt": "string"
      }
    ]
  },
  "timestamp": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|false|none||状态码 (HTTP 状态码或业务状态码)|
|message|string|false|none||响应消息|
|data|[ConsultationPageResponse](#schemaconsultationpageresponse)|false|none||响应数据|
|timestamp|integer(int64)|false|none||时间戳 (毫秒)<br />默认值：System.currentTimeMillis()|

<h2 id="tocS_ResultConsultationItemResponse">ResultConsultationItemResponse</h2>

<a id="schemaresultconsultationitemresponse"></a>
<a id="schema_ResultConsultationItemResponse"></a>
<a id="tocSresultconsultationitemresponse"></a>
<a id="tocsresultconsultationitemresponse"></a>

```json
{
  "code": 0,
  "message": "string",
  "data": {
    "id": 0,
    "chiefComplaint": "string",
    "status": "string",
    "riskLevel": "string",
    "createdAt": "string",
    "completedAt": "string"
  },
  "timestamp": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|false|none||状态码 (HTTP 状态码或业务状态码)|
|message|string|false|none||响应消息|
|data|[ConsultationItemResponse](#schemaconsultationitemresponse)|false|none||响应数据|
|timestamp|integer(int64)|false|none||时间戳 (毫秒)<br />默认值：System.currentTimeMillis()|

<h2 id="tocS_UpdateConsultationStatusRequest">UpdateConsultationStatusRequest</h2>

<a id="schemaupdateconsultationstatusrequest"></a>
<a id="schema_UpdateConsultationStatusRequest"></a>
<a id="tocSupdateconsultationstatusrequest"></a>
<a id="tocsupdateconsultationstatusrequest"></a>

```json
{
  "status": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|status|string|true|none||none|

