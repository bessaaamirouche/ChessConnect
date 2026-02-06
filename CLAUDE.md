# mychess - Plateforme de Cours d'Echecs

Plateforme de mise en relation Profs/Joueurs d'echecs avec systeme de progression standardise.

## REGLES IMPORTANTES POUR CLAUDE

### Environnement Production (ports 4200, 8282)

- NE JAMAIS rebuild les containers sauf demande explicite
- Mots-clés pour déployer : "applique", "patch", "mise à jour", "déploie", "rebuild"

### Commandes de rebuild
```bash
# Backend seulement
docker compose up -d backend --build

# Frontend seulement
docker compose up -d frontend --build

# Les deux
docker compose up -d --build
```

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

| Service | URL |
|---------|-----|
| Frontend | http://localhost:4200 |
| Backend | http://localhost:8282/api |
| Database | interne (PostgreSQL) |

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

## Fonctionnalites

### Fonctionnalites Principales
- **Inscription Joueur/Coach** : Creation de compte avec roles et badges visuels
- **Premier Cours Offert** : Les nouveaux joueurs beneficient d'un premier cours gratuit
- **Quiz d'Evaluation** : Determinez votre niveau d'echecs (A, B, C, D)
- **Parcours d'Apprentissage** : 120 cours structures par niveau, 546 lecons
- **Reservation de Cours** : Reservez des sessions avec des coachs
- **Disponibilites 24h/24** : Les coachs peuvent creer des creneaux a n'importe quelle heure
- **Reservations urgentes** : Les creneaux restent visibles jusqu'a 5 min apres l'heure de debut
- **Suivi de Progression** : Suivez votre parcours d'apprentissage
- **Paiements Stripe** : Paiements securises integres (mode test)
- **Portefeuille (Wallet)** : Credit de solde pour payer les cours, remboursements automatiques
- **Factures PDF** : Generation automatique avec mentions legales, avoir pour remboursements
- **Video Jitsi Meet** : Cours en visioconference integree avec theme Premium mychess
- **Indicateur de presence** : Pastille verte quand un coach est en ligne
- **Blog SEO** : Articles optimises pour le referencement avec support Markdown
- **Design Apple-like** : Animations scroll, sections immersives, effets parallax
- **Evaluation des Coachs** : Systeme de notation 5 etoiles apres chaque cours avec commentaires
- **Coachs Favoris** : Ajoutez vos coachs preferes en favoris avec notifications de nouveaux creneaux
- **Google Calendar** : Synchronisation automatique des cours avec votre agenda Google
- **Rappels Email** : Notification automatique 1 heure avant chaque cours
- **Enregistrement Video** : Les cours sont enregistres via Jibri pour revisionnage (Premium)

### Internationalisation (i18n) 🌍

Support multilingue complet avec 2 langues :
- **Francais** (`/assets/i18n/fr.json`) - Langue par defaut
- **Anglais** (`/assets/i18n/en.json`)

**Implementation :**
- Librairie `@ngx-translate/core`
- `LanguageService` - Gestion du changement de langue avec persistance localStorage
- `LanguageSelectorComponent` - Selecteur de langue dans les settings
- 1795 cles de traduction par langue couvrant toute l'application

**Zones traduites :**
- Navigation, hero, features, pricing, FAQ
- Authentification (login, register, forgot password)
- Dashboard, lessons, teachers, progress
- Wallet, subscription, invoices
- Settings, availability, ratings
- Blog, library, exercise
- Messages d'erreur, validations, statuts

### Notifications Push Web 🔔

Notifications push navigateur (Web Push API) pour notifier les utilisateurs meme quand l'app est fermee.

**Backend :**
- `WebPushService.java` - Gestion des subscriptions et envoi des notifications
- `PushController.java` - Endpoints REST pour la gestion push
- `WebPushConfig.java` - Configuration VAPID (cles auto-generees si non definies)

**Frontend :**
- `PushNotificationService` - Gestion client des subscriptions push
- `sw-push.js` - Service Worker pour recevoir les notifications
- Integration dans Settings pour activer/desactiver

