# mychess - Plateforme de Cours d'Echecs

Plateforme de mise en relation Profs/Joueurs d'echecs avec systeme de progression standardise.

## REGLES IMPORTANTES POUR CLAUDE

### Environnement Production (ports 4200, 8282)

- NE JAMAIS rebuild les containers sauf demande explicite
- Mots-cles pour deployer : "applique", "patch", "mise a jour", "deploie", "rebuild"

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

### Mode Maintenance
- **Backend** : `MaintenanceController` expose `/api/maintenance/status`
- **Backend** : `MaintenanceFilter` bloque les operations sensibles quand le mode est actif
- **Frontend** : `MaintenanceService` interroge le statut et affiche une banniere d'alerte
- **Config** : `app.maintenance.enabled=true/false` dans `application.yml` ou variable d'environnement

### Enregistrement Video et Concatenation
- **Probleme resolu (2026-02-08)** : Les enregistrements multiples ecrasaient les precedents
- **Solution** : Systeme de concatenation automatique avec FFmpeg
- **Fonctionnement** :
  - Jibri cree plusieurs segments quand les participants quittent/rejoignent
  - Chaque segment est stocke dans `recording_segments` (JSON array)
  - Scheduler concatene automatiquement 60 min apres le cours
  - Upload de la video finale sur Bunny CDN
  - Isolation stricte : chaque cours a ses propres segments
- **Configuration Jibri** (4 instances paralleles) :
  - Resolution capture : 1280x720 (Xorg dummy + ffmpeg.resolution)
  - VideoRam : 192000 (192 Mo par instance)
  - Serveur : 6 cores, 12 Go RAM
- **Parametres FFmpeg de concatenation** (optimises 2026-02-15) :
  - `-preset medium` : Meilleure compression (etait: fast)
  - `-crf 20` : Haute qualite (etait: 23)
  - `-b:a 192k` : Audio ameliore (etait: 128k)
  - `-vf scale=1280:720:force_original_aspect_ratio=decrease,pad=1280:720:-1:-1:color=black` : Corrige les bandes noires
- **Fichiers cles** :
  - `VideoConcatenationService.java` - Service de concatenation FFmpeg
  - `VideoConcatenationScheduler.java` - Scheduler (toutes les 10 min)
  - `RecordingController.java` - Accumulation des segments
  - Migration `V23__add_recording_segments.sql`
  - Config Jibri : `/etc/jitsi/jibri/jibri.conf` (x4 instances)
  - Config Xorg : `/etc/jitsi/jibri/xorg-video-dummy.conf` (x4 instances)

### Fonctionnalites Principales
- **Inscription Joueur/Coach** : Creation de compte avec roles et badges visuels
- **Premier Cours Offert** : Les nouveaux joueurs beneficient d'un premier cours gratuit
- **Quiz d'Evaluation** : Determinez votre niveau d'echecs (A, B, C, D)
- **Parcours d'Apprentissage** : 120 cours structures par niveau, 546 lecons
- **Reservation de Cours** : Reservez des sessions individuelles ou de groupe avec des coachs
- **Cours de Groupe** : Le coach definit la taille du groupe, prix degressif, deadline pour remplir le groupe
- **Disponibilites 24h/24** : Les coachs peuvent creer des creneaux a n'importe quelle heure
- **Types de cours** : Individuel ou groupe, configurable par disponibilite
- **Reservations urgentes** : Les creneaux restent visibles jusqu'a 5 min apres l'heure de debut
- **Suivi de Progression** : Suivez votre parcours d'apprentissage
- **Paiements Stripe** : Paiements securises integres (mode test)
- **Stripe Connect** : Paiement direct aux coachs avec commissions, onboarding, retrait
- **Portefeuille (Wallet)** : Credit de solde pour payer les cours, remboursements automatiques
- **Factures PDF** : Generation automatique avec mentions legales, avoir pour remboursements, numerotation sequentielle
- **Codes Promo** : Systeme de codes promotionnels et parrainage (admin)
- **Video Jitsi Meet** : Cours en visioconference integree avec theme Premium mychess
- **Minuteur d'appel** : Timer visible pendant les appels video
- **Auto-hangup** : Raccrochage automatique en fin de cours
- **Indicateur de presence** : Pastille verte quand un coach est en ligne
- **Blog SEO** : Articles optimises pour le referencement avec support Markdown
- **Design Apple-like** : Animations scroll, sections immersives, effets parallax
- **Evaluation des Coachs** : Systeme de notation 5 etoiles apres chaque cours avec commentaires
- **Coachs Favoris** : Ajoutez vos coachs preferes en favoris avec notifications de nouveaux creneaux
- **Rappels Email** : Notification automatique 1 heure avant chaque cours
- **Enregistrement Video** : Les cours sont enregistres via Jibri pour revisionnage (Premium)

