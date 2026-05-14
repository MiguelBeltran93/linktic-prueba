# Linktic Prueba Técnica — SPEC de Continuación

> **Para retomar en otro PC.** Todo lo necesario para saber qué está hecho, qué falta y cómo ejecutarlo.

---

## 1. Repositorio

```
GitHub SSH : git@github.com:MiguelBeltran93/linktic-prueba.git
Rama activa: feature/catalog-service
Develop    : develop
```

```bash
git clone git@github.com:MiguelBeltran93/linktic-prueba.git
cd linktic-prueba
git checkout feature/catalog-service
```

---

## 2. Stack Tecnológico

| Capa | Tecnología |
|---|---|
| Build | **Gradle Groovy 8.8** (diferenciador vs Maven del proyecto referencia) |
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.3.4 |
| Base de datos | PostgreSQL 15 |
| Seguridad | Filtro custom `X-API-KEY` (NO Spring Security complejo) |
| Docs | Springdoc OpenAPI 2.5.0 (Swagger UI) |
| Logs | Logstash Logback Encoder 7.4 (JSON estructurado) |
| Tests unitarios | JUnit 5 + Mockito |
| Tests integración | Testcontainers 1.21.3 + PostgreSQL |
| Cobertura | JaCoCo ≥ 80% |
| Contenedores | Docker multi-stage (gradle:8.8-jdk21 → eclipse-temurin:21-jre-jammy) |
| Mensajería | RabbitMQ (SOLO stock-service como publisher) |
| Circuit Breaker | Resilience4j (SOLO stock-service) |
| Frontend | Quasar 2.x + Vue 3 + dark theme (índigo/cyan) |

---

## 3. Estado Actual

### ✅ FASE 0 — Setup inicial (COMPLETO, en develop)
- Monorepo: `catalog-service/`, `stock-service/`, `frontend/`
- `.gitignore`, `.editorconfig`, git config con user Miguel Beltran
- Remote SSH configurado

### 🔄 FASE 1 — catalog-service (EN PROGRESO, en feature/catalog-service)

#### Commits ya hechos:
```
d258132 feat: add CatalogService and CatalogController with JSON API responses
0188a0b feat: add DTOs, mapper, and exception handling
a49bc1d feat: add CatalogItem entity and CatalogRepository
```

#### Archivos por commitear todavía (sin stagear):
```
catalog-service/Dockerfile
catalog-service/build.gradle
catalog-service/settings.gradle
catalog-service/gradlew + gradle/
catalog-service/src/main/java/com/linktic/catalog/config/
  ├── ApiAuthFilter.java
  ├── JpaConfiguration.java
  ├── SwaggerConfiguration.java
  └── WebSecurityConfiguration.java
catalog-service/src/main/resources/
  ├── application.yml
  ├── application-docker.yml
  └── logback-spring.xml
catalog-service/src/test/java/com/linktic/catalog/
  ├── controller/CatalogControllerTest.java  (7 tests)
  ├── exception/ApiExceptionHandlerTest.java (3 tests)
  ├── integration/CatalogIntegrationTest.java (5 tests)
  └── service/CatalogServiceTest.java        (6 tests)
```

#### Commits pendientes (hacer en orden):
```bash
# 1 — Config classes
git add catalog-service/src/main/java/com/linktic/catalog/config/
git commit -m "feat: add security, Swagger and JPA configuration"

# 2 — Resources
git add catalog-service/src/main/resources/
git commit -m "feat: add application config and structured JSON logging"

# 3 — Build files
git add catalog-service/build.gradle catalog-service/settings.gradle \
        catalog-service/gradlew catalog-service/gradlew.bat catalog-service/gradle/
git commit -m "build: add Gradle Groovy build with JaCoCo 80% coverage verification"

# 4 — Dockerfile
git add catalog-service/Dockerfile
git commit -m "feat: add multi-stage Dockerfile for catalog-service"

# 5 — Tests
git add catalog-service/src/test/
git commit -m "test: add unit and integration tests for catalog-service (100% coverage)"

# 6 — Merge a develop
git checkout develop
git merge --no-ff feature/catalog-service -m "feat: merge feature/catalog-service into develop"
git push origin develop
```

