# Sabor del Himalaya - Sistema Web de Pedidos a Domicilio

Sistema web de comercio electrónico y gestión de pedidos para el restaurante nepalí **"Sabor del Himalaya"**
Desarrollado con:
Java 21
Spring Boot 3.4
Thymeleaf
Spring Security
Spring Data JPA
PostgreSQL

---

## Arquitectura

- **Lenguaje / Plataforma:** Java 21 (LTS)
- **Framework:** Spring Boot 3.4.2
- **Arquitectura:** MVC tradicional con SSR (Server-Side Rendering) y `@Controller`
- **Motor de Plantillas:** Thymeleaf + Tailwind CSS
- **Persistencia:** Spring Data JPA / Hibernate (`ddl-auto=update`)
- **Base de Datos:** PostgreSQL 16 (con soporte Docker Compose)
- **Seguridad:** Spring Security (RBAC: `ROLE_CLIENT`, `ROLE_ADMIN`, `ROLE_CHEF`)
- **Gestor de Construcción:** Maven Wrapper (`./mvnw`)

---

## Funcionamiento

### 1. Iniciar la Base de Datos
```bash
docker compose up -d
```
El contenedor expondrá PostgreSQL en el puerto `5432` con la base de datos `himalaya_db` y credenciales `postgres/postgres`.

### 2. Ejecutar la App
```bash
./mvnw spring-boot:run
```
Al iniciar por primera vez, `DataInitializer` sembrará automáticamente:
- **Usuario Administrador:** `admin@himalaya.com` / `admin123`
- **Usuario Chef:** `chef@himalaya.com` / `chef123`
- **6 Categorías:** Entrantes, Ensaladas, Tandoori, Postres, Bebidas típicas nepalíes, Bebidas tradicionales
- **10 Platos de prueba** de la gastronomía nepalí con precios y descripciones completas

### 3. Ejecutar los Tests
```bash
./mvnw test
```
