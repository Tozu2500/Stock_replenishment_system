# Stock Replenishment System (StockFlow)

A Spring Boot web application for managing products, warehouses, stock levels, suppliers and purchase orders.
It answers the questions a small company asks every day:

* Which products are running low, and in which warehouse?
* How much should be ordered, and from which supplier?
* Which purchase orders are still open?

When the stock of a product falls to its minimum level, the system suggests a purchase order that brings the stock
back up to the target level. When the goods arrive and the order is received, stock is updated automatically and
every change is stored in a movement history.

## Features

* **Dashboard**: product, warehouse and supplier counts, open orders, low and out-of-stock rows, inventory value.
* **Products** (CRUD) with search, category filter, sorting and pagination. Each product has a category (one-to-many)
  and any number of suppliers (many-to-many).
* **Categories, warehouses, suppliers** (CRUD) with business rules. For example, a category that still has products,
  or a warehouse that still holds stock, cannot be deleted.
* **Stock** per product and warehouse, with status `IN_STOCK` / `LOW_STOCK` / `OUT_OF_STOCK`. Sales, returns,
  receipts and adjustments are recorded as stock movements (who, when, why). Stock can never go negative.
* **Replenishment**: recommended order quantities and the preferred supplier. One click adds the line to a purchase order.
* **Purchase orders**: `PENDING → ORDERED → RECEIVED` (or `CANCELLED`). Only active products that the supplier
  supplies can be ordered, inactive suppliers cannot receive orders, and receiving adds stock in one database transaction.
* **Security**: login with two roles. `USER` can view everything; `ADMIN` can also create, edit and delete.
* **REST API** for products, categories, stock and purchase orders (GET, POST, PUT, DELETE) with JSON error responses.

## How the course criteria are met

| Grade | Requirement | Where |
|---|---|---|
| 1 | Spring Boot, JPA, MVC, Thymeleaf page with data from the database | `model/`, `repository/`, `service/`, `controller/`, `templates/` |
| 2 | CRUD, OneToMany | All entities have CRUD. `Category → Product`, `Warehouse → StockItem`, `Product → StockItem`, `Supplier → PurchaseOrder`, `PurchaseOrder → PurchaseOrderItem`, `StockItem → StockMovement` |
| 3 | Login with Spring Security, USER read-only, ADMIN everything | `security/SecurityConfig.java`, `security/DatabaseUserDetailsService.java` |
| 4 | `@Valid` input validation, REST with `@RestController` (GET/POST/PUT/DELETE) | `dto/*Request.java`, `validation/`, `rest/` |
| 5 | JUnit tests, external database (MySQL), deployment, extra Spring features | `src/test/`, `application.properties`, `Dockerfile`; extras below |

Extra Spring features beyond the course basics:

* **Many-to-many** relationship `Product ↔ Supplier` (join table `product_suppliers`), which is also used in the
  business rules (preferred supplier, which products can be ordered from whom).
* **Error handling**: `@RestControllerAdvice` returns consistent JSON errors (400/404/409/500), and
  `@ControllerAdvice` shows business-rule errors as messages in the web UI. There is a custom error page for 403/404/500.
* **Custom cross-field validator** `@ValidStockLevels` (target stock must not be below minimum stock).
* **Spring Data JPA auditing** (`@CreatedBy`, `@CreatedDate`, `@LastModifiedDate`) records who made each stock movement.
* **Transactions**: receiving an order updates several stock rows atomically (`@Transactional`).
* **Two security filter chains**: session and form login for the web UI, stateless HTTP Basic for the REST API.
* **Pagination and sorting** with Spring Data `Page` and `Sort`; JPQL search query.
* **Security tests** for authentication and authorization (`SecurityTest`).

## Technology

Java 21, Spring Boot 4.1 (Web MVC, Data JPA, Security, Validation), Thymeleaf, MySQL 8, H2 (tests and the deployed
demo only), JUnit 5, Mockito, AssertJ and Maven.

## Architecture

```
Browser ──> controller/ (Thymeleaf MVC) ──┐
                                          ├──> service/ (business rules) ──> repository/ (Spring Data JPA) ──> MySQL
Postman ──> rest/ (@RestController) ──────┘
```

| Package | Responsibility |
|---|---|
| `model` | JPA entities and enums (`StockStatus`, `OrderStatus`, `MovementType`) |
| `repository` | Spring Data JPA repositories (derived queries + one JPQL search query) |
| `service` | Business logic and transactions; the only layer that changes data |
| `controller` | Web pages: reads input, calls a service, chooses a Thymeleaf view |
| `rest` | JSON API and `ApiExceptionHandler` |
| `dto` | Request records (shared by HTML forms and the API, they carry the validation rules) and response records |
| `security`, `config`, `validation`, `exception` | Security rules, JPA auditing + demo data, custom validator, exceptions |

Controllers never contain business rules. For example, both `PurchaseOrderController` (web) and
`PurchaseOrderRestController` (API) call the same `PurchaseOrderService.changeStatus(...)`.

### Data model

```
Category 1───* Product *───* Supplier 1───* PurchaseOrder 1───* PurchaseOrderItem *───1 Product
                  1                                  *
                  │                                  │
                  *                                  1
Warehouse 1───* StockItem 1───* StockMovement     Warehouse (delivery destination)
AppUser (username, BCrypt password hash, role USER/ADMIN)
```

