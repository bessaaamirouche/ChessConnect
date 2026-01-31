# Plan de Test Pre-Production - mychess

## 1. Authentification & Sécurité (CRITIQUE)

### Inscription
| Test | Action | Résultat attendu | Status |
|------|--------|------------------|--------|
| Inscription Joueur | Créer compte avec email valide | Compte créé, redirection dashboard | ☐ |
| Inscription Coach | Créer compte coach | Compte créé, accès aux disponibilités | ☐ |
| Email invalide | Tenter inscription avec "test@" | Erreur de validation | ☐ |
| Email dupliqué | Réinscription même email | Erreur "email déjà utilisé" | ☐ |
| Mot de passe faible | Mot de passe < 8 caractères | Erreur de validation | ☐ |

### Connexion
| Test | Action | Résultat attendu | Status |
|------|--------|------------------|--------|
| Login valide | Email + mot de passe corrects | JWT cookie HttpOnly créé | ☐ |
| Login invalide | Mauvais mot de passe | Erreur 401 | ☐ |
| Rate limiting | 6 tentatives rapides | Blocage après 5 essais | ☐ |
| Session expirée | Attendre > 1h | Redirection login | ☐ |
| Logout | Clic déconnexion | Cookie supprimé, redirection home | ☐ |

### Accès protégé
| Test | Action | Résultat attendu | Status |
|------|--------|------------------|--------|
| Route joueur sans auth | Accéder `/dashboard` | Redirection `/login` | ☐ |
| Route coach en tant que joueur | Accéder `/availability` | Erreur 403 ou redirection | ☐ |
| Route admin en tant que joueur | Accéder `/admin` | Erreur 403 | ☐ |

---

## 2. Paiements Stripe (TRÈS CRITIQUE)

### Configuration
| Test | Action | Résultat attendu | Status |
|------|--------|------------------|--------|
| Clés Stripe | Vérifier `GET /api/payments/config` | Clé publique retournée | ☐ |
| Mode test/live | Vérifier préfixe clé | `pk_live_*` en prod, `pk_test_*` en staging | ☐ |

### Paiement cours
| Test | Action | Résultat attendu | Status |
|------|--------|------------------|--------|
| Checkout réussi | Carte `4242424242424242` | Paiement OK, cours réservé | ☐ |
| Carte refusée | Carte `4000000000000002` | Erreur "carte refusée" | ☐ |
| 3D Secure | Carte `4000002500003155` | Authentification 3DS, puis succès | ☐ |
| Montant correct | Vérifier montant Stripe | Tarif coach affiché | ☐ |
| Commission 12.5% | Vérifier dans Stripe Dashboard | 87.5% vers coach, 12.5% plateforme | ☐ |

### Abonnement Premium
| Test | Action | Résultat attendu | Status |
|------|--------|------------------|--------|
| Souscrire 4.99€/mois | Payer abonnement | Badge Premium visible | ☐ |
| Accès fonctions Premium | Exercices, revisionnage | Accès autorisé | ☐ |
| Annuler abonnement | Via interface | Accès jusqu'à fin période | ☐ |
| Renouvellement | Attendre cycle (test) | Prélèvement automatique | ☐ |

### Portefeuille
| Test | Action | Résultat attendu | Status |
|------|--------|------------------|--------|
| Créditer 10€ | Paiement Stripe | Solde = 10€ | ☐ |
| Créditer < 10€ | Tenter 5€ | Erreur "minimum 10€" | ☐ |
| Payer avec solde | Réserver cours | Solde débité | ☐ |
| Solde insuffisant | Cours > solde | Basculer vers Stripe | ☐ |

---

## 3. Réservation de Cours (CRITIQUE)

### Créneaux
| Test | Action | Résultat attendu | Status |
|------|--------|------------------|--------|
| Créer disponibilité | Coach crée créneau 1h | Visible pour joueurs | ☐ |
| Créneau < 1h | Coach crée 30min | Erreur "minimum 1h" | ☐ |
| Créneau passé | Date dans le passé | Non visible ou erreur | ☐ |
| Créneau urgent | 10min après début | Encore visible (règle 5min) | ☐ |

### Réservation
| Test | Action | Résultat attendu | Status |
|------|--------|------------------|--------|
| Réserver créneau | Joueur paye | Statut PENDING | ☐ |
| Double réservation | 2 joueurs même créneau | 2ème = erreur | ☐ |
| Confirmation coach | Coach confirme | Statut CONFIRMED | ☐ |
| Cours terminé | Coach marque "terminé" | Statut COMPLETED | ☐ |

