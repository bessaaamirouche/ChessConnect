# mychess - Plateforme de Cours d'Echecs

Plateforme de mise en relation Profs/Joueurs d'echecs avec systeme de progression standardise.

## Demarrage Rapide (Docker)

**Prerequis:** Uniquement [Docker Desktop](https://www.docker.com/products/docker-desktop/) installe et en cours d'execution.

```bash
# 1. Cloner le projet
git clone https://github.com/bessaaamirouche/mychess.git
cd mychess

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

### Comptes de Test

Apres le premier demarrage, creez un compte via l'interface:

1. Allez sur http://localhost:4200
2. Cliquez sur "S'inscrire"
3. Choisissez "Joueur" ou "Coach"
4. Remplissez le formulaire

## Deploiement sur VPS

### 1. Installer Docker sur le serveur

```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER && newgrp docker
```

### 2. Cloner et configurer

```bash
git clone https://github.com/bessaaamirouche/mychess.git
cd mychess

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

- **Inscription Joueur/Coach** : Creation de compte avec roles et badges visuels
- **Premier Cours Offert** : Les nouveaux joueurs beneficient d'un premier cours gratuit
- **Quiz d'Evaluation** : Determinez votre niveau d'echecs (Pion, Cavalier, Fou, Tour, Dame)
- **Parcours d'Apprentissage** : 50 cours structures par niveau
- **Reservation de Cours** : Reservez des sessions avec des coachs
- **Disponibilites 24h/24** : Les coachs peuvent creer des creneaux a n'importe quelle heure
- **Reservations urgentes** : Les creneaux restent visibles jusqu'a 5 min apres l'heure de debut
- **Suivi de Progression** : Suivez votre parcours d'apprentissage
- **Abonnement Premium** : 4,99€/mois avec fonctionnalités exclusives (revisionnage, notifications prioritaires, badge Premium)
- **Paiements Stripe** : Paiements securises integres (mode test)
- **Video Jitsi Meet** : Cours en visioconference integree
- **Notifications en temps reel** : Alertes pour nouvelles disponibilites et reservations
- **Blog SEO** : Articles optimises pour le referencement
- **Design Apple-like** : Animations scroll, sections immersives, effets parallax

### Systeme d'annulation et remboursement

- **Prof ne confirme pas sous 24h** : Annulation auto + remboursement 100%
- **Prof annule** : Remboursement 100%
- **Joueur annule > 24h avant** : Remboursement 100%
- **Joueur annule 2-24h avant** : Remboursement 50%
- **Joueur annule < 2h avant** : Pas de remboursement
- Affichage dynamique du statut : "Annule par moi" / "Annule par le coach" / "Annule par le joueur" / "Annule (auto)"

### Abonnement Premium (4,99€/mois)

Fonctionnalités exclusives pour les abonnés Premium :
- **Revisionnage des cours** : Accès aux enregistrements vidéo dans l'historique
- **Notifications prioritaires** : Alertes email quand les coachs favoris publient des créneaux
- **Badge Premium** : Badge doré visible sur le profil

### Premier Cours Offert

- Les nouveaux joueurs peuvent reserver leur premier cours gratuitement
- Banniere promotionnelle sur la page de reservation
- Bouton vert "Reserver gratuitement" au lieu du paiement classique
- Tracking via le champ `hasUsedFreeTrial` sur le User
- Endpoints API : `GET /api/lessons/free-trial/eligible`, `POST /api/lessons/free-trial/book`

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
/mychess
├── /backend-api              # API Spring Boot
│   ├── /src/main/java/com/chessconnect
│   │   ├── /config           # SecurityConfig, StripeConfig, QuizDataInitializer
│   │   ├── /controller       # REST Controllers
│   │   ├── /dto              # Data Transfer Objects
│   │   ├── /model            # Entites JPA (User avec hasUsedFreeTrial)
│   │   ├── /repository       # Repositories Spring Data
│   │   ├── /security         # JWT (JwtService, JwtAuthenticationFilter)
│   │   └── /service          # Services metier (LessonService avec free trial)
│   ├── Dockerfile
│   └── pom.xml
├── /frontend-web             # Application Angular 17
│   ├── /src/app
│   │   ├── /core             # Services, Guards, Interceptors, Models
│   │   ├── /features         # Composants par fonctionnalite
│   │   │   ├── /admin        # Back-office administrateur
│   │   │   ├── /auth         # Login, Register, Forgot/Reset Password
│   │   │   ├── /availability # Gestion des disponibilites (prof)
│   │   │   ├── /blog         # Articles SEO (liste + detail)
│   │   │   ├── /dashboard    # Tableau de bord avec badges de role
│   │   │   ├── /home         # Landing page style Apple
│   │   │   ├── /lessons      # Reservation (avec premier cours offert) et liste
│   │   │   ├── /progress     # Suivi de progression
│   │   │   ├── /quiz         # Quiz d'evaluation de niveau
│   │   │   ├── /subscription # Gestion des abonnements
│   │   │   └── /teachers     # Liste et profil des coachs
│   │   └── /shared           # Composants partages (toast, modals, video-call, etc.)
│   ├── /src/assets
│   │   ├── logo.svg          # Logo vectoriel mychess
│   │   ├── logo.png          # Logo PNG 400x400
│   │   ├── og-image.jpg      # Image Open Graph 1200x630
│   │   └── /icons            # Icones PWA (72-512px), favicons, apple-touch-icon
│   ├── /src/styles
│   │   ├── _variables.scss   # Variables CSS et theme
│   │   ├── _animations.scss  # Animations scroll reveal
│   │   └── _mobile.scss      # Styles responsive mobile
│   ├── Dockerfile
│   └── package.json
├── docker-compose.yml
├── start.sh                  # Script de demarrage Docker
├── stop.sh                   # Script d'arret Docker
├── .env                      # Variables d'environnement (a creer)
└── CLAUDE.md
```

## Specifications Metier

- **Cursus Standardise:** 5 niveaux (Pion, Cavalier, Fou, Tour, Dame). Le joueur progresse meme s'il change de prof.
- **Quiz d'Evaluation:** 25 questions (5 par niveau) pour determiner le niveau initial
- **Reservations:** Sessions d'une heure via Jitsi Meet
- **Disponibilites:** Le coach cree des creneaux d'au moins 1h pour permettre une reservation
- **Abonnement Premium:** 4,99€/mois pour fonctionnalités exclusives (pas de quota de cours)
- **Paiement des cours:** Directement au tarif du coach (30€-80€/h)
- **Commission:** 10% prélevés par la plateforme
- **Premier Cours Offert:** Un cours gratuit pour les nouveaux joueurs

### Gestion des cours (Lessons)

- Reservation de cours avec un coach
- Premier cours offert pour les nouveaux joueurs
- Confirmation/Annulation par le coach
- Statuts : PENDING, CONFIRMED, COMPLETED, CANCELLED, NO_SHOW
- Affichage des informations joueur pour le coach (niveau, age, ELO)
- Raison d'annulation visible (tooltip)
- Suppression de l'historique
- Visioconference integree (Jitsi)

### Progression (Learning Path)

- Niveaux d'echecs : Pion, Cavalier, Fou, Tour, Dame
- Cours par niveau (grades) avec accordeon
- Statuts cours : LOCKED, IN_PROGRESS, PENDING_VALIDATION, COMPLETED
- Validation des cours par le coach uniquement
- Modale coachil joueur (cote prof) avec progression et validation

### Interface Utilisateur

- **Badges de role** : Joueur (bleu), Coach (violet), Admin (dore) dans la sidebar
- **Design responsive** : Optimise pour mobile avec touch targets accessibles
- **Landing page** : Style Apple avec animations au scroll, sections plein ecran
- **Theme sombre** : Interface elegante avec accents dores
- **Notifications toast** : Cliquables sans fleches, croix pour fermer
- **Menu hamburger mobile** : Fixe, semi-transparent (30% opacite), toujours visible
- **Footer** : Logo et tagline sur la meme ligne

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

### Coachs (`/api/teachers`)
| Methode | Endpoint        | Description                        | Auth |
|---------|-----------------|------------------------------------|------|
| GET     | `/`             | Liste tous les coachs         | Non  |
| GET     | `/{id}`         | Detail d'un coach             | Non  |

### Disponibilites (`/api/availabilities`)
| Methode | Endpoint              | Description                     | Auth    |
|---------|-----------------------|---------------------------------|---------|
| POST    | `/`                   | Creer une disponibilite         | TEACHER |
| GET     | `/me`                 | Mes disponibilites              | TEACHER |
| DELETE  | `/{id}`               | Supprimer une disponibilite     | TEACHER |
| GET     | `/teacher/{id}/slots` | Creneaux disponibles d'un coach  | Non     |

### Cours (`/api/lessons`)
| Methode | Endpoint               | Description                     | Auth    |
|---------|------------------------|---------------------------------|---------|
| POST    | `/book`                | Reserver un cours               | STUDENT |
| GET     | `/free-trial/eligible` | Verifier eligibilite 1er cours  | STUDENT |
| POST    | `/free-trial/book`     | Reserver le 1er cours gratuit   | STUDENT |
| GET     | `/upcoming`            | Cours a venir                   | JWT     |
| GET     | `/history`             | Historique des cours            | JWT     |
| PATCH   | `/{id}/confirm`        | Confirmer un cours              | TEACHER |
| PATCH   | `/{id}/cancel`         | Annuler un cours                | JWT     |
| PATCH   | `/{id}/complete`       | Marquer comme termine           | TEACHER |

### Paiements (`/api/payments`)
| Methode | Endpoint                | Description                     | Auth    |
|---------|-------------------------|---------------------------------|---------|
| GET     | `/config`               | Configuration Stripe publique   | Non     |
| GET     | `/plans`                | Liste des plans d'abonnement    | Non     |
| POST    | `/checkout/subscription`| Creer session Stripe Checkout   | STUDENT |
| POST    | `/checkout/lesson`      | Payer un cours a l'unite        | STUDENT |
| GET     | `/subscription`         | Abonnement actif                | STUDENT |

### Stripe Connect (`/api/stripe-connect`)
| Methode | Endpoint        | Description                           | Auth    |
|---------|-----------------|---------------------------------------|---------|
| POST    | `/onboarding`   | Demarre l'onboarding Stripe Connect   | TEACHER |
| GET     | `/status`       | Statut du compte Stripe Connect       | TEACHER |
| POST    | `/refresh-link` | Rafraichir le lien d'onboarding       | TEACHER |
| DELETE  | `/disconnect`   | Deconnecter le compte Stripe Connect  | TEACHER |

### Articles/Blog (`/api/articles`)
| Methode | Endpoint              | Description                     | Auth    |
|---------|-----------------------|---------------------------------|---------|
| GET     | `/`                   | Liste des articles publies      | Non     |
| GET     | `/{slug}`             | Detail d'un article par slug    | Non     |
| GET     | `/category/{cat}`     | Articles par categorie          | Non     |
| POST    | `/`                   | Creer un article                | ADMIN   |
| PUT     | `/{id}`               | Modifier un article             | ADMIN   |
| DELETE  | `/{id}`               | Supprimer un article            | ADMIN   |

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
| Frontend  | Angular 17, Signals, Standalone Components, RxJS, ng-icons, SCSS |
| Database  | PostgreSQL 16 |
| DevOps    | Docker, Docker Compose, Nginx |
| Paiements | Stripe (Embedded Checkout) |
| Video     | Jitsi Meet (gratuit, sans compte) |
| Images    | Sharp (generation logo/icones) |

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

## Logo et Assets

Le logo mychess :

- **Design** : Cavalier d'echecs en bois avec texture effet bois moderne
- **Couleurs** : Degrade brun/orange (#8B4513 → #CD853F → #DEB887)
- **Fond** : Transparent
- **Fichiers** :
  - `logo.png` - Logo cavalier seul avec fond transparent (248x375)
  - `og-image.jpg` - 1200x630 pour partage social
  - `icons/icon-*.png` - PWA (72-512px)
  - `icons/apple-touch-icon.png` - 180x180
  - `icons/favicon-*.png` - 16x16, 32x32
  - `favicon.ico` - Multi-resolution

## Troubleshooting

### Le backend ne demarre pas
```bash
docker logs chessconnect-backend --tail 50
```

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

## Problemes Connus

### Enregistrement video Jitsi
- **Statut** : Non fonctionnel
- L'enregistrement automatique des cours via Jitsi Meet ne fonctionne pas encore
- La commande `startRecording` est appelee mais Jitsi necessite une configuration serveur specifique (Jibri) pour l'enregistrement
- A investiguer : configuration Jibri sur le serveur meet.mychess.fr ou solution alternative

### Paiement des coachs via Stripe Connect
- **Statut** : Implemente
- Les coachs peuvent configurer leur compte Stripe Connect dans les parametres
- L'admin peut effectuer des virements reels via le bouton "Transferer" dans la comptabilite
- Le transfert Stripe est enregistre avec l'ID de transaction
- Pre-requis pour recevoir des paiements : compte Stripe Connect configure et verifie

## Fonctionnalites a Implementer

### 1. Evaluation des Coachs
- Apres la fin d'un cours, le joueur peut evaluer le coach sur 5 etoiles
- La note moyenne est affichee sur le coachil du coach

### 2. Coachs Favoris et Abonnement
- Le joueur peut mettre un coach en favori pour le voir en haut de la liste lors de la reservation
- Bouton "S'abonner" pour recevoir un email quand le coach publie de nouveaux creneaux

### 3. Integration Google Calendar
- Ajouter automatiquement les creneaux reserves dans l'agenda Google de le joueur et du coach

### 4. Rappel par Email
- Envoyer un email de rappel 1 heure avant le cours
- Option configurable dans les preferences utilisateur

### 5. Enregistrement des appels video
Lorsqu'un appel video est lance, la session doit etre :
- Enregistree automatiquement
- Stockee sur le serveur
- Accessible ulterieurement pour consultation par l'admin en cas d'infraction du coach (signale par l'etudiant) et accessible dans l'historique des cours de chaque joueur pour revisionner le cours
