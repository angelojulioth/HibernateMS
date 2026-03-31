# Guía de Arquitectura Limpia con Spring Boot

Una guía para principiantes que explica cada componente de este proyecto.

---

## Tabla de Contenidos
1. [Estructura del Proyecto](#estructura-del-proyecto)
2. [Arquitectura Limpia Explicada](#arquitectura-limpia-explicada)
3. [Maven y Dependencias](#maven-y-dependencias)
4. [Docker Compose y PostgreSQL](#docker-compose-y-postgresql)
5. [Patrón Mediador](#patrón-mediador)
6. [Validadores Personalizados](#validadores-personalizados)
7. [Problem Details RFC 7807](#problem-details-rfc-7807)
8. [Bus de Eventos y Eventos de Dominio](#bus-de-eventos-y-eventos-de-dominio)
9. [Spring Security y JWT](#spring-security-y-jwt)
10. [Adaptación a Microservicios](#adaptación-a-microservicios)
11. [Análisis Profundo de Anotaciones](#análisis-profundo-de-anotaciones)
12. [Guía de Registro de Actividad (Logging)](#guía-de-registro-de-actividad-logging)
13. [Guía de Pruebas](#guía-de-pruebas)
14. [Cómo Ejecutar](#cómo-ejecutar)

---

## Estructura del Proyecto

```
src/
├── main/java/com/example/hibernatedemo/
│   ├── domain/
│   │   ├── entity/           # Entidades JPA (Order, Product, AppUser)
│   │   └── enums/            # Enumeraciones (OrderStatus, Role)
│   ├── application/
│   │   ├── dto/              # DTOs de Petición/Respuesta
│   │   ├── mediator/         # Patrón Mediador
│   │   │   ├── commands/     # Objetos Command (operaciones de escritura)
│   │   │   ├── queries/      # Objetos Query (operaciones de lectura)
│   │   │   └── handlers/     # Manejadores de Command y Query
│   │   ├── service/          # Servicios tradicionales (lecturas paginadas)
│   │   ├── events/           # Eventos de dominio e interfaz EventBus
│   │   └── validators/       # Anotaciones de validación personalizadas
│   ├── infrastructure/
│   │   ├── repository/       # Repositorios JPA
│   │   ├── config/           # Configuración de Spring
│   │   ├── security/         # JWT, SecurityConfig, UserDetailsService
│   │   ├── events/           # Implementación SpringEventBus, listeners
│   │   ├── client/           # Clientes HTTP para comunicación entre servicios
│   │   └── resilience/       # Configuración de Circuit Breaker y Retry
│   ├── interfaces/
│   │   ├── controller/       # Controladores REST (usan Mediador)
│   │   └── exception/        # Manejador global ProblemDetail
│   └── HibernateDemoApplication.java
├── test/java/com/example/hibernatedemo/integration/
│   ├── OrderControllerIntegrationTest.java
│   ├── ProductControllerIntegrationTest.java
│   ├── EventDispatchIntegrationTest.java
│   └── security/SecurityIntegrationTest.java
├── main/resources/application.yml
├── test/resources/application-test.yml
├── pom.xml
├── docker-compose.yml
├── Dockerfile
├── prometheus.yml
└── k8s/
    ├── configmap.yaml
    ├── secret.yaml
    ├── deployment.yaml
    └── service.yaml
```

---

## Arquitectura Limpia Explicada

La Arquitectura Limpia separa el código en capas donde las capas internas no saben nada sobre las capas externas.

```
┌─────────────────────────────────────────────────────────────┐
│                    CAPA DE INTERFAZ                         │
│  Controladores → Mediator.send(Command/Query)               │
│   ┌─────────────────────────────────────────────────────┐   │
│   │               CAPA DE APLICACIÓN                    │   │
│   │  Manejadores → Repositorios + EventBus              │   │
│   │   ┌─────────────────────────────────────────────┐   │   │
│   │   │                CAPA DE DOMINIO              │   │   │
│   │   │   Entidades + Enums (puro, sin dependencias)│   │   │
│   │   └─────────────────────────────────────────────┘   │   │
│   └─────────────────────────────────────────────────────┘   │
│   ┌─────────────────────────────────────────────────────┐   │
│   │            CAPA DE INFRAESTRUCTURA                  │   │
│   │   Repositorios + Seguridad + EventBus impl          │   │
│   └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### Flujo de una Petición (Patrón Mediador)

```
Petición HTTP → Controlador → Mediator.send(Command) → CommandHandler → Repositorio → EventBus.publish(Event) → EventListener
```

---

## Maven y Dependencias

### ¿Qué es Maven?

Maven es una herramienta de automatización de construcción. Gestiona:
- **Dependencias**: Librerías que tu proyecto necesita
- **Ciclo de vida**: Compilar → Probar → Empaquetar → Desplegar
- **Estructura del proyecto**: Diseño de carpetas estandarizado

### Dependencias Clave

| Dependencia | Propósito |
|------------|---------|
| `spring-boot-starter-web` | APIs REST, servidor Tomcat embebido |
| `spring-boot-starter-data-jpa` | Hibernate, JPA, abstracción de base de datos |
| `spring-boot-starter-validation` | Anotaciones `@Valid`, `@NotBlank`, `@Size` |
| `spring-boot-starter-security` | Autenticación y autorización |
| `spring-boot-starter-actuator` | Monitoreo, health checks, métricas |
| `jjwt-api/impl/jackson` | Generación y validación de tokens JWT |
| `postgresql` | Controlador JDBC de PostgreSQL |
| `lombok` | Reduce código repetitivo (getters, setters, builders) |
| `resilience4j-spring-boot3` | Circuit breaker, retry, rate limiter |
| `micrometer-tracing-bridge-brave` | Trazabilidad distribuida |
| `micrometer-registry-prometheus` | Métricas en formato Prometheus |
| `h2` | Base de datos en memoria para pruebas |
| `spring-security-test` | Utilidades para pruebas de seguridad |

---

## Docker Compose y PostgreSQL

### ¿Qué es Docker Compose?

Docker Compose te permite definir y ejecutar aplicaciones multi-contenedor con un solo archivo.

### docker-compose.yml

```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: hibernate-demo-db
    environment:
      POSTGRES_DB: hibernate_demo
      POSTGRES_USER: demo_user
      POSTGRES_PASSWORD: demo_password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U demo_user -d hibernate_demo"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - app-network

  app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: hibernate-demo-app
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/hibernate_demo
      SPRING_DATASOURCE_USERNAME: demo_user
      SPRING_DATASOURCE_PASSWORD: demo_password
      MANAGEMENT_ZIPKIN_TRACING_ENDPOINT: http://zipkin:9411/api/v2/spans
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy
    networks:
      - app-network

  zipkin:
    image: openzipkin/zipkin:latest
    container_name: hibernate-demo-zipkin
    ports:
      - "9411:9411"
    networks:
      - app-network

  prometheus:
    image: prom/prometheus:latest
    container_name: hibernate-demo-prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
    networks:
      - app-network

volumes:
  postgres_data:

networks:
  app-network:
    driver: bridge
```

| Propiedad | Significado |
|----------|---------|
| `image` | Imagen Docker a usar (PostgreSQL 16, versión Alpine Linux) |
| `container_name` | Nombre legible para el contenedor |
| `environment` | Variables de entorno pasadas al contenedor |
| `ports` | Mapea puerto del host al puerto del contenedor (`host:contenedor`) |
| `volumes` | Almacenamiento persistente (los datos sobreviven reinicios) |
| `healthcheck` | Verifica si PostgreSQL está listo antes de marcarlo como saludable |
| `networks` | Red interna para comunicación entre contenedores |

### Comandos

```bash
# Iniciar todos los servicios
docker compose up -d

# Ver registros
docker compose logs -f

# Detener los servicios
docker compose down

# Detener y eliminar datos
docker compose down -v

# Construir y ejecutar
docker compose up --build
```

### Servicios Incluidos

| Servicio | Puerto | Propósito |
|----------|--------|-----------|
| PostgreSQL | 5432 | Base de datos principal |
| Aplicación | 8080 | Tu API Spring Boot |
| Zipkin | 9411 | Trazabilidad distribuida |
| Prometheus | 9090 | Métricas y monitoreo |

---

## Patrón Mediador

### ¿Qué es?

El patrón Mediador centraliza el manejo de peticiones. En lugar de que los controladores llamen servicios directamente, envían **Commands** (escrituras) o **Queries** (lecturas) a un **Mediador**, que los enruta al **Manejador** correcto. Este es el enfoque **CQRS-lite**.

### ¿Por qué usarlo?

- **Responsabilidad Única**: Cada manejador hace UNA sola cosa
- **Testabilidad**: Prueba manejadores de forma aislada
- **Desacoplamiento**: Los controladores no dependen de implementaciones de servicios
- **Preocupaciones transversales**: Agrega registro, validación o métricas en un solo lugar (el Mediador)

### Interfaces Principales

```java
// Interfaz marcadora para commands (operaciones de escritura)
public interface Command<R> {}

// Interfaz marcadora para queries (operaciones de lectura)
public interface Query<R> {}

// Maneja un command, retorna un resultado
public interface CommandHandler<C extends Command<R>, R> {
    R handle(C command);
}

// Maneja un query, retorna un resultado
public interface QueryHandler<Q extends Query<R>, R> {
    R handle(Q query);
}
```

### Cómo funciona

**1. Define un Command:**
```java
public class CreateOrderCommand implements Command<OrderResponse> {
    private CreateOrderRequest request;
}
```

**2. Crea un Manejador:**
```java
@Component
@RequiredArgsConstructor
public class CreateOrderCommandHandler
        implements CommandHandler<CreateOrderCommand, OrderResponse> {

    private final OrderRepository orderRepository;
    private final EventBus eventBus;

    @Override
    @Transactional
    public OrderResponse handle(CreateOrderCommand command) {
        // 1. Convertir DTO a entidad
        Order order = Order.builder()
                .customerName(command.getRequest().getCustomerName())
                .build();

        // 2. Guardar
        Order saved = orderRepository.save(order);

        // 3. Publicar evento
        eventBus.publish(new OrderCreatedEvent(saved.getId(), ...));

        // 4. Retornar respuesta
        return toResponse(saved);
    }
}
```

**3. Úsalo en el Controlador:**
```java
@RestController
@RequiredArgsConstructor
public class OrderController {
    private final Mediator mediator;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = mediator.send(new CreateOrderCommand(request));
        return ResponseEntity.status(201).body(response);
    }
}
```

### La clase Mediator

```java
@Component
public class Mediator {
    private final Map<Class<?>, CommandHandler<?, ?>> commandHandlers;
    private final Map<Class<?>, QueryHandler<?, ?>> queryHandlers;

    // Spring inyecta TODOS los manejadores automáticamente vía List<>
    public Mediator(List<CommandHandler<?, ?>> cmds, List<QueryHandler<?, ?>> qrys) { ... }

    public <R> R send(Command<R> command) {
        CommandHandler handler = commandHandlers.get(command.getClass());
        return handler.handle(command);
    }

    public <R> R send(Query<R> query) {
        QueryHandler handler = queryHandlers.get(query.getClass());
        return handler.handle(query);
    }
}
```

Spring auto-descubre todos los manejadores `@Component` y los inyecta. El Mediador construye un mapa de búsqueda por tipo de command/query.

---

## Validadores Personalizados

### ¿Qué son?

Los validadores personalizados te permiten escribir lógica de validación que se integra con `@Valid`. Spring los llama automáticamente cuando anotas un campo.

### Cómo crear uno

**Paso 1: Define la anotación**
```java
@Documented
@Constraint(validatedBy = UniqueOrderNumberValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueOrderNumber {
    String message() default "El número de orden ya existe";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

| Meta-anotación | Propósito |
|-----------------|---------|
| `@Constraint(validatedBy = ...)` | Vincula con la clase validadora |
| `@Target` | ¿Dónde se puede usar esta anotación? |
| `@Retention(RUNTIME)` | Disponible en tiempo de ejecución para reflexión |

**Paso 2: Implementa el validador**
```java
@RequiredArgsConstructor
public class UniqueOrderNumberValidator
        implements ConstraintValidator<UniqueOrderNumber, String> {

    private final OrderRepository orderRepository;

    @Override
    public boolean isValid(String orderNumber, ConstraintValidatorContext context) {
        if (orderNumber == null) return true;  // Deja que @NotBlank maneje null
        return !orderRepository.existsByOrderNumber(orderNumber);
    }
}
```

| Método | Propósito |
|--------|---------|
| `isValid()` | Retorna `true` si es válido, `false` si es inválido |
| `ConstraintValidatorContext` | Agrega mensajes de error personalizados dinámicamente |

**Paso 3: Úsalo**
```java
public class CreateOrderRequest {
    @UniqueOrderNumber(message = "Este número de orden ya está en uso")
    private String orderNumber;
}
```

### Anotaciones de Validación Integradas

| Anotación | Propósito |
|------------|---------|
| `@NotNull` | No puede ser null |
| `@NotBlank` | No puede ser null, vacío o solo espacios |
| `@NotEmpty` | Colección/string no puede ser null o vacío |
| `@Size(min, max)` | Longitud de string/colección |
| `@DecimalMin / @DecimalMax` | Rango numérico decimal |
| `@Min / @Max` | Rango de enteros |
| `@Email` | Formato de email válido |
| `@Pattern(regexp)` | Coincidencia con expresión regular |

---

## Problem Details RFC 7807

### ¿Qué es?

RFC 7807 es un formato estándar para respuestas de error de APIs. En lugar de inventar tu propia estructura JSON de error, usas una estandarizada que todos los consumidores de API entienden.

### Formato estándar
```json
{
    "type": "https://api.example.com/errors/validation",
    "title": "Error de Validación",
    "status": 400,
    "detail": "La validación de entrada falló",
    "errors": {
        "customerName": "El nombre del cliente es obligatorio",
        "orderNumber": "El número de orden es obligatorio"
    },
    "timestamp": 1711900000000
}
```

| Campo | Significado |
|-------|---------|
| `type` | URI que identifica el tipo de error (legible por máquina) |
| `title` | Resumen corto legible por humanos |
| `status` | Código de estado HTTP |
| `detail` | Explicación específica para esta ocurrencia |
| `errors` | Campo personalizado para detalles de validación |

### Cómo lo implementa Spring Boot

Spring Boot 3 tiene la clase `ProblemDetail` integrada:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ProblemDetail handleRuntimeException(RuntimeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Recurso No Encontrado");
        problem.setType(URI.create("https://api.example.com/errors/not-found"));
        problem.setProperty("timestamp", System.currentTimeMillis());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError e : ex.getBindingResult().getFieldErrors()) {
            errors.put(e.getField(), e.getDefaultMessage());
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "La validación de entrada falló");
        problem.setTitle("Error de Validación");
        problem.setType(URI.create("https://api.example.com/errors/validation"));
        problem.setProperty("errors", errors);
        return problem;
    }
}
```

### ¿Por qué usar ProblemDetail en lugar de ErrorResponse personalizado?

- **Estandarizado**: Cualquier librería cliente de API lo entiende
- **Integrado en Spring**: No necesitas una clase personalizada
- **Extensible**: Agrega campos personalizados con `setProperty()`
- **Consistente**: Todos los errores siguen la misma forma

---

## Bus de Eventos y Eventos de Dominio

### ¿Qué es?

Los Eventos de Dominio representan algo importante que sucedió en tu sistema. El Bus de Eventos los distribuye a los escuchadores. Esto desacopla la acción de sus efectos secundarios.

### ¿Por qué usar eventos?

- **Desacoplamiento**: La creación de órdenes no necesita saber sobre notificaciones por email, analíticas, etc.
- **Extensibilidad**: Agrega nuevos escuchadores sin modificar código existente
- **Testabilidad**: Verifica que los eventos fueron publicados
- **Preparado para el futuro**: Reemplaza el bus en memoria con RabbitMQ/Kafka después

### Arquitectura

```
┌──────────────┐     publish()      ┌──────────────┐
│ CommandHandler │ ────────────────→ │  EventBus    │
└──────────────┘                    └──────┬───────┘
                                           │
                                    Spring ApplicationEventPublisher
                                           │
                              ┌────────────┴────────────┐
                              ▼                         ▼
                    ┌─────────────────┐       ┌─────────────────┐
                    │ OrderCreated    │       │ OrderStatus     │
                    │ EventListener   │       │ ChangedListener │
                    └─────────────────┘       └─────────────────┘
```

### Componentes Principales

**1. DomainEvent (clase base)**
```java
public abstract class DomainEvent {
    private final String eventId;        // ID único para rastreo
    private final LocalDateTime occurredOn;  // Cuándo ocurrió
    private final String eventType;      // Nombre de clase para enrutamiento
}
```

**2. Eventos Concretos**
```java
public class OrderCreatedEvent extends DomainEvent {
    private final Long orderId;
    private final String orderNumber;
    private final String customerName;
}
```

**3. Interfaz EventBus (en capa de aplicación)**
```java
public interface EventBus {
    void publish(DomainEvent event);
}
```

**4. Implementación SpringEventBus (en capa de infraestructura)**
```java
@Component
public class SpringEventBus implements EventBus {
    private final ApplicationEventPublisher publisher;

    @Override
    public void publish(DomainEvent event) {
        publisher.publishEvent(event);  // Usa el sistema de eventos de Spring
    }
}
```

**5. Escuchadores de Eventos**
```java
@Component
public class OrderEventListener {
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Orden creada: {}", event.getOrderNumber());
        // Enviar email, actualizar analíticas, notificar almacén, etc.
    }
}
```

### Uso en un Manejador

```java
@Override
@Transactional
public OrderResponse handle(CreateOrderCommand command) {
    Order saved = orderRepository.save(order);

    // Publicar evento - al manejador no le importa quién escucha
    eventBus.publish(new OrderCreatedEvent(
        saved.getId(), saved.getOrderNumber(), saved.getCustomerName()));

    return toResponse(saved);
}
```

### Reemplazar con una cola de mensajes real después

Para cambiar de memoria a RabbitMQ o Kafka:

```java
// Solo reemplaza la implementación del EventBus:
@Component
public class RabbitMqEventBus implements EventBus {
    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(DomainEvent event) {
        rabbitTemplate.convertAndSend("events.exchange", event.getEventType(), event);
    }
}
```

El resto de tu código (manejadores, controladores) no cambia en absoluto.

---

## Spring Security y JWT

### ¿Qué es JWT?

JSON Web Token (JWT) es un formato de token compacto y autocontenido. Contiene:
- **Encabezado**: Información del algoritmo
- **Carga útil**: Claims (nombre de usuario, roles, expiración)
- **Firma**: Previene manipulación

### Flujo de Autenticación

```
1. POST /api/auth/register { username, password, roles }
2. POST /api/auth/login { username, password } → retorna { token }
3. GET /api/orders  Authorization: Bearer <token>
```

### Componentes

**1. Entidad AppUser**
```java
@Entity
public class AppUser {
    private String username;
    private String password;        // Codificado con BCrypt
    private Set<Role> roles;        // ROLE_USER, ROLE_ADMIN
    private Boolean enabled;
}
```

**2. CustomUserDetailsService**
```java
@Service
public class CustomUserDetailsService implements UserDetailsService {
    @Override
    public UserDetails loadUserByUsername(String username) {
        AppUser user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("..."));

        return User.builder()
            .username(user.getUsername())
            .password(user.getPassword())
            .authorities(user.getRoles().stream()
                .map(r -> new SimpleGrantedAuthority(r.name()))
                .toList())
            .build();
    }
}
```

**3. JwtService**
```java
@Component
public class JwtService {
    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
            .subject(userDetails.getUsername())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 86400000))
            .signWith(getSigningKey())
            .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }
}
```

**4. JwtAuthenticationFilter**
```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, ...) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);
        String username = jwtService.extractUsername(jwt);

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (jwtService.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        filterChain.doFilter(request, response);
    }
}
```

**5. SecurityConfig**
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/orders/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/orders/**").hasAnyRole("USER", "ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

### Reglas de Autorización

| Endpoint | USER | ADMIN |
|----------|------|-------|
| `POST /api/auth/register` | ✓ | ✓ |
| `POST /api/auth/login` | ✓ | ✓ |
| `GET /api/products` | ✓ | ✓ |
| `POST /api/products` | ✗ (403) | ✓ |
| `DELETE /api/products` | ✗ (403) | ✓ |
| `GET /api/orders` | ✓ | ✓ |
| `POST /api/orders` | ✓ | ✓ |
| `DELETE /api/orders` | ✗ (403) | ✓ |

### @EnableMethodSecurity

Habilita anotaciones de seguridad a nivel de método:
```java
@PreAuthorize("hasRole('ADMIN')")
public void adminOnlyMethod() { ... }
```

---

## Adaptación a Microservicios

### ¿Qué hace a este proyecto adaptable como microservicio?

Este proyecto está diseñado para escalar de una aplicación monolítica a una arquitectura de microservicios con cambios mínimos.

### Componentes de Microservicio Agregados

#### 1. Spring Boot Actuator

Proporciona endpoints de producción:

| Endpoint | Propósito |
|----------|-----------|
| `/actuator/health` | Verifica si la aplicación está funcionando |
| `/actuator/health/readiness` | ¿Está lista para recibir tráfico? |
| `/actuator/health/liveness` | ¿Está viva la aplicación? |
| `/actuator/info` | Información de la aplicación |
| `/actuator/metrics` | Métricas de rendimiento |
| `/actuator/prometheus` | Métricas en formato Prometheus |

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
```

#### 2. Trazabilidad Distribuida (Micrometer + Zipkin)

Rastrea una petición a través de múltiples servicios:

```yaml
management:
  tracing:
    sampling:
      probability: 1.0
    enabled: true
```

Cada petición recibe un `traceId` único que se propaga entre servicios. Zipkin visualiza el camino completo.

#### 3. Resilience4j (Circuit Breaker + Retry)

Protege tu servicio cuando otros servicios fallan:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      inventoryService:
        slidingWindowSize: 10           # Ventana de 10 peticiones
        failureRateThreshold: 50        # Se abre al 50% de fallos
        waitDurationInOpenState: 10000  # Espera 10s antes de reintentar
        permittedNumberOfCallsInHalfOpenState: 3
  retry:
    instances:
      inventoryService:
        maxAttempts: 3                  # Máximo 3 intentos
        waitDuration: 500ms             # Espera 500ms entre intentos
```

**Estados del Circuit Breaker:**
```
CERRADO (normal) → ABIERTO (demasiados fallos) → SEMI-ABIERTO (probando) → CERRADO (recuperado)
```

#### 4. RestClient para Comunicación entre Servicios

```java
@Component
@RequiredArgsConstructor
public class ServiceClient {
    private final RestClient restClient;

    @Value("${app.service.inventory.base-url:http://inventory-service:8081}")
    private String inventoryBaseUrl;

    public String checkInventory(String sku) {
        return restClient.get()
                .uri(inventoryBaseUrl + "/api/inventory/{sku}", sku)
                .retrieve()
                .body(String.class);
    }
}
```

#### 5. Dockerfile

```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
EXPOSE 8080
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

| Instrucción | Propósito |
|-------------|-----------|
| `eclipse-temurin:17-jre-alpine` | Imagen ligera de Java 17 |
| `adduser` | Ejecutar como usuario no-root (seguridad) |
| `MaxRAMPercentage=75.0` | Usa 75% de la memoria del contenedor |
| `/dev/./urandom` | Generación rápida de números aleatorios |

#### 6. Kubernetes

**ConfigMap** - Configuración externa:
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: hibernate-demo-config
data:
  SPRING_PROFILES_ACTIVE: "kubernetes"
  APP_JWT_SECRET: "..."
  MANAGEMENT_ENDPOINTS_WEB_EXPOSURE: "health,info,metrics,prometheus"
```

**Secret** - Datos sensibles:
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: hibernate-demo-secrets
type: Opaque
stringData:
  SPRING_DATASOURCE_URL: "jdbc:postgresql://postgres:5432/hibernate_demo"
  SPRING_DATASOURCE_USERNAME: "demo_user"
  SPRING_DATASOURCE_PASSWORD: "demo_password"
```

**Deployment** - Despliegue con auto-escalado:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: hibernate-demo
spec:
  replicas: 2                          # 2 instancias
  strategy:
    type: RollingUpdate                # Actualización sin downtime
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    spec:
      containers:
        - name: hibernate-demo
          image: hibernate-demo:latest
          readinessProbe:              # ¿Listo para tráfico?
            httpGet:
              path: /actuator/health/readiness
              port: 8080
          livenessProbe:               # ¿Sigue vivo?
            httpGet:
              path: /actuator/health/liveness
              port: 8080
          resources:
            requests:
              memory: "256Mi"
              cpu: "250m"
            limits:
              memory: "512Mi"
              cpu: "500m"
```

**Service** - Descubrimiento interno:
```yaml
apiVersion: v1
kind: Service
metadata:
  name: hibernate-demo
spec:
  selector:
    app: hibernate-demo
  ports:
    - name: http
      port: 80
      targetPort: 8080
  type: ClusterIP                      # Solo accesible dentro del cluster
```

### Cómo Escalar a Múltiples Microservicios

```
                    ┌─────────────┐
                    │  API Gateway │
                    │  (Kong/Nginx)│
                    └──────┬──────┘
                           │
              ┌────────────┼────────────┐
              ▼            ▼            ▼
        ┌──────────┐ ┌──────────┐ ┌──────────┐
        │  Órdenes │ │Productos │ │ Inventario│
        │ (este    │ │ (este    │ │ (nuevo    │
        │ servicio)│ │ servicio)│ │ servicio) │
        └────┬─────┘ └────┬─────┘ └────┬─────┘
             │            │            │
             └────────────┼────────────┘
                          ▼
                   ┌─────────────┐
                   │   EventBus  │
                   │ (RabbitMQ/  │
                   │   Kafka)    │
                   └─────────────┘
```

**Pasos para separar en microservicios:**

1. **Extrae el dominio**: Cada dominio (Órdenes, Productos, Inventario) se convierte en su propio servicio
2. **Compartir EventBus**: Reemplaza `SpringEventBus` con `RabbitMqEventBus` o `KafkaEventBus`
3. **Comunicación HTTP**: Usa `ServiceClient` con `RestClient` para llamadas entre servicios
4. **Configuración centralizada**: Usa Spring Cloud Config o Kubernetes ConfigMaps
5. **Descubrimiento de servicios**: Usa Eureka, Consul o Kubernetes DNS
6. **Gateway**: Agrega un API Gateway para enrutar peticiones externas

### Comandos de Kubernetes

```bash
# Aplicar configuración
kubectl apply -f k8s/

# Ver pods
kubectl get pods

# Ver logs
kubectl logs -f deployment/hibernate-demo

# Escalar manualmente
kubectl scale deployment hibernate-demo --replicas=5

# Eliminar todo
kubectl delete -f k8s/
```

---

## Análisis Profundo de Anotaciones

### Anotaciones Nuevas

| Anotación | Propósito |
|------------|---------|
| `@RestControllerAdvice` | Manejador global de excepciones para REST |
| `@ExceptionHandler` | Mapea tipos de excepción a métodos manejadores |
| `@Constraint` | Vincula anotación de validación personalizada con clase validadora |
| `@EventListener` | Marca un método para recibir ApplicationEvents de Spring |
| `@EnableWebSecurity` | Habilita soporte web de Spring Security |
| `@EnableMethodSecurity` | Habilita `@PreAuthorize`, `@PostAuthorize` |
| `@OncePerRequestFilter` | Asegura que el filtro se ejecute una vez por petición |
| `@PreAuthorize` | Verificación de seguridad a nivel de método antes de ejecutar |
| `@CircuitBreaker` | Aplica circuit breaker a un método (Resilience4j) |
| `@Retry` | Reintenta automáticamente un método fallido (Resilience4j) |

### Orden de la Cadena de Filtros

```
Petición HTTP
    ↓
JwtAuthenticationFilter (extrae y valida JWT)
    ↓
UsernamePasswordAuthenticationFilter (login por formulario - deshabilitado)
    ↓
AuthorizationFilter (verifica @PreAuthorize y reglas de URL)
    ↓
Controlador
```

### Filtro de Cadena Completo con Resilience

```
HTTP Request
    ↓
JwtAuthenticationFilter
    ↓
AuthorizationFilter
    ↓
Controller → Mediator → Handler → ServiceClient → CircuitBreaker → Otro Servicio
                                                              ↓ (si falla)
                                                         Fallback Method
```

---

## Guía de Registro de Actividad (Logging)

### Niveles de Logging (de más a menos verboso)

| Nivel | Cuándo Usar | Ejemplo |
|-------|-------------|---------|
| `TRACE` | Depuración extremadamente detallada | Valores de parámetros SQL |
| `DEBUG` | Depuración en desarrollo | Entrada/salida de métodos, valores de variables |
| `INFO` | Eventos importantes de negocio | Orden creada, usuario conectado |
| `WARN` | Problemas potenciales | Uso de API obsoleta, consultas lentas |
| `ERROR` | Errores que necesitan atención | Excepciones, operaciones fallidas |

### Cómo Registrar

```java
@Slf4j  // Lombok genera: private static final Logger log = LoggerFactory.getLogger(ThisClass.class);
@Service
public class OrderService {

    public void doSomething() {
        log.trace("Información muy detallada: {}", variable);
        log.debug("Información de depuración: {}", valor);
        log.info("Evento de negocio: orden creada para {}", nombreCliente);
        log.warn("Problema potencial: consulta lenta detectada");
        log.error("Error ocurrido: {}", excepcion.getMessage(), excepcion);
    }
}
```

### Patrón de Log Explicado

```yaml
logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

| Patrón | Salida |
|---------|--------|
| `%d{yyyy-MM-dd HH:mm:ss}` | Marca de tiempo: `2024-01-15 10:30:45` |
| `[%thread]` | Nombre del hilo: `[http-nio-8080-exec-1]` |
| `%-5level` | Nivel de log (rellenado a 5 chars): `INFO ` |
| `%logger{36}` | Nombre del logger (truncado): `c.e.h.application.service.OrderService` |
| `%msg` | El mensaje de log |
| `%n` | Nueva línea |

### Logging de SQL de Hibernate

```yaml
logging:
  level:
    org.hibernate.SQL: DEBUG                    # Muestra sentencias SQL
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE  # Muestra valores de parámetros
```

Ejemplo de salida:
```
DEBUG: insert into orders (customer_name, order_number, status, total_amount) values (?, ?, ?, ?)
TRACE: binding parameter [1] as [VARCHAR] - [John Doe]
TRACE: binding parameter [2] as [VARCHAR] - [ORD-001]
```

### Logging de Eventos

```java
// En el manejador
log.info("Manejando CreateOrderCommand para cliente: {}", request.getCustomerName());
log.info("Orden creada con id: {}", savedOrder.getId());

// En el bus de eventos
log.info("Publicando evento: {} (id: {})", event.getEventType(), event.getEventId());

// En el escuchador de eventos
log.info("Recibido OrderCreatedEvent - OrderId: {}, Cliente: {}",
    event.getOrderId(), event.getCustomerName());
```

### Logging de Seguridad

```java
log.info("Intento de login para usuario: {}", request.getUsername());
log.info("Login exitoso para usuario: {}", request.getUsername());
log.info("Registrando nuevo usuario: {}", request.getUsername());
```

---

## Guía de Pruebas

### Pruebas de Autenticación y Autorización

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        // Registrar y hacer login como admin
        restTemplate.postForEntity(baseUrl + "/api/auth/register",
            RegisterRequest.builder()
                .username("admin")
                .password("admin123")
                .roles(Set.of(Role.ROLE_ADMIN))
                .build(), String.class);

        LoginResponse login = restTemplate.postForEntity(
            baseUrl + "/api/auth/login",
            new LoginRequest("admin", "admin123"),
            LoginResponse.class).getBody();
        adminToken = login.getToken();
    }

    @Test
    void peticionNoAutenticada_deberiaRetornar401() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl + "/api/orders", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void usuario_noPuedeEliminarOrdenes_deberiaRetornar403() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(userToken);

        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl + "/api/orders/1",
            HttpMethod.DELETE,
            new HttpEntity<>(headers),
            String.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void admin_puedeEliminarOrdenes() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        ResponseEntity<Void> response = restTemplate.exchange(
            baseUrl + "/api/orders/1",
            HttpMethod.DELETE,
            new HttpEntity<>(headers),
            Void.class);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}
```

### Pruebas de Envío de Eventos

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class EventDispatchIntegrationTest {

    @Autowired
    private EventBus eventBus;

    @Autowired
    private TestEventListener testEventListener;

    @Test
    void crearOrden_deberiaEnviarOrderCreatedEvent() {
        // 1. Crear orden vía HTTP
        restTemplate.exchange(baseUrl + "/api/orders", HttpMethod.POST,
            new HttpEntity<>(request, authHeaders()), Map.class);

        // 2. Verificar que el evento fue enviado
        assertTrue(testEventListener.getOrderCreatedEvents().size() >= 1);

        // 3. Verificar contenido del evento
        OrderCreatedEvent event = testEventListener.getOrderCreatedEvents().get(0);
        assertEquals("ORD-001", event.getOrderNumber());
    }

    @Test
    void actualizarEstadoOrden_deberiaEnviarOrderStatusChangedEvent() {
        // Crear orden, luego actualizar estado
        restTemplate.exchange(baseUrl + "/api/orders/" + id, HttpMethod.PUT,
            new HttpEntity<>(updateRequest, authHeaders()), Map.class);

        OrderStatusChangedEvent event = testEventListener.getStatusChangedEvents().get(0);
        assertEquals(OrderStatus.PENDING, event.getOldStatus());
        assertEquals(OrderStatus.PROCESSING, event.getNewStatus());
    }

    @Test
    void eventos_deberianTenerIdsUnicos() {
        eventBus.publish(new OrderCreatedEvent(1L, "A", "Test 1"));
        eventBus.publish(new OrderCreatedEvent(2L, "B", "Test 2"));

        var events = testEventListener.getOrderCreatedEvents();
        assertNotEquals(events.get(0).getEventId(), events.get(1).getEventId());
    }

    // Escuchador de prueba que captura eventos
    @Component
    static class TestEventListener implements ApplicationListener<DomainEvent> {
        private final CopyOnWriteArrayList<OrderCreatedEvent> orderCreatedEvents = new CopyOnWriteArrayList<>();
        private final CopyOnWriteArrayList<OrderStatusChangedEvent> statusChangedEvents = new CopyOnWriteArrayList<>();

        @Override
        public void onApplicationEvent(DomainEvent event) {
            if (event instanceof OrderCreatedEvent e) orderCreatedEvents.add(e);
            if (event instanceof OrderStatusChangedEvent e) statusChangedEvents.add(e);
        }

        public void reset() {
            orderCreatedEvents.clear();
            statusChangedEvents.clear();
        }
    }
}
```

### Pruebas de Respuestas ProblemDetail

```java
@Test
void entradaInvalida_deberiaRetornarProblemDetail() {
    ResponseEntity<Map> response = restTemplate.exchange(
        baseUrl + "/api/orders",
        HttpMethod.POST,
        new HttpEntity<>(Map.of("customerName", ""), authHeaders()),
        Map.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    Map body = response.getBody();
    assertEquals("Error de Validación", body.get("title"));
    assertEquals(400, body.get("status"));
    assertNotNull(body.get("errors"));
}
```

### Pruebas con @WithMockUser (pruebas unitarias)

```java
@Test
@WithMockUser(username = "admin", roles = {"ADMIN"})
void usuarioAdmin_puedeAccederEndpointAdmin() {
    mockMvc.perform(get("/api/admin"))
        .andExpect(status().isOk());
}

@Test
@WithMockUser(username = "user", roles = {"USER"})
void usuarioRegular_noPuedeAccederEndpointAdmin() {
    mockMvc.perform(get("/api/admin"))
        .andExpect(status().isForbidden());
}
```

### Anotaciones de Prueba Explicadas

| Anotación | Propósito |
|------------|---------|
| `@SpringBootTest` | Carga el contexto completo de la aplicación |
| `webEnvironment = RANDOM_PORT` | Inicia el servidor en puerto aleatorio (evita conflictos) |
| `@ActiveProfiles("test")` | Usa configuración de `application-test.yml` |
| `@TestMethodOrder(OrderAnnotation.class)` | Ejecuta pruebas en secuencia `@Order` |
| `@Order(1)` | Orden de ejecución |
| `@LocalServerPort` | Inyecta el número de puerto aleatorio |
| `@BeforeEach` | Se ejecuta antes de cada método de prueba |
| `@Test` | Marca un método como prueba |
| `@WithMockUser` | Simula un usuario autenticado para pruebas unitarias |

### Base de datos H2 en modo PostgreSQL

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH
    username: sa
    password:
    driver-class-name: org.h2.Driver

  jpa:
    hibernate:
      ddl-auto: create-drop
```

| Propiedad | Propósito |
|----------|---------|
| `MODE=PostgreSQL` | Hace que H2 se comporte como PostgreSQL |
| `DATABASE_TO_LOWER=TRUE` | Convierte identificadores a minúsculas (comportamiento PostgreSQL) |
| `create-drop` | Crea el esquema al inicio, lo elimina al detener |

---

## Cómo Ejecutar

### Prerrequisitos
- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- (Opcional) kubectl para Kubernetes

### Paso 1: Iniciar PostgreSQL
```bash
docker compose up -d
```

### Paso 2: Construir
```bash
mvn clean install
```

### Paso 3: Ejecutar
```bash
mvn spring-boot:run
```

### Paso 4: Ejecutar Pruebas
```bash
# Todas las pruebas
mvn test

# Solo pruebas de seguridad
mvn test -Dtest="SecurityIntegrationTest"

# Solo pruebas de eventos
mvn test -Dtest="EventDispatchIntegrationTest"
```

### Paso 5: Probar la API

```bash
# Registrar un usuario
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"demo123","roles":["ROLE_USER"]}'

# Iniciar sesión
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"demo123"}'

# Usar el token
TOKEN="tu-token-aqui"

# Crear una orden
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"customerName":"John","orderNumber":"ORD-001","status":"PENDING","totalAmount":99.99}'

# Obtener órdenes
curl http://localhost:8080/api/orders \
  -H "Authorization: Bearer $TOKEN"
```

### Ejecutar con Docker Compose (todos los servicios)

```bash
# Construir y ejecutar todo
docker compose up --build

# Acceder a:
# - API: http://localhost:8080
# - Zipkin: http://localhost:9411
# - Prometheus: http://localhost:9090
```

### Desplegar en Kubernetes

```bash
# Construir imagen Docker
mvn clean package
docker build -t hibernate-demo:latest .

# Aplicar configuración de Kubernetes
kubectl apply -f k8s/

# Verificar despliegue
kubectl get pods
kubectl get services

# Acceder a la API (port-forward)
kubectl port-forward service/hibernate-demo 8080:80
```

### Endpoints de Monitoreo

```bash
# Health check
curl http://localhost:8080/actuator/health

# Métricas
curl http://localhost:8080/actuator/metrics

# Métricas Prometheus
curl http://localhost:8080/actuator/prometheus

# Información de la aplicación
curl http://localhost:8080/actuator/info
```
