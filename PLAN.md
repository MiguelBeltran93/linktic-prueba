# Plan de Continuación — Linktic Prueba Técnica

**Repo:** `git@github.com:MiguelBeltran93/linktic-prueba.git`  
**Estado:** `catalog-service` ✅ completo en `develop` — siguiente: `stock-service`

---

## Setup en el nuevo PC

```bash
# 1. Clonar
git clone git@github.com:MiguelBeltran93/linktic-prueba.git
cd linktic-prueba

# 2. Crear rama stock-service
git checkout develop
git checkout -b feature/stock-service

# 3. Verificar Java 21
java -version   # debe ser 21.x
# Si no: brew install openjdk@21
# export JAVA_HOME="/opt/homebrew/opt/openjdk@21"

# 4. Docker para tests de integración (ver sección al final)
```

---

## Lo que está hecho ✅

### catalog-service (puerto 8080)
- `CatalogItem` entity + `CatalogRepository`
- `CatalogService`: register, findById, listAll
- `CatalogController`: `POST/GET /api/v1/catalog/items`
- `ApiAuthFilter`: valida header `X-API-KEY: secret123`
- Respuestas JSON API spec (`{data: {id, type, attributes}}`)
- Swagger UI en `/swagger-ui.html`
- Actuator en `/actuator/health`
- Logs JSON estructurados (Logstash encoder)
- Dockerfile multi-stage
- 21 tests — cobertura 100%

---

## FASE 2 — stock-service ❌ (siguiente)

### Estructura de carpetas a crear

```
stock-service/
├── build.gradle
├── settings.gradle
├── Dockerfile
└── src/
    ├── main/
    │   ├── java/com/linktic/stock/
    │   │   ├── StockServiceApplication.java
    │   │   ├── config/
    │   │   │   ├── StockAuthFilter.java
    │   │   │   ├── BrokerConfiguration.java
    │   │   │   ├── WebSecurityConfiguration.java
    │   │   │   └── SwaggerConfiguration.java
    │   │   ├── controller/StockController.java
    │   │   ├── dto/
    │   │   │   ├── request/PurchaseRequest.java
    │   │   │   └── response/{StockRepresentation,PurchaseRepresentation}.java
    │   │   ├── exception/
    │   │   │   ├── StockExceptionHandler.java
    │   │   │   ├── ProductNotFoundException.java
    │   │   │   └── InsufficientStockException.java
    │   │   ├── gateway/ProductGateway.java
    │   │   ├── mapper/StockMapper.java
    │   │   ├── messaging/StockEventPublisher.java
    │   │   ├── model/{StockEntry,PurchaseRecord}.java
    │   │   ├── repository/{StockRepository,PurchaseRepository}.java
    │   │   └── service/StockService.java
    │   └── resources/
    │       ├── application.yml
    │       ├── application-docker.yml
    │       └── logback-spring.xml
    └── test/
        └── java/com/linktic/stock/
            ├── controller/StockControllerTest.java
            ├── exception/StockExceptionHandlerTest.java
            ├── integration/StockIntegrationTest.java
            └── service/StockServiceTest.java
```

---

### build.gradle — stock-service

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.3.4'
    id 'io.spring.dependency-management' version '1.1.6'
    id 'jacoco'
}

group = 'com.linktic'
version = '1.0.0'
java { sourceCompatibility = JavaVersion.VERSION_21 }

configurations {
    compileOnly { extendsFrom annotationProcessor }
}

repositories { mavenCentral() }

ext { resilience4jVersion = '2.2.0' }

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-amqp'
    implementation 'org.springframework.boot:spring-boot-starter-aop'
    implementation "io.github.resilience4j:resilience4j-spring-boot3:${resilience4jVersion}"
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0'
    implementation 'net.logstash.logback:logstash-logback-encoder:7.4'
    runtimeOnly 'org.postgresql:postgresql'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.testcontainers:postgresql:1.21.3'
    testImplementation 'org.testcontainers:junit-jupiter:1.21.3'
    testImplementation 'org.testcontainers:rabbitmq:1.21.3'
    testImplementation 'com.github.tomakehurst:wiremock-standalone:3.0.1'
}

jacoco { toolVersion = '0.8.12' }

jacocoTestReport {
    dependsOn test
    reports { xml.required = true; html.required = true }
    afterEvaluate {
        classDirectories.setFrom(files(classDirectories.files.collect {
            fileTree(dir: it, exclude: [
                '**/dto/**', '**/model/**', '**/config/**',
                '**/exception/ErrorResponse*', '**/exception/*Exception*',
                '**/messaging/StockUpdatedEvent*', '**/*Application*'
            ])
        }))
    }
}

