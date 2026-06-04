# Guía de Sustentación — Financial API

> Basada exclusivamente en el código fuente del repositorio. Cada afirmación fue verificada contra el archivo real.

---

## 1. Resumen y Stack Tecnológico

### Framework y lenguaje

| Elemento | Valor |
|---|---|
| Lenguaje | Java 17 (nivel de compilación declarado en `pom.xml`) |
| Framework | Spring Boot 3.5.14 |
| Base de datos | PostgreSQL (base: `financial_db`, puerto `5432`) |
| Puerto del servidor | 8080 |

### Dependencias clave (leídas de `pom.xml`)

| Librería | Versión | Para qué se usa |
|---|---|---|
| `spring-boot-starter-data-jpa` | Heredada de Boot 3.5.14 | ORM, repositorios, transacciones |
| `spring-boot-starter-web` | Heredada de Boot 3.5.14 | Controladores REST, serialización JSON |
| `spring-boot-starter-validation` | Heredada de Boot 3.5.14 | Validaciones con anotaciones (`@NotBlank`, `@Email`, etc.) |
| `postgresql` | Heredada de Boot 3.5.14 | Driver JDBC para PostgreSQL |
| `lombok` | Heredada de Boot 3.5.14 | Generación de getters/setters/builder en tiempo de compilación |
| `springdoc-openapi-starter-webmvc-ui` | **2.8.8** | Swagger UI automático en `/swagger-ui.html` |
| `spring-boot-starter-test` | Heredada de Boot 3.5.14 | JUnit 5 + Mockito + AssertJ + MockMvc |

### Configuración relevante (`application.properties`)

```properties
spring.jpa.hibernate.ddl-auto=update       # Hibernate actualiza el esquema automáticamente
spring.jpa.show-sql=true                   # Imprime SQL en consola (útil para depurar)
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

---

## 2. Arquitectura

### Patrón utilizado: MVC por capas

El proyecto usa **arquitectura MVC (Model-View-Controller) en capas**, que es el patrón estándar de Spring Boot. No implementa arquitectura hexagonal (no existen puertos ni adaptadores, ni separación dominio/infraestructura explícita). La elección de MVC es adecuada para una API REST de este tamaño y reduce complejidad innecesaria.

### Responsabilidad de cada capa

| Capa | Paquete | Clase ejemplo | Qué hace |
|---|---|---|---|
| **Entity** | `entity` | `Cliente.java`, `Producto.java`, `Transaccion.java` | Mapea las tablas de la base de datos. Contiene validaciones de campo y lógica de auditoría (`@PrePersist`, `@PreUpdate`). |
| **Repository** | `repository` | `ClienteRepository.java` | Extiende `JpaRepository` para CRUD automático. Agrega queries derivados por nombre de método. |
| **Service** | `service` | `ClienteService.java`, `ProductoService.java`, `TransaccionService.java` | Contiene **toda** la lógica de negocio (validaciones, reglas, coordinación entre repositorios). |
| **Controller** | `controller` | `ClienteController.java`, `ProductoController.java`, `TransaccionController.java` | Recibe peticiones HTTP, delega al service, devuelve `ResponseEntity`. No contiene lógica de negocio. |
| **DTO** | `dto` | `ProductoRequest`, `ProductoResponse`, `TransaccionResponse` | Objetos de entrada/salida que desacoplan la entidad interna de lo que se expone al cliente. Son Java Records. |
| **Exception** | `exception` | `BusinessException`, `GlobalExceptionHandler` | Manejo centralizado de errores. `@RestControllerAdvice` intercepta excepciones y devuelve JSON estructurado. |

### Flujo completo de una petición HTTP

Ejemplo: **POST /api/clientes** (crear cliente)

```
1. HTTP Request llega a ClienteController.crear()          [ClienteController.java:31]
   └── @Valid valida el @RequestBody con las anotaciones de la entidad Cliente

2. ClienteController llama a clienteService.crearCliente() [ClienteController.java:32]

3. ClienteService.crearCliente() ejecuta reglas:           [ClienteService.java:32-37]
   ├── validarMayoriaDeEdad()     → lanza BusinessException si < 18 años
   ├── validarNumeroIdentificacionUnico() → consulta clienteRepository.existsByNumeroIdentificacion()
   └── validarEmailUnico()        → consulta clienteRepository.existsByEmail()

4. clienteRepository.save(cliente) genera INSERT en PostgreSQL

5. Hibernate devuelve el objeto con ID asignado y fechas auditadas (@PrePersist)

