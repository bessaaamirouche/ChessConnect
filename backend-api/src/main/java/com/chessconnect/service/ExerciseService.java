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

        // =====================================================
        // NIVEAU CAVALIER - 45 cours (Intermediaire)
        // =====================================================

        // Tactiques de base
        COURSE_EXERCISE_CONFIGS.put("fourchette du cavalier",
            new ExerciseConfig(
                "r1bqkb1r/pppp1ppp/2n5/4N3/4n3/8/PPPP1PPP/RNBQKB1R w KQkq - 0 5",
                "Le Cavalier est le roi des fourchettes ! Trouvez la fourchette royale.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("fourchette de pion",
            new ExerciseConfig(
                "r1bqkbnr/ppp2ppp/2n5/3p4/3pP3/5N2/PPP2PPP/RNBQKB1R w KQkq - 0 5",
                "Meme le humble Pion peut fourchetter deux pieces !",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("clouage absolu",
            new ExerciseConfig(
                "r1bqk2r/pppp1ppp/2n2n2/4p3/1b2P3/2N2N2/PPPP1PPP/R1BQKB1R w KQkq - 4 4",
                "Le clouage absolu : la piece clouee ne peut pas bouger car le Roi est derriere.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("clouage relatif",
            new ExerciseConfig(
                "r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4",
                "Le clouage relatif : la piece peut bouger mais perdrait du materiel.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("exploiter un clouage",
            new ExerciseConfig(
                "r1bqkb1r/ppppnppp/5n2/4p3/2B1P3/3P1N2/PPP2PPP/RNBQK2R w KQkq - 0 5",
                "Une piece clouee est une cible ! Ajoutez de la pression dessus.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("brochette",
            new ExerciseConfig(
                "4k3/8/8/8/8/8/4R3/4K3 w - - 0 1",
                "La brochette : attaquez une piece de valeur, la piece derriere sera capturee.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("attaque double",
            new ExerciseConfig(
                "r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4",
                "Creez deux menaces simultanees : l'adversaire ne peut parer les deux !",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("attaque a la decouverte",
            new ExerciseConfig(
                "r1bqkb1r/pppp1ppp/2n2n2/4N3/2B1P3/8/PPPP1PPP/RNBQK2R w KQkq - 0 5",
                "Bougez une piece pour reveler une attaque cachee derriere.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("echec a la decouverte",
            new ExerciseConfig(
                "r1bqk2r/pppp1ppp/2n2n2/4N3/1bB1P3/8/PPPP1PPP/RNBQK2R w KQkq - 0 5",
                "L'echec a la decouverte : la piece qui bouge peut attaquer n'importe quoi !",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("echec double",
            new ExerciseConfig(
                "r1bqk2r/pppp1Npp/2n2n2/4p3/1bB1P3/8/PPPP1PPP/RNBQK2R b KQkq - 0 5",
                "L'echec double est devastateur : deux pieces donnent echec simultanement !",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("sacrifice d'attraction",
            new ExerciseConfig(
                "r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/3P1N2/PPP2PPP/RNBQK2R w KQkq - 0 5",
                "Sacrifiez pour attirer une piece sur une case defavorable.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("sacrifice de deviation",
            new ExerciseConfig(
                "r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQ1RK1 w kq - 5 5",
                "Le sacrifice de deviation eloigne un defenseur cle.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("sacrifice de destruction",
            new ExerciseConfig(
                "r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/3P1N2/PPP2PPP/RNBQ1RK1 w kq - 0 6",
                "Detruisez le defenseur pour exposer une faiblesse.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("motifs tactiques",
            new ExerciseConfig(
                "r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4",
                "Combinez plusieurs motifs tactiques pour une attaque decisive.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("attaque sur f7",
            new ExerciseConfig(
                "r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4",
                "f7 est le talon d'Achille des Noirs. Attaquez-le !",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("mat en 3",
            new ExerciseConfig(
                "r1bqk2r/pppp1Qpp/2n2n2/2b1p3/2B1P3/8/PPPP1PPP/RNB1K1NR b KQkq - 0 5",
                "Trouvez le mat en 3 coups. Calculez precisement !",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("tactique niveau 1",
            new ExerciseConfig(
                "r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4",
                "Exercice tactique : trouvez le meilleur coup !",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("defense passive",
            new ExerciseConfig(
                "r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR b KQkq - 4 4",
                "Parfois, il suffit de proteger simplement la piece attaquee.",
                "black"));
        COURSE_EXERCISE_CONFIGS.put("defense active",
            new ExerciseConfig(
                "r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR b KQkq - 4 4",
                "Defendez en contre-attaquant ! La meilleure defense est parfois l'attaque.",
                "black"));
        COURSE_EXERCISE_CONFIGS.put("parer les menaces",
            new ExerciseConfig(
                "r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR b KQkq - 4 4",
                "Identifiez la menace adverse et trouvez la parade.",
                "black"));

        // Finales CAVALIER
        COURSE_EXERCISE_CONFIGS.put("roi + dame vs roi",
            new ExerciseConfig(
                "8/8/8/4k3/8/8/3QK3/8 w - - 0 1",
                "Le mat Dame + Roi est le plus facile. Repoussez le Roi adverse vers le bord.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("roi + tour vs roi",
            new ExerciseConfig(
                "8/8/8/4k3/8/8/4K3/4R3 w - - 0 1",
                "La technique de l'escalier : coupez le Roi adverse et approchez le votre.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("roi + 2 fous vs roi",
            new ExerciseConfig(
                "8/8/8/4k3/8/8/2B1K3/3B4 w - - 0 1",
                "Coordonnez vos deux Fous pour pousser le Roi dans un coin.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("fou + cavalier vs roi",
            new ExerciseConfig(
                "8/8/8/4k3/8/8/2B1K3/3N4 w - - 0 1",
                "Le mat le plus difficile ! Le Roi doit aller dans le coin de la couleur du Fou.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("mat du coin",
            new ExerciseConfig(
                "7k/8/6K1/8/8/8/8/6R1 w - - 0 1",
                "Poussez le Roi dans le coin pour donner mat.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("opposition simple",
            new ExerciseConfig(
                "8/8/8/3k4/8/3K4/3P4/8 w - - 0 1",
                "L'opposition directe : Rois face a face avec une case entre eux.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("regle du carre",
            new ExerciseConfig(
                "8/8/8/8/P7/8/8/4k1K1 w - - 0 1",
                "Le Roi adverse peut-il rattraper le pion ? Comptez les cases !",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("pion passe en finale",
            new ExerciseConfig(
                "8/8/8/8/P3k3/8/8/4K3 w - - 0 1",
                "Un pion passe est un atout majeur en finale. Faites-le avancer !",
                "white"));

        // Ouvertures CAVALIER
        COURSE_EXERCISE_CONFIGS.put("principes de l'ouverture",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Les 4 regles d'or : centre, developpement, roque, connecter les Tours.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("italienne - introduction",
            new ExerciseConfig(
                "r1bqkbnr/pppp1ppp/2n5/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 3 3",
                "L'Italienne : 1.e4 e5 2.Cf3 Cc6 3.Fc4. Visez f7 !",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("espagnole - introduction",
            new ExerciseConfig(
                "r1bqkbnr/pppp1ppp/2n5/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 3 3",
                "L'Espagnole : 1.e4 e5 2.Cf3 Cc6 3.Fb5. Pression sur e5.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("francaise - introduction",
            new ExerciseConfig(
                "rnbqkbnr/pppp1ppp/4p3/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2",
                "La Francaise : 1.e4 e6. Les Noirs construisent une forteresse.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("sicilienne - introduction",
            new ExerciseConfig(
                "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2",
                "La Sicilienne : 1.e4 c5. Defense asymetrique et combative.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("gambit dame - introduction",
            new ExerciseConfig(
                "rnbqkbnr/ppp1pppp/8/3p4/2PP4/8/PP2PPPP/RNBQKBNR b KQkq - 0 2",
                "Le Gambit Dame : 1.d4 d5 2.c4. Controlez le centre !",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("indienne - introduction",
            new ExerciseConfig(
                "rnbqkb1r/pppppppp/5n2/8/3P4/8/PPP1PPPP/RNBQKBNR w KQkq - 1 2",
                "Les defenses Indiennes : developpement flexible avec Cf6.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("pieges d'ouverture",
            new ExerciseConfig(
                "r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4",
                "Attention aux pieges ! Le mat du berger guette les imprudents.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("activite des pieces",
            new ExerciseConfig(
                "r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4",
                "Des pieces actives valent plus que des pieces passives.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("coordination des pieces",
            new ExerciseConfig(
                "r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/5N2/PPPP1PPP/RNBQ1RK1 w kq - 5 5",
                "Vos pieces doivent travailler ensemble, pas isolement.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("tours sur colonnes ouvertes",
            new ExerciseConfig(
                "r4rk1/ppp2ppp/2n2n2/3p4/3P4/2N2N2/PPP2PPP/R4RK1 w - - 0 10",
                "Placez vos Tours sur les colonnes ouvertes (sans pions).",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("fou bon vs fou mauvais",
            new ExerciseConfig(
                "r1bqk2r/ppp2ppp/2np1n2/2b1p3/2B1P3/2NP1N2/PPP2PPP/R1BQK2R w KQkq - 0 6",
                "Un bon Fou a ses pions sur l'autre couleur. Evaluez vos Fous !",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("cavalier vs fou",
            new ExerciseConfig(
                "r1bqk2r/ppp2ppp/2np1n2/2b1p3/2B1P3/2NP1N2/PPP2PPP/R1BQK2R w KQkq - 0 6",
                "Cavalier en position fermee, Fou en position ouverte.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("tactique niveau 2",
            new ExerciseConfig(
                "r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/3P1N2/PPP2PPP/RNBQK2R w KQkq - 0 5",
                "Exercices tactiques plus complexes. Calculez bien !",
                "white"));

        // =====================================================
        // NIVEAU FOU - 45 cours (Confirme)
        // =====================================================

        // Ouvertures approfondies
        COURSE_EXERCISE_CONFIGS.put("italienne - approfondie",
            new ExerciseConfig(
                "r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/2P2N2/PP1P1PPP/RNBQK2R b KQkq - 0 5",
                "L'Italienne avec c3 : preparez d4 pour un centre ideal.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("espagnole - approfondie",
            new ExerciseConfig(
                "r1bqkb1r/1ppp1ppp/p1n2n2/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 0 4",
                "La variante a6 de l'Espagnole. Le Fou recule en a4 ou c4.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("sicilienne najdorf",
            new ExerciseConfig(
                "rnbqkb1r/1p2pppp/p2p1n2/8/3NP3/2N5/PPP2PPP/R1BQKB1R w KQkq - 0 6",
                "La Najdorf : a6 prepare b5 et offre de la flexibilite.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("sicilienne dragon",
            new ExerciseConfig(
                "rnbqkb1r/pp2pp1p/3p1np1/8/3NP3/2N5/PPP2PPP/R1BQKB1R w KQkq - 0 6",
                "Le Dragon : g6 et Fg7 visent le centre et l'aile dame.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("francaise avancee",
            new ExerciseConfig(
                "rnbqkb1r/ppp2ppp/4pn2/3pP3/3P4/2N5/PPP2PPP/R1BQKBNR b KQkq - 0 4",
                "3.e5 : l'avancee francaise. Les Noirs attaquent la chaine.",
                "black"));
        COURSE_EXERCISE_CONFIGS.put("caro-kann",
            new ExerciseConfig(
                "rnbqkbnr/pp2pppp/2p5/3p4/3PP3/8/PPP2PPP/RNBQKBNR w KQkq - 0 3",
                "La Caro-Kann est solide. Les Noirs veulent reprendre en d5.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("scandinave",
            new ExerciseConfig(
                "rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2",
                "1...d5 : la Scandinave. Directe mais risquee.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("gambit dame accepte",
            new ExerciseConfig(
                "rnbqkbnr/ppp1pppp/8/8/2pP4/8/PP2PPPP/RNBQKBNR w KQkq - 0 3",
                "2...dxc4 : les Blancs ont le centre, les Noirs le pion c4.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("gambit dame refuse",
            new ExerciseConfig(
                "rnbqkbnr/ppp2ppp/4p3/3p4/2PP4/8/PP2PPPP/RNBQKBNR w KQkq - 0 3",
                "2...e6 : solide et classique. Le centre est bloque.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("defense slave",
            new ExerciseConfig(
                "rnbqkbnr/pp2pppp/2p5/3p4/2PP4/8/PP2PPPP/RNBQKBNR w KQkq - 0 3",
                "La Slave : c6 soutient d5 et libere le Fc8.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("nimzo-indienne",
            new ExerciseConfig(
                "rnbqk2r/pppp1ppp/4pn2/8/1bPP4/2N5/PP2PPPP/R1BQKBNR w KQkq - 2 4",
                "La Nimzo : Fb4 cloue Cc3 et controle e4.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("est-indienne",
            new ExerciseConfig(
                "rnbqk2r/ppp1ppbp/3p1np1/8/2PPP3/2N5/PP3PPP/R1BQKBNR w KQkq - 0 5",
                "L'Est-Indienne : les Noirs preparent f5 ou e5.",
                "black"));
        COURSE_EXERCISE_CONFIGS.put("ouverture anglaise",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/2P5/8/PP1PPPPP/RNBQKBNR b KQkq - 0 1",
                "1.c4 : l'Anglaise. Controle indirect du centre.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("ouverture reti",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/5N2/PPPPPPPP/RNBQKB1R b KQkq - 1 1",
                "1.Cf3 : l'ouverture Reti. Flexible et hypermoderne.",
                "white"));

        // Structures de pions
        COURSE_EXERCISE_CONFIGS.put("pions isoles",
            new ExerciseConfig(
                "r1bqkb1r/pp3ppp/2n1pn2/3p4/3P4/2N2N2/PP2PPPP/R1BQKB1R w KQkq - 0 6",
                "Le pion isole : faiblesse a attaquer ou case forte devant.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("pions pendants",
            new ExerciseConfig(
                "r1bqkb1r/pp3ppp/2n1pn2/8/2pP4/2N2N2/PP2PPPP/R1BQKB1R w KQkq - 0 6",
                "Les pions pendants : dynamiques mais fragiles.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("pions doubles",
            new ExerciseConfig(
                "r1bqkb1r/pp2pppp/2n2n2/3p4/3P4/2P2N2/PP2PPPP/RNBQKB1R w KQkq - 0 5",
                "Les pions doubles sont souvent une faiblesse structurelle.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("chaine de pions",
            new ExerciseConfig(
                "rnbqkb1r/ppp2ppp/4pn2/3pP3/3P4/8/PPP2PPP/RNBQKBNR w KQkq - 0 4",
                "Attaquez la base de la chaine de pions adverse.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("pions passes",
            new ExerciseConfig(
                "8/pp3ppp/8/3P4/8/8/PP3PPP/8 w - - 0 1",
                "Un pion passe doit etre pousse ou bloque.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("pion passe protege",
            new ExerciseConfig(
                "8/pp3ppp/8/2PP4/8/8/PP3PPP/8 w - - 0 1",
                "Le pion passe protege est un atout strategique majeur.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("cases faibles",
            new ExerciseConfig(
                "r1bqk2r/pp2ppbp/2np1np1/8/3NP3/2N1B3/PPP1BPPP/R2QK2R w KQkq - 0 8",
                "Occupez les cases faibles avec vos pieces.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("avant-postes",
            new ExerciseConfig(
                "r1bqk2r/ppp2ppp/2n1pn2/3p4/3P4/2N1PN2/PP3PPP/R1BQKB1R w KQkq - 0 6",
                "Un avant-poste est une case ideale pour un Cavalier.",
                "white"));

        // Strategie de milieu de partie
        COURSE_EXERCISE_CONFIGS.put("colonne ouverte",
            new ExerciseConfig(
                "r4rk1/ppp2ppp/2n2n2/3p4/3P4/2N2N2/PPP2PPP/R4RK1 w - - 0 10",
                "Controlez les colonnes ouvertes avec vos Tours.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("colonne semi-ouverte",
            new ExerciseConfig(
                "r4rk1/ppp2ppp/2n2n2/3p4/3PP3/2N2N2/PPP2PPP/R4RK1 w - - 0 10",
                "Sur une colonne semi-ouverte, pressez le pion adverse.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("7eme rangee",
            new ExerciseConfig(
                "6k1/ppp2ppp/8/8/8/8/PPP2PPP/1R4K1 w - - 0 1",
                "Une Tour en 7eme rangee est tres puissante.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("deux tours en 7eme",
            new ExerciseConfig(
                "6k1/ppp2ppp/8/8/8/8/PPP2PPP/RR4K1 w - - 0 1",
                "Deux Tours en 7eme = mat ou gain de materiel.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("attaque sur le roque",
            new ExerciseConfig(
                "r1bq1rk1/ppp2ppp/2n2n2/3p4/3P4/2N2N2/PPPQ1PPP/R1B2RK1 w - - 0 10",
                "Pour attaquer le roque, ouvrez des lignes vers le Roi.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("sacrifice en h7",
            new ExerciseConfig(
                "r1bq1rk1/pppn1ppp/4pn2/3p4/3P1B2/2NBPN2/PPP2PPP/R2QK2R w KQ - 0 8",
                "Le sacrifice grec Fxh7+ est un classique !",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("attaque avec pions",
            new ExerciseConfig(
                "r1bq1rk1/ppp2ppp/2n2n2/3p4/3P4/2N2N2/PPPQ1PPP/R1B2RK1 w - - 0 10",
                "Avancez vos pions pour ouvrir des lignes d'attaque.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("defense du roque",
            new ExerciseConfig(
                "r1bq1rk1/ppp2ppp/2n2n2/3p4/3P4/2N2N2/PPPQ1PPP/R1B2RK1 b - - 0 10",
                "Defendez votre roque : h6, Rh8, Ff8 sont des coups utiles.",
                "black"));
        COURSE_EXERCISE_CONFIGS.put("jeu sur les deux ailes",
            new ExerciseConfig(
                "r1bq1rk1/ppp2ppp/2n2n2/3p4/3P4/2N2N2/PPP2PPP/R1BQ1RK1 w - - 0 10",
                "Alternez les menaces sur les deux ailes pour desequilibrer.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("centre ferme",
            new ExerciseConfig(
                "r1bqk2r/ppp2ppp/2np1n2/2b1p3/2B1P3/2NP1N2/PPP2PPP/R1BQK2R w KQkq - 0 6",
                "Centre ferme : jouez sur les ailes !",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("centre ouvert",
            new ExerciseConfig(
                "r1bqkb1r/ppp2ppp/2n2n2/4p3/3pP3/2N2N2/PPP2PPP/R1BQKB1R w KQkq - 0 5",
                "Centre ouvert : l'activite des pieces prime !",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("echanger les dames",
            new ExerciseConfig(
                "r1b1k2r/ppp2ppp/2n2n2/3qp3/3P4/2N2N2/PPP2PPP/R1BQK2R w KQkq - 0 8",
                "Echangez les Dames si vous avez l'avantage materiel.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("garder les dames",
            new ExerciseConfig(
                "r1bqk2r/ppp2ppp/2n2n2/3p4/3P4/2N2N2/PPPQ1PPP/R1B1KB1R w KQkq - 0 8",
                "Gardez la Dame pour attaquer le Roi adverse.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("majorite de pions",
            new ExerciseConfig(
                "8/ppp2ppp/8/8/3P4/8/PPP2PPP/8 w - - 0 1",
                "Utilisez votre majorite de pions pour creer un pion passe.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("minorite de pions",
            new ExerciseConfig(
                "r1bqk2r/1pp2ppp/p1np1n2/4p3/2PP4/2N2N2/PP2PPPP/R1BQKB1R w KQkq - 0 6",
                "L'attaque de minorite b4-b5 cree des faiblesses adverses.",
                "white"));

        // Finales FOU
        COURSE_EXERCISE_CONFIGS.put("finales de tours - bases",
            new ExerciseConfig(
                "8/8/8/4k3/4p3/4K3/8/4R3 w - - 0 1",
                "Les finales de Tours sont les plus courantes. Activite !",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("position de lucena",
            new ExerciseConfig(
                "1K1k4/1P6/8/8/8/8/r7/4R3 w - - 0 1",
                "Construisez le pont de Lucena pour promouvoir.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("position de philidor",
            new ExerciseConfig(
                "8/5k2/8/8/8/R7/4PK2/r7 w - - 0 1",
                "La defense Philidor : Tour en 6eme rangee puis echecs.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("fous de meme couleur",
            new ExerciseConfig(
                "8/8/8/4k3/8/4K3/2B5/5b2 w - - 0 1",
                "Finales de Fous de meme couleur : le Roi actif gagne.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("fous de couleurs opposees",
            new ExerciseConfig(
                "8/8/8/4k3/8/4K3/2B5/2b5 w - - 0 1",
                "Fous opposes = souvent nulle, meme avec 2 pions de plus.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("finales de cavaliers",
            new ExerciseConfig(
                "8/8/8/4k3/8/4K3/3N4/8 w - - 0 1",
                "Le Cavalier progresse lentement. Le Roi doit aider.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("evaluation positionnelle",
            new ExerciseConfig(
                "r1bqkb1r/ppp2ppp/2n2n2/3pp3/2PP4/2N2N2/PP2PPPP/R1BQKB1R w KQkq - 0 5",
                "Evaluez : structure de pions, activite, securite du Roi.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("strategie niveau 1",
            new ExerciseConfig(
                "r1bqk2r/ppp2ppp/2n2n2/3p4/3P4/2N2N2/PPP2PPP/R1BQKB1R w KQkq - 0 6",
                "Trouvez le meilleur plan strategique dans cette position.",
                "white"));

        // =====================================================
        // NIVEAU TOUR - 45 cours (Avance)
        // =====================================================

        // Preparation et etude
        COURSE_EXERCISE_CONFIGS.put("preparation d'ouverture",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Preparez vos ouvertures en etudiant les coups critiques.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("repertoire d'ouvertures",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Construisez un repertoire coherent avec les Blancs et les Noirs.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("nouveautes theoriques",
            new ExerciseConfig(
                "r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4",
                "Les nouveautes theoriques surprennent l'adversaire.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("analyse avec moteur",
            new ExerciseConfig(
                "r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4",
                "Utilisez le moteur pour comprendre, pas pour memoriser.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("bases de donnees",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Etudiez les parties des maitres pour comprendre les plans.",
                "white"));

        // Finales avancees
        COURSE_EXERCISE_CONFIGS.put("opposition distante",
            new ExerciseConfig(
                "8/8/8/8/3k4/8/8/3K4 w - - 0 1",
                "L'opposition distante : 3 ou 5 cases entre les Rois.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("zugzwang",
            new ExerciseConfig(
                "8/8/8/3k4/8/3K4/3P4/8 w - - 0 1",
                "Zugzwang : celui qui doit jouer perd. Forcez-le !",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("finales de tours avancees",
            new ExerciseConfig(
                "8/8/4k3/8/4P3/8/4K3/r7 w - - 0 1",
                "Finales Tour+Pion vs Tour : technique avancee.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("tour active vs tour passive",
            new ExerciseConfig(
                "8/8/4k3/8/4P3/8/4K3/r7 w - - 0 1",
                "La Tour active domine toujours la Tour passive.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("tour + pion vs tour",
            new ExerciseConfig(
                "8/8/8/4k3/4P3/8/4K3/r7 w - - 0 1",
                "Les cles : position du Roi defenseur et activite de la Tour.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("tour + 2 pions vs tour + pion",
            new ExerciseConfig(
                "8/8/8/4k3/4PP2/8/4K3/r7 w - - 0 1",
                "L'avantage d'un pion est souvent decisif avec les Tours.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("finales de pions complexes",
            new ExerciseConfig(
                "8/8/8/1p1k4/1P6/3K4/8/8 w - - 0 1",
                "Calcul precis requis dans les finales de pions complexes.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("pions passes lies",
            new ExerciseConfig(
                "8/8/8/3PP3/8/4k3/8/4K3 w - - 0 1",
                "Deux pions passes lies sont une force irresistible.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("roi actif en finale",
            new ExerciseConfig(
                "8/8/4k3/8/8/4K3/4P3/8 w - - 0 1",
                "En finale, le Roi devient une piece d'attaque !",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("triangle de reti",
            new ExerciseConfig(
                "8/8/8/8/8/1k6/1P6/K7 w - - 0 1",
                "L'etude de Reti : le Roi poursuit deux objectifs.",
                "white"));

        // Strategie avancee
        COURSE_EXERCISE_CONFIGS.put("prophylaxie",
            new ExerciseConfig(
                "r1bqk2r/ppp2ppp/2n2n2/3p4/3P4/2N2N2/PPP2PPP/R1BQKB1R w KQkq - 0 6",
                "La prophylaxie : empechezles plans adverses avant de jouer.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("coup d'attente",
            new ExerciseConfig(
                "r1bqk2r/ppp2ppp/2n2n2/3p4/3P4/2N2N2/PPP2PPP/R1BQKB1R w KQkq - 0 6",
                "Le coup d'attente passe le trait pour creer un zugzwang.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("amelioration des pieces",
            new ExerciseConfig(
                "r1bqk2r/ppp2ppp/2n2n2/3p4/3P4/2N2N2/PPP2PPP/R1BQKB1R w KQkq - 0 6",
                "Ameliorez vos pieces une par une vers leurs cases ideales.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("restriction",
            new ExerciseConfig(
                "r1bqk2r/ppp2ppp/2n2n2/3p4/3P4/2N2N2/PPP2PPP/R1BQKB1R w KQkq - 0 6",
                "Limitez l'activite des pieces adverses.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("echanges favorables",
            new ExerciseConfig(
                "r1bqk2r/ppp2ppp/2n2n2/3p4/3P4/2N2N2/PPP2PPP/R1BQKB1R w KQkq - 0 6",
                "Echangez les pieces actives adverses, gardez les votres.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("garder la tension",
            new ExerciseConfig(
                "r1bqk2r/ppp2ppp/2n2n2/3p4/2PP4/2N2N2/PP3PPP/R1BQKB1R b KQkq - 0 6",
                "Ne resolvez pas la tension trop tot. Gardez les options.",
                "black"));
        COURSE_EXERCISE_CONFIGS.put("plan a long terme",
            new ExerciseConfig(
                "r1bqk2r/ppp2ppp/2n2n2/3p4/3P4/2N2N2/PPP2PPP/R1BQKB1R w KQkq - 0 6",
                "Elaborez un plan sur plusieurs coups, pas un seul coup.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("changer de plan",
            new ExerciseConfig(
                "r1bqk2r/ppp2ppp/2n2n2/3p4/3P4/2N2N2/PPP2PPP/R1BQKB1R w KQkq - 0 6",
                "Adaptez votre plan si la position change.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("initiative",
            new ExerciseConfig(
                "r1bqk2r/ppp2ppp/2n2n2/3p4/3P4/2N2N2/PPP2PPP/R1BQKB1R w KQkq - 0 6",
                "L'initiative vaut souvent plus que du materiel.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("compensation",
            new ExerciseConfig(
                "r1bqk2r/ppp2ppp/2n2n2/3p4/3P4/2N2N2/PPP2PPP/R1BQKB1R w KQkq - 0 6",
                "Evaluez la compensation pour le materiel sacrifie.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("attaque roi non roque",
            new ExerciseConfig(
                "r1bqk2r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4",
                "Attaquez le Roi au centre avant qu'il ne roque !",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("roques opposes",
            new ExerciseConfig(
                "r3kb1r/ppp1qppp/2n2n2/3p4/3P4/2N2N2/PPPQ1PPP/R3KB1R w KQkq - 0 8",
                "Roques opposes : course a l'attaque. Avancez vos pions !",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("sacrifices positionnels",
            new ExerciseConfig(
                "r1bqk2r/ppp2ppp/2n2n2/3p4/3P4/2N2N2/PPP2PPP/R1BQKB1R w KQkq - 0 6",
                "Un sacrifice positionnel donne un avantage durable.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("sacrifice de qualite",
            new ExerciseConfig(
                "r1bqk2r/ppp2ppp/2n2n2/3p4/3P4/2N2N2/PPP2PPP/R1BQKB1R w KQkq - 0 6",
                "Tour contre piece mineure + compensation = bon echange.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("cases fortes",
            new ExerciseConfig(
                "r1bqk2r/ppp2ppp/2n2n2/3p4/3P4/2N2N2/PPP2PPP/R1BQKB1R w KQkq - 0 6",
                "Une piece sur une case forte domine la position.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("fou dominant",
            new ExerciseConfig(
                "r1bqk2r/ppp2ppp/2n2n2/3p4/3P4/2N2N2/PPP2PPP/R1BQKB1R w KQkq - 0 6",
                "En position ouverte, le Fou domine le Cavalier.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("cavalier dominant",
            new ExerciseConfig(
                "r1bqk2r/ppp2ppp/2np1n2/2b1p3/2B1P3/2NP1N2/PPP2PPP/R1BQK2R w KQkq - 0 6",
                "En position fermee, le Cavalier brille sur un avant-poste.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("paire de fous",
            new ExerciseConfig(
                "r1bqk2r/ppp2ppp/2n2n2/3p4/3P4/2N2N2/PPP2PPP/R1BQKB1R w KQkq - 0 6",
                "La paire de Fous est un avantage a long terme.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("fous opposes milieu",
            new ExerciseConfig(
                "r1bqk2r/ppp2ppp/2n2n2/3p4/3P4/2N2N2/PPP2PPP/R1BQKB1R w KQkq - 0 6",
                "Fous opposes en milieu de partie = potentiel d'attaque.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("pieces mineures finale",
            new ExerciseConfig(
                "8/8/8/4k3/8/4K3/2B5/3n4 w - - 0 1",
                "Fou vs Cavalier en finale : tout depend de la structure.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("evaluation dynamique",
            new ExerciseConfig(
                "r1bqk2r/ppp2ppp/2n2n2/3p4/3P4/2N2N2/PPP2PPP/R1BQKB1R w KQkq - 0 6",
                "L'evaluation dynamique tient compte du temps et de l'activite.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("positions critiques",
            new ExerciseConfig(
                "r1bqk2r/ppp2ppp/2n2n2/3p4/3P4/2N2N2/PPP2PPP/R1BQKB1R w KQkq - 0 6",
                "Identifiez les moments critiques ou tout se decide.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("gestion du temps",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Investissez votre temps dans les positions critiques.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("calcul des variantes",
            new ExerciseConfig(
                "r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4",
                "Methode de calcul : coups forces puis evaluation.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("visualisation",
            new ExerciseConfig(
                "r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4",
                "Visualisez la position apres la sequence de coups.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("coups candidats",
            new ExerciseConfig(
                "r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4",
                "Listez les coups candidats avant de calculer.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("elimination des coups",
            new ExerciseConfig(
                "r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4",
                "Eliminez les mauvais coups pour trouver le meilleur.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("intuition",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "L'intuition se developpe avec l'experience et l'etude.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("parties de maitres",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Etudiez les parties de maitres pour comprendre les plans.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("strategie niveau 2",
            new ExerciseConfig(
                "r1bqk2r/ppp2ppp/2n2n2/3p4/3P4/2N2N2/PPP2PPP/R1BQKB1R w KQkq - 0 6",
                "Positions strategiques complexes. Trouvez le meilleur plan !",
                "white"));

        // =====================================================
        // NIVEAU DAME - 45 cours (Expert)
        // =====================================================

        // Analyse et preparation
        COURSE_EXERCISE_CONFIGS.put("analyse approfondie",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "L'analyse approfondie revele les subtilites de la position.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("preparation psychologique",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "La preparation mentale est aussi importante que la technique.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("differents styles",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Adaptez votre jeu au style de l'adversaire.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("sacrifices speculatifs",
            new ExerciseConfig(
                "r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/3P1N2/PPP2PPP/RNBQK2R w KQkq - 0 5",
                "Les sacrifices speculatifs offrent compensation floue mais dangereuse.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("attaque et defense",
            new ExerciseConfig(
                "r1bqk2r/ppp2ppp/2n2n2/3p4/3P4/2N2N2/PPP2PPP/R1BQKB1R w KQkq - 0 6",
                "L'equilibre parfait entre attaque et defense.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("positions dynamiques",
            new ExerciseConfig(
                "r1bqk2r/ppp2ppp/2n2n2/3p4/3P4/2N2N2/PPP2PPP/R1BQKB1R w KQkq - 0 6",
                "En position dynamique, le temps compte plus que le materiel.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("positions statiques",
            new ExerciseConfig(
                "r1bqk2r/ppp2ppp/2n2n2/3p4/3P4/2N2N2/PPP2PPP/R1BQKB1R w KQkq - 0 6",
                "En position statique, accumulez petits avantages.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("transformation avantages",
            new ExerciseConfig(
                "r1bqk2r/ppp2ppp/2n2n2/3p4/3P4/2N2N2/PPP2PPP/R1BQKB1R w KQkq - 0 6",
                "Transformez un avantage en un autre plus exploitable.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("jeu technique",
            new ExerciseConfig(
                "8/8/8/4k3/4P3/4K3/8/8 w - - 0 1",
                "Le jeu technique convertit l'avantage en victoire.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("defendre positions difficiles",
            new ExerciseConfig(
                "8/8/8/4k3/8/4K3/4p3/8 w - - 0 1",
                "L'art de la defense dans les positions inferieures.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("ressources defensives",
            new ExerciseConfig(
                "8/8/8/4k3/8/4K3/4p3/8 w - - 0 1",
                "Trouvez les ressources cachees pour sauver la partie.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("contre-jeu",
            new ExerciseConfig(
                "r1bqk2r/ppp2ppp/2n2n2/3p4/3P4/2N2N2/PPP2PPP/R1BQKB1R b KQkq - 0 6",
                "Creez du contre-jeu pour compliquer la tache de l'attaquant.",
                "black"));
        COURSE_EXERCISE_CONFIGS.put("finales theoriques",
            new ExerciseConfig(
                "8/8/8/4k3/8/4K3/8/8 w - - 0 1",
                "Les finales theoriques : connaissances essentielles.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("finales tours complexes",
            new ExerciseConfig(
                "8/8/4k3/8/4P3/4K3/8/r7 w - - 0 1",
                "Maitrise complete des finales de Tours.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("finales pieces mineures",
            new ExerciseConfig(
                "8/8/8/4k3/8/4K3/2B5/3n4 w - - 0 1",
                "Nuances des finales de pieces mineures.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("finales de dames",
            new ExerciseConfig(
                "8/8/8/4k3/8/4K3/3Q4/3q4 w - - 0 1",
                "Techniques speciales des finales de Dames.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("etudes finales celebres",
            new ExerciseConfig(
                "8/8/8/4k3/8/4K3/8/8 w - - 0 1",
                "Les etudes classiques : Reti, Saavedra, et autres.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("preparation tournoi",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Preparez-vous physiquement et mentalement avant un tournoi.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("gestion tournoi",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Gerez votre energie et vos emotions pendant le tournoi.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("analyse post-tournoi",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Analysez vos parties pour identifier axes d'amelioration.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("adversaires plus forts",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Contre un adversaire plus fort : solidite et opportunisme.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("adversaires plus faibles",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Ne sous-estimez jamais un adversaire plus faible.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("parties rapides",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "En rapide et blitz : intuition et experience priment.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("parties longues",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "En parties longues : analyse approfondie et patience.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("style universel",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Le style universel s'adapte a toutes les positions.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("parties modeles - tactique",
            new ExerciseConfig(
                "r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4",
                "Etudiez les combinaisons brillantes des grands maitres.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("parties modeles - strategie",
            new ExerciseConfig(
                "r1bqk2r/ppp2ppp/2n2n2/3p4/3P4/2N2N2/PPP2PPP/R1BQKB1R w KQkq - 0 6",
                "Les plans magistraux de Capablanca, Karpov, Carlsen.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("parties modeles - finales",
            new ExerciseConfig(
                "8/8/8/4k3/4P3/4K3/8/8 w - - 0 1",
                "La technique parfaite des grands finalistes.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("steinitz capablanca",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Steinitz a Capablanca : les fondements positionnels.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("alekhine botvinnik",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Alekhine a Botvinnik : l'ere sovietique commence.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("smyslov spassky",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Smyslov a Spassky : l'age d'or des echecs.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("fischer",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Bobby Fischer : le genie americain qui a tout change.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("karpov kasparov",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Karpov vs Kasparov : la plus grande rivalite.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("kramnik carlsen",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Kramnik a Carlsen : l'ere moderne des echecs.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("evolution des echecs",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Du romantisme a l'ere informatique : evolution du jeu.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("echecs et informatique",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "L'impact des moteurs sur la theorie et le jeu.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("preparer avec ordinateur",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Utilisez l'ordinateur comme outil, pas comme bequille.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("au-dela de l'ordinateur",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "L'humain voit ce que la machine ne comprend pas.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("style personnel",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Developpez votre propre style de jeu.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("philosophie du jeu",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "L'approche mentale des champions.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("enseigner les echecs",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Transmettez votre savoir aux autres.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("competition haut niveau",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Les exigences du jeu professionnel.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("maintenir son niveau",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "L'entrainement continu pour rester au sommet.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("passion des echecs",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Cultivez votre amour du jeu.",
                "white"));
        COURSE_EXERCISE_CONFIGS.put("maitrise complete",
            new ExerciseConfig(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "Le chemin vers la maitrise ne s'arrete jamais.",
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

    /**
     * Update old exercises that still reference "l'IA" to use "myChessBot"
     */
    @jakarta.annotation.PostConstruct
    @Transactional
    public void updateOldExerciseDescriptions() {
        List<Exercise> allExercises = exerciseRepository.findAll();
        int updated = 0;
        for (Exercise exercise : allExercises) {
            boolean modified = false;
            if (exercise.getDescription() != null && exercise.getDescription().contains("l'IA")) {
                exercise.setDescription(exercise.getDescription().replace("l'IA", "myChessBot"));
                modified = true;
            }
            if (exercise.getTitle() != null && exercise.getTitle().contains("l'IA")) {
                exercise.setTitle(exercise.getTitle().replace("l'IA", "myChessBot"));
                modified = true;
            }
            if (modified) {
                exerciseRepository.save(exercise);
                updated++;
            }
        }
        if (updated > 0) {
            log.info("Updated {} old exercises to use 'myChessBot' instead of 'l'IA'", updated);
        }
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
            .orElse(ChessLevel.A);

        DifficultyLevel difficulty = mapChessLevelToDifficulty(studentLevel);

        // First, use the course directly attached to the lesson (set by teacher during booking)
        // Fallback to learning path course if no course is attached
        Course lessonCourse = lesson.getCourse();
        Optional<Course> currentCourse = lessonCourse != null
            ? Optional.of(lessonCourse)
            : getCurrentCourseForStudent(userId, studentLevel);

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
            case A -> DifficultyLevel.DEBUTANT;
            case B -> DifficultyLevel.FACILE;
            case C -> DifficultyLevel.MOYEN;
            case D -> DifficultyLevel.EXPERT;
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
            .orElse(ChessLevel.A);

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