jacocoTestCoverageVerification {
    dependsOn jacocoTestReport
    violationRules { rule { limit { minimum = 0.80 } } }
}

test {
    useJUnitPlatform()
    finalizedBy jacocoTestReport
    environment 'TESTCONTAINERS_RYUK_DISABLED', System.getenv('TESTCONTAINERS_RYUK_DISABLED') ?: 'true'
    def customDockerHost = System.getenv('DOCKER_HOST')
    if (customDockerHost) {
        environment 'DOCKER_HOST', customDockerHost
        environment 'TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE', customDockerHost.replaceAll('^unix://', '')
    }
}
```

---

### settings.gradle — stock-service

```groovy
rootProject.name = 'stock-service'
```

---

### Entidades

```java
// StockEntry.java
@Entity @Table(name = "stock_entries")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class StockEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @UpdateTimestamp
    @Column(updatable = true)
    private LocalDateTime updatedAt;
}
```

```java
// PurchaseRecord.java
@Entity @Table(name = "purchase_records")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PurchaseRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer units;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime purchasedAt;
}
```

---

### StockService.java — lógica principal

```java
@Service @RequiredArgsConstructor @Slf4j
public class StockService {

    private final StockRepository stockRepository;
    private final PurchaseRepository purchaseRepository;
    private final ProductGateway productGateway;
    private final StockEventPublisher eventPublisher;

    // Lazy creation: crea entry con qty=0 si no existe
    public ApiResponse<ApiData<StockRepresentation>> getStock(Long productId) {
        productGateway.validateProduct(productId); // 404 si no existe en catalog
        StockEntry entry = stockRepository.findByProductId(productId)
                .orElseGet(() -> stockRepository.save(
                        StockEntry.builder().productId(productId).quantity(0).build()
                ));
        return StockMapper.toSingleResponse(entry);
    }

    // Descuenta stock + guarda PurchaseRecord + publica evento
    @Transactional
    public ApiResponse<ApiData<PurchaseRepresentation>> processPurchase(PurchaseRequest request) {
        StockEntry entry = stockRepository.findByProductId(request.getProductId())
                .orElseGet(() -> stockRepository.save(
                        StockEntry.builder().productId(request.getProductId()).quantity(0).build()
                ));

        if (entry.getQuantity() < request.getUnits()) {
            throw new InsufficientStockException(request.getProductId(), entry.getQuantity());
        }

        entry.setQuantity(entry.getQuantity() - request.getUnits());
        stockRepository.save(entry);

        PurchaseRecord record = purchaseRepository.save(
                PurchaseRecord.builder()
                        .productId(request.getProductId())
                        .units(request.getUnits())
                        .build()
        );

        eventPublisher.publishStockUpdated(entry.getProductId(), entry.getQuantity(), "PURCHASE");
        return StockMapper.toPurchaseResponse(record, entry.getQuantity());
    }

    // Ajuste manual de inventario
    @Transactional
    public ApiResponse<ApiData<StockRepresentation>> adjustStock(Long productId, Integer newQuantity) {
        StockEntry entry = stockRepository.findByProductId(productId)
                .orElseGet(() -> stockRepository.save(
                        StockEntry.builder().productId(productId).quantity(0).build()
                ));
        entry.setQuantity(newQuantity);
        stockRepository.save(entry);
        eventPublisher.publishStockUpdated(productId, newQuantity, "ADJUSTMENT");
        return StockMapper.toSingleResponse(entry);
    }
}
```

---

### ProductGateway.java — Resilience4j

```java
@Component @RequiredArgsConstructor @Slf4j
public class ProductGateway {

    private final RestTemplate restTemplate;

    @Value("${services.catalog.url}")
    private String catalogUrl;

    @Value("${services.catalog.api-key}")
    private String catalogApiKey;

    @CircuitBreaker(name = "catalogGateway", fallbackMethod = "fallbackValidate")
    @Retry(name = "catalogGateway")
    public void validateProduct(Long productId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-KEY", catalogApiKey);
        try {
            restTemplate.exchange(
                    catalogUrl + "/api/v1/catalog/items/" + productId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Object.class
            );
        } catch (HttpClientErrorException.NotFound e) {
            throw new ProductNotFoundException(productId);
        }
    }

    public void fallbackValidate(Long productId, Exception ex) {
        log.warn("Circuit breaker open for productId={}: {}", productId, ex.getMessage());
        // En fallback: asumir que el producto existe (degraded mode)
    }
}
```

---

### BrokerConfiguration.java — RabbitMQ

```java
@Configuration
public class BrokerConfiguration {

