import { Component, signal, ChangeDetectionStrategy, inject, OnInit, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import { TranslateModule } from '@ngx-translate/core';
import {
  heroAcademicCap,
  heroChevronDown,
  heroChevronRight,
  heroBookOpen,
  heroTrophy,
  heroStar,
  heroCheckCircle,
  heroArrowLeft,
  heroArrowRight,
  heroPlay
} from '@ng-icons/heroicons/outline';
import { AuthService } from '../../core/services/auth.service';
import { ProgrammeService, ProgrammeCourse } from '../../core/services/programme.service';
import { ToastService } from '../../core/services/toast.service';

interface SubLesson {
  title: string;
}

interface Course {
  id: number;
  title: string;
  subLessons: SubLesson[];
}

interface Level {
  code: string;
  name: string;
  description: string;
  color: string;
  icon: string;
  courses: Course[];
}

@Component({
  selector: 'app-programme',
  standalone: true,
  imports: [CommonModule, NgIconComponent, TranslateModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  viewProviders: [provideIcons({
    heroAcademicCap,
    heroChevronDown,
    heroChevronRight,
    heroBookOpen,
    heroTrophy,
    heroStar,
    heroCheckCircle,
    heroArrowLeft,
    heroArrowRight,
    heroPlay
  })],
  templateUrl: './programme.component.html',
  styleUrl: './programme.component.scss'
})
export class ProgrammeComponent implements OnInit {
  private authService = inject(AuthService);
  private programmeService = inject(ProgrammeService);
  private toastService = inject(ToastService);

  expandedLevels = signal<Set<string>>(new Set(['A']));
  expandedCourses = signal<Set<number>>(new Set());
  currentCourseId = signal<number>(1);
  loading = signal(false);

  isStudent = computed(() => this.authService.isStudent());
  isTeacher = computed(() => this.authService.isTeacher());

  ngOnInit(): void {
    if (this.authService.isStudent()) {
      this.loadStudentProgress();
    }
  }

  loadStudentProgress(): void {
    this.loading.set(true);
    this.programmeService.loadCourses().subscribe({
      next: (courses) => {
        const current = courses.find(c => c.isCurrent);
        if (current) {
          this.currentCourseId.set(current.id);
          // Auto-expand the level containing the current course
          const level = this.levels.find(l => l.courses.some(c => c.id === current.id));
          if (level) {
            this.expandedLevels.set(new Set([level.code]));
          }
        }
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  selectStartingCourse(courseId: number): void {
    if (!this.authService.isStudent()) return;

    this.programmeService.setCurrentCourse(courseId).subscribe({
      next: (course) => {
        this.currentCourseId.set(course.id);
        this.toastService.success(`Cours de départ défini : ${course.title}`);
      },
      error: () => this.toastService.error('Erreur lors de la sélection du cours')
    });
  }

  goBackToPreviousCourse(): void {
    if (!this.authService.isStudent()) return;

    this.programmeService.goBackToPreviousCourse().subscribe({
      next: (course) => {
        this.currentCourseId.set(course.id);
        this.toastService.success(`Retour au cours : ${course.title}`);
      },
      error: () => this.toastService.error('Erreur lors du retour au cours précédent')
    });
  }

  isCurrent(courseId: number): boolean {
    return this.currentCourseId() === courseId;
  }

  isCompleted(courseId: number): boolean {
    return courseId < this.currentCourseId();
  }

  getCurrentCourseTitle(): string {
    const currentId = this.currentCourseId();
    for (const level of this.levels) {
      const course = level.courses.find(c => c.id === currentId);
      if (course) return course.title;
    }
    return '';
  }

  getCurrentLevelName(): string {
    const currentId = this.currentCourseId();
    const levelNames: Record<string, string> = {
      'A': 'Débutant',
      'B': 'Intermédiaire',
      'C': 'Avancé',
      'D': 'Expert'
    };
    for (const level of this.levels) {
      if (level.courses.some(c => c.id === currentId)) {
        return levelNames[level.code] || level.code;
      }
    }
    return 'Débutant';
  }

  levels: Level[] = [
    {
      code: 'A',
      name: 'Pion - Débutant',
      description: 'Les fondamentaux des échecs pour bien commencer',
      color: '#4CAF50',
      icon: '♙',
      courses: [
        {
          id: 1,
          title: 'L\'échiquier et les pièces',
          subLessons: [
            { title: 'Présentation de l\'échiquier (colonnes, rangées, diagonales)' },
            { title: 'Les cases blanches et noires' },
            { title: 'La notation algébrique' },
            { title: 'Placement initial des pièces' },
            { title: 'Valeur relative des pièces' }
          ]
        },
        {
          id: 2,
          title: 'Le déplacement du Pion',
          subLessons: [
            { title: 'Avance d\'une case' },
            { title: 'Avance de deux cases (premier coup)' },
            { title: 'La prise en diagonale' },
            { title: 'La prise en passant' },
            { title: 'La promotion du pion' },
            { title: 'Exercices pratiques' }
          ]
        },
        {
          id: 3,
          title: 'Le déplacement de la Tour',
          subLessons: [
            { title: 'Mouvement horizontal' },
            { title: 'Mouvement vertical' },
            { title: 'La Tour et les colonnes ouvertes' },
            { title: 'Coordination de deux Tours' }
          ]
        },
        {
          id: 4,
          title: 'Le déplacement du Fou',
          subLessons: [
            { title: 'Mouvement en diagonale' },
            { title: 'Fou de cases blanches vs cases noires' },
            { title: 'La paire de Fous' },
            { title: 'Les diagonales ouvertes' },
            { title: 'Exercices de trajectoire' }
          ]
        },
        {
          id: 5,
          title: 'Le déplacement de la Dame',
          subLessons: [
            { title: 'Combinaison Tour + Fou' },
            { title: 'Puissance de la Dame' },
            { title: 'Dangers de la Dame exposée' },
            { title: 'La Dame en attaque' }
          ]
        },
        {
          id: 6,
          title: 'Le déplacement du Cavalier',
          subLessons: [
            { title: 'Le mouvement en L' },
            { title: 'Le saut par-dessus les pièces' },
            { title: 'Les cases accessibles' },
            { title: 'Le Cavalier au centre vs au bord' },
            { title: 'La fourche du Cavalier' },
            { title: 'Exercices de parcours' }
          ]
        },
        {
          id: 7,
          title: 'Le déplacement du Roi',
          subLessons: [
            { title: 'Mouvement d\'une case' },
            { title: 'Le Roi ne peut pas se mettre en échec' },
            { title: 'L\'importance de protéger le Roi' },
            { title: 'Le Roi actif en finale' }
          ]
        },
        {
          id: 8,
          title: 'L\'échec et l\'échec et mat',
          subLessons: [
            { title: 'Définition de l\'échec' },
            { title: 'Les trois façons de parer un échec' },
            { title: 'L\'échec et mat : fin de partie' },
            { title: 'Mats simples avec Dame' },
            { title: 'Mats simples avec Tour' }
          ]
        },
        {
          id: 9,
          title: 'Le Roque',
          subLessons: [
            { title: 'Le petit roque (côté Roi)' },
            { title: 'Le grand roque (côté Dame)' },
            { title: 'Conditions du roque' },
            { title: 'Quand roquer ?' },
            { title: 'Erreurs courantes' }
          ]
        },
        {
          id: 10,
          title: 'Le Pat et les nulles',
          subLessons: [
            { title: 'Définition du Pat' },
            { title: 'Nulle par accord mutuel' },
            { title: 'Nulle par répétition de coups' },
            { title: 'Règle des 50 coups' },
            { title: 'Matériel insuffisant' }
          ]
        },
        {
          id: 11,
          title: 'Principes d\'ouverture (1)',
          subLessons: [
            { title: 'Contrôler le centre' },
            { title: 'Développer les pièces mineures' },
            { title: 'Ne pas sortir la Dame trop tôt' },
            { title: 'Roquer rapidement' }
          ]
        },
        {
          id: 12,
          title: 'Principes d\'ouverture (2)',
          subLessons: [
            { title: 'Ne pas jouer la même pièce deux fois' },
            { title: 'Connecter les Tours' },
            { title: 'Éviter les pions faibles' },
            { title: 'L\'importance du développement' },
            { title: 'Exemples pratiques' }
          ]
        },
        {
          id: 13,
          title: 'Les mats élémentaires',
          subLessons: [
            { title: 'Mat du couloir' },
            { title: 'Mat du baiser' },
            { title: 'Mat à l\'étouffée' },
            { title: 'Mat de l\'escalier' },
            { title: 'Mat avec deux Tours' },
            { title: 'Exercices de mat en 1 coup' }
          ]
        },
        {
          id: 14,
          title: 'Introduction aux tactiques',
          subLessons: [
            { title: 'Qu\'est-ce qu\'une tactique ?' },
            { title: 'La menace' },
            { title: 'L\'attaque double' },
            { title: 'Repérer les opportunités' }
          ]
        },
        {
          id: 15,
          title: 'La fourchette',
          subLessons: [
            { title: 'Fourchette de Cavalier' },
            { title: 'Fourchette de Pion' },
            { title: 'Fourchette de Dame' },
            { title: 'Reconnaître les positions de fourchette' },
            { title: 'Exercices pratiques' }
          ]
        },
        {
          id: 16,
          title: 'Le clouage',
          subLessons: [
            { title: 'Clouage absolu (sur le Roi)' },
            { title: 'Clouage relatif' },
            { title: 'Exploiter un clouage' },
            { title: 'Se libérer d\'un clouage' }
          ]
        },
        {
          id: 17,
          title: 'L\'enfilade',
          subLessons: [
            { title: 'Définition et mécanisme' },
            { title: 'Enfilade avec la Tour' },
            { title: 'Enfilade avec le Fou' },
            { title: 'Enfilade avec la Dame' },
            { title: 'Exercices d\'identification' }
          ]
        },
        {
          id: 18,
          title: 'Finale Roi + Dame vs Roi',
          subLessons: [
            { title: 'Technique de base' },
            { title: 'Pousser le Roi adverse au bord' },
            { title: 'Éviter le Pat' },
            { title: 'Méthode systématique' }
          ]
        },
        {
          id: 19,
          title: 'Finale Roi + Tour vs Roi',
          subLessons: [
            { title: 'La méthode de l\'escalier' },
            { title: 'L\'opposition' },
            { title: 'Forcer le mat' },
            { title: 'Exercices chronométrés' }
          ]
        },
        {
          id: 20,
          title: 'Révision et évaluation Pion',
          subLessons: [
            { title: 'Quiz sur les règles' },
            { title: 'Exercices de mat en 1' },
            { title: 'Tactiques de base' },
            { title: 'Analyse de parties simples' },
            { title: 'Conseils pour progresser' }
          ]
        },
        {
          id: 81,
          title: 'Les pièges d\'ouverture classiques',
          subLessons: [
            { title: 'Le piège du berger' },
            { title: 'Le mat du lion' },
            { title: 'Pièges dans la partie italienne' },
            { title: 'Comment les éviter' }
          ]
        },
        {
          id: 82,
          title: 'L\'attaque double',
          subLessons: [
            { title: 'Définition et mécanisme' },
            { title: 'Attaque double du Cavalier' },
            { title: 'Attaque double de la Dame' },
            { title: 'Exercices pratiques' }
          ]
        },
        {
          id: 83,
          title: 'La défense des pièces',
          subLessons: [
            { title: 'Protéger ses pièces' },
            { title: 'La défense mutuelle' },
            { title: 'Pièces en prise' },
            { title: 'Vigilance tactique' }
          ]
        },
        {
          id: 84,
          title: 'Les échanges de pièces',
          subLessons: [
            { title: 'Quand échanger' },
            { title: 'Échange favorable' },
            { title: 'Éviter les mauvais échanges' },
            { title: 'Simplification' }
          ]
        },
        {
          id: 85,
          title: 'Le centre fort',
          subLessons: [
            { title: 'Importance du centre' },
            { title: 'Pions centraux' },
            { title: 'Pièces au centre' },
            { title: 'Contrôle du centre' }
          ]
        },
        {
          id: 86,
          title: 'La sécurité du Roi',
          subLessons: [
            { title: 'Garder le Roi protégé' },
            { title: 'Structure de pions devant le Roi' },
            { title: 'Dangers du Roi exposé' },
            { title: 'Quand ne pas roquer' }
          ]
        },
        {
          id: 87,
          title: 'Les finales de base',
          subLessons: [
            { title: 'Roi et pion vs Roi' },
            { title: 'Opposition simple' },
            { title: 'Pion sur la 7ème rangée' },
            { title: 'Exercices de finales simples' }
          ]
        },
        {
          id: 88,
          title: 'La coordination des pièces',
          subLessons: [
            { title: 'Faire travailler ses pièces ensemble' },
            { title: 'Pièces qui se soutiennent' },
            { title: 'Harmonie dans le jeu' },
            { title: 'Exemples pratiques' }
          ]
        },
        {
          id: 89,
          title: 'Les erreurs courantes',
          subLessons: [
            { title: 'Pièces en prise oubliées' },
            { title: 'Négliger le développement' },
            { title: 'Coups de pions inutiles' },
            { title: 'Comment les éviter' }
          ]
        },
        {
          id: 90,
          title: 'Exercices de consolidation Pion',
          subLessons: [
            { title: 'Révision des règles' },
            { title: 'Exercices tactiques de base' },
            { title: 'Problèmes de mat' },
            { title: 'Auto-évaluation' }
          ]
        }
      ]
    },
    {
      code: 'B',
      name: 'Cavalier - Intermédiaire',
      description: 'Approfondissement tactique et stratégique',
      color: '#2196F3',
      icon: '♘',
      courses: [
        {
          id: 21,
          title: 'Tactique : L\'attaque à la découverte',
          subLessons: [
            { title: 'Mécanisme de la découverte' },
            { title: 'Échec à la découverte' },
            { title: 'Double échec' },
            { title: 'Préparation de l\'attaque' },
            { title: 'Exercices pratiques' }
          ]
        },
        {
          id: 22,
          title: 'Tactique : Le sacrifice',
          subLessons: [
            { title: 'Sacrifice pour le mat' },
            { title: 'Sacrifice pour gain matériel' },
            { title: 'Sacrifice positionnel' },
            { title: 'Quand sacrifier ?' },
            { title: 'Exercices de sacrifice' }
          ]
        },
        {
          id: 23,
          title: 'Tactique : La déviation',
          subLessons: [
            { title: 'Dévier une pièce défenseur' },
            { title: 'Déviation du Roi' },
            { title: 'Déviation de la Dame' },
            { title: 'Combinaisons avec déviation' }
          ]
        },
        {
          id: 24,
          title: 'Tactique : L\'attraction',
          subLessons: [
            { title: 'Attirer le Roi' },
            { title: 'Attirer une pièce sur une case' },
            { title: 'Combinaisons d\'attraction' },
            { title: 'Exercices pratiques' }
          ]
        },
        {
          id: 25,
          title: 'Tactique : L\'élimination du défenseur',
          subLessons: [
            { title: 'Identifier le défenseur clé' },
            { title: 'Méthodes d\'élimination' },
            { title: 'Surcharge des pièces' },
            { title: 'Exercices d\'élimination' },
            { title: 'Combinaisons complexes' }
          ]
        },
        {
          id: 26,
          title: 'Tactique : L\'interférence',
          subLessons: [
            { title: 'Couper la communication' },
            { title: 'Bloquer une ligne' },
            { title: 'Interférence sur diagonale' },
            { title: 'Exercices d\'interférence' }
          ]
        },
        {
          id: 27,
          title: 'Combinaisons tactiques',
          subLessons: [
            { title: 'Enchaîner les thèmes tactiques' },
            { title: 'Calcul des variantes' },
            { title: 'Vérification des coups candidats' },
            { title: 'Exercices combinés' },
            { title: 'Analyse de parties' }
          ]
        },
        {
          id: 28,
          title: 'Les ouvertures : 1.e4 e5',
          subLessons: [
            { title: 'La Partie Italienne' },
            { title: 'Le Gambit du Roi' },
            { title: 'La Partie Écossaise' },
            { title: 'La Défense Petrov' },
            { title: 'Pièges d\'ouverture' }
          ]
        },
        {
          id: 29,
          title: 'Les ouvertures : 1.d4 d5',
          subLessons: [
            { title: 'Le Gambit Dame' },
            { title: 'La Défense Slave' },
            { title: 'La Défense Tarrasch' },
            { title: 'Plans typiques' }
          ]
        },
        {
          id: 30,
          title: 'Les ouvertures : Défense Sicilienne',
          subLessons: [
            { title: 'Introduction à la Sicilienne' },
            { title: 'Variante Najdorf' },
            { title: 'Variante Dragon' },
            { title: 'Variante Classique' },
            { title: 'Plans stratégiques' },
            { title: 'Pièges courants' }
          ]
        },
        {
          id: 31,
          title: 'Les ouvertures : Défense Française',
          subLessons: [
            { title: 'Structure de pions' },
            { title: 'Variante d\'avance' },
            { title: 'Variante d\'échange' },
            { title: 'Variante Winawer' },
            { title: 'Plans pour les deux camps' }
          ]
        },
        {
          id: 32,
          title: 'Les ouvertures : Défense Caro-Kann',
          subLessons: [
            { title: 'Idées principales' },
            { title: 'Variante classique' },
            { title: 'Variante d\'avance' },
            { title: 'Plans stratégiques' }
          ]
        },
        {
          id: 33,
          title: 'Stratégie : Les structures de pions',
          subLessons: [
            { title: 'Pions isolés' },
            { title: 'Pions doublés' },
            { title: 'Pions arriérés' },
            { title: 'Chaîne de pions' },
            { title: 'Majorité de pions' },
            { title: 'Plans selon la structure' }
          ]
        },
        {
          id: 34,
          title: 'Stratégie : Les colonnes ouvertes',
          subLessons: [
            { title: 'Contrôle d\'une colonne' },
            { title: 'Pénétration en 7ème rangée' },
            { title: 'Doubler les Tours' },
            { title: 'La colonne c ouverte' },
            { title: 'Exercices stratégiques' }
          ]
        },
        {
          id: 35,
          title: 'Stratégie : Les cases faibles',
          subLessons: [
            { title: 'Identifier les cases faibles' },
            { title: 'Occuper un avant-poste' },
            { title: 'Le trou' },
            { title: 'Exploiter les faiblesses adverses' }
          ]
        },
        {
          id: 36,
          title: 'Stratégie : Le centre',
          subLessons: [
            { title: 'Centre de pions classique' },
            { title: 'Centre fermé' },
            { title: 'Centre ouvert' },
            { title: 'Attaque du centre' },
            { title: 'Contrôle à distance' }
          ]
        },
        {
          id: 37,
          title: 'Finales de pions (1)',
          subLessons: [
            { title: 'L\'opposition' },
            { title: 'La règle du carré' },
            { title: 'Le Roi actif' },
            { title: 'Pion passé' },
            { title: 'Finales Roi + Pion vs Roi' }
          ]
        },
        {
          id: 38,
          title: 'Finales de pions (2)',
          subLessons: [
            { title: 'Le zugzwang' },
            { title: 'Triangulation' },
            { title: 'Percée de pions' },
            { title: 'Finales avec plusieurs pions' },
            { title: 'Exercices de finales' }
          ]
        },
        {
          id: 39,
          title: 'Finales de Tours (1)',
          subLessons: [
            { title: 'Position de Lucena' },
            { title: 'Position de Philidor' },
            { title: 'Tour active vs passive' },
            { title: 'Couper le Roi' }
          ]
        },
        {
          id: 40,
          title: 'Révision et évaluation Cavalier',
          subLessons: [
            { title: 'Test tactique avancé' },
            { title: 'Quiz sur les ouvertures' },
            { title: 'Exercices de stratégie' },
            { title: 'Finales essentielles' },
            { title: 'Analyse de parties modèles' },
            { title: 'Bilan et conseils' }
          ]
        },
        {
          id: 91,
          title: 'Tactique : Le rayon X',
          subLessons: [
            { title: 'Définition et mécanisme' },
            { title: 'Rayon X avec la Tour' },
            { title: 'Rayon X avec la Dame' },
            { title: 'Exercices pratiques' }
          ]
        },
        {
          id: 92,
          title: 'Tactique : Le moulin',
          subLessons: [
            { title: 'Le mécanisme du moulin' },
            { title: 'Échec à la découverte répété' },
            { title: 'Exemples célèbres' },
            { title: 'Exercices de moulin' }
          ]
        },
        {
          id: 93,
          title: 'Les ouvertures : Défenses indiennes',
          subLessons: [
            { title: 'Introduction aux défenses indiennes' },
            { title: 'Nimzo-Indienne : idées principales' },
            { title: 'Est-Indienne : plans stratégiques' },
            { title: 'Exercices de compréhension' }
          ]
        },
        {
          id: 94,
          title: 'Les ouvertures : Systèmes de Londres',
          subLessons: [
            { title: 'Structure du système de Londres' },
            { title: 'Plans pour les blancs' },
            { title: 'Comment contrer en tant que noirs' },
            { title: 'Parties modèles' }
          ]
        },
        {
          id: 95,
          title: 'Stratégie : Le pion passé',
          subLessons: [
            { title: 'Création d\'un pion passé' },
            { title: 'Le pion passé protégé' },
            { title: 'Avancer le pion passé' },
            { title: 'Bloquer le pion passé adverse' }
          ]
        },
        {
          id: 96,
          title: 'Stratégie : La majorité sur l\'aile',
          subLessons: [
            { title: 'Majorité de pions sur l\'aile dame' },
            { title: 'Majorité sur l\'aile roi' },
            { title: 'Exploiter la majorité' },
            { title: 'Exemples pratiques' }
          ]
        },
        {
          id: 97,
          title: 'Finales de Tours (2)',
          subLessons: [
            { title: 'Tour et 2 pions vs Tour et pion' },
            { title: 'Défense active' },
            { title: 'Technique de gain' },
            { title: 'Exercices chronométrés' }
          ]
        },
        {
          id: 98,
          title: 'Finales Fou contre Cavalier',
          subLessons: [
            { title: 'Quand le Fou est meilleur' },
            { title: 'Quand le Cavalier est meilleur' },
            { title: 'Positions ouvertes vs fermées' },
            { title: 'Exercices de jugement' }
          ]
        },
        {
          id: 99,
          title: 'Analyse de parties classiques',
          subLessons: [
            { title: 'Parties immortelles' },
            { title: 'Parties célèbres commentées' },
            { title: 'Leçons à tirer' },
            { title: 'Méthode d\'analyse' }
          ]
        },
        {
          id: 100,
          title: 'Exercices de consolidation Cavalier',
          subLessons: [
            { title: 'Test tactique intermédiaire' },
            { title: 'Quiz stratégique' },
            { title: 'Finales à résoudre' },
            { title: 'Auto-évaluation niveau B' }
          ]
        }
      ]
    },
    {
      code: 'C',
      name: 'Reine - Avancé',
      description: 'Maîtrise stratégique et préparation approfondie',
      color: '#9C27B0',
      icon: '♕',
      courses: [
        {
          id: 41,
          title: 'Tactique avancée : Calcul profond',
          subLessons: [
            { title: 'Calcul de variantes longues' },
            { title: 'Visualisation mentale' },
            { title: 'Méthode des coups candidats' },
            { title: 'Vérification et prophylaxie' },
            { title: 'Exercices chronométrés' }
          ]
        },
        {
          id: 42,
          title: 'Tactique avancée : Combinaisons complexes',
          subLessons: [
            { title: 'Combinaisons multi-thèmes' },
            { title: 'Sacrifices positionnels' },
            { title: 'Attaques sur le Roi roqué' },
            { title: 'Combinaisons défensives' },
            { title: 'Études tactiques célèbres' }
          ]
        },
        {
          id: 43,
          title: 'Attaque sur le Roi',
          subLessons: [
            { title: 'Conditions pour attaquer' },
            { title: 'Sacrifice sur h7/h2' },
            { title: 'Sacrifice sur g7/g2' },
            { title: 'Double sacrifice des Fous' },
            { title: 'Attaque avec pièces lourdes' },
            { title: 'Parties modèles' }
          ]
        },
        {
          id: 44,
          title: 'La défense active',
          subLessons: [
            { title: 'Défense par contre-attaque' },
            { title: 'Ressources défensives' },
            { title: 'Échec perpétuel' },
            { title: 'Forteresses' },
            { title: 'Sens du danger' }
          ]
        },
        {
          id: 45,
          title: 'Répertoire d\'ouvertures blancs (1)',
          subLessons: [
            { title: 'Choisir son répertoire' },
            { title: '1.e4 : système complet' },
            { title: 'Contre la Sicilienne' },
            { title: 'Contre la Française' },
            { title: 'Contre la Caro-Kann' },
            { title: 'Lignes critiques' }
          ]
        },
        {
          id: 46,
          title: 'Répertoire d\'ouvertures blancs (2)',
          subLessons: [
            { title: '1.d4 : système complet' },
            { title: 'Contre la Slave' },
            { title: 'Contre la Nimzo-Indienne' },
            { title: 'Contre l\'Est-Indienne' },
            { title: 'Variantes critiques' }
          ]
        },
        {
          id: 47,
          title: 'Répertoire d\'ouvertures noirs (1)',
          subLessons: [
            { title: 'Réponse à 1.e4' },
            { title: 'La Sicilienne en profondeur' },
            { title: 'Variantes principales' },
            { title: 'Préparation théorique' },
            { title: 'Nouveautés théoriques' }
          ]
        },
        {
          id: 48,
          title: 'Répertoire d\'ouvertures noirs (2)',
          subLessons: [
            { title: 'Réponse à 1.d4' },
            { title: 'Défense Nimzo-Indienne' },
            { title: 'Défense Est-Indienne' },
            { title: 'Plans typiques' },
            { title: 'Finales théoriques' }
          ]
        },
        {
          id: 49,
          title: 'Stratégie : La prophylaxie',
          subLessons: [
            { title: 'Penser aux plans adverses' },
            { title: 'Coups prophylactiques' },
            { title: 'Restriction des pièces adverses' },
            { title: 'Méthode de Nimzowitsch' }
          ]
        },
        {
          id: 50,
          title: 'Stratégie : L\'échange des pièces',
          subLessons: [
            { title: 'Quand échanger' },
            { title: 'Bons Fous vs mauvais Fous' },
            { title: 'Cavalier vs Fou' },
            { title: 'La qualité' },
            { title: 'Simplification vers les finales' }
          ]
        },
        {
          id: 51,
          title: 'Stratégie : Le jeu positionnel',
          subLessons: [
            { title: 'Amélioration des pièces' },
            { title: 'Création de faiblesses' },
            { title: 'Le plan à long terme' },
            { title: 'Manœuvres typiques' },
            { title: 'Étude de parties classiques' },
            { title: 'Karpov et le jeu positionnel' }
          ]
        },
        {
          id: 52,
          title: 'Stratégie : Les positions fermées',
          subLessons: [
            { title: 'Caractéristiques' },
            { title: 'Manœuvres de Cavaliers' },
            { title: 'Percées de pions' },
            { title: 'Jeu sur les deux ailes' },
            { title: 'Patience stratégique' }
          ]
        },
        {
          id: 53,
          title: 'Finales de Tours (2)',
          subLessons: [
            { title: 'Tour et pions vs Tour et pions' },
            { title: 'Technique de défense' },
            { title: 'Finales de Tour théoriques' },
            { title: 'Position de Vancura' },
            { title: 'Exercices pratiques' }
          ]
        },
        {
          id: 54,
          title: 'Finales de Fous',
          subLessons: [
            { title: 'Fou et pion vs Roi' },
            { title: 'Fous de même couleur' },
            { title: 'Fous de couleurs opposées' },
            { title: 'Fou vs Cavalier en finale' }
          ]
        },
        {
          id: 55,
          title: 'Finales de Cavaliers',
          subLessons: [
            { title: 'Cavalier et pion vs Roi' },
            { title: 'Finales de Cavaliers' },
            { title: 'Cavalier vs pions' },
            { title: 'Techniques avancées' }
          ]
        },
        {
          id: 56,
          title: 'Finales de Dames',
          subLessons: [
            { title: 'Dame et pion vs Dame' },
            { title: 'Échecs perpétuels' },
            { title: 'Technique de gain' },
            { title: 'Positions théoriques' },
            { title: 'Exercices pratiques' }
          ]
        },
        {
          id: 57,
          title: 'Analyse de parties de Grands Maîtres',
          subLessons: [
            { title: 'Méthode d\'analyse' },
            { title: 'Parties de Kasparov' },
            { title: 'Parties de Carlsen' },
            { title: 'Commentaire approfondi' },
            { title: 'Leçons à tirer' }
          ]
        },
        {
          id: 58,
          title: 'Préparation psychologique',
          subLessons: [
            { title: 'Gestion du temps' },
            { title: 'Concentration' },
            { title: 'Gestion du stress' },
            { title: 'Attitude face à l\'adversaire' }
          ]
        },
        {
          id: 59,
          title: 'Préparation aux tournois',
          subLessons: [
            { title: 'Préparer ses adversaires' },
            { title: 'Bases de données' },
            { title: 'Gestion de l\'énergie' },
            { title: 'Routine pré-partie' },
            { title: 'Analyse post-partie' }
          ]
        },
        {
          id: 60,
          title: 'Révision et évaluation Reine',
          subLessons: [
            { title: 'Test tactique avancé' },
            { title: 'Évaluation stratégique' },
            { title: 'Quiz d\'ouvertures' },
            { title: 'Finales complexes' },
            { title: 'Analyse de position' },
            { title: 'Bilan et orientation' }
          ]
        },
        {
          id: 101,
          title: 'Tactique : Sacrifices sur h7 et g7',
          subLessons: [
            { title: 'Le sacrifice classique Fxh7+' },
            { title: 'Conditions pour le sacrifice' },
            { title: 'Sacrifice sur g7' },
            { title: 'Exercices de sacrifice' }
          ]
        },
        {
          id: 102,
          title: 'Tactique : L\'attaque grecque',
          subLessons: [
            { title: 'Mécanisme de l\'attaque grecque' },
            { title: 'Sacrifice du Fou en h7' },
            { title: 'Suite avec Cg5+' },
            { title: 'Parties modèles' }
          ]
        },
        {
          id: 103,
          title: 'Répertoire d\'ouvertures : Lignes secondaires',
          subLessons: [
            { title: 'Variantes surprises' },
            { title: 'Lignes peu jouées mais solides' },
            { title: 'Avantages psychologiques' },
            { title: 'Préparation spécifique' }
          ]
        },
        {
          id: 104,
          title: 'Stratégie : La restriction',
          subLessons: [
            { title: 'Limiter les pièces adverses' },
            { title: 'Restriction du Cavalier' },
            { title: 'Restriction du Fou' },
            { title: 'Jeu prophylactique' }
          ]
        },
        {
          id: 105,
          title: 'Stratégie : Les faiblesses chroniques',
          subLessons: [
            { title: 'Identifier les faiblesses permanentes' },
            { title: 'Pions faibles' },
            { title: 'Cases faibles' },
            { title: 'Exploitation à long terme' }
          ]
        },
        {
          id: 106,
          title: 'Finales théoriques avancées',
          subLessons: [
            { title: 'Finales de pions complexes' },
            { title: 'Finales avec pièces mineures' },
            { title: 'Positions critiques' },
            { title: 'Étude approfondie' }
          ]
        },
        {
          id: 107,
          title: 'Analyse de parties de Karpov',
          subLessons: [
            { title: 'Le style positionnel de Karpov' },
            { title: 'La technique d\'étouffement' },
            { title: 'Parties commentées' },
            { title: 'Leçons stratégiques' }
          ]
        },
        {
          id: 108,
          title: 'Analyse de parties de Fischer',
          subLessons: [
            { title: 'La précision de Fischer' },
            { title: 'Ouvertures de Fischer' },
            { title: 'Parties immortelles' },
            { title: 'Leçons tactiques et stratégiques' }
          ]
        },
        {
          id: 109,
          title: 'Gestion du temps en partie',
          subLessons: [
            { title: 'Répartition du temps' },
            { title: 'Moments critiques' },
            { title: 'Éviter le zeitnot' },
            { title: 'Prise de décision rapide' }
          ]
        },
        {
          id: 110,
          title: 'Exercices de consolidation Reine',
          subLessons: [
            { title: 'Test tactique avancé' },
            { title: 'Évaluation stratégique' },
            { title: 'Finales à résoudre' },
            { title: 'Auto-évaluation niveau C' }
          ]
        }
      ]
    },
    {
      code: 'D',
      name: 'Roi - Expert',
      description: 'Perfectionnement et préparation professionnelle',
      color: '#FF9800',
      icon: '♔',
      courses: [
        {
          id: 61,
          title: 'Calcul expert et visualisation',
          subLessons: [
            { title: 'Calcul à l\'aveugle' },
            { title: 'Variantes forcées longues' },
            { title: 'Intuition vs calcul' },
            { title: 'Méthode de vérification' },
            { title: 'Exercices de haut niveau' }
          ]
        },
        {
          id: 62,
          title: 'Tactique : Études artistiques',
          subLessons: [
            { title: 'Les grands compositeurs' },
            { title: 'Études de mat' },
            { title: 'Études de nulle' },
            { title: 'Études complexes' },
            { title: 'Créativité tactique' }
          ]
        },
        {
          id: 63,
          title: 'L\'initiative et le temps',
          subLessons: [
            { title: 'Gagner des temps' },
            { title: 'Sacrifier pour l\'initiative' },
            { title: 'Maintenir la pression' },
            { title: 'Convertir l\'initiative en avantage' }
          ]
        },
        {
          id: 64,
          title: 'Les déséquilibres',
          subLessons: [
            { title: 'Structure de pions vs développement' },
            { title: 'Matériel vs initiative' },
            { title: 'Espace vs pièces actives' },
            { title: 'Évaluation dynamique' },
            { title: 'Prise de décision' }
          ]
        },
        {
          id: 65,
          title: 'Théorie d\'ouvertures : Lignes critiques',
          subLessons: [
            { title: 'Analyse des lignes principales' },
            { title: 'Nouveautés théoriques' },
            { title: 'Préparation contre un adversaire' },
            { title: 'Mémorisation efficace' },
            { title: 'Utilisation des engines' }
          ]
        },
        {
          id: 66,
          title: 'Théorie d\'ouvertures : Systèmes anti-mainline',
          subLessons: [
            { title: 'Surprise opening' },
            { title: 'Répertoire secondaire' },
            { title: 'Gambits dangereux' },
            { title: 'Lignes de surprise' }
          ]
        },
        {
          id: 67,
          title: 'Le milieu de jeu complexe',
          subLessons: [
            { title: 'Positions critiques' },
            { title: 'Évaluation des positions complexes' },
            { title: 'Plans dans des positions inconnues' },
            { title: 'Gestion de l\'incertitude' },
            { title: 'Parties décisives' }
          ]
        },
        {
          id: 68,
          title: 'Technique de conversion',
          subLessons: [
            { title: 'Convertir un avantage matériel' },
            { title: 'Convertir un avantage positionnel' },
            { title: 'Simplification technique' },
            { title: 'Éviter les pièges' }
          ]
        },
        {
          id: 69,
          title: 'Défense dans les positions difficiles',
          subLessons: [
            { title: 'Ressources cachées' },
            { title: 'Défense active' },
            { title: 'Contre-jeu pratique' },
            { title: 'Psychologie défensive' },
            { title: 'Exemples célèbres' }
          ]
        },
        {
          id: 70,
          title: 'Finales complexes (1)',
          subLessons: [
            { title: 'Tour et Fou vs Tour' },
            { title: 'Tour et Cavalier vs Tour' },
            { title: 'Deux Tours vs Dame' },
            { title: 'Finales théoriques avancées' }
          ]
        },
        {
          id: 71,
          title: 'Finales complexes (2)',
          subLessons: [
            { title: 'Dame vs deux pièces' },
            { title: 'Trois pièces vs Dame' },
            { title: 'Finales de pièces lourdes' },
            { title: 'Positions exceptionnelles' },
            { title: 'Étude approfondie' }
          ]
        },
        {
          id: 72,
          title: 'Analyse avec moteur',
          subLessons: [
            { title: 'Utilisation efficace de Stockfish' },
            { title: 'Interprétation des évaluations' },
            { title: 'Quand faire confiance au moteur' },
            { title: 'Analyse critique' },
            { title: 'Améliorer son jeu avec les engines' }
          ]
        },
        {
          id: 73,
          title: 'Préparation spécifique aux adversaires',
          subLessons: [
            { title: 'Étude du répertoire adverse' },
            { title: 'Identifier les faiblesses' },
            { title: 'Préparer des surprises' },
            { title: 'Adaptation du style' }
          ]
        },
        {
          id: 74,
          title: 'Gestion du temps en compétition',
          subLessons: [
            { title: 'Cadences rapides' },
            { title: 'Zeitnot et prise de décision' },
            { title: 'Increment et gestion' },
            { title: 'Exercices en temps réel' }
          ]
        },
        {
          id: 75,
          title: 'Psychologie avancée',
          subLessons: [
            { title: 'Lecture de l\'adversaire' },
            { title: 'Gérer la pression' },
            { title: 'Résilience mentale' },
            { title: 'Pic de performance' },
            { title: 'Récupération entre parties' }
          ]
        },
        {
          id: 76,
          title: 'Style de jeu personnel',
          subLessons: [
            { title: 'Identifier son style' },
            { title: 'Renforcer ses points forts' },
            { title: 'Travailler ses faiblesses' },
            { title: 'Adaptation situationnelle' }
          ]
        },
        {
          id: 77,
          title: 'Étude des champions du monde',
          subLessons: [
            { title: 'Steinitz et les principes modernes' },
            { title: 'Alekhine et l\'attaque' },
            { title: 'Capablanca et la technique' },
            { title: 'Fischer et la précision' },
            { title: 'Kasparov et la dynamique' },
            { title: 'Carlsen et l\'universalité' }
          ]
        },
        {
          id: 78,
          title: 'Entraînement intensif',
          subLessons: [
            { title: 'Programme d\'entraînement' },
            { title: 'Exercices quotidiens' },
            { title: 'Analyse régulière' },
            { title: 'Objectifs mesurables' },
            { title: 'Suivi des progrès' }
          ]
        },
        {
          id: 79,
          title: 'Préparation aux normes de titre',
          subLessons: [
            { title: 'Comprendre le système Elo' },
            { title: 'Normes FM, IM, GM' },
            { title: 'Sélection des tournois' },
            { title: 'Stratégie de performance' }
          ]
        },
        {
          id: 80,
          title: 'Révision et évaluation Roi',
          subLessons: [
            { title: 'Test tactique expert' },
            { title: 'Évaluation stratégique avancée' },
            { title: 'Finales théoriques' },
            { title: 'Analyse complète de partie' },
            { title: 'Plan de progression personnalisé' },
            { title: 'Certification du niveau' }
          ]
        },
        {
          id: 111,
          title: 'Calcul à longue portée',
          subLessons: [
            { title: 'Calcul de 10+ coups' },
            { title: 'Visualisation avancée' },
            { title: 'Vérification systématique' },
            { title: 'Exercices de haut niveau' }
          ]
        },
        {
          id: 112,
          title: 'Intuition et jugement positionnel',
          subLessons: [
            { title: 'Développer son intuition' },
            { title: 'Jugement sans calcul' },
            { title: 'Sens positionnel' },
            { title: 'Exercices d\'évaluation' }
          ]
        },
        {
          id: 113,
          title: 'Préparation d\'ouverture spécifique',
          subLessons: [
            { title: 'Étude approfondie d\'une ligne' },
            { title: 'Mémorisation efficace' },
            { title: 'Nouveautés à préparer' },
            { title: 'Fichiers de préparation' }
          ]
        },
        {
          id: 114,
          title: 'Les transformations de pions',
          subLessons: [
            { title: 'Changer la structure' },
            { title: 'Ouverture vs fermeture du centre' },
            { title: 'Percées de pions' },
            { title: 'Timing des transformations' }
          ]
        },
        {
          id: 115,
          title: 'Finales de pièces lourdes',
          subLessons: [
            { title: 'Dame et Tour vs Dame et Tour' },
            { title: 'Technique de conversion' },
            { title: 'Positions théoriques' },
            { title: 'Exercices pratiques' }
          ]
        },
        {
          id: 116,
          title: 'Analyse de parties de Carlsen',
          subLessons: [
            { title: 'Le style universel de Carlsen' },
            { title: 'Technique en finale' },
            { title: 'Parties commentées' },
            { title: 'Leçons à retenir' }
          ]
        },
        {
          id: 117,
          title: 'L\'art de la défense',
          subLessons: [
            { title: 'Défense passive vs active' },
            { title: 'Ressources défensives' },
            { title: 'Sauver des positions perdues' },
            { title: 'Psychologie défensive' }
          ]
        },
        {
          id: 118,
          title: 'Psychologie en compétition',
          subLessons: [
            { title: 'Gérer la pression' },
            { title: 'Concentration maximale' },
            { title: 'Récupération entre parties' },
            { title: 'Mentalité de champion' }
          ]
        },
        {
          id: 119,
          title: 'Stratégies pour gagner des points Elo',
          subLessons: [
            { title: 'Choix des tournois' },
            { title: 'Gestion des adversaires' },
            { title: 'Optimisation de la performance' },
            { title: 'Progression constante' }
          ]
        },
        {
          id: 120,
          title: 'Examen final Roi',
          subLessons: [
            { title: 'Test tactique expert' },
            { title: 'Évaluation stratégique complète' },
            { title: 'Finales complexes' },
            { title: 'Analyse de partie complète' },
            { title: 'Certification Expert' }
          ]
        }
      ]
    }
  ];

  toggleLevel(code: string): void {
    this.expandedLevels.update(levels => {
      const newLevels = new Set(levels);
      if (newLevels.has(code)) {
        newLevels.delete(code);
      } else {
        newLevels.add(code);
      }
      return newLevels;
    });
  }

  toggleCourse(courseId: number): void {
    this.expandedCourses.update(courses => {
      const newCourses = new Set(courses);
      if (newCourses.has(courseId)) {
        newCourses.delete(courseId);
      } else {
        newCourses.add(courseId);
      }
      return newCourses;
    });
  }

  isLevelExpanded(code: string): boolean {
    return this.expandedLevels().has(code);
  }

  isCourseExpanded(courseId: number): boolean {
    return this.expandedCourses().has(courseId);
  }

  getTotalCourses(): number {
    return this.levels.reduce((sum, level) => sum + level.courses.length, 0);
  }

  getTotalSubLessons(): number {
    return this.levels.reduce((sum, level) =>
      sum + level.courses.reduce((s, c) => s + c.subLessons.length, 0), 0);
  }
}