**Cas d'usage :**
- Nouveau creneau d'un coach favori
- Confirmation/annulation de cours
- Rappel 1h avant le cours
- Nouvelle reservation (pour les coachs)

**Compatibilite :**
- Android Chrome : ✅ Excellent
- Desktop (Chrome, Firefox, Edge) : ✅ Excellent
- iOS Safari : ⚠️ Uniquement si PWA installee (iOS 16.4+)

### Notifications Temps Reel (SSE) 📡

Server-Sent Events pour les notifications en temps reel quand l'app est ouverte.

**Backend :**
- `SseConnectionManager.java` - Gestion des connexions SSE actives
- `UserNotificationService.java` - Orchestration des notifications

**Frontend :**
- `NotificationService` - Client SSE avec reconnexion automatique

**Logique intelligente :** Si l'utilisateur est connecte via SSE, les notifications push ne sont pas envoyees (evite les doublons).

### Bibliotheque Video (Premium) 📚

Fonctionnalite exclusive pour les abonnes Premium permettant de revoir les cours enregistres.

**Backend :**
- `LibraryController.java` - Endpoints `/library/videos`
- Integration avec `LessonRepository.findLibraryVideos()`

**Frontend :**
- `LibraryComponent` - Interface de la bibliotheque video
- Route : `/library`

**Fonctionnalites :**
- Liste des cours enregistres avec vignettes
- Recherche par nom de coach
- Filtres par periode (7j, 30j, 3 mois, annee)
- Filtre par plage de dates personnalisee
- Suppression soft des videos
- Lecteur video integre avec reprise de lecture

### Integration Bunny CDN 🐰

Hebergement video avec Bunny.net pour le streaming des enregistrements.

**Services :**
- `BunnyStreamService.java` - API Bunny Stream (transcoding HLS)
- `BunnyStorageService.java` - Stockage direct CDN
- `ThumbnailService.java` - Generation de vignettes

**Variables d'environnement :**
```env
BUNNY_STREAM_API_KEY=
BUNNY_STREAM_LIBRARY_ID=590076
BUNNY_STREAM_CDN_HOSTNAME=vz-34fe20be-093.b-cdn.net
BUNNY_STORAGE_ZONE=mychess
BUNNY_STORAGE_API_KEY=
BUNNY_CDN_URL=https://mychess.b-cdn.net
```

### Abonnement Premium (4,99€/mois) ⭐

Fonctionnalites exclusives pour les abonnes Premium :
- **Bibliotheque Video** : Acces aux enregistrements de cours
- **Reprise de lecture** : Progression video sauvegardee
- **Notifications prioritaires** : Alertes email quand les coachs favoris publient des creneaux
- **Badge Premium** : Badge dore visible sur le profil
- **Entrainement myChessBot** : Jouez contre le bot apres vos cours

### Interface Jitsi Premium 🎥

Configuration personnalisee de Jitsi Meet aux couleurs mychess :
- Theme sombre avec accents dores
- Logo mychess integre
- Watermarks Jitsi masques
- Toolbar complete avec toutes les fonctionnalites
- CSS injecte dynamiquement pour le style premium
- Fond par defaut : `#0d0d0f`
- Couleur accent : `#d4a84b` (or)

### Systeme d'annulation et remboursement

- **Prof ne confirme pas sous 24h** : Annulation auto + remboursement 100%
- **Prof annule** : Remboursement 100%
- **Joueur annule > 24h avant** : Remboursement 100%
- **Joueur annule 2-24h avant** : Remboursement 50%
- **Joueur annule < 2h avant** : Pas de remboursement
- Affichage dynamique du statut : "Annule par moi" / "Annule par le coach" / "Annule par le joueur" / "Annule (auto)"

### Premier Cours Offert