    public static final String STOCK_EXCHANGE   = "stock.events";
    public static final String STOCK_QUEUE      = "stock.updates.queue";
    public static final String STOCK_ROUTING_KEY = "stock.updated";

    @Bean
    public TopicExchange stockExchange() {
        return new TopicExchange(STOCK_EXCHANGE, true, false);
    }

    @Bean
    public Queue stockQueue() {
        return QueueBuilder.durable(STOCK_QUEUE).build();
    }

    @Bean
    public Binding stockBinding(Queue stockQueue, TopicExchange stockExchange) {
        return BindingBuilder.bind(stockQueue).to(stockExchange).with(STOCK_ROUTING_KEY);
    }
}
```

---

### StockEventPublisher.java

```java
@Component @RequiredArgsConstructor @Slf4j
public class StockEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishStockUpdated(Long productId, Integer newQuantity, String operation) {
        StockUpdatedEvent event = StockUpdatedEvent.builder()
                .productId(productId)
                .newQuantity(newQuantity)
                .operation(operation)
                .occurredAt(LocalDateTime.now())
                .build();
        rabbitTemplate.convertAndSend(
                BrokerConfiguration.STOCK_EXCHANGE,
                BrokerConfiguration.STOCK_ROUTING_KEY,
                event
        );
        log.info("Published stock.updated: productId={}, qty={}, op={}", productId, newQuantity, operation);
    }
}
```

---

### StockController.java

```java
@RestController
@RequestMapping("/api/v1/stock")
@RequiredArgsConstructor
@Tag(name = "Stock", description = "Inventory management")
@SecurityRequirement(name = "X-API-KEY")
public class StockController {

    private final StockService stockService;

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<?>> getStock(@PathVariable Long productId) {
        return ResponseEntity.ok(stockService.getStock(productId));
    }

    @PostMapping("/purchase")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<?> purchase(@Valid @RequestBody PurchaseRequest request) {
        return stockService.processPurchase(request);
    }

    @PutMapping("/{productId}/adjust")
    public ApiResponse<?> adjust(
            @PathVariable Long productId,
            @RequestParam @Min(0) Integer quantity) {
        return stockService.adjustStock(productId, quantity);
    }
}
```

---

### application.yml — stock-service

```yaml
server:
  port: 8081

spring:
  application:
    name: stock-service
  datasource:
    url: jdbc:postgresql://localhost:5432/linktic_db
    username: postgres
    password: admin
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate.dialect: org.hibernate.dialect.PostgreSQLDialect
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

security:
  api-key: secret123

services:
  catalog:
    url: http://localhost:8080
    api-key: secret123

resilience4j:
  circuitbreaker:
    instances:
      catalogGateway:
        slidingWindowSize: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
        permittedNumberOfCallsInHalfOpenState: 3
  retry:
    instances:
      catalogGateway:
        maxAttempts: 3
        waitDuration: 500ms

management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: always

springdoc:
  swagger-ui:
    path: /swagger-ui.html
  api-docs:
    path: /v3/api-docs
```

---

### application-docker.yml — stock-service

```yaml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/linktic_db
  rabbitmq:
    host: rabbitmq

services:
  catalog:
    url: http://catalog-service:8080
```

---

### Dockerfile — stock-service

```dockerfile
FROM gradle:8.8-jdk21 AS build
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY src ./src
RUN gradle build -x test --no-daemon

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

### Endpoints para tests rápidos

```bash
API_KEY="secret123"

# stock-service
curl -s -X GET "http://localhost:8081/api/v1/stock/1" -H "X-API-KEY: $API_KEY" | jq
curl -s -X POST "http://localhost:8081/api/v1/stock/purchase" \
  -H "X-API-KEY: $API_KEY" -H "Content-Type: application/json" \
  -d '{"productId":1,"units":2}' | jq
curl -s -X PUT "http://localhost:8081/api/v1/stock/1/adjust?quantity=50" \
  -H "X-API-KEY: $API_KEY" | jq
```

---

## FASE 3 — Frontend Quasar ❌

```bash
cd frontend
npm create quasar@latest .
# Opciones: Vue 3, Composition API, SCSS, ESLint, sin TypeScript
```

**Tema** (`src/css/quasar.variables.scss`):
```scss
$primary   : #3F51B5;  // indigo
$secondary : #00BCD4;  // cyan
$dark      : #1A1A2E;
$dark-page : #0F0F23;
```

**Páginas**: `CatalogPage.vue` · `PurchasePage.vue` (QStepper 3 pasos) · `StockPage.vue`

---

