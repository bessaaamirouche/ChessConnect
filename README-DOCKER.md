# ChessConnect - Guide de Deploiement Docker

## Prerequis

- **Docker Desktop** installe et en cours d'execution
  - [Telecharger pour Mac](https://www.docker.com/products/docker-desktop/)
  - [Telecharger pour Windows](https://www.docker.com/products/docker-desktop/)

## Demarrage Rapide

### Option 1: Script automatique (recommande)

```bash
# Demarrer l'application
./start.sh

# Arreter l'application
./stop.sh
```

### Option 2: Commandes manuelles

```bash
# Construire et demarrer tous les services
docker compose up --build -d

# Voir les logs
docker compose logs -f

# Arreter tous les services
docker compose down

# Arreter et supprimer les donnees
docker compose down -v
```

## Acces a l'Application

Une fois demarree (attendre 30-60 secondes):

| Service  | URL                         |
|----------|----------------------------|
| Frontend | http://localhost:4200       |
| Backend  | http://localhost:8282/api   |
| Database | localhost:5433 (PostgreSQL) |

## Comptes de Test

Apres le premier demarrage, creez un compte via l'interface:

1. Allez sur http://localhost:4200
2. Cliquez sur "S'inscrire"
3. Choisissez "Eleve" ou "Professeur"
4. Remplissez le formulaire

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│    Frontend     │     │    Backend      │     │   PostgreSQL    │
│  (Angular/Nginx)│────>│  (Spring Boot)  │────>│   (Database)    │
│   Port: 4200    │     │   Port: 8282    │     │   Port: 5433    │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

## Depannage

### Les conteneurs ne demarrent pas

```bash
# Verifier l'etat des conteneurs
docker compose ps

# Voir les logs d'erreur
docker compose logs backend
docker compose logs frontend
docker compose logs postgres
```

### Reinitialiser l'application

```bash
# Arreter et supprimer tout (y compris la base de donnees)
docker compose down -v

# Reconstruire et redemarrer
docker compose up --build -d
```

### Port deja utilise

Si le port 4200, 8282 ou 5433 est deja utilise, modifiez les ports dans `docker-compose.yml`:

```yaml
ports:
  - "NOUVEAU_PORT:PORT_INTERNE"
```

## Fonctionnalites

- **Inscription Eleve/Professeur**: Creation de compte avec roles
- **Quiz d'Evaluation**: Determinez votre niveau d'echecs (Pion → Dame)
- **Reservation de Cours**: Reservez des cours avec des professeurs
- **Suivi de Progression**: Suivez votre parcours d'apprentissage
- **Abonnements**: Gerez vos abonnements
- **Paiements Stripe**: Paiements securises (mode test)

## Support

Pour toute question ou probleme, contactez le developpeur.