### Premier cours offert
| Test | Action | Résultat attendu | Status |
|------|--------|------------------|--------|
| Éligibilité | `GET /api/lessons/free-trial/eligible` | `true` pour nouveau joueur | ☐ |
| Réservation gratuite | Bouton vert "Réserver gratuitement" | Cours créé sans paiement | ☐ |
| 2ème cours | Même joueur | Paiement requis | ☐ |

---

## 4. Annulation & Remboursement (CRITIQUE)

| Test | Scénario | Résultat attendu | Status |
|------|----------|------------------|--------|
| Coach annule | Coach clique "Annuler" | Remboursement 100% wallet | ☐ |
| Joueur annule > 24h | Annulation anticipée | Remboursement 100% wallet | ☐ |
| Joueur annule 12h avant | Entre 2-24h | Remboursement 50% wallet | ☐ |
| Joueur annule 1h avant | < 2h avant cours | Pas de remboursement | ☐ |
| Auto-annulation | Coach ne confirme pas 24h | Annulé auto + remb 100% | ☐ |
| Raison visible | Après annulation | Tooltip avec raison | ☐ |

---

## 5. Visioconférence Jitsi (CRITIQUE)

| Test | Action | Résultat attendu | Status |
|------|--------|------------------|--------|
| Lancer appel | Clic "Rejoindre" cours confirmé | Jitsi s'ouvre | ☐ |
| Lien unique | Vérifier URL Jitsi | Room ID unique par cours | ☐ |
| Avant l'heure | 30min avant cours | Bouton disponible | ☐ |
| Cours non confirmé | Tenter rejoindre | Bouton grisé ou absent | ☐ |

---

## 6. Facturation (CRITIQUE LÉGAL)

| Test | Action | Résultat attendu | Status |
|------|--------|------------------|--------|
| Facture générée | Après paiement | Facture dans "Mes factures" | ☐ |
| Télécharger PDF | Clic téléchargement | PDF avec mentions légales | ☐ |
| Mentions SIREN | Vérifier PDF | SIREN + "TVA non applicable art. 293B CGI" | ☐ |
| Avoir crédit | Après remboursement | Credit note générée | ☐ |
| Filtrage dates | Sélectionner période | Factures filtrées | ☐ |

---

## 7. Progression & Quiz (IMPORTANT)

| Test | Action | Résultat attendu | Status |
|------|--------|------------------|--------|
| Quiz disponible | Nouveau joueur | Accès quiz d'évaluation | ☐ |
| Compléter quiz | Répondre 25 questions | Niveau attribué (Pion→Dame) | ☐ |
| Parcours affiché | Page progression | Cours par niveau visibles | ☐ |
| Validation coach | Coach valide cours | Cours marqué COMPLETED | ☐ |
| Progression conservée | Changer de coach | Niveau maintenu | ☐ |

---

## 8. Notifications (IMPORTANT)

| Test | Action | Résultat attendu | Status |
|------|--------|------------------|--------|
| Nouvelle réservation | Joueur réserve | Coach notifié (email/toast) | ☐ |
| Rappel 1h avant | Attendre H-1 | Email de rappel | ☐ |
| Coach favori publie | Coach crée créneau | Joueur Premium notifié | ☐ |
| Désactiver notifs | Paramètres profil | Plus de rappels email | ☐ |

---

## 9. Google Calendar (OPTIONNEL)

| Test | Action | Résultat attendu | Status |
|------|--------|------------------|--------|
| Connecter compte | OAuth Google | Statut "connecté" | ☐ |
| Événement créé | Réserver cours | Event dans Google Calendar | ☐ |
| Lien Jitsi | Vérifier event | Lien visio inclus | ☐ |
| Annulation | Annuler cours | Event supprimé du calendrier | ☐ |
| Déconnexion | Retirer accès | Statut "non connecté" | ☐ |

---

## 10. Interface Utilisateur (IMPORTANT)

| Test | Action | Résultat attendu | Status |
|------|--------|------------------|--------|
| Responsive mobile | Tester sur mobile | Menu hamburger, touch OK | ☐ |
| Badge rôle | Vérifier sidebar | Joueur bleu, Coach violet, Admin doré | ☐ |
| Indicateur en ligne | Coach actif | Pastille verte visible | ☐ |
| Toast notifications | Déclencher action | Toast cliquable, croix fermer | ☐ |
| Thème sombre | Vérifier contraste | Texte lisible partout | ☐ |

---

## 11. Administration (CRITIQUE)

| Test | Action | Résultat attendu | Status |
|------|--------|------------------|--------|
| Liste utilisateurs | Admin → Utilisateurs | Tous les users listés | ☐ |
| Désactiver compte | Admin désactive user | User ne peut plus se connecter | ☐ |
| Liste cours | Admin → Cours | Tous les cours visibles | ☐ |
| Comptabilité | Admin → Comptabilité | Revenus, commissions affichés | ☐ |
| Transfert Stripe | Clic "Transférer" | Virement coach effectué | ☐ |