## Running locally

### With MySQL (the default)

1. Start MySQL 8. The database `stock_replenishment` is created automatically on first start.
2. Give the database password. Either set the environment variables `DB_URL`, `DB_USERNAME` and `DB_PASSWORD`, or
   create a file named `.env` in the project root (it is in `.gitignore`, so it is never committed):
   ```
   DB_USERNAME=root
   DB_PASSWORD=your-mysql-password
   ```
3. Start the application: `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`).
4. Open http://localhost:8473

Demo data is inserted when the database is empty. To start again from the original demo data, drop the database
(`DROP DATABASE stock_replenishment;`) and restart the application.

### Quick start without MySQL

`./mvnw spring-boot:run -Dspring-boot.run.profiles=demo` uses an in-memory H2 database. The data is reset on every start.

### Demo accounts

| Username | Password | Role |
|---|---|---|
| `admin` | `admin123` | ADMIN: everything |
| `user` | `user123` | USER: read only |

The passwords can be changed with the environment variables `ADMIN_PASSWORD` and `USER_PASSWORD` before the first
start. Passwords are stored only as BCrypt hashes.

## REST API

Authentication is HTTP Basic (Postman: *Authorization → Basic Auth*). Any logged-in user may use `GET`; `POST`, `PUT`
and `DELETE` require the ADMIN role. Without credentials the API answers `401`, and a USER trying to change data gets `403`.

| Method | URL | Description |
|---|---|---|
| GET | `/api/products`, `/api/products/{id}` | List / get products |
| POST | `/api/products` | Create (201) |
| PUT | `/api/products/{id}` | Update (200) |
| DELETE | `/api/products/{id}` | Delete (204; 409 if the product still has stock or orders) |
| GET, POST, PUT, DELETE | `/api/categories`, `/api/categories/{id}` | Same operations for categories |
| GET | `/api/stock?warehouseId=1&status=LOW_STOCK` | Stock rows, optional filters |
| GET | `/api/stock/replenishment` | Replenishment suggestions |
| POST | `/api/stock`, `/api/stock/{id}/movements` | Add a product to a warehouse / record a sale, return or adjustment |
| DELETE | `/api/stock/{id}` | Delete an empty stock row |
| GET, POST | `/api/purchase-orders`, `/api/purchase-orders/{id}` | List, get and create orders (lines in `items`) |
| PUT | `/api/purchase-orders/{id}` | Replace a pending order |
| PUT | `/api/purchase-orders/{id}/status` | `{"status": "ORDERED"}`, then `{"status": "RECEIVED"}` (adds stock) |
| DELETE | `/api/purchase-orders/{id}` | Delete a pending or cancelled order |

Example: `POST /api/products`

```json
{
  "name": "Webcam HD",
  "sku": "CAM-HD",
  "unitPrice": 59.90,
  "minimumStock": 4,
  "targetStock": 12,
  "categoryId": 1,
  "supplierIds": [2]
}
```

Validation error (`400`):

```json
{
  "timestamp": "2026-10-06T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/products",
  "fieldErrors": { "name": "must not be blank", "targetStock": "must be greater than or equal to minimum stock" }
}
```

## Tests

`./mvnw test` runs 38 tests against an in-memory H2 database (`src/test/resources/application-test.properties`):

| Test class | Type | What it checks |
|---|---|---|
| `ReplenishmentRulesTest` | Unit | Stock status, reorder formula, movement signs, order status workflow |
| `ProductServiceTest` | Unit with Mockito | Duplicate SKU, not found, cannot delete a product in stock |
| `InventoryServiceTest` | Integration | Sales, receiving updates stock, invalid transitions, supplier rules, inactive products, replenishment ordering |
| `SecurityTest` | Integration | Login success/failure, USER read-only, ADMIN rights, API 401/403, redirects never leave the site |
| `ProductApiTest` | Integration | REST CRUD, 400 validation errors, 404 (also unknown supplier), 409, 415 |
| `ProductFormTest` | Integration | Thymeleaf form: errors and typed values are shown again, valid input is saved |

## Deployment

The `Dockerfile` builds the application and runs it with the `demo` profile (H2, which the course allows for the
deployed version). On Render.com: *New → Web Service*, connect the GitHub repository, choose the Docker runtime and
deploy. Render sets the `PORT` variable, which the application uses automatically. Set `ADMIN_PASSWORD` and
`USER_PASSWORD` in the service settings. To use an external MySQL instead, set `SPRING_PROFILES_ACTIVE=default`
together with `DB_URL`, `DB_USERNAME` and `DB_PASSWORD`.

## Code size

The project is kept compact on purpose (limit 4,000 lines without templates, 4,500 with templates):

| Part | Lines (incl. blank lines and comments) |
|---|---|
| Java, application | 2,927 |
| Java, tests | 553 |
| CSS | 91 |
| Thymeleaf templates | 667 |
| **Total** | **4,238** |

## Possible improvements

Partial receiving of orders, stock transfers between warehouses, a user management page, a scheduled low-stock
e-mail, database migrations with Flyway and optimistic locking (`@Version`) for concurrent stock updates.
