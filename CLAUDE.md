# ChessConnect - Plateforme de Cours d'Echecs

Plateforme de mise en relation Profs/Eleves d'echecs avec systeme de progression standardise.

## Demarrage Rapide (Docker)

**Prerequis:** Uniquement [Docker Desktop](https://www.docker.com/products/docker-desktop/) installe et en cours d'execution.

```bash
# 1. Cloner le projet
git clone https://github.com/bessaaamirouche/ChessConnect.git
cd ChessConnect

# 2. Configurer Stripe (obligatoire pour les paiements)
cat > .env << 'EOF'
STRIPE_SECRET_KEY=sk_test_votre_cle_secrete
STRIPE_PUBLISHABLE_KEY=pk_test_votre_cle_publique
STRIPE_WEBHOOK_SECRET=whsec_votre_secret_webhook
EOF

# 3. Lancer l'application
./start.sh

# 4. Attendre 1-2 minutes, puis ouvrir
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

## Deploiement sur VPS

### 1. Installer Docker sur le serveur

```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER && newgrp docker
```

### 2. Cloner et configurer

```bash
git clone https://github.com/bessaaamirouche/ChessConnect.git
cd ChessConnect

# Creer le fichier .env avec vos cles Stripe
cat > .env << 'EOF'
STRIPE_SECRET_KEY=sk_test_votre_cle_secrete
STRIPE_PUBLISHABLE_KEY=pk_test_votre_cle_publique
STRIPE_WEBHOOK_SECRET=whsec_votre_secret_webhook
EOF

# Modifier l'URL frontend avec l'IP du serveur
sed -i 's|FRONTEND_URL: http://localhost:4200|FRONTEND_URL: http://VOTRE_IP:4200|' docker-compose.yml
```

### 3. Lancer

```bash
./start.sh
# ou: docker compose up --build -d
```

L'application sera accessible sur `http://VOTRE_IP:4200`

### 4. Ouvrir les ports (si necessaire)

```bash
sudo ufw allow 4200
sudo ufw allow 8282
```

## Fonctionnalites

- **Inscription Eleve/Professeur** : Creation de compte avec roles
- **Quiz d'Evaluation** : Determinez votre niveau d'echecs (Pion, Cavalier, Fou, Tour, Dame)
- **Parcours d'Apprentissage** : 50 cours structures par niveau
- **Reservation de Cours** : Reservez des sessions avec des professeurs
- **Disponibilites 24h/24** : Les professeurs peuvent creer des creneaux a n'importe quelle heure
- **Reservations urgentes** : Les creneaux restent visibles jusqu'a 5 min apres l'heure de debut
- **Suivi de Progression** : Suivez votre parcours d'apprentissage
- **Abonnements** : 3 formules (1, 2 ou 3 cours/semaine)
- **Paiements Stripe** : Paiements securises integres (mode test)
- **Video Jitsi Meet** : Cours en visioconference (ouvre dans un nouvel onglet)
- **Notifications en temps reel** : Alertes pour nouvelles disponibilites et reservations

## Mode Developpement (sans Docker)

### Prerequis

- Java 17+
- Node.js 18+ et npm
- Maven 3.8+
- PostgreSQL 16 (port 5433)

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
│   │   ├── /config           # SecurityConfig, StripeConfig, QuizDataInitializer
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
│   │   │   ├── /availability # Gestion des disponibilites (prof)
│   │   │   ├── /dashboard    # Tableau de bord
│   │   │   ├── /lessons      # Reservation et liste des cours
│   │   │   ├── /progress     # Suivi de progression
│   │   │   ├── /quiz         # Quiz d'evaluation de niveau
│   │   │   ├── /subscription # Gestion des abonnements
│   │   │   └── /teachers     # Liste et profil des profs
│   │   └── /shared           # Composants partages (toast, modals, etc.)
│   ├── Dockerfile
│   └── package.json
├── docker-compose.yml
├── start.sh                  # Script de demarrage Docker
├── stop.sh                   # Script d'arret Docker
├── .env                      # Variables d'environnement (a creer)
└── README.md
```

## Specifications Metier

- **Cursus Standardise:** 5 niveaux (Pion, Cavalier, Fou, Tour, Dame). L'eleve progresse meme s'il change de prof.
- **Quiz d'Evaluation:** 25 questions (5 par niveau) pour determiner le niveau initial
- **Reservations:** Sessions d'une heure via Jitsi Meet
- **Disponibilites:** Le prof cree des creneaux d'au moins 1h pour permettre une reservation
- **Abonnements:** 69€/mois (3 cours/mois), 129€/mois (6 cours/mois), 179€/mois (9 cours/mois)
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

### Disponibilites (`/api/availabilities`)
| Methode | Endpoint              | Description                     | Auth    |
|---------|-----------------------|---------------------------------|---------|
| POST    | `/`                   | Creer une disponibilite         | TEACHER |
| GET     | `/me`                 | Mes disponibilites              | TEACHER |
| DELETE  | `/{id}`               | Supprimer une disponibilite     | TEACHER |
| GET     | `/teacher/{id}/slots` | Creneaux disponibles d'un prof  | Non     |

### Cours (`/api/lessons`)
| Methode | Endpoint          | Description                     | Auth    |
|---------|-------------------|---------------------------------|---------|
| POST    | `/book`           | Reserver un cours               | STUDENT |
| GET     | `/upcoming`       | Cours a venir                   | JWT     |
| GET     | `/history`        | Historique des cours            | JWT     |
| PATCH   | `/{id}/confirm`   | Confirmer un cours              | TEACHER |
| PATCH   | `/{id}/cancel`    | Annuler un cours                | JWT     |
| PATCH   | `/{id}/complete`  | Marquer comme termine           | TEACHER |

### Paiements (`/api/payments`)
| Methode | Endpoint                | Description                     | Auth    |
|---------|-------------------------|---------------------------------|---------|
| GET     | `/config`               | Configuration Stripe publique   | Non     |
| GET     | `/plans`                | Liste des plans d'abonnement    | Non     |
| POST    | `/checkout/subscription`| Creer session Stripe Checkout   | STUDENT |
| POST    | `/checkout/lesson`      | Payer un cours a l'unite        | STUDENT |
| GET     | `/subscription`         | Abonnement actif                | STUDENT |

## Variables d'Environnement

Creer un fichier `.env` a la racine du projet :

```env
# Stripe (obligatoire pour les paiements)
STRIPE_SECRET_KEY=sk_test_...
STRIPE_PUBLISHABLE_KEY=pk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
```

Variables configurees dans `docker-compose.yml` :

| Variable                    | Description                  | Defaut |
|-----------------------------|------------------------------|--------|
| `POSTGRES_DB`               | Nom de la base               | chessconnect |
| `POSTGRES_USER`             | Utilisateur PostgreSQL       | chess |
| `POSTGRES_PASSWORD`         | Mot de passe PostgreSQL      | chess123 |
| `FRONTEND_URL`              | URL du frontend              | http://localhost:4200 |
| `TZ`                        | Fuseau horaire               | Europe/Paris |

## Stack Technique

| Composant | Technologies |
|-----------|-------------|
| Backend   | Spring Boot 3.2, Spring Security (JWT), Spring Data JPA |
| Frontend  | Angular 17, Signals, Standalone Components |
| Database  | PostgreSQL 16 |
| DevOps    | Docker, Docker Compose, Nginx |
| Paiements | Stripe (Embedded Checkout) |
| Video     | Jitsi Meet (gratuit, sans compte) |

## Architecture Docker

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│    Frontend     │     │    Backend      │     │   PostgreSQL    │
│  (Angular/Nginx)│────>│  (Spring Boot)  │────>│   (Database)    │
│   Port: 4200    │     │   Port: 8282    │     │   Port: 5433    │
└─────────────────┘     └─────────────────┘     └─────────────────┘
        │                       │
        │   Proxy /api/*        │
        └───────────────────────┘
```

## Troubleshooting

### Le backend ne demarre pas
```bash
docker logs chessconnect-backend --tail 50
```

### Probleme de fuseau horaire
Verifier que le conteneur est en heure Paris :
```bash
docker exec chessconnect-backend date
```

### Les paiements ne fonctionnent pas
Verifier que le fichier `.env` existe et contient les cles Stripe :
```bash
cat .env
```

### CORS errors
Verifier que `FRONTEND_URL` dans `docker-compose.yml` correspond a l'URL utilisee.

## Fonctionnalites a Implementer

### 1. Evaluation des Professeurs
- Apres la fin d'un cours, l'eleve peut evaluer le professeur sur 5 etoiles
- La note moyenne est affichee sur le profil du professeur

### 2. Professeurs Favoris et Abonnement
- L'eleve peut mettre un professeur en favori pour le voir en haut de la liste lors de la reservation
- Bouton "S'abonner" pour recevoir un email quand le professeur publie de nouveaux creneaux

### 3. Mot de Passe Oublie
- Ajouter un lien "Mot de passe oublie ?" sur la page de connexion
- Envoyer un token par email pour reinitialiser le mot de passe

### 4. Integration Google Calendar
- Ajouter automatiquement les creneaux reserves dans l'agenda Google de l'eleve et du professeur

### 5. Rappel par Email
- Envoyer un email de rappel 1 heure avant le cours
- Option configurable dans les preferences utilisateur

### 6. Back-Office Administrateur
- Gestion des eleves et professeurs
- Comptabilite : CA, commissions (10%)
- Gestion des remboursements avec motif
- Modification des abonnements
- Restrictions et moderation

### 7. Gestion des Conflits Horaires (Etudiant)
- Un etudiant ne peut pas reserver le meme creneau horaire chez deux professeurs differents
- Il doit annuler la premiere reservation pour pouvoir en faire une autre au meme horaire

### 8. Gestion des Conflits de Creneaux (Professeur)
- Les creneaux de disponibilite d'un professeur ne doivent pas se chevaucher
- Afficher un message d'erreur en cas de conflit

### 9. Langues Parlees du Professeur
- Le professeur renseigne les langues qu'il parle a l'inscription ou dans ses preferences
- Les langues sont affichees sur son profil