---

## 12. Performance & Sécurité (CRITIQUE)

| Test | Action | Résultat attendu | Status |
|------|--------|------------------|--------|
| HTTPS | Accéder en HTTP | Redirection HTTPS | ☐ |
| Headers sécurité | Inspecter headers | CSP, X-Frame-Options OK | ☐ |
| IBAN masqué | API retourne IBAN | Format `FR76XXXX...1234` | ☐ |
| XSS markdown | Injecter `<script>` dans blog | Script non exécuté | ☐ |
| SQL Injection | Paramètres malformés | Erreur propre, pas de leak | ☐ |
| Cache | Charger articles 2x | 2ème chargement rapide | ☐ |

---

## 13. Tests de bout en bout (E2E)

### Parcours Joueur complet
| Étape | Action | Status |
|-------|--------|--------|
| 1 | Inscription joueur | ☐ |
| 2 | Quiz d'évaluation | ☐ |
| 3 | Créditer wallet 20€ | ☐ |
| 4 | Trouver un coach | ☐ |
| 5 | Réserver 1er cours gratuit | ☐ |
| 6 | Réserver 2ème cours (payant) | ☐ |
| 7 | Rejoindre visioconférence | ☐ |
| 8 | Évaluer le coach | ☐ |
| 9 | Ajouter coach en favori | ☐ |
| 10 | Souscrire Premium | ☐ |
| 11 | Accéder exercice myChessBot | ☐ |
| 12 | Télécharger facture | ☐ |

### Parcours Coach complet
| Étape | Action | Status |
|-------|--------|--------|
| 1 | Inscription coach | ☐ |
| 2 | Configurer Stripe Connect | ☐ |
| 3 | Créer disponibilités | ☐ |
| 4 | Confirmer réservation | ☐ |
| 5 | Rejoindre visioconférence | ☐ |
| 6 | Valider progression joueur | ☐ |
| 7 | Marquer cours terminé | ☐ |
| 8 | Vérifier revenus | ☐ |

---

## Priorité des Tests

| Priorité | Domaine |
|----------|---------|
| 🔴 P0 | Authentification, Paiements Stripe, Remboursements |
| 🟠 P1 | Réservation, Annulation, Facturation |
| 🟡 P2 | Visioconférence, Notifications, Admin |
| 🟢 P3 | Quiz, Progression, Google Calendar, Exercices |

**Recommandation** : Exécuter tous les tests P0 et P1 minimum avant mise en production.

---

## RAPPORT DE VERIFICATION TECHNIQUE (Automatisé)

Date de vérification : 2026-01-29

### Variables d'environnement (.env)

| Element | Statut | Details |
|---------|--------|---------|
| Fichier .env | ✅ OK | Présent et configuré |
| POSTGRES_PASSWORD | ✅ OK | Fort (base64, 32+ chars) |
| JWT_SECRET | ✅ OK | Fort (base64, 64+ chars) |
| JITSI_APP_SECRET | ✅ OK | Hex 64 chars |
| FRONTEND_URL | ✅ OK | https://mychess.fr |
| MAIL_HOST | ✅ OK | smtp.mail.ovh.net |
| ADMIN_EMAIL | ✅ OK | support@mychess.fr |

### Clés Stripe

| Element | Statut | Details |
|---------|--------|---------|
| STRIPE_SECRET_KEY | ⚠️ MODE TEST | `sk_test_51SsCR5...` |
| STRIPE_PUBLISHABLE_KEY | ⚠️ MODE TEST | `pk_test_51SsCR5...` |
| STRIPE_WEBHOOK_SECRET | ✅ OK | `whsec_3ugrq7...` |

**⚠️ ATTENTION : Les clés Stripe sont en MODE TEST (`sk_test_`, `pk_test_`)**
**Pour la production, remplacer par des clés LIVE (`sk_live_`, `pk_live_`) dans le fichier .env**

### Sécurité Backend

| Element | Statut | Details |
|---------|--------|---------|
| Rate Limiting | ✅ OK | 5 req/min sur /auth, 10 sur /payments |
| JWT Expiration | ✅ OK | 1 heure (3600000ms) |
| Refresh Token | ✅ OK | 7 jours (604800000ms) |
| Port DB exposé | ✅ OK | Non exposé (sécurisé) |
| Logging Stripe | ✅ OK | Niveau WARN (pas de secrets) |

### Sécurité Frontend (Headers Nginx)

