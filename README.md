# Mendel Transactions

Servicio REST en Spring Boot que guarda transacciones en memoria y responde cuánto suman las que cuelgan de una transacción.

No usa base de datos. El proceso es la fuente de verdad: al reiniciar, el almacén queda vacío.

## API

### `PUT /transactions/{transaction_id}`

Crea o reemplaza una transacción.

```json
{ "amount": 5000, "type": "cars", "parent_id": 10 }
```

`parent_id` es opcional. La respuesta es `{ "status": "ok" }`.

Un `PUT` sobre un id existente reemplaza amount, type y parent. Si el body no trae `parent_id`, la transacción queda sin padre.

### `GET /transactions/types/{type}`

Devuelve los ids de ese tipo, en el orden en que se crearon:

```json
[10, 11]
```

Un tipo desconocido responde `[]`.

### `GET /transactions/sum/{transaction_id}`

```json
{ "sum": 20000.0 }
```

La suma es el monto de esa transacción más el de todos sus descendientes, siguiendo `parent_id` hacia abajo. No incluye ancestros.

```text
10 (cars, 5000)
 └── 11 (shopping, 10000)
      └── 12 (shopping, 5000)

GET /transactions/sum/10  -> 20000
GET /transactions/sum/11  -> 15000
GET /transactions/sum/12  -> 5000
```

Si el id no existe, responde `404`.

## Reglas de vínculo

- El padre tiene que existir antes de crear el hijo.
- Una transacción no puede ser su propio padre.
- No se acepta un `parent_id` que forme un ciclo.
- Esos casos responden `400` con `{ "message": "..." }`.

## Diseño

```text
web        TransactionController, DTOs, GlobalExceptionHandler
service    TransactionService          reglas de amount/type y la suma
repository TransactionRepository       puerto
           InMemoryTransactionRepository   mapas por id, por type y de hijos
domain     Transaction
```

El controlador no conoce el almacén. El servicio depende del puerto `TransactionRepository`, no de la implementación en memoria. Los índices se actualizan en el mismo lock que el alta, así un cambio de type o de padre no deja la suma a medias.

## Requisitos

- Java 17
- Maven 3.9+

## Tests

```bash
mvn test
```

`TransactionServiceTest` cubre la suma, el reparent, los ciclos y los índices por tipo. `TransactionApiTest` levanta la aplicación y ejercita los tres endpoints, incluido el ejemplo de la especificación.

En esta máquina el JDK del sistema es 8. Los tests se pueden correr con la imagen de Maven:

```bash
docker run --rm -v "$PWD":/app -w /app maven:3.9.9-eclipse-temurin-17 mvn -B test
```

## Ejecutar

```bash
mvn spring-boot:run
```

o con Docker:

```bash
docker compose up --build
```

El servicio escucha en el puerto 8080.

```bash
curl -X PUT localhost:8080/transactions/10 \
  -H "Content-Type: application/json" \
  -d "{\"amount\":5000,\"type\":\"cars\"}"

curl -X PUT localhost:8080/transactions/11 \
  -H "Content-Type: application/json" \
  -d "{\"amount\":10000,\"type\":\"shopping\",\"parent_id\":10}"

curl -X PUT localhost:8080/transactions/12 \
  -H "Content-Type: application/json" \
  -d "{\"amount\":5000,\"type\":\"shopping\",\"parent_id\":11}"

curl localhost:8080/transactions/types/cars
curl localhost:8080/transactions/sum/10
```