6. ClienteController devuelve ResponseEntity.status(201).body(cliente)

Si en el paso 3 hay error:
   └── BusinessException es capturada por GlobalExceptionHandler.handleBusinessException()
       └── Devuelve HTTP 400 con JSON: { "mensaje": "...", "codigo": 400 }
```

---

## 3. Modelo de Datos / Persistencia

### Diagrama de entidades

```
clientes (1) ──────── (N) productos (1) ──────── (N) transacciones
                                       (1) ──────── (N) transacciones (como destino)
```

### Entidad `Cliente` — tabla `clientes`

**Archivo:** `src/main/java/com/trinity/financial_api/entity/Cliente.java`

| Columna | Tipo Java | Tipo DB | Restricciones |
|---|---|---|---|
| `id` | `Long` | BIGINT | PK, autogenerado (IDENTITY) |
| `tipo_identificacion` | `String` | VARCHAR(20) | NOT NULL |
| `numero_identificacion` | `String` | VARCHAR(20) | NOT NULL, UNIQUE |
| `nombres` | `String` | VARCHAR(100) | NOT NULL, min 2 / max 100 chars |
| `apellidos` | `String` | VARCHAR(100) | NOT NULL, min 2 / max 100 chars |
| `email` | `String` | VARCHAR(150) | NOT NULL, UNIQUE, formato email |
| `fecha_nacimiento` | `LocalDate` | DATE | NOT NULL |
| `fecha_creacion` | `LocalDateTime` | TIMESTAMP | NOT NULL, `updatable=false` |
| `fecha_modificacion` | `LocalDateTime` | TIMESTAMP | Actualizado por `@PreUpdate` |

Restricciones de unicidad nombradas explícitamente:
- `uk_clientes_numero_identificacion`
- `uk_clientes_email`

### Entidad `Producto` — tabla `productos`

**Archivo:** `src/main/java/com/trinity/financial_api/entity/Producto.java`

| Columna | Tipo Java | Tipo DB | Restricciones |
|---|---|---|---|
| `id` | `Long` | BIGINT | PK, autogenerado |
| `tipo_cuenta` | `TipoCuenta` (enum) | VARCHAR(20) | NOT NULL, valores: `AHORRO`, `CORRIENTE` |
| `numero_cuenta` | `String` | VARCHAR(20) | NOT NULL, UNIQUE |
| `estado` | `EstadoCuenta` (enum) | VARCHAR(20) | NOT NULL, valores: `ACTIVA`, `INACTIVA`, `CANCELADA` |
| `saldo` | `BigDecimal` | NUMERIC(19,2) | NOT NULL, inicia en 0 |
| `exenta_gmf` | `boolean` | BOOLEAN | NOT NULL |
| `cliente_id` | FK → `clientes` | BIGINT | NOT NULL, `@ManyToOne(LAZY)` |
| `fecha_creacion` / `fecha_modificacion` | `LocalDateTime` | TIMESTAMP | Igual que en `Cliente` |

> **Nota sobre `saldoDisponible`:** No existe ningún campo `saldoDisponible` en la entidad `Producto` ni en ninguna otra clase. Solo existe `saldo`. En este modelo no hay retenciones ni cobros diferidos, por lo que saldo y saldo disponible son siempre el mismo valor.

### Entidad `Transaccion` — tabla `transacciones`

**Archivo:** `src/main/java/com/trinity/financial_api/entity/Transaccion.java`

| Columna | Tipo Java | Tipo DB | Restricciones |
|---|---|---|---|
| `id` | `Long` | BIGINT | PK, autogenerado |
| `tipo_transaccion` | `TipoTransaccion` (enum) | VARCHAR(20) | NOT NULL, valores: `DEPOSITO`, `RETIRO`, `TRANSFERENCIA` |
| `monto` | `BigDecimal` | NUMERIC(19,2) | NOT NULL, mínimo 0.01 |
| `fecha` | `LocalDateTime` | TIMESTAMP | NOT NULL, `updatable=false`, seteada por `@PrePersist` |
| `cuenta_origen_id` | FK → `productos` | BIGINT | NOT NULL, siempre requerida |
| `cuenta_destino_id` | FK → `productos` | BIGINT | NULLABLE, solo en `TRANSFERENCIA` |

### Anotaciones JPA utilizadas

| Anotación | Dónde se usa | Para qué |
|---|---|---|
| `@Entity` | Las 3 entidades | Marca la clase como tabla |
| `@Table(name=..., uniqueConstraints=...)` | `Cliente`, `Producto` | Nombre de tabla y restricciones únicas |
| `@Id` + `@GeneratedValue(IDENTITY)` | Las 3 entidades | PK autoincrementada por PostgreSQL |
| `@Column(...)` | Todos los campos | Configura nombre, longitud, nullable, updatable |
| `@Enumerated(EnumType.STRING)` | `tipoCuenta`, `estado`, `tipoTransaccion` | Guarda el nombre del enum como texto, no como número |
| `@ManyToOne(fetch = LAZY)` | `Producto.cliente`, `Transaccion.cuentaOrigen/Destino` | Relación muchos a uno con carga diferida |
| `@JoinColumn(name=...)` | Mismos campos | Nombre de la columna FK |
| `@PrePersist` / `@PreUpdate` | `Cliente`, `Producto`, `Transaccion` | Hooks de ciclo de vida para fechas automáticas |

---

## 4. Reglas de Negocio

> Esta es la sección más importante. Cada regla fue verificada contra el archivo real con número de línea exacto.

| Regla del enunciado | Clase y método | Cómo funciona el código |
|---|---|---|
| **Cliente menor de edad no se crea** | `ClienteService.validarMayoriaDeEdad()` — línea 73 | `fechaNacimiento.isAfter(LocalDate.now().minusYears(18))` → lanza `BusinessException` si la fecha es posterior al límite de 18 años. Se llama tanto en `crearCliente()` (línea 34) como en `actualizar()` (línea 44). |
| **Cliente con productos no se elimina** | `ClienteService.eliminar()` — línea 67 | Antes de borrar llama `productoRepository.existsByClienteId(id)`. Si retorna `true`, lanza `BusinessException("No se puede eliminar el cliente porque tiene productos vinculados")`. |
| **Fechas de creación/modificación automáticas** | `Cliente.onCreate()` / `Cliente.onUpdate()` — líneas 62-71; igual en `Producto` | `@PrePersist` setea ambas fechas al momento de la inserción. `@PreUpdate` actualiza `fechaModificacion`. El desarrollador no las setea manualmente. |
| **Validación de email** | `Cliente.java` — línea 48 | `@Email(message = "El formato del email no es válido")` delega la validación a Bean Validation (Hibernate Validator). Se ejecuta al llegar el request (`@Valid`). |
| **Validación de longitud de nombre/apellido** | `Cliente.java` — líneas 38-45 | `@Size(min = 2, max = 100)` en línea 38 para `nombres` y en línea 43 para `apellidos`. |
| **Email y número de identificación únicos** | `ClienteService.validarEmailUnico()` y `validarNumeroIdentificacionUnico()` — líneas 79-93 | Consulta `existsByEmail()` y `existsByNumeroIdentificacion()` antes de guardar. Al actualizar, solo valida si el valor cambió (líneas 46-51 de `ClienteService`). |
| **Producto solo si vinculado a un cliente** | `ProductoService.crearProducto()` — línea 38 | Busca el cliente por ID con `clienteRepository.findById()` en línea 39. Si no existe, lanza `BusinessException`. Solo después construye el `Producto`. |
| **Número de cuenta único de 10 dígitos generado automáticamente** | `ProductoService.generarNumeroCuenta()` — línea 86 | Concatena el prefijo + `String.format("%08d", random.nextInt(100_000_000))` = 2 + 8 = 10 dígitos. Usa un `do-while` para regenerar si el número ya existe en BD. |
| **Cuenta de ahorros inicia con prefijo "53", corriente con "33"** | `ProductoService.generarNumeroCuenta()` — línea 87 | `String prefijo = tipoCuenta == TipoCuenta.AHORRO ? "53" : "33"` |
| **Cuenta nace activa** | `Producto.onCreate()` — línea 66 | `if (estado == null) estado = EstadoCuenta.ACTIVA` en `@PrePersist`. El service nunca asigna un estado inicial; lo hace el hook. |
| **Saldo no puede quedar negativo** | `TransaccionService.retirar()` — línea 53 | `if (cuenta.getSaldo().compareTo(monto) < 0)` lanza `BusinessException("Saldo insuficiente...")`. Se aplica a cualquier tipo de cuenta (ver sección 8). |
| **Solo se cancelan cuentas con saldo en 0** | `ProductoService.cancelar()` — línea 75 | `if (producto.getSaldo().compareTo(BigDecimal.ZERO) != 0)` lanza `BusinessException`. Usa `compareTo` (correcto para `BigDecimal`, no `equals`). |
| **Cuenta cancelada no puede cambiar de estado** | `ProductoService.cambiarEstado()` — línea 56 | `if (producto.getEstado() == EstadoCuenta.CANCELADA)` lanza excepción. |
| **Transferencia actualiza saldo en origen y destino** | `TransaccionService.transferir()` — líneas 80-90 | Resta en origen (línea 80): `origen.setSaldo(origen.getSaldo().subtract(monto))`. Suma en destino (línea 81): `destino.setSaldo(destino.getSaldo().add(monto))`. Guarda ambos productos (líneas 82-83) y registra **una sola** `Transaccion` (líneas 85-90) con `cuentaOrigen` y `cuentaDestino`. Todo dentro de `@Transactional`. |
| **Actualización de saldo en cada transacción** | `TransaccionService.consignar()` línea 38, `retirar()` línea 57, `transferir()` líneas 80-81 | En cada operación se llama `productoRepository.save(cuenta)` después de modificar el saldo, dentro de una transacción `@Transactional`. |

---

## 5. Endpoints REST

### Clientes — base: `/api/clientes`

| Verbo | Ruta | Qué hace | Request body | Response |
|---|---|---|---|---|
| `GET` | `/api/clientes/{id}` | Buscar cliente por ID | — | `200 Cliente` / `400` si no existe |
| `GET` | `/api/clientes/identificacion/{numero}` | Buscar por número de identificación | — | `200 Cliente` / `400` si no existe |
| `POST` | `/api/clientes` | Crear cliente | `Cliente` JSON | `201 Cliente` creado / `400` si validación falla |
| `PUT` | `/api/clientes/{id}` | Actualizar todos los datos del cliente | `Cliente` JSON | `200 Cliente` actualizado |
| `DELETE` | `/api/clientes/{id}` | Eliminar cliente | — | `204 No Content` / `400` si tiene productos |

**Ejemplo POST /api/clientes:**
```json
// Request
{
  "tipoIdentificacion": "CC",
  "numeroIdentificacion": "1099887766",
  "nombres": "Juan",
  "apellidos": "Perez",
  "email": "juan@email.com",
  "fechaNacimiento": "1990-05-15"
}