| Header | Statut | Valeur |
|--------|--------|--------|
| X-Frame-Options | ✅ OK | SAMEORIGIN |
| X-Content-Type-Options | ✅ OK | nosniff |
| X-XSS-Protection | ✅ OK | 1; mode=block |
| Referrer-Policy | ✅ OK | strict-origin-when-cross-origin |
| Permissions-Policy | ✅ OK | geolocation=(), microphone=(self), camera=(self) |
| Content-Security-Policy | ✅ OK | Configuré (Stripe, Jitsi whitelist) |

### SSL/HTTPS

| Element | Statut | Details |
|---------|--------|---------|
| FRONTEND_URL | ✅ OK | https://mychess.fr |
| Certificat SSL | ✅ OK | acme.sh (Let's Encrypt) dans cron |
| Cron renouvellement | ✅ OK | `0 4 * * *` - quotidien |
| Nginx X-Forwarded-Proto | ✅ OK | Configuré pour HTTPS |

**Note** : Le SSL est géré par un reverse proxy externe (acme.sh). Vérifier que le proxy (Traefik/Caddy/Nginx externe) est bien configuré.

### Backup Base de Données

| Element | Statut | Details |
|---------|--------|---------|
| Script backup.sh | ✅ OK | Présent, complet avec checksum |
| Script restore.sh | ✅ OK | Présent |
| Dossier /backups | ⚠️ Non testé | Volume Docker monté |
| Cron backup auto | ⚠️ NON CONFIGURE | Ajouter manuellement |

**Action requise** : Configurer le cron pour les backups automatiques :
```bash
# Ajouter au crontab :
0 2 * * * cd /root/ChessConnect && ./backup.sh daily >> /var/log/mychess-backup.log 2>&1
0 3 * * 0 cd /root/ChessConnect && ./backup.sh weekly >> /var/log/mychess-backup.log 2>&1
0 4 1 * * cd /root/ChessConnect && ./backup.sh monthly >> /var/log/mychess-backup.log 2>&1
```

### Connexion Pool (HikariCP)

| Element | Valeur | Statut |
|---------|--------|--------|
| minimum-idle | 5 | ✅ OK |
| maximum-pool-size | 20 | ✅ OK |
| connection-timeout | 20s | ✅ OK |
| idle-timeout | 5min | ✅ OK |
| max-lifetime | 20min | ✅ OK |

---

## ACTIONS REQUISES AVANT PRODUCTION

### 🔴 Critique (Bloquant)

1. **Remplacer les clés Stripe TEST par LIVE**
   ```bash
   # Dans .env, remplacer :
   STRIPE_SECRET_KEY=sk_live_...
   STRIPE_PUBLISHABLE_KEY=pk_live_...
   STRIPE_WEBHOOK_SECRET=whsec_... (nouveau webhook live)
   ```

2. **Configurer le webhook Stripe en production**
   - Dashboard Stripe → Developers → Webhooks
   - URL : `https://mychess.fr/api/payments/webhook`
   - Events : `checkout.session.completed`, `invoice.paid`, `customer.subscription.*`

### 🟠 Important

3. **Configurer les backups automatiques**
   ```bash
   crontab -e
   # Ajouter les lignes de backup (voir ci-dessus)
   ```

4. **Tester le backup/restore**
   ```bash
   ./backup.sh manual
   # Vérifier que le fichier est créé dans ./backups/
   ```

5. **Vérifier le certificat SSL**
   ```bash
   curl -vI https://mychess.fr 2>&1 | grep -E "SSL|certificate"
   ```

### 🟡 Recommandé

6. **Configurer les alertes email en cas d'erreur backup**
   ```bash
   # Dans .env, ajouter :
   ALERT_EMAIL=support@mychess.fr
   ```

7. **Tester l'envoi d'emails**
   - Créer un compte test
   - Vérifier réception email de bienvenue
   - Tester "mot de passe oublié"

8. **Vérifier les logs en production**
   ```bash
   docker logs mychess-backend --tail 100
   docker logs mychess-frontend --tail 100
   ```

---

## Checklist Finale

| Catégorie | Status |
|-----------|--------|
| Variables `.env` production configurées | ✅ |
| Clés Stripe **LIVE** (pas test) | ⚠️ À CHANGER |
| FRONTEND_URL = URL production | ✅ |
| JWT_SECRET sécurisé (64+ chars) | ✅ |
| POSTGRES_PASSWORD fort | ✅ |
| Backup base de données configuré | ⚠️ Script OK, cron à ajouter |
| Certificat SSL installé | ✅ (acme.sh) |
| Domaine DNS configuré | ✅ (mychess.fr) |
| Logs sans secrets sensibles | ✅ |
| Rate limiting actif | ✅ |
