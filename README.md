# MiniBanco — API Financiera

API REST para la gestión de clientes, cuentas financieras y transacciones bancarias, desarrollada como prueba técnica con Spring Boot y PostgreSQL.

---

## Stack tecnológico

| Tecnología | Versión |
|---|---|
| Java | 17 |
| Spring Boot | 3.5.14 |
| Spring Data JPA / Hibernate | — |
| Spring Validation | — |
| springdoc-openapi (Swagger) | 2.8.8 |
| PostgreSQL | 14+ |
| H2 (tests) | — |
| Lombok | — |
| JUnit 5 + Mockito | — |
| Maven | 3.x |
| Docker / Docker Compose | — |

---

## Arquitectura

El proyecto utiliza una **arquitectura en capas MVC** con separación clara de responsabilidades:

```
Controller  →  Service  →  Repository  →  Entity (JPA)
   ↕                           ↕
  DTO                      Base de datos
```

| Capa | Responsabilidad |
|---|---|
| `controller` | Recibe peticiones HTTP, valida entrada, devuelve respuestas |
| `service` | Contiene toda la lógica de negocio y validaciones |
| `repository` | Acceso a base de datos mediante Spring Data JPA |
| `entity` | Mapeo objeto-relacional de las tablas |
| `dto` | Objetos de transferencia para desacoplar la API del modelo interno |
| `exception` | Manejo centralizado de errores con `GlobalExceptionHandler` |
| `enums` | Tipos seguros para estados y categorías de negocio |

---

## Patrones de diseño utilizados

| Patrón | Dónde se aplica |
|---|---|
| **Repository** | `ClienteRepository`, `ProductoRepository`, `TransaccionRepository` — abstracción del acceso a datos |
| **DTO (Data Transfer Object)** | `ProductoRequest`, `ProductoResponse`, `TransaccionResponse` — separa la API del modelo interno |
| **Builder** | `@Builder` de Lombok en todas las entidades — construcción legible de objetos complejos |
| **Singleton** | Todos los `@Service`, `@Repository` y `@Controller` son beans Spring con scope singleton |
| **Template Method** | `@PrePersist` / `@PreUpdate` en entidades — hooks de ciclo de vida reutilizados por JPA |
| **Exception Handler** | `GlobalExceptionHandler` con `@RestControllerAdvice` — manejo centralizado de errores |

---

## Principios SOLID

| Principio | Aplicación en el proyecto |
|---|---|
| **S** — Single Responsibility | Cada clase tiene una sola razón para cambiar: `ClienteService` solo gestiona clientes, `TransaccionService` solo transacciones |
| **O** — Open/Closed | Los enums `TipoCuenta`, `EstadoCuenta`, `TipoTransaccion` permiten extender comportamiento sin modificar código existente |
| **L** — Liskov Substitution | Las interfaces `JpaRepository` son intercambiables en tests (reemplazadas por mocks de Mockito) |
| **I** — Interface Segregation | Cada `Repository` expone solo los métodos que su consumidor necesita (ej. `existsByEmail`, `findByNumeroCuenta`) |
| **D** — Dependency Inversion | Los servicios reciben sus dependencias por constructor, no las instancian directamente |

---

## Principio ACID en transacciones

Las operaciones financieras críticas están anotadas con `@Transactional`:

| Propiedad | Garantía en MiniBanco |
|---|---|
| **Atomicidad** | Si falla cualquier paso de una transferencia (débito o crédito), toda la operación se revierte |
| **Consistencia** | Las validaciones de negocio (saldo suficiente, cuenta activa) garantizan un estado válido antes y después |
| **Isolation** | Spring usa el nivel de aislamiento por defecto de PostgreSQL (READ COMMITTED), evitando lecturas sucias |
| **Durabilidad** | PostgreSQL persiste los cambios en disco; ante un fallo del servidor los datos confirmados no se pierden |

---

## Ejecución local

### Opción 1 — Con Maven (requiere PostgreSQL local)

```bash
# Crear la base de datos
psql -U postgres -c "CREATE DATABASE financial_db;"

# Levantar la aplicación
./mvnw spring-boot:run
```

### Opción 2 — Con Docker (recomendado, sin dependencias locales)

```bash
# Levantar backend + PostgreSQL en contenedores
docker-compose up --build

# Detener
docker-compose down

# Detener y borrar volúmenes de datos
docker-compose down -v
```

La API queda disponible en `http://localhost:8080`.
Swagger UI: `http://localhost:8080/swagger-ui/index.html`

---

## Estructura del proyecto

```
src/main/java/com/trinity/financial_api/
├── config/
│   ├── OpenApiConfig.java
│   └── CorsConfig.java
├── controller/
│   ├── ClienteController.java
│   ├── ProductoController.java
│   └── TransaccionController.java
├── service/
│   ├── ClienteService.java
│   ├── ProductoService.java
│   └── TransaccionService.java
├── entity/
│   ├── Cliente.java
│   ├── Producto.java
│   └── Transaccion.java
├── repository/
│   ├── ClienteRepository.java
│   ├── ProductoRepository.java
│   └── TransaccionRepository.java
├── dto/
│   ├── EstadoRequest.java
│   ├── ProductoRequest.java
│   ├── ProductoResponse.java
│   ├── TransaccionSimpleRequest.java
│   ├── TransferenciaRequest.java
│   └── TransaccionResponse.java
├── enums/
│   ├── TipoCuenta.java       (AHORRO, CORRIENTE)
│   ├── EstadoCuenta.java     (ACTIVA, INACTIVA, CANCELADA)
│   └── TipoTransaccion.java  (DEPOSITO, RETIRO, TRANSFERENCIA, DEBITO, CREDITO)
└── exception/
    ├── BusinessException.java
    ├── ErrorResponse.java
    └── GlobalExceptionHandler.java

src/main/resources/
├── sql/
│   ├── ddl.sql     ← Definición de tablas (referencia)
│   └── dml.sql     ← Datos de prueba
└── static/         ← Frontend web integrado
```