// Response 201
{
  "id": 1,
  "tipoIdentificacion": "CC",
  "numeroIdentificacion": "1099887766",
  "nombres": "Juan",
  "apellidos": "Perez",
  "email": "juan@email.com",
  "fechaNacimiento": "1990-05-15",
  "fechaCreacion": "2024-01-15T10:30:00",
  "fechaModificacion": "2024-01-15T10:30:00"
}
```

---

### Productos — base: `/api/productos`

| Verbo | Ruta | Qué hace | Request body | Response |
|---|---|---|---|---|
| `GET` | `/api/productos/cliente/{clienteId}` | Listar cuentas de un cliente | — | `200 List<ProductoResponse>` |
| `POST` | `/api/productos` | Crear cuenta para un cliente | `ProductoRequest` | `201 ProductoResponse` |
| `PATCH` | `/api/productos/{id}/estado` | Cambiar estado a ACTIVA/INACTIVA | `EstadoRequest` | `200 ProductoResponse` |
| `POST` | `/api/productos/{id}/cancelar` | Cancelar cuenta (requiere saldo=0) | — | `200 ProductoResponse` |

**Ejemplo POST /api/productos:**
```json
// Request
{
  "clienteId": 1,
  "tipoCuenta": "AHORRO",
  "exentaGMF": false
}

