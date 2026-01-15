# ChessConnect - Plateforme de Cours d'Echecs

Plateforme de mise en relation Profs/Eleves d'echecs avec systeme de progression standardise.

## Demarrage Rapide (Docker)

**Prerequis:** Uniquement [Docker Desktop](https://www.docker.com/products/docker-desktop/) installe et en cours d'execution.

```bash
# 1. Cloner le projet
git clone https://github.com/bessaaamirouche/ChessConnect.git
cd ChessConnect

# 2. Lancer l'application
./start.sh

# 3. Attendre 1-2 minutes, puis ouvrir
# http://localhost:4200
```

**C'est tout !** Pas besoin de Java, Node.js, ou PostgreSQL.

### URLs d'acces

| Service  | URL                         |
|----------|----------------------------|
| Frontend | http://localhost:4200       |
| Backend  | http://localhost:8282/api   |
| Database | localhost:5433 (PostgreSQL) |

### Commandes utiles

```bash
# Voir les logs
docker compose logs -f

# Arreter l'application
./stop.sh

# Arreter et supprimer les donnees
docker compose down -v
```

## Fonctionnalites

- **Inscription Eleve/Professeur** : Creation de compte avec roles
- **Quiz d'Evaluation** : Determinez votre niveau d'echecs (Pion, Cavalier, Fou, Tour, Dame)
- **Parcours d'Apprentissage** : 50 cours structures par niveau
- **Reservation de Cours** : Reservez des sessions avec des professeurs
- **Suivi de Progression** : Suivez votre parcours d'apprentissage
- **Abonnements** : 3 formules (1, 2 ou 3 cours/semaine)
- **Paiements Stripe** : Paiements securises (mode test)
- **Integration Zoom** : Cours en visioconference

## Mode Developpement (sans Docker)

### Prerequis

- Java 17+
- Node.js 18+ et npm
- Maven 3.8+

### Backend (Spring Boot)

```bash
cd backend-api
./mvnw spring-boot:run
```
Le backend demarre sur `http://localhost:8282/api`

### Frontend (Angular)

```bash
cd frontend-web
npm install
npm start
```
Le frontend demarre sur `http://localhost:4200`

## Structure du Projet

```
/ChessConnect
├── /backend-api              # API Spring Boot
│   ├── /src/main/java/com/chessconnect
│   │   ├── /config           # SecurityConfig, ZoomConfig, QuizDataInitializer
│   │   ├── /controller       # REST Controllers
│   │   ├── /dto              # Data Transfer Objects
│   │   ├── /model            # Entites JPA
│   │   ├── /repository       # Repositories Spring Data
│   │   ├── /security         # JWT (JwtService, JwtAuthenticationFilter)
│   │   └── /service          # Services metier
│   ├── Dockerfile
│   └── pom.xml
├── /frontend-web             # Application Angular 17
│   ├── /src/app
│   │   ├── /core             # Services, Guards, Interceptors, Models
│   │   ├── /features         # Composants par fonctionnalite
│   │   │   ├── /auth         # Login, Register
│   │   │   ├── /dashboard    # Tableau de bord
│   │   │   ├── /lessons      # Reservation et liste des cours
│   │   │   ├── /progress     # Suivi de progression
│   │   │   ├── /quiz         # Quiz d'evaluation de niveau
│   │   │   └── /teachers     # Liste et profil des profs
│   │   └── /shared           # Composants partages
│   ├── Dockerfile
│   └── package.json
├── docker-compose.yml
├── start.sh                  # Script de demarrage Docker
├── stop.sh                   # Script d'arret Docker
├── .env.example              # Variables d'environnement (template)
└── README.md
```

## Specifications Metier

- **Cursus Standardise:** 5 niveaux (Pion, Cavalier, Fou, Tour, Dame). L'eleve progresse meme s'il change de prof.
- **Quiz d'Evaluation:** 25 questions (5 par niveau) pour determiner le niveau initial
- **Reservations:** Sessions d'une heure via API Zoom
- **Abonnements:** 69€/mois (1 cours/sem), 129€/mois (2 cours/sem), 179€/mois (3 cours/sem)
- **Commission:** 10% preleves par la plateforme

## API Endpoints

### Authentification (`/api/auth`)
| Methode | Endpoint    | Description                | Auth |
|---------|-------------|----------------------------|------|
| POST    | `/register` | Inscription d'un utilisateur | Non  |
| POST    | `/login`    | Connexion (retourne JWT)   | Non  |

### Quiz (`/api/quiz`)
| Methode | Endpoint     | Description                     | Auth    |
|---------|--------------|---------------------------------|---------|
| GET     | `/questions` | Liste des questions du quiz     | Non     |
| POST    | `/submit`    | Soumettre les reponses          | STUDENT |
| GET     | `/result`    | Dernier resultat du quiz        | STUDENT |

### Progression (`/api/progress`)
| Methode | Endpoint          | Description                     | Auth    |
|---------|-------------------|---------------------------------|---------|
| GET     | `/me`             | Ma progression                  | STUDENT |
| GET     | `/learning-path`  | Parcours d'apprentissage        | STUDENT |

### Professeurs (`/api/teachers`)
| Methode | Endpoint        | Description                        | Auth |
|---------|-----------------|------------------------------------|------|
| GET     | `/`             | Liste tous les professeurs         | Non  |
| GET     | `/{id}`         | Detail d'un professeur             | Non  |

### Cours (`/api/lessons`)
| Methode | Endpoint          | Description                     | Auth    |
|---------|-------------------|---------------------------------|---------|
| POST    | `/book`           | Reserver un cours               | STUDENT |
| GET     | `/upcoming`       | Cours a venir                   | JWT     |
| GET     | `/history`        | Historique des cours            | JWT     |

### Paiements (`/api/payments`)
| Methode | Endpoint                | Description                     | Auth    |
|---------|-------------------------|---------------------------------|---------|
| GET     | `/plans`                | Liste des plans d'abonnement    | Non     |
| POST    | `/checkout/subscription`| Creer session Stripe Checkout   | STUDENT |
| GET     | `/subscription`         | Abonnement actif                | STUDENT |

## Variables d'Environnement

Voir `.env.example` pour la liste complete. Les valeurs par defaut permettent de tester l'application sans configuration.

| Variable                    | Description                  |
|-----------------------------|------------------------------|
| `POSTGRES_*`                | Configuration PostgreSQL     |
| `JWT_SECRET`                | Cle secrete JWT              |
| `STRIPE_*`                  | Configuration Stripe         |
| `ZOOM_*`                    | Configuration Zoom API       |

## Stack Technique

| Composant | Technologies |
|-----------|-------------|
| Backend   | Spring Boot 3.2, Spring Security (JWT), Spring Data JPA |
| Frontend  | Angular 17, Signals, Standalone Components |
| Database  | PostgreSQL 16 |
| DevOps    | Docker, Docker Compose, Nginx |
| Paiements | Stripe (mode test) |
| Video     | Zoom Server-to-Server OAuth |

## Architecture Docker

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│    Frontend     │     │    Backend      │     │   PostgreSQL    │
│  (Angular/Nginx)│────>│  (Spring Boot)  │────>│   (Database)    │
│   Port: 4200    │     │   Port: 8282    │     │   Port: 5433    │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```