### Cours de Groupe

Systeme de cours collectifs :
- **Creation** : Le coach definit `maxParticipants` sur ses disponibilites (2-10)
- **Prix degressif** : `GroupPricingCalculator` calcule le prix par personne selon la taille du groupe
- **Deadline** : Le createur choisit une deadline pour remplir le groupe
- **Invitation** : Lien d'invitation partageable avec token unique
- **Resolution deadline** : Le createur peut payer la totalite ou annuler si le groupe n'est pas rempli
- **Modele** : `GroupInvitation`, `LessonParticipant`
- **Endpoints** : `GroupLessonController` (`/api/group-lessons`)
- **Migrations** : `V22__add_group_lessons.sql`, `V28__add_availability_max_participants.sql`

### Codes Promo et Parrainage

Systeme de codes promotionnels :
- **Types** : Pourcentage ou montant fixe
- **Administration** : CRUD complet via `AdminPromoController`
- **Validation** : Verification dates, limites d'usage, eligibilite
- **Parrainage** : Code referral avec suivi des gains (`ReferralEarning`)
- **Modeles** : `PromoCode`, `PromoCodeUsage`, `ReferralEarning`
- **Endpoints** : `/api/admin/promo-codes` (ADMIN), `/api/promo` (utilisateurs)
- **Migration** : `V27__add_promo_and_referral_codes.sql`

### Stripe Connect (Paiement Coachs)

Paiement direct aux coachs via Stripe Connect Express :
- **Onboarding** : Creation de compte Connect et lien d'inscription
- **Balance** : Suivi des gains avec commissions
- **Retrait** : Le coach peut retirer un montant personnalise
- **Admin** : Vue des comptes Connect, liens dashboard Express
- **Service** : `StripeConnectService.java`, `TeacherBalanceService.java`
- **Endpoints** : `StripeConnectController` (`/api/stripe-connect`)

### Internationalisation (i18n)

Support multilingue complet avec 2 langues :
- **Francais** (`/assets/i18n/fr.json`) - Langue par defaut
- **Anglais** (`/assets/i18n/en.json`)

**Implementation :**
- Librairie `@ngx-translate/core`
- `LanguageService` - Gestion du changement de langue avec persistance localStorage
- `LanguageSelectorComponent` - Selecteur de langue dans les settings
- 1945 cles de traduction par langue couvrant toute l'application

**Zones traduites :**
- Navigation, hero, features, pricing, FAQ
- Authentification (login, register, forgot password)
- Dashboard, lessons, teachers, progress
- Wallet, subscription, invoices
- Settings, availability, ratings
- Blog, library, exercise
- Cours de groupe, codes promo
- Messages d'erreur, validations, statuts

### Notifications Push Web

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
- Android Chrome : Excellent
- Desktop (Chrome, Firefox, Edge) : Excellent
- iOS Safari : Uniquement si PWA installee (iOS 16.4+)

### Notifications Temps Reel (SSE)

Server-Sent Events pour les notifications en temps reel quand l'app est ouverte.

**Backend :**
- `SseConnectionManager.java` - Gestion des connexions SSE actives
- `UserNotificationService.java` - Orchestration des notifications
- `SseController.java` - Endpoints `/notifications/stream` et `/notifications/stats`

**Securite et fiabilite (ameliore 2026-02-15) :**
- Multi-onglets : jusqu'a 3 connexions SSE simultanees par utilisateur
- Limite globale : 1000 connexions max (prevention DoS)
- Heartbeat nomme toutes les 20s avec timestamp (detection connexions mortes)
- Nettoyage automatique des connexions inactives
- Endpoint `/notifications/stats` pour monitoring