---

## Tests

```bash
./mvnw test
```

| Suite | Tipo | Tests |
|---|---|---|
| `ClienteControllerComponentTest` | Integración (H2) | 6 |
| `ProductoControllerComponentTest` | Integración (H2) | 4 |
| `TransaccionControllerComponentTest` | Integración (H2) | 3 |
| `ClienteControllerTest` | Unitario (Mockito) | 5 |
| `ProductoControllerTest` | Unitario (Mockito) | 4 |
| `TransaccionControllerTest` | Unitario (Mockito) | 3 |
| `ClienteServiceTest` | Unitario (Mockito) | 9 |
| `ProductoServiceTest` | Unitario (Mockito) | 9 |
| `TransaccionServiceTest` | Unitario (Mockito) | 4 |
| `FinancialApiApplicationTests` | Contexto Spring | 1 |

**Total: 48 tests — capas Service, Controller (unitarios + integración).**

Los tests de integración usan **H2 en memoria** y levantan el contexto completo de Spring (`@SpringBootTest + @AutoConfigureMockMvc`), validando el flujo real desde el endpoint HTTP hasta la base de datos.

---

## Endpoints

### Clientes — `/api/clientes`

| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `/api/clientes` | Crear cliente |
| `GET` | `/api/clientes/{id}` | Consultar por ID |
| `GET` | `/api/clientes/identificacion/{numero}` | Consultar por número de identificación |
| `PUT` | `/api/clientes/{id}` | Actualizar cliente |
| `DELETE` | `/api/clientes/{id}` | Eliminar cliente |

**POST /api/clientes — Request:**
```json
{
  "tipoIdentificacion": "CC",
  "numeroIdentificacion": "1099887766",
  "nombres": "Laura",
  "apellidos": "Gómez",
  "email": "laura.gomez@email.com",
  "fechaNacimiento": "1990-03-15"
}
```

---

### Productos (Cuentas) — `/api/productos`

| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `/api/productos` | Crear cuenta |
| `GET` | `/api/productos/cliente/{clienteId}` | Listar cuentas del cliente |
| `PATCH` | `/api/productos/{id}/estado` | Activar o inactivar |
| `POST` | `/api/productos/{id}/cancelar` | Cancelar cuenta |

**POST /api/productos — Request:**
```json
{
  "clienteId": 1,
  "tipoCuenta": "AHORRO",
  "exentaGMF": false
}
```

---

### Transacciones — `/api/transacciones`

| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `/api/transacciones/deposito` | Consignación |
| `POST` | `/api/transacciones/retiro` | Retiro |
| `POST` | `/api/transacciones/transferencia` | Transferencia entre cuentas |
| `GET` | `/api/transacciones/cuenta/{numeroCuenta}` | Historial de movimientos |

Una transferencia genera **dos registros** en la tabla `transacciones`: un movimiento `DEBITO` en la cuenta origen y un movimiento `CREDITO` en la cuenta destino.

---

## Reglas de negocio

| Módulo | Regla |
|---|---|
| Cliente | Debe ser mayor de 18 años |
| Cliente | Número de identificación único |
| Cliente | Email único y con formato válido |
| Cliente | Nombre y apellido mínimo 2 caracteres |
| Cliente | No se puede eliminar si tiene productos vinculados |
| Producto | Número de cuenta generado automáticamente (10 dígitos) |
| Producto | Cuenta ahorro inicia con prefijo `53`, corriente con `33` |
| Producto | Saldo inicial $0, estado inicial ACTIVA |
| Producto | Solo se cancela si el saldo es $0 |
| Transacción | La cuenta debe estar ACTIVA |
| Retiro | El saldo no puede quedar negativo |
| Transferencia | Origen y destino deben ser cuentas distintas y activas |

---

## Manejo de errores

```json
{ "mensaje": "El cliente debe ser mayor de edad (18 años o más)", "codigo": 400 }
```

| Escenario | HTTP |
|---|---|
| Regla de negocio violada | `400` |
| Campos inválidos o faltantes | `400` |
| Error interno del servidor | `500` |

---

## Frontend

El proyecto incluye un dashboard web servido directamente por Spring Boot en `http://localhost:8080`. No requiere Node.js ni build tools adicionales.

| Sección | Funcionalidad |
|---|---|
| Dashboard | Panel de bienvenida con accesos rápidos |
| Clientes | Crear, buscar, editar y eliminar clientes |
| Cuentas | Crear cuentas, gestionar estados, ver saldos |
| Operaciones | Depósito, retiro y transferencia |
| Historial | Movimientos por número de cuenta con filtros |

---

## Repositorio

[https://github.com/Darkandligh/financial-api](https://github.com/Darkandligh/financial-api)
