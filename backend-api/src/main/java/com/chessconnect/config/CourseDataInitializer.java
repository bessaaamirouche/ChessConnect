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

            // Grade PION (Débutant) - 45 cours
            courses.addAll(createPionCourses());

            // Grade CAVALIER (Intermédiaire) - 45 cours
            courses.addAll(createCavalierCourses());

            // Grade FOU (Confirmé) - 45 cours
            courses.addAll(createFouCourses());

            // Grade TOUR (Avancé) - 45 cours
            courses.addAll(createTourCourses());

            // Grade DAME (Expert) - 45 cours
            courses.addAll(createDameCourses());

            // Grade ROI (Maître) - 45 cours
            courses.addAll(createRoiCourses());

            courseRepository.saveAll(courses);
            System.out.println("✓ " + courses.size() + " cours initialisés avec succès");
        }
    }

    private List<Course> createPionCourses() {
        String[][] data = {
            {"L'échiquier et la notation", "Découvrez le plateau de jeu et apprenez la notation algébrique"},
            {"Le mouvement du Roi", "Apprenez à déplacer la pièce la plus importante"},
            {"Le mouvement de la Dame", "Maîtrisez les déplacements de la pièce la plus puissante"},
            {"Le mouvement de la Tour", "Comprenez les mouvements horizontaux et verticaux"},
            {"Le mouvement du Fou", "Apprenez à utiliser les diagonales"},
            {"Le mouvement du Cavalier", "Découvrez le mouvement en L unique du Cavalier"},
            {"Le mouvement du Pion", "Maîtrisez les subtilités du Pion"},
            {"La prise en passant", "Découvrez ce coup spécial du Pion"},
            {"La promotion du Pion", "Transformez vos Pions en pièces puissantes"},
            {"Le petit roque", "Mettez votre Roi en sécurité côté roi"},
            {"Le grand roque", "Mettez votre Roi en sécurité côté dame"},
            {"Les conditions du roque", "Quand peut-on roquer ?"},
            {"L'échec au Roi", "Comprendre et donner des échecs"},
            {"Parer un échec", "Les trois façons de sortir d'un échec"},
            {"L'échec et mat", "L'objectif ultime du jeu"},
            {"Le pat et la nulle", "Quand la partie est nulle"},
            {"La valeur des pièces", "Comparez la force de chaque pièce"},
            {"Les échanges favorables", "Quand échanger ses pièces"},
            {"Le mat du couloir", "Un mat classique à connaître"},
            {"Le mat avec Dame et Roi", "Technique de base du mat"},
            {"Le mat avec deux Tours", "L'escalier vers le mat"},
            {"Le mat avec Tour et Roi", "Technique essentielle de finale"},
            {"Le mat du berger", "Évitez ce piège de débutant"},
            {"Le mat de l'écolier", "Un autre piège classique"},
            {"Défendre f7/f2", "Protégez ces cases vulnérables"},
            {"Les premiers coups", "Comment bien débuter une partie"},
            {"Contrôler le centre", "L'importance des cases centrales"},
            {"Développer ses pièces", "Activez votre armée rapidement"},
            {"La sécurité du Roi", "Roquez tôt pour protéger votre Roi"},
            {"Ne pas sortir la Dame trop tôt", "Une erreur classique à éviter"},
            {"Connecter les Tours", "Finalisez votre développement"},
            {"Les pièces en prise", "Ne donnez pas de matériel gratuit"},
            {"Vérifier les menaces adverses", "Avant chaque coup, regardez !"},
            {"Les erreurs de débutant", "Pièges à éviter absolument"},
            {"Jouer avec un plan", "Donnez un but à vos coups"},
            {"La règle touché-joué", "Les règles officielles du jeu"},
            {"Le temps aux échecs", "Comprendre la pendule"},
            {"Notation d'une partie", "Enregistrez vos parties"},
            {"Analyser ses parties", "Progressez en étudiant vos erreurs"},
            {"Les sites de jeu en ligne", "Où pratiquer les échecs"},
            {"L'étiquette aux échecs", "Le fair-play et le respect"},
            {"Gagner, perdre, apprendre", "L'attitude du bon joueur"},
            {"Les échecs et la patience", "Prenez votre temps pour réfléchir"},
            {"Révision des bases", "Consolidez vos connaissances"},
            {"Votre première partie guidée", "Appliquez tout ce que vous avez appris"}
        };
        return createCoursesForLevel(data, ChessLevel.PION, 10);
    }

    private List<Course> createCavalierCourses() {
        String[][] data = {
            {"La fourchette", "Attaquez deux pièces simultanément"},
            {"La fourchette du Cavalier", "Le Cavalier roi des fourchettes"},
            {"La fourchette de Pion", "Même le Pion peut fourchetter"},
            {"Le clouage absolu", "Immobilisez une pièce devant le Roi"},
            {"Le clouage relatif", "Clouage sur une pièce importante"},
            {"Exploiter un clouage", "Profitez des pièces clouées"},
            {"L'enfilade", "L'inverse du clouage"},
            {"La brochette", "Attaque en ligne sur deux pièces"},
            {"L'attaque double", "Créez deux menaces à la fois"},
            {"L'attaque à la découverte", "Révélez une attaque cachée"},
            {"L'échec à la découverte", "Un échec surprise"},
            {"L'échec double", "Deux pièces donnent échec"},
            {"Le sacrifice d'attraction", "Attirez une pièce sur une mauvaise case"},
            {"Le sacrifice de déviation", "Éloignez un défenseur"},
            {"Le sacrifice de destruction", "Détruisez un défenseur clé"},
            {"Les motifs tactiques combinés", "Enchaînez plusieurs tactiques"},
            {"L'attaque sur f7/f2", "Exploitez ces faiblesses"},
            {"Le mat en 2 coups", "Calcul de combinaisons simples"},
            {"Le mat en 3 coups", "Combinaisons plus complexes"},
            {"Exercices de tactique niveau 1", "Pratiquez les bases"},
            {"La défense passive", "Protégez simplement vos pièces"},
            {"La défense active", "Défendez en contre-attaquant"},
            {"Parer les menaces tactiques", "Anticipez les coups adverses"},
            {"Finale Roi + Dame vs Roi", "Technique du mat basique"},
            {"Finale Roi + Tour vs Roi", "L'escalier et le mat"},
            {"Finale Roi + 2 Fous vs Roi", "Coordination des Fous"},
            {"Finale Roi + Fou + Cavalier vs Roi", "Le mat le plus difficile"},
            {"Le mat du coin", "Pousser le Roi dans le coin"},
            {"L'opposition simple", "Concept clé des finales"},
            {"La règle du carré", "Le Roi peut-il rattraper le Pion ?"},
            {"Pion passé en finale", "La course vers la promotion"},
            {"Principes de l'ouverture", "Règles d'or du début de partie"},
            {"L'ouverture Italienne - introduction", "1.e4 e5 2.Cf3 Cc6 3.Fc4"},
            {"L'ouverture Espagnole - introduction", "1.e4 e5 2.Cf3 Cc6 3.Fb5"},
            {"La défense Française - introduction", "1.e4 e6"},
            {"La défense Sicilienne - introduction", "1.e4 c5"},
            {"Le Gambit Dame - introduction", "1.d4 d5 2.c4"},
            {"La défense Indienne - introduction", "Systèmes avec ...Cf6 et ...g6"},
            {"Pièges d'ouverture à connaître", "Ne tombez pas dedans !"},
            {"L'activité des pièces", "Des pièces actives gagnent"},
            {"La coordination des pièces", "Faites travailler ensemble vos pièces"},
            {"Les Tours sur les colonnes ouvertes", "Activez vos Tours"},
            {"Le Fou bon vs le Fou mauvais", "Évaluez vos Fous"},
            {"Cavalier vs Fou", "Quand préférer l'un ou l'autre"},
            {"Exercices de tactique niveau 2", "Consolidez vos acquis tactiques"}
        };
        return createCoursesForLevel(data, ChessLevel.CAVALIER, 15);
    }

    private List<Course> createFouCourses() {
        String[][] data = {
            {"L'ouverture Italienne - approfondie", "Variantes principales et plans"},
            {"L'ouverture Espagnole - approfondie", "La Ruy Lopez en détail"},
            {"La défense Sicilienne Najdorf", "La variante la plus populaire"},
            {"La défense Sicilienne Dragon", "L'attaque avec ...g6"},
            {"La défense Française avancée", "Plans typiques après 3.e5"},
            {"La défense Caro-Kann", "Une défense solide contre 1.e4"},
            {"La défense Scandinave", "1.e4 d5 - défense directe"},
            {"Le Gambit Dame accepté", "2...dxc4 et ses suites"},
            {"Le Gambit Dame refusé", "2...e6 - la réponse classique"},
            {"La défense Slave", "Solide et flexible"},
            {"La défense Nimzo-Indienne", "Contrôle stratégique avec ...Fb4"},
            {"La défense Est-Indienne", "Plans d'attaque sur l'aile roi"},
            {"L'ouverture Anglaise", "1.c4 - le jeu positionnel"},
            {"L'ouverture Réti", "1.Cf3 - le jeu hypermoderne"},
            {"Les structures de pions isolés", "Le pion d isolé"},
            {"Les structures de pions pendants", "Forces et faiblesses"},
            {"Les pions doublés", "Quand sont-ils faibles ?"},
            {"La chaîne de pions", "Attaquer la base"},
            {"Les pions passés", "Leur force en milieu de partie"},
            {"Le pion passé protégé", "Un atout majeur"},
            {"Les cases faibles", "Occupez-les avec vos pièces"},
            {"Les avant-postes", "Positions idéales pour les Cavaliers"},
            {"La colonne ouverte", "Dominez avec vos Tours"},
            {"La colonne semi-ouverte", "Pression sur les pions adverses"},
            {"La 7ème rangée", "Les Tours y sont puissantes"},
            {"Deux Tours en 7ème", "Souvent décisif"},
            {"L'attaque sur le roque", "Conditions nécessaires"},
            {"Le sacrifice en h7", "Le sacrifice grec classique"},
            {"L'attaque avec pions", "Ouvrez des lignes"},
            {"La défense du roque", "Protégez votre Roi"},
            {"Le jeu sur les deux ailes", "Alternez les menaces"},
            {"Le centre fermé", "Jouez sur les ailes"},
            {"Le centre ouvert", "L'activité prime"},
            {"Quand échanger les Dames", "Simplifiez à bon escient"},
            {"Quand garder les Dames", "L'attaque nécessite la Dame"},
            {"La majorité de pions", "Créez un pion passé"},
            {"La minorité de pions", "Attaque de minorité"},
            {"Les finales de Tours - bases", "Les plus courantes"},
            {"Position de Lucena", "Gagner avec Tour + Pion"},
            {"Position de Philidor", "Défendre avec Tour + Pion"},
            {"Les finales de Fous de même couleur", "Techniques de gain"},
            {"Les finales de Fous de couleurs opposées", "Souvent nulles"},
            {"Les finales de Cavaliers", "Particularités du Cavalier"},
            {"Évaluation positionnelle", "Analysez une position"},
            {"Exercices de stratégie niveau 1", "Appliquez vos connaissances"}
        };
        return createCoursesForLevel(data, ChessLevel.FOU, 20);
    }

    private List<Course> createTourCourses() {
        String[][] data = {
            {"Préparation d'ouverture", "Comment étudier une ouverture"},
            {"Le répertoire d'ouvertures", "Construisez votre répertoire"},
            {"Les nouveautés théoriques", "Surprenez vos adversaires"},
            {"L'analyse avec moteur", "Utilisez l'ordinateur intelligemment"},
            {"Les bases de données de parties", "Étudiez les parties de maîtres"},
            {"L'opposition distante", "Opposition à plusieurs cases"},
            {"Le zugzwang", "Quand jouer est un désavantage"},
            {"Les finales de Tours avancées", "Techniques de gain et de défense"},
            {"La Tour active vs Tour passive", "L'activité prime"},
            {"Finales Tour + Pion vs Tour", "Études approfondies"},
            {"Finales Tour + 2 Pions vs Tour + Pion", "Évaluations précises"},
            {"Les finales de pions complexes", "Calcul précis requis"},
            {"Pions passés liés", "Une force considérable"},
            {"Roi actif en finale", "Le Roi devient une pièce forte"},
            {"Le triangle de Réti", "Études célèbres de finales"},
            {"La prophylaxie", "Empêchez les plans adverses"},
            {"Le coup d'attente", "Passez le trait à l'adversaire"},
            {"L'amélioration des pièces", "Optimisez vos pièces une par une"},
            {"La restriction", "Limitez les options adverses"},
            {"Les échanges favorables", "Quand simplifier"},
            {"Garder la tension", "Ne résolvez pas trop tôt"},
            {"Le plan à long terme", "Pensez plusieurs coups à l'avance"},
            {"Changer de plan", "Adaptez-vous à la position"},
            {"L'initiative", "Gardez la pression"},
            {"La compensation pour le matériel", "Évaluez les sacrifices"},
            {"L'attaque sur le Roi non roqué", "Exploitez le retard de développement"},
            {"L'attaque sur roques opposés", "Course aux attaques"},
            {"Sacrifices positionnels", "Donnez du matériel pour la position"},
            {"Le sacrifice de qualité", "Tour contre pièce mineure"},
            {"Les cases fortes", "Occupez les avant-postes"},
            {"Le Fou dominant", "Quand le Fou est supérieur"},
            {"Le Cavalier dominant", "Positions fermées et avant-postes"},
            {"La paire de Fous", "Un avantage durable"},
            {"Fous de couleurs opposées - milieu de partie", "Potentiel d'attaque"},
            {"Les pièces mineures en finale", "Fou vs Cavalier avancé"},
            {"L'évaluation dynamique", "Facteurs dynamiques vs statiques"},
            {"Les positions critiques", "Moments clés de la partie"},
            {"La gestion du temps", "Utilisez votre temps efficacement"},
            {"Le calcul des variantes", "Méthode de calcul"},
            {"La visualisation", "Voir les coups dans sa tête"},
            {"Les coups candidats", "Identifiez les meilleurs coups"},
            {"L'élimination des coups", "Processus de décision"},
            {"L'intuition aux échecs", "Quand faire confiance à son instinct"},
            {"Études de parties de maîtres", "Apprenez des meilleurs"},
            {"Exercices de stratégie niveau 2", "Positions complexes"}
        };
        return createCoursesForLevel(data, ChessLevel.TOUR, 25);
    }

    private List<Course> createDameCourses() {
        String[][] data = {
            {"L'analyse approfondie des parties", "Méthode professionnelle"},
            {"La préparation psychologique", "L'aspect mental du jeu"},
            {"Jouer contre différents styles", "Adaptez votre jeu"},
            {"Les sacrifices spéculatifs", "Risque calculé"},
            {"L'attaque et la défense simultanées", "L'équilibre parfait"},
            {"Les positions dynamiques", "Quand le temps compte"},
            {"Les positions statiques", "Accumulation d'avantages"},
            {"La transformation des avantages", "Convertissez vos atouts"},
            {"Le jeu technique", "Réalisez votre avantage"},
            {"Défendre les positions difficiles", "L'art de la défense"},
            {"Les ressources défensives", "Trouvez les échappatoires"},
            {"Le contre-jeu", "Créez des problèmes à l'attaquant"},
            {"Les finales théoriques", "Connaissances essentielles"},
            {"Les finales de Tours complexes", "Maîtrise complète"},
            {"Les finales de pièces mineures", "Nuances et techniques"},
            {"Les finales de Dames", "Techniques spéciales"},
            {"Études de finales célèbres", "Les classiques à connaître"},
            {"La préparation en tournoi", "Avant la compétition"},
            {"La gestion de tournoi", "Pendant la compétition"},
            {"L'analyse post-tournoi", "Après la compétition"},
            {"Jouer contre des adversaires plus forts", "Tirez le meilleur de vous-même"},
            {"Jouer contre des adversaires plus faibles", "Ne sous-estimez personne"},
            {"Les parties rapides et blitz", "Adaptez votre jeu au temps"},
            {"Les parties longues", "La réflexion approfondie"},
            {"Le style universel", "Maîtrisez tous les aspects"},
            {"Les parties modèles - tactique", "Combinaisons brillantes"},
            {"Les parties modèles - stratégie", "Plans magistraux"},
            {"Les parties modèles - finales", "Technique parfaite"},
            {"Les champions du monde - Steinitz à Capablanca", "Les fondateurs"},
            {"Les champions du monde - Alekhine à Botvinnik", "L'ère soviétique"},
            {"Les champions du monde - Smyslov à Spassky", "L'âge d'or"},
            {"Les champions du monde - Fischer", "Le génie américain"},
            {"Les champions du monde - Karpov à Kasparov", "La grande rivalité"},
            {"Les champions du monde - Kramnik à Carlsen", "L'ère moderne"},
            {"L'évolution des échecs", "Du romantisme à l'ère informatique"},
            {"Les échecs et l'informatique", "L'impact des moteurs"},
            {"Préparer avec l'ordinateur", "Utilisation optimale"},
            {"Au-delà de l'ordinateur", "Ce que la machine ne voit pas"},
            {"Créer son style personnel", "Exprimez votre personnalité"},
            {"La philosophie du jeu", "Approche mentale du champion"},
            {"Enseigner les échecs", "Transmettez votre savoir"},
            {"La compétition de haut niveau", "Exigences du jeu professionnel"},
            {"Maintenir son niveau", "L'entraînement continu"},
            {"La passion des échecs", "Cultivez votre amour du jeu"},
            {"Vers la maîtrise complète", "Le chemin ne s'arrête jamais"}
        };
        return createCoursesForLevel(data, ChessLevel.DAME, 30);
    }

    private List<Course> createRoiCourses() {
        String[][] data = {
            {"La théorie des cases correspondantes", "Maîtrisez les finales de pions complexes"},
            {"Les finales de Tours théoriques avancées", "Positions critiques à connaître"},
            {"Le principe des deux faiblesses", "Exploitez les faiblesses multiples"},
            {"La forteresse", "L'art de construire une défense imprenable"},
            {"Les sacrifices de pièce pour l'attaque", "Quand sacrifier vaut le gain"},
            {"L'attaque Anglaise contre la Najdorf", "Préparation d'une arme redoutable"},
            {"Les structures Maroczy Bind", "Domination spatiale stratégique"},
            {"La transformation des avantages", "Convertissez vos atouts en victoire"},
            {"Les finales de Cavalier complexes", "Subtilités du Cavalier en finale"},
            {"L'initiative permanente", "Gardez la pression sans relâche"},
            {"Les sacrifices de qualité modernes", "Quand la Tour vaut moins que la position"},
            {"Les positions IQP avancées", "Pion Dame isolé: force ou faiblesse?"},
            {"La préparation spécifique", "Préparer contre un adversaire précis"},
            {"Les structures de pions Carlsbad", "Plans typiques et manœuvres"},
            {"L'attaque minoritaire approfondie", "Quand et comment l'appliquer"},
            {"Les finales Fou contre Cavalier", "Évaluation précise"},
            {"Le zeitnot stratégique", "Profiter du manque de temps adverse"},
            {"Les coups prophylactiques avancés", "Anticipez les menaces cachées"},
            {"La restriction maximale", "Paralysez les pièces adverses"},
            {"Les études de finales célèbres", "Trouvailles géniales à connaître"},
            {"Le jeu en zeitnot", "Technique sous pression temporelle"},
            {"Les parties à double tranchant", "Naviguer dans le chaos calculé"},
            {"L'évaluation des positions non-standard", "Au-delà des règles classiques"},
            {"Les gambits positionnels modernes", "Sacrifices d'espace et de temps"},
            {"La défense des positions inférieures", "Maximisez vos chances de sauvetage"},
            {"Les finales de pions avec Roi actif", "Technique du Roi centralisé"},
            {"L'ouverture Catalane approfondie", "Plans stratégiques et pièges"},
            {"La variante Sveshnikov de la Sicilienne", "Comprendre le pion arriéré d5"},
            {"Les structures Hedgehog", "Défense élastique et contre-jeu"},
            {"Le jeu avec deux résultats possibles", "Jouer pour gagner ou annuler"},
            {"Les positions avec roques opposés", "Course aux attaques maîtrisée"},
            {"L'analyse de parties de champions", "Méthode d'étude approfondie"},
            {"Les nouveautés théoriques profondes", "Comment innover en ouverture"},
            {"La préparation psychologique avancée", "Gérer la pression du haut niveau"},
            {"Les parties lentes vs parties rapides", "Adapter son approche au format"},
            {"Le blitz de haut niveau", "Réflexes et intuition aiguisés"},
            {"Les tendances modernes des ouvertures", "État de l'art théorique"},
            {"L'utilisation avancée des moteurs", "Analyse assistée intelligente"},
            {"Les finales de Dames complexes", "Technique et calcul précis"},
            {"Le jeu positionnel de Karpov", "La méthode du Boa"},
            {"Le jeu dynamique de Kasparov", "L'attaque comme philosophie"},
            {"Le style universel de Carlsen", "Adaptabilité maximale"},
            {"La préparation en tournoi d'élite", "Routine du joueur professionnel"},
            {"L'analyse post-mortem approfondie", "Tirez le maximum de vos parties"},
            {"Vers la maîtrise absolue", "Le voyage ne fait que commencer"}
        };
        return createCoursesForLevel(data, ChessLevel.ROI, 35);
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
            course.setEstimatedMinutes(baseMinutes + (i % 10));
            courses.add(course);
        }
        return courses;
    }
}