**Frontend :**
- `NotificationService` / `SseService` - Client SSE avec reconnexion automatique
- Detection des connexions stale via timeout heartbeat

**Logique intelligente :** Si l'utilisateur est connecte via SSE, les notifications push ne sont pas envoyees (evite les doublons).

### Bibliotheque Video (Premium)

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

### Integration Bunny CDN

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

### Abonnement Premium (4,99 EUR/mois)

Fonctionnalites exclusives pour les abonnes Premium :
- **Bibliotheque Video** : Acces aux enregistrements de cours
- **Reprise de lecture** : Progression video sauvegardee
- **Notifications prioritaires** : Alertes email quand les coachs favoris publient des creneaux
- **Badge Premium** : Badge dore visible sur le profil
- **Entrainement myChessBot** : Jouez contre le bot apres vos cours

### Interface Jitsi Premium

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
- **Crediter son solde** : Paiement Stripe pour ajouter des credits (minimum 10 EUR)
- **Payer avec le solde** : Option de paiement lors de la reservation si solde suffisant
- **Remboursements automatiques** : Les annulations creditent le portefeuille (pas de remboursement Stripe)
- **Historique des transactions** : Liste des credits, debits et remboursements
- Endpoints API : `GET /api/wallet/balance`, `POST /api/wallet/credit`, `GET /api/wallet/transactions`

### Factures et Avoirs

Systeme de facturation conforme :
- **Generation automatique** : Facture PDF a chaque paiement (cours ou credit wallet)
- **Mentions legales** : Infos entreprise configurables dans InvoiceService.java
- **Avoirs (Credit Notes)** : Generation automatique lors des remboursements
- **Numerotation sequentielle** : Sequence PostgreSQL `invoice_number_seq` (Migration V26)
- **Retention** : Champs denormalises pour conservation apres suppression utilisateur (V24)
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
├── /backend-api              # API Spring Boot 3.2
│   ├── /src/main/java/com/chessconnect
│   │   ├── /config           # SecurityConfig, StripeConfig, WebPushConfig, CacheConfig
│   │   ├── /controller       # 33 REST Controllers
│   │   ├── /dto              # Data Transfer Objects
│   │   ├── /model            # 32 Entites JPA + 16 enums
│   │   ├── /repository       # Repositories Spring Data
│   │   ├── /security         # JWT, RateLimiting
│   │   └── /service          # 36 Services metier
│   ├── /src/main/resources
│   │   └── /db/migration     # Migrations Flyway (V0-V29)
│   ├── Dockerfile
│   └── pom.xml
├── /frontend-web             # Application Angular 21
│   ├── /src/app
│   │   ├── /core             # Services, Guards, Interceptors, Models
│   │   │   └── /services     # PushNotificationService, LanguageService, LibraryService, etc.
│   │   ├── /features         # 24 modules par fonctionnalite
│   │   │   ├── /admin        # Back-office administrateur
│   │   │   ├── /auth         # Login, Register, Forgot/Reset Password
│   │   │   ├── /availability # Gestion des disponibilites (prof)
│   │   │   ├── /blog         # Articles SEO
│   │   │   ├── /dashboard    # Tableau de bord
│   │   │   ├── /exercise     # Entrainement myChessBot (Premium)
│   │   │   ├── /faq          # Foire aux questions
│   │   │   ├── /home         # Landing page
│   │   │   ├── /how-it-works # Page comment ca marche
│   │   │   ├── /invoices     # Factures
│   │   │   ├── /legal        # Pages legales (CGU, CGV, mentions legales)
│   │   │   ├── /lessons      # Reservation, liste des cours, book-lesson, join-group, payment-success
│   │   │   ├── /library      # Bibliotheque video (Premium)
│   │   │   ├── /links        # Page liens
│   │   │   ├── /not-found    # Page 404
│   │   │   ├── /pricing      # Page tarifs
│   │   │   ├── /programme    # Programme d'apprentissage
│   │   │   ├── /progress     # Suivi de progression
│   │   │   ├── /quiz         # Quiz d'evaluation
│   │   │   ├── /settings     # Parametres utilisateur
│   │   │   ├── /subscription # Gestion des abonnements
│   │   │   ├── /teachers     # Liste et profil des coachs
│   │   │   ├── /user-layout  # Layout utilisateur connecte
│   │   │   └── /wallet       # Portefeuille
│   │   └── /shared           # Composants partages (video-call, call-timer, language-selector, etc.)
│   ├── /src/assets
│   │   ├── /i18n             # Fichiers de traduction (en.json, fr.json) - 1945 cles
│   │   ├── /icons            # Icones PWA
│   │   ├── logo.png          # Logo mychess
│   │   └── og-image.jpg      # Image Open Graph
│   ├── /src/styles
│   │   ├── _variables.scss   # Variables CSS (theme sombre + accents dores)
│   │   ├── _animations.scss  # Animations scroll reveal
│   │   └── _mobile.scss      # Styles responsive
│   ├── /tests/stress         # Tests de stress Playwright
│   ├── sw-push.js            # Service Worker pour Push
│   ├── Dockerfile            # Node 22-alpine, port interne 4000 (expose 4200 via docker-compose)
│   └── package.json
├── /jibri-setup              # Configuration Jibri (enregistrement video)
├── /shared/contracts          # Contrats TypeScript partages
├── docker-compose.yml        # Config Docker (frontend:4200, backend:8282, postgres:interne)
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

