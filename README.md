# ChessConnect - Plateforme de Cours d'Echecs

Plateforme de mise en relation Profs/Eleves d'echecs avec systeme de progression standardise.

## Prerequis

- **Java 17+** (ou Java 21 recommande)
- **Node.js 18+** et npm
- **Maven 3.8+**
- **Docker & Docker Compose** (optionnel, pour deploiement)

## Demarrage Rapide

### Mode Developpement (sans Docker)

**1. Backend (Spring Boot avec H2 en memoire)**
```bash
cd backend-api
./mvnw spring-boot:run
```
Le backend demarre sur `http://localhost:8282/api`
Console H2 disponible sur `http://localhost:8282/api/h2-console`

**2. Frontend (Angular)**
```bash
cd frontend-web
npm install
npm start
```
Le frontend demarre sur `http://localhost:4200`

### Mode Docker (PostgreSQL)

```bash
docker-compose up --build
```
- Frontend: `http://localhost:4200`
- Backend: `http://localhost:8282/api`
- PostgreSQL: `localhost:5432`

## Structure du Projet

```
/chess-connect
├── /backend-api              # API Spring Boot
│   ├── /src/main/java/com/chessconnect
│   │   ├── /config           # SecurityConfig, ZoomConfig
│   │   ├── /controller       # AuthController, LessonController, TeacherController, UserController
│   │   ├── /dto              # DTOs pour auth, lesson, user, zoom
│   │   ├── /model            # Entites JPA (User, Lesson, Progress, Subscription, Payment)
│   │   ├── /repository       # Repositories Spring Data
│   │   ├── /security         # JWT (JwtService, JwtAuthenticationFilter)
│   │   └── /service          # Services metier
│   ├── Dockerfile
│   └── pom.xml
├── /frontend-web             # Application Angular 17
│   ├── /src/app
│   │   ├── /core             # Services, Guards, Interceptors, Models
│   │   └── /features         # Composants par fonctionnalite
│   │       ├── /auth         # Login, Register
│   │       ├── /dashboard    # Tableau de bord
│   │       ├── /home         # Page d'accueil
│   │       ├── /lessons      # Reservation et liste des cours
│   │       └── /teachers     # Liste et profil des profs
│   ├── Dockerfile
│   └── package.json
├── docker-compose.yml
└── README.md
```

## Specifications Metier

- **Cursus Standardise:** 5 niveaux (Pion, Cavalier, Fou, Tour, Dame). L'eleve progresse meme s'il change de prof.
- **Reservations:** Sessions d'une heure via API Zoom.
- **Abonnements:** 69€/mois (1 cours/sem), 129€/mois (2 cours/sem), 179€/mois (3 cours/sem)
- **Commission:** 10% preleves par la plateforme sur les abonnements et cours ponctuels.

## API Endpoints

### Authentification (`/api/auth`)
| Methode | Endpoint    | Description                | Auth |
|---------|-------------|----------------------------|------|
| POST    | `/register` | Inscription d'un utilisateur | Non  |
| POST    | `/login`    | Connexion (retourne JWT)   | Non  |

### Utilisateurs (`/api/users`)
| Methode | Endpoint              | Description                    | Auth   |
|---------|-----------------------|--------------------------------|--------|
| GET     | `/me`                 | Profil de l'utilisateur connecte | JWT    |
| PUT     | `/me/teacher-profile` | Modifier profil prof           | TEACHER |

### Professeurs (`/api/teachers`)
| Methode | Endpoint        | Description                        | Auth |
|---------|-----------------|------------------------------------|------|
| GET     | `/`             | Liste tous les professeurs         | Non  |
| GET     | `/{id}`         | Detail d'un professeur             | Non  |
| GET     | `/search?q=`    | Recherche par nom/bio              | Non  |
| GET     | `/subscription` | Profs acceptant les abonnements    | Non  |

### Cours (`/api/lessons`)
| Methode | Endpoint          | Description                     | Auth    |
|---------|-------------------|---------------------------------|---------|
| POST    | `/book`           | Reserver un cours               | STUDENT |
| GET     | `/{lessonId}`     | Detail d'un cours               | JWT     |
| PATCH   | `/{lessonId}/status` | Modifier le statut           | JWT     |
| GET     | `/upcoming`       | Cours a venir                   | JWT     |
| GET     | `/history`        | Historique des cours            | JWT     |