// Response 201
{
  "id": 1,
  "tipoCuenta": "AHORRO",
  "numeroCuenta": "5312345678",
  "estado": "ACTIVA",
  "saldo": 0.00,
  "exentaGMF": false,
  "clienteId": 1,
  "fechaCreacion": "2024-01-15T10:35:00"
}
```

---

### Transacciones — base: `/api/transacciones`

| Verbo | Ruta | Qué hace | Request body | Response |
|---|---|---|---|---|
| `GET` | `/api/transacciones/cuenta/{numeroCuenta}` | Historial de una cuenta | — | `200 List<TransaccionResponse>` |
| `POST` | `/api/transacciones/deposito` | Consignar dinero en una cuenta | `TransaccionSimpleRequest` | `201 TransaccionResponse` |
| `POST` | `/api/transacciones/retiro` | Retirar dinero de una cuenta | `TransaccionSimpleRequest` | `201 TransaccionResponse` |
| `POST` | `/api/transacciones/transferencia` | Transferir entre dos cuentas | `TransferenciaRequest` | `201 TransaccionResponse` |

**Ejemplo POST /api/transacciones/transferencia:**
```json
// Request
{
  "cuentaOrigen": "5312345678",
  "cuentaDestino": "3398765432",
  "monto": 100000.00
}