### Utilisateurs (`/api/users`)
| Methode | Endpoint            | Description                       | Auth    |
|---------|---------------------|-----------------------------------|---------|
| GET     | `/me`               | Profil de l'utilisateur connecte  | JWT     |
| PATCH   | `/me`               | Mettre a jour le profil           | JWT     |
| POST    | `/me/password`      | Changer le mot de passe           | JWT     |
| PUT     | `/me/teacher-profile`| Mettre a jour le profil coach     | TEACHER |
| POST    | `/me/heartbeat`     | Heartbeat presence en ligne       | JWT     |
| DELETE  | `/me`               | Supprimer son compte (RGPD)       | JWT     |

### Notifications Push (`/api/push`)
| Methode | Endpoint      | Description                     | Auth    |
|---------|---------------|---------------------------------|---------|
| GET     | `/vapid-key`  | Cle publique VAPID              | Non     |
| POST    | `/subscribe`  | S'abonner aux notifications     | JWT     |
| POST    | `/unsubscribe`| Se desabonner                   | JWT     |
| GET     | `/status`     | Statut des notifications        | JWT     |
| PATCH   | `/preference` | Activer/desactiver les push     | JWT     |

### Notifications (`/api/notifications`)
| Methode | Endpoint        | Description                        | Auth    |
|---------|-----------------|-----------------------------------|---------|
| GET     | `/stream`       | Flux SSE temps reel               | JWT     |
| GET     | `/`             | Toutes les notifications          | JWT     |
| GET     | `/unread`       | Notifications non lues            | JWT     |
| GET     | `/unread/count` | Compteur non lus                  | JWT     |
| PATCH   | `/{id}/read`    | Marquer comme lue                 | JWT     |
| PATCH   | `/read-all`     | Tout marquer comme lu             | JWT     |
| DELETE  | `/{id}`         | Supprimer une notification        | JWT     |
| GET     | `/stats`        | Statistiques connexions SSE       | JWT     |

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

### Cours de Groupe (`/api/group-lessons`)
| Methode | Endpoint               | Description                          | Auth    |
|---------|------------------------|--------------------------------------|---------|
| POST    | `/`                    | Creer/rejoindre un cours de groupe   | STUDENT |
| GET     | `/invitation/{token}`  | Details d'une invitation             | Non     |
| POST    | `/join/credit`         | Rejoindre avec wallet                | STUDENT |
| POST    | `/join/checkout`       | Rejoindre via Stripe Checkout        | STUDENT |
| POST    | `/join/confirm`        | Confirmer paiement Stripe            | Non     |
| POST    | `/checkout`            | Creer via Stripe Checkout            | STUDENT |
| POST    | `/create/confirm`      | Confirmer creation Stripe            | Non     |
| POST    | `/{id}/resolve-deadline`| Resoudre deadline (payer/annuler)   | STUDENT |
| DELETE  | `/{id}/leave`          | Quitter un cours de groupe           | STUDENT |
| GET     | `/{id}`                | Details du cours de groupe           | JWT     |

