# mychess - Plateforme de Cours d'Echecs

Plateforme de mise en relation Profs/Eleves d'echecs avec systeme de progression standardise.

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
3. Choisissez "Eleve" ou "Professeur"
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

- **Inscription Eleve/Professeur** : Creation de compte avec roles et badges visuels
- **Premier Cours Offert** : Les nouveaux eleves beneficient d'un premier cours gratuit
- **Quiz d'Evaluation** : Determinez votre niveau d'echecs (Pion, Cavalier, Fou, Tour, Dame)
- **Parcours d'Apprentissage** : 50 cours structures par niveau
- **Reservation de Cours** : Reservez des sessions avec des professeurs
- **Disponibilites 24h/24** : Les professeurs peuvent creer des creneaux a n'importe quelle heure
- **Reservations urgentes** : Les creneaux restent visibles jusqu'a 5 min apres l'heure de debut
- **Suivi de Progression** : Suivez votre parcours d'apprentissage
- **Abonnements** : 3 formules (1, 2 ou 3 cours/semaine)
- **Paiements Stripe** : Paiements securises integres (mode test)
- **Video Jitsi Meet** : Cours en visioconference integree
- **Notifications en temps reel** : Alertes pour nouvelles disponibilites et reservations
- **Blog SEO** : Articles optimises pour le referencement
- **Design Apple-like** : Animations scroll, sections immersives, effets parallax

### Systeme d'annulation et remboursement

- **Prof ne confirme pas sous 24h** : Annulation auto + remboursement 100%
- **Prof annule** : Remboursement 100%
- **Eleve annule > 24h avant** : Remboursement 100%
- **Eleve annule 2-24h avant** : Remboursement 50%
- **Eleve annule < 2h avant** : Pas de remboursement
- **Cours abonnement** : Quota restaure (sauf annulation tardive eleve)
- Affichage dynamique du statut : "Annule par moi" / "Annule par le prof" / "Annule par l'eleve" / "Annule (auto)"

### Premier Cours Offert

- Les nouveaux eleves peuvent reserver leur premier cours gratuitement
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
│   │   │   └── /teachers     # Liste et profil des profs
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

- **Cursus Standardise:** 5 niveaux (Pion, Cavalier, Fou, Tour, Dame). L'eleve progresse meme s'il change de prof.
- **Quiz d'Evaluation:** 25 questions (5 par niveau) pour determiner le niveau initial
- **Reservations:** Sessions d'une heure via Jitsi Meet
- **Disponibilites:** Le prof cree des creneaux d'au moins 1h pour permettre une reservation
- **Abonnements:** 69€/mois (3 cours/mois), 129€/mois (6 cours/mois), 179€/mois (9 cours/mois)
- **Commission:** 10% preleves par la plateforme
- **Premier Cours Offert:** Un cours gratuit pour les nouveaux eleves

### Gestion des cours (Lessons)

- Reservation de cours avec un professeur
- Premier cours offert pour les nouveaux eleves
- Confirmation/Annulation par le professeur
- Statuts : PENDING, CONFIRMED, COMPLETED, CANCELLED, NO_SHOW
- Affichage des informations eleve pour le prof (niveau, age, ELO)
- Raison d'annulation visible (tooltip)
- Suppression de l'historique
- Visioconference integree (Jitsi)

### Progression (Learning Path)

- Niveaux d'echecs : Pion, Cavalier, Fou, Tour, Dame
- Cours par niveau (grades) avec accordeon
- Statuts cours : LOCKED, IN_PROGRESS, PENDING_VALIDATION, COMPLETED
- Validation des cours par le professeur uniquement
- Modale profil eleve (cote prof) avec progression et validation

### Interface Utilisateur

- **Badges de role** : Eleve (bleu), Professeur (violet), Admin (dore) dans la sidebar
- **Design responsive** : Optimise pour mobile avec touch targets accessibles
- **Landing page** : Style Apple avec animations au scroll, sections plein ecran
- **Theme sombre** : Interface elegante avec accents dores

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

Le logo mychess a ete genere avec Sharp :

- **Design** : Cavalier d'echecs stylise en degrade dore sur fond sombre
- **Couleurs** : Or (#f5d485 → #d4a84b → #b8860b) sur noir (#1a1a1f)
- **Fichiers** :
  - `logo.svg` - Vectoriel principal
  - `logo.png` - 400x400 avec texte
  - `og-image.jpg` - 1200x630 pour partage social
  - `icons/icon-*.png` - PWA (72-512px) sans texte
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

### Paiement des professeurs via Stripe
- **Statut** : Non implemente
- Actuellement le bouton "Marquer comme paye" ne fait que changer le statut visuellement
- **Requis** : Un vrai bouton qui effectue un virement Stripe (Stripe Connect / Transfer) vers le compte du professeur sans quitter l'application
- Le professeur doit pouvoir renseigner ses informations bancaires (RIB, infos entreprise) dans son profil
- A implementer : Stripe Connect Onboarding pour les profs + API Transfer pour les paiements admin

## Fonctionnalites a Implementer

### 1. Evaluation des Professeurs
- Apres la fin d'un cours, l'eleve peut evaluer le professeur sur 5 etoiles
- La note moyenne est affichee sur le profil du professeur

### 2. Professeurs Favoris et Abonnement
- L'eleve peut mettre un professeur en favori pour le voir en haut de la liste lors de la reservation
- Bouton "S'abonner" pour recevoir un email quand le professeur publie de nouveaux creneaux

### 3. Integration Google Calendar
- Ajouter automatiquement les creneaux reserves dans l'agenda Google de l'eleve et du professeur

### 4. Rappel par Email
- Envoyer un email de rappel 1 heure avant le cours
- Option configurable dans les preferences utilisateur

### 5. Enregistrement des appels video
Lorsqu'un appel video est lance, la session doit etre :
- Enregistree automatiquement
- Stockee sur le serveur
- Accessible ulterieurement pour consultation par l'admin en cas d'infraction du prof (signale par l'etudiant) et accessible dans l'historique des cours de chaque eleve pour revisionner le cours
