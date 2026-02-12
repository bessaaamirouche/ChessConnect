package com.chessconnect.config;

import com.chessconnect.model.Article;
import com.chessconnect.repository.ArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Order(3)
public class ArticleDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ArticleDataInitializer.class);
    private final ArticleRepository articleRepository;

    public ArticleDataInitializer(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    @Override
    public void run(String... args) {
        if (articleRepository.count() == 0) {
            log.info("Initializing blog articles...");
            createSampleArticles();
            log.info("Blog articles initialized successfully");
        }
    }

    private void createSampleArticles() {
        // Article 1: Guide debutant
        Article article1 = new Article();
        article1.setTitle("Comment debuter aux echecs : Le guide complet pour 2024");
        article1.setSlug("comment-debuter-aux-echecs-guide-complet");
        article1.setMetaDescription("Apprenez les bases des echecs avec notre guide complet pour debutants. Regles, mouvements des pieces, strategies de base et conseils pour progresser rapidement.");
        article1.setMetaKeywords("echecs debutant, apprendre echecs, regles echecs, cours echecs");
        article1.setExcerpt("Vous souhaitez apprendre a jouer aux echecs ? Decouvrez notre guide complet pour maitriser les bases et commencer votre parcours vers la maitrise du jeu royal.");
        article1.setContent(getDebutantArticleContent());
        article1.setCoverImage("https://images.unsplash.com/photo-1529699211952-734e80c4d42b?w=1200");
        article1.setAuthor("ChessConnect");
        article1.setCategory("debutant");
        article1.setPublished(true);
        article1.setPublishedAt(LocalDateTime.now().minusDays(30));
        article1.setReadingTimeMinutes(8);
        articleRepository.save(article1);

        // Article 2: Ouvertures
        Article article2 = new Article();
        article2.setTitle("Les 5 ouvertures essentielles pour bien debuter aux echecs");
        article2.setSlug("5-ouvertures-essentielles-echecs-debutant");
        article2.setMetaDescription("Decouvrez les 5 ouvertures d'echecs incontournables pour les debutants. Partie italienne, defense sicilienne, gambit dame et plus encore.");
        article2.setMetaKeywords("ouvertures echecs, partie italienne, defense sicilienne, gambit dame");
        article2.setExcerpt("Les premieres coups d'une partie d'echecs sont cruciaux. Apprenez les 5 ouvertures les plus efficaces pour prendre l'avantage des le debut de la partie.");
        article2.setContent(getOuverturesArticleContent());
        article2.setCoverImage("https://images.unsplash.com/photo-1586165368502-1bad197a6461?w=1200");
        article2.setAuthor("ChessConnect");
        article2.setCategory("strategie");
        article2.setPublished(true);
        article2.setPublishedAt(LocalDateTime.now().minusDays(20));
        article2.setReadingTimeMinutes(10);
        articleRepository.save(article2);

        // Article 3: Progression
        Article article3 = new Article();
        article3.setTitle("Progresser aux echecs : De Pion a Dame en 6 mois");
        article3.setSlug("progresser-echecs-pion-dame-6-mois");
        article3.setMetaDescription("Decouvrez notre methode structuree pour progresser aux echecs. Du niveau debutant au niveau expert, suivez un parcours adapte a vos objectifs.");
        article3.setMetaKeywords("progresser echecs, ameliorer echecs, niveau echecs, entrainement echecs");
        article3.setExcerpt("Vous jouez aux echecs depuis un moment mais vous stagnez ? Decouvrez notre methode eprouvee pour passer au niveau superieur et atteindre vos objectifs.");
        article3.setContent(getProgressionArticleContent());
        article3.setCoverImage("https://images.unsplash.com/photo-1580541832626-2a7131ee809f?w=1200");
        article3.setAuthor("ChessConnect");
        article3.setCategory("progression");
        article3.setPublished(true);
        article3.setPublishedAt(LocalDateTime.now().minusDays(15));
        article3.setReadingTimeMinutes(12);
        articleRepository.save(article3);

        // Article 4: Coach en ligne
        Article article4 = new Article();
        article4.setTitle("Trouver un coach d'echecs en ligne : Le guide ultime");
        article4.setSlug("trouver-coach-echecs-en-ligne");
        article4.setMetaDescription("Comment choisir le bon coach d'echecs en ligne ? Criteres de selection, tarifs, formats de cours et avantages de l'apprentissage a distance.");
        article4.setMetaKeywords("coach echecs, cours echecs en ligne, coach echecs, apprendre echecs distance");
        article4.setExcerpt("L'apprentissage en ligne a revolutionne la facon d'apprendre les echecs. Decouvrez comment trouver le coach ideal pour accelerer votre progression.");
        article4.setContent(getCoachArticleContent());
        article4.setCoverImage("https://images.unsplash.com/photo-1560174038-da43ac74f01b?w=1200");
        article4.setAuthor("ChessConnect");
        article4.setCategory("conseils");
        article4.setPublished(true);
        article4.setPublishedAt(LocalDateTime.now().minusDays(10));
        article4.setReadingTimeMinutes(7);
        articleRepository.save(article4);

        // Article 5: Bienfaits enfants
        Article article5 = new Article();
        article5.setTitle("Les bienfaits des echecs pour les enfants : Developpement cognitif et social");
        article5.setSlug("bienfaits-echecs-enfants-developpement");
        article5.setMetaDescription("Decouvrez pourquoi les echecs sont excellents pour le developpement des enfants. Concentration, logique, patience et competences sociales.");
        article5.setMetaKeywords("echecs enfants, bienfaits echecs, developpement cognitif, jeux educatifs");
        article5.setExcerpt("Les echecs sont bien plus qu'un jeu pour les enfants. Decouvrez comment cette activite millenaire peut contribuer a leur developpement intellectuel et social.");
        article5.setContent(getEnfantsArticleContent());
        article5.setCoverImage("https://images.unsplash.com/photo-1604948501466-4e9c339b9c24?w=1200");
        article5.setAuthor("ChessConnect");
        article5.setCategory("enfants");
        article5.setPublished(true);
        article5.setPublishedAt(LocalDateTime.now().minusDays(5));
        article5.setReadingTimeMinutes(6);
        articleRepository.save(article5);

        // Article 6: Devenir coach
        Article article6 = new Article();
        article6.setTitle("Devenir coach d'echecs en ligne : Le guide complet pour se lancer");
        article6.setSlug("devenir-coach-echecs-en-ligne-guide-complet");
        article6.setMetaDescription("Decouvrez comment devenir coach d'echecs sur mychess. Creation d'entreprise, inscription, gestion des disponibilites et premiers cours.");
        article6.setMetaKeywords("devenir coach echecs, enseigner echecs, coach echecs en ligne, cours echecs");
        article6.setExcerpt("Vous etes passionne d'echecs et souhaitez transmettre votre savoir ? Decouvrez comment devenir coach sur mychess et commencer a donner des cours en ligne.");
        article6.setContent(getDevenirCoachArticleContent());
        article6.setCoverImage("https://images.unsplash.com/photo-1523875194681-bedd468c58bf?w=1200");
        article6.setAuthor("mychess");
        article6.setCategory("coach");
        article6.setPublished(true);
        article6.setPublishedAt(LocalDateTime.now().minusDays(2));
        article6.setReadingTimeMinutes(10);
        articleRepository.save(article6);
    }

    private String getDebutantArticleContent() {
        return """
            ## Introduction aux echecs

            Les echecs sont un jeu de strategie millenaire qui continue de fasciner des millions de joueurs a travers le monde. Que vous soyez completement novice ou que vous ayez deja quelques notions, ce guide vous accompagnera pas a pas dans votre apprentissage.

            ## Les regles de base

            ### L'echiquier
            L'echiquier est compose de 64 cases alternant entre blanc et noir. Il doit toujours etre positionne avec une case blanche en bas a droite de chaque joueur.

            ### Les pieces et leurs mouvements

            **Le Roi** : Il peut se deplacer d'une case dans toutes les directions. C'est la piece la plus importante : si votre Roi est mat, vous perdez la partie.

            **La Dame** : C'est la piece la plus puissante. Elle peut se deplacer en ligne droite ou en diagonale sur autant de cases qu'elle le souhaite.

            **La Tour** : Elle se deplace en ligne droite, horizontalement ou verticalement.

            **Le Fou** : Il se deplace en diagonale uniquement.

            **Le Cavalier** : Il se deplace en "L" (2 cases puis 1 case perpendiculairement) et peut sauter par-dessus les autres pieces.

            **Le Pion** : Il avance d'une case (ou deux depuis sa position initiale) et capture en diagonale.

            ## Les concepts strategiques essentiels

            ### Le controle du centre
            Les quatre cases centrales (e4, d4, e5, d5) sont strategiquement cruciales. Controleer le centre vous donne plus de mobilite et d'options.

            ### Le developpement des pieces
            En debut de partie, sortez vos pieces rapidement : les cavaliers et les fous d'abord, puis roquez pour mettre votre roi en securite.

            ### La securite du roi
            Ne negligez jamais la securite de votre roi. Le roque (petit ou grand) est generalement recommande dans les 10 premiers coups.

            ## Conseils pour progresser

            1. **Jouez regulierement** : La pratique est essentielle
            2. **Analysez vos parties** : Comprenez vos erreurs
            3. **Etudiez les tactiques de base** : Fourchettes, clouages, enfilades
            4. **Prenez des cours** : Un coach accelerera votre progression

            ## Conclusion

            Les echecs recompensent la patience et la perseverance. Avec de la pratique reguliere et les bons conseils, vous progresserez rapidement. N'hesitez pas a rejoindre ChessConnect pour beneficier de cours personnalises avec nos maitres certifies.
            """;
    }

    private String getOuverturesArticleContent() {
        return """
            ## Pourquoi les ouvertures sont importantes

            Les premiers coups d'une partie d'echecs determinent souvent le cours du jeu. Une bonne ouverture vous permet de developper vos pieces harmonieusement, de controler le centre et de preparer votre milieu de partie.

            ## Les 5 ouvertures essentielles

            ### 1. La Partie Italienne (1.e4 e5 2.Cf3 Cc6 3.Fc4)

            L'une des ouvertures les plus anciennes et les plus instructives. Elle vise le point faible f7 et permet un developpement naturel des pieces.

            **Avantages** : Facile a apprendre, principes clairs
            **Pour qui** : Debutants et joueurs intermediaires

            ### 2. La Defense Sicilienne (1.e4 c5)

            L'ouverture la plus populaire au niveau coachessionnel. Les noirs combattent immediatement pour le centre.

            **Avantages** : Positions dynamiques, nombreuses possibilites
            **Pour qui** : Joueurs aimant les positions tranchantes

            ### 3. Le Gambit Dame (1.d4 d5 2.c4)

            Une ouverture solide ou les blancs sacrifient temporairement un pion pour le controle du centre.

            **Avantages** : Position solide, jeu strategique
            **Pour qui** : Joueurs appreciant les positions positionnelles

            ### 4. La Defense Francaise (1.e4 e6)

            Une defense solide qui cree une structure de pions robuste pour les noirs.

            **Avantages** : Tres solide, contre-jeu sur l'aile dame
            **Pour qui** : Joueurs defensifs

            ### 5. Le Systeme de Londres (1.d4 suivi de Ff4, e3, c3)

            Un systeme universel que les blancs peuvent jouer contre presque toutes les reponses noires.

            **Avantages** : Facile a memoriser, plans clairs
            **Pour qui** : Joueurs preferant eviter la theorie

            ## Comment etudier les ouvertures

            1. Comprenez les **idees** derriere les coups, pas seulement les coups eux-memes
            2. Etudiez les **plans typiques** du milieu de partie
            3. Connaissez les **pieges courants** de vos ouvertures
            4. **Pratiquez** vos ouvertures en jouant des parties

            ## Conclusion

            Maitriser quelques ouvertures solides est essentiel pour progresser aux echecs. Commencez par une ou deux ouvertures et approfondissez-les avant d'en explorer d'autres.
            """;
    }

    private String getProgressionArticleContent() {
        return """
            ## La methode ChessConnect pour progresser

            Progresser aux echecs demande une approche structuree et de la regularite. Notre methode, testee par des centaines de joueurs, vous guide de debutant a expert.

            ## Les 5 niveaux de progression

            ### Niveau Pion (Debutant)
            - Maitriser les regles et mouvements
            - Comprendre les tactiques de base
            - Eviter les erreurs grossieres
            **Objectif** : Ne plus perdre de pieces gratuitement

            ### Niveau Cavalier (Intermediaire)
            - Connaitre 2-3 ouvertures solides
            - Calculer 2-3 coups a l'avance
            - Comprendre les principes du milieu de partie
            **Objectif** : Gagner contre des joueurs occasionnels

            ### Niveau Fou (Confirme)
            - Maitriser les tactiques avancees
            - Comprendre les structures de pions
            - Ameliorer son calcul
            **Objectif** : Atteindre 1400-1600 ELO

            ### Niveau Tour (Avance)
            - Etudier les finales en profondeur
            - Analyser ses parties serieusement
            - Developper un repertoire d'ouvertures complet
            **Objectif** : Atteindre 1600-1900 ELO

            ### Niveau Dame (Expert)
            - Preparation specifique contre les adversaires
            - Travail sur la psychologie du jeu
            - Perfectionnement continu
            **Objectif** : Depasser 1900 ELO

            ## Les cles de la progression

            ### 1. Tactiques quotidiennes
            Resolvez 15-20 exercices tactiques par jour. La tactique represente 80% des parties aux niveaux amateur.

            ### 2. Analyse de vos parties
            Chaque partie jouee devrait etre analysee. Identifiez vos erreurs et comprenez pourquoi vous les avez commises.

            ### 3. Etude des finales
            Les finales sont souvent negligees mais cruciales. Maitrisez les finales de base (Roi et Tour, Roi et Pions).

            ### 4. Cours avec un coach
            Un coach identifie rapidement vos faiblesses et vous propose un plan de travail adapte.

            ## Le planning type

            | Jour | Activite | Duree |
            |------|----------|-------|
            | Lundi | Tactiques | 30 min |
            | Mardi | Ouvertures | 30 min |
            | Mercredi | Partie en ligne + analyse | 1h |
            | Jeudi | Tactiques | 30 min |
            | Vendredi | Finales | 30 min |
            | Samedi | Cours avec coach | 1h |
            | Dimanche | Tournoi en ligne | 2h |

            ## Conclusion

            La progression aux echecs est un marathon, pas un sprint. Avec de la regularite et la bonne methode, vous atteindrez vos objectifs. Nos coachs sont la pour vous accompagner a chaque etape.
            """;
    }

    private String getCoachArticleContent() {
        return """
            ## Pourquoi prendre des cours d'echecs en ligne ?

            L'apprentissage en ligne a revolutionne l'enseignement des echecs. Plus besoin de se deplacer dans un club : vous pouvez apprendre avec les meilleurs coachs depuis votre salon.

            ## Les avantages des cours en ligne

            ### Flexibilite
            - Cours disponibles 7j/7
            - Horaires adaptes a votre emploi du temps
            - Possibilite de reporter facilement

            ### Qualite de l'enseignement
            - Acces a des coachs partout en France
            - Outils interactifs (ecran partage, analyse en temps reel)
            - Enregistrement des cours pour revision

            ### Rapport qualite/prix
            - Pas de frais de deplacement
            - Tarifs souvent plus avantageux
            - Formules d'abonnement flexibles

            ## Comment choisir son coach ?

            ### Les criteres essentiels

            1. **Le niveau de jeu**
               - Verifiez le classement ELO du coach
               - Un bon coach n'a pas besoin d'etre grand maitre
               - L'important est qu'il soit significativement au-dessus de votre niveau

            2. **L'experience pedagogique**
               - Depuis combien de temps enseigne-t-il ?
               - A-t-il des avis d'anciens joueurs ?
               - Est-il patient et pedagogue ?

            3. **La specialisation**
               - Certains coachs sont specialises (enfants, competition, etc.)
               - Choisissez en fonction de vos objectifs

            4. **Le feeling**
               - Le premier cours est souvent gratuit ou a prix reduit
               - Profitez-en pour evaluer la compatibilite

            ## Les tarifs moyens

            | Type de cours | Tarif moyen |
            |--------------|-------------|
            | Debutant | 30-40€/h |
            | Intermediaire | 40-50€/h |
            | Avance | 50-70€/h |
            | Grand Maitre | 80-150€/h |

            ## Pourquoi choisir ChessConnect ?

            - **Coachs verifies** : Tous nos coaches sont certifies
            - **Flexibilite maximale** : Reservez 24h/24, 7j/7
            - **Progression suivie** : Un cursus adapte a votre niveau
            - **Satisfait ou rembourse** : Premier cours garanti

            ## Conclusion

            Investir dans un coach d'echecs est le moyen le plus efficace de progresser. Avec ChessConnect, trouvez le coach ideal et commencez votre progression des aujourd'hui.
            """;
    }

    private String getEnfantsArticleContent() {
        return """
            ## Les echecs : bien plus qu'un jeu pour les enfants

            De nombreuses etudes scientifiques demontrent les bienfaits des echecs sur le developpement cognitif et social des enfants. Decouvrez pourquoi ce jeu millenaire est un excellent outil educatif.

            ## Les bienfaits cognitifs

            ### Amelioration de la concentration
            Une partie d'echecs exige une attention soutenue. Les enfants apprennent naturellement a se concentrer plus longtemps.

            ### Developpement de la logique
            Les echecs entrainent le raisonnement logique : si je joue ce coup, alors mon adversaire pourra repondre ainsi...

            ### Renforcement de la memoire
            Retenir les ouvertures, les schemas tactiques et les erreurs passees stimule la memoire de travail et la memoire a long terme.

            ### Creativite et imagination
            Trouver des solutions originales, imaginer des combinaisons : les echecs stimulent la creativite.

            ## Les bienfaits sociaux et emotionnels

            ### Gestion des emotions
            Apprendre a gerer la frustration de la defaite et l'excitation de la victoire est une competence precieuse.

            ### Patience et perseverance
            Les echecs enseignent que le succes vient avec l'effort et la pratique reguliere.

            ### Respect de l'adversaire
            La tradition des echecs (serrer la main, accepter la defaite avec dignite) enseigne le fair-play.

            ## A quel age commencer ?

            - **5-6 ans** : Introduction ludique aux mouvements des pieces
            - **7-8 ans** : Apprentissage des regles completes et premiers tournois
            - **9-10 ans** : Approfondissement strategique
            - **11+ ans** : Etude serieuse possible

            ## Comment motiver un enfant ?

            1. **Rendre l'apprentissage ludique** : Histoires, puzzles, recompenses
            2. **Jouer avec lui** : Le partage est essentiel
            3. **Eviter la pression** : Le plaisir doit rester au centre
            4. **Celebrer les progres** : Meme les petites victoires comptent
            5. **Trouver un bon coach** : Un coach adapte fait toute la difference

            ## Les echecs a l'ecole

            De nombreuses ecoles integrent desormais les echecs dans leurs programmes :
            - Amelioration des resultats en mathematiques
            - Meilleur comportement en classe
            - Developpement de la confiance en soi

            ## Conclusion

            Les echecs sont un formidable outil de developpement pour les enfants. Chez ChessConnect, nos coachs specialises savent rendre l'apprentissage passionnant et adapte a chaque jeune joueur.
            """;
    }

    private String getDevenirCoachArticleContent() {
        return """
            ## Pourquoi devenir coach d'echecs ?

            Vous maitrisez les echecs et aimez transmettre votre passion ? Devenir coach en ligne est une excellente opportunite de generer des revenus complementaires tout en partageant votre expertise.

            Avec mychess, vous beneficiez d'une plateforme cle en main : pas besoin de chercher des joueurs, de gerer les paiements ou de configurer un systeme de reservation. On s'occupe de tout !

            ## Etape 1 : Creer votre statut d'entreprise

            Pour exercer legalement et etre remunere pour vos cours, vous devez avoir un statut d'entreprise. La solution la plus simple est la **micro-entreprise**.

            ### Pourquoi la micro-entreprise ?

            - **Simple a creer** : 15 minutes en ligne
            - **Peu de formalites** : Pas de comptabilite complexe
            - **Charges reduites** : Environ 22% du chiffre d'affaires
            - **Ideal pour debuter** : Jusqu'a 77 700€ de CA annuel

            ### Comment creer votre micro-entreprise ?

            1. **Rendez-vous sur le guichet unique des entreprises**
               [https://procedures.inpi.fr](https://procedures.inpi.fr/?/)

            2. **Choisissez l'activite**
               Code APE recommande : 85.51Z - Enseignement de disciplines sportives et d'activites de loisirs

            3. **Remplissez le formulaire**
               - Identite
               - Adresse du siege (votre domicile)
               - Date de debut d'activite

            4. **Validez et attendez votre numero SIRET**
               Vous le recevrez sous 1 a 2 semaines par courrier

            ### Ressources utiles

            - [Guide officiel de la micro-entreprise](https://www.autoentrepreneur.urssaf.fr/)
            - [Simulateur de charges URSSAF](https://www.autoentrepreneur.urssaf.fr/portail/accueil/une-question/simulateur-de-charges.html)
            - [Aide a la creation d'entreprise (ACRE)](https://www.service-public.fr/particuliers/vosdroits/F11677)

            ## Etape 2 : Creer votre compte coach sur mychess

            Une fois votre SIRET en poche, vous pouvez vous inscrire sur notre plateforme.

            ### Inscription

            1. **Allez sur mychess.fr** et cliquez sur "S'inscrire"
            2. **Selectionnez "Coach"** dans le choix du role
            3. **Remplissez vos informations** :
               - Nom et prenom
               - Email
               - Mot de passe securise
               - Tarif horaire (libre, au choix du coach)
               - Bio : presentez-vous en quelques lignes
               - Langues parlees

            ### Conseils pour votre profil

            - **Photo professionnelle** : Un visage souriant inspire confiance
            - **Bio detaillee** : Mentionnez votre niveau ELO, vos titres, votre experience
            - **Tarif competitif** : Commencez avec un tarif attractif pour avoir vos premiers avis

            ## Etape 3 : Configurer vos disponibilites

            C'est la cle pour recevoir des reservations !

            ### Acces a la gestion des disponibilites

            1. Connectez-vous a votre compte
            2. Allez dans le menu "Mes Disponibilites"

            ### Types de creneaux

            **Creneaux recurrents** : Ideals pour une routine reguliere
            - Exemple : Tous les mardis de 18h a 20h

            **Creneaux ponctuels** : Pour des disponibilites exceptionnelles
            - Exemple : Le 15 janvier de 14h a 16h

            ### Bonnes pratiques

            - **Proposez plusieurs creneaux** : Plus vous en avez, plus vous aurez de reservations
            - **Variez les horaires** : Matin, midi, soir, week-end
            - **Soyez regulier** : Les joueurs apprecient la stabilite

            ## Etape 4 : Configurer vos paiements (Stripe Connect)

            Pour recevoir vos gains, vous devez connecter votre compte bancaire via Stripe.

            ### Configuration

            1. Allez dans **Parametres** > **Paiements**
            2. Cliquez sur **"Configurer mes paiements"**
            3. Suivez le processus Stripe Connect :
               - Informations personnelles
               - Coordonnees bancaires (IBAN)
               - Verification d'identite (piece d'identite)

            ### Comment ca marche ?

            - Lorsqu'un joueur reserve un cours, il paie en ligne
            - Apres le cours, les fonds sont credites sur votre compte Stripe
            - Vous pouvez demander un virement a tout moment
            - Commission mychess : 12.5% (10% plateforme + 2.5% frais bancaires)

            **Exemple** : Pour un cours a 50€, vous recevez 43.75€

            ## Etape 5 : Recevoir et gerer vos reservations

            ### Notification de reservation

            Quand un joueur reserve un creneau :
            1. Vous recevez un email de notification
            2. La reservation apparait dans "Mes Cours"

            ### Confirmer une reservation

            - Vous avez **24 heures** pour confirmer
            - Sans confirmation, la reservation est automatiquement annulee et le joueur rembourse

            ### Avant le cours

            - Verifiez le profil du joueur (niveau, objectifs)
            - Preparez votre cours en fonction
            - Testez votre connexion et votre webcam

            ### Pendant le cours

            - Le cours se deroule en **visioconference** directement sur la plateforme
            - Utilisez l'ecran partage pour montrer l'echiquier
            - Duree standard : 1 heure

            ### Apres le cours

            - Cliquez sur **"Terminer"** pour valider le cours
            - Ajoutez vos observations pour le suivi du joueur
            - Vos gains sont credites automatiquement

            ## Conseils pour reussir

            ### Construisez votre reputation

            - **Soyez ponctuel** : Connectez-vous 5 min avant le cours
            - **Soyez pedagogue** : Adaptez-vous au niveau du joueur
            - **Soyez patient** : Les debutants ont besoin d'encouragements

            ### Fidelisez vos joueurs

            - Proposez un suivi personnalise
            - Donnez des exercices entre les cours
            - Celebrez leurs progres

            ### Augmentez votre visibilite

            - Completez votre profil a 100%
            - Repondez rapidement aux reservations
            - Encouragez les avis positifs

            ## Les avantages mychess

            | Avantage | Description |
            |----------|-------------|
            | Joueurs qualifies | Des passionnes motives pour apprendre |
            | Paiement securise | Stripe garantit vos paiements |
            | Outils integres | Visioconference, suivi progression |
            | Support reactif | Une equipe a votre ecoute |
            | 0 frais fixes | Vous ne payez que sur vos gains |

            ## FAQ

            **Quel niveau faut-il pour enseigner ?**
            Nous recommandons un niveau ELO minimum de 1600, mais l'experience pedagogique compte aussi beaucoup.

            **Puis-je fixer mon propre tarif ?**
            Oui, vous etes libre de fixer votre tarif horaire sans restriction.

            **Comment sont geres les annulations ?**
            - Joueur annule > 24h avant : remboursement 100%
            - Joueur annule 2-24h avant : remboursement 50%
            - Joueur annule < 2h avant : pas de remboursement (vous etes paye)

            **Puis-je donner des cours depuis l'etranger ?**
            Oui, tant que vous avez un statut permettant de facturer et un compte bancaire compatible Stripe.

            ## Conclusion

            Devenir coach sur mychess est simple et accessible. En quelques etapes, vous pouvez commencer a transmettre votre passion et generer des revenus complementaires.

            **Pret a vous lancer ?** [Creez votre compte coach](https://mychess.fr/register) des maintenant !
            """;
    }
}
