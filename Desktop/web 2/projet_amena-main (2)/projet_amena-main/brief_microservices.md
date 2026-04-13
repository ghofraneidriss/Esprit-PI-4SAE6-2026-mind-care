# BRIEF TECHNIQUE — PROJET MICROSERVICES
### Spring Boot + Angular + Spring Cloud
> Prompt / Contexte pour Agent IA

---

## 1. Vue d'ensemble de l'architecture

Application web distribuée basée sur une architecture microservices.
- **Backend** : Spring Boot / Spring Cloud
- **Frontend** : Angular
- Chaque microservice est indépendant, possède sa propre base de données, et communique via des mécanismes standardisés.

| Service | Rôle | Port |
|---|---|---|
| `eureka-server` | Service de découverte — enregistre et localise tous les MS | 8761 |
| `config-server` | Configuration centralisée — fournit les properties à tous les MS | 8888 |
| `gateway` | API Gateway — point d'entrée unique, route les requêtes | 8085 |
| `candidat-ms` | Microservice Candidat | 8060 |
| `job-ms` | Microservice Job | 8081 |
| `keycloak` | Serveur d'authentification OAuth2/OIDC | 8080 |
| `frontend` | Application Angular | 4200 |

---

## 2. Eureka Server (Service Discovery)

Eureka est le registre de services. Chaque MS s'y enregistre au démarrage et récupère l'URL des autres services dynamiquement.

**Dépendance Maven (serveur)**
```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>
```

**Annotation principale**
```java
@EnableEurekaServer  // dans la classe main du serveur
```

**application.properties (serveur)**
```properties
spring.application.name=eureka-server
server.port=8761
eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false
```

**Chaque MS client ajoute**
```properties
eureka.client.serviceUrl.defaultZone=http://localhost:8761/eureka
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=true
```

---

## 3. Config Server (Configuration centralisée)

Spring Cloud Config Server centralise toutes les configurations. Les MS récupèrent leurs properties au démarrage depuis ce serveur.

**Dépendances Maven (serveur)**
```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-config-server</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

**Annotations**
```java
@EnableConfigServer
@EnableDiscoveryClient
```

**application.properties (serveur)**
```properties
spring.application.name=config-server
server.port=8888
eureka.client.serviceUrl.defaultZone=http://localhost:8761/eureka
eureka.client.register-with-eureka=true

spring.profiles.active=native
spring.cloud.config.server.native.searchLocations=classpath:/config
# Alternative Git :
# spring.cloud.config.server.git.uri=https://github.com/mon-repo/config.git
```

**Structure des fichiers centralisés** dans `src/main/resources/config/` :
```
config/
├── application.properties       ← config partagée à tous les MS
├── candidat-ms.properties       ← config spécifique au MS Candidat
└── job-ms.properties            ← config spécifique au MS Job
```

**Dépendance Maven (clients MS)**
```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
```

**application.properties (clients)**
```properties
spring.cloud.config.enabled=true
spring.config.import=optional:configserver:http://localhost:8888
```

### Rafraîchissement dynamique avec Actuator

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

```properties
management.endpoints.web.exposure.include=refresh
```

```http
POST http://localhost:8060/actuator/refresh
```

> Les beans utilisant `@Value` doivent être annotés `@RefreshScope` pour être rechargés sans redémarrage.

---

## 4. API Gateway

L'API Gateway est le seul point d'entrée exposé au frontend Angular. Elle route les requêtes vers les MS appropriés avec load balancing automatique via Eureka.

**Dépendances Maven**
```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

### Configuration dynamique via Eureka (recommandée)

**application.properties**
```properties
spring.application.name=gateway
server.port=8085
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=true

# Route vers candidat-ms
spring.cloud.gateway.routes[0].id=CANDIDAT
spring.cloud.gateway.routes[0].uri=lb://candidat-ms
spring.cloud.gateway.routes[0].predicates[0]=Path=/candidats/**

# Route vers job-ms
spring.cloud.gateway.routes[1].id=JOB
spring.cloud.gateway.routes[1].uri=lb://job-ms
spring.cloud.gateway.routes[1].predicates[0]=Path=/jobs/**
```

> `lb://` = "load balanced" : la Gateway utilise Eureka pour résoudre l'URL réelle du service.

### Configuration statique (sans Eureka)

