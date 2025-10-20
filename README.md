# consul-customer-service

> Microservice consumer with Consul service discovery and client-side load balancing

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2024.0.2-blue.svg)](https://spring.io/projects/spring-cloud)
[![Consul](https://img.shields.io/badge/Consul-1.4.5-purple.svg)](https://www.consul.io/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A comprehensive demonstration of **service consumer** using **Consul** for service discovery and **Spring Cloud LoadBalancer** for client-side load balancing, featuring RestTemplate with service name resolution, connection pool management, and Money type serialization.

## Features

- Consul service discovery (auto-discover waiter-service)
- Client-side load balancing (Spring Cloud LoadBalancer)
- Service-to-service communication via RestTemplate
- @LoadBalanced for automatic service name resolution
- Apache HttpClient 5 connection pool management
- Custom Keep-Alive strategy
- Money type JSON serialization/deserialization
- ApplicationRunner for auto-execution on startup
- Random port assignment for multi-instance deployment

## Tech Stack

- Spring Boot 3.4.5
- Spring Cloud 2024.0.2
- Spring Cloud Consul Discovery
- Spring Cloud LoadBalancer
- Java 21
- Apache HttpClient 5
- Joda Money 2.0.2
- Apache Commons Lang3
- Lombok
- Maven 3.8+

## Getting Started

### Prerequisites

- JDK 21 or higher
- Maven 3.8+ (or use included Maven Wrapper)
- Consul 1.4.5 or higher
- Docker (for Consul)
- **consul-waiter-service** (service provider must be running)

### Quick Start

**Step 1: Start Consul**

```bash
# Using Docker (recommended)
docker run -d --name consul \
  -p 8500:8500 \
  -p 8600:8600/udp \
  consul:1.4.5

# Verify Consul is running
curl http://localhost:8500/v1/status/leader
# Expected: "127.0.0.1:8300"
```

**Step 2: Start waiter-service (Service Provider)**

```bash
cd ../consul-waiter-service
./mvnw spring-boot:run
```

**Step 3: Start customer-service (Service Consumer)**

```bash
cd consul-customer-service
./mvnw spring-boot:run
```

**Step 4: Observe Auto-Execution**

The application will automatically execute `CustomerRunner` on startup:

1. **Discover waiter-service** via DiscoveryClient
2. **Read coffee menu** (5 coffees)
3. **Create order** (Customer: Ray Chu, Item: capuccino)
4. **Query order** by ID

## Configuration

### Application Properties

```properties
# Server configuration
server.port=0  # Random port assignment

# Error response configuration (Development only)
server.error.include-message=always
server.error.include-binding-errors=always

# Actuator configuration (Development only)
management.endpoints.web.exposure.include=*
management.endpoint.health.show-details=always

# Consul service discovery configuration
spring.cloud.consul.host=localhost
spring.cloud.consul.port=8500
spring.cloud.consul.discovery.prefer-ip-address=true

# Consul health check configuration
spring.cloud.consul.discovery.health-check-path=/actuator/health
spring.cloud.consul.discovery.health-check-interval=10s
spring.cloud.consul.discovery.heartbeat.enabled=true
```

### Bootstrap Properties

```properties
# Service name (used for Consul registration)
spring.application.name=customer-service
```

## Usage

### Application Flow

```
1. Spring Boot starts
   ↓
2. Auto-register to Consul
   - Service Name: customer-service
   - Port: Random (e.g., 65386)
   ↓
3. ApplicationRunner.run() executes:
   ↓
4. showServiceInstances()
   - Query Consul for "waiter-service"
   - Display: Host: 192.168.100.194, Port: 8082
   ↓
5. readMenu()
   - Call: http://waiter-service/coffee/
   - LoadBalancer resolves to: http://192.168.100.194:8082/coffee/
   - Display: 5 coffees (espresso, latte, capuccino, mocha, macchiato)
   ↓
6. orderCoffee()
   - POST: http://waiter-service/order/
   - Body: {"customer":"Ray Chu","items":["capuccino"]}
   - Response: Order ID = 1
   ↓
7. queryOrder(1)
   - GET: http://waiter-service/order/1
   - Display: Order details
```

### Sample Output

```
INFO ... CustomerRunner : DiscoveryClient: org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClient
INFO ... CustomerRunner : Host: 192.168.100.194, Port: 8082
INFO ... CustomerRunner : Coffee: Coffee(id=1, name=espresso, price=TWD 100.00, ...)
INFO ... CustomerRunner : Coffee: Coffee(id=2, name=latte, price=TWD 125.00, ...)
INFO ... CustomerRunner : Coffee: Coffee(id=3, name=capuccino, price=TWD 125.00, ...)
INFO ... CustomerRunner : Coffee: Coffee(id=4, name=mocha, price=TWD 150.00, ...)
INFO ... CustomerRunner : Coffee: Coffee(id=5, name=macchiato, price=TWD 150.00, ...)
INFO ... CustomerRunner : Order Request Status Code: 201 CREATED
INFO ... CustomerRunner : Order ID: 1
INFO ... CustomerRunner : Order: CoffeeOrder(id=1, customer=Ray Chu, items=[...], state=INIT, ...)
```

**Output Analysis:**
- **DiscoveryClient**: CompositeDiscoveryClient (Consul-based)
- **Service Discovery**: Found waiter-service at 192.168.100.194:8082
- **Menu Reading**: Successfully retrieved 5 coffees
- **Order Creation**: HTTP 201 CREATED, Order ID = 1
- **Order Query**: Successfully retrieved order details

## Key Components

### CustomerServiceApplication

```java
@SpringBootApplication
@EnableDiscoveryClient  // Enable Consul service discovery
public class CustomerServiceApplication {
    
    /**
     * Configure Apache HttpClient 5 connection pool
     * Improves HTTP request performance and connection management
     */
    @Bean
    public CloseableHttpClient httpClient() {
        return HttpClients.custom()
            .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(200)              // Max total connections
                .setMaxConnPerRoute(20)            // Max connections per route
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                    .setTimeToLive(TimeValue.ofSeconds(30))  // Connection TTL
                    .build())
                .build())
            .evictIdleConnections(TimeValue.ofSeconds(30))  // Evict idle connections
            .disableAutomaticRetries()             // Disable retries for idempotency
            .setKeepAliveStrategy(new CustomConnectionKeepAliveStrategy())
            .build();
    }
    
    /**
     * Configure RestTemplate with load balancing
     * @LoadBalanced enables Spring Cloud LoadBalancer
     */
    @LoadBalanced  // Enable client-side load balancing
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        HttpComponentsClientHttpRequestFactory requestFactory = 
            new HttpComponentsClientHttpRequestFactory(httpClient());
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));  // Connect timeout
        requestFactory.setReadTimeout(Duration.ofSeconds(1));     // Read timeout
        
        return builder
            .requestFactory(() -> requestFactory)
            .build();
    }
}
```

**Key Configuration:**

| Component | Configuration | Purpose |
|-----------|--------------|---------|
| **@EnableDiscoveryClient** | Enables service discovery | Auto-register to Consul |
| **@LoadBalanced** | Enables load balancing | Resolve service name to IP:Port |
| **HttpClient Pool** | maxTotal=200, maxPerRoute=20 | Connection reuse and performance |
| **Keep-Alive** | Custom strategy (30s) | Reduce connection overhead |
| **Timeouts** | connect=5s, read=1s | Prevent hanging requests |

### CustomConnectionKeepAliveStrategy

```java
public class CustomConnectionKeepAliveStrategy implements ConnectionKeepAliveStrategy {
    private final long DEFAULT_SECONDS = 30;
    
    @Override
    public TimeValue getKeepAliveDuration(HttpResponse response, HttpContext context) {
        // Read "Keep-Alive: timeout=60" from response header
        // If not present, use default value (30 seconds)
        long milliseconds = Arrays.stream(response.getHeaders("Connection"))
            .filter(h -> StringUtils.equalsIgnoreCase(h.getName(), "timeout")
                    && StringUtils.isNumeric(h.getValue()))
            .findFirst()
            .map(h -> NumberUtils.toLong(h.getValue(), DEFAULT_SECONDS))
            .orElse(DEFAULT_SECONDS) * 1000;
        
        return TimeValue.ofMilliseconds(milliseconds);
    }
}
```

**Purpose:**
- Keep connections alive for reuse
- Reduce connection establishment overhead
- Improve HTTP request performance

### CustomerRunner (ApplicationRunner)

```java
@Component
public class CustomerRunner implements ApplicationRunner {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private DiscoveryClient discoveryClient;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        showServiceInstances();  // 1. Show service instances
        readMenu();              // 2. Read coffee menu
        Long id = orderCoffee(); // 3. Create order
        queryOrder(id);          // 4. Query order
    }
    
    /**
     * Show DiscoveryClient type and service instance info
     */
    private void showServiceInstances() {
        log.info("DiscoveryClient: {}", discoveryClient.getClass().getName());
        discoveryClient.getInstances("waiter-service").forEach(s -> {
            log.info("Host: {}, Port: {}", s.getHost(), s.getPort());
        });
    }
    
    /**
     * Read coffee menu via RestTemplate + @LoadBalanced
     */
    private void readMenu() {
        ParameterizedTypeReference<List<Coffee>> ptr = 
            new ParameterizedTypeReference<List<Coffee>>() {};
        
        // Use service name instead of IP:Port
        ResponseEntity<List<Coffee>> list = restTemplate
            .exchange("http://waiter-service/coffee/", HttpMethod.GET, null, ptr);
        
        list.getBody().forEach(c -> log.info("Coffee: {}", c));
    }
    
    /**
     * Create order
     */
    private Long orderCoffee() {
        NewOrderRequest orderRequest = NewOrderRequest.builder()
            .customer("Ray Chu")
            .items(Arrays.asList("capuccino"))
            .build();
        
        RequestEntity<NewOrderRequest> request = RequestEntity
            .post(UriComponentsBuilder
                .fromUriString("http://waiter-service/order/")
                .build()
                .toUri())
            .body(orderRequest);
        
        ResponseEntity<CoffeeOrder> response = restTemplate.exchange(request, CoffeeOrder.class);
        log.info("Order Request Status Code: {}", response.getStatusCode());
        
        Long id = response.getBody().getId();
        log.info("Order ID: {}", id);
        return id;
    }
    
    /**
     * Query order by ID
     */
    private void queryOrder(Long id) {
        CoffeeOrder order = restTemplate
            .getForObject("http://waiter-service/order/{id}", CoffeeOrder.class, id);
        log.info("Order: {}", order);
    }
}
```

**Key Points:**

| Method | Purpose | Key Technology |
|--------|---------|----------------|
| `showServiceInstances()` | Service discovery demo | DiscoveryClient API |
| `readMenu()` | Read coffee menu | @LoadBalanced RestTemplate |
| `orderCoffee()` | Create order | HTTP POST with JSON |
| `queryOrder()` | Query order | HTTP GET with path variable |

## Service Name Resolution

### @LoadBalanced Magic

```java
// Developer writes:
restTemplate.exchange("http://waiter-service/coffee/", ...)
                      ↑
                      Service name (NOT IP:Port)

// Spring Cloud LoadBalancer automatically resolves to:
http://192.168.100.194:8082/coffee/
     ↑                     ↑
     Real IP               Real Port
```

**Resolution Flow:**

```
1. RestTemplate intercepts request: http://waiter-service/coffee/
   ↓
2. LoadBalancerClient detects service name "waiter-service"
   ↓
3. Query Consul via DiscoveryClient
   ↓
4. Consul returns service instances:
   - waiter-service-0: 192.168.100.194:8082
   ↓
5. LoadBalancer selects instance (RoundRobin strategy)
   ↓
6. Replace service name with actual IP:Port
   ↓
7. Execute HTTP request: http://192.168.100.194:8082/coffee/
```

**Benefits:**

| Traditional | Microservice (@LoadBalanced) |
|------------|------------------------------|
| `http://192.168.100.194:8082/coffee/` | `http://waiter-service/coffee/` |
| Hard-coded IP:Port | Service name |
| Manual configuration on change | No code change needed |
| Manual load balancing | Automatic load balancing |
| Manual failover | Automatic failover |

## Multi-Instance Testing

### ✅ Correct Approach: Multiple Customer Services

```bash
# Start 1 waiter-service (service provider)
cd consul-waiter-service
./mvnw spring-boot:run

# Start multiple customer-service instances (service consumers)

# Instance 1: Random port
cd consul-customer-service
./mvnw spring-boot:run

# Instance 2: Port 8091 (new terminal)
cd consul-customer-service
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8091"

# Instance 3: Port 8092 (new terminal)
cd consul-customer-service
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8092"
```

**Verify Multiple Instances:**

```bash
# Query all customer-service instances
curl -s http://localhost:8500/v1/catalog/service/customer-service | jq '.[] | {ID: .ServiceID, Port: .ServicePort}'

# Expected output (3 instances):
# {
#   "ID": "customer-service-0",
#   "Port": 65386
# }
# {
#   "ID": "customer-service-8091",
#   "Port": 8091
# }
# {
#   "ID": "customer-service-8092",
#   "Port": 8092
# }
```

**Architecture:**

```
3 customer-service instances:
┌──────────────────────┐
│ customer-service-0   │ (Port: 65386) ─┐
│ customer-service-1   │ (Port: 8091)   ├─ All discover same
│ customer-service-2   │ (Port: 8092)  ─┘  waiter-service
└──────────────────────┘
          ↓
    All call via @LoadBalanced RestTemplate
          ↓
┌──────────────────────┐
│  waiter-service-0    │ (Port: 8082)
│  H2 in-memory DB     │ ← Shared data
└──────────────────────┘
```

**Benefits:**
- ✅ All customer-service instances discover same waiter-service
- ✅ Data consistency guaranteed (single waiter-service instance)
- ✅ Can observe multiple customer-service in Consul UI
- ✅ Each customer-service can successfully create and query orders

## Service Discovery

### 1. HTTP API Discovery

**Query Service Instances:**

```bash
# List all services
curl http://localhost:8500/v1/catalog/services | jq

# Query customer-service instances
curl http://localhost:8500/v1/catalog/service/customer-service | jq

# Extract service ports
curl -s http://localhost:8500/v1/catalog/service/customer-service | jq '.[].ServicePort'

# Expected output (3 instances):
# 65386
# 8091
# 8092
```

### 2. DNS Discovery

**A Record (IP Address Only):**

```bash
# DNS A record query
dig @127.0.0.1 -p 8600 customer-service.service.consul A

# Expected output (all instances on same IP):
# customer-service.service.consul. 0 IN A 192.168.100.194
```

> ⚠️ **Note**: DNS **A record** only contains **IP address** (no port). If multiple instances are on the same IP, only 1 A record is returned.

**SRV Record (IP + Port):**

```bash
# DNS SRV record query (includes port information)
dig @127.0.0.1 -p 8600 customer-service.service.consul SRV

# Expected output (3 instances with ports):
# customer-service.service.consul. 0 IN SRV 1 1 65386 ...
# customer-service.service.consul. 0 IN SRV 1 1 8091 ...
# customer-service.service.consul. 0 IN SRV 1 1 8092 ...
```

**DNS Record Comparison:**

| Record Type | Contains | Use Case | Multi-Instance (Same IP) | Example |
|------------|----------|----------|--------------------------|---------|
| **A Record** | IP address only | Simple domain resolution | Returns 1 IP | `192.168.100.194` |
| **SRV Record** | IP + **Port** + Priority + Weight | Microservices | Returns all ports | `192.168.100.194:8091` |

## Load Balancing

### Spring Cloud LoadBalancer

**Default Strategy: RoundRobin**

```
Request 1 → http://waiter-service/coffee/ → 192.168.100.194:8082
Request 2 → http://waiter-service/coffee/ → 192.168.100.194:8082
Request 3 → http://waiter-service/coffee/ → 192.168.100.194:8082

(Only 1 waiter-service instance in this demo)
```

**With Multiple waiter-service Instances (Requires Shared Database):**

```
Request 1 → http://waiter-service/coffee/ → 192.168.100.194:8081
Request 2 → http://waiter-service/coffee/ → 192.168.100.194:8082
Request 3 → http://waiter-service/coffee/ → 192.168.100.194:8083
Request 4 → http://waiter-service/coffee/ → 192.168.100.194:8081  (Round-robin)
```

### Cache Warning

```
WARN ... LoadBalancerCaffeineWarnLogger : 
Spring Cloud LoadBalancer is currently working with the default cache. 
While this cache implementation is useful for development and tests, 
it's recommended to use Caffeine cache in production.
```

**Add Caffeine for Production:**

```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

## HTTP Client Configuration

### Connection Pool Management

```java
@Bean
public CloseableHttpClient httpClient() {
    return HttpClients.custom()
        .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
            .setMaxConnTotal(200)       // Max total connections
            .setMaxConnPerRoute(20)     // Max connections per route
            .setDefaultConnectionConfig(ConnectionConfig.custom()
                .setTimeToLive(TimeValue.ofSeconds(30))  // Connection TTL: 30s
                .build())
            .build())
        .evictIdleConnections(TimeValue.ofSeconds(30))  // Evict idle: 30s
        .disableAutomaticRetries()      // Disable retries (idempotency)
        .setKeepAliveStrategy(new CustomConnectionKeepAliveStrategy())
        .build();
}
```

**Configuration Explanation:**

| Parameter | Value | Purpose |
|-----------|-------|---------|
| `maxConnTotal` | 200 | Max total connections across all routes |
| `maxConnPerRoute` | 20 | Max connections per service |
| `timeToLive` | 30s | Connection lifetime |
| `evictIdleConnections` | 30s | Clean up idle connections |
| `disableAutomaticRetries` | - | Prevent duplicate requests for non-idempotent operations |

### RestTemplate Configuration

```java
@LoadBalanced
@Bean
public RestTemplate restTemplate(RestTemplateBuilder builder) {
    return builder
        .setConnectTimeout(Duration.ofSeconds(5))  // Connection timeout
        .setReadTimeout(Duration.ofSeconds(1))     // Read timeout
        .build();
}
```

**Timeout Configuration:**

| Timeout | Value | Purpose |
|---------|-------|---------|
| `connectTimeout` | 5s | Max time to establish connection |
| `readTimeout` | 1s | Max time to wait for response |

## Money Serialization

### MoneySerializer

```java
@JsonComponent
public class MoneySerializer extends StdSerializer<Money> {
    
    @Override
    public void serialize(Money money, JsonGenerator jsonGenerator, 
                         SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeNumber(money.getAmount());
    }
}
```

**Serialization:**
- `TWD 100.00` → `100.00` (decimal)
- Sent as JSON number (not string)

### MoneyDeserializer

```java
@JsonComponent
public class MoneyDeserializer extends StdDeserializer<Money> {
    
    @Override
    public Money deserialize(JsonParser p, DeserializationContext ctxt) 
            throws IOException {
        return Money.of(CurrencyUnit.of("TWD"), p.getDecimalValue());
    }
}
```

**Deserialization:**
- `100.00` (decimal) → `TWD 100.00`
- Auto-applies TWD currency

## Monitoring

### Consul Service Status

```bash
# Check service registration
curl -s http://localhost:8500/v1/catalog/service/customer-service | jq '.[] | {ID: .ServiceID, IP: .ServiceAddress, Port: .ServicePort}'

# Check service health
curl -s http://localhost:8500/v1/health/service/customer-service | jq '.[] | {ServiceID: .Service.ID, Status: .Checks[0].Status}'

# Expected output:
# {
#   "ServiceID": "customer-service-0",
#   "Status": "passing"
# }
```

### Application Logs

**Enable Detailed Logging:**

```properties
# Consul discovery logs
logging.level.org.springframework.cloud.consul=DEBUG

# LoadBalancer logs
logging.level.org.springframework.cloud.loadbalancer=DEBUG

# Application logs
logging.level.tw.fengqing.spring.springbucks.customer=DEBUG
```

**Watch Logs:**

```bash
# Monitor service discovery
tail -f logs/spring.log | grep "DiscoveryClient"

# Monitor HTTP requests
tail -f logs/spring.log | grep "RestTemplate"
```

## Common Issues

### Issue 1: waiter-service Not Found

**Error:**
```
org.springframework.web.client.ResourceAccessException: 
I/O error on GET request for "http://waiter-service/coffee/": 
waiter-service
```

**Solutions:**

```bash
# 1. Verify waiter-service is registered in Consul
curl http://localhost:8500/v1/catalog/service/waiter-service

# 2. Check Consul connection
curl http://localhost:8500/v1/status/leader

# 3. Restart customer-service after waiter-service is ready
./mvnw spring-boot:run
```

### Issue 2: Connection Timeout

**Error:**
```
java.net.SocketTimeoutException: Read timed out
```

**Solutions:**

```properties
# Increase read timeout (application.properties)
# Note: Adjust in RestTemplate bean configuration

# In CustomerServiceApplication.java:
requestFactory.setReadTimeout(Duration.ofSeconds(5));  # Increase from 1s to 5s
```

### Issue 3: Money Deserialization Error

**Error:**
```
com.fasterxml.jackson.databind.exc.InvalidDefinitionException: 
Cannot construct instance of `org.joda.money.Money`
```

**Solution:**

Ensure `MoneyDeserializer` is registered:

```java
@JsonComponent  // This annotation auto-registers deserializer
public class MoneyDeserializer extends StdDeserializer<Money> {
    // ...
}
```

### Issue 4: Multiple Instances Same Port

**Error:**
```
Web server failed to start. Port 8091 was already in use.
```

**Solutions:**

```bash
# Solution 1: Use random port (remove --server.port argument)
./mvnw spring-boot:run

# Solution 2: Use different ports for each instance
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8091"
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8092"
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8093"

# Solution 3: Kill process using the port
lsof -ti:8091 | xargs kill -9
```

## Consul Best Practices

### 1. Service Registration

```properties
# ✅ Recommended: Use IP address
spring.cloud.consul.discovery.prefer-ip-address=true

# ✅ Recommended: Set appropriate health check interval
spring.cloud.consul.discovery.health-check-interval=10s

# ✅ Recommended: Enable heartbeat
spring.cloud.consul.discovery.heartbeat.enabled=true

# ⚠️ Optional: Set service instance ID
spring.cloud.consul.discovery.instance-id=${spring.application.name}:${server.port}
```

### 2. HTTP Client Optimization

**Development:**

```java
requestFactory.setConnectTimeout(Duration.ofSeconds(5));
requestFactory.setReadTimeout(Duration.ofSeconds(1));
```

**Production:**

```java
requestFactory.setConnectTimeout(Duration.ofSeconds(3));
requestFactory.setReadTimeout(Duration.ofSeconds(10));

// Add retry mechanism
requestFactory.setBufferRequestBody(true);
```

### 3. Load Balancer Optimization

**Add Caffeine Cache (Production):**

```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

**Configure Cache:**

```java
@Bean
public CacheManager cacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager();
    cacheManager.setCaffeine(Caffeine.newBuilder()
        .expireAfterWrite(10, TimeUnit.SECONDS)
        .maximumSize(100));
    return cacheManager;
}
```

### 4. Failover Strategy

**Circuit Breaker (Resilience4j):**

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>
```

```java
@CircuitBreaker(name = "waiter-service", fallbackMethod = "fallbackMenu")
public List<Coffee> readMenu() {
    ResponseEntity<List<Coffee>> response = restTemplate
        .exchange("http://waiter-service/coffee/", ...);
    return response.getBody();
}

public List<Coffee> fallbackMenu(Exception e) {
    log.warn("Fallback triggered: {}", e.getMessage());
    return Collections.emptyList();  // Return empty menu
}
```

## Architecture Comparison

### Consul vs Eureka vs ZooKeeper

| Feature | Consul | Eureka | ZooKeeper |
|---------|--------|--------|-----------|
| **Service Discovery** | HTTP API + DNS | HTTP API | ZNode + Watcher |
| **Multi-Datacenter** | ✅ Native support | ⚠️ Requires config | ⚠️ Limited |
| **Health Check** | ✅ HTTP, TCP, Script, TTL, gRPC | ⚠️ Heartbeat only | ⚠️ Session timeout |
| **Config Center** | ✅ Built-in KV store | ❌ No | ✅ ZNode storage |
| **DNS Integration** | ✅ Built-in DNS server | ❌ No | ❌ No |
| **CAP Theorem** | CP (Consistency) | AP (Availability) | CP (Consistency) |
| **Maintenance** | ✅ Active | ⚠️ 2.0 EOL | ✅ Active |
| **Client Library** | Multiple languages | Java only | Multiple languages |
| **Learning Curve** | Medium | Easy | Hard |

**Selection Guide:**
- **Consul**: Multi-datacenter, DNS integration, config center
- **Eureka**: Simple setup, AP mode (no longer maintained)
- **ZooKeeper**: Strong consistency, requires more configuration

## Best Practices Demonstrated

1. **Service Discovery**: Auto-discover waiter-service via Consul
2. **Load Balancing**: Client-side load balancing with @LoadBalanced
3. **Connection Pool**: Apache HttpClient 5 connection pool management
4. **Keep-Alive Strategy**: Custom connection keep-alive for performance
5. **Timeout Configuration**: Appropriate connect and read timeouts
6. **Money Serialization**: Type-safe Money handling with JSON
7. **Random Port**: Support multi-instance with `server.port=0`
8. **Health Check**: TTL heartbeat for efficient monitoring

## Testing

### Unit Testing

```bash
# Run all tests
./mvnw test

# Run with coverage
./mvnw clean test jacoco:report
```

### Integration Testing

```bash
# 1. Start Consul
docker run -d --name consul -p 8500:8500 -p 8600:8600/udp consul:1.4.5

# 2. Start waiter-service
cd ../consul-waiter-service && ./mvnw spring-boot:run &

# 3. Wait for waiter-service to register (2-3 seconds)
sleep 3

# 4. Start customer-service
cd consul-customer-service && ./mvnw spring-boot:run

# 5. Observe auto-execution in logs
# Expected: Service discovery, menu reading, order creation, order query
```

## References

- [Consul Documentation](https://www.consul.io/docs)
- [Spring Cloud Consul Reference](https://docs.spring.io/spring-cloud-consul/docs/current/reference/html/)
- [Spring Cloud LoadBalancer](https://docs.spring.io/spring-cloud-commons/docs/current/reference/html/#spring-cloud-loadbalancer)
- [Apache HttpClient 5](https://hc.apache.org/httpcomponents-client-5.2.x/)
- [Joda Money Documentation](https://www.joda.org/joda-money/)

## License

MIT License - see [LICENSE](LICENSE) file for details.

## About Us

我們主要專注在敏捷專案管理、物聯網（IoT）應用開發和領域驅動設計（DDD）。喜歡把先進技術和實務經驗結合，打造好用又靈活的軟體解決方案。近來也積極結合 AI 技術，推動自動化工作流，讓開發與運維更有效率、更智慧。持續學習與分享，希望能一起推動軟體開發的創新和進步。

## Contact

**風清雲談** - 專注於敏捷專案管理、物聯網（IoT）應用開發和領域驅動設計（DDD）。

- 🌐 官方網站：[風清雲談部落格](https://blog.fengqing.tw/)
- 📘 Facebook：[風清雲談粉絲頁](https://www.facebook.com/profile.php?id=61576838896062)
- 💼 LinkedIn：[Chu Kuo-Lung](https://www.linkedin.com/in/chu-kuo-lung)
- 📺 YouTube：[雲談風清頻道](https://www.youtube.com/channel/UCXDqLTdCMiCJ1j8xGRfwEig)
- 📧 Email：[fengqing.tw@gmail.com](mailto:fengqing.tw@gmail.com)

---

**⭐ If this project helps you, please give it a Star!**
