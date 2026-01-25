package com.chessconnect.service;

import com.chessconnect.dto.exercise.ExerciseResponse;
import com.chessconnect.model.Course;
import com.chessconnect.model.Exercise;
import com.chessconnect.model.Lesson;
import com.chessconnect.model.Progress;
import com.chessconnect.model.UserCourseProgress;
import com.chessconnect.model.enums.ChessLevel;
import com.chessconnect.model.enums.CourseStatus;
import com.chessconnect.model.enums.DifficultyLevel;
import com.chessconnect.repository.CourseRepository;
import com.chessconnect.repository.ExerciseRepository;
import com.chessconnect.repository.LessonRepository;
import com.chessconnect.repository.ProgressRepository;
import com.chessconnect.repository.UserCourseProgressRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ExerciseService {

    private static final Logger log = LoggerFactory.getLogger(ExerciseService.class);

    private final ExerciseRepository exerciseRepository;
    private final LessonRepository lessonRepository;
    private final ProgressRepository progressRepository;
    private final SubscriptionService subscriptionService;
    private final UserCourseProgressRepository userCourseProgressRepository;
    private final CourseRepository courseRepository;

    // Map of course title keywords to starting FEN positions
    private static final Map<String, ExerciseConfig> COURSE_EXERCISE_CONFIGS = new HashMap<>();

    static {
        // Ouvertures - Niveau CAVALIER et FOU
        COURSE_EXERCISE_CONFIGS.put("ouverture italienne",
            new ExerciseConfig(
                "r1bqkbnr/pppp1ppp/2n5/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 3 3",
                "Jouez l'ouverture Italienne. Developpez vos pieces vers le centre et preparez le roque.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("ouverture espagnole",
            new ExerciseConfig(
                "r1bqkbnr/pppp1ppp/2n5/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 3 3",
                "Jouez la Ruy Lopez. Mettez la pression sur le cavalier c6 et preparez le centre.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("defense sicilienne",
            new ExerciseConfig(
                "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq c6 0 2",
                "Affrontez la Sicilienne. Les Blancs ont plusieurs plans : Cf3, c3, ou f4.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("defense francaise",
            new ExerciseConfig(
                "rnbqkbnr/pppp1ppp/4p3/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2",
                "Jouez contre la Defense Francaise. Poussez e5 ou echangez en d5.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("gambit dame",
            new ExerciseConfig(
                "rnbqkbnr/ppp1pppp/8/3p4/2PP4/8/PP2PPPP/RNBQKBNR b KQkq c3 0 2",
                "Jouez le Gambit Dame. Controlez le centre et developpez harmonieusement.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("defense indienne",
            new ExerciseConfig(
                "rnbqkb1r/pppppppp/5n2/8/3P4/8/PPP1PPPP/RNBQKBNR w KQkq - 1 2",
                "Affrontez une defense Indienne. Developpez avec c4 et Cc3.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("caro-kann",
            new ExerciseConfig(
                "rnbqkbnr/pp1ppppp/2p5/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2",
                "Jouez contre la Caro-Kann. Une defense solide a combattre.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("defense slave",
            new ExerciseConfig(
                "rnbqkbnr/pp2pppp/2p5/3p4/2PP4/8/PP2PPPP/RNBQKBNR w KQkq - 0 3",
                "La Defense Slave. Solide et flexible pour les Noirs.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("nimzo-indienne",
            new ExerciseConfig(
                "rnbqk2r/pppp1ppp/4pn2/8/1bPP4/2N5/PP2PPPP/R1BQKBNR w KQkq - 2 4",
                "La Nimzo-Indienne. Les Noirs controlent e4 avec le Fou en b4.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("est-indienne",
            new ExerciseConfig(
                "rnbqk2r/ppp1ppbp/3p1np1/8/2PPP3/2N5/PP3PPP/R1BQKBNR w KQkq - 0 5",
                "L'Est-Indienne. Les Noirs preparent une attaque sur l'aile roi.",
                "white"));

        // Tactiques - Niveau CAVALIER
        COURSE_EXERCISE_CONFIGS.put("fourchette",
            new ExerciseConfig(
                "r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4",
                "Trouvez la fourchette ! Attaquez deux pieces en meme temps.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("clouage",
            new ExerciseConfig(
                "r1bqkbnr/pppp1ppp/2n5/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4",
                "Utilisez le clouage pour immobiliser une piece adverse.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("enfilade",
            new ExerciseConfig(
                "4k3/8/8/8/8/8/4R3/4K3 w - - 0 1",
                "Pratiquez l'enfilade : attaquez une piece qui protege une autre.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("mat en 2",
            new ExerciseConfig(
                "r1bqkb1r/pppp1Qpp/2n2n2/4p3/2B1P3/8/PPPP1PPP/RNB1K1NR b KQkq - 0 4",
                "Trouvez le mat en 2 coups !",
                "white"));

        // Finales - Niveau CAVALIER et FOU
        COURSE_EXERCISE_CONFIGS.put("finale roi + dame",
            new ExerciseConfig(
                "8/8/8/4k3/8/8/3QK3/8 w - - 0 1",
                "Matez avec Roi et Dame contre Roi seul. Poussez le Roi adverse vers le bord.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("finale roi + tour",
            new ExerciseConfig(
                "8/8/8/4k3/8/8/4K3/4R3 w - - 0 1",
                "Matez avec Roi et Tour. Utilisez la technique de l'escalier.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("finale roi + 2 fous",
            new ExerciseConfig(
                "8/8/8/4k3/8/8/2B1K3/3B4 w - - 0 1",
                "Matez avec deux Fous. Coordonnez vos Fous pour pousser le Roi dans le coin.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("lucena",
            new ExerciseConfig(
                "1K1k4/1P6/8/8/8/8/r7/4R3 w - - 0 1",
                "Position de Lucena : construisez le pont pour promouvoir le pion.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("philidor",
            new ExerciseConfig(
                "8/8/8/8/3k4/8/3KP3/r7 b - - 0 1",
                "Position de Philidor : defendez avec votre Tour sur la 6eme rangee.",
                "black"));
        COURSE_EXERCISE_CONFIGS.put("opposition",
            new ExerciseConfig(
                "8/8/8/3k4/8/3K4/3P4/8 w - - 0 1",
                "L'opposition : prenez l'opposition pour promouvoir votre pion.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("regle du carre",
            new ExerciseConfig(
                "8/8/8/8/P7/8/8/4k1K1 w - - 0 1",
                "La regle du carre : votre Roi peut-il rattraper le pion ?",
                "black"));

        // =====================================================
        // NIVEAU PION - 45 cours
        // =====================================================

        // 1. L'echiquier et la notation
        COURSE_EXERCISE_CONFIGS.put("echiquier",
            new ExerciseConfig(
                "8/8/8/8/8/8/8/8 w - - 0 1",
                "Familiarisez-vous avec l'echiquier. Les colonnes vont de a a h, les rangees de 1 a 8.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("notation",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Apprenez la notation algebrique : chaque case a un nom (ex: e4, d5).",
                "white"));

        // 2. Le mouvement du Roi
        COURSE_EXERCISE_CONFIGS.put("mouvement du roi",
            new ExerciseConfig(
                "8/8/8/4K3/8/8/8/8 w - - 0 1",
                "Le Roi se deplace d'une case dans toutes les directions. Pratiquez ses mouvements.",
                "white"));

        // 3. Le mouvement de la Dame
        COURSE_EXERCISE_CONFIGS.put("mouvement de la dame",
            new ExerciseConfig(
                "8/8/8/3Q4/8/8/8/4K3 w - - 0 1",
                "La Dame est la piece la plus puissante. Elle combine les mouvements de la Tour et du Fou.",
                "white"));

        // 4. Le mouvement de la Tour
        COURSE_EXERCISE_CONFIGS.put("mouvement de la tour",
            new ExerciseConfig(
                "8/8/8/3R4/8/8/8/4K3 w - - 0 1",
                "La Tour se deplace en ligne droite : horizontalement ou verticalement.",
                "white"));

        // 5. Le mouvement du Fou
        COURSE_EXERCISE_CONFIGS.put("mouvement du fou",
            new ExerciseConfig(
                "8/8/8/3B4/8/8/8/4K3 w - - 0 1",
                "Le Fou se deplace en diagonale. Chaque Fou reste sur sa couleur de case.",
                "white"));

        // 6. Le mouvement du Cavalier
        COURSE_EXERCISE_CONFIGS.put("mouvement du cavalier",
            new ExerciseConfig(
                "8/8/8/3N4/8/8/8/4K3 w - - 0 1",
                "Le Cavalier saute en L : 2 cases + 1 case. Il peut sauter par-dessus les autres pieces.",
                "white"));

        // 7. Le mouvement du Pion
        COURSE_EXERCISE_CONFIGS.put("mouvement du pion",
            new ExerciseConfig(
                "8/8/8/8/8/8/4P3/4K3 w - - 0 1",
                "Le Pion avance d'une case (ou deux depuis sa position initiale). Il capture en diagonale.",
                "white"));

        // 8. La prise en passant
        COURSE_EXERCISE_CONFIGS.put("prise en passant",
            new ExerciseConfig(
                "8/8/8/3Pp3/8/8/8/4K3 w - e6 0 1",
                "La prise en passant : capturez le pion adverse qui vient d'avancer de 2 cases.",
                "white"));

        // 9. La promotion du Pion
        COURSE_EXERCISE_CONFIGS.put("promotion",
            new ExerciseConfig(
                "8/4P3/8/8/8/8/8/4K3 w - - 0 1",
                "Promotion : quand un pion atteint la derniere rangee, il devient Dame, Tour, Fou ou Cavalier.",
                "white"));

        // 10. Le petit roque
        COURSE_EXERCISE_CONFIGS.put("petit roque",
            new ExerciseConfig(
                "r3k2r/pppppppp/8/8/8/8/PPPPPPPP/R3K2R w KQkq - 0 1",
                "Le petit roque (O-O) : le Roi va en g1 et la Tour en f1. Roquez pour proteger votre Roi !",
                "white"));

        // 11. Le grand roque
        COURSE_EXERCISE_CONFIGS.put("grand roque",
            new ExerciseConfig(
                "r3k2r/pppppppp/8/8/8/8/PPPPPPPP/R3K2R w KQkq - 0 1",
                "Le grand roque (O-O-O) : le Roi va en c1 et la Tour en d1. Plus long mais parfois utile.",
                "white"));

        // 12. Les conditions du roque
        COURSE_EXERCISE_CONFIGS.put("conditions du roque",
            new ExerciseConfig(
                "r3k2r/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/R3K2R w KQkq - 0 1",
                "Pour roquer : Roi et Tour n'ont pas bouge, cases entre eux vides, pas d'echec sur le chemin.",
                "white"));

        // 13. L'echec au Roi
        COURSE_EXERCISE_CONFIGS.put("echec au roi",
            new ExerciseConfig(
                "4k3/8/8/8/8/5B2/8/4K3 w - - 0 1",
                "L'echec : une piece menace le Roi adverse. Le Roi DOIT sortir de l'echec.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("echec",
            new ExerciseConfig(
                "4k3/8/8/8/8/5B2/8/4K3 w - - 0 1",
                "Mettez le Roi adverse en echec avec votre Fou !",
                "white"));

        // 14. Parer un echec
        COURSE_EXERCISE_CONFIGS.put("parer un echec",
            new ExerciseConfig(
                "4k3/8/8/8/8/4r3/8/4K3 w - - 0 1",
                "3 facons de parer un echec : bouger le Roi, bloquer, ou capturer la piece attaquante.",
                "white"));

        // 15. L'echec et mat
        COURSE_EXERCISE_CONFIGS.put("echec et mat",
            new ExerciseConfig(
                "6k1/5ppp/8/8/8/8/8/4K2R w - - 0 1",
                "Echec et mat : le Roi est en echec et ne peut pas s'echapper. Matez en 1 coup !",
                "white"));

        // 16. Le pat et la nulle
        COURSE_EXERCISE_CONFIGS.put("pat",
            new ExerciseConfig(
                "7k/8/6K1/8/8/8/8/6Q1 w - - 0 1",
                "Pat : le joueur n'a aucun coup legal mais n'est pas en echec. C'est une nulle !",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("nulle",
            new ExerciseConfig(
                "7k/8/6K1/8/8/8/8/6Q1 w - - 0 1",
                "Attention au pat ! Approchez la Dame sans bloquer tous les mouvements du Roi adverse.",
                "white"));

        // 17. La valeur des pieces
        COURSE_EXERCISE_CONFIGS.put("valeur des pieces",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Valeurs : Pion=1, Cavalier=3, Fou=3, Tour=5, Dame=9. Le Roi n'a pas de valeur (indispensable).",
                "white"));

        // 18. Les echanges favorables
        COURSE_EXERCISE_CONFIGS.put("echanges favorables",
            new ExerciseConfig(
                "r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4",
                "Un bon echange : capturer une piece de valeur superieure ou egale a celle que vous perdez.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("echanges",
            new ExerciseConfig(
                "r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4",
                "Evaluez les echanges : ne donnez pas une Tour (5) pour un Cavalier (3) !",
                "white"));

        // 19. Le mat du couloir
        COURSE_EXERCISE_CONFIGS.put("mat du couloir",
            new ExerciseConfig(
                "6k1/5ppp/8/8/8/8/8/R3K3 w - - 0 1",
                "Le mat du couloir : le Roi est piege derriere ses propres pions. Matez avec la Tour !",
                "white"));

        // 20. Le mat avec Dame et Roi
        COURSE_EXERCISE_CONFIGS.put("mat avec dame",
            new ExerciseConfig(
                "8/8/8/4k3/8/8/3QK3/8 w - - 0 1",
                "Mat avec Dame et Roi : poussez le Roi adverse vers le bord, puis matez.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("dame et roi",
            new ExerciseConfig(
                "8/8/8/4k3/8/8/3QK3/8 w - - 0 1",
                "Coordonnez votre Roi et Dame pour acculer le Roi adverse.",
                "white"));

        // 21. Le mat avec deux Tours
        COURSE_EXERCISE_CONFIGS.put("mat avec deux tours",
            new ExerciseConfig(
                "8/8/8/4k3/8/8/R3K3/R7 w - - 0 1",
                "Mat avec deux Tours : utilisez la technique de l'escalier pour repousser le Roi.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("deux tours",
            new ExerciseConfig(
                "8/8/8/4k3/8/8/R3K3/R7 w - - 0 1",
                "Les Tours montent en escalier pour mater le Roi sur le bord.",
                "white"));

        // 22. Le mat avec Tour et Roi
        COURSE_EXERCISE_CONFIGS.put("mat avec tour",
            new ExerciseConfig(
                "8/8/8/4k3/8/8/4K3/4R3 w - - 0 1",
                "Mat avec Tour et Roi : prenez l'opposition et utilisez la Tour pour couper le Roi.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("tour et roi",
            new ExerciseConfig(
                "8/8/8/4k3/8/8/4K3/4R3 w - - 0 1",
                "Coupez le Roi adverse avec la Tour, puis approchez votre Roi.",
                "white"));

        // 23. Le mat du berger
        COURSE_EXERCISE_CONFIGS.put("mat du berger",
            new ExerciseConfig(
                "r1bqkbnr/pppp1ppp/2n5/4p3/2B1P3/5Q2/PPPP1PPP/RNB1K1NR w KQkq - 3 3",
                "Le mat du berger menace f7. Apprenez a le reconnaitre et a vous en defendre !",
                "white"));

        // 24. Le mat de l'ecolier
        COURSE_EXERCISE_CONFIGS.put("mat de l'ecolier",
            new ExerciseConfig(
                "r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4",
                "Le mat de l'ecolier : Dxf7# si les Noirs ne font pas attention. Defendez f7 !",
                "black"));

        // 25. Defendre f7/f2
        COURSE_EXERCISE_CONFIGS.put("defendre f7",
            new ExerciseConfig(
                "r1bqkbnr/pppp1ppp/2n5/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 3 3",
                "f7 (et f2 pour les Blancs) est le point faible en debut de partie. Protegez-le !",
                "black"));
        COURSE_EXERCISE_CONFIGS.put("f7",
            new ExerciseConfig(
                "r1bqkbnr/pppp1ppp/2n5/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 3 3",
                "Le pion f7 n'est protege que par le Roi. Attention aux attaques sur cette case !",
                "black"));

        // 26. Les premiers coups
        COURSE_EXERCISE_CONFIGS.put("premiers coups",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Les meilleurs premiers coups : e4 ou d4 controlent le centre. Commencez la partie !",
                "white"));

        // 27. Controler le centre
        COURSE_EXERCISE_CONFIGS.put("controler le centre",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1",
                "Le centre (e4, d4, e5, d5) est strategiquement important. Controlez-le avec vos pions !",
                "black"));
        COURSE_EXERCISE_CONFIGS.put("centre",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/3PP3/8/PPP2PPP/RNBQKBNR b KQkq - 0 2",
                "Occupez ou controlez les cases centrales pour dominer la partie.",
                "black"));

        // 28. Developper ses pieces
        COURSE_EXERCISE_CONFIGS.put("developper ses pieces",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 1",
                "Developpez vos pieces vers des cases actives : Cavaliers vers c3/f3, Fous vers c4/f4.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("developpement",
            new ExerciseConfig(
                "r1bqkbnr/pppppppp/2n5/8/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 2",
                "Sortez vos pieces mineures (Cavaliers et Fous) avant les pieces lourdes.",
                "white"));

        // 29. La securite du Roi
        COURSE_EXERCISE_CONFIGS.put("securite du roi",
            new ExerciseConfig(
                "r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4",
                "Roquez tot pour mettre votre Roi en securite. Un Roi au centre est vulnerable.",
                "white"));

        // 30. Ne pas sortir la Dame trop tot
        COURSE_EXERCISE_CONFIGS.put("dame trop tot",
            new ExerciseConfig(
                "rnb1kbnr/pppp1ppp/8/4p3/2B1P2q/8/PPPP1PPP/RNBQK1NR w KQkq - 2 3",
                "Sortir la Dame trop tot l'expose aux attaques. Developpez d'abord les pieces mineures.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("sortir la dame",
            new ExerciseConfig(
                "rnb1kbnr/pppp1ppp/8/4p3/2B1P2q/8/PPPP1PPP/RNBQK1NR w KQkq - 2 3",
                "Gagnez du temps en attaquant la Dame adverse avec vos pieces mineures.",
                "white"));

        // 31. Connecter les Tours
        COURSE_EXERCISE_CONFIGS.put("connecter les tours",
            new ExerciseConfig(
                "r4rk1/ppp2ppp/2n2n2/3p4/3P4/2N2N2/PPP2PPP/R4RK1 w - - 0 10",
                "Connectez vos Tours : elles doivent se proteger mutuellement sur la premiere rangee.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("tours",
            new ExerciseConfig(
                "r4rk1/ppp2ppp/2n2n2/3p4/3P4/2N2N2/PPP2PPP/R4RK1 w - - 0 10",
                "Des Tours connectees sont puissantes sur les colonnes ouvertes.",
                "white"));

        // 32. Les pieces en prise
        COURSE_EXERCISE_CONFIGS.put("pieces en prise",
            new ExerciseConfig(
                "r1bqkbnr/pppp1ppp/2n5/4p3/3PP3/5N2/PPP2PPP/RNBQKB1R b KQkq - 0 3",
                "Une piece en prise peut etre capturee gratuitement. Verifiez toujours vos pieces !",
                "black"));

        // 33. Verifier les menaces adverses
        COURSE_EXERCISE_CONFIGS.put("menaces adverses",
            new ExerciseConfig(
                "r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR b KQkq - 3 3",
                "Avant de jouer, demandez-vous : quelle est la menace adverse ?",
                "black"));
        COURSE_EXERCISE_CONFIGS.put("menaces",
            new ExerciseConfig(
                "r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR b KQkq - 3 3",
                "La Dame et le Fou blancs menacent f7. Defendez-vous !",
                "black"));

        // 34. Les erreurs de debutant
        COURSE_EXERCISE_CONFIGS.put("erreurs de debutant",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Evitez les erreurs classiques : pieces en prise, Roi au centre, Dame sortie trop tot.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("erreurs",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Apprenez des erreurs courantes pour les eviter dans vos parties.",
                "white"));

        // 35. Jouer avec un plan
        COURSE_EXERCISE_CONFIGS.put("jouer avec un plan",
            new ExerciseConfig(
                "r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4",
                "Ayez toujours un plan : controler le centre, developper, roquer, attaquer.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("plan",
            new ExerciseConfig(
                "r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4",
                "Un coup sans but est un coup perdu. Jouez avec un objectif !",
                "white"));

        // 36-42. Cours conceptuels (meme position de depart avec conseils differents)
        COURSE_EXERCISE_CONFIGS.put("touche-joue",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Regle touche-joue : si vous touchez une piece, vous devez la jouer. Reflechissez avant !",
                "white"));

        COURSE_EXERCISE_CONFIGS.put("temps aux echecs",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Gerez votre temps : ne jouez pas trop vite ni trop lentement. Trouvez le bon rythme.",
                "white"));

        COURSE_EXERCISE_CONFIGS.put("notation d'une partie",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1",
                "Notation : 1.e4 signifie que les Blancs ont joue le pion en e4 au premier coup.",
                "white"));

        COURSE_EXERCISE_CONFIGS.put("analyser ses parties",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Analysez vos parties pour comprendre vos erreurs et progresser.",
                "white"));

        COURSE_EXERCISE_CONFIGS.put("sites de jeu",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Pratiquez en ligne sur chess.com ou lichess.org pour progresser.",
                "white"));

        COURSE_EXERCISE_CONFIGS.put("etiquette",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "L'etiquette : serrez la main, ne commentez pas pendant la partie, felicitez l'adversaire.",
                "white"));

        COURSE_EXERCISE_CONFIGS.put("gagner perdre",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Gagnez avec humilite, perdez avec grace. Chaque partie est une occasion d'apprendre.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("apprendre",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "La defaite est le meilleur professeur. Analysez vos erreurs pour progresser.",
                "white"));

        COURSE_EXERCISE_CONFIGS.put("patience",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Les echecs demandent de la patience. Ne precipitez pas vos coups.",
                "white"));

        // 44. Revision des bases
        COURSE_EXERCISE_CONFIGS.put("revision des bases",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Revision : controlez le centre, developpez vos pieces, roquez, jouez avec un plan !",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("revision",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Appliquez tous les principes appris : les bases sont la cle de la victoire.",
                "white"));

        // 45. Votre premiere partie guidee
        COURSE_EXERCISE_CONFIGS.put("premiere partie",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Votre premiere vraie partie ! Appliquez tout ce que vous avez appris.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("partie guidee",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Jouez une partie complete en appliquant les principes : centre, developpement, roque.",
                "white"));
    }

    public ExerciseService(
        ExerciseRepository exerciseRepository,
        LessonRepository lessonRepository,
        ProgressRepository progressRepository,
        SubscriptionService subscriptionService,
        UserCourseProgressRepository userCourseProgressRepository,
        CourseRepository courseRepository
    ) {
        this.exerciseRepository = exerciseRepository;
        this.lessonRepository = lessonRepository;
        this.progressRepository = progressRepository;
        this.subscriptionService = subscriptionService;
        this.userCourseProgressRepository = userCourseProgressRepository;
        this.courseRepository = courseRepository;
    }

    // Configuration class for exercises
    private static class ExerciseConfig {
        final String fen;
        final String tip;
        final String playerColor;

        ExerciseConfig(String fen, String tip, String playerColor) {
            this.fen = fen;
            this.tip = tip;
            this.playerColor = playerColor;
        }
    }

    public ExerciseResponse getExerciseForLesson(Long lessonId, Long userId) {
        // Check premium access
        if (!subscriptionService.isPremium(userId)) {
            throw new IllegalArgumentException("Un abonnement Premium est requis pour acceder aux exercices");
        }

        // Verify the lesson belongs to this user
        Lesson lesson = lessonRepository.findById(lessonId)
            .orElseThrow(() -> new IllegalArgumentException("Cours non trouve"));

        if (!lesson.getStudent().getId().equals(userId)) {
            throw new IllegalArgumentException("Vous n'avez pas acces a ce cours");
        }

        // Find or generate exercise for this lesson
        Exercise exercise = exerciseRepository.findByLessonId(lessonId)
            .orElseGet(() -> generateExerciseForLesson(lesson, userId));

        log.info("Exercise loaded for lesson {}, user {}: difficulty={}",
            lessonId, userId, exercise.getDifficultyLevel());

        return ExerciseResponse.from(exercise);
    }

    @Transactional
    private Exercise generateExerciseForLesson(Lesson lesson, Long userId) {
        // Determine difficulty based on student level
        ChessLevel studentLevel = progressRepository.findByStudentId(userId)
            .map(Progress::getCurrentLevel)
            .orElse(ChessLevel.PION);

        DifficultyLevel difficulty = mapChessLevelToDifficulty(studentLevel);

        // Get current course (IN_PROGRESS or last COMPLETED) for the student
        Optional<Course> currentCourse = getCurrentCourseForStudent(userId, studentLevel);

        // Find matching exercise config based on course title
        ExerciseConfig config = null;
        String courseTitle = null;
        if (currentCourse.isPresent()) {
            courseTitle = currentCourse.get().getTitle();
            config = findExerciseConfigForCourse(courseTitle);
        }

        Exercise exercise = new Exercise();
        exercise.setLesson(lesson);

        if (config != null && courseTitle != null) {
            // Exercise linked to the current course
            exercise.setTitle(courseTitle);
            exercise.setDescription(config.tip);
            exercise.setStartingFen(config.fen);
            exercise.setPlayerColor(config.playerColor);
            log.info("Generated exercise linked to course '{}' for user {}", courseTitle, userId);
        } else {
            // Default exercise - standard position
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String lessonDate = lesson.getScheduledAt().format(formatter);
            exercise.setTitle("Cours du " + lessonDate + " avec " + lesson.getTeacher().getFirstName());

            // Use teacher observations as tips if available
            String description;
            if (lesson.getTeacherObservations() != null && !lesson.getTeacherObservations().isBlank()) {
                description = lesson.getTeacherObservations();
            } else {
                description = "Exercez-vous contre myChessBot pour mettre en pratique ce que vous avez appris. Niveau: " + studentLevel.getDisplayName();
            }
            exercise.setDescription(description);
            exercise.setStartingFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
            exercise.setPlayerColor("white");
        }

        exercise.setLesson(lesson);
        exercise.setDifficultyLevel(difficulty);
        exercise.setChessLevel(studentLevel);

        Exercise saved = exerciseRepository.save(exercise);
        log.info("Generated new exercise {} for lesson {}, difficulty={}, course={}",
            saved.getId(), lesson.getId(), difficulty, courseTitle);

        return saved;
    }

    /**
     * Get the current course (IN_PROGRESS) or the last completed course for the student
     */
    private Optional<Course> getCurrentCourseForStudent(Long userId, ChessLevel studentLevel) {
        List<UserCourseProgress> allProgress = userCourseProgressRepository.findByUserId(userId);

        // First, try to find a course IN_PROGRESS or PENDING_VALIDATION
        Optional<UserCourseProgress> inProgress = allProgress.stream()
            .filter(p -> p.getStatus() == CourseStatus.IN_PROGRESS || p.getStatus() == CourseStatus.PENDING_VALIDATION)
            .max(Comparator.comparing(p -> p.getStartedAt() != null ? p.getStartedAt() : java.time.LocalDateTime.MIN));

        if (inProgress.isPresent()) {
            return Optional.of(inProgress.get().getCourse());
        }

        // If no IN_PROGRESS, get the last COMPLETED course
        Optional<UserCourseProgress> lastCompleted = allProgress.stream()
            .filter(p -> p.getStatus() == CourseStatus.COMPLETED)
            .max(Comparator.comparing(p -> p.getCompletedAt() != null ? p.getCompletedAt() : java.time.LocalDateTime.MIN));

        if (lastCompleted.isPresent()) {
            return Optional.of(lastCompleted.get().getCourse());
        }

        // If no progress, get the first course of the student's level
        return courseRepository.findByGradeAndOrderInGrade(studentLevel, 1);
    }

    /**
     * Find exercise config that matches the course title
     */
    private ExerciseConfig findExerciseConfigForCourse(String courseTitle) {
        if (courseTitle == null) return null;

        String titleLower = courseTitle.toLowerCase();

        for (Map.Entry<String, ExerciseConfig> entry : COURSE_EXERCISE_CONFIGS.entrySet()) {
            if (titleLower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    private DifficultyLevel mapChessLevelToDifficulty(ChessLevel chessLevel) {
        return switch (chessLevel) {
            case PION -> DifficultyLevel.DEBUTANT;
            case CAVALIER -> DifficultyLevel.FACILE;
            case FOU -> DifficultyLevel.MOYEN;
            case TOUR -> DifficultyLevel.DIFFICILE;
            case DAME -> DifficultyLevel.EXPERT;
        };
    }

    public List<ExerciseResponse> getAllExercisesForUser(Long userId) {
        // Check premium access
        if (!subscriptionService.isPremium(userId)) {
            throw new IllegalArgumentException("Un abonnement Premium est requis pour acceder aux exercices");
        }

        // Get student's level for default exercises
        ChessLevel studentLevel = progressRepository.findByStudentId(userId)
            .map(Progress::getCurrentLevel)
            .orElse(ChessLevel.PION);

        return exerciseRepository.findByChessLevelOrderByCreatedAtDesc(studentLevel)
            .stream()
            .map(ExerciseResponse::from)
            .toList();
    }

    public ExerciseResponse getExerciseById(Long exerciseId, Long userId) {
        // Check premium access
        if (!subscriptionService.isPremium(userId)) {
            throw new IllegalArgumentException("Un abonnement Premium est requis pour acceder aux exercices");
        }

        Exercise exercise = exerciseRepository.findById(exerciseId)
            .orElseThrow(() -> new IllegalArgumentException("Exercice non trouve"));

        return ExerciseResponse.from(exercise);
    }
}