## FASE 4 — docker-compose.yml ❌

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:15
    environment: {POSTGRES_DB: linktic_db, POSTGRES_USER: postgres, POSTGRES_PASSWORD: admin}
    healthcheck: {test: ["CMD-SHELL", "pg_isready -U postgres"], interval: 5s, retries: 5}

  rabbitmq:
    image: rabbitmq:3.12-management
    ports: ["5672:5672", "15672:15672"]
    healthcheck: {test: ["CMD", "rabbitmq-diagnostics", "ping"], interval: 10s, retries: 5}

  catalog-service:
    build: ./catalog-service
    ports: ["8080:8080"]
    environment: {SPRING_PROFILES_ACTIVE: docker}
    depends_on: {postgres: {condition: service_healthy}}
    healthcheck: {test: ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"], interval: 10s, retries: 5}

  stock-service:
    build: ./stock-service
    ports: ["8081:8081"]
    environment: {SPRING_PROFILES_ACTIVE: docker}
    depends_on:
      postgres: {condition: service_healthy}
      rabbitmq: {condition: service_healthy}
      catalog-service: {condition: service_healthy}
    healthcheck: {test: ["CMD-SHELL", "curl -f http://localhost:8081/actuator/health || exit 1"], interval: 10s, retries: 5}

  frontend:
    build: ./frontend
    ports: ["9000:80"]
    depends_on: [catalog-service, stock-service]
```

---

## Workaround Docker para Integration Tests (macOS + Docker Desktop 29.x)

> Testcontainers usa docker-java 3.3.6 con API v1.32 pero Docker Desktop 29.x requiere v1.40+.

**Opción A — Colima + proxy Python** (recomendado):

```bash
# 1. Instalar y arrancar Colima
brew install colima && colima start

# 2. Guardar esto como /tmp/docker_proxy.py
cat > /tmp/docker_proxy.py << 'EOF'
import socket, threading, os, re

PROXY_SOCKET = '/tmp/docker-proxy.sock'
REAL_SOCKET  = os.path.expanduser('~/.colima/default/docker.sock')
MIN_VERSION  = '1.44'

def upgrade_version(data):
    try:
        text = data.decode('utf-8', errors='replace')
        def repl(m):
            if int(m.group(1)) < 1 or (int(m.group(1)) == 1 and int(m.group(2)) < 44):
                return f'/v{MIN_VERSION}/'
            return m.group(0)
        return re.sub(r'/v(\d+)\.(\d+)/', repl, text).encode('utf-8')
    except: return data

def forward(src, dst, up=False):
    try:
        while True:
            d = src.recv(65536)
            if not d: break
            dst.sendall(upgrade_version(d) if up else d)
    except: pass
    finally:
        for s in [src, dst]:
            try: s.close()
            except: pass

def handle(c):
    r = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
    r.connect(REAL_SOCKET)
    for args in [(c, r, True), (r, c, False)]:
        threading.Thread(target=forward, args=args, daemon=True).start()

if os.path.exists(PROXY_SOCKET): os.unlink(PROXY_SOCKET)
s = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
s.bind(PROXY_SOCKET)
os.chmod(PROXY_SOCKET, 0o777)
s.listen(100)
print(f'Proxy OK: {PROXY_SOCKET}')
while True:
    c, _ = s.accept()
    threading.Thread(target=handle, args=(c,), daemon=True).start()
EOF

# 3. Arrancar proxy en background
python3 /tmp/docker_proxy.py &

# 4. Configurar ~/.testcontainers.properties
echo "docker.client.strategy=org.testcontainers.dockerclient.EnvironmentAndSystemPropertyClientProviderStrategy" > ~/.testcontainers.properties

# 5. Correr tests
export DOCKER_HOST="unix:///tmp/docker-proxy.sock"
./gradlew cleanTest test
```

**Opción B — Saltar integration tests** (si no quieres el proxy):
```bash
./gradlew test --tests "com.linktic.stock.service.*" --tests "com.linktic.stock.controller.*" --tests "com.linktic.stock.exception.*"
```

---

## Checklist

| | Tarea |
|---|---|
| ✅ | catalog-service completo + mergeado en develop |
| ⬜ | `git checkout -b feature/stock-service` |
| ⬜ | stock-service: entidades + repos |
| ⬜ | stock-service: service + gateway + publisher |
| ⬜ | stock-service: controller + config + exceptions |
| ⬜ | stock-service: tests ≥80% cobertura |
| ⬜ | stock-service: merge a develop |
| ⬜ | frontend: Quasar + tema + 3 páginas |
| ⬜ | docker-compose.yml con healthchecks |
| ⬜ | README.md con diagrama Mermaid |
| ⬜ | Tag v1.0.0 + merge develop→main |