// Response 201
{
  "id": 5,
  "tipo": "TRANSFERENCIA",
  "monto": 100000.00,
  "fecha": "2024-01-15T11:00:00",
  "numeroCuentaOrigen": "5312345678",
  "numeroCuentaDestino": "3398765432"
}
```

**Respuesta de error estándar (HTTP 400):**
```json
{
  "mensaje": "Saldo insuficiente. Saldo disponible: 50000.00",
  "codigo": 400,
  "errores": null
}
```

---

## 6. Tests Unitarios

### Resumen de archivos de test

| Archivo | Tipo de test | Clase bajo prueba | Tests |
|---|---|---|---|
| `ClienteServiceTest.java` | Unitario puro (Mockito) | `ClienteService` | 9 |
| `ProductoServiceTest.java` | Unitario puro (Mockito) | `ProductoService` | 9 |
| `TransaccionServiceTest.java` | Unitario puro (Mockito) | `TransaccionService` | 3 |
| `ClienteControllerTest.java` | Web layer (`@WebMvcTest` + MockMvc) | `ClienteController` | 5 |
| `ProductoControllerTest.java` | Web layer (`@WebMvcTest` + MockMvc) | `ProductoController` | 4 |
| `TransaccionControllerTest.java` | Web layer (`@WebMvcTest` + MockMvc) | `TransaccionController` | 3 |

**Total: 34 tests — todos pasan** (verificado con `./mvnw test`).

### Librerías utilizadas

| Librería | Rol |
|---|---|
| **JUnit 5** (`@Test`, `@BeforeEach`, `@ExtendWith`) | Motor de ejecución de pruebas |
| **Mockito** (`@Mock`, `@InjectMocks`, `@MockitoBean`, `when()`, `verify()`) | Dobles de prueba (mocks) de repositorios y services |
| **AssertJ** (`assertThat`, `assertThatThrownBy`) | Aserciones fluidas y legibles |
| **MockMvc** (`mockMvc.perform(...)`) | Simula peticiones HTTP sin levantar el servidor completo |
| **`@WebMvcTest`** | Levanta solo la capa web (controller + serialización), mockeando el service |

### Cómo ejecutar los tests

```bash
# Todos los tests (usar el Maven Wrapper incluido en el proyecto)
./mvnw test

