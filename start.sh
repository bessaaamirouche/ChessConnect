#!/bin/bash

echo "========================================="
echo "    ChessConnect - Demarrage Docker"
echo "========================================="
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "Erreur: Docker n'est pas en cours d'execution."
    echo "Veuillez demarrer Docker Desktop et reessayer."
    exit 1
fi

echo "Construction et demarrage des conteneurs..."
echo ""

# Build and start all services
docker compose up --build -d

echo ""
echo "========================================="
echo "    Application en cours de demarrage"
echo "========================================="
echo ""
echo "Veuillez patienter 30-60 secondes pour le demarrage complet."
echo ""
echo "URLs d'acces:"
echo "  - Frontend:  http://localhost:4200"
echo "  - Backend:   http://localhost:8282/api"
echo ""
echo "Pour voir les logs:"
echo "  docker compose logs -f"
echo ""
echo "Pour arreter:"
echo "  docker compose down"
echo ""
echo "========================================="
