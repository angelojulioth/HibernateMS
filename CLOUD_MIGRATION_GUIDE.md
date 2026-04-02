# Guía de Migración a Google Cloud Pub/Sub y Cloud Logging

Este proyecto está diseñado para que cambiar a Google Cloud Pub/Sub y Cloud Logging sea **reemplazar un archivo de configuración y agregar dependencias**. Tu código de negocio **no cambia**.

---

## Parte 1: Migrar EventBus a Google Cloud Pub/Sub

### ¿Por qué es fácil?

Porque usamos el **patrón Adapter**. Tu código solo conoce la interfaz `EventBus`:

```java
// TU código solo ve esto (NO CAMBIA NUNCA):
public interface EventBus {
    void publish(DomainEvent event);
}
```

La implementación actual (`SpringEventBus`) y la futura (`GooglePubSubEventBus`) son **intercambiables**.

### Arquitectura actual vs. futura

```
ACTUAL (en memoria):                    FUTURA (Google Cloud Pub/Sub):
┌──────────────┐                        ┌──────────────┐
│  Handler     │                        │  Handler     │
│  publish()   │                        │  publish()   │
└──────┬───────┘                        └──────┬───────┘
       │                                       │
       ▼                                       ▼
┌──────────────┐                        ┌──────────────────┐
│ SpringEventBus│                       │ GooglePubSub     │
│              │                        │ EventBus         │
│ Application  │                        │                  │
│ EventPublisher                       │ PubSubTemplate   │
└──────┬───────┘                        └───────┬──────────┘
       │                                       │
       ▼                                       ▼
┌──────────────┐                        ┌──────────────────┐
│ @EventListener│                       │ Google Cloud     │
│ (mismo JVM)  │                       │ Pub/Sub Topics   │
└──────────────┘                        └───────┬──────────┘
                                                │
                                                ▼
                                        ┌──────────────────┐
                                        │ Suscriptores      │
                                        │ (pueden estar en  │
                                        │  otros servicios) │
                                        └──────────────────┘
```

### Paso 1: Agregar dependencias

Agrega esto a tu `pom.xml`:

```xml
<!-- Google Cloud Pub/Sub -->
<dependency>
    <groupId>com.google.cloud</groupId>
    <artifactId>spring-cloud-gcp-starter-pubsub</artifactId>
    <version>5.0.0</version>
</dependency>

<!-- Para serializar eventos a JSON -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

### Paso 2: Configurar credenciales de Google Cloud

Crea un archivo de configuración para Google Cloud:

```yaml
# application.yml (agrega esto)
spring:
  cloud:
    gcp:
      project-id: tu-proyecto-gcp
      pubsub:
        enabled: true

# Autenticación (elige UNA de estas opciones):

# Opción A: Archivo de credenciales (desarrollo local)
google:
  cloud:
    pubsub:
      credentials:
        location: classpath:google-credentials.json

# Opción B: Variable de entorno (producción en GCP)
# GOOGLE_APPLICATION_CREDENTIALS=/path/to/credentials.json

# Opción C: Service Account de GKE/GCE (producción)
# No necesitas configurar nada, usa las credenciales del entorno
```

### Paso 3: Crear la nueva implementación del EventBus

Crea este archivo **sin borrar** el existente:

```java
// src/main/java/com/example/orderservice/infrastructure/events/GooglePubSubEventBus.java

package com.example.orderservice.infrastructure.events;

import com.example.orderservice.application.events.DomainEvent;
import com.example.orderservice.application.events.EventBus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.PublisherFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("gcp")  // Solo se activa con el perfil "gcp"
public class GooglePubSubEventBus implements EventBus {

    private final PubSubTemplate pubSubTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.pubsub.topic-prefix:order-service-}")
    private String topicPrefix;

    public GooglePubSubEventBus(PubSubTemplate pubSubTemplate, ObjectMapper objectMapper) {
        this.pubSubTemplate = pubSubTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(DomainEvent event) {
        try {
            String topicName = topicPrefix + event.getEventType();
            String payload = objectMapper.writeValueAsString(event);

            log.info("Publicando evento en Pub/Sub: topic={}, eventId={}", topicName, event.getEventId());

            pubSubTemplate.publish(topicName, payload.getBytes());

        } catch (JsonProcessingException e) {
            log.error("Error serializando evento: {}", event.getEventType(), e);
            throw new RuntimeException("Error publicando evento en Pub/Sub", e);
        }
    }
}
```

### Paso 4: Desactivar el SpringEventBus en producción

Modifica el `SpringEventBus` existente para que solo se use en desarrollo:

```java
// src/main/java/com/example/orderservice/infrastructure/events/SpringEventBus.java

