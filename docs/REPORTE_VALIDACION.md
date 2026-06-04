# Reporte de Validación — SUSTENTACION.md

Fecha de validación: 2026-06-03
Método: cada afirmación del documento anterior fue cruzada contra el archivo fuente real, abriendo el archivo y verificando el número de línea exacto.

---

## Discrepancias encontradas y corregidas

### 1. Número de línea incorrecto — "Cuenta nace activa" (Sección 4)

**Documento anterior:** `Producto.onCreate()` — línea 65 — `if (estado == null) estado = EstadoCuenta.ACTIVA`

**Código real (`Producto.java`):**
```
65        if (saldo == null) saldo = BigDecimal.ZERO;
66        if (estado == null) estado = EstadoCuenta.ACTIVA;
```

**Corrección:** línea 65 → **línea 66**.

---

### 2. Número de línea incorrecto — saldo insuficiente en `retirar()` (Sección 4)

**Documento anterior:** `TransaccionService.retirar()` — línea 52 — `if (cuenta.getSaldo().compareTo(monto) < 0)`

**Código real (`TransaccionService.java`):**
```
52        (línea en blanco)
53        if (cuenta.getSaldo().compareTo(monto) < 0) {
```

**Corrección:** línea 52 → **línea 53**.

---

### 3. Rango de líneas incorrecto — actualización de saldo en `transferir()` (Sección 4)

**Documento anterior:** "líneas 79-89" para las actualizaciones de saldo y guardado de transacción.

**Código real (`TransaccionService.java`):**
```
79        (línea en blanco)
80        origen.setSaldo(origen.getSaldo().subtract(monto));
81        destino.setSaldo(destino.getSaldo().add(monto));
82        productoRepository.save(origen);
83        productoRepository.save(destino);
84        (línea en blanco)
85        return transaccionRepository.save(Transaccion.builder()
...
90            .build());
```

**Corrección:** líneas 79-89 → **líneas 80-90** (línea 79 es en blanco).

---

### 4. Número de línea incorrecto — saldo update en regla de actualización por transacción (Sección 4)

**Documento anterior:** `retirar()` línea 55, `transferir()` líneas 79-80

**Código real:**
- `retirar()`: `setSaldo` está en línea 57 (línea 55 es la llave de cierre del bloque `if`)
- `transferir()`: las actualizaciones están en líneas 80-81 (línea 79 es en blanco)

**Corrección:** `retirar()` línea 55 → **línea 57**; `transferir()` líneas 79-80 → **líneas 80-81**.

---

### 5. Rango de líneas incorrecto — validación de longitud nombre/apellido (Sección 4)

**Documento anterior:** `Cliente.java` — "líneas 39-45"

**Código real (`Cliente.java`):**
```
38        @Size(min = 2, max = 100, message = "Los nombres deben tener entre 2 y 100 caracteres")
39        @Column(name = "nombres", nullable = false, length = 100)
...
43        @Size(min = 2, max = 100, message = "Los apellidos deben tener entre 2 y 100 caracteres")
```

El `@Size` para `nombres` está en **línea 38**, fuera del rango 39-45 citado.

**Corrección:** líneas 39-45 → **líneas 38-45**.

---

### 6. Número de línea incorrecto — `transferir()` en pregunta P2 (Sección 7)

**Documento anterior:** `TransaccionService.transferir()` (línea 67)

**Código real (`TransaccionService.java`):**
```
67        @Transactional
68        public Transaccion transferir(...)
```

Línea 67 es la anotación `@Transactional`, no el método.

**Corrección:** línea 67 → **línea 68**.

---

### 7. Número de línea incorrecto — saldo check en `transferir()` en pregunta P14 (Sección 7)

**Documento anterior:** `TransaccionService.transferir()` (línea 75)

**Código real:**
```
75        (línea en blanco)
76        if (origen.getSaldo().compareTo(monto) < 0) {
```

**Corrección:** línea 75 → **línea 76**.

---

### 8. Sección 6 completamente desactualizada

**Documento anterior:** la tabla de archivos de test listaba solo 4 archivos. La tabla de cobertura decía:
- "Service — `ClienteService`: No hay `ClienteServiceTest`"
- "Service — `ProductoService`: No hay `ProductoServiceTest`"

**Realidad actual:** `ClienteServiceTest.java` (9 tests) y `ProductoServiceTest.java` (9 tests) existen y pasan. Total del proyecto: **34 tests, 0 fallos**.