#### Tests — cómo correr:
```bash
cd catalog-service

# PREREQUISITO LOCAL: necesitas el proxy Docker (ver sección 8)
# Con el proxy corriendo:
export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
export PATH="$JAVA_HOME/bin:$PATH"
export DOCKER_HOST="unix:///tmp/docker-proxy.sock"

./gradlew cleanTest test                    # todos los tests
./gradlew jacocoTestCoverageVerification   # verifica ≥80% cobertura
```

---

## 4. catalog-service — Arquitectura Completa

### Puertos y URLs
- Puerto: **8080**
- Base URL: `/api/v1/catalog/items`
- Swagger: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`

### Endpoints
| Método | Path | Descripción |
|---|---|---|
| POST | `/api/v1/catalog/items` | Registrar ítem (requiere X-API-KEY) |
| GET | `/api/v1/catalog/items/{id}` | Obtener ítem por ID |
| GET | `/api/v1/catalog/items` | Listar todos los ítems |

### Estructura de paquetes
```
com.linktic.catalog
├── CatalogServiceApplication.java
├── config/
│   ├── ApiAuthFilter.java          # Filter que valida X-API-KEY header
│   ├── JpaConfiguration.java       # @EnableJpaAuditing separado (fix WebMvcTest)
│   ├── SwaggerConfiguration.java   # OpenAPI 3 + SecurityScheme X-API-KEY
│   └── WebSecurityConfiguration.java # CSRF off, CORS open, permitAll
├── controller/
│   └── CatalogController.java      # @RestController /api/v1/catalog/items
├── dto/
│   ├── request/CreateItemRequest.java
│   └── response/{ApiData, ApiResponse, ItemRepresentation}.java
├── exception/
│   ├── ApiExceptionHandler.java    # @RestControllerAdvice: 404/422/500
│   ├── ErrorResponse.java          # {errors: [{status, title, detail}]}
│   └── ItemNotFoundException.java
├── mapper/
│   └── CatalogMapper.java          # Static utility: toEntity, toRepresentation, etc.
├── model/
│   └── CatalogItem.java            # @Entity tabla catalog_items
├── repository/
│   └── CatalogRepository.java      # JpaRepository<CatalogItem, Long>
└── service/
    └── CatalogService.java         # register, findById, listAll
```

### Formato de respuesta (JSON API spec)
```json
// Éxito single:
{"data": {"id": "1", "type": "catalog-items", "attributes": {"name": "...", "price": 99.99}}}

// Éxito lista:
{"data": [{"id": "1", "type": "catalog-items", "attributes": {...}}]}

// Error:
{"errors": [{"status": "404", "title": "Item Not Found", "detail": "No catalog item found with id: 1"}]}
```

### Variables de entorno / configuración
```yaml
# application.yml
security.api-key: secret123
spring.datasource.url: jdbc:postgresql://localhost:5432/linktic_db
spring.datasource.username: postgres
spring.datasource.password: admin

# application-docker.yml (override para Docker)
spring.datasource.url: jdbc:postgresql://postgres:5432/linktic_db
```

---

## 5. FASE 2 — stock-service (POR HACER)

### Puerto: 8081 | Base URL: `/api/v1/stock`

### Entidades
```java
// StockEntry.java — inventario por producto
@Entity @Table(name="stock_entries")
Long id, Long productId, Integer quantity, LocalDateTime updatedAt

