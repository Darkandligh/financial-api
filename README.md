# Financial API - MiniBanco

API REST para la gestión de clientes, cuentas financieras y transacciones bancarias, desarrollada como prueba técnica con Spring Boot y PostgreSQL.

---

## Stack tecnológico

| Tecnología | Versión |
|---|---|
| Java | 17 |
| Spring Boot | 3.5.14 |
| Spring Data JPA | - |
| Spring Validation | - |
| springdoc-openapi (Swagger) | 2.8.8 |
| PostgreSQL | 14+ |
| Lombok | - |
| JUnit 5 + Mockito | - |
| Maven | 3.x |

---

## Requisitos previos

- Java 17 instalado
- PostgreSQL corriendo en `localhost:5432`
- Base de datos creada manualmente:

```sql
CREATE DATABASE financial_db;
```

---

## Configuración

Editar `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/financial_db
spring.datasource.username=postgres
spring.datasource.password=adminadmin
```

Las tablas se crean automáticamente al levantar la app (`ddl-auto=update`).

---

## Ejecución

```bash
./mvnw spring-boot:run
```

La API queda disponible en `http://localhost:8080`.

Documentación Swagger UI: `http://localhost:8080/swagger-ui/index.html`

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
│   └── TipoTransaccion.java  (DEPOSITO, RETIRO, TRANSFERENCIA)
└── exception/
    ├── BusinessException.java
    ├── ErrorResponse.java
    └── GlobalExceptionHandler.java
