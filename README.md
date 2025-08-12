# Consul Customer Service ⚡

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.0-blue.svg)](https://spring.io/projects/spring-cloud)
[![Consul](https://img.shields.io/badge/Consul-1.17.0-purple.svg)](https://www.consul.io/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## 專案介紹

這是一個基於 Spring Cloud 的微服務客戶端應用程式，使用 **Consul** 作為服務註冊與發現中心。專案展示了如何在微服務架構中實現服務間的動態發現與負載均衡，讓客戶端能夠自動發現並連接到可用的服務提供者。

### 🎯 核心功能
- **服務發現**：透過 Consul 自動發現可用的 waiter-service
- **負載均衡**：使用 Spring Cloud LoadBalancer 實現請求分散
- **健康檢查**：整合 Consul 的健康檢查機制
- **配置管理**：支援動態配置更新
- **HTTP 客戶端**：使用 RestTemplate 進行服務間通訊

### 💡 為什麼選擇 Consul？
- **多資料中心支援**：原生支援跨資料中心的服務發現
- **DNS 整合**：可透過 DNS 方式解析服務，無需修改現有基礎設施
- **健康檢查**：內建多種健康檢查機制（HTTP、TCP、Script 等）
- **配置中心**：不僅是服務發現，還可作為配置管理中心
- **安全性**：支援 TLS 加密通訊和 ACL 存取控制

### 🎯 專案特色

- **零配置切換**：只需更換依賴即可從 Eureka 切換到 Consul
- **多種服務發現方式**：支援 HTTP API、DNS、Nginx 整合
- **生產級別**：具備完整的健康檢查和故障轉移機制
- **開發友善**：提供完整的本地開發環境設定

## 技術棧

### 核心框架
- **Spring Boot 3.2.0** - 微服務應用程式框架
- **Spring Cloud 2023.0.0** - 微服務生態系統
- **Spring Cloud Consul Discovery** - Consul 服務發現整合
- **Spring Cloud LoadBalancer** - 客戶端負載均衡

### 開發工具與輔助
- **Apache HttpClient 5.2.1** - HTTP 客戶端連線池管理
- **Apache Commons Lang3 3.12.0** - 工具類庫
- **Joda Money 2.0.2** - 貨幣處理
- **Lombok** - 減少樣板程式碼
- **Maven** - 專案建置與依賴管理

## 專案結構

```
consul-customer-service/
├── src/
│   ├── main/
│   │   ├── java/tw/fengqing/springbucks/customer/
│   │   │   ├── CustomerServiceApplication.java    # 主應用程式類別
│   │   │   ├── CustomerRunner.java                # 應用程式啟動執行器
│   │   │   ├── model/                             # 資料模型
│   │   │   │   ├── Coffee.java                    # 咖啡實體類別
│   │   │   │   ├── CoffeeOrder.java               # 訂單實體類別
│   │   │   │   ├── NewOrderRequest.java           # 新訂單請求
│   │   │   │   └── OrderState.java                # 訂單狀態列舉
│   │   │   └── support/                           # 支援類別
│   │   │       ├── CustomConnectionKeepAliveStrategy.java  # 自訂連線保持策略
│   │   │       ├── MoneyDeserializer.java         # 貨幣反序列化器
│   │   │       └── MoneySerializer.java           # 貨幣序列化器
│   │   └── resources/
│   │       ├── application.properties             # 應用程式配置
│   │       └── bootstrap.properties               # 啟動配置
│   └── test/
│       └── java/tw/fengqing/spring/springbucks/customer/
│           └── CustomerServiceApplicationTests.java
├── pom.xml                                        # Maven 專案配置
└── README.md                                      # 專案說明文件
```

## 快速開始

### 前置需求
- **Java 21** 或更高版本
- **Maven 3.6+** 或更高版本
- **Consul 1.17.0** 或更高版本
- **Docker**（可選，用於快速啟動 Consul）

### 安裝與執行

#### 1. 啟動 Consul 服務

**使用 Docker（推薦）：**
```bash
# 啟動 Consul 開發模式，包含 UI 介面
docker run -d --name consul \
  -p 8500:8500 \
  -p 8600:8600/udp \
  consul:1.4.5
```

#### 2. 克隆並編譯專案
```bash
# 進入專案目錄
cd consul-customer-service

# 編譯專案
mvn clean compile

# 打包專案
mvn clean package -DskipTests
```

#### 3. 執行應用程式
```bash
# 使用 Maven 執行
mvn spring-boot:run

# 或使用 JAR 檔案執行
java -jar target/customer-service-0.0.1-SNAPSHOT.jar
```

#### 4. 驗證服務
```bash
# 檢查應用程式是否正常啟動
curl http://localhost:8080/actuator/health

# 查看 Consul UI（瀏覽器開啟）
http://localhost:8500
```

## 進階說明

### 環境變數
```properties
# Consul 服務發現配置
CONSUL_HOST=localhost
CONSUL_PORT=8500
CONSUL_DISCOVERY_SERVICE_NAME=customer-service

# 應用程式配置
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev
```

### 設定檔說明

#### bootstrap.properties
```properties
# 應用程式名稱（服務註冊名稱）
spring.application.name=customer-service

# Consul 服務發現配置
spring.cloud.consul.host=localhost
spring.cloud.consul.port=8500
spring.cloud.consul.discovery.service-name=${spring.application.name}
spring.cloud.consul.discovery.instance-id=${spring.application.name}:${server.port}
spring.cloud.consul.discovery.health-check-path=/actuator/health
spring.cloud.consul.discovery.health-check-interval=15s
```

#### application.properties
```properties
# 伺服器配置
server.port=8080

# 日誌配置
logging.level.tw.fengqing.springbucks=DEBUG
logging.level.org.springframework.cloud.consul=DEBUG

# Actuator 配置
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=always
```

### DNS 服務發現測試
```bash
# 使用 dig 命令測試 DNS 服務發現
dig @localhost -p 8600 waiter-service.service.consul

# 使用 nslookup 測試
nslookup waiter-service.service.consul localhost
```

## 參考資源

- [Spring Cloud Consul 官方文件](https://docs.spring.io/spring-cloud-consul/docs/current/reference/html/)
- [Consul 官方文件](https://www.consul.io/docs)
- [Spring Boot 參考指南](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [微服務架構實戰課程](https://blog.fengqing.tw/)

## 注意事項與最佳實踐

### ⚠️ 重要提醒

| 項目 | 說明 | 建議做法 |
|------|------|----------|
| 服務註冊 | 服務名稱唯一性 | 使用 `spring.application.name` 確保唯一性 |
| 健康檢查 | 端點可用性 | 實作 `/actuator/health` 端點 |
| 連線池管理 | HTTP 客戶端效能 | 使用 HttpClient 連線池配置 |
| 故障轉移 | 服務不可用處理 | 實作重試機制和熔斷器 |

### 🔒 最佳實踐指南

#### 1. 服務發現配置
```java
// 在 CustomerServiceApplication.java 中配置 RestTemplate
@LoadBalanced  // 啟用負載均衡
@Bean
public RestTemplate restTemplate(RestTemplateBuilder builder) {
    return builder
        .setConnectTimeout(Duration.ofMillis(100))  // 連線超時設定
        .setReadTimeout(Duration.ofMillis(500))     // 讀取超時設定
        .build();
}
```

#### 2. 自訂連線保持策略
```java
// CustomConnectionKeepAliveStrategy.java
// 實作自訂的 Keep-Alive 策略，提升連線效能
public class CustomConnectionKeepAliveStrategy implements ConnectionKeepAliveStrategy {
    private final long DEFAULT_SECONDS = 30;
    
    @Override
    public TimeValue getKeepAliveDuration(HttpResponse response, HttpContext context) {
        // 根據伺服器回應的 Keep-Alive 標頭設定連線保持時間
        // 預設為 30 秒
        return TimeValue.ofSeconds(DEFAULT_SECONDS);
    }
}
```

#### 3. 貨幣處理序列化
```java
// MoneySerializer.java 和 MoneyDeserializer.java
// 處理 Joda Money 物件的 JSON 序列化和反序列化
// 確保貨幣資料的正確傳輸和處理
```

### 🚀 效能優化建議

1. **連線池配置**：根據實際負載調整 HttpClient 連線池大小
2. **健康檢查間隔**：平衡可用性和效能，建議 15-30 秒
3. **快取策略**：實作服務清單快取，減少 Consul 查詢頻率
4. **監控指標**：整合 Micrometer 監控服務發現效能

## 授權說明

本專案採用 MIT 授權條款，詳見 LICENSE 檔案。

## 關於我們

我們主要專注在敏捷專案管理、物聯網（IoT）應用開發和領域驅動設計（DDD）。喜歡把先進技術和實務經驗結合，打造好用又靈活的軟體解決方案。

## 聯繫我們

- **FB 粉絲頁**：[風清雲談 | Facebook](https://www.facebook.com/profile.php?id=61576838896062)
- **LinkedIn**：[linkedin.com/in/chu-kuo-lung](https://www.linkedin.com/in/chu-kuo-lung)
- **YouTube 頻道**：[雲談風清 - YouTube](https://www.youtube.com/channel/UCXDqLTdCMiCJ1j8xGRfwEig)
- **風清雲談 部落格**：[風清雲談](https://blog.fengqing.tw/)
- **電子郵件**：[fengqing.tw@gmail.com](mailto:fengqing.tw@gmail.com)

---

**📅 最後更新：2024年12月19日**  
**👨‍💻 維護者：風清雲談團隊**  
**🔧 版本：0.0.1-SNAPSHOT**