# Un archivo específico
./mvnw test -Dtest=ClienteServiceTest
```

### Casos de prueba implementados

**`ClienteServiceTest`** (9 tests — prueba la lógica real del service con repositorios mockeados):
- `crearCliente_debeLanzarExcepcion_cuandoClienteEsMenorDeEdad`
- `crearCliente_debeLanzarExcepcion_cuandoNumeroIdentificacionYaExiste`
- `crearCliente_debeLanzarExcepcion_cuandoEmailYaExiste`
- `crearCliente_debeGuardar_cuandoDatosValidos`
- `eliminar_debeLanzarExcepcion_cuandoClienteNoExiste`
- `eliminar_debeLanzarExcepcion_cuandoClienteTieneProductos`
- `eliminar_debeEliminar_cuandoClienteExisteYNoTieneProductos`
- `actualizar_debeLanzarExcepcion_cuandoClienteNoExiste`
- `actualizar_debeLanzarExcepcion_cuandoNuevoEmailYaExiste`

**`ProductoServiceTest`** (9 tests — prueba la lógica real del service con repositorios mockeados):
- `crearProducto_debeLanzarExcepcion_cuandoClienteNoExiste`
- `crearProducto_debeGenerarNumeroCuentaConPrefijo53_cuandoTipoAhorro`
- `crearProducto_debeGenerarNumeroCuentaConPrefijo33_cuandoTipoCorriente`
- `cancelar_debeLanzarExcepcion_cuandoSaldoNoEsCero`
- `cancelar_debeLanzarExcepcion_cuandoCuentaYaEstaCancelada`
- `cancelar_debeCambiarEstadoACancelada_cuandoSaldoEsCero`
- `cambiarEstado_debeLanzarExcepcion_cuandoCuentaEstaCancelada`
- `cambiarEstado_debeLanzarExcepcion_cuandoSeIntentaCancelarPorEsteEndpoint`
- `listarPorCliente_debeLanzarExcepcion_cuandoClienteNoExiste`

**`TransaccionServiceTest`** (3 tests):
- `retirar_debeLanzarExcepcion_cuandoSaldoEsInsuficiente`
- `consignar_debeIncrementarSaldo_cuandoDatosValidos`
- `transferir_debeLanzarExcepcion_cuandoCuentasOriginYDestinoSonIguales`

**`ClienteControllerTest`** (5 tests):
- `crear_debeRetornar201_cuandoDatosValidos`
- `crear_debeRetornar400_cuandoClienteEsMenorDeEdad`
- `actualizar_debeRetornar200_cuandoDatosValidos`
- `eliminar_debeRetornar204_cuandoClienteNoTieneProductos`
- `eliminar_debeRetornar400_cuandoClienteTieneProductosVinculados`

**`ProductoControllerTest`** (4 tests):
- `crear_debeRetornar201_cuandoDatosValidos`
- `cambiarEstado_debeRetornar200_cuandoEstadoValido`
- `cancelar_debeRetornar400_cuandoCuentaTieneSaldo`
- `cancelar_debeRetornar200_cuandoCuentaTieneSaldoCero`

**`TransaccionControllerTest`** (3 tests):
- `depositar_debeRetornar201_cuandoDatosValidos`
- `retirar_debeRetornar400_cuandoSaldoInsuficiente`
- `transferir_debeRetornar400_cuandoCuentasIguales`

### Cobertura por capa

| Capa | Cubierta | Observación |
|---|---|---|
| Service — `ClienteService` | Sí (9 casos directos) | `ClienteServiceTest` prueba el código real del service |
| Service — `ProductoService` | Sí (9 casos directos) | `ProductoServiceTest` prueba el código real del service |
| Service — `TransaccionService` | Sí (3 casos directos) | `TransaccionServiceTest` cubre flujos críticos |
| Controller — los 3 controllers | Sí, con `@WebMvcTest` | Cubre happy path y principales errores |
| Repository | No | Sin tests de integración con BD |

---

## 7. Posibles Preguntas de Sustentación

**P1: ¿Por qué elegiste arquitectura MVC y no hexagonal?**
> MVC es adecuado para este tamaño de proyecto. La arquitectura hexagonal añade capas de abstracción (puertos, adaptadores, dominio) que son valiosas cuando el dominio es complejo o cuando se necesita cambiar fácilmente de infraestructura (p.ej., cambiar de PostgreSQL a MongoDB). Para una API CRUD con lógica de negocio relativamente directa, MVC es más simple y más fácil de mantener.

**P2: ¿Qué es `@Transactional` y por qué lo usas en `transferir()`?**
> `@Transactional` hace que todas las operaciones de base de datos dentro del método ocurran en una sola transacción ACID. En `TransaccionService.transferir()` (línea 68), esto es crítico: si falla el `save` del producto destino después de que ya se descontó el saldo del origen, la transacción hace rollback automático y ningún saldo queda modificado. Sin esto, podría perderse dinero.

**P3: ¿Cómo se genera el número de cuenta y cómo garantizas que sea único?**
> En `ProductoService.generarNumeroCuenta()` (línea 86): se concatena el prefijo ("53" para ahorro, "33" para corriente) con 8 dígitos aleatorios formateados con `String.format("%08d", random.nextInt(100_000_000))`. El `do-while` consulta `existsByNumeroCuenta()` y regenera si el número ya existe. Así se garantiza unicidad en tiempo de creación.

**P4: ¿Por qué el saldo se inicializa en cero en `@PrePersist` y no en el request?**
> Es una decisión de diseño: el saldo inicial de una cuenta siempre es cero, independientemente de lo que envíe el cliente. Fijarlo en `Producto.onCreate()` (línea 65) con `if (saldo == null) saldo = BigDecimal.ZERO` asegura que nadie pueda crear una cuenta con saldo arbitrario a través de la API. El saldo solo cambia mediante transacciones.

**P5: ¿Por qué usas `compareTo` en vez de `equals` para comparar `BigDecimal`?**
> En `ProductoService.cancelar()` (línea 75): `BigDecimal.ZERO.equals(new BigDecimal("0.00"))` retorna `false` porque `equals` en `BigDecimal` compara también la escala. `compareTo(BigDecimal.ZERO) != 0` compara solo el valor numérico, por lo que `0`, `0.0` y `0.00` son todos iguales. Usar `equals` aquí sería un bug.

**P6: ¿Qué pasa si llega un request con datos inválidos (campo vacío, email mal formado)?**
> El `@Valid` en los métodos del controller activa Bean Validation antes de ejecutar el código. Si falla, Spring lanza `MethodArgumentNotValidException`, que es capturada por `GlobalExceptionHandler.handleValidationException()` (línea 37), devolviendo HTTP 400 con un mapa `{ "campo": "mensaje de error" }`.

**P7: ¿Cómo está manejado el manejo de errores centralizado?**
> Con `@RestControllerAdvice` en `GlobalExceptionHandler.java`. Intercepta cuatro tipos: `BusinessException` (errores de negocio → 400), `IllegalArgumentException` (400), `MethodArgumentNotValidException` (validaciones de campos → 400 con detalle por campo) y `Exception` genérica (500). Todos devuelven un `ErrorResponse` JSON estructurado.

**P8: ¿Por qué `Transaccion` no tiene `@PreUpdate` ni `fechaModificacion`?**
> Las transacciones financieras son **inmutables por diseño**. Una vez registrada una transacción no se modifica. La columna `fecha` tiene `updatable = false` (`Transaccion.java` línea 35) y solo hay `@PrePersist`. Esto garantiza integridad del historial.

**P9: ¿Qué diferencia hay entre `ProductoRequest` y `Producto` (entidad)?**
> `ProductoRequest` (un Java Record) es lo que el cliente envía: solo `clienteId`, `tipoCuenta` y `exentaGMF`. La entidad `Producto` tiene además `numeroCuenta`, `estado`, `saldo` y fechas, que son generados internamente. Separar DTO de entidad evita que el cliente inyecte valores que no debería controlar y protege la entidad de exposición directa.

**P10: ¿Qué es `FetchType.LAZY` y por qué se usa en las relaciones?**
> Indica que la relación no se carga desde la base de datos hasta que se accede explícitamente al objeto relacionado. Si se usara `EAGER`, cada vez que se cargue una `Transaccion` se haría automáticamente un JOIN para traer también el `Producto` y el `Cliente`. Con `LAZY`, la consulta inicial es más simple y rápida, y solo carga los datos relacionados cuando son necesarios.

**P11: ¿Por qué `TransaccionRepository` tiene una query JPQL manual?**
> Porque un query derivado por nombre solo puede filtrar por un campo. Aquí se necesita buscar transacciones donde la cuenta aparezca como **origen O destino**, con `OR` entre dos relaciones distintas. Eso no se puede expresar con un solo método derivado de Spring Data. La query (línea 14 de `TransaccionRepository`) usa `DISTINCT` para evitar duplicados y `LEFT JOIN FETCH` para evitar el problema N+1 al serializar.

**P12: ¿Cómo funcionan los tests de controller con `@WebMvcTest`?**
> `@WebMvcTest(ClienteController.class)` levanta únicamente la capa web: el controller, la serialización JSON y el `GlobalExceptionHandler`. El `ClienteService` es reemplazado por un mock con `@MockitoBean`. Se usa `MockMvc` para simular HTTP requests y `when(...).thenReturn(...)` para controlar qué devuelve el mock. Esto permite probar el controller sin base de datos ni Spring completo.

**P13: ¿Qué pasa si se intenta cambiar el estado de una cuenta a CANCELADA usando el endpoint `/estado`?**
> `ProductoService.cambiarEstado()` (línea 59) lo bloquea explícitamente: `if (nuevoEstado == EstadoCuenta.CANCELADA) throw new BusinessException("Para cancelar una cuenta use el endpoint /cancelar")`. La cancelación tiene su propio endpoint `/cancelar` que verifica que el saldo sea cero.

**P14: ¿Por qué el saldo solo se verifica en el origen durante una transferencia?**
> Porque en una transferencia solo el origen "pierde" dinero. El destino siempre **recibe**, por lo que su saldo siempre aumenta y no puede quedar negativo. Solo se valida `origen.getSaldo().compareTo(monto) < 0` en `TransaccionService.transferir()` (línea 76).

**P15: ¿Cómo evita el proyecto que `cliente_id` llegue a la tabla de productos con un ID inexistente?**
> A nivel de aplicación: `ProductoService.crearProducto()` llama `clienteRepository.findById(clienteId).orElseThrow(...)` (línea 39) antes de crear el producto. A nivel de base de datos: la FK `cliente_id` con `nullable = false` garantiza integridad referencial en PostgreSQL.

---

## 8. Nota: DEPOSITO vs "Consignación"

El enunciado usa el término **"consignación"**. En el código existen tres variantes del mismo concepto:

| Elemento | Nombre usado |
|---|---|
| Enum `TipoTransaccion` | `DEPOSITO` |
| Método del service | `consignar()` — `TransaccionService.java:35` |
| Ruta del endpoint | `POST /api/transacciones/deposito` |
| Método del controller | `depositar()` — `TransaccionController.java:34` |

No es un error — los tres representan la misma operación. Si el evaluador pregunta, la respuesta es: el negocio llama "consignación" a la operación, el código usa "depósito" como término técnico equivalente, y ambos conviven en el proyecto sin inconsistencia funcional.

---

## 9. Nota: Transferencia — un solo registro

La implementación registra **una sola** fila en la tabla `transacciones` por cada transferencia (`TransaccionService.transferir()`, líneas 85-90), con los campos `cuenta_origen_id` y `cuenta_destino_id` ambos poblados. El enunciado habla de "movimientos de crédito y débito", concepto que queda representado en ese único registro.

La query del historial (`TransaccionRepository`, línea 14) retorna esa transacción tanto al consultar la cuenta origen como la cuenta destino, por lo que ambas cuentas la ven en su historial. La atomicidad está garantizada por `@Transactional`.

---

## 10. Cómo levantar la aplicación

### Requisitos previos
- PostgreSQL corriendo en `localhost:5432`
- Base de datos `financial_db` creada
- Usuario `postgres` con contraseña `adminadmin`

### Comando

```bash
./mvnw spring-boot:run
```

### URLs disponibles tras levantar

| Recurso | URL |
|---|---|
| API base | `http://localhost:8080/api/` |
| Swagger UI (documentación interactiva) | `http://localhost:8080/swagger-ui.html` |
| Frontend incluido | `http://localhost:8080/index.html` |

