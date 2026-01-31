# Analyse Bunny Stream pour mychess

**Date :** Janvier 2025
**Contexte :** Remplacement potentiel de Jibri pour l'enregistrement et le revisionnage des cours

---

## 1. Tarification Bunny Stream

### Coûts principaux

| Service | Prix |
|---------|------|
| **Stockage** | 0,01 $/GB/mois |
| **Livraison CDN** | 0,005 $/GB (Europe) |
| **Transcodage standard** | **Gratuit** |
| **Transcodage premium** (1080p) | 0,05 $/minute |
| **Transcodage premium** (720p) | 0,05 $/minute |
| **Transcodage premium** (480p) | 0,025 $/minute |
| **Minimum mensuel** | 1 $/mois |

### Estimation par cours (1 heure)

| Qualité | Taille fichier | Transcodage | Stockage/mois |
|---------|----------------|-------------|---------------|
| 720p (standard) | ~1 GB | Gratuit | 0,01 $ |
| 1080p (premium) | ~2 GB | 3 $ | 0,02 $ |

---

## 2. Analyse de rentabilité avec Premium à 4,99 €

### Revenus par abonné Premium

| | Montant |
|---|---|
| Abonnement mensuel | 4,99 € |
| Frais Stripe (~3% + 0,25 €) | -0,40 € |
| **Revenu net** | **4,59 €/mois** |

### Coût Bunny par utilisateur Premium

**Hypothèse :** Un étudiant Premium regarde ~10h de vidéo/mois

| Poste | Calcul | Coût |
|-------|--------|------|
| Bande passante | 10h × 1,5 GB × 0,005 $ | ~0,08 $ |
| Stockage (mutualisé) | Négligeable par user | ~0,01 $ |
| **Total/user/mois** | | **~0,09 €** |

### Marge par utilisateur Premium

```
Revenu net :     4,59 €
Coût Bunny :    -0,09 €
─────────────────────────
Marge :          4,50 €/mois
Taux de marge :  ~90%
```

---

## 3. Scénario : Enregistrer TOUS les cours

### Stratégie business

Enregistrer tous les cours pour tous les étudiants, mais seuls les abonnés Premium peuvent visionner les replays. Avantage : crée un levier de conversion.

> "Vous avez 12 cours enregistrés dans votre historique. Passez Premium pour les revoir !"

### Coûts selon taille de la plateforme

**Hypothèses :**
- 4 cours/mois par étudiant actif
- Qualité : 720p (transcodage standard gratuit)
- Rétention : 6 mois

| Étudiants actifs | Cours/mois | Vidéos stockées (6 mois) | Stockage | Coût/mois |
|------------------|------------|--------------------------|----------|-----------|
| 50 | 200 | 1 200 | 1,2 TB | ~12 $ |
| 100 | 400 | 2 400 | 2,4 TB | ~24 $ |
| 200 | 800 | 4 800 | 4,8 TB | ~48 $ |
| 500 | 2 000 | 12 000 | 12 TB | ~120 $ |
| 1 000 | 4 000 | 24 000 | 24 TB | ~240 $ |

### Seuil de rentabilité

| Étudiants actifs | Coût Bunny/mois | Premium requis | Taux conversion min |
|------------------|-----------------|----------------|---------------------|
| 50 | 11 € | 3 | **6%** |
| 100 | 22 € | 5 | **5%** |
| 200 | 44 € | 10 | **5%** |
| 500 | 110 € | 25 | **5%** |
| 1 000 | 220 € | 50 | **5%** |

**Conclusion : Avec seulement 5% de conversion en Premium, le modèle est rentable.**

---

## 4. Comparaison 720p vs 1080p

### Transcodage standard (720p) - RECOMMANDÉ

| Avantages | Inconvénients |
|-----------|---------------|
| Transcodage **gratuit** | Qualité légèrement inférieure |
| Stockage ÷ 2 | - |
| Bande passante ÷ 2 | - |
| Suffisant pour cours d'échecs | - |

### Transcodage premium (1080p) - NON RECOMMANDÉ

| Étudiants | Coût transcodage/mois | Verdict |
|-----------|----------------------|---------|
| 100 | 1 200 $ | ❌ Intenable |
| 500 | 6 000 $ | ❌ Intenable |

**Recommandation : Utiliser 720p (transcodage standard gratuit)**

---

## 5. Optimisations recommandées

### Stratégies de réduction des coûts

| Stratégie | Impact sur coûts | Recommandation |
|-----------|------------------|----------------|
| 720p au lieu de 1080p | Stockage ÷ 2, transcodage gratuit | ✅ Fortement recommandé |
| Rétention 3 mois au lieu de 6 | Stockage ÷ 2 | ✅ Recommandé |
| Supprimer si étudiant inactif 6 mois | Évite accumulation | ✅ Recommandé |
| Compression agressive | Stockage -30% | ⚠️ Optionnel |

