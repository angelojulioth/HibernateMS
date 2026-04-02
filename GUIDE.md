# Guia de Arquitectura y Operacion del Repositorio

Esta guia refleja el estado actual real del repositorio (microservicios separados).

## 1. Panorama General

El repositorio contiene 4 microservicios Spring Boot y recursos compartidos para orquestacion local:

- `order-service`: API principal de ordenes y productos. Implementa arquitectura limpia, CQRS-lite (Mediator), eventos de dominio, seguridad JWT y resiliencia para llamadas entre servicios.
- `inventory-service`: Gestion de inventario.
- `shipping-service`: Gestion de envios.
- `auth-service`: Registro/login y validacion de credenciales/JWT.

Infraestructura compartida:

- `docker-compose.yml`: levanta PostgreSQL + los 4 microservicios + Zipkin + Prometheus.
- `init-db.sql`: crea `inventory_db`, `shipping_db`, `auth_db` (la DB `order_db` se crea por variable `POSTGRES_DB`).
- `prometheus.yml`: scraping de metricas.
- `k8s/`: manifiestos base para despliegue.
- `CLOUD_MIGRATION_GUIDE.md`: pasos para nube/event bus gestionado.

## 2. Estructura Real del Repositorio

```text
.
├── auth-service/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
├── inventory-service/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
├── order-service/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
├── shipping-service/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
├── k8s/
├── docker-compose.yml
├── init-db.sql
├── prometheus.yml
└── pom.xml                  # agregador Maven (packaging=pom)
```

Nota importante: ahora `order-service` ya no vive en la raiz del repo; esta completamente separado dentro de `order-service/`.

## 3. Modelo de Build (Maven)

- `pom.xml` en la raiz es agregador y declara modulos:
  - `auth-service`
  - `inventory-service`
  - `order-service`
  - `shipping-service`
- Cada servicio mantiene su propio `pom.xml` con `spring-boot-starter-parent`.

Comandos utiles:

```bash
# Compilar todo el workspace
mvn clean verify

# Compilar un servicio puntual
mvn -pl order-service clean verify
mvn -pl auth-service clean verify
```

## 4. Orquestacion Local (Docker Compose)

Servicios publicados:

- `order-service` -> `localhost:8080`
- `inventory-service` -> `localhost:8081`
- `shipping-service` -> `localhost:8082`
- `auth-service` -> `localhost:8083`
- `postgres` -> `localhost:5432`
- `zipkin` -> `localhost:9411`
- `prometheus` -> `localhost:9090`

Flujo de red:

1. `order-service` atiende requests externos.
2. `order-service` consulta/invoca a `inventory-service`, `shipping-service` y `auth-service` por HTTP interno.
3. Todos persisten en PostgreSQL (bases separadas por servicio).
4. Actuator + Micrometer exponen metricas para Prometheus.
5. Trazas distribuidas se envian a Zipkin.

Comandos utiles:

```bash
docker compose up --build -d
docker compose logs -f order-service
docker compose down
docker compose down -v
```

## 5. Arquitectura Interna de `order-service`

`order-service` es el servicio mas completo y aplica una version de Clean Architecture:

- `domain`: entidades y enums de negocio.
- `application`: DTOs, mediador (commands/queries/handlers), eventos, validadores y servicios de aplicacion.
- `infrastructure`: repositorios JPA, seguridad JWT, clientes HTTP, config, resiliencia y event bus Spring.
- `interfaces`: controladores REST y manejo global de excepciones (`ProblemDetail`).

Flujo tipico:

```text
HTTP Request -> Controller -> Mediator -> Handler -> Repository
                                      -> EventBus -> Listener
```

## 6. Seguridad y Comunicacion entre Servicios

- `auth-service` autentica usuarios y emite/valida JWT.
- `order-service` protege endpoints con Spring Security + filtro JWT.
- `order-service` usa URLs configurables para `inventory`, `shipping` y `auth`.

Variables y propiedades relevantes estan en los `application.yml` de cada servicio.

## 7. Observabilidad

- Actuator habilitado para health/metrics/prometheus.
- Zipkin para trazas.
- Prometheus para metricas.
- Niveles de log configurados por servicio.

## 8. Pruebas

Cada microservicio incluye `src/test` y profile de test (`application-test.yml`) con H2 para pruebas aisladas.

## 9. Estado de esta Guia

Esta guia fue actualizada para alinear:

- estructura de carpetas real,
- build multi-modulo,
- compose multi-servicio,
- separacion fisica de `order-service` en su propio directorio.