// PurchaseRecord.java — historial de compras
@Entity @Table(name="purchase_records")
Long id, Long productId, Integer units, LocalDateTime purchasedAt
```

### Estructura de paquetes
```
com.linktic.stock
├── StockServiceApplication.java
├── config/
│   ├── StockAuthFilter.java        # igual que ApiAuthFilter
│   ├── BrokerConfiguration.java    # RabbitMQ: exchange + routingKey
│   ├── Resilience4jConfiguration.java
│   ├── WebSecurityConfiguration.java
│   └── SwaggerConfiguration.java
├── controller/
│   └── StockController.java        # /api/v1/stock
├── dto/
│   ├── request/PurchaseRequest.java   # {productId, units}
│   └── response/{StockRepresentation, PurchaseRepresentation}.java
├── exception/
│   ├── StockExceptionHandler.java
│   ├── ProductNotFoundException.java
│   └── InsufficientStockException.java
├── gateway/
│   └── ProductGateway.java         # HTTP client a catalog-service con Resilience4j
├── mapper/
│   └── StockMapper.java
├── messaging/
│   └── StockEventPublisher.java    # Publica stock.updated a RabbitMQ
├── model/
│   ├── StockEntry.java
│   └── PurchaseRecord.java
├── repository/
│   ├── StockRepository.java
│   └── PurchaseRepository.java
└── service/
    └── StockService.java           # getOrCreate (lazy), processPurchase, adjustStock
```

### Endpoints stock-service
| Método | Path | Descripción |
|---|---|---|
| GET | `/api/v1/stock/{productId}` | Consultar stock (crea en 0 si no existe) |
| POST | `/api/v1/stock/purchase` | Procesar compra (descuenta stock) |
| PUT | `/api/v1/stock/{productId}/adjust` | Ajustar inventario manualmente |

### Resilience4j (en ProductGateway)
```java
@CircuitBreaker(name = "catalogGateway", fallbackMethod = "fallbackProduct")
@Retry(name = "catalogGateway")
public ProductInfo fetchProduct(Long productId) { ... }
```

```yaml
# application.yml stock-service
resilience4j:
  circuitbreaker:
    instances:
      catalogGateway:
        slidingWindowSize: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
  retry:
    instances:
      catalogGateway:
        maxAttempts: 3
        waitDuration: 500ms
```

### RabbitMQ — stock-service SOLO es publisher
```java
// BrokerConfiguration.java
Exchange: "stock.events" (TopicExchange, durable)
RoutingKey: "stock.updated"
Queue: "stock.updates.queue" (durable, creación lazy)

// StockEventPublisher.java
rabbitTemplate.convertAndSend("stock.events", "stock.updated", event);

// StockUpdatedEvent {productId, newQuantity, operation (PURCHASE/ADJUSTMENT), timestamp}
```

### build.gradle stock-service (dependencias extra vs catalog)
```groovy
implementation 'org.springframework.boot:spring-boot-starter-amqp'
implementation 'io.github.resilience4j:resilience4j-spring-boot3:2.2.0'
implementation 'io.github.resilience4j:resilience4j-reactor:2.2.0'
implementation 'org.springframework.boot:spring-boot-starter-aop'  // requerido por resilience4j
implementation 'org.springframework.cloud:spring-cloud-starter-openfeign'  // O usar RestTemplate/WebClient
```

### Tests stock-service
- `StockServiceTest.java` — lazy creation, processPurchase, adjustStock, insufficient stock
- `StockControllerTest.java` — @WebMvcTest excluyendo StockAuthFilter
- `StockExceptionHandlerTest.java` — unit test handler
- `StockIntegrationTest.java` — @Testcontainers + mock catalog-service (WireMock)

---

## 6. FASE 3 — Frontend Quasar (POR HACER)

```bash
cd frontend
npm create quasar@latest . -- --preset app
# Framework: Vue 3, TypeScript: No, Quasar version: latest, CSS: SCSS, features: ESLint
```

### Tema oscuro custom (src/css/quasar.variables.scss)
```scss
$primary: #3F51B5;    // indigo
$secondary: #00BCD4;  // cyan
$accent: #00ACC1;
$dark: #1A1A2E;
$dark-page: #0F0F23;
```

### Páginas requeridas
| Ruta | Componente | Descripción |
|---|---|---|
| `/` | `CatalogPage.vue` | Tabla de productos del catálogo |
| `/purchase` | `PurchasePage.vue` | QStepper: selección → confirmación → resultado |
| `/stock/:id` | `StockPage.vue` | Consulta de stock por producto |

### QStepper en PurchasePage
```
Paso 1: Seleccionar producto (QSelect con items del catálogo)
Paso 2: Cantidad + confirmar (QInput + resumen)
Paso 3: Resultado (éxito con nuevo stock, o error)
```

---

## 7. FASE 4 — docker-compose.yml (POR HACER)

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:15
    environment: {POSTGRES_DB: linktic_db, POSTGRES_USER: postgres, POSTGRES_PASSWORD: admin}
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]

  rabbitmq:
    image: rabbitmq:3.12-management
    ports: ["5672:5672", "15672:15672"]
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "ping"]

  catalog-service:
    build: ./catalog-service
    ports: ["8080:8080"]
    environment: {SPRING_PROFILES_ACTIVE: docker}
    depends_on:
      postgres: {condition: service_healthy}
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]

  stock-service:
    build: ./stock-service
    ports: ["8081:8081"]
    environment: {SPRING_PROFILES_ACTIVE: docker}
    depends_on:
      postgres: {condition: service_healthy}
      rabbitmq: {condition: service_healthy}
      catalog-service: {condition: service_healthy}

  frontend:
    build: ./frontend
    ports: ["9000:80"]
    depends_on:
      - catalog-service
      - stock-service
```