### Paiements (`/api/payments`)
| Methode | Endpoint                | Description                     | Auth    |
|---------|-------------------------|---------------------------------|---------|
| GET     | `/config`               | Configuration Stripe publique   | Non     |
| GET     | `/plans`                | Liste des plans d'abonnement    | Non     |
| POST    | `/checkout/subscription`| Creer session Stripe Checkout   | STUDENT |
| POST    | `/checkout/lesson`      | Payer un cours a l'unite        | STUDENT |
| GET     | `/subscription`         | Abonnement actif                | STUDENT |

### Stripe Connect (`/api/stripe-connect`)
| Methode | Endpoint             | Description                        | Auth    |
|---------|----------------------|------------------------------------|---------|
| POST    | `/onboarding`        | URL onboarding Stripe Connect      | TEACHER |
| GET     | `/status`            | Statut du compte Connect           | TEACHER |
| POST    | `/refresh-link`      | Rafraichir le lien onboarding      | TEACHER |
| DELETE  | `/disconnect`        | Deconnecter le compte Connect      | TEACHER |
| GET     | `/balance`           | Solde disponible pour retrait      | TEACHER |
| POST    | `/recalculate-balance`| Recalculer le solde               | TEACHER |
| POST    | `/withdraw`          | Retirer un montant                 | TEACHER |

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

### Codes Promo (`/api/promo`)
| Methode | Endpoint          | Description                     | Auth    |
|---------|-------------------|---------------------------------|---------|
| GET     | `/validate`       | Valider un code promo           | JWT     |
| POST    | `/apply-referral` | Appliquer un code parrainage    | JWT     |

### Administration Promo (`/api/admin/promo-codes`)
| Methode | Endpoint            | Description                       | Auth  |
|---------|---------------------|-----------------------------------|-------|
| GET     | `/`                 | Liste des codes promo             | ADMIN |
| POST    | `/`                 | Creer un code promo               | ADMIN |
| GET     | `/{id}`             | Detail d'un code                  | ADMIN |
| PUT     | `/{id}`             | Modifier un code                  | ADMIN |
| PATCH   | `/{id}/toggle`      | Activer/desactiver                | ADMIN |
| DELETE  | `/{id}`             | Supprimer un code                 | ADMIN |
| POST    | `/generate`         | Generer un code unique            | ADMIN |
| GET     | `/{id}/usages`      | Historique d'utilisation          | ADMIN |
| GET     | `/{id}/earnings`    | Gains du code                     | ADMIN |
| PATCH   | `/{id}/mark-paid`   | Marquer les gains comme payes     | ADMIN |

### Parcours d'Apprentissage (`/api/learning-path`)
| Methode | Endpoint                                | Description                       | Auth    |
|---------|-----------------------------------------|-----------------------------------|---------|
| GET     | `/`                                     | Parcours du joueur                | STUDENT |
| GET     | `/courses/{id}`                         | Detail d'un cours                 | STUDENT |
| POST    | `/courses/{id}/start`                   | Demarrer un cours                 | STUDENT |
| POST    | `/courses/{courseId}/validate/{studentId}`| Valider un cours (prof)          | TEACHER |
| GET     | `/students/{studentId}`                 | Profil eleve avec progression     | TEACHER |
| POST    | `/students/{studentId}/level`           | Definir le niveau d'un eleve      | TEACHER |
| GET     | `/students/{studentId}/next-course`     | Prochain cours d'un eleve         | TEACHER |

### Programme (`/api/programme`)
| Methode | Endpoint          | Description                        | Auth    |
|---------|-------------------|------------------------------------|---------|
| GET     | `/courses`        | Tous les cours avec statut         | JWT     |
| GET     | `/courses/by-level`| Cours groupes par niveau          | JWT     |
| GET     | `/current`        | Cours actuel de l'utilisateur      | JWT     |
| POST    | `/current`        | Definir le cours de depart         | JWT     |
| POST    | `/advance`        | Avancer au cours suivant           | JWT     |
| POST    | `/go-back`        | Revenir au cours precedent         | JWT     |
| GET     | `/public/courses` | Tous les cours (page inscription)  | Non     |

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
| POST    | `/webhook`               | Webhook Jibri (HMAC-SHA256)        | Non     |
| GET     | `/video/{lessonId}`      | Recuperer la video d'un cours      | JWT     |