```java
@Bean
public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
    return builder.routes()
        .route("Candidat", r -> r.path("/candidats/**").uri("http://localhost:8060"))
        .route("Job",      r -> r.path("/jobs/**").uri("http://localhost:8081"))
        .build();
}
```

### Algorithmes de load balancing

| Algorithme | Description |
|---|---|
| Round Robin (défaut) | Distribue les requêtes à tour de rôle entre les instances |
| Random | Sélectionne une instance au hasard |
| Weighted | Attribue un poids à chaque instance |

### Logs de débogage Gateway

```properties
logging.level.org.springframework.cloud.gateway=DEBUG
logging.level.reactor.netty.http.client=DEBUG
```

### CORS (obligatoire pour Angular)

```properties
spring.cloud.gateway.globalcors.corsConfigurations.[/**].allowedOrigins=http://localhost:4200
spring.cloud.gateway.globalcors.corsConfigurations.[/**].allowedMethods=*
spring.cloud.gateway.globalcors.corsConfigurations.[/**].allowedHeaders=*
spring.cloud.gateway.globalcors.corsConfigurations.[/**].allowCredentials=true
```

---

## 5. Communication inter-services : OpenFeign

OpenFeign permet la communication synchrone HTTP entre microservices de façon déclarative, comme si on appelait une méthode locale.

**Dépendance Maven (MS appelant — ici candidat-ms)**
```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

**Activation**
```java
@SpringBootApplication
@EnableFeignClients
public class CandidatApplication { ... }
```

**Créer le client Feign (interface dans candidat-ms)**
```java
@FeignClient(name = "job-ms")  // name = spring.application.name du MS cible
public interface JobClient {

    @GetMapping("/jobs")
    List<Job> getAllJobs();

    @GetMapping("/jobs/{id}")
    Job getJobById(@PathVariable int id);
}
```

> Avec Eureka actif, l'attribut `url` est inutile : Feign résout l'URL automatiquement depuis le registre.

**DTO (Data Transfer Object)**

Créer une classe `Job` dans `candidat-ms` **sans `@Entity`** — elle sert uniquement à recevoir les données du `job-ms`, elle n'est pas persistée en BDD :

```java
public class Job {
    private int id;
    private String service;
    private boolean etat;
    // constructeurs, getters, setters
}
```

**Injection et utilisation dans le service**
```java
@Service
public class CandidatService {
    @Autowired
    private JobClient jobClient;

    public List<Job> getJobs() {
        return jobClient.getAllJobs();
    }

    public Job getJobById(int id) {
        return jobClient.getJobById(id);
    }
}
```

**Exposition dans le contrôleur**
```java
@RestController
@RequestMapping("/candidats")
public class CandidatController {
    @Autowired
    private CandidatService candidatService;

    @GetMapping("/jobs")
    public List<Job> getAllJobs() {
        return candidatService.getJobs();
    }

    @GetMapping("/jobs/{id}")
    public Job getJobById(@PathVariable int id) {
        return candidatService.getJobById(id);
    }
}
```

---

## 6. Sécurité avec Keycloak (OAuth2 / OIDC)

Keycloak est le serveur d'authentification. Il émet des JWT tokens. Les MS et la Gateway vérifient ces tokens. Angular obtient un token et l'envoie dans chaque requête HTTP.

### Configuration Keycloak (côté serveur Keycloak)

1. Créer un **realm** (ex: `microservices-realm`)
2. Créer un **client** OAuth2 (ex: `angular-client`) avec :
   - Access Type : `public`
   - Valid Redirect URIs : `http://localhost:4200/*`
   - Web Origins : `http://localhost:4200`
3. Créer des **rôles** (ex: `ROLE_USER`, `ROLE_ADMIN`) et les assigner aux utilisateurs

### Côté microservices (Resource Server)

**Dépendances Maven**
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

**application.properties**
```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8080/realms/microservices-realm
```

**SecurityConfig**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/public/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }
}
```

### Côté Angular

**Installation**
```bash
npm install keycloak-js keycloak-angular
```

**Configuration (app.config.ts ou app.module.ts)**
```typescript
import Keycloak from 'keycloak-js';
import { KeycloakService } from 'keycloak-angular';

