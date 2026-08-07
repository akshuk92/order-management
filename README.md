# Order Management DevOps Demo

A production-style Spring Boot REST API for Order Management, built as a **DevOps CI/CD practice project**. It demonstrates a complete pipeline: Maven build → unit tests → Docker image → Docker Compose deployment → Jenkins CI/CD → Kubernetes deployment → Prometheus/Grafana monitoring.

## Tech Stack

| Layer            | Technology                          |
|-------------------|--------------------------------------|
| Language          | Java 17                             |
| Framework         | Spring Boot 3.3.x                   |
| Build Tool        | Maven                               |
| Persistence       | Spring Data JPA + MySQL 8           |
| API               | REST (JSON)                         |
| Monitoring        | Spring Boot Actuator + Micrometer/Prometheus |
| Containerization  | Docker (multi-stage) + Docker Compose |
| CI/CD             | Jenkins (declarative pipeline)      |
| Orchestration     | Kubernetes (Deployment/Service/ConfigMap/Secret) |

## Project Structure

```
order-management-devops-demo
├── src/main/java/com/example/order
│   ├── controller        # REST controllers
│   ├── service             # Business logic (interface + impl)
│   ├── repository         # Spring Data JPA repositories
│   ├── entity              # JPA entities + OrderStatus enum
│   ├── dto                # Request/response DTOs
│   ├── exception           # Custom exceptions + global handler
│   └── OrderManagementApplication.java
├── src/main/resources
│   ├── application.properties
│   └── data.sql
├── src/test/java           # Unit tests (service + controller)
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── Jenkinsfile
├── k8s/                    # Kubernetes manifests
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── configmap.yaml
│   ├── secret.yaml
│   └── mysql.yaml          # optional in-cluster MySQL for demo
└── README.md
```

## Entity: Order

| Field        | Type            | Notes                                   |
|--------------|-----------------|------------------------------------------|
| id           | Long            | Primary key, auto-generated              |
| customerName | String          | Required                                 |
| productName  | String          | Required                                 |
| quantity     | Integer         | Required, minimum 1                      |
| price        | Double          | Required, unit price, must be positive   |
| status       | OrderStatus     | PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED (defaults to PENDING) |
| orderDate    | LocalDateTime   | Set automatically on creation            |

The API response also includes a computed `totalAmount` (`price * quantity`).

## REST API Endpoints

| Method | Endpoint             | Description            |
|--------|------------------------|--------------------------|
| GET    | `/api/orders`          | List all orders         |
| GET    | `/api/orders/{id}`     | Get order by id         |
| POST   | `/api/orders`          | Create a new order      |
| PUT    | `/api/orders/{id}`     | Update an order          |
| DELETE | `/api/orders/{id}`     | Delete an order          |

Sample request body (POST/PUT):
```json
{
  "customerName": "Alice Johnson",
  "productName": "Wireless Mouse",
  "quantity": 2,
  "price": 19.99,
  "status": "PENDING"
}
```

`status` is optional on creation (defaults to `PENDING`). Orders in a terminal state (`DELIVERED` or `CANCELLED`) cannot be updated — attempting to do so returns `409 Conflict`.

Actuator endpoints:
- `/actuator/health`
- `/actuator/info`
- `/actuator/metrics`
- `/actuator/prometheus`

## Database Configuration

Credentials are **never hardcoded**. They are injected via environment variables:

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

Database: `order_db` · Table: `orders`

---

## 1. How to Run Locally (without Docker)

**Prerequisites:** Java 17, Maven 3.9+, a running MySQL 8 instance.

```bash
# 1. Create the database
mysql -u root -p -e "CREATE DATABASE order_db;"

# 2. Export environment variables
export DB_URL="jdbc:mysql://localhost:3306/order_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export DB_USERNAME=root
export DB_PASSWORD=yourpassword

# 3. Run the application
mvn spring-boot:run
```

The API will be available at `http://localhost:8080/api/orders`.

## 2. How to Build the Maven Package

```bash
mvn clean package
```

This produces `target/app.jar`. To skip tests during packaging:

```bash
mvn clean package -DskipTests
```

To run the unit tests only:

```bash
mvn test
```

## 3. How to Create the Docker Image

The `Dockerfile` uses a **multi-stage build**: a Maven image compiles the app, and a lightweight Eclipse Temurin JRE image runs it as a **non-root user**.

```bash
docker build -t order-management:latest .
```

Run the container standalone (requires an external/reachable MySQL):

```bash
docker run -d --name order-app -p 8080:8080 \
  -e DB_URL="jdbc:mysql://<mysql-host>:3306/order_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
  -e DB_USERNAME=order_user \
  -e DB_PASSWORD=order_pass \
  order-management:latest
```

## 4. How to Run with Docker Compose