### Jitsi (`/api/jitsi`)
| Methode | Endpoint  | Description                        | Auth |
|---------|-----------|------------------------------------|------|
| GET     | `/token`  | Token Jitsi pour acces salle video | JWT  |

### Progression (`/api/progress`)
| Methode | Endpoint         | Description                        | Auth    |
|---------|------------------|------------------------------------|---------|
| GET     | `/me`            | Progression du joueur              | STUDENT |
| GET     | `/levels`        | Tous les niveaux d'echecs          | Non     |
| GET     | `/levels/{level}`| Info d'un niveau specifique        | Non     |

### Articles/Blog (`/api/articles`)
| Methode | Endpoint              | Description                     | Auth    |
|---------|-----------------------|---------------------------------|---------|
| GET     | `/`                   | Articles publies (pagine)       | Non     |
| GET     | `/category/{cat}`     | Articles par categorie          | Non     |
| GET     | `/latest`             | Derniers articles (homepage)    | Non     |
| GET     | `/categories`         | Toutes les categories           | Non     |
| GET     | `/{slug}`             | Article par slug                | Non     |
| GET     | `/admin`              | Tous les articles (admin)       | ADMIN   |
| POST    | `/`                   | Creer un article                | ADMIN   |
| PUT     | `/{id}`               | Modifier un article             | ADMIN   |
| DELETE  | `/{id}`               | Supprimer un article            | ADMIN   |
| PATCH   | `/{id}/publish`       | Publier un article              | ADMIN   |
| PATCH   | `/{id}/unpublish`     | Depublier un article            | ADMIN   |

### Upload (`/api/upload`)
| Methode | Endpoint  | Description           | Auth |
|---------|-----------|-----------------------|------|
| POST    | `/avatar` | Upload avatar         | JWT  |
| DELETE  | `/avatar` | Supprimer avatar      | JWT  |

### Contact (`/api/contact`)
| Methode | Endpoint  | Description                         | Auth |
|---------|-----------|-------------------------------------|------|
| POST    | `/admin`  | Envoyer un message au support       | Non  |

### Tracking (`/api/tracking`)
| Methode | Endpoint    | Description                  | Auth |
|---------|-------------|------------------------------|------|
| POST    | `/pageview` | Tracker une vue de page      | Non  |

### SEO
| Methode | Endpoint       | Description          | Auth |
|---------|----------------|----------------------|------|
| GET     | `/sitemap.xml` | Sitemap XML          | Non  |
| GET     | `/robots.txt`  | Robots.txt           | Non  |

### Maintenance (`/api/maintenance`)
| Methode | Endpoint    | Description                        | Auth |
|---------|-------------|------------------------------------|------|
| GET     | `/status`   | Statut du mode maintenance         | Non  |