@Slf4j
@Component
@Profile("!gcp")  // Se activa en TODOS los perfiles EXCEPTO "gcp"
public class SpringEventBus implements EventBus {
    // ... (el código existente NO cambia)
}
```

### Paso 5: Crear suscriptores (listeners) en Pub/Sub

Los `@EventListener` actuales solo funcionan en el mismo JVM. Para Pub/Sub, necesitas suscriptores:

```java
// src/main/java/com/example/orderservice/infrastructure/events/PubSubOrderEventListener.java

package com.example.orderservice.infrastructure.events;

import com.example.orderservice.application.events.OrderCreatedEvent;
import com.example.orderservice.application.events.OrderStatusChangedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.PublisherFactory;
import com.google.cloud.spring.pubsub.support.SubscriberFactory;
import com.google.cloud.spring.pubsub.integration.AckMode;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("gcp")
public class PubSubOrderEventListener {

    private final ObjectMapper objectMapper;

    public PubSubOrderEventListener(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // Canal para recibir mensajes de OrderCreatedEvent
    @Bean
    public MessageChannel orderCreatedInputChannel() {
        return new DirectChannel();
    }

    // Adaptador que conecta Pub/Sub con el canal
    @Bean
    public PubSubInboundChannelAdapter orderCreatedAdapter(
            @Qualifier("orderCreatedInputChannel") MessageChannel channel,
            @Value("${app.pubsub.topic-prefix:order-service-}") String topicPrefix,
            SubscriberFactory subscriberFactory) {

        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(
                subscriberFactory,
                topicPrefix + "OrderCreatedEvent-subscription"
        );
        adapter.setOutputChannel(channel);
        adapter.setAckMode(AckMode.AUTO);
        return adapter;
    }

    // Suscribirse al canal y procesar mensajes
    @org.springframework.integration.annotation.ServiceActivator(inputChannel = "orderCreatedInputChannel")
    public void handleOrderCreated(com.google.cloud.spring.pubsub.core.PubSubMessage message) {
        try {
            String payload = new String(message.getPayload());
            OrderCreatedEvent event = objectMapper.readValue(payload, OrderCreatedEvent.class);

            log.info("Recibido OrderCreatedEvent desde Pub/Sub - OrderId: {}, Customer: {}",
                    event.getOrderId(), event.getCustomerName());

            // Aquí va tu lógica: enviar email, actualizar inventario, etc.

        } catch (Exception e) {
            log.error("Error procesando OrderCreatedEvent desde Pub/Sub", e);
        }
    }

    // Repite el patrón para OrderStatusChangedEvent
    @Bean
    public MessageChannel orderStatusChangedInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public PubSubInboundChannelAdapter orderStatusChangedAdapter(
            @Qualifier("orderStatusChangedInputChannel") MessageChannel channel,
            @Value("${app.pubsub.topic-prefix:order-service-}") String topicPrefix,
            SubscriberFactory subscriberFactory) {

        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(
                subscriberFactory,
                topicPrefix + "OrderStatusChangedEvent-subscription"
        );
        adapter.setOutputChannel(channel);
        adapter.setAckMode(AckMode.AUTO);
        return adapter;
    }

    @org.springframework.integration.annotation.ServiceActivator(inputChannel = "orderStatusChangedInputChannel")
    public void handleOrderStatusChanged(com.google.cloud.spring.pubsub.core.PubSubMessage message) {
        try {
            String payload = new String(message.getPayload());
            OrderStatusChangedEvent event = objectMapper.readValue(payload, OrderStatusChangedEvent.class);

            log.info("Recibido OrderStatusChangedEvent desde Pub/Sub - OrderId: {} -> {}",
                    event.getOldStatus(), event.getNewStatus());

        } catch (Exception e) {
            log.error("Error procesando OrderStatusChangedEvent desde Pub/Sub", e);
        }
    }
}
```

### Paso 6: Crear los topics y suscripciones en Google Cloud

Antes de ejecutar, necesitas crear los recursos en Google Cloud:

```bash
# Instalar gcloud CLI si no lo tienes
# https://cloud.google.com/sdk/docs/install

# Autenticar
gcloud auth login
gcloud config set project tu-proyecto-gcp

# Crear topics (uno por tipo de evento)
gcloud pubsub topics create order-service-OrderCreatedEvent
gcloud pubsub topics create order-service-OrderStatusChangedEvent

# Crear suscripciones (una por cada listener)
gcloud pubsub subscriptions create order-service-OrderCreatedEvent-subscription \
    --topic=order-service-OrderCreatedEvent

gcloud pubsub subscriptions create order-service-OrderStatusChangedEvent-subscription \
    --topic=order-service-OrderStatusChangedEvent
```

### Paso 7: Ejecutar con el perfil "gcp"

```bash
# Desarrollo local con Pub/Sub emulado
mvn spring-boot:run -Dspring-boot.run.profiles=gcp

# O con variable de entorno
SPRING_PROFILES_ACTIVE=gcp mvn spring-boot:run
```

### Resumen de cambios

| Archivo | Acción | ¿Cambia tu código de negocio? |
|---------|--------|-------------------------------|
| `pom.xml` | Agregar dependencia | No |
| `application.yml` | Agregar config de GCP | No |
| `SpringEventBus.java` | Agregar `@Profile("!gcp")` | No |
| `GooglePubSubEventBus.java` | **Nuevo archivo** | No |
| `PubSubOrderEventListener.java` | **Nuevo archivo** | No |
| `OrderCreatedEvent.java` | Sin cambios | No |
| `CreateOrderCommandHandler.java` | Sin cambios | No |
| **Código de negocio** | **Sin cambios** | **No** |

### ¿Cómo probar localmente sin Google Cloud?

Google Cloud ofrece un emulador de Pub/Sub para desarrollo local:

```bash
# Instalar emulador
gcloud components install pubsub-emulator

# Iniciar emulador
gcloud beta emulators pubsub start --host-port=localhost:8085

# En otra terminal, configurar variables de entorno
export PUBSUB_EMULATOR_HOST=localhost:8085

# Ejecutar tu aplicación
SPRING_PROFILES_ACTIVE=gcp mvn spring-boot:run
```

---

## Parte 2: Migrar a Google Cloud Logging

### ¿Por qué es fácil?

Porque usas **SLF4J** (la interfaz estándar de logging en Java). Google Cloud Logging solo necesita un **adapter** que redirige las llamadas a SLF4J hacia Google Cloud.

**Tu código de logging NO cambia en absoluto:**

```java
// Este código NO cambia:
log.info("Orden creada con id: {}", savedOrder.getId());
log.error("Error procesando evento", e);
```

### Arquitectura actual vs. futura

```
ACTUAL:                               FUTURA:
┌──────────────┐                      ┌──────────────┐
│  Tu código   │                      │  Tu código   │
│  log.info()  │                      │  log.info()  │
└──────┬───────┘                      └──────┬───────┘
       │                                     │
       ▼                                     ▼
┌──────────────┐                      ┌──────────────┐
│   SLF4J      │                      │   SLF4J      │
└──────┬───────┘                      └──────┬───────┘
       │                                     │
       ▼                                     ▼
┌──────────────┐                      ┌──────────────┐
│  Logback     │                      │  Logback     │
│  Console     │                      │  + Google    │
│  Appender    │                      │  Appender    │
└──────┬───────┘                      └──────┬───────┘
       │                                     │
       ▼                                     ▼
┌──────────────┐                      ┌──────────────┐
│  Consola     │                      │ Google Cloud │
│  (terminal)  │                      │   Logging    │
└──────────────┘                      └──────────────┘
```

### Paso 1: Agregar dependencia

Agrega esto a tu `pom.xml`:

```xml
<!-- Google Cloud Logging (adapter para Logback) -->
<dependency>
    <groupId>com.google.cloud</groupId>
    <artifactId>google-cloud-logging-logback</artifactId>
    <version>0.131.0-alpha</version>
</dependency>
```

### Paso 2: Configurar Logback

Crea o modifica el archivo `src/main/resources/logback-spring.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