export function initializeKeycloak(keycloak: KeycloakService): () => Promise<boolean> {
  return () => keycloak.init({
    config: {
      url: 'http://localhost:8080',
      realm: 'microservices-realm',
      clientId: 'angular-client'
    },
    initOptions: {
      onLoad: 'login-required',
      checkLoginIframe: false
    }
  });
}
```

**Intercepteur HTTP — envoi automatique du token**
```typescript
// KeycloakBearerInterceptor ajoute automatiquement
// 'Authorization: Bearer <token>' dans chaque requête HTTP
providers: [
  {
    provide: HTTP_INTERCEPTORS,
    useClass: KeycloakBearerInterceptor,
    multi: true
  }
]
```

---

## 7. Ordre de démarrage des services

| Ordre | Service | Raison |
|---|---|---|
| 1 | Keycloak | Doit être disponible avant tout pour l'authentification |
| 2 | Eureka Server | Registre — les autres services doivent s'y enregistrer |
| 3 | Config Server | Fournit les configurations aux MS au démarrage |
| 4 | candidat-ms, job-ms | Dans n'importe quel ordre |
| 5 | API Gateway | Doit trouver les MS dans Eureka |
| 6 | Frontend Angular | Consomme tout via la Gateway |

---

## 8. Stack technique complète

| Composant | Technologie |
|---|---|
| Langage backend | Java 17 |
| Framework backend | Spring Boot 3.x |
| Cloud | Spring Cloud 2023.x |
| Build tool | Maven |
| Service discovery | Spring Cloud Netflix Eureka |
| Configuration | Spring Cloud Config (mode natif ou Git) |
| Gateway | Spring Cloud Gateway (reactive / WebFlux) |
| Communication sync | OpenFeign (`spring-cloud-starter-openfeign`) |
| Sécurité | Keycloak + Spring Security OAuth2 Resource Server |
| BDD (dev) | H2 in-memory par MS — remplacer par PostgreSQL en prod |
| Frontend | Angular 17+, keycloak-angular, HttpClient |
| Tests API | Postman / Bruno |

---

## 9. Structure de projet recommandée

```
project-root/
├── eureka-server/
│   └── src/main/
│       ├── java/  (@EnableEurekaServer)
│       └── resources/application.properties
│
├── config-server/
│   └── src/main/
│       ├── java/  (@EnableConfigServer @EnableDiscoveryClient)
│       └── resources/
│           ├── application.properties
│           └── config/
│               ├── application.properties
│               ├── candidat-ms.properties
│               └── job-ms.properties
│
├── gateway/
│   └── src/main/resources/application.properties
│
├── candidat-ms/
│   └── src/main/java/
│       ├── CandidatApplication.java  (@EnableFeignClients)
│       ├── Candidat.java             (entité JPA)
│       ├── Job.java                  (DTO — pas d'@Entity)
│       ├── JobClient.java            (@FeignClient)
│       ├── CandidatRepository.java
│       ├── CandidatService.java
│       └── CandidatController.java
│
├── job-ms/
│   └── src/main/java/
│       ├── Job.java                  (entité JPA)
│       ├── JobRepository.java
│       ├── JobService.java
│       └── JobController.java
│
└── frontend-angular/
    └── src/app/
        ├── app.config.ts             (Keycloak init)
        ├── services/
        │   ├── candidat.service.ts
        │   └── job.service.ts
        └── components/
```

---

## 10. Points d'attention pour l'agent

- **Spring Cloud Gateway utilise WebFlux (réactif)** — ne pas ajouter `spring-boot-starter-web`, utiliser `spring-boot-starter-webflux` si besoin.
- **`lb://` dans l'URI de la Gateway** nécessite obligatoirement Eureka actif.
- **`@RefreshScope`** sur les beans utilisant `@Value` pour le rechargement dynamique via Config Server.
- **Pas de `url` dans `@FeignClient`** si Eureka est actif — Feign résout l'URL tout seul.
- **CORS doit être configuré dans la Gateway**, pas dans chaque MS séparément (sauf si accès direct).
- **Keycloak en dev** : démarrer avec Docker pour simplifier :
  ```bash
  docker run -p 8080:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak:latest start-dev
  ```
- **H2 en dev, PostgreSQL en prod** — prévoir les profils Spring (`application-dev.properties`, `application-prod.properties`).

---

*— Fin du brief technique —*