### Administration (`/api/admin`)
| Methode | Endpoint                                      | Description                          | Auth  |
|---------|-----------------------------------------------|--------------------------------------|-------|
| GET     | `/users`                                      | Liste des utilisateurs (pagine)      | ADMIN |
| GET     | `/users/{id}`                                 | Detail utilisateur                   | ADMIN |
| PATCH   | `/users/{id}/suspend`                         | Suspendre un utilisateur             | ADMIN |
| PATCH   | `/users/{id}/activate`                        | Reactiver un utilisateur             | ADMIN |
| DELETE  | `/users/{id}`                                 | Supprimer utilisateur (RGPD)         | ADMIN |
| POST    | `/users/{id}/refund-wallet`                   | Rembourser le wallet                 | ADMIN |
| GET     | `/users/{id}/wallet`                          | Solde wallet d'un utilisateur        | ADMIN |
| GET     | `/lessons/upcoming`                           | Cours a venir                        | ADMIN |
| GET     | `/lessons/completed`                          | Cours termines                       | ADMIN |
| GET     | `/lessons/history`                            | Historique des cours                 | ADMIN |
| GET     | `/lessons/all`                                | Tous les cours                       | ADMIN |
| GET     | `/accounting/revenue`                         | Vue d'ensemble revenus/commissions   | ADMIN |
| GET     | `/accounting/teachers`                        | Soldes et infos bancaires des coachs | ADMIN |
| POST    | `/accounting/teachers/{teacherId}/pay`        | Marquer un coach comme paye          | ADMIN |
| GET     | `/accounting/payments`                        | Historique paiements (pagine)        | ADMIN |
| GET     | `/subscriptions`                              | Tous les abonnements (pagine)        | ADMIN |
| POST    | `/subscriptions/{id}/cancel`                  | Annuler un abonnement                | ADMIN |
| POST    | `/payments/{paymentIntentId}/refund`          | Rembourser un paiement               | ADMIN |
| GET     | `/stats`                                      | Statistiques dashboard               | ADMIN |
| GET     | `/analytics`                                  | Donnees analytiques (graphiques)     | ADMIN |
| GET     | `/stripe-connect/accounts`                    | Comptes Connect des coachs           | ADMIN |
| POST    | `/stripe-connect/accounts/{id}/dashboard-link`| Lien dashboard Express               | ADMIN |
| GET     | `/stripe-connect/accounts/{id}`               | Detail compte Connect                | ADMIN |
| GET     | `/messages`                                   | Messages des cours                   | ADMIN |
| GET     | `/messages/teachers`                          | Liste coachs (filtre)                | ADMIN |
| GET     | `/messages/students`                          | Liste eleves (filtre)                | ADMIN |
| POST    | `/thumbnails/generate-missing`                | Generer vignettes manquantes         | ADMIN |
| POST    | `/thumbnails/generate/{lessonId}`             | Generer vignette d'un cours          | ADMIN |
| POST    | `/broadcast-availabilities`                   | Envoyer email dispos a tous          | ADMIN |
| GET     | `/library/videos`                             | Toutes les videos                    | ADMIN |

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
| V0 | Schema initial (users, lessons, payments, etc.) |
| V1 | Notifications utilisateur |
| V2 | Index de production |
| V3 | Verification email |
| V4 | Suppression Google Calendar |
| V5 | Suivi du cours actuel |
| V6 | Ajout cours au programme |
| V7 | Fix contrainte type facture |
| V8 | Fix doublons avoirs et remboursements commissions |
| V9 | Champ free trial started at |
| V10 | Validations de cours en attente |
| V11 | Simplification niveaux echecs |
| V12 | Version utilisateur (optimistic locking) |
| V13 | Commentaire cours et soft delete |
| V14 | Essai premium sans carte |
| V15 | Progression video |
| V16 | Nettoyage donnees paiement test |
| V17 | URLs vignettes |
| V18 | Table push subscriptions |
| V19 | Preference notifications push |
| V20 | UUID utilisateur |
| V21 | Activation essai premium et notifications existants |
| V22 | Cours de groupe (group_invitation, lesson_participant) |
| V23 | Segments d'enregistrement video |
| V24 | Champs denormalises factures (retention) |
| V25 | Type de cours par disponibilite |
| V26 | Sequence numerotation factures |
| V27 | Codes promo et parrainage |
| V28 | Max participants par disponibilite |
| V29 | Index de performance (lessons, invoices, payments, users, availabilities) |

## Stack Technique

| Composant | Technologies |
|-----------|-------------|
| Backend | Spring Boot 3.2, Java 17, Spring Security (JWT HttpOnly), Spring Data JPA, Caffeine Cache |
| Frontend | Angular 21, Signals, Standalone Components, SSR, RxJS, ng-icons, ngx-translate, SCSS |
| Database | PostgreSQL 16, Flyway migrations (V0-V28), HikariCP |
| DevOps | Docker, Docker Compose, Nginx, Node 22-alpine |
| Paiements | Stripe (Embedded Checkout, Connect Express) |
| Video | Jitsi Meet (theme Premium), Jibri (4 instances 720p), Bunny CDN, FFmpeg |
| Echecs | Chessground 9, Chess.js, Stockfish.js |
| Push | Web Push API, VAPID, BouncyCastle |
| Charts | ng2-charts 8 (BaseChartDirective) |
| i18n | ngx-translate (FR, EN) - 1945 cles |

## Securite

