package com.chessconnect.config;

import com.chessconnect.model.QuizQuestion;
import com.chessconnect.model.enums.ChessLevel;
import com.chessconnect.repository.QuizQuestionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Order(2) // Run after CourseDataInitializer
public class QuizDataInitializer implements CommandLineRunner {

    private final QuizQuestionRepository questionRepository;

    public QuizDataInitializer(QuizQuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @Override
    public void run(String... args) {
        if (questionRepository.count() == 0) {
            List<QuizQuestion> questions = new ArrayList<>();

            // 5 questions per level = 25 total
            questions.addAll(createPionQuestions());
            questions.addAll(createCavalierQuestions());
            questions.addAll(createFouQuestions());
            questions.addAll(createTourQuestions());
            questions.addAll(createDameQuestions());

            questionRepository.saveAll(questions);
            System.out.println("✓ 25 questions de quiz initialisées avec succès");
        }
    }

    private List<QuizQuestion> createPionQuestions() {
        List<QuizQuestion> questions = new ArrayList<>();

        questions.add(new QuizQuestion(
            ChessLevel.PION,
            "Comment se déplace le Cavalier ?",
            "En diagonale uniquement",
            "En forme de L (2 cases + 1 case perpendiculaire)",
            "En ligne droite uniquement",
            "D'une case dans toutes les directions",
            "B",
            "Le Cavalier se déplace en forme de L : deux cases dans une direction puis une case perpendiculaire. C'est la seule pièce qui peut sauter par-dessus les autres.",
            1
        ));

        questions.add(new QuizQuestion(
            ChessLevel.PION,
            "Combien de cases peut avancer un pion lors de son premier coup ?",
            "1 case uniquement",
            "1 ou 2 cases au choix",
            "3 cases",
            "Autant qu'il veut",
            "B",
            "Lors de son premier mouvement, un pion peut avancer d'une ou deux cases. Après, il ne peut avancer que d'une case à la fois.",
            2
        ));

        questions.add(new QuizQuestion(
            ChessLevel.PION,
            "Qu'est-ce qu'un échec et mat ?",
            "Quand le roi est capturé",
            "Quand le roi est attaqué et ne peut pas s'échapper",
            "Quand toutes les pièces sont prises",
            "Quand un joueur abandonne",
            "B",
            "L'échec et mat survient quand le roi est en échec (attaqué) et qu'il n'existe aucun coup légal pour l'en sortir. C'est la fin de la partie.",
            3
        ));

        questions.add(new QuizQuestion(
            ChessLevel.PION,
            "Quelle est la valeur relative de la Dame par rapport au Pion ?",
            "3 pions",
            "5 pions",
            "9 pions",
            "12 pions",
            "C",
            "La Dame vaut environ 9 pions. C'est la pièce la plus puissante car elle combine les mouvements de la Tour et du Fou.",
            4
        ));

        questions.add(new QuizQuestion(
            ChessLevel.PION,
            "Qu'est-ce que le roque ?",
            "Un coup qui permet de capturer deux pièces",
            "Un coup spécial où le Roi et une Tour bougent ensemble",
            "Un coup qui transforme un pion en Dame",
            "Un coup où le Cavalier saute trois cases",
            "B",
            "Le roque est un coup spécial qui permet de mettre le Roi en sécurité tout en activant la Tour. Le Roi se déplace de deux cases vers la Tour, et la Tour saute par-dessus le Roi.",
            5
        ));

        return questions;
    }

    private List<QuizQuestion> createCavalierQuestions() {
        List<QuizQuestion> questions = new ArrayList<>();

        questions.add(new QuizQuestion(
            ChessLevel.CAVALIER,
            "Qu'est-ce qu'une fourchette aux échecs ?",
            "Un coup qui défend deux pièces",
            "Une attaque simultanée sur deux pièces ou plus",
            "Un échange de pièces",
            "Une ouverture populaire",
            "B",
            "Une fourchette est une tactique où une pièce attaque deux (ou plus) pièces adverses simultanément. Le Cavalier est particulièrement efficace pour les fourchettes.",
            1
        ));

        questions.add(new QuizQuestion(
            ChessLevel.CAVALIER,
            "Qu'est-ce qu'un clouage ?",
            "Quand une pièce ne peut pas bouger car elle protège une pièce plus importante",
            "Quand deux pièces sont sur la même case",
            "Quand le Roi est bloqué dans un coin",
            "Une technique d'ouverture",
            "A",
            "Un clouage empêche une pièce de bouger car son déplacement exposerait une pièce plus importante (souvent le Roi) à une attaque.",
            2
        ));

        questions.add(new QuizQuestion(
            ChessLevel.CAVALIER,
            "Comment mater avec Roi + Tour contre Roi seul ?",
            "En plaçant la Tour au centre",
            "En poussant le Roi adverse vers le bord de l'échiquier",
            "C'est impossible",
            "En sacrifiant la Tour",
            "B",
            "Pour mater avec Roi + Tour, il faut utiliser la technique de 'l'escalier' : repousser progressivement le Roi adverse vers le bord, puis donner le mat sur la dernière rangée.",
            3
        ));

        questions.add(new QuizQuestion(
            ChessLevel.CAVALIER,
            "Qu'est-ce qu'une enfilade ?",
            "Une attaque sur une pièce importante qui, en bougeant, expose une pièce derrière elle",
            "Une série de coups forcés",
            "Une formation de pions",
            "Un type de sacrifice",
            "A",
            "L'enfilade est l'inverse du clouage : on attaque une pièce importante (comme le Roi ou la Dame) qui doit bouger, exposant une pièce moins importante derrière elle.",
            4
        ));

        questions.add(new QuizQuestion(
            ChessLevel.CAVALIER,
            "En général, quel couple de pièces mineures est considéré comme légèrement supérieur ?",
            "Deux Cavaliers",
            "Deux Fous (la paire de Fous)",
            "Un Cavalier et un Fou",
            "Ils sont exactement égaux",
            "B",
            "La paire de Fous est souvent considérée comme légèrement supérieure, surtout dans les positions ouvertes où les diagonales sont dégagées.",
            5
        ));

        return questions;
    }

    private List<QuizQuestion> createFouQuestions() {
        List<QuizQuestion> questions = new ArrayList<>();

        questions.add(new QuizQuestion(
            ChessLevel.FOU,
            "Quels sont les premiers coups de la Défense Sicilienne ?",
            "1.e4 e5",
            "1.d4 d5",
            "1.e4 c5",
            "1.c4 e5",
            "C",
            "La Défense Sicilienne commence par 1.e4 c5. C'est la réponse la plus populaire et combative contre 1.e4.",
            1
        ));

        questions.add(new QuizQuestion(
            ChessLevel.FOU,
            "Qu'est-ce qu'un pion passé ?",
            "Un pion qui a atteint la dernière rangée",
            "Un pion qui n'a plus de pions adverses devant lui ou sur les colonnes adjacentes",
            "Un pion protégé par un autre pion",
            "Un pion isolé",
            "B",
            "Un pion passé n'a plus de pions adverses pouvant bloquer son avance vers la promotion. Ces pions sont très précieux en finale.",
            2
        ));

        questions.add(new QuizQuestion(
            ChessLevel.FOU,
            "Pourquoi le contrôle du centre est-il important en ouverture ?",
            "Car le Roi doit être au centre",
            "Car les pièces au centre contrôlent plus de cases et sont plus mobiles",
            "Car c'est une règle obligatoire",
            "Car les pions du centre valent plus",
            "B",
            "Les pièces placées au centre contrôlent plus de cases et peuvent se déplacer facilement des deux côtés de l'échiquier. Le centre (e4, d4, e5, d5) est stratégiquement crucial.",
            3
        ));

        questions.add(new QuizQuestion(
            ChessLevel.FOU,
            "Qu'est-ce que l'Ouverture Italienne ?",
            "1.e4 e5 2.Cf3 Cc6 3.Fc4",
            "1.e4 e5 2.Cf3 Cc6 3.Fb5",
            "1.d4 d5 2.c4",
            "1.e4 c5",
            "A",
            "L'Ouverture Italienne (Giuoco Piano) commence par 1.e4 e5 2.Cf3 Cc6 3.Fc4, où le Fou vise la case faible f7. L'Espagnole serait 3.Fb5.",
            4
        ));

        questions.add(new QuizQuestion(
            ChessLevel.FOU,
            "Qu'est-ce qu'une colonne ouverte ?",
            "Une colonne sans aucune pièce",
            "Une colonne sans pions",
            "Une colonne contrôlée par une Tour",
            "La colonne du Roi après le roque",
            "B",
            "Une colonne ouverte est une colonne verticale (a-h) qui ne contient aucun pion. Les Tours sont particulièrement efficaces sur les colonnes ouvertes.",
            5
        ));

        return questions;
    }

    private List<QuizQuestion> createTourQuestions() {
        List<QuizQuestion> questions = new ArrayList<>();

        questions.add(new QuizQuestion(
            ChessLevel.TOUR,
            "Qu'est-ce que la technique de Philidor dans les finales Tour + Pion ?",
            "Attaquer le pion adverse avec la Tour",
            "Placer la Tour sur la 6ème rangée pour couper le Roi adverse",
            "Sacrifier la Tour pour le pion",
            "Avancer son propre pion rapidement",
            "B",
            "La défense Philidor consiste à placer sa Tour sur la 3ème rangée (6ème pour les Noirs) pour empêcher le Roi adverse d'avancer. Une fois que le pion avance, on donne des échecs par derrière.",
            1
        ));

        questions.add(new QuizQuestion(
            ChessLevel.TOUR,
            "Qu'est-ce que l'opposition des Rois ?",
            "Quand les deux Rois sont face à face avec une case entre eux",
            "Quand un Roi attaque l'autre",
            "Quand les Rois sont sur la même diagonale",
            "Quand un Roi ne peut plus bouger",
            "A",
            "L'opposition est une position où les deux Rois sont sur la même ligne avec une case (ou nombre impair de cases) entre eux. Le joueur qui n'a pas le trait 'a l'opposition'.",
            2
        ));

        questions.add(new QuizQuestion(
            ChessLevel.TOUR,
            "Qu'est-ce que la règle du carré dans les finales de pions ?",
            "Les pions doivent former un carré",
            "Si le Roi peut entrer dans le carré imaginaire du pion, il peut l'arrêter",
            "Le Roi doit rester au centre du carré",
            "Il faut quatre pions pour gagner",
            "B",
            "La règle du carré permet de savoir rapidement si un Roi peut rattraper un pion en course vers la promotion : si le Roi peut entrer dans le carré formé par le pion et la case de promotion, il l'attrape.",
            3
        ));

        questions.add(new QuizQuestion(
            ChessLevel.TOUR,
            "Qu'est-ce que la position de Lucena ?",
            "Une position de mat célèbre",
            "Une position gagnante avec Tour + Pion où on utilise la technique du 'pont'",
            "Une défense contre le mat du couloir",
            "Une ouverture italienne avancée",
            "B",
            "La position de Lucena est la position gagnante classique Tour + Pion vs Tour. On utilise la technique du 'pont' : le Roi se protège des échecs en utilisant sa propre Tour.",
            4
        ));

        questions.add(new QuizQuestion(
            ChessLevel.TOUR,
            "Qu'est-ce que la prophylaxie aux échecs ?",
            "Une technique de promotion de pion",
            "Anticiper et empêcher les plans de l'adversaire avant qu'il ne les réalise",
            "Une stratégie d'attaque agressive",
            "Un type de sacrifice positionnel",
            "B",
            "La prophylaxie consiste à identifier ce que l'adversaire veut faire et à l'en empêcher. On se pose la question : 'Quel est le plan adverse ?' avant de jouer.",
            5
        ));

        return questions;
    }

    private List<QuizQuestion> createDameQuestions() {
        List<QuizQuestion> questions = new ArrayList<>();

        questions.add(new QuizQuestion(
            ChessLevel.DAME,
            "Qu'est-ce qu'un sacrifice positionnel ?",
            "Un sacrifice qui gagne immédiatement du matériel",
            "Un sacrifice pour un mat en quelques coups",
            "Un sacrifice sans compensation matérielle immédiate, pour des avantages positionnels durables",
            "Un sacrifice de pion en ouverture",
            "C",
            "Un sacrifice positionnel est un sacrifice où la compensation n'est pas tactique (mat ou gain de matériel) mais positionnelle : colonnes ouvertes, cases faibles, initiative durable.",
            1
        ));

        questions.add(new QuizQuestion(
            ChessLevel.DAME,
            "Quels facteurs sont essentiels pour évaluer une position complexe ?",
            "Uniquement le matériel",
            "Matériel, activité des pièces, structure de pions, sécurité du Roi, et initiative",
            "Le nombre de pièces développées",
            "La position du Roi uniquement",
            "B",
            "L'évaluation d'une position prend en compte de nombreux facteurs : le matériel, l'activité des pièces, la structure de pions, la sécurité des Rois, l'initiative, et les plans possibles.",
            2
        ));

        questions.add(new QuizQuestion(
            ChessLevel.DAME,
            "Qu'est-ce que le zugzwang ?",
            "Une attaque double",
            "Une situation où tout coup empire sa position - l'obligation de jouer est un désavantage",
            "Un échec découvert",
            "Une technique de finale",
            "B",
            "Le zugzwang (de l'allemand 'obligation de jouer') est une situation où le joueur qui doit jouer voit sa position se détériorer quel que soit son coup. C'est courant en finale.",
            3
        ));

        questions.add(new QuizQuestion(
            ChessLevel.DAME,
            "Quels éléments sont nécessaires pour mener une attaque sur le roque adverse ?",
            "Avoir plus de matériel",
            "Plus de pièces en attaque qu'en défense, des faiblesses dans l'abri du Roi, des lignes ouvertes",
            "Avoir la Dame et rien d'autre",
            "Avoir fait le grand roque soi-même",
            "B",
            "Pour attaquer le roque, il faut généralement : plus de pièces participant à l'attaque que de défenseurs, des faiblesses (pions avancés, cases faibles), et des lignes/diagonales ouvertes vers le Roi.",
            4
        ));

        questions.add(new QuizQuestion(
            ChessLevel.DAME,
            "Qu'est-ce que la défense dynamique ?",
            "Défendre passivement ses pièces",
            "Se défendre en créant des contre-menaces et en maintenant l'activité de ses pièces",
            "Échanger toutes les pièces",
            "Fuir avec le Roi",
            "B",
            "La défense dynamique consiste à se défendre de façon active : créer des contre-menaces, maintenir l'initiative, utiliser des ressources tactiques plutôt que de défendre passivement.",
            5
        ));

        return questions;
    }
}