- Les nouveaux joueurs peuvent reserver leur premier cours gratuitement
- Banniere promotionnelle sur la page de reservation
- Bouton vert "Reserver gratuitement" au lieu du paiement classique
- Tracking via le champ `hasUsedFreeTrial` sur le User
- Endpoints API : `GET /api/lessons/free-trial/eligible`, `POST /api/lessons/free-trial/book`

### Portefeuille (Wallet)

Systeme de credit pour les joueurs :
- **Crediter son solde** : Paiement Stripe pour ajouter des credits (minimum 10€)
- **Payer avec le solde** : Option de paiement lors de la reservation si solde suffisant
- **Remboursements automatiques** : Les annulations creditent le portefeuille (pas de remboursement Stripe)
- **Historique des transactions** : Liste des credits, debits et remboursements
- Endpoints API : `GET /api/wallet/balance`, `POST /api/wallet/credit`, `GET /api/wallet/transactions`

### Factures et Avoirs

Systeme de facturation conforme :
- **Generation automatique** : Facture PDF a chaque paiement (cours ou credit wallet)
- **Mentions legales** : CANDLE (SIREN, TVA non applicable art. 293B CGI)
- **Avoirs (Credit Notes)** : Generation automatique lors des remboursements
- **Filtres par date** : Recherche par plage de dates dans l'historique
- **Telechargement PDF** : Ouverture dans un nouvel onglet
- Endpoints API : `GET /api/invoices/me`, `GET /api/invoices/{id}/pdf`

### Entrainement contre myChessBot (Premium)

Module d'exercice contre myChessBot accessible depuis l'historique des cours :
- **myChessBot** : Moteur d'echecs JavaScript leger (calcul cote client, pas de charge serveur)
- **Chessground** : Echiquier interactif avec animations
- **Chess.js** : Validation des coups et regles du jeu
- **Difficulte adaptative** : 5 niveaux (Debutant, Facile, Moyen, Difficile, Expert) mappes sur le niveau du joueur
- **Evaluation en temps reel** : Barre d'evaluation de la position
- **Detection fin de partie** : Mat, pat, nulle
- Bouton "M'exercer" dans l'historique des cours (visible uniquement pour Premium)
- Route : `/exercise/:lessonId` protegee par `premiumGuard`

## Structure du Projet

```
/mychess
├── /backend-api              # API Spring Boot
│   ├── /src/main/java/com/chessconnect
│   │   ├── /config           # SecurityConfig, StripeConfig, WebPushConfig, CacheConfig
│   │   ├── /controller       # REST Controllers (+ PushController, LibraryController)
│   │   ├── /dto              # Data Transfer Objects
│   │   ├── /model            # Entites JPA (User, PushSubscription, etc.)
│   │   ├── /repository       # Repositories Spring Data
│   │   ├── /security         # JWT, RateLimiting
│   │   └── /service          # Services metier (WebPushService, BunnyStreamService, etc.)
│   ├── /src/main/resources
│   │   └── /db/migration     # Migrations Flyway (V1-V19)
│   ├── Dockerfile
│   └── pom.xml
├── /frontend-web             # Application Angular 17
│   ├── /src/app
│   │   ├── /core             # Services, Guards, Interceptors, Models
│   │   │   └── /services     # PushNotificationService, LanguageService, LibraryService
│   │   ├── /features         # Composants par fonctionnalite
│   │   │   ├── /admin        # Back-office administrateur
│   │   │   ├── /auth         # Login, Register, Forgot/Reset Password
│   │   │   ├── /availability # Gestion des disponibilites (prof)
│   │   │   ├── /blog         # Articles SEO
│   │   │   ├── /dashboard    # Tableau de bord
│   │   │   ├── /exercise     # Entrainement myChessBot (Premium)
│   │   │   ├── /home         # Landing page
│   │   │   ├── /lessons      # Reservation et liste des cours
│   │   │   ├── /library      # Bibliotheque video (Premium)
│   │   │   ├── /progress     # Suivi de progression
│   │   │   ├── /quiz         # Quiz d'evaluation
│   │   │   ├── /settings     # Parametres utilisateur
│   │   │   ├── /subscription # Gestion des abonnements
│   │   │   ├── /teachers     # Liste et profil des coachs
│   │   │   ├── /wallet       # Portefeuille
│   │   │   └── /invoices     # Factures
│   │   └── /shared           # Composants partages (video-call, language-selector, etc.)
│   ├── /src/assets
│   │   ├── /i18n             # Fichiers de traduction (en.json, fr.json)
│   │   ├── /icons            # Icones PWA
│   │   ├── logo.png          # Logo mychess
│   │   └── og-image.jpg      # Image Open Graph
│   ├── /src/styles
│   │   ├── _variables.scss   # Variables CSS (theme sombre + accents dores)
│   │   ├── _animations.scss  # Animations scroll reveal
│   │   └── _mobile.scss      # Styles responsive
│   ├── sw-push.js            # Service Worker pour Push
│   ├── Dockerfile
│   └── package.json
├── docker-compose.yml        # Config Docker
├── .env                      # Variables d'environnement (a creer)
├── .env.example              # Exemple de configuration
├── start.sh / stop.sh        # Scripts Docker
└── CLAUDE.md
```