**Corrección:** tabla actualizada con los 6 archivos de test, 34 tests totales verificados, tabla de cobertura actualizada a "Sí" para los tres services.

---

### 9. Comando incorrecto para ejecutar tests (Sección 6)

**Documento anterior:** `mvn test`

**Realidad:** `mvn` no está instalado globalmente en este entorno. El proyecto incluye Maven Wrapper.

**Corrección:** `mvn test` → `./mvnw test`

---

### 10. Sección 8 listaba como pendiente algo ya resuelto

**Documento anterior:** "Tests de `ClienteService` y `ProductoService` — Parcialmente ausentes" figuraba como deuda técnica. El punto 1 de "Qué mejoraría" decía "Agregar tests unitarios completos para `ClienteService` y `ProductoService`".

**Realidad:** ambos archivos de test existen y pasan.

**Corrección:** eliminada esa fila de la tabla de pendientes y eliminado el punto 1 de "Qué mejoraría".

---

## Contenido nuevo agregado (no existía en el documento anterior)

### A. Nota sobre `saldoDisponible` (nueva sección en §3)

Verificado: no existe ningún campo `saldoDisponible` en `Producto.java`, `Cliente.java`, `Transaccion.java` ni en ningún DTO. Solo existe `saldo`. Se documenta explícitamente con la justificación del modelo.

### B. Nota sobre transferencia — un solo registro (nueva sección §9)

Verificado: `TransaccionService.transferir()` guarda exactamente **un** objeto `Transaccion` con `cuentaOrigen` y `cuentaDestino` ambos poblados (líneas 85-90). No se generan dos registros separados.

### C. Nota sobre DEPOSITO vs "Consignación" (nueva sección §8)

El enunciado usa "consignación". El código usa el enum `DEPOSITO`, el método `consignar()` y la ruta `/deposito`. Los tres coexisten sin inconsistencia funcional. Se documenta para tener la respuesta preparada.

### D. Sección "Cómo levantar la aplicación" (nueva sección §10)

Comando, requisitos previos y URLs de Swagger y frontend.

### E. Sección "Control de versiones" (nueva sección §11)

Comandos `git log`, `git show`, `git status` para evidenciar el historial durante la sustentación.

---

## Afirmaciones verificadas y confirmadas correctas

Las siguientes referencias del documento anterior fueron verificadas y **no requerían corrección**:

| Afirmación | Verificación |
|---|---|
| `ClienteController.crear()` — línea 31 | ✅ Confirmado |
| `ClienteService.crearCliente()` — líneas 32-37 | ✅ Confirmado |
| `ClienteService.validarMayoriaDeEdad()` — línea 73 | ✅ Confirmado |
| `ClienteService.eliminar()` — línea 67 | ✅ Confirmado |
| `Cliente.@Email` — línea 48 | ✅ Confirmado |
| `ClienteService.validarNumeroIdentificacionUnico/validarEmailUnico` — líneas 79-93 | ✅ Confirmado |
| `ClienteService.actualizar()` validación condicional — líneas 46-51 | ✅ Confirmado |
| `ProductoService.crearProducto()` — línea 38 | ✅ Confirmado |
| `ProductoService.generarNumeroCuenta()` — línea 86 | ✅ Confirmado |
| Prefijo ahorro/corriente — línea 87 | ✅ Confirmado |
| `Producto.@PrePersist` saldo cero — línea 65 | ✅ Confirmado |
| `ProductoService.cancelar()` saldo check — línea 75 | ✅ Confirmado |
| `ProductoService.cambiarEstado()` cancelada check — línea 56 | ✅ Confirmado |
| `ProductoService.cambiarEstado()` bloqueo vía endpoint — línea 59 | ✅ Confirmado |
| `TransaccionService.consignar()` setSaldo — línea 38 | ✅ Confirmado |
| `Transaccion.fecha updatable=false` — línea 35 | ✅ Confirmado |
| `TransaccionRepository @Query` — línea 14 | ✅ Confirmado |
| `GlobalExceptionHandler.handleValidationException()` — línea 37 | ✅ Confirmado |
| `GlobalExceptionHandler.handleException()` printStackTrace — línea 55 | ✅ Confirmado |
| Todas las rutas HTTP y verbos de los 3 controllers | ✅ Confirmado |
| Todas las columnas y tipos de las 3 entidades | ✅ Confirmado |
| Springdoc versión 2.8.8 en pom.xml | ✅ Confirmado |