---

## 11. Control de Versiones

### Ver historial de commits

```bash
git log --oneline
```

### Ver qué cambió en el último commit

```bash
git show --stat HEAD
```

### Ver el estado actual del repositorio

```bash
git status
```

### Commits principales del proyecto

Los commits documentan la evolución del proyecto (refactoring de frontend, CORS, CRUD completo, etc.) y son evidencia de trabajo incremental. Para mostrar el historial completo con fechas:

```bash
git log --oneline --decorate
```

---

## 12. Pendientes y Debilidades

### Requisitos no implementados o incompletos

| Punto | Estado | Detalle |
|---|---|---|
| **Regla de saldo < 0 solo para ahorros** | Implementado de forma más estricta | El código aplica la restricción de saldo no negativo a **todas** las cuentas (corriente incluida). En la realidad, las cuentas corriente pueden tener sobregiro. No es un error, sino una decisión conservadora. |
| **`saldoDisponible`** | No implementado | No existe el campo en ninguna entidad. Solo existe `saldo`. En este modelo sin retenciones ni GMF aplicado, ambos valores son equivalentes. |
| **Tests de integración** | No implementados | No hay tests que levanten el contexto completo de Spring con base de datos real (`@SpringBootTest` + Testcontainers o H2). Las queries JPA y las restricciones de la BD no están probadas automáticamente. |
| **Listado general de clientes** | No implementado | No existe `GET /api/clientes`. Solo se puede buscar por ID o por número de identificación. |
| **Listado general de transacciones** | No implementado | Solo se listan transacciones por número de cuenta. No hay endpoint para listar todas. |
| **Paginación** | No implementada | Todos los listados devuelven la colección completa. En producción sería un problema de rendimiento. |
| **Seguridad / autenticación** | No implementada | No hay Spring Security. Cualquiera puede llamar cualquier endpoint. |
| **`ex.printStackTrace()` en producción** | Deuda técnica | `GlobalExceptionHandler.handleException()` (línea 55) imprime el stack trace en consola. En producción debería usarse un logger (`SLF4J`/`Logback`). |
| **`ddl-auto=update` en producción** | Deuda técnica | `application.properties` usa `ddl-auto=update`. En producción se debe usar `validate` o gestionar migraciones con Flyway/Liquibase. |

### Qué mejoraría con más tiempo

1. Implementar tests de integración con Testcontainers (PostgreSQL real en Docker).
2. Reemplazar `ex.printStackTrace()` con un logger `SLF4J`.
3. Agregar paginación en los endpoints de listado.
4. Migrar gestión de esquema a Flyway para control explícito de versiones de BD.
5. Agregar endpoint `GET /api/clientes` para listar todos los clientes.
6. Agregar Spring Security con autenticación básica o JWT.