### Backend
- **JWT HttpOnly Cookies** : Tokens stockes dans des cookies HttpOnly + Secure + SameSite=Strict
- **Rate Limiting** : 5 requetes/minute sur les endpoints d'authentification
- **CORS** : Origins specifiques : `localhost:4200`, `localhost:3000`, `mychess.fr`, `www.mychess.fr`, `meet.mychess.fr` (http + https)
- **Expiration JWT** : 1 heure avec refresh token de 7 jours
- **Masquage IBAN** : Les IBAN sont masques dans les reponses API
- **Webhook Validation** : HMAC-SHA256 pour Jibri et Stripe
- **ACL Videos** : Seul le student/teacher/admin d'un cours peut acceder a son enregistrement
- **Jitsi Room ACL** : Verification que l'utilisateur est participant du cours avant de generer un token Jitsi
- **Wallet Pessimistic Lock** : Verrou pessimiste (`SELECT FOR UPDATE`) pour eviter les race conditions sur le solde
- **Wallet Idempotency** : Protection contre le rejeu des paiements Stripe (verification `stripePaymentIntentId`)
- **Admin endpoints securises** : `/teachers/admin/**` et `/payments/admin/**` restreints au role ADMIN
- **Trusted Proxy RFC 1918** : Validation correcte des plages 172.16-31.x.x
- **Mode Maintenance** : Filtre global pour bloquer les operations sensibles en maintenance

### Frontend
- **CSP Headers** : Content-Security-Policy complete (domaines specifiques mychess.fr, Bunny CDN)
- **XSS Protection** : Sanitization whitelist avec DOMParser
- **Container non-root** : Le frontend s'execute avec un utilisateur non-privilegie (nodejs:1001)
- **HSTS** : Strict-Transport-Security active
- **Server Tokens Off** : Version nginx masquee
- **Security Headers sur assets** : X-Content-Type-Options et X-Frame-Options sur les fichiers caches

## Performance

### Backend
- **Caffeine Cache** : TTL 5 minutes sur Articles et Ratings
- **Batch Queries** : Evite le N+1
- **HikariCP** : Pool 5-20 connexions
- **Pagination** : Endpoints admin pagines (Spring Pageable)
- **Index V29** : Index composites sur lessons(status, scheduled_at), teacher/student lookups, invoices, payments, users(role), availabilities

### Frontend
- **OnPush Strategy** : Change Detection optimisee
- **Angular Signals** : Reactivite sans polling (input(), output(), viewChild(), computed(), signal())
- **Lazy Loading** : Images et modules
- **Gzip Compression** : Via nginx
- **SSR** : Server-Side Rendering via @angular/ssr

## Architecture Docker

```
┌─────────────────┐
│ mychess-frontend│ :4200 (interne :4000)
│ nginx + node    │
└────────┬────────┘
         │
┌────────▼────────┐
│ mychess-backend │ :8282
│ Spring Boot     │
└────────┬────────┘
         │
┌────────▼────────┐
│   mychess-db    │ interne
│ PostgreSQL 16   │
└─────────────────┘
```

## Troubleshooting

### Le backend ne demarre pas
```bash
docker logs mychess-backend --tail 50
```

### CORS errors
Verifier que l'origin est dans la liste CORS de `SecurityConfig.java` :
- `http://localhost:4200`, `http://localhost:3000`
- `https://mychess.fr`, `https://www.mychess.fr`
- `https://meet.mychess.fr`

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
| 89c769a | Fix stress test video timing: scheduled_at NOW()+5min to NOW()+2min |
| 11236ef | Improve FFmpeg video quality and comprehensive CLAUDE.md audit update |
| 05c89d2 | Improve SSE reliability, video quality, and DB performance (V29 indexes) |
| 78ea54e | Redesign group lessons: coach defines group size, fix library/recording/wallet for groups |
| fbb614f | Fix library video player: hide wrong duration badge, remove double modal, fill video width |
| 4e00ec9 | Add promo codes, availability lesson types, auto-hangup video calls, call timer, video duration fix |
| 7c6446c | Angular 21 upgrade, pagination, invoice retention, recording premium-only |
| 1968572 | Add group lessons, video concatenation, and UI improvements |
| 4afcc1d | Add HMAC-SHA256 signature to Jibri recording webhook |
| e82589e | Fix SSE excessive reconnections, Jitsi disconnections, and timezone issues |
