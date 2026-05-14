#!/bin/bash

echo "Iniciando Linktic..."
docker compose up --build -d

echo "Esperando que los servicios estén listos..."
until docker inspect linktic-catalog --format='{{.State.Health.Status}}' 2>/dev/null | grep -q "healthy"; do
  sleep 3
done
until docker inspect linktic-stock --format='{{.State.Health.Status}}' 2>/dev/null | grep -q "healthy"; do
  sleep 3
done

echo ""
echo "Servicios disponibles:"
echo "  Frontend:          http://localhost:9000"
echo "  Catalog Service:   http://localhost:8080/swagger-ui/index.html"
echo "  Stock Service:     http://localhost:8081/swagger-ui/index.html"
echo "  RabbitMQ:          http://localhost:15672  (guest / guest)"
echo ""
