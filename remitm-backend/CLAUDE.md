# Remitm Money Transfer — Backend

Spring Boot monolith for Remitm Money Transfer.
Rebranded from RemitmGlobal / ForexBridge backend.

## Important Note
Java package namespace is `com.remitm.*` — intentionally preserved for functional stability.
Only visible brand strings (API title, emails, app name) have been updated.

## Stack
Java 17, Spring Boot 3.2.3, Spring Security, MySQL 8.0, Redis 7, Flyway, JJWT 0.12.5, SpringDoc OpenAPI 2.3.0

## Brand Config (application.yml)
```yaml
brand:
  name: Remitm Money Transfer
  support-email: support@remitm.com
  frontend-url: https://remitm.com
```

## Ports
| Environment | Port |
|-------------|------|
| Local | 8080 |
| Docker (remitm stack) | 8095 |

## Commands
```bash
cd remitm-backend
mvn clean package -DskipTests   # build JAR
java -jar target/remitm-monolith-1.0.0-SNAPSHOT.jar  # run
```

## Database
- Same schema as RemitmGlobal (shared Flyway migrations)
- DB name: `remitm` (unchanged — functionally required)
- Local: `root`/`root` or `root`/`mysql` on port 3310 (Docker stack)

## Do-Not-Touch
- `com.remitm.*` package names — changing requires full refactor
- `db/migration/V*__*.sql` — Flyway migrations are immutable
- Business logic in all `*ServiceImpl.java` files
- Security config in `SecurityConfig.java`
- JWT logic in `JwtService.java`