## API Endpoints

### Authentification (`/api/auth`)
| Methode | Endpoint    | Description                | Auth |
|---------|-------------|----------------------------|------|
| POST    | `/register` | Inscription d'un utilisateur | Non  |
| POST    | `/login`    | Connexion (retourne JWT)   | Non  |

### Notifications Push (`/api/push`)
| Methode | Endpoint      | Description                     | Auth    |
|---------|---------------|---------------------------------|---------|
| GET     | `/vapid-key`  | Cle publique VAPID              | Non     |
| POST    | `/subscribe`  | S'abonner aux notifications     | JWT     |
| POST    | `/unsubscribe`| Se desabonner                   | JWT     |
| GET     | `/status`     | Statut des notifications        | JWT     |
| PATCH   | `/preference` | Activer/desactiver les push     | JWT     |

### Bibliotheque Video (`/api/library`)
| Methode | Endpoint              | Description                     | Auth    |
|---------|-----------------------|---------------------------------|---------|
| GET     | `/videos`             | Liste des videos (avec filtres) | PREMIUM |
| DELETE  | `/videos/{lessonId}`  | Supprimer une video             | PREMIUM |

### Progression Video (`/api/video-progress`)
| Methode | Endpoint              | Description                     | Auth    |
|---------|-----------------------|---------------------------------|---------|
| GET     | `/{lessonId}`         | Progression d'une video         | JWT     |
| POST    | `/`                   | Sauvegarder la progression      | JWT     |

### Notifications SSE (`/api/notifications`)
| Methode | Endpoint      | Description                     | Auth    |
|---------|---------------|---------------------------------|---------|
| GET     | `/stream`     | Flux SSE temps reel             | JWT     |

### Quiz (`/api/quiz`)
| Methode | Endpoint     | Description                     | Auth    |
|---------|--------------|---------------------------------|---------|
| GET     | `/questions` | Liste des questions du quiz     | Non     |
| POST    | `/submit`    | Soumettre les reponses          | STUDENT |
| GET     | `/result`    | Dernier resultat du quiz        | STUDENT |

### Coachs (`/api/teachers`)
| Methode | Endpoint        | Description                        | Auth |
|---------|-----------------|------------------------------------|------|
| GET     | `/`             | Liste tous les coachs              | Non  |
| GET     | `/{id}`         | Detail d'un coach                  | Non  |

### Disponibilites (`/api/availabilities`)
| Methode | Endpoint              | Description                     | Auth    |
|---------|-----------------------|---------------------------------|---------|
| POST    | `/`                   | Creer une disponibilite         | TEACHER |
| GET     | `/me`                 | Mes disponibilites              | TEACHER |
| DELETE  | `/{id}`               | Supprimer une disponibilite     | TEACHER |
| GET     | `/teacher/{id}/slots` | Creneaux disponibles d'un coach | Non     |

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