    <!-- Perfil por defecto: logging a consola -->
    <springProfile name="default,dev,test">
        <include resource="org/springframework/boot/logging/logback/base.xml"/>
    </springProfile>

    <!-- Perfil GCP: logging a Google Cloud -->
    <springProfile name="gcp">
        <appender name="CLOUD" class="com.google.cloud.logging.logback.LoggingAppender">
            <!-- Nombre del log en Google Cloud -->
            <log>order-service-app</log>

            <!-- Nivel mínimo de log -->
            <loggingLevel>INFO</loggingLevel>

            <!-- Flush automático cada 5 segundos -->
            <flushLevel>INFO</flushLevel>

            <!-- Opcional: agregar campos personalizados -->
            <enhancer>com.example.orderservice.infrastructure.config.GcpLoggingEnhancer</enhancer>
        </appender>

        <!-- También mantener consola para debugging local -->
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>

        <root level="INFO">
            <appender-ref ref="CLOUD"/>
            <appender-ref ref="CONSOLE"/>
        </root>

        <!-- Logs específicos de tu aplicación -->
        <logger name="com.example.orderservice" level="DEBUG"/>
    </springProfile>

</configuration>
```

### Paso 3: (Opcional) Agregar campos personalizados a los logs

Crea un enhancer para agregar metadata útil a cada log:

```java
// src/main/java/com/example/orderservice/infrastructure/config/GcpLoggingEnhancer.java

package com.example.orderservice.infrastructure.config;

import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.LoggingEnhancer;
import com.google.cloud.logging.MonitoredResource;

import java.util.HashMap;
import java.util.Map;

public class GcpLoggingEnhancer implements LoggingEnhancer {

