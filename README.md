# Revive Backend

This is the microservices backend for the Revive Graduation Project.

## Architecture

This project is built using a Spring Boot microservices architecture. It contains the following services:

- **Auth Service** (Port 8081)
- **Client Service** (Port 8082)
- **Inventory Service** (Port 8083)
- **Menu Management Service** (Port 8084)
- **Order Service** (Port 8085)
- **Kitchen Service** (Port 8086)

Each service has its own dedicated PostgreSQL database. All databases and services run within a shared Docker network (`restaurant-net`).

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running.

## Running the Application

To build and start all microservices and databases, simply run:

```bash
docker compose up -d --build
```

This will run `mvn clean package` on all services, build their Docker images, and start the containers.

To view the logs of any specific service (e.g., auth-service):

```bash
docker logs auth-service
```

To stop all containers:

```bash
docker compose down
```
