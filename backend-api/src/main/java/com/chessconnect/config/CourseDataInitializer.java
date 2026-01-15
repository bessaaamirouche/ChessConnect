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

            // Grade PION (Débutant) - 10 cours
            courses.addAll(createPionCourses());

            // Grade CAVALIER (Intermédiaire) - 10 cours
            courses.addAll(createCavalierCourses());

            // Grade FOU (Confirmé) - 10 cours
            courses.addAll(createFouCourses());

            // Grade TOUR (Avancé) - 10 cours
            courses.addAll(createTourCourses());

            // Grade DAME (Expert) - 10 cours
            courses.addAll(createDameCourses());

            courseRepository.saveAll(courses);
            System.out.println("✓ 50 cours initialisés avec succès");
        }
    }

    private List<Course> createPionCourses() {
        List<Course> courses = new ArrayList<>();
        String[][] data = {
            {"Les règles de base", "Découvrez les fondamentaux du jeu d'échecs", "heroBookOpen",
             "# Les règles de base\n\nBienvenue dans votre première leçon d'échecs !\n\n## L'objectif du jeu\n\nLe but des échecs est de mettre le roi adverse en **échec et mat**. Cela signifie que le roi est attaqué et ne peut s'échapper.\n\n## Le plateau\n\n- L'échiquier comporte 64 cases (8x8)\n- Les cases alternent entre claires et foncées\n- Le plateau se place avec une case blanche en bas à droite\n\n## Les pièces\n\nChaque joueur commence avec :\n- 1 Roi\n- 1 Dame\n- 2 Tours\n- 2 Fous\n- 2 Cavaliers\n- 8 Pions"},
            {"Le mouvement des pièces", "Apprenez comment déplacer chaque pièce", "heroArrowsPointingOut",
             "# Le mouvement des pièces\n\n## Le Roi ♔\nLe roi se déplace d'une case dans n'importe quelle direction.\n\n## La Dame ♕\nLa dame combine les mouvements de la tour et du fou.\n\n## La Tour ♖\nLa tour se déplace horizontalement ou verticalement.\n\n## Le Fou ♗\nLe fou se déplace en diagonale.\n\n## Le Cavalier ♘\nLe cavalier se déplace en \"L\" et peut sauter par-dessus les autres pièces.\n\n## Le Pion ♙\nLe pion avance d'une case (deux cases depuis sa position initiale) et capture en diagonale."},
            {"L'échiquier et la notation", "Maîtrisez la notation algébrique", "heroTableCells",
             "# L'échiquier et la notation\n\n## La notation algébrique\n\nChaque case a une coordonnée unique :\n- Colonnes : a, b, c, d, e, f, g, h\n- Rangées : 1, 2, 3, 4, 5, 6, 7, 8\n\n## Exemples\n- e4 : la case e4\n- Cf3 : Cavalier en f3\n- Fxc6 : Fou prend en c6\n\n## Symboles importants\n- x : capture\n- + : échec\n- # : mat\n- O-O : petit roque\n- O-O-O : grand roque"},
            {"Le roque et la prise en passant", "Découvrez les coups spéciaux", "heroSparkles",
             "# Les coups spéciaux\n\n## Le Roque\n\nLe roque permet de mettre le roi en sécurité tout en activant la tour.\n\n### Conditions :\n- Ni le roi ni la tour n'ont bougé\n- Aucune pièce entre le roi et la tour\n- Le roi n'est pas en échec\n- Le roi ne traverse pas une case attaquée\n\n## La prise en passant\n\nQuand un pion avance de deux cases et se retrouve à côté d'un pion adverse, celui-ci peut le capturer \"en passant\"."},
            {"Le mat en 1 coup", "Apprenez à reconnaître les mats simples", "heroTrophy",
             "# Le mat en 1 coup\n\n## Qu'est-ce qu'un mat ?\n\nLe mat survient quand le roi est attaqué et ne peut pas :\n- Capturer l'attaquant\n- Bloquer l'attaque\n- S'échapper\n\n## Le mat du couloir\n\nLe roi coincé sur la dernière rangée par ses propres pions.\n\n## Le mat à l'étouffée\n\nLe roi entouré de ses propres pièces et maté par un cavalier.\n\n## Exercice\n\nRepérez les positions de mat dans vos parties !"},
            {"Les échecs simples", "Donnez et parez les échecs", "heroExclamationTriangle",
             "# Les échecs simples\n\n## Qu'est-ce qu'un échec ?\n\nUn échec est une attaque directe sur le roi.\n\n## Comment parer un échec ?\n\n1. **Capturer** l'attaquant\n2. **Bloquer** avec une autre pièce\n3. **Fuir** avec le roi\n\n## L'échec double\n\nQuand deux pièces attaquent le roi simultanément, seule la fuite est possible.\n\n## Conseil\n\nNe donnez pas d'échecs inutiles - chaque coup doit avoir un but !"},
            {"La valeur des pièces", "Comprenez l'importance de chaque pièce", "heroScale",
             "# La valeur des pièces\n\n## Valeurs standards\n\n- Pion : 1 point\n- Cavalier : 3 points\n- Fou : 3 points\n- Tour : 5 points\n- Dame : 9 points\n- Roi : invaluable\n\n## Conseils\n\n- Ne perdez pas de matériel gratuitement\n- Un fou et un cavalier valent presque une tour\n- Deux tours valent plus qu'une dame\n\n## L'exception\n\nParfois, la position est plus importante que le matériel !"},
            {"Les premiers coups d'une partie", "Débutez correctement vos parties", "heroPlay",
             "# Les premiers coups\n\n## Principes de l'ouverture\n\n1. **Contrôlez le centre** (e4, d4, e5, d5)\n2. **Développez vos pièces** (cavaliers avant fous)\n3. **Roquez rapidement** pour protéger le roi\n4. **Connectez vos tours**\n\n## Erreurs à éviter\n\n- Ne bougez pas la même pièce deux fois\n- Ne sortez pas la dame trop tôt\n- Ne négligez pas le développement\n\n## Premier coup recommandé\n\n1.e4 ou 1.d4 sont excellents pour les débutants."},
            {"Les erreurs courantes", "Évitez les pièges du débutant", "heroXCircle",
             "# Les erreurs courantes\n\n## Le mat du berger\n\nAttention à 1.e4 e5 2.Fc4 Cc6 3.Dh5 et menace de mat en f7 !\n\n## Défense : protégez f7/f2\n\nCe sont les cases les plus faibles en début de partie.\n\n## Erreurs typiques\n\n- Laisser des pièces en prise\n- Oublier de roquer\n- Jouer sans plan\n- Ne pas regarder les menaces adverses\n\n## Conseil d'or\n\nAvant chaque coup, demandez-vous : \"Quelle est la menace de mon adversaire ?\""},
            {"Votre première partie complète", "Jouez une partie entière", "heroFlag",
             "# Votre première partie\n\n## Récapitulatif\n\nVous avez appris :\n- Les règles de base\n- Le mouvement des pièces\n- La notation\n- Les coups spéciaux\n- Les mats simples\n\n## Conseils pour votre première partie\n\n1. Prenez votre temps\n2. Contrôlez le centre\n3. Développez toutes vos pièces\n4. Roquez\n5. Attaquez quand vous êtes prêt\n\n## Félicitations !\n\nVous êtes prêt à jouer votre première partie. Bonne chance !"}
        };

        for (int i = 0; i < data.length; i++) {
            Course course = new Course();
            course.setTitle(data[i][0]);
            course.setDescription(data[i][1]);
            course.setIconName(data[i][2]);
            course.setContent(data[i][3]);
            course.setGrade(ChessLevel.PION);
            course.setOrderInGrade(i + 1);
            course.setEstimatedMinutes(10 + (i * 2));
            courses.add(course);
        }

        return courses;
    }

    private List<Course> createCavalierCourses() {
        List<Course> courses = new ArrayList<>();
        String[][] data = {
            {"Les tactiques de base - la fourchette", "Attaquez deux pièces simultanément", "heroArrowsPointingOut",
             "# La Fourchette\n\n## Définition\n\nUne fourchette est une attaque simultanée sur deux pièces ou plus.\n\n## Le cavalier fourcheur\n\nLe cavalier est excellent pour les fourchettes car il saute par-dessus les pièces.\n\n## Exemple classique\n\nLe cavalier en e7 attaque le roi en g8 et la tour en c8.\n\n## Exercice\n\nCherchez toujours les possibilités de fourchette dans vos parties !"},
            {"Le clouage", "Immobilisez les pièces adverses", "heroPaperClip",
             "# Le Clouage\n\n## Définition\n\nUn clouage empêche une pièce de bouger car elle protège une pièce plus importante.\n\n## Types de clouage\n\n- **Clouage absolu** : la pièce derrière est le roi\n- **Clouage relatif** : la pièce derrière est plus importante\n\n## Pièces cloueuses\n\n- Le fou\n- La tour\n- La dame\n\n## Conseil\n\nExploitez les pièces clouées, elles ne peuvent pas se défendre !"},
            {"L'enfilade et la brochette", "Forcez les gains matériels", "heroArrowLongRight",
             "# L'Enfilade et la Brochette\n\n## L'Enfilade\n\nContraire du clouage : on attaque la pièce importante qui doit bouger, exposant une pièce derrière.\n\n## La Brochette\n\nAttaque en ligne sur deux pièces, forçant la plus forte à bouger.\n\n## Exemple\n\nUne tour attaque le roi et la dame derrière lui."},
            {"L'attaque double", "Créez des menaces multiples", "heroArrowsExpand",
             "# L'Attaque Double\n\n## Principe\n\nCréer deux menaces simultanées que l'adversaire ne peut pas parer toutes les deux.\n\n## Types\n\n- Échec + attaque sur une pièce\n- Menace de mat + gain de matériel\n- Double attaque avec une seule pièce\n\n## La découverte\n\nUne pièce se déplace et en révèle une autre qui attaque."},
            {"Les sacrifices simples", "Donnez pour mieux recevoir", "heroGift",
             "# Les Sacrifices Simples\n\n## Définition\n\nAbandonner du matériel pour obtenir un avantage plus grand.\n\n## Types de sacrifices\n\n- Sacrifice de déviation\n- Sacrifice d'attraction\n- Sacrifice de destruction\n\n## Exemple célèbre\n\nLe sacrifice de fou en h7 pour exposer le roi."},
            {"La défense des pièces", "Protégez efficacement votre matériel", "heroShield",
             "# La Défense des Pièces\n\n## Principes\n\n1. Assurez-vous que chaque pièce est protégée\n2. Identifiez les pièces en prise\n3. Anticipez les attaques\n\n## Méthodes de défense\n\n- Déplacer la pièce\n- Interposer une pièce\n- Capturer l'attaquant\n- Protéger avec une autre pièce"},
            {"Les finales Roi + Tour", "Maîtrisez cette finale essentielle", "heroChess",
             "# Finale Roi + Tour vs Roi\n\n## Technique\n\n1. Poussez le roi adverse vers le bord\n2. Utilisez la technique de l'escalier\n3. Donnez le mat sur la dernière rangée\n\n## Méthode de l'escalier\n\nLa tour coupe le roi adverse, rangée par rangée."},
            {"Les finales Roi + Dame", "Le mat avec la dame", "heroStar",
             "# Finale Roi + Dame vs Roi\n\n## Technique\n\n1. La dame limite les cases du roi\n2. Le roi s'approche\n3. Mat sur le bord\n\n## Attention au pat !\n\nNe coincez pas le roi sans lui donner échec !"},
            {"L'activité des pièces", "Rendez vos pièces actives", "heroBolt",
             "# L'Activité des Pièces\n\n## Principe\n\nUne pièce active contrôle plus de cases et a plus d'options.\n\n## Comment activer vos pièces\n\n- Centralisez-les\n- Donnez-leur des cibles\n- Coordonnez-les entre elles"},
            {"La coordination des pièces", "Faites travailler vos pièces ensemble", "heroUserGroup",
             "# La Coordination\n\n## Principe\n\nVos pièces sont plus fortes quand elles travaillent ensemble.\n\n## Exemples\n\n- Deux tours sur la même colonne\n- Fou et dame sur une diagonale\n- Cavalier et fou qui se complètent"}
        };

        for (int i = 0; i < data.length; i++) {
            Course course = new Course();
            course.setTitle(data[i][0]);
            course.setDescription(data[i][1]);
            course.setIconName(data[i][2]);
            course.setContent(data[i][3]);
            course.setGrade(ChessLevel.CAVALIER);
            course.setOrderInGrade(i + 1);
            course.setEstimatedMinutes(15 + (i * 2));
            courses.add(course);
        }

        return courses;
    }

    private List<Course> createFouCourses() {
        List<Course> courses = new ArrayList<>();
        String[][] data = {
            {"Les ouvertures classiques", "Découvrez les grandes ouvertures", "heroBookOpen",
             "# Les Ouvertures Classiques\n\n## L'Italienne\n1.e4 e5 2.Cf3 Cc6 3.Fc4\n\n## L'Espagnole\n1.e4 e5 2.Cf3 Cc6 3.Fb5\n\n## La Française\n1.e4 e6\n\n## La Sicilienne\n1.e4 c5"},
            {"La défense Sicilienne", "La défense la plus populaire", "heroShield",
             "# La Défense Sicilienne\n\n## 1.e4 c5\n\nLa réponse la plus populaire contre 1.e4.\n\n## Variantes principales\n\n- Najdorf : 5...a6\n- Dragon : 5...g6\n- Scheveningen : 5...e6"},
            {"L'ouverture Italienne", "Un classique intemporel", "heroFlag",
             "# L'Ouverture Italienne\n\n## 1.e4 e5 2.Cf3 Cc6 3.Fc4\n\nUne ouverture solide et instructive.\n\n## Idées principales\n\n- Contrôle du centre\n- Développement rapide\n- Attaque sur f7"},
            {"Le Gambit Dame", "L'ouverture des champions", "heroTrophy",
             "# Le Gambit Dame\n\n## 1.d4 d5 2.c4\n\nLes Blancs offrent un pion pour contrôler le centre.\n\n## Accepté vs Refusé\n\n- Gambit Dame Accepté : 2...dxc4\n- Gambit Dame Refusé : 2...e6"},
            {"Les structures de pions", "La colonne vertébrale de la position", "heroTableCells",
             "# Les Structures de Pions\n\n## Importance\n\nLes pions définissent le caractère de la position.\n\n## Types de structures\n\n- Pions isolés\n- Pions doublés\n- Chaîne de pions\n- Pions pendants"},
            {"Les pions passés", "La force du pion libre", "heroArrowUp",
             "# Les Pions Passés\n\n## Définition\n\nUn pion passé n'a plus de pions adverses devant lui.\n\n## Règle d'or\n\nLes pions passés doivent être poussés !"},
            {"Le jeu positionnel", "Améliorez votre position", "heroChartBar",
             "# Le Jeu Positionnel\n\n## Principes\n\n- Améliorer ses pièces\n- Créer des faiblesses chez l'adversaire\n- Contrôler les cases clés"},
            {"Le contrôle du centre", "Dominez le centre", "heroViewfinderCircle",
             "# Le Contrôle du Centre\n\n## Cases centrales\n\ne4, d4, e5, d5\n\n## Pourquoi le centre ?\n\nLes pièces au centre contrôlent plus de cases."},
            {"Les colonnes ouvertes", "Activez vos tours", "heroArrowsUpDown",
             "# Les Colonnes Ouvertes\n\n## Définition\n\nUne colonne sans pions.\n\n## Stratégie\n\nOccupez les colonnes ouvertes avec vos tours !"},
            {"La septième rangée", "La rangée décisive", "heroStar",
             "# La Septième Rangée\n\n## Importance\n\nUne tour en 7ème rangée attaque les pions et confine le roi.\n\n## Deux tours en 7ème\n\nSouvent décisif !"}
        };

        for (int i = 0; i < data.length; i++) {
            Course course = new Course();
            course.setTitle(data[i][0]);
            course.setDescription(data[i][1]);
            course.setIconName(data[i][2]);
            course.setContent(data[i][3]);
            course.setGrade(ChessLevel.FOU);
            course.setOrderInGrade(i + 1);
            course.setEstimatedMinutes(20 + (i * 2));
            courses.add(course);
        }

        return courses;
    }

    private List<Course> createTourCourses() {
        List<Course> courses = new ArrayList<>();
        String[][] data = {
            {"Les finales complexes", "Maîtrisez les finales difficiles", "heroAcademicCap",
             "# Finales Complexes\n\n## Tour et pion vs Tour\n\nLa finale la plus courante aux échecs.\n\n## Principes\n\n- Position de Lucena\n- Position de Philidor"},
            {"La technique de Philidor", "La défense parfaite", "heroShield",
             "# La Technique de Philidor\n\n## Position\n\nLa tour coupe le roi adverse à la 6ème rangée.\n\n## Méthode\n\nAttendre que le pion avance, puis donner des échecs par derrière."},
            {"La technique de Lucena", "Gagner avec tour et pion", "heroTrophy",
             "# La Technique de Lucena\n\n## Le pont\n\nUtiliser la tour pour protéger le roi et permettre au pion de promouvoir."},
            {"Les tours actives vs passives", "L'importance de l'activité", "heroBolt",
             "# Tours Actives vs Passives\n\n## Principe\n\nUne tour active vaut mieux qu'un pion.\n\n## Conseil\n\nPlacez vos tours sur les colonnes ouvertes !"},
            {"L'opposition des rois", "La clé des finales de pions", "heroUsers",
             "# L'Opposition\n\n## Définition\n\nLes deux rois face à face avec une case entre eux.\n\n## Qui a l'opposition ?\n\nCelui qui n'a pas le trait."},
            {"Les finales de pions", "Calcul et technique", "heroCalculator",
             "# Finales de Pions\n\n## Règles essentielles\n\n- La règle du carré\n- L'opposition\n- Le zugzwang"},
            {"La règle du carré", "Calculer la promotion", "heroSquare2Stack",
             "# La Règle du Carré\n\n## Principe\n\nSi le roi peut entrer dans le carré du pion, il l'attrape."},
            {"Les plans à long terme", "Pensez stratégiquement", "heroMap",
             "# Plans à Long Terme\n\n## Importance\n\nAvoir un plan guide vos coups.\n\n## Comment former un plan\n\n1. Évaluez la position\n2. Identifiez les faiblesses\n3. Choisissez une cible"},
            {"La prophylaxie", "Anticipez les plans adverses", "heroEye",
             "# La Prophylaxie\n\n## Définition\n\nEmpêcher les plans de l'adversaire avant qu'il ne les réalise.\n\n## Questions à se poser\n\nQue veut jouer mon adversaire ?"},
            {"L'amélioration des pièces", "Optimisez vos pièces", "heroArrowTrendingUp",
             "# Amélioration des Pièces\n\n## Principe\n\nAméliorez votre pire pièce.\n\n## Méthode\n\nIdentifiez la pièce la moins active et trouvez-lui une meilleure case."}
        };

        for (int i = 0; i < data.length; i++) {
            Course course = new Course();
            course.setTitle(data[i][0]);
            course.setDescription(data[i][1]);
            course.setIconName(data[i][2]);
            course.setContent(data[i][3]);
            course.setGrade(ChessLevel.TOUR);
            course.setOrderInGrade(i + 1);
            course.setEstimatedMinutes(25 + (i * 2));
            courses.add(course);
        }

        return courses;
    }

    private List<Course> createDameCourses() {
        List<Course> courses = new ArrayList<>();
        String[][] data = {
            {"L'analyse approfondie", "Calculez comme un maître", "heroMagnifyingGlass",
             "# L'Analyse Approfondie\n\n## Méthode\n\n1. Identifier les coups candidats\n2. Calculer les variantes\n3. Évaluer les positions finales"},
            {"Les sacrifices positionnels", "Sacrifier pour la position", "heroGift",
             "# Sacrifices Positionnels\n\n## Différence avec le sacrifice tactique\n\nPas de gain matériel immédiat, mais une amélioration durable de la position."},
            {"L'attaque sur le roque", "Détruisez le refuge adverse", "heroFire",
             "# L'Attaque sur le Roque\n\n## Conditions\n\n- Plus de pièces en attaque qu'en défense\n- Faiblesses autour du roi\n- Lignes d'attaque ouvertes"},
            {"La défense dynamique", "Défendez activement", "heroShield",
             "# La Défense Dynamique\n\n## Principe\n\nContre-attaquer plutôt que défendre passivement."},
            {"Les zeitnots", "Gérez votre temps", "heroClock",
             "# Les Zeitnots\n\n## Définition\n\nManque de temps à la pendule.\n\n## Conseils\n\n- Gardez du temps pour les moments critiques\n- Faites des coups solides sous pression"},
            {"La psychologie aux échecs", "L'aspect mental du jeu", "heroBrain",
             "# Psychologie aux Échecs\n\n## Facteurs\n\n- La gestion du stress\n- La confiance en soi\n- La concentration"},
            {"La préparation des tournois", "Préparez-vous comme un pro", "heroClipboardDocument",
             "# Préparation des Tournois\n\n## Éléments\n\n- Préparation d'ouverture\n- Étude des adversaires\n- Condition physique"},
            {"L'analyse de vos parties", "Apprenez de vos erreurs", "heroChartPie",
             "# Analyse de Parties\n\n## Méthode\n\n1. Rejouez sans moteur\n2. Identifiez les moments critiques\n3. Cherchez les améliorations"},
            {"Les styles de jeu", "Trouvez votre style", "heroUser",
             "# Les Styles de Jeu\n\n## Types\n\n- Tactique vs Positionnel\n- Agressif vs Solide\n- Universal"},
            {"Vers la maîtrise", "Continuez votre progression", "heroAcademicCap",
             "# Vers la Maîtrise\n\n## Félicitations !\n\nVous avez terminé le parcours d'apprentissage.\n\n## Et maintenant ?\n\n- Jouez régulièrement\n- Analysez vos parties\n- Étudiez les grands maîtres\n- Ne cessez jamais d'apprendre !"}
        };

        for (int i = 0; i < data.length; i++) {
            Course course = new Course();
            course.setTitle(data[i][0]);
            course.setDescription(data[i][1]);
            course.setIconName(data[i][2]);
            course.setContent(data[i][3]);
            course.setGrade(ChessLevel.DAME);
            course.setOrderInGrade(i + 1);
            course.setEstimatedMinutes(30 + (i * 2));
            courses.add(course);
        }

        return courses;
    }
}