`docker-compose.yml` spins up two services: `order-db` (MySQL 8, with a persistent volume) and `order-app` (built from the local `Dockerfile`), on a shared network.

```bash
# Optional: copy and adjust environment variables
cp .env.example .env

# Build and start both containers
docker compose up -d --build

# Check status
docker compose ps

# Tail logs
docker compose logs -f order-app

# Stop and remove containers
docker compose down
```

Once healthy, verify:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/orders
```

## 5. Jenkins Pipeline Flow

The `Jenkinsfile` defines a declarative pipeline with the following stages:

1. **Checkout** — pulls source code from GitHub (`GIT_REPO_URL` / `GIT_BRANCH`).
2. **Maven Build** — runs `mvn clean package -DskipTests` to compile and package `app.jar`.
3. **Run Unit Tests** — runs `mvn test`; results are published via JUnit report.
4. **Docker Build** — builds a Docker image tagged `order-management:${BUILD_NUMBER}` (and `:latest`).
5. **Docker Compose Deployment** — runs `docker compose up -d --build` to deploy app + database.
6. **Health Check** — polls `/actuator/health` until the app reports `UP`, with retries; fails the build if the app never becomes healthy.

Requirements on the Jenkins agent: Docker, Docker Compose plugin/CLI, Maven, and JDK 17 (tool names `Maven3` / `JDK17` configured in Jenkins Global Tool Configuration, or adjust the `tools {}` block).

To use this pipeline:
1. Create a new Jenkins Pipeline job pointing at this repository.
2. Ensure the Jenkins agent user can run `docker` commands (add to the `docker` group).
3. Update `GIT_REPO_URL` in the `Jenkinsfile` (or configure via SCM in the job itself).
4. Trigger a build manually or via a webhook on push.

## 6. Kubernetes Deployment Steps

Manifests live under `k8s/`:

- `configmap.yaml` — non-secret app configuration (DB URL, active profile, actuator exposure).
- `secret.yaml` — DB username/password (base64-encoded demo placeholders — replace before real use).
- `deployment.yaml` — 2 replicas of the app, readiness/liveness probes on actuator health endpoints, resource requests/limits, Prometheus scrape annotations.
- `service.yaml` — ClusterIP service exposing port 8080.
- `mysql.yaml` — optional in-cluster MySQL (Deployment + PVC + Service) for demo/testing.

Steps:

```bash
# 1. Build and push the image to a registry accessible by your cluster
docker build -t <your-registry>/order-management:1.0.0 .
docker push <your-registry>/order-management:1.0.0

# 2. Update the image reference in k8s/deployment.yaml
#    image: <your-registry>/order-management:1.0.0

# 3. (Optional) Update k8s/secret.yaml with real base64-encoded credentials
echo -n 'my_db_user' | base64
echo -n 'my_db_password' | base64

# 4. Apply manifests
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/mysql.yaml        # optional in-cluster MySQL
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml

# 5. Verify rollout
kubectl rollout status deployment/order-app-deployment
kubectl get pods -l app=order-management
kubectl get svc order-app-service

# 6. Access the service (example: port-forward for local testing)
kubectl port-forward svc/order-app-service 8080:8080
curl http://localhost:8080/actuator/health
```

For external access, put an Ingress or a `LoadBalancer`/`NodePort` Service in front of `order-app-service` as needed for your cluster.

## 7. Monitoring with Prometheus & Grafana

The app exposes Prometheus-formatted metrics at `/actuator/prometheus`.

Example Prometheus scrape config:

```yaml
scrape_configs:
  - job_name: 'order-management'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['order-app:8080']
```

In Kubernetes, the `Deployment` and `Service` already carry `prometheus.io/scrape`, `prometheus.io/path`, and `prometheus.io/port` annotations for auto-discovery by a Prometheus instance configured with `kubernetes_sd_configs`.

Point Grafana at your Prometheus data source and import (or build) a JVM/Spring Boot dashboard to visualize request rates, latency, JVM memory/GC, and HTTP status codes.

## Code Quality Notes

- **Clean architecture**: controller → service (interface + impl) → repository → entity, with DTOs isolating the API contract from persistence models.
- **Validation**: `jakarta.validation` annotations (`@NotBlank`, `@Min`, `@Positive`) on request DTOs.
- **Exception handling**: centralized `@RestControllerAdvice` (`GlobalExceptionHandler`) mapping domain exceptions (not found, invalid state transition, validation failure) to structured JSON error responses.
- **Business rule**: orders in a terminal state (`DELIVERED`/`CANCELLED`) cannot be modified — enforced in the service layer.
- **Logging**: SLF4J logging (via Lombok `@Slf4j`) at controller and service layers.
- **Testing**: Mockito-based service unit tests and `@WebMvcTest` controller tests with `MockMvc`.