### Configuration optimale recommandée

```
Qualité :        720p (standard)
Transcodage :    Gratuit
Rétention :      3-6 mois
Suppression :    Si inactif > 6 mois
```

---

## 6. Projection financière

### Scénario conservateur

**100 étudiants actifs, 10% conversion Premium, 720p, 3 mois rétention**

| Poste | Montant |
|-------|---------|
| Revenus Premium (10 users) | +49,90 € |
| Stockage (1 200 vidéos × 1 GB) | -11,00 € |
| Bande passante (10 users × 15 GB) | -0,70 € |
| **Marge mensuelle** | **+38,20 €** |

### Scénario optimiste

**500 étudiants actifs, 15% conversion Premium, 720p, 6 mois rétention**

| Poste | Montant |
|-------|---------|
| Revenus Premium (75 users) | +344,25 € |
| Stockage (12 000 vidéos × 1 GB) | -110,00 € |
| Bande passante (75 users × 15 GB) | -5,20 € |
| **Marge mensuelle** | **+229,05 €** |

---

## 7. Comparaison Jibri vs Bunny Stream

| Critère | Jibri (actuel) | Bunny Stream |
|---------|----------------|--------------|
| **Coût fixe** | 20-50 €/mois (serveur) | ~1 $/mois minimum |
| **Coût variable** | Bande passante serveur | 0,005 $/GB |
| **Transcodage** | Manuel/inexistant | Automatique multi-résolution |
| **Stockage** | Limité au serveur | Illimité (CDN) |
| **Fiabilité** | Moyenne (maintenance requise) | Excellente (SLA 99,9%) |
| **Scalabilité** | Limitée | Illimitée |
| **Maintenance** | Complexe | Aucune |
| **Lecteur vidéo** | À développer | Inclus (embed iframe) |
| **DRM/Protection** | Non | Oui (token signé) |

**Verdict : Bunny Stream est plus économique ET plus fiable pour < 500 utilisateurs**

---

## 8. Intégration technique

### API Upload

```bash
# Créer une vidéo
curl -X POST "https://video.bunnycdn.com/library/{libraryId}/videos" \
  -H "AccessKey: {apiKey}" \
  -H "Content-Type: application/json" \
  -d '{"title": "Cours #123 - Jean D."}'

# Upload le fichier
curl -X PUT "https://video.bunnycdn.com/library/{libraryId}/videos/{videoId}" \
  -H "AccessKey: {apiKey}" \
  -T "recording.mp4"
```

### Embed Player

```html
<iframe
  src="https://iframe.mediadelivery.net/embed/{libraryId}/{videoId}?token={signedToken}"
  width="100%"
  height="400"
  allowfullscreen>
</iframe>
```

### Workflow proposé

```
1. Fin du cours Jitsi
   ↓
2. Webhook déclenche l'upload vers Bunny
   ↓
3. Bunny transcode automatiquement (720p)
   ↓
4. URL stockée dans lessons.recording_url
   ↓
5. Premium peut visionner via iframe sécurisé
```

---

## 9. Conclusion et recommandations

### Verdict final

| Question | Réponse |
|----------|---------|
| Bunny Stream est-il cher ? | **Non**, très abordable |
| Rentable avec Premium à 4,99 € ? | **Oui**, marge de 90% |
| Rentable en enregistrant TOUS les cours ? | **Oui**, avec 5% de conversion |
| Mieux que Jibri ? | **Oui**, plus fiable et moins cher |

### Actions recommandées

1. ✅ **Adopter Bunny Stream** en remplacement de Jibri
2. ✅ **Utiliser 720p** (transcodage gratuit)
3. ✅ **Enregistrer tous les cours** (levier de conversion)
4. ✅ **Rétention 3-6 mois** pour limiter les coûts
5. ✅ **Intégrer le lecteur Bunny** avec tokens signés pour la sécurité

### ROI estimé

- **Investissement initial** : ~2-3 jours de développement
- **Économie vs Jibri** : ~20-40 €/mois (serveur)
- **Fiabilité** : +++ (plus de maintenance Jibri)
- **Conversion Premium** : Potentiel d'augmentation grâce au contenu accumulé

---

## Sources

- [Bunny Stream Pricing](https://bunny.net/pricing/stream/)
- [Bunny Stream Documentation](https://docs.bunny.net/docs/stream-pricing)
- [Bunny CDN Pricing](https://bunny.net/pricing/cdn/)
- [Bunny Stream API](https://docs.bunny.net/reference/video)
