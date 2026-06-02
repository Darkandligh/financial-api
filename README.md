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
| PostgreSQL | 14+ |
| Lombok | - |
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

Editar `src/main/resources/application.properties` con las credenciales de tu PostgreSQL:

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

La API quedará disponible en `http://localhost:8080`.

---

## Estructura del proyecto

```
src/main/java/com/trinity/financial_api/
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
│   ├── ProductoRequest.java
│   ├── ProductoResponse.java
│   ├── TransaccionSimpleRequest.java
│   ├── TransferenciaRequest.java
│   └── TransaccionResponse.java
├── enums/
│   ├── TipoCuenta.java
│   ├── EstadoCuenta.java
│   └── TipoTransaccion.java
└── exception/
    ├── BusinessException.java
    ├── ErrorResponse.java
    └── GlobalExceptionHandler.java
```

---

## Endpoints

### Clientes — `POST /api/clientes`

Crea un nuevo cliente. El cliente debe ser mayor de 18 años. El número de identificación y el email deben ser únicos.

**Request:**
```json
{
  "tipoIdentificacion": "CC",
  "numeroIdentificacion": "1234567890",
  "nombres": "Juan",
  "apellidos": "Pérez",
  "email": "juan.perez@email.com",
  "fechaNacimiento": "1990-05-15"
}
```

**Response `201 Created`:**
```json
{
  "id": 1,
  "tipoIdentificacion": "CC",
  "numeroIdentificacion": "1234567890",
  "nombres": "Juan",
  "apellidos": "Pérez",
  "email": "juan.perez@email.com",
  "fechaNacimiento": "1990-05-15",
  "fechaCreacion": "2026-06-01T10:00:00"
}
```

---

### Productos (Cuentas) — `POST /api/productos`

Crea una cuenta bancaria vinculada a un cliente existente. El número de cuenta se genera automáticamente:
- Cuenta de **ahorro**: prefijo `53` + 8 dígitos
- Cuenta **corriente**: prefijo `33` + 8 dígitos

El saldo inicial es `0` y el estado inicial es `ACTIVA`.

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
  "numeroCuenta": "5300000001",
  "estado": "ACTIVA",
  "saldo": 0.00,
  "exentaGMF": false,
  "clienteId": 1,
  "fechaCreacion": "2026-06-01T10:05:00"
}
```

---

### Transacciones

Todos los endpoints de transacciones requieren que la cuenta esté en estado `ACTIVA`.

#### Depósito — `POST /api/transacciones/deposito`

Incrementa el saldo de la cuenta indicada.

**Request:**
```json
{
  "numeroCuenta": "5300000001",
  "monto": 500000.00
}
```

**Response `201 Created`:**
```json
{
  "id": 1,
  "tipo": "DEPOSITO",
  "monto": 500000.00,
  "fecha": "2026-06-01T10:10:00",
  "numeroCuentaOrigen": "5300000001",
  "numeroCuentaDestino": null
}
```

---

#### Retiro — `POST /api/transacciones/retiro`

Disminuye el saldo de la cuenta. Requiere saldo suficiente.

**Request:**
```json
{
  "numeroCuenta": "5300000001",
  "monto": 100000.00
}
```

**Response `201 Created`:**
```json
{
  "id": 2,
  "tipo": "RETIRO",
  "monto": 100000.00,
  "fecha": "2026-06-01T10:15:00",
  "numeroCuentaOrigen": "5300000001",
  "numeroCuentaDestino": null
}
```

---

#### Transferencia — `POST /api/transacciones/transferencia`

Mueve fondos entre dos cuentas distintas. Ambas deben estar activas y la cuenta origen debe tener saldo suficiente.

**Request:**
```json
{
  "cuentaOrigen": "5300000001",
  "cuentaDestino": "3300000002",
  "monto": 50000.00
}
```

**Response `201 Created`:**
```json
{
  "id": 3,
  "tipo": "TRANSFERENCIA",
  "monto": 50000.00,
  "fecha": "2026-06-01T10:20:00",
  "numeroCuentaOrigen": "5300000001",
  "numeroCuentaDestino": "3300000002"
}
```

---

## Reglas de negocio

| Módulo | Regla |
|---|---|
| Cliente | Debe ser mayor de 18 años |
| Cliente | Número de identificación único |
| Cliente | Email único |
| Producto | Número de cuenta generado automáticamente |
| Producto | Saldo inicial = 0 |
| Producto | Estado inicial = ACTIVA |
| Transacción | La cuenta debe estar ACTIVA |
| Retiro | El saldo no puede quedar negativo |
| Transferencia | Origen y destino no pueden ser la misma cuenta |
| Transferencia | Ambas cuentas deben estar ACTIVAS |

---

## Manejo de errores

Todos los errores retornan JSON estructurado con el siguiente formato:

```json
{
  "mensaje": "Descripción del error",
  "codigo": 400
}
```

Para errores de validación de campos:

```json
{
  "mensaje": "Error de validación en los campos enviados",
  "codigo": 400,
  "errores": {
    "monto": "El monto debe ser mayor a cero",
    "numeroCuenta": "El número de cuenta es obligatorio"
  }
}
```

| Escenario | HTTP |
|---|---|
| Error de negocio (saldo insuficiente, cuenta inactiva, etc.) | `400` |
| Campos inválidos o faltantes | `400` |
| Error interno del servidor | `500` |

---

## Flujo de prueba sugerido en Postman

1. `POST /api/clientes` — crear cliente
2. `POST /api/productos` — crear cuenta de ahorro con el `id` del cliente
3. `POST /api/productos` — crear segunda cuenta (corriente) para probar transferencias
4. `POST /api/transacciones/deposito` — depositar en la primera cuenta
5. `POST /api/transacciones/retiro` — retirar de la primera cuenta
6. `POST /api/transacciones/transferencia` — transferir entre las dos cuentas

---

## Repositorio

[https://github.com/Darkandligh/financial-api](https://github.com/Darkandligh/financial-api)