### Portefeuille (`/api/wallet`)
| Methode | Endpoint        | Description                     | Auth    |
|---------|-----------------|--------------------------------|---------|
| GET     | `/balance`      | Solde actuel du portefeuille   | STUDENT |
| POST    | `/credit`       | Crediter le portefeuille       | STUDENT |
| GET     | `/transactions` | Historique des transactions    | STUDENT |

### Factures (`/api/invoices`)
| Methode | Endpoint        | Description                     | Auth    |
|---------|-----------------|--------------------------------|---------|
| GET     | `/me`           | Mes factures                   | JWT     |
| GET     | `/{id}/pdf`     | Telecharger PDF de la facture  | JWT     |
| GET     | `/` (admin)     | Toutes les factures            | ADMIN   |

### Exercices (`/api/exercises`)
| Methode | Endpoint              | Description                     | Auth    |
|---------|-----------------------|---------------------------------|---------|
| GET     | `/lesson/{lessonId}`  | Exercice lie a un cours         | PREMIUM |
| GET     | `/{id}`               | Detail d'un exercice            | PREMIUM |
| GET     | `/`                   | Liste des exercices             | PREMIUM |

### Evaluations (`/api/ratings`)
| Methode | Endpoint                       | Description                     | Auth    |
|---------|--------------------------------|---------------------------------|---------|
| POST    | `/`                            | Creer une evaluation            | STUDENT |
| GET     | `/teacher/{teacherId}`         | Evaluations d'un coach          | Non     |
| GET     | `/teacher/{teacherId}/summary` | Moyenne et nombre d'avis        | Non     |

### Favoris (`/api/favorites`)
| Methode | Endpoint                 | Description                        | Auth    |
|---------|--------------------------|------------------------------------|---------|
| POST    | `/{teacherId}`           | Ajouter un coach en favori         | STUDENT |
| DELETE  | `/{teacherId}`           | Retirer un coach des favoris       | STUDENT |
| GET     | `/`                      | Liste de mes coachs favoris        | STUDENT |
| PATCH   | `/{teacherId}/notify`    | Activer/desactiver notifications   | STUDENT |

### Enregistrements (`/api/recordings`)
| Methode | Endpoint                 | Description                        | Auth    |
|---------|--------------------------|------------------------------------|---------|
| POST    | `/webhook`               | Webhook Jibri                      | Non     |
| GET     | `/video/{lessonId}`      | Recuperer la video d'un cours      | JWT     |

## Variables d'Environnement

Creer un fichier `.env` a la racine du projet (voir `.env.example`) :

```env
# Base de donnees (obligatoire)
POSTGRES_DB=chessconnect
POSTGRES_USER=chess
POSTGRES_PASSWORD=votre_mot_de_passe_fort

# JWT (obligatoire)
JWT_SECRET=votre_secret_jwt_64_caracteres_minimum

# Stripe (obligatoire pour les paiements)
STRIPE_SECRET_KEY=sk_live_...
STRIPE_PUBLISHABLE_KEY=pk_live_...
STRIPE_WEBHOOK_SECRET=whsec_...

# Jitsi (obligatoire pour les appels video)
JITSI_APP_ID=mychess
JITSI_APP_SECRET=votre_secret_jitsi
JITSI_DOMAIN=meet.mychess.fr

# Email SMTP (optionnel)
MAIL_ENABLED=false
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=
MAIL_PASSWORD=

# Web Push Notifications (optionnel - auto-genere si vide)
VAPID_PUBLIC_KEY=
VAPID_PRIVATE_KEY=
VAPID_SUBJECT=mailto:support@mychess.fr

# Bunny CDN (optionnel - pour la bibliotheque video)
BUNNY_STREAM_API_KEY=
BUNNY_STREAM_LIBRARY_ID=590076
BUNNY_STREAM_CDN_HOSTNAME=vz-34fe20be-093.b-cdn.net
BUNNY_STORAGE_ZONE=mychess
BUNNY_STORAGE_API_KEY=
BUNNY_CDN_URL=https://mychess.b-cdn.net

# Frontend URL
FRONTEND_URL=https://mychess.fr

# Admin
ADMIN_EMAIL=support@mychess.fr
```