---

## 8. WORKAROUND Docker en macOS con Docker Desktop 29.x

> **Problema**: Testcontainers usa docker-java 3.3.6 con API v1.32, pero Docker Desktop 29.x y Colima 29.2.x requieren mínimo API v1.40/1.44. Esto rompe los integration tests.

### Solución: proxy Python que upgradeea la versión API

**1. Instalar Colima** (runtime Docker compatible):
```bash
brew install colima
colima start
```

**2. Crear proxy** `/tmp/docker_proxy.py`:
```python
#!/usr/bin/env python3
import socket, threading, os, re

PROXY_SOCKET = '/tmp/docker-proxy.sock'
REAL_SOCKET  = os.path.expanduser('~/.colima/default/docker.sock')
MIN_VERSION  = '1.44'

def upgrade_version(data):
    try:
        text = data.decode('utf-8', errors='replace')
        def replace_version(m):
            major, minor = int(m.group(1)), int(m.group(2))
            if major < 1 or (major == 1 and minor < 44):
                return f'/v{MIN_VERSION}/'
            return m.group(0)
        return re.sub(r'/v(\d+)\.(\d+)/', replace_version, text).encode('utf-8')
    except:
        return data

def forward(src, dst, upgrade=False):
    try:
        while True:
            data = src.recv(65536)
            if not data: break
            dst.sendall(upgrade_version(data) if upgrade else data)
    except: pass
    finally:
        try: src.close()
        except: pass
        try: dst.close()
        except: pass

def handle_client(client_sock):
    real = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
    real.connect(REAL_SOCKET)
    for args in [(client_sock, real, True), (real, client_sock, False)]:
        t = threading.Thread(target=forward, args=args, daemon=True)
        t.start()

if os.path.exists(PROXY_SOCKET): os.unlink(PROXY_SOCKET)
server = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
server.bind(PROXY_SOCKET)
os.chmod(PROXY_SOCKET, 0o777)
server.listen(100)
print(f'Proxy on {PROXY_SOCKET} → {REAL_SOCKET}')
while True:
    c, _ = server.accept()
    threading.Thread(target=handle_client, args=(c,), daemon=True).start()
```

**3. Arrancar proxy en background**:
```bash
python3 /tmp/docker_proxy.py &
```

**4. Configurar testcontainers** (`~/.testcontainers.properties`):
```properties
docker.client.strategy=org.testcontainers.dockerclient.EnvironmentAndSystemPropertyClientProviderStrategy
```

**5. Correr tests**:
```bash
export DOCKER_HOST="unix:///tmp/docker-proxy.sock"
./gradlew cleanTest test
```

---

## 9. Commits pendientes — secuencia completa

