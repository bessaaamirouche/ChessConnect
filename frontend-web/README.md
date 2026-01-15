# ChessConnect - Frontend Web

Application Angular 17 pour la plateforme de cours d'échecs en ligne.

## Développement

```bash
npm install
ng serve
```

Naviguer vers `http://localhost:4200/`

## Fonctionnalités implémentées

### Authentification & Utilisateurs
- Inscription/Connexion (JWT)
- Rôles : Étudiant, Professeur, Admin
- Profil utilisateur avec niveau d'échecs

### Gestion des cours (Lessons)
- Réservation de cours avec un professeur
- Confirmation/Annulation par le professeur
- Statuts : PENDING, CONFIRMED, COMPLETED, CANCELLED, NO_SHOW
- Affichage des informations élève pour le prof (niveau, âge, ELO)
- Raison d'annulation visible (tooltip)
- Suppression de l'historique
- Visioconférence intégrée (Jitsi)

### Système d'annulation et remboursement (15/01/2026)
- **Prof ne confirme pas sous 24h** → Annulation auto + remboursement 100%
- **Prof annule** → Remboursement 100%
- **Élève annule > 24h avant** → Remboursement 100%
- **Élève annule 2-24h avant** → Remboursement 50%
- **Élève annule < 2h avant** → Pas de remboursement
- **Cours abonnement** : Quota restauré (sauf annulation tardive élève)
- Affichage dynamique du statut : "Annulé par moi" / "Annulé par le prof" / "Annulé par l'élève" / "Annulé (auto)"

### Abonnements
- Formules : Mensuel (4 cours), Trimestriel (12 cours), Annuel (48 cours)
- Paiement Stripe
- Affichage "Abonnement annulé" avec date d'annulation

### Progression (Learning Path)
- Niveaux d'échecs : Pion → Grand Maître
- Cours par niveau (grades) avec accordéon
- Statuts cours : LOCKED, IN_PROGRESS, PENDING_VALIDATION, COMPLETED
- Validation des cours par le professeur uniquement
- Modale profil élève (côté prof) avec progression et validation

### Dashboard
- Vue personnalisée selon le rôle
- Cartes compactes
- Navigation "Mon Espace"

## Structure du projet

```
src/app/
├── core/
│   ├── models/          # Interfaces TypeScript
│   ├── services/        # Services Angular
│   └── guards/          # Route guards
├── features/
│   ├── auth/            # Login, Register
│   ├── dashboard/       # Tableau de bord
│   ├── lessons/         # Gestion des cours
│   ├── progress/        # Progression élève
│   ├── subscription/    # Abonnements
│   └── teachers/        # Liste des profs
└── shared/
    ├── confirm-modal/   # Modal de confirmation
    └── student-profile-modal/  # Profil élève (prof)
```

## Technologies

- Angular 17 (Standalone components, Signals)
- RxJS
- ng-icons (Heroicons)
- SCSS

## Backend API

Le backend Spring Boot est dans `/backend-api/`. Voir sa documentation pour les endpoints.