    @Override
    public void enhanceLogEntry(LogEntry.Builder builder) {
        Map<String, String> labels = new HashMap<>();

        // Identificar la aplicación
        labels.put("application", "order-service");
        labels.put("environment", System.getenv("ENVIRONMENT") != null
                ? System.getenv("ENVIRONMENT") : "development");

        // Versión de la aplicación (útil para debugging)
        labels.put("version", System.getenv("APP_VERSION") != null
                ? System.getenv("APP_VERSION") : "latest");

        // Zona/región si está disponible
        String zone = System.getenv("GCP_ZONE");
        if (zone != null) {
            labels.put("zone", zone);
        }

        builder.setLabels(labels);

        // Configurar el recurso monitoreado
        builder.setResource(MonitoredResource.newBuilder("global")
                .addLabel("project_id", System.getenv("GOOGLE_CLOUD_PROJECT"))
                .build());
    }
}
```

### Paso 4: Configurar credenciales

Google Cloud Logging usa las mismas credenciales que Pub/Sub. Si ya configuraste Pub/Sub, **no necesitas hacer nada adicional**.

Si solo quieres Cloud Logging sin Pub/Sub:

```yaml
# application-gcp.yml
spring:
  cloud:
    gcp:
      project-id: tu-proyecto-gcp
      logging:
        enabled: true
```

### Paso 5: Ejecutar con el perfil "gcp"

```bash
SPRING_PROFILES_ACTIVE=gcp mvn spring-boot:run
```

### Ver los logs en Google Cloud Console

1. Ve a https://console.cloud.google.com/logs/query
2. En el filtro, escribe:
   ```
   resource.type="global"
   logName="projects/tu-proyecto-gcp/logs/order-service-app"
   ```
3. Verás todos tus logs con:
   - Severidad (INFO, ERROR, DEBUG)
   - Timestamp
   - Campos personalizados (application, version, environment)
   - Stack traces completos

### Ejemplo de cómo se ven los logs en Google Cloud

```json
{
  "insertId": "abc123",
  "jsonPayload": {
    "message": "Orden creada con id: 42",
    "logger": "com.example.orderservice.application.mediator.handlers.CreateOrderCommandHandler",
    "thread": "http-nio-8080-exec-1",
    "level": "INFO"
  },
  "resource": {
    "type": "global",
    "labels": {
      "project_id": "tu-proyecto-gcp"
    }
  },
  "labels": {
    "application": "order-service",
    "environment": "production",
    "version": "1.0.0"
  },
  "timestamp": "2026-03-31T15:30:00.000Z",
  "severity": "INFO"
}
```

### ¿Cómo probar localmente sin Google Cloud?

Puedes usar el emulador de Cloud Logging:

```bash
# Instalar emulador
gcloud components install beta

# No hay emulador específico para logging, pero puedes:
# 1. Usar el perfil "default" para desarrollo local (logs a consola)
# 2. Usar el perfil "gcp" solo en staging/producción

# Desarrollo local (consola)
mvn spring-boot:run

# Producción (Google Cloud)
SPRING_PROFILES_ACTIVE=gcp mvn spring-boot:run
```

---

## Parte 3: Usar ambos juntos (recomendado)

### Configuración completa para producción en GCP

```yaml
# application-gcp.yml
spring:
  cloud:
    gcp:
      project-id: tu-proyecto-gcp
      pubsub:
        enabled: true
      logging:
        enabled: true

app:
  pubsub:
    topic-prefix: order-service-
  logging:
    gcp:
      log-name: order-service-app
```

### Perfiles de Spring Boot

| Perfil | EventBus | Logging | Cuándo usar |
|--------|----------|---------|-------------|
| `default` | SpringEventBus (en memoria) | Consola | Desarrollo local |
| `test` | SpringEventBus (en memoria) | Consola | Pruebas |
| `gcp` | GooglePubSubEventBus | Google Cloud Logging | Staging/Producción |

### Ejecutar con múltiples perfiles

```bash
# Desarrollo con logging a consola y eventos en memoria
mvn spring-boot:run

# Producción con Pub/Sub y Cloud Logging
SPRING_PROFILES_ACTIVE=gcp mvn spring-boot:run

# Producción con Pub/Sub pero logging a consola (debugging)
SPRING_PROFILES_ACTIVE=gcp,console-logging mvn spring-boot:run
```

---

## Parte 4: Consideraciones importantes

### 1. Serialización de eventos

Google Cloud Pub/Sub envía bytes, no objetos Java. Necesitas serializar:

```java
// Lo que ya tienes en GooglePubSubEventBus:
String payload = objectMapper.writeValueAsString(event);
pubSubTemplate.publish(topicName, payload.getBytes());

// Y deserializar en el listener:
OrderCreatedEvent event = objectMapper.readValue(payload, OrderCreatedEvent.class);
```

**⚠️ Importante:** Asegúrate de que tus eventos tengan un constructor sin argumentos o usa Jackson mixins:

```java
// DomainEvent.java - asegúrate de que sea serializable
public abstract class DomainEvent implements Serializable {
    // ...
}
```

### 2. Orden de mensajes

Pub/Sub **no garantiza orden** por defecto. Si necesitas orden:

```java
// Usar ordering key (todos los mensajes del mismo orderId van en orden)
pubSubTemplate.publish(topicName, payload.getBytes(), orderingKey);
```

### 3. Manejo de errores

Con Pub/Sub, si tu listener falla, el mensaje se reintenta automáticamente:

```java
@org.springframework.integration.annotation.ServiceActivator(inputChannel = "orderCreatedInputChannel")
public void handleOrderCreated(PubSubMessage message) {
    try {
        // Tu lógica aquí
    } catch (Exception e) {
        log.error("Error procesando mensaje", e);
        // NO hagas ack manual - Pub/Sub reintentará
        throw e;  // El mensaje vuelve a la cola
    }
}
```

### 4. Costos de Google Cloud

| Servicio | Capa gratuita | Precio después |
|----------|---------------|----------------|
| Pub/Sub | 10 GB/mes gratis | $0.04/GB después |
| Cloud Logging | 50 GB/mes gratis | $0.50/GB después |

### 5. Monitoreo de Pub/Sub

Puedes monitorear tus topics y suscripciones en:
- https://console.cloud.google.com/cloudpubsub/topic/list
- Métricas disponibles: mensajes publicados, mensajes entregados, latencia, mensajes no entregados

### 6. Retención de mensajes

Por defecto, Pub/Sub retiene mensajes por **7 días**. Si tu servicio está caído por 3 días, al volver procesará todos los mensajes pendientes.

---

## Parte 5: Checklist de migración

### Para Pub/Sub:

- [ ] Agregar dependencia `spring-cloud-gcp-starter-pubsub`
- [ ] Crear `GooglePubSubEventBus.java`
- [ ] Agregar `@Profile("!gcp")` a `SpringEventBus.java`
- [ ] Crear `PubSubOrderEventListener.java`
- [ ] Crear topics en Google Cloud (`gcloud pubsub topics create`)
- [ ] Crear suscripciones (`gcloud pubsub subscriptions create`)
- [ ] Configurar credenciales (`GOOGLE_APPLICATION_CREDENTIALS`)
- [ ] Agregar `@Profile("gcp")` a los nuevos componentes
- [ ] Probar con emulador local
- [ ] Probar en staging
- [ ] Desplegar a producción

### Para Cloud Logging:

- [ ] Agregar dependencia `google-cloud-logging-logback`
- [ ] Crear `logback-spring.xml` con perfil gcp
- [ ] (Opcional) Crear `GcpLoggingEnhancer.java`
- [ ] Configurar credenciales
- [ ] Verificar logs en Cloud Console
- [ ] Configurar alertas basadas en logs (opcional)
- [ ] Probar en staging
- [ ] Desplegar a producción

---

## Resumen

| Aspecto | Dificultad | Tiempo estimado | Riesgo |
|---------|------------|-----------------|--------|
| Pub/Sub | Baja | 2-4 horas | Bajo (el EventBus actual sigue funcionando) |
| Cloud Logging | Muy baja | 30 minutos | Muy bajo (solo cambia dónde van los logs) |
| Ambos juntos | Baja | 3-5 horas | Bajo |

**Tu código de negocio NO cambia en absoluto.** Solo agregas archivos nuevos y configuras perfiles de Spring Boot.