```bash
# En feature/catalog-service:

git add catalog-service/src/main/java/com/linktic/catalog/config/
git commit -m "feat: add security, Swagger and JPA configuration"

git add catalog-service/src/main/resources/
git commit -m "feat: add application config and structured JSON logging"

git add catalog-service/build.gradle catalog-service/settings.gradle \
        catalog-service/gradlew catalog-service/gradlew.bat catalog-service/gradle/
git commit -m "build: add Gradle Groovy build with JaCoCo 80% coverage verification"

git add catalog-service/Dockerfile
git commit -m "feat: add multi-stage Dockerfile for catalog-service"

git add catalog-service/src/test/
git commit -m "test: add unit and integration tests for catalog-service (100% coverage)"

git push origin feature/catalog-service

# Merge a develop:
git checkout develop
git merge --no-ff feature/catalog-service -m "feat: merge feature/catalog-service into develop"
git push origin develop

# Crear rama stock-service:
git checkout -b feature/stock-service
```

---

## 10. Checklist Final

### catalog-service ✅/🔄
- [x] CatalogItem + CatalogRepository
- [x] DTOs + Mapper + Exceptions
- [x] CatalogService + CatalogController
- [ ] Config classes (ApiAuthFilter, Swagger, WebSecurity, JPA) — por commitear
- [ ] application.yml + logback-spring.xml — por commitear
- [ ] build.gradle + Dockerfile — por commitear
- [ ] Tests (21 tests, 100% cobertura) — por commitear
- [ ] Merge a develop

### stock-service ❌
- [ ] Entidades StockEntry + PurchaseRecord
- [ ] StockRepository + PurchaseRepository
- [ ] StockService (lazy creation, processPurchase, adjustStock)
- [ ] ProductGateway con Resilience4j @CircuitBreaker + @Retry
- [ ] StockEventPublisher → RabbitMQ exchange stock.events
- [ ] BrokerConfiguration (TopicExchange, Queue, Binding)
- [ ] StockController (/api/v1/stock)
- [ ] DTOs + Mapper + Exception handler
- [ ] Config (auth filter, swagger, security)
- [ ] application.yml + application-docker.yml
- [ ] logback-spring.xml
- [ ] build.gradle + Dockerfile
- [ ] Tests unitarios + integración (≥80% cobertura)

### frontend ❌
- [ ] Inicializar Quasar 2.x (npm create quasar)
- [ ] Tema oscuro custom (indigo/cyan)
- [ ] CatalogPage (tabla productos)
- [ ] PurchasePage (QStepper 3 pasos)
- [ ] StockPage (consulta por producto)
- [ ] Axios con interceptors (X-API-KEY header)
- [ ] Dockerfile multi-stage (node → nginx)

### infraestructura ❌
- [ ] docker-compose.yml con healthchecks en todos los servicios
- [ ] Variables de entorno documentadas

### documentación ❌
- [ ] README.md con diagrama Mermaid de arquitectura
- [ ] Decisiones técnicas documentadas
- [ ] Instrucciones de ejecución local y Docker

### QA final ❌
- [ ] `./gradlew test` en ambos servicios: BUILD SUCCESSFUL
- [ ] `./gradlew jacocoTestCoverageVerification` pasa ≥80%
- [ ] `docker-compose up` levanta todo sin errores
- [ ] Tag `v1.0.0` en main
- [ ] Merge develop → main

---

## 11. Decisiones de diseño importantes

1. **Lazy creation en stock-service**: si el producto no tiene registro de stock, se crea con quantity=0 en el primer acceso (en lugar de escuchar un evento de RabbitMQ del catalog-service)
2. **RabbitMQ solo en stock-service**: publica `stock.updated` después de compra/ajuste; no hay consumer en este proyecto
3. **@EnableJpaAuditing en JpaConfiguration separado**: evita conflicto con @WebMvcTest
4. **ApiAuthFilter como Filter puro** (no Spring Security filter): requiere excluirlo en @WebMvcTest con `excludeFilters`
5. **Todos los nombres son distintos** al proyecto de referencia (linktic-test-project): clases, métodos, endpoints, topics RabbitMQ

---

*Generado: 2026-05-13 | Proyecto: linktic-prueba | Rama: feature/catalog-service*