```

---

## Endpoints

### Clientes — `/api/clientes`

| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `/api/clientes` | Crear cliente |
| `GET` | `/api/clientes/{id}` | Consultar cliente por ID |
| `GET` | `/api/clientes/identificacion/{numero}` | Consultar cliente por número de identificación |
| `PUT` | `/api/clientes/{id}` | Actualizar cliente |
| `DELETE` | `/api/clientes/{id}` | Eliminar cliente |

#### POST /api/clientes — Crear cliente

**Request:**
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

**Response `201 Created`:**
```json
{
  "id": 1,
  "tipoIdentificacion": "CC",
  "numeroIdentificacion": "1099887766",
  "nombres": "Laura",
  "apellidos": "Gómez",
  "email": "laura.gomez@email.com",
  "fechaNacimiento": "1990-03-15",
  "fechaCreacion": "2026-06-01T10:00:00",
  "fechaModificacion": "2026-06-01T10:00:00"
}
```

#### PUT /api/clientes/{id} — Actualizar cliente

Mismo body que el POST. La `fechaModificacion` se recalcula automáticamente.

**Response `200 OK`:** cliente con datos actualizados.

#### DELETE /api/clientes/{id} — Eliminar cliente

**Response `204 No Content`** si no tiene productos vinculados.
**Response `400`** si el cliente tiene cuentas asociadas.

---

### Productos (Cuentas) — `/api/productos`

| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `/api/productos` | Crear cuenta |
| `GET` | `/api/productos/cliente/{clienteId}` | Listar cuentas de un cliente |
| `PATCH` | `/api/productos/{id}/estado` | Activar o inactivar cuenta |
| `POST` | `/api/productos/{id}/cancelar` | Cancelar cuenta |

#### POST /api/productos — Crear cuenta

**Request:**
```json
{
  "clienteId": 1,
  "tipoCuenta": "AHORRO",
  "exentaGMF": false
}
```

Valores válidos para `tipoCuenta`: `AHORRO`, `CORRIENTE`

**Response `201 Created`:**
```json
{
  "id": 1,
  "tipoCuenta": "AHORRO",
  "numeroCuenta": "5312345678",
  "estado": "ACTIVA",
  "saldo": 0.00,
  "exentaGMF": false,
  "clienteId": 1,
  "fechaCreacion": "2026-06-01T10:05:00"
}
```

#### GET /api/productos/cliente/{clienteId} — Listar cuentas del cliente

**Response `200 OK`:** lista de todas las cuentas del cliente (activas, inactivas y canceladas).

#### PATCH /api/productos/{id}/estado — Cambiar estado

**Request:**
```json
{ "estado": "INACTIVA" }
```

Valores válidos: `ACTIVA`, `INACTIVA`. Para cancelar usar `/cancelar`.

#### POST /api/productos/{id}/cancelar — Cancelar cuenta

Solo permitido si el saldo es `$0`. **Response `200 OK`** con estado `CANCELADA`.

---

### Transacciones — `/api/transacciones`

| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `/api/transacciones/deposito` | Consignación |
| `POST` | `/api/transacciones/retiro` | Retiro |
| `POST` | `/api/transacciones/transferencia` | Transferencia entre cuentas |
| `GET` | `/api/transacciones/cuenta/{numeroCuenta}` | Historial de transacciones |

#### POST /api/transacciones/deposito

```json
{
  "numeroCuenta": "5312345678",
  "monto": 500000.00
}
```

#### POST /api/transacciones/retiro

```json
{
  "numeroCuenta": "5312345678",
  "monto": 100000.00
}
```

#### POST /api/transacciones/transferencia

```json
{
  "cuentaOrigen": "5312345678",
  "cuentaDestino": "3387654321",
  "monto": 50000.00
}
```

#### GET /api/transacciones/cuenta/{numeroCuenta} — Historial

Retorna todas las transacciones donde la cuenta participó (como origen o destino), ordenadas de más reciente a más antigua.

**Response `200 OK`:**
```json
[
  {
    "id": 3,
    "tipo": "TRANSFERENCIA",
    "monto": 50000.00,
    "fecha": "2026-06-01T10:20:00",
    "numeroCuentaOrigen": "5312345678",
    "numeroCuentaDestino": "3387654321"
  },
  {
    "id": 1,
    "tipo": "DEPOSITO",
    "monto": 500000.00,
    "fecha": "2026-06-01T10:10:00",
    "numeroCuentaOrigen": "5312345678",
    "numeroCuentaDestino": null
  }
]
```

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
| Producto | Saldo inicial = $0, estado inicial = ACTIVA |
| Producto | Solo se cancela si el saldo es $0 |
| Producto | No se puede cambiar estado de una cuenta cancelada |
| Transacción | La cuenta debe estar ACTIVA para operar |
| Retiro | El saldo no puede quedar negativo |
| Transferencia | Origen y destino deben ser cuentas distintas y activas |

---

## Manejo de errores

**Error de negocio:**
```json
{
  "mensaje": "No se puede eliminar el cliente porque tiene productos vinculados",
  "codigo": 400
}
```

**Error de validación:**
```json
{
  "mensaje": "Error de validación en los campos enviados",
  "codigo": 400,
  "errores": {
    "email": "El formato del email no es válido",
    "nombres": "Los nombres deben tener entre 2 y 100 caracteres"
  }
}
```

| Escenario | HTTP |
|---|---|
| Regla de negocio violada | `400` |
| Campos inválidos o faltantes | `400` |
| Error interno del servidor | `500` |

---

## Tests unitarios

```bash
./mvnw test
```

| Suite | Capa | Tests |
|---|---|---|
| `TransaccionServiceTest` | Service | 3 |
| `ClienteControllerTest` | Controller | 5 |
| `ProductoControllerTest` | Controller | 4 |
| `TransaccionControllerTest` | Controller | 3 |

**Total: 16 tests — cobertura en capas Service y Controller.**

---

## Colección Postman

El archivo `MiniBanco.postman_collection.json` en la raíz del proyecto contiene 27 requests listos para importar, cubriendo todos los escenarios exitosos y de error.

**Importar:** Postman → Import → seleccionar el archivo.

---

## Flujo de prueba sugerido

1. `POST /api/clientes` — crear cliente
2. `POST /api/productos` — crear cuenta ahorro
3. `POST /api/productos` — crear cuenta corriente
4. `POST /api/transacciones/deposito` — depositar en cuenta ahorro
5. `POST /api/transacciones/retiro` — retirar de cuenta ahorro
6. `POST /api/transacciones/transferencia` — transferir entre cuentas
7. `GET /api/transacciones/cuenta/{numeroCuenta}` — ver historial
8. `GET /api/productos/cliente/{id}` — ver estado de todas las cuentas

---

## Frontend

El proyecto incluye un dashboard web servido directamente por Spring Boot. No requiere Node.js, npm ni ningún build tool.

### Archivos

| Archivo | Descripción |
|---|---|
| `src/main/resources/static/index.html` | Estructura HTML con sidebar y 5 secciones |
| `src/main/resources/static/styles.css` | Diseño dark fintech con glassmorphism y animaciones |
| `src/main/resources/static/app.js` | Lógica completa: fetch, toasts, estados de carga |

### Cómo usarlo

1. Levanta el backend: `./mvnw spring-boot:run`
2. Abre `http://localhost:8080` en el navegador
3. El indicador de estado en el sidebar confirma la conexión con la API

### Secciones del dashboard

| Sección | Funcionalidad |
|---|---|
| **Dashboard** | Panel de bienvenida con accesos rápidos |
| **Clientes** | Crear cliente, buscar por ID, eliminar |
| **Cuentas** | Crear cuenta, listar por cliente, activar / inactivar / cancelar |
| **Operaciones** | Depósito, retiro y transferencia con resultado visual |
| **Historial** | Tabla de movimientos por número de cuenta |

> El backend tiene CORS habilitado para permitir peticiones desde el navegador (`CorsConfig.java`).

---

## Repositorio

[https://github.com/Darkandligh/financial-api](https://github.com/Darkandligh/financial-api)