## Migrations Base de Donnees (Flyway)

| Version | Description |
|---------|-------------|
| V1 | Schema initial (users, lessons, etc.) |
| V2-V14 | Evolutions progressives |
| V15 | Video watch progress tracking |
| V16 | Lesson recording URLs |
| V17 | Thumbnail URLs |
| V18 | Push subscriptions table |
| V19 | Push notifications preference |

## Stack Technique

| Composant | Technologies |
|-----------|-------------|
| Backend | Spring Boot 3.2, Spring Security (JWT HttpOnly), Spring Data JPA, Caffeine Cache |
| Frontend | Angular 17, Signals, Standalone Components, RxJS, ng-icons, ngx-translate, SCSS |
| Database | PostgreSQL 16, Flyway migrations, HikariCP |
| DevOps | Docker, Docker Compose, Nginx |
| Paiements | Stripe (Embedded Checkout, Connect) |
| Video | Jitsi Meet (theme Premium), Bunny CDN |
| Push | Web Push API, VAPID, BouncyCastle |
| i18n | ngx-translate (FR, EN) |

## Securite

### Backend
- **JWT HttpOnly Cookies** : Tokens stockes dans des cookies HttpOnly + Secure + SameSite=Strict
- **Rate Limiting** : 5 requetes/minute sur les endpoints d'authentification
- **CORS** : Origins specifiques (localhost:4200, mychess.fr)
- **Expiration JWT** : 1 heure avec refresh token de 7 jours
- **Masquage IBAN** : Les IBAN sont masques dans les reponses API
- **Webhook Validation** : HMAC-SHA256 pour Jibri et Stripe
- **ACL Videos** : Seul le student/teacher/admin d'un cours peut acceder a son enregistrement

### Frontend
- **CSP Headers** : Content-Security-Policy complete
- **XSS Protection** : Sanitization whitelist avec DOMParser
- **Container non-root** : Le frontend s'execute avec un utilisateur non-privilegie

## Performance

### Backend
- **Caffeine Cache** : TTL 5 minutes sur Articles et Ratings
- **Batch Queries** : Evite le N+1
- **HikariCP** : Pool 5-20 connexions

### Frontend
- **OnPush Strategy** : Change Detection optimisee
- **Angular Signals** : Reactivite sans polling
- **Lazy Loading** : Images et modules
- **Gzip Compression** : Via nginx

## Architecture Docker

```
┌─────────────────┐
│ mychess-frontend│ :4200
│                 │
└────────┬────────┘
         │
┌────────▼────────┐
│ mychess-backend │ :8282
│                 │
└────────┬────────┘
         │
┌────────▼────────┐
│   mychess-db    │ interne
│                 │
└─────────────────┘
```

## Troubleshooting

### Le backend ne demarre pas
```bash
docker logs mychess-backend --tail 50
```

### CORS errors
Verifier que l'origin est dans la liste CORS de `SecurityConfig.java` :
- `http://localhost:4200`
- `https://mychess.fr`

### Les notifications push ne fonctionnent pas
1. Verifier que VAPID keys sont definies dans `.env`
2. Verifier que le Service Worker est enregistre (DevTools > Application)
3. Verifier les permissions navigateur

### Reset complet (attention: efface les donnees)
```bash
docker compose down -v
./start.sh
```

## Commits Recents

| Hash | Description |
|------|-------------|
| 6d3245b | Close all programme accordion levels by default |
| 7b68d95 | Fix missing i18n translation keys |
| 41e39fc | Misc improvements and fixes |
| 2dedfd2 | Add internationalization (i18n) support with French/English |
| 4ac9a85 | Add video library feature for Premium users |
| 6321e7f | Add video thumbnail generation |
| ec8994e | Add SSE for real-time notifications |
| d3dcc3d | Add video progress tracking, Bunny CDN, premium trial |
