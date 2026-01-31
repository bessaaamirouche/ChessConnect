package com.chessconnect.config;

import com.chessconnect.model.Course;
import com.chessconnect.model.enums.ChessLevel;
import com.chessconnect.repository.CourseRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CourseDataInitializer implements CommandLineRunner {

    private final CourseRepository courseRepository;

    public CourseDataInitializer(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @Override
    public void run(String... args) {
        if (courseRepository.count() == 0) {
            List<Course> courses = new ArrayList<>();

            // Niveau A - Pion (Débutant) - 50 cours (50h)
            courses.addAll(createLevelACourses());

            // Niveau B - Cavalier (Intermédiaire) - 60 cours (60h)
            courses.addAll(createLevelBCourses());

            // Niveau C - Reine (Avancé) - 70 cours (70h)
            courses.addAll(createLevelCCourses());

            // Niveau D - Roi (Expert) - 80 cours (80h)
            courses.addAll(createLevelDCourses());

            courseRepository.saveAll(courses);
            System.out.println("✓ " + courses.size() + " cours initialisés (Pion:50, Cavalier:60, Reine:70, Roi:80)");
        }
    }

    // =====================================================
    // NIVEAU A - PION (Débutant) - 50 cours
    // =====================================================
    private List<Course> createLevelACourses() {
        String[][] data = {
            // Module 1: Découverte de l'échiquier (10 cours)
            {"L'échiquier: votre terrain de jeu", "Découvrez les 64 cases et la disposition initiale"},
            {"Les colonnes et rangées", "Apprenez à identifier les coordonnées sur l'échiquier"},
            {"Les diagonales", "Comprenez l'importance des lignes obliques"},
            {"La notation algébrique", "Apprenez à lire et écrire les coups d'échecs"},
            {"La position de départ", "Disposez correctement toutes les pièces"},
            {"Les cases blanches et noires", "L'importance de la couleur des cases"},
            {"Le centre de l'échiquier", "Pourquoi les cases centrales sont cruciales"},
            {"Les flancs de l'échiquier", "Aile roi et aile dame expliquées"},
            {"Lire une partie notée", "Suivez une partie avec la notation"},
            {"Écrire vos premiers coups", "Pratiquez la notation avec des exercices"},

            // Module 2: Le mouvement des pièces (15 cours)
            {"Le Roi: la pièce essentielle", "Apprenez le mouvement d'une case dans toutes les directions"},
            {"Protéger son Roi", "Pourquoi le Roi doit toujours être en sécurité"},
            {"La Dame: la pièce la plus puissante", "Maîtrisez ses déplacements multidirectionnels"},
            {"L'utilisation de la Dame", "Quand et comment déployer votre Dame"},
            {"La Tour: puissance sur les lignes", "Déplacements horizontaux et verticaux"},
            {"Les Tours jumelles", "L'importance de connecter vos Tours"},
            {"Le Fou: maître des diagonales", "Un Fou reste toujours sur sa couleur"},
            {"Fou de cases blanches vs Fou de cases noires", "Comprenez la différence"},
            {"Le Cavalier: le sauteur unique", "Le mouvement en L expliqué pas à pas"},
            {"Les cases accessibles au Cavalier", "Calculer les destinations possibles"},
            {"Le Cavalier au bord vs au centre", "Pourquoi le Cavalier aime le centre"},
            {"Le Pion: l'âme des échecs", "Le mouvement de base et la prise en diagonale"},
            {"Le premier coup du Pion", "L'option d'avancer de deux cases"},
            {"La prise en passant", "Cette règle spéciale expliquée en détail"},
            {"La promotion du Pion", "Transformez vos Pions en pièces puissantes"},

            // Module 3: Règles spéciales et objectifs (10 cours)
            {"Le petit roque", "Mettez votre Roi en sécurité côté roi"},
            {"Le grand roque", "Mettez votre Roi en sécurité côté dame"},
            {"Quand peut-on roquer?", "Les conditions du roque expliquées"},
            {"L'échec au Roi", "Comprendre ce qu'est un échec"},
            {"Les trois façons de parer un échec", "Prendre, bloquer ou fuir"},
            {"L'échec et mat", "L'objectif ultime du jeu d'échecs"},
            {"Le pat: une partie nulle", "Quand le Roi ne peut plus bouger sans être en échec"},
            {"Les autres types de nulles", "Répétition, règle des 50 coups, matériel insuffisant"},
            {"La valeur relative des pièces", "Dame=9, Tour=5, Fou=Cavalier=3, Pion=1"},
            {"Les échanges favorables", "Quand échanger ses pièces"},

            // Module 4: Mats de base (8 cours)
            {"Le mat du couloir", "Un mat classique avec la Tour"},
            {"Mat avec Dame et Roi", "Technique pas à pas pour mater avec la Dame"},
            {"Mat avec deux Tours", "L'escalier vers le mat"},
            {"Mat avec une seule Tour", "La technique de base de la finale Tour+Roi vs Roi"},
            {"Le mat du berger", "Évitez ce piège de débutant"},
            {"Le mat de l'écolier", "Un autre piège classique à connaître"},
            {"Le mat à l'étouffée", "Le Cavalier enferme le Roi"},
            {"Reconnaître les patterns de mat", "Visualisez les schémas de mat courants"},

            // Module 5: Principes du jeu (7 cours)
            {"Contrôler le centre", "L'importance des cases e4, d4, e5, d5"},
            {"Développer ses pièces", "Sortez vos pièces rapidement"},
            {"Roquer tôt", "Mettez votre Roi en sécurité dès que possible"},
            {"Ne pas sortir la Dame trop tôt", "Pourquoi c'est une erreur classique"},
            {"Connecter les Tours", "Finalisez votre développement"},
            {"Jouer avec un plan", "Donnez un objectif à vos coups"},
            {"Les erreurs de débutant à éviter", "Les pièges les plus courants"}
        };
        return createCoursesForLevel(data, ChessLevel.A, 45);
    }

    // =====================================================
    // NIVEAU B - CAVALIER (Intermédiaire) - 60 cours
    // =====================================================
    private List<Course> createLevelBCourses() {
        String[][] data = {
            // Module 1: Tactiques fondamentales (20 cours)
            {"La fourchette: attaquer deux pièces", "Le concept de la double attaque"},
            {"Fourchette de Cavalier", "Le Cavalier roi des fourchettes"},
            {"Fourchette de Dame", "Utilisez la Dame pour des fourchettes dévastatrices"},
            {"Fourchette de Pion", "Même le Pion peut fourchetter"},
            {"Le clouage absolu", "La pièce clouée ne peut pas bouger légalement"},
            {"Le clouage relatif", "La pièce peut bouger mais c'est coûteux"},
            {"Exploiter un clouage", "Profitez des pièces clouées"},
            {"L'enfilade", "L'inverse du clouage: attaquer l'alignement"},
            {"La brochette", "Deux pièces sur une même ligne"},
            {"L'attaque double", "Créez deux menaces simultanées"},
            {"L'attaque à la découverte", "Révélez une attaque cachée en bougeant une pièce"},
            {"L'échec à la découverte", "Un échec surprise dévastateur"},
            {"L'échec double", "Deux pièces donnent échec en même temps"},
            {"Le sacrifice d'attraction", "Attirez une pièce sur une case vulnérable"},
            {"Le sacrifice de déviation", "Éloignez un défenseur de son poste"},
            {"Le sacrifice de destruction", "Éliminez un défenseur clé"},
            {"Surcharge d'une pièce", "Quand une pièce a trop de tâches"},
            {"L'interception", "Coupez une ligne de défense"},
            {"Les motifs tactiques combinés", "Enchaînez plusieurs tactiques"},
            {"Exercices tactiques niveau 1", "Pratiquez les bases tactiques"},

            // Module 2: Calcul et visualisation (10 cours)
            {"Le mat en 1 coup", "Trouvez le mat immédiat"},
            {"Le mat en 2 coups", "Calculez une combinaison simple"},
            {"Le mat en 3 coups", "Combinaisons plus élaborées"},
            {"Calculer les échanges", "Évaluez qui gagne dans un échange"},
            {"La défense passive", "Protégez simplement vos pièces"},
            {"La défense active", "Défendez en contre-attaquant"},
            {"Parer les menaces tactiques", "Anticipez les coups adverses"},
            {"Le calcul des variantes", "Méthode pour calculer plusieurs coups"},
            {"Exercices de calcul niveau 1", "Améliorez votre calcul"},
            {"Exercices tactiques niveau 2", "Tactiques plus complexes"},

            // Module 3: Finales essentielles (15 cours)
            {"Finale Roi + Dame vs Roi", "La technique du mat pas à pas"},
            {"Finale Roi + Tour vs Roi", "L'escalier et le mat"},
            {"Finale Roi + 2 Fous vs Roi", "Coordination des deux Fous"},
            {"Finale Roi + Fou + Cavalier vs Roi", "Le mat le plus difficile de base"},
            {"L'opposition simple", "Le concept clé des finales de pions"},
            {"La règle du carré", "Le Roi peut-il rattraper le Pion?"},
            {"Le pion passé en finale", "La course vers la promotion"},
            {"Le Roi actif en finale", "L'importance du Roi centralisé"},
            {"Finale Roi + Pion vs Roi", "Les positions gagnantes et nulles"},
            {"La case critique", "Les cases que le Roi doit contrôler"},
            {"L'opposition distante", "Opposition à plusieurs cases"},
            {"Finales Roi + 2 Pions vs Roi + 1 Pion", "Technique de base"},
            {"Le zugzwang simple", "Quand bouger est désavantageux"},
            {"Finales de Tours: introduction", "Les finales les plus courantes"},
            {"Position de base Tour + Pion vs Tour", "Concepts fondamentaux"},

            // Module 4: Ouvertures fondamentales (15 cours)
            {"Principes de l'ouverture", "Les règles d'or du début de partie"},
            {"L'ouverture Italienne", "1.e4 e5 2.Cf3 Cc6 3.Fc4"},
            {"Le Giuoco Piano", "La variante tranquille de l'Italienne"},
            {"L'ouverture Espagnole", "1.e4 e5 2.Cf3 Cc6 3.Fb5"},
            {"La défense Française", "1.e4 e6 - structure solide"},
            {"La défense Sicilienne: introduction", "1.e4 c5 - l'arme principale contre 1.e4"},
            {"La défense Caro-Kann: introduction", "1.e4 c6 - solide et fiable"},
            {"Le Gambit Dame", "1.d4 d5 2.c4 - le début classique"},
            {"La défense Slave", "2...c6 contre le Gambit Dame"},
            {"Pièges d'ouverture courants", "Ne tombez pas dedans!"},
            {"Le développement harmonieux", "Coordonnez vos pièces"},
            {"L'activité des pièces", "Des pièces actives gagnent"},
            {"Les Tours sur colonnes ouvertes", "Activez vos Tours"},
            {"Le bon Fou vs le mauvais Fou", "Évaluez vos Fous"},
            {"Cavalier vs Fou: le grand débat", "Quand préférer l'un à l'autre"}
        };
        return createCoursesForLevel(data, ChessLevel.B, 50);
    }

    // =====================================================
    // NIVEAU C - REINE (Avancé) - 70 cours
    // =====================================================
    private List<Course> createLevelCCourses() {
        String[][] data = {
            // Module 1: Ouvertures approfondies (20 cours)
            {"L'Italienne: variantes avancées", "Giuoco Piano, Evans Gambit"},
            {"L'Espagnole: les plans stratégiques", "La Ruy Lopez en profondeur"},
            {"La Sicilienne Najdorf", "La variante la plus populaire"},
            {"La Sicilienne Dragon", "L'attaque avec ...g6 et ...Fg7"},
            {"La Sicilienne Scheveningen", "Structure ...e6 et ...d6"},
            {"La Française: variante d'avance", "Plans après 3.e5"},
            {"La Française: variante d'échange", "Quand les blancs prennent en d5"},
            {"La Caro-Kann classique", "Plans typiques pour les deux camps"},
            {"La Scandinave", "1.e4 d5 - défense directe"},
            {"Le Gambit Dame accepté", "2...dxc4 et comment le jouer"},
            {"Le Gambit Dame refusé", "2...e6 - la réponse classique"},
            {"La Nimzo-Indienne", "Contrôle stratégique avec ...Fb4"},
            {"L'Est-Indienne", "Plans d'attaque sur l'aile roi"},
            {"La Grünfeld", "Le contre central avec ...d5"},
            {"L'ouverture Anglaise", "1.c4 - le jeu positionnel"},
            {"L'ouverture Réti", "1.Cf3 - approche hypermoderne"},
            {"L'ouverture Catalane", "Fianchetto avec pression sur l'aile dame"},
            {"Le système Londres", "Développement systématique pour les blancs"},
            {"Le système Colle", "Structure solide avec e3 et d4"},
            {"Choisir son répertoire d'ouverture", "Comment construire votre arsenal"},

            // Module 2: Stratégie positionnelle (20 cours)
            {"Les structures de pions: introduction", "L'importance de la structure de pions"},
            {"Le pion isolé", "Force et faiblesse du pion d isolé"},
            {"Les pions pendants", "Deux pions côte à côte non protégés"},
            {"Les pions doublés", "Quand sont-ils vraiment faibles?"},
            {"La chaîne de pions", "Attaquer la base de la chaîne"},
            {"Les pions passés", "Leur force en milieu de partie"},
            {"Le pion passé protégé", "Un atout stratégique majeur"},
            {"Les cases faibles", "Comment les exploiter"},
            {"Les avant-postes", "Positions idéales pour les Cavaliers"},
            {"La colonne ouverte", "Dominez avec vos Tours"},
            {"La colonne semi-ouverte", "Pression sur les pions adverses"},
            {"La septième rangée", "Les Tours y sont particulièrement fortes"},
            {"Deux Tours en septième", "Souvent décisif"},
            {"L'attaque sur le roque", "Quand et comment attaquer"},
            {"Le sacrifice grec en h7", "Le sacrifice classique du Fou"},
            {"L'attaque avec les pions", "Ouvrez des lignes contre le roque"},
            {"La défense du roque", "Techniques de défense"},
            {"Le jeu sur les deux ailes", "Alternez les menaces"},
            {"Le centre fermé", "Jouez sur les ailes"},
            {"Le centre ouvert", "L'activité des pièces prime"},

            // Module 3: Finales avancées (15 cours)
            {"Position de Lucena", "Comment gagner avec Tour + Pion"},
            {"Position de Philidor", "La défense parfaite avec Tour"},
            {"Finales de Tours: pion sur la 5ème", "Techniques avancées"},
            {"Finales de Tours: pion sur la 6ème", "Positions critiques"},
            {"Finales Fou + Pion vs Fou même couleur", "Techniques de gain"},
            {"Finales Fous de couleurs opposées", "Pourquoi souvent nulles"},
            {"Finales de Cavaliers", "Les particularités du Cavalier"},
            {"Fou vs Cavalier: évaluation", "Quand l'un est meilleur"},
            {"Tour + Fou vs Tour + Cavalier", "Déséquilibres matériels"},
            {"Dame vs Tour + pièce", "Finales délicates"},
            {"Le principe des deux faiblesses", "Attaquer sur plusieurs fronts"},
            {"La forteresse", "Quand on peut tenir malgré le déficit"},
            {"Le zugzwang avancé", "Forcer l'adversaire à perdre"},
            {"Études de finales célèbres", "Les classiques à connaître"},
            {"Finales pratiques", "Ce qui arrive vraiment en partie"},

            // Module 4: Middlegame et plans (15 cours)
            {"Évaluation positionnelle", "Comment juger une position"},
            {"La majorité de pions", "Créez un pion passé sur une aile"},
            {"L'attaque de minorité", "Technique avec moins de pions"},
            {"Quand échanger les Dames", "Simplifiez au bon moment"},
            {"Quand garder les Dames", "L'attaque nécessite souvent la Dame"},
            {"La transformation de l'avantage", "Convertir un type d'avantage en un autre"},
            {"L'initiative", "Gardez la pression sur l'adversaire"},
            {"Le jeu prophylactique", "Empêchez les plans adverses"},
            {"Améliorer vos pièces", "Le repositionnement stratégique"},
            {"Les échanges favorables", "Quand et quoi échanger"},
            {"Le sacrifice positionnel", "Donner du matériel pour la position"},
            {"Le sacrifice de qualité", "Tour contre pièce mineure"},
            {"Les cases fortes", "Occupez des avant-postes"},
            {"La paire de Fous", "Un avantage à long terme"},
            {"Exercices stratégiques", "Appliquez vos connaissances"}
        };
        return createCoursesForLevel(data, ChessLevel.C, 55);
    }

    // =====================================================
    // NIVEAU D - ROI (Expert) - 80 cours
    // =====================================================
    private List<Course> createLevelDCourses() {
        String[][] data = {
            // Module 1: Préparation et ouvertures avancées (20 cours)
            {"Construire un répertoire complet", "Choisir ses ouvertures principales"},
            {"Préparer contre un adversaire", "L'art de la préparation spécifique"},
            {"Les nouveautés théoriques", "Comment innover en ouverture"},
            {"Utiliser les bases de données", "ChessBase, Lichess, Chess.com"},
            {"L'analyse avec moteur", "Stockfish, Leela - utilisation intelligente"},
            {"La Sicilienne Sveshnikov", "Le pion arriéré en d6"},
            {"La Sicilienne Taimanov", "Flexibilité avec ...a6 et ...Dc7"},
            {"Le Gambit Marshall", "L'attaque spectaculaire dans l'Espagnole"},
            {"La Berlinoise", "Le mur défensif moderne"},
            {"Le système Maroczy contre la Sicilienne", "Contrôle spatial avec c4"},
            {"La structure Hedgehog", "Défense élastique et contre-jeu"},
            {"Les structures Carlsbad", "Plans typiques et attaque minoritaire"},
            {"L'Est-Indienne: variante Sämisch", "Les blancs jouent f3"},
            {"L'Est-Indienne: variante classique", "Les grandes batailles"},
            {"La Nimzo-Indienne: variante Rubinstein", "Plans stratégiques"},
            {"La Semi-Slave", "Complexité et richesse tactique"},
            {"Le Gambit Benko", "Sacrifice de pion pour initiative"},
            {"Les anti-Siciliennes", "Alapin, Grand Prix, Closed"},
            {"Le système Trompowsky", "1.d4 Cf6 2.Fg5"},
            {"Les tendances modernes", "État de l'art théorique"},

            // Module 2: Stratégie avancée (20 cours)
            {"La compensation pour le matériel", "Évaluez les sacrifices"},
            {"L'initiative permanente", "Maintenir la pression sans relâche"},
            {"Les sacrifices spéculatifs", "Quand le calcul ne suffit pas"},
            {"L'attaque sur le Roi non roqué", "Exploiter le retard de développement"},
            {"L'attaque avec roques opposés", "Course aux attaques"},
            {"Le Fou dominant", "Quand le Fou surpasse le Cavalier"},
            {"Le Cavalier dominant", "Positions fermées et avant-postes forts"},
            {"Fous de couleurs opposées: middlegame", "Potentiel d'attaque"},
            {"L'évaluation dynamique vs statique", "Quand les facteurs changent"},
            {"Les positions critiques", "Identifier les moments clés"},
            {"Prophylaxie avancée", "Pensez aux coups de l'adversaire"},
            {"La restriction maximale", "Paralysez les pièces adverses"},
            {"Garder la tension", "Ne résolvez pas prématurément"},
            {"Le plan à long terme", "Vision sur plusieurs coups"},
            {"Changer de plan", "Flexibilité stratégique"},
            {"Le coup d'attente", "Forcer l'adversaire à se compromettre"},
            {"L'amélioration progressive", "Optimisez pièce par pièce"},
            {"Technique de réalisation", "Convertir un avantage"},
            {"Défendre les positions inférieures", "L'art de la défense"},
            {"Créer des complications", "Quand vous êtes en difficulté"},

            // Module 3: Finales d'expert (20 cours)
            {"Finales de Tours: la 3ème rangée", "Défense avec Tour active"},
            {"Finales de Tours: le pont", "Technique de gain"},
            {"Finales avec Tours et pions", "Principes généraux"},
            {"Finales Tour + Fou vs Tour", "L'avantage du Fou"},
            {"Finales Tour + Cavalier vs Tour", "Généralement nulles"},
            {"Les finales de Dames", "Échecs perpétuels et gains"},
            {"Dame + Pion vs Dame", "Positions gagnantes et nulles"},
            {"Dame vs 2 Tours", "Évaluations précises"},
            {"La théorie des cases correspondantes", "Zugzwang géométrique"},
            {"Finales avec Fous de même couleur", "Technique avancée"},
            {"Finales avec pièces mineures", "Nuances et techniques"},
            {"Le triangle de Réti", "Études célèbres"},
            {"Études de Troitzky", "Finales artistiques"},
            {"Le principe de l'élimination", "Réduire les options adverses"},
            {"Finales pratiques de tournoi", "Ce qui compte vraiment"},
            {"Les erreurs en finale", "Comment les éviter"},
            {"Le zeitnot en finale", "Gérer le temps"},
            {"Finales complexes avec matériel inégal", "Déséquilibres"},
            {"Études de finales modernes", "Les découvertes récentes"},
            {"La technique de Carlsen", "Apprendre du champion"},

            // Module 4: Psychologie et compétition (20 cours)
            {"La préparation psychologique", "L'aspect mental du jeu"},
            {"Gérer le stress en tournoi", "Techniques de relaxation"},
            {"Jouer contre différents styles", "S'adapter à l'adversaire"},
            {"La gestion du temps", "Utilisez votre temps intelligemment"},
            {"Le calcul des variantes avancé", "Méthode systématique"},
            {"La visualisation", "Voir les positions dans sa tête"},
            {"Les coups candidats", "Identifier les meilleurs coups"},
            {"L'intuition aux échecs", "Quand faire confiance à son instinct"},
            {"L'analyse post-mortem", "Apprenez de vos parties"},
            {"Utiliser l'ordinateur pour progresser", "Analyse et préparation"},
            {"Les champions du monde: de Steinitz à Fischer", "Styles historiques"},
            {"Les champions du monde: de Karpov à Carlsen", "L'ère moderne"},
            {"Le jeu positionnel de Karpov", "La méthode du Boa"},
            {"Le jeu dynamique de Kasparov", "L'attaque comme philosophie"},
            {"Le style universel de Carlsen", "Adaptabilité maximale"},
            {"Préparer un tournoi important", "Routine du joueur sérieux"},
            {"Les parties à enjeux", "Gérer la pression"},
            {"Le blitz et le rapide", "Adapter son jeu au format"},
            {"Maintenir son niveau", "L'entraînement continu"},
            {"Vers la maîtrise absolue", "Le voyage ne fait que commencer"}
        };
        return createCoursesForLevel(data, ChessLevel.D, 60);
    }

    private List<Course> createCoursesForLevel(String[][] data, ChessLevel level, int baseMinutes) {
        List<Course> courses = new ArrayList<>();
        for (int i = 0; i < data.length; i++) {
            Course course = new Course();
            course.setTitle(data[i][0]);
            course.setDescription(data[i][1]);
            course.setIconName("heroAcademicCap");
            course.setContent("# " + data[i][0] + "\n\n" + data[i][1] + "\n\n*Contenu du cours à venir...*");
            course.setGrade(level);
            course.setOrderInGrade(i + 1);
            course.setEstimatedMinutes(baseMinutes + (i % 15));
            courses.add(course);
        }
        return courses;
    }
}
