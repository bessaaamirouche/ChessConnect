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

            // 5 questions per level = 20 total (4 levels)
            questions.addAll(createLevelAQuestions());
            questions.addAll(createLevelBQuestions());
            questions.addAll(createLevelCQuestions());
            questions.addAll(createLevelDQuestions());

            questionRepository.saveAll(questions);
            System.out.println("✓ 20 questions de quiz initialisées avec succès");
        }
    }

    private List<QuizQuestion> createLevelAQuestions() {
        List<QuizQuestion> questions = new ArrayList<>();

        questions.add(new QuizQuestion(
            ChessLevel.A,
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
            ChessLevel.A,
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
            ChessLevel.A,
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
            ChessLevel.A,
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
            ChessLevel.A,
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

    private List<QuizQuestion> createLevelBQuestions() {
        List<QuizQuestion> questions = new ArrayList<>();

        questions.add(new QuizQuestion(
            ChessLevel.B,
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
            ChessLevel.B,
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
            ChessLevel.B,
            "Quels sont les premiers coups de la Défense Sicilienne ?",
            "1.e4 e5",
            "1.d4 d5",
            "1.e4 c5",
            "1.c4 e5",
            "C",
            "La Défense Sicilienne commence par 1.e4 c5. C'est la réponse la plus populaire et combative contre 1.e4.",
            3
        ));

        questions.add(new QuizQuestion(
            ChessLevel.B,
            "Qu'est-ce qu'un pion passé ?",
            "Un pion qui a atteint la dernière rangée",
            "Un pion qui n'a plus de pions adverses devant lui ou sur les colonnes adjacentes",
            "Un pion protégé par un autre pion",
            "Un pion isolé",
            "B",
            "Un pion passé n'a plus de pions adverses pouvant bloquer son avance vers la promotion. Ces pions sont très précieux en finale.",
            4
        ));

        questions.add(new QuizQuestion(
            ChessLevel.B,
            "Pourquoi le contrôle du centre est-il important en ouverture ?",
            "Car le Roi doit être au centre",
            "Car les pièces au centre contrôlent plus de cases et sont plus mobiles",
            "Car c'est une règle obligatoire",
            "Car les pions du centre valent plus",
            "B",
            "Les pièces placées au centre contrôlent plus de cases et peuvent se déplacer facilement des deux côtés de l'échiquier. Le centre (e4, d4, e5, d5) est stratégiquement crucial.",
            5
        ));

        return questions;
    }

    private List<QuizQuestion> createLevelCQuestions() {
        List<QuizQuestion> questions = new ArrayList<>();

        questions.add(new QuizQuestion(
            ChessLevel.C,
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
            ChessLevel.C,
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
            ChessLevel.C,
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
            ChessLevel.C,
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
            ChessLevel.C,
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

    private List<QuizQuestion> createLevelDQuestions() {
        List<QuizQuestion> questions = new ArrayList<>();

        questions.add(new QuizQuestion(
            ChessLevel.D,
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
            ChessLevel.D,
            "Qu'est-ce que le zugzwang ?",
            "Une attaque double",
            "Une situation où tout coup empire sa position - l'obligation de jouer est un désavantage",
            "Un échec découvert",
            "Une technique de finale",
            "B",
            "Le zugzwang (de l'allemand 'obligation de jouer') est une situation où le joueur qui doit jouer voit sa position se détériorer quel que soit son coup. C'est courant en finale.",
            2
        ));

        questions.add(new QuizQuestion(
            ChessLevel.D,
            "Qu'est-ce que le concept de 'forteresse' aux échecs ?",
            "Une position où le matériel supérieur ne peut pas forcer le gain",
            "Un roque avec tous les pions intacts",
            "Une structure de pions imprenable",
            "Une technique d'attaque sur le Roi adverse",
            "A",
            "Une forteresse est une position défensive où malgré un désavantage matériel significatif, l'adversaire ne peut pas progresser ni forcer le gain. C'est un concept crucial en finale.",
            3
        ));

        questions.add(new QuizQuestion(
            ChessLevel.D,
            "Quelle est la caractéristique principale du système de Maroczy (Bind) ?",
            "Une attaque rapide sur le Roi",
            "Le contrôle spatial avec les pions c4 et e4 empêchant ...d5",
            "Un sacrifice de pion pour l'initiative",
            "Un système basé sur le fianchetto des deux Fous",
            "B",
            "Le Maroczy Bind (étau de Maroczy) est une structure où les Blancs placent leurs pions en c4 et e4, contrôlant d5 et limitant l'espace des Noirs. C'est une arme stratégique durable.",
            4
        ));

        questions.add(new QuizQuestion(
            ChessLevel.D,
            "Qu'est-ce que le principe de la 'transformation des avantages' de Steinitz ?",
            "Échanger ses pièces quand on a l'avantage",
            "Convertir un type d'avantage temporaire en un avantage permanent d'une autre nature",
            "Transformer un pion en Dame",
            "Changer de plan quand l'adversaire défend bien",
            "B",
            "La transformation des avantages est un concept stratégique où l'on convertit un avantage temporaire (initiative, espace) en un avantage permanent (structure de pions, matériel) avant qu'il ne disparaisse.",
            5
        ));

        return questions;
    }
}