### Paiements (`/api/payments`)
| Methode | Endpoint                | Description                     | Auth    |
|---------|-------------------------|---------------------------------|---------|
| GET     | `/config`               | Cle publique Stripe             | Non     |
| GET     | `/plans`                | Liste des plans d'abonnement    | Non     |
| POST    | `/checkout/subscription`| Creer session Stripe Checkout   | STUDENT |
| GET     | `/subscription`         | Abonnement actif                | STUDENT |
| GET     | `/subscription/history` | Historique abonnements          | STUDENT |
| POST    | `/subscription/cancel`  | Annuler abonnement              | STUDENT |
| GET     | `/history`              | Historique des paiements        | JWT     |
| GET     | `/checkout/verify`      | Verifier session checkout       | JWT     |
| POST    | `/webhooks/stripe`      | Webhook Stripe                  | Non     |

### Progression (`/api/progress`)
| Methode | Endpoint          | Description                     | Auth    |
|---------|-------------------|---------------------------------|---------|
| GET     | `/me`             | Ma progression                  | STUDENT |
| GET     | `/levels`         | Liste des niveaux               | Non     |
| GET     | `/levels/{level}` | Detail d'un niveau              | Non     |

## Variables d'Environnement

### Backend (Docker)
| Variable                    | Description                  | Defaut                |
|-----------------------------|------------------------------|-----------------------|
| `SPRING_PROFILES_ACTIVE`    | Profil Spring                | `docker`              |
| `SPRING_DATASOURCE_URL`     | URL PostgreSQL               | -                     |
| `SPRING_DATASOURCE_USERNAME`| Utilisateur DB               | -                     |
| `SPRING_DATASOURCE_PASSWORD`| Mot de passe DB              | -                     |
| `JWT_SECRET`                | Cle secrete JWT              | -                     |
| `JWT_EXPIRATION`            | Duree token (ms)             | `86400000` (24h)      |
| `ZOOM_ACCOUNT_ID`           | Zoom Server-to-Server OAuth  | -                     |
| `ZOOM_CLIENT_ID`            | Zoom Client ID               | -                     |
| `ZOOM_CLIENT_SECRET`        | Zoom Client Secret           | -                     |
| `STRIPE_SECRET_KEY`         | Cle secrete Stripe           | -                     |
| `STRIPE_PUBLISHABLE_KEY`    | Cle publique Stripe          | -                     |
| `STRIPE_WEBHOOK_SECRET`     | Secret webhook Stripe        | -                     |
| `FRONTEND_URL`              | URL du frontend              | `http://localhost:4200`|

## Schema Base de Donnees

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│    users     │     │   lessons    │     │   progress   │
├──────────────┤     ├──────────────┤     ├──────────────┤
│ id           │◄────│ student_id   │     │ id           │
│ email        │◄────│ teacher_id   │     │ student_id   │──►users
│ password     │     │ scheduled_at │     │ current_level│
│ first_name   │     │ status       │     │ lessons_count│
│ last_name    │     │ zoom_url     │     │ updated_at   │
│ role         │     │ created_at   │     └──────────────┘
│ hourly_rate  │     └──────────────┘
│ bio          │
│ avatar_url   │     ┌──────────────┐     ┌──────────────┐
│ created_at   │     │subscriptions │     │   payments   │
└──────────────┘     ├──────────────┤     ├──────────────┤
                     │ id           │     │ id           │
                     │ student_id   │──►  │ user_id      │──►users
                     │ plan_type    │     │ amount_cents │
                     │ starts_at    │     │ type         │
                     │ ends_at      │     │ created_at   │
                     │ status       │     └──────────────┘
                     └──────────────┘

Roles: STUDENT, TEACHER, ADMIN
Niveaux: PION, CAVALIER, FOU, TOUR, DAME
```

## Stack Technique

| Composant | Technologies |
|-----------|-------------|
| Backend   | Spring Boot 3.2, Spring Security (JWT), Spring Data JPA, WebFlux |
| Frontend  | Angular 17, Signals, HttpClient, TypeScript |
| Database  | PostgreSQL 16 (prod), H2 (dev) |
| DevOps    | Docker, Docker Compose, Nginx |
| API Externe | Zoom Server-to-Server OAuth |

## Tests

```bash
# Backend
cd backend-api
./mvnw test

# Frontend
cd frontend-web
npm test
```