import { Component, signal, ChangeDetectionStrategy, inject, OnInit, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
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
  imports: [CommonModule, NgIconComponent],
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
        this.toastService.success(`Cours de depart defini : ${course.title}`);
      },
      error: () => this.toastService.error('Erreur lors de la selection du cours')
    });
  }

  goBackToPreviousCourse(): void {
    if (!this.authService.isStudent()) return;

    this.programmeService.goBackToPreviousCourse().subscribe({
      next: (course) => {
        this.currentCourseId.set(course.id);
        this.toastService.success(`Retour au cours : ${course.title}`);
      },
      error: () => this.toastService.error('Erreur lors du retour au cours precedent')
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

  getCurrentLevelCode(): string {
    const currentId = this.currentCourseId();
    for (const level of this.levels) {
      if (level.courses.some(c => c.id === currentId)) {
        return level.code;
      }
    }
    return 'A';
  }

  levels: Level[] = [
    {
      code: 'A',
      name: 'Niveau A - Debutant',
      description: 'Les fondamentaux des echecs pour bien commencer',
      color: '#4CAF50',
      icon: 'heroBookOpen',
      courses: [
        {
          id: 1,
          title: 'L\'echiquier et les pieces',
          subLessons: [
            { title: 'Presentation de l\'echiquier (colonnes, rangees, diagonales)' },
            { title: 'Les cases blanches et noires' },
            { title: 'La notation algebrique' },
            { title: 'Placement initial des pieces' },
            { title: 'Valeur relative des pieces' }
          ]
        },
        {
          id: 2,
          title: 'Le deplacement du Pion',
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
          title: 'Le deplacement de la Tour',
          subLessons: [
            { title: 'Mouvement horizontal' },
            { title: 'Mouvement vertical' },
            { title: 'La Tour et les colonnes ouvertes' },
            { title: 'Coordination de deux Tours' }
          ]
        },
        {
          id: 4,
          title: 'Le deplacement du Fou',
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
          title: 'Le deplacement de la Dame',
          subLessons: [
            { title: 'Combinaison Tour + Fou' },
            { title: 'Puissance de la Dame' },
            { title: 'Dangers de la Dame exposee' },
            { title: 'La Dame en attaque' }
          ]
        },
        {
          id: 6,
          title: 'Le deplacement du Cavalier',
          subLessons: [
            { title: 'Le mouvement en L' },
            { title: 'Le saut par-dessus les pieces' },
            { title: 'Les cases accessibles' },
            { title: 'Le Cavalier au centre vs au bord' },
            { title: 'La fourche du Cavalier' },
            { title: 'Exercices de parcours' }
          ]
        },
        {
          id: 7,
          title: 'Le deplacement du Roi',
          subLessons: [
            { title: 'Mouvement d\'une case' },
            { title: 'Le Roi ne peut pas se mettre en echec' },
            { title: 'L\'importance de proteger le Roi' },
            { title: 'Le Roi actif en finale' }
          ]
        },
        {
          id: 8,
          title: 'L\'echec et l\'echec et mat',
          subLessons: [
            { title: 'Definition de l\'echec' },
            { title: 'Les trois facons de parer un echec' },
            { title: 'L\'echec et mat : fin de partie' },
            { title: 'Mats simples avec Dame' },
            { title: 'Mats simples avec Tour' }
          ]
        },
        {
          id: 9,
          title: 'Le Roque',
          subLessons: [
            { title: 'Le petit roque (cote Roi)' },
            { title: 'Le grand roque (cote Dame)' },
            { title: 'Conditions du roque' },
            { title: 'Quand roquer ?' },
            { title: 'Erreurs courantes' }
          ]
        },
        {
          id: 10,
          title: 'Le Pat et les nulles',
          subLessons: [
            { title: 'Definition du Pat' },
            { title: 'Nulle par accord mutuel' },
            { title: 'Nulle par repetition de coups' },
            { title: 'Regle des 50 coups' },
            { title: 'Materiel insuffisant' }
          ]
        },
        {
          id: 11,
          title: 'Principes d\'ouverture (1)',
          subLessons: [
            { title: 'Controler le centre' },
            { title: 'Developper les pieces mineures' },
            { title: 'Ne pas sortir la Dame trop tot' },
            { title: 'Roquer rapidement' }
          ]
        },
        {
          id: 12,
          title: 'Principes d\'ouverture (2)',
          subLessons: [
            { title: 'Ne pas jouer la meme piece deux fois' },
            { title: 'Connecter les Tours' },
            { title: 'Eviter les pions faibles' },
            { title: 'L\'importance du developpement' },
            { title: 'Exemples pratiques' }
          ]
        },
        {
          id: 13,
          title: 'Les mats elementaires',
          subLessons: [
            { title: 'Mat du couloir' },
            { title: 'Mat du baiser' },
            { title: 'Mat a l\'etouffee' },
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
            { title: 'Reperer les opportunites' }
          ]
        },
        {
          id: 15,
          title: 'La fourchette',
          subLessons: [
            { title: 'Fourchette de Cavalier' },
            { title: 'Fourchette de Pion' },
            { title: 'Fourchette de Dame' },
            { title: 'Reconnaitre les positions de fourchette' },
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
            { title: 'Se liberer d\'un clouage' }
          ]
        },
        {
          id: 17,
          title: 'L\'enfilade',
          subLessons: [
            { title: 'Definition et mecanisme' },
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
            { title: 'Eviter le Pat' },
            { title: 'Methode systematique' }
          ]
        },
        {
          id: 19,
          title: 'Finale Roi + Tour vs Roi',
          subLessons: [
            { title: 'La methode de l\'escalier' },
            { title: 'L\'opposition' },
            { title: 'Forcer le mat' },
            { title: 'Exercices chronometres' }
          ]
        },
        {
          id: 20,
          title: 'Revision et evaluation Niveau A',
          subLessons: [
            { title: 'Quiz sur les regles' },
            { title: 'Exercices de mat en 1' },
            { title: 'Tactiques de base' },
            { title: 'Analyse de parties simples' },
            { title: 'Conseils pour progresser' }
          ]
        },
        {
          id: 81,
          title: 'Les pieges d\'ouverture classiques',
          subLessons: [
            { title: 'Le piege du berger' },
            { title: 'Le mat du lion' },
            { title: 'Pieges dans la partie italienne' },
            { title: 'Comment les eviter' }
          ]
        },
        {
          id: 82,
          title: 'L\'attaque double',
          subLessons: [
            { title: 'Definition et mecanisme' },
            { title: 'Attaque double du Cavalier' },
            { title: 'Attaque double de la Dame' },
            { title: 'Exercices pratiques' }
          ]
        },
        {
          id: 83,
          title: 'La defense des pieces',
          subLessons: [
            { title: 'Proteger ses pieces' },
            { title: 'La defense mutuelle' },
            { title: 'Pieces en prise' },
            { title: 'Vigilance tactique' }
          ]
        },
        {
          id: 84,
          title: 'Les echanges de pieces',
          subLessons: [
            { title: 'Quand echanger' },
            { title: 'Echange favorable' },
            { title: 'Eviter les mauvais echanges' },
            { title: 'Simplification' }
          ]
        },
        {
          id: 85,
          title: 'Le centre fort',
          subLessons: [
            { title: 'Importance du centre' },
            { title: 'Pions centraux' },
            { title: 'Pieces au centre' },
            { title: 'Controle du centre' }
          ]
        },
        {
          id: 86,
          title: 'La securite du Roi',
          subLessons: [
            { title: 'Garder le Roi protege' },
            { title: 'Structure de pions devant le Roi' },
            { title: 'Dangers du Roi expose' },
            { title: 'Quand ne pas roquer' }
          ]
        },
        {
          id: 87,
          title: 'Les finales de base',
          subLessons: [
            { title: 'Roi et pion vs Roi' },
            { title: 'Opposition simple' },
            { title: 'Pion sur la 7eme rangee' },
            { title: 'Exercices de finales simples' }
          ]
        },
        {
          id: 88,
          title: 'La coordination des pieces',
          subLessons: [
            { title: 'Faire travailler ses pieces ensemble' },
            { title: 'Pieces qui se soutiennent' },
            { title: 'Harmonie dans le jeu' },
            { title: 'Exemples pratiques' }
          ]
        },
        {
          id: 89,
          title: 'Les erreurs courantes',
          subLessons: [
            { title: 'Pieces en prise oubliees' },
            { title: 'Negliger le developpement' },
            { title: 'Coups de pions inutiles' },
            { title: 'Comment les eviter' }
          ]
        },
        {
          id: 90,
          title: 'Exercices de consolidation Niveau A',
          subLessons: [
            { title: 'Revision des regles' },
            { title: 'Exercices tactiques de base' },
            { title: 'Problemes de mat' },
            { title: 'Auto-evaluation' }
          ]
        }
      ]
    },
    {
      code: 'B',
      name: 'Niveau B - Intermediaire',
      description: 'Approfondissement tactique et strategique',
      color: '#2196F3',
      icon: 'heroAcademicCap',
      courses: [
        {
          id: 21,
          title: 'Tactique : L\'attaque a la decouverte',
          subLessons: [
            { title: 'Mecanisme de la decouverte' },
            { title: 'Echec a la decouverte' },
            { title: 'Double echec' },
            { title: 'Preparation de l\'attaque' },
            { title: 'Exercices pratiques' }
          ]
        },
        {
          id: 22,
          title: 'Tactique : Le sacrifice',
          subLessons: [
            { title: 'Sacrifice pour le mat' },
            { title: 'Sacrifice pour gain materiel' },
            { title: 'Sacrifice positionnel' },
            { title: 'Quand sacrifier ?' },
            { title: 'Exercices de sacrifice' }
          ]
        },
        {
          id: 23,
          title: 'Tactique : La deviation',
          subLessons: [
            { title: 'Devier une piece defenseur' },
            { title: 'Deviation du Roi' },
            { title: 'Deviation de la Dame' },
            { title: 'Combinaisons avec deviation' }
          ]
        },
        {
          id: 24,
          title: 'Tactique : L\'attraction',
          subLessons: [
            { title: 'Attirer le Roi' },
            { title: 'Attirer une piece sur une case' },
            { title: 'Combinaisons d\'attraction' },
            { title: 'Exercices pratiques' }
          ]
        },
        {
          id: 25,
          title: 'Tactique : L\'elimination du defenseur',
          subLessons: [
            { title: 'Identifier le defenseur cle' },
            { title: 'Methodes d\'elimination' },
            { title: 'Surcharge des pieces' },
            { title: 'Exercices d\'elimination' },
            { title: 'Combinaisons complexes' }
          ]
        },
        {
          id: 26,
          title: 'Tactique : L\'interference',
          subLessons: [
            { title: 'Couper la communication' },
            { title: 'Bloquer une ligne' },
            { title: 'Interference sur diagonale' },
            { title: 'Exercices d\'interference' }
          ]
        },
        {
          id: 27,
          title: 'Combinaisons tactiques',
          subLessons: [
            { title: 'Enchainer les themes tactiques' },
            { title: 'Calcul des variantes' },
            { title: 'Verification des coups candidats' },
            { title: 'Exercices combines' },
            { title: 'Analyse de parties' }
          ]
        },
        {
          id: 28,
          title: 'Les ouvertures : 1.e4 e5',
          subLessons: [
            { title: 'La Partie Italienne' },
            { title: 'Le Gambit du Roi' },
            { title: 'La Partie Ecossaise' },
            { title: 'La Defense Petrov' },
            { title: 'Pieges d\'ouverture' }
          ]
        },
        {
          id: 29,
          title: 'Les ouvertures : 1.d4 d5',
          subLessons: [
            { title: 'Le Gambit Dame' },
            { title: 'La Defense Slave' },
            { title: 'La Defense Tarrasch' },
            { title: 'Plans typiques' }
          ]
        },
        {
          id: 30,
          title: 'Les ouvertures : Defense Sicilienne',
          subLessons: [
            { title: 'Introduction a la Sicilienne' },
            { title: 'Variante Najdorf' },
            { title: 'Variante Dragon' },
            { title: 'Variante Classique' },
            { title: 'Plans strategiques' },
            { title: 'Pieges courants' }
          ]
        },
        {
          id: 31,
          title: 'Les ouvertures : Defense Francaise',
          subLessons: [
            { title: 'Structure de pions' },
            { title: 'Variante d\'avance' },
            { title: 'Variante d\'echange' },
            { title: 'Variante Winawer' },
            { title: 'Plans pour les deux camps' }
          ]
        },
        {
          id: 32,
          title: 'Les ouvertures : Defense Caro-Kann',
          subLessons: [
            { title: 'Idees principales' },
            { title: 'Variante classique' },
            { title: 'Variante d\'avance' },
            { title: 'Plans strategiques' }
          ]
        },
        {
          id: 33,
          title: 'Strategie : Les structures de pions',
          subLessons: [
            { title: 'Pions isoles' },
            { title: 'Pions doubles' },
            { title: 'Pions arrieres' },
            { title: 'Chaine de pions' },
            { title: 'Majorite de pions' },
            { title: 'Plans selon la structure' }
          ]
        },
        {
          id: 34,
          title: 'Strategie : Les colonnes ouvertes',
          subLessons: [
            { title: 'Controle d\'une colonne' },
            { title: 'Penetration en 7eme rangee' },
            { title: 'Doubler les Tours' },
            { title: 'La colonne c ouverte' },
            { title: 'Exercices strategiques' }
          ]
        },
        {
          id: 35,
          title: 'Strategie : Les cases faibles',
          subLessons: [
            { title: 'Identifier les cases faibles' },
            { title: 'Occuper un avant-poste' },
            { title: 'Le trou' },
            { title: 'Exploiter les faiblesses adverses' }
          ]
        },
        {
          id: 36,
          title: 'Strategie : Le centre',
          subLessons: [
            { title: 'Centre de pions classique' },
            { title: 'Centre ferme' },
            { title: 'Centre ouvert' },
            { title: 'Attaque du centre' },
            { title: 'Controle a distance' }
          ]
        },
        {
          id: 37,
          title: 'Finales de pions (1)',
          subLessons: [
            { title: 'L\'opposition' },
            { title: 'La regle du carre' },
            { title: 'Le Roi actif' },
            { title: 'Pion passe' },
            { title: 'Finales Roi + Pion vs Roi' }
          ]
        },
        {
          id: 38,
          title: 'Finales de pions (2)',
          subLessons: [
            { title: 'Le zugzwang' },
            { title: 'Triangulation' },
            { title: 'Percee de pions' },
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
          title: 'Revision et evaluation Niveau B',
          subLessons: [
            { title: 'Test tactique avance' },
            { title: 'Quiz sur les ouvertures' },
            { title: 'Exercices de strategie' },
            { title: 'Finales essentielles' },
            { title: 'Analyse de parties modeles' },
            { title: 'Bilan et conseils' }
          ]
        },
        {
          id: 91,
          title: 'Tactique : Le rayon X',
          subLessons: [
            { title: 'Definition et mecanisme' },
            { title: 'Rayon X avec la Tour' },
            { title: 'Rayon X avec la Dame' },
            { title: 'Exercices pratiques' }
          ]
        },
        {
          id: 92,
          title: 'Tactique : Le moulin',
          subLessons: [
            { title: 'Le mecanisme du moulin' },
            { title: 'Echec a la decouverte repete' },
            { title: 'Exemples celebres' },
            { title: 'Exercices de moulin' }
          ]
        },
        {
          id: 93,
          title: 'Les ouvertures : Defenses indiennes',
          subLessons: [
            { title: 'Introduction aux defenses indiennes' },
            { title: 'Nimzo-Indienne : idees principales' },
            { title: 'Est-Indienne : plans strategiques' },
            { title: 'Exercices de comprehension' }
          ]
        },
        {
          id: 94,
          title: 'Les ouvertures : Systemes de Londres',
          subLessons: [
            { title: 'Structure du systeme de Londres' },
            { title: 'Plans pour les blancs' },
            { title: 'Comment contrer en tant que noirs' },
            { title: 'Parties modeles' }
          ]
        },
        {
          id: 95,
          title: 'Strategie : Le pion passe',
          subLessons: [
            { title: 'Creation d\'un pion passe' },
            { title: 'Le pion passe protege' },
            { title: 'Avancer le pion passe' },
            { title: 'Bloquer le pion passe adverse' }
          ]
        },
        {
          id: 96,
          title: 'Strategie : La majorite sur l\'aile',
          subLessons: [
            { title: 'Majorite de pions sur l\'aile dame' },
            { title: 'Majorite sur l\'aile roi' },
            { title: 'Exploiter la majorite' },
            { title: 'Exemples pratiques' }
          ]
        },
        {
          id: 97,
          title: 'Finales de Tours (2)',
          subLessons: [
            { title: 'Tour et 2 pions vs Tour et pion' },
            { title: 'Defense active' },
            { title: 'Technique de gain' },
            { title: 'Exercices chronometres' }
          ]
        },
        {
          id: 98,
          title: 'Finales Fou contre Cavalier',
          subLessons: [
            { title: 'Quand le Fou est meilleur' },
            { title: 'Quand le Cavalier est meilleur' },
            { title: 'Positions ouvertes vs fermees' },
            { title: 'Exercices de jugement' }
          ]
        },
        {
          id: 99,
          title: 'Analyse de parties classiques',
          subLessons: [
            { title: 'Parties immortelles' },
            { title: 'Parties celebres commentees' },
            { title: 'Lecons a tirer' },
            { title: 'Methode d\'analyse' }
          ]
        },
        {
          id: 100,
          title: 'Exercices de consolidation Niveau B',
          subLessons: [
            { title: 'Test tactique intermediaire' },
            { title: 'Quiz strategique' },
            { title: 'Finales a resoudre' },
            { title: 'Auto-evaluation niveau B' }
          ]
        }
      ]
    },
    {
      code: 'C',
      name: 'Niveau C - Avance',
      description: 'Maitrise strategique et preparation approfondie',
      color: '#9C27B0',
      icon: 'heroTrophy',
      courses: [
        {
          id: 41,
          title: 'Tactique avancee : Calcul profond',
          subLessons: [
            { title: 'Calcul de variantes longues' },
            { title: 'Visualisation mentale' },
            { title: 'Methode des coups candidats' },
            { title: 'Verification et prophylaxie' },
            { title: 'Exercices chronometres' }
          ]
        },
        {
          id: 42,
          title: 'Tactique avancee : Combinaisons complexes',
          subLessons: [
            { title: 'Combinaisons multi-themes' },
            { title: 'Sacrifices positionels' },
            { title: 'Attaques sur le Roi roque' },
            { title: 'Combinaisons defensives' },
            { title: 'Etudes tactiques celebres' }
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
            { title: 'Attaque avec pieces lourdes' },
            { title: 'Parties modeles' }
          ]
        },
        {
          id: 44,
          title: 'La defense active',
          subLessons: [
            { title: 'Defense par contre-attaque' },
            { title: 'Ressources defensives' },
            { title: 'Echec perpetuel' },
            { title: 'Forteresses' },
            { title: 'Sens du danger' }
          ]
        },
        {
          id: 45,
          title: 'Repertoire d\'ouvertures blancs (1)',
          subLessons: [
            { title: 'Choisir son repertoire' },
            { title: '1.e4 : systeme complet' },
            { title: 'Contre la Sicilienne' },
            { title: 'Contre la Francaise' },
            { title: 'Contre la Caro-Kann' },
            { title: 'Lignes critiques' }
          ]
        },
        {
          id: 46,
          title: 'Repertoire d\'ouvertures blancs (2)',
          subLessons: [
            { title: '1.d4 : systeme complet' },
            { title: 'Contre la Slave' },
            { title: 'Contre la Nimzo-Indienne' },
            { title: 'Contre l\'Est-Indienne' },
            { title: 'Variantes critiques' }
          ]
        },
        {
          id: 47,
          title: 'Repertoire d\'ouvertures noirs (1)',
          subLessons: [
            { title: 'Reponse a 1.e4' },
            { title: 'La Sicilienne en profondeur' },
            { title: 'Variantes principales' },
            { title: 'Preparation theorique' },
            { title: 'Nouveautes theoriques' }
          ]
        },
        {
          id: 48,
          title: 'Repertoire d\'ouvertures noirs (2)',
          subLessons: [
            { title: 'Reponse a 1.d4' },
            { title: 'Defense Nimzo-Indienne' },
            { title: 'Defense Est-Indienne' },
            { title: 'Plans typiques' },
            { title: 'Finales theoriques' }
          ]
        },
        {
          id: 49,
          title: 'Strategie : La prophylaxie',
          subLessons: [
            { title: 'Penser aux plans adverses' },
            { title: 'Coups prophylactiques' },
            { title: 'Restriction des pieces adverses' },
            { title: 'Methode de Nimzowitsch' }
          ]
        },
        {
          id: 50,
          title: 'Strategie : L\'echange des pieces',
          subLessons: [
            { title: 'Quand echanger' },
            { title: 'Bons Fous vs mauvais Fous' },
            { title: 'Cavalier vs Fou' },
            { title: 'La qualite' },
            { title: 'Simplification vers les finales' }
          ]
        },
        {
          id: 51,
          title: 'Strategie : Le jeu positionnel',
          subLessons: [
            { title: 'Amelioration des pieces' },
            { title: 'Creation de faiblesses' },
            { title: 'Le plan a long terme' },
            { title: 'Maneuvres typiques' },
            { title: 'Etude de parties classiques' },
            { title: 'Karpov et le jeu positionnel' }
          ]
        },
        {
          id: 52,
          title: 'Strategie : Les positions fermees',
          subLessons: [
            { title: 'Caracteristiques' },
            { title: 'Maneuvres de Cavaliers' },
            { title: 'Percees de pions' },
            { title: 'Jeu sur les deux ailes' },
            { title: 'Patience strategique' }
          ]
        },
        {
          id: 53,
          title: 'Finales de Tours (2)',
          subLessons: [
            { title: 'Tour et pions vs Tour et pions' },
            { title: 'Technique de defense' },
            { title: 'Finales de Tour theoriques' },
            { title: 'Position de Vancura' },
            { title: 'Exercices pratiques' }
          ]
        },
        {
          id: 54,
          title: 'Finales de Fous',
          subLessons: [
            { title: 'Fou et pion vs Roi' },
            { title: 'Fous de meme couleur' },
            { title: 'Fous de couleurs opposees' },
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
            { title: 'Techniques avancees' }
          ]
        },
        {
          id: 56,
          title: 'Finales de Dames',
          subLessons: [
            { title: 'Dame et pion vs Dame' },
            { title: 'Echecs perpetuels' },
            { title: 'Technique de gain' },
            { title: 'Positions theoriques' },
            { title: 'Exercices pratiques' }
          ]
        },
        {
          id: 57,
          title: 'Analyse de parties de Grands Maitres',
          subLessons: [
            { title: 'Methode d\'analyse' },
            { title: 'Parties de Kasparov' },
            { title: 'Parties de Carlsen' },
            { title: 'Commentaire approfondi' },
            { title: 'Lecons a tirer' }
          ]
        },
        {
          id: 58,
          title: 'Preparation psychologique',
          subLessons: [
            { title: 'Gestion du temps' },
            { title: 'Concentration' },
            { title: 'Gestion du stress' },
            { title: 'Attitude face a l\'adversaire' }
          ]
        },
        {
          id: 59,
          title: 'Preparation aux tournois',
          subLessons: [
            { title: 'Preparer ses adversaires' },
            { title: 'Bases de donnees' },
            { title: 'Gestion de l\'energie' },
            { title: 'Routine pre-partie' },
            { title: 'Analyse post-partie' }
          ]
        },
        {
          id: 60,
          title: 'Revision et evaluation Niveau C',
          subLessons: [
            { title: 'Test tactique avance' },
            { title: 'Evaluation strategique' },
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
            { title: 'Mecanisme de l\'attaque grecque' },
            { title: 'Sacrifice du Fou en h7' },
            { title: 'Suite avec Cg5+' },
            { title: 'Parties modeles' }
          ]
        },
        {
          id: 103,
          title: 'Repertoire d\'ouvertures : Lignes secondaires',
          subLessons: [
            { title: 'Variantes surprises' },
            { title: 'Lignes peu jouees mais solides' },
            { title: 'Avantages psychologiques' },
            { title: 'Preparation specifique' }
          ]
        },
        {
          id: 104,
          title: 'Strategie : La restriction',
          subLessons: [
            { title: 'Limiter les pieces adverses' },
            { title: 'Restriction du Cavalier' },
            { title: 'Restriction du Fou' },
            { title: 'Jeu prophylactique' }
          ]
        },
        {
          id: 105,
          title: 'Strategie : Les faiblesses chroniques',
          subLessons: [
            { title: 'Identifier les faiblesses permanentes' },
            { title: 'Pions faibles' },
            { title: 'Cases faibles' },
            { title: 'Exploitation a long terme' }
          ]
        },
        {
          id: 106,
          title: 'Finales theoriques avancees',
          subLessons: [
            { title: 'Finales de pions complexes' },
            { title: 'Finales avec pieces mineures' },
            { title: 'Positions critiques' },
            { title: 'Etude approfondie' }
          ]
        },
        {
          id: 107,
          title: 'Analyse de parties de Karpov',
          subLessons: [
            { title: 'Le style positionnel de Karpov' },
            { title: 'La technique d\'etouffement' },
            { title: 'Parties commentees' },
            { title: 'Lecons strategiques' }
          ]
        },
        {
          id: 108,
          title: 'Analyse de parties de Fischer',
          subLessons: [
            { title: 'La precision de Fischer' },
            { title: 'Ouvertures de Fischer' },
            { title: 'Parties immortelles' },
            { title: 'Lecons tactiques et strategiques' }
          ]
        },
        {
          id: 109,
          title: 'Gestion du temps en partie',
          subLessons: [
            { title: 'Repartition du temps' },
            { title: 'Moments critiques' },
            { title: 'Eviter le zeitnot' },
            { title: 'Prise de decision rapide' }
          ]
        },
        {
          id: 110,
          title: 'Exercices de consolidation Niveau C',
          subLessons: [
            { title: 'Test tactique avance' },
            { title: 'Evaluation strategique' },
            { title: 'Finales a resoudre' },
            { title: 'Auto-evaluation niveau C' }
          ]
        }
      ]
    },
    {
      code: 'D',
      name: 'Niveau D - Expert',
      description: 'Perfectionnement et preparation professionnelle',
      color: '#FF9800',
      icon: 'heroStar',
      courses: [
        {
          id: 61,
          title: 'Calcul expert et visualisation',
          subLessons: [
            { title: 'Calcul a l\'aveugle' },
            { title: 'Variantes forcees longues' },
            { title: 'Intuition vs calcul' },
            { title: 'Methode de verification' },
            { title: 'Exercices de haut niveau' }
          ]
        },
        {
          id: 62,
          title: 'Tactique : Etudes artistiques',
          subLessons: [
            { title: 'Les grands compositeurs' },
            { title: 'Etudes de mat' },
            { title: 'Etudes de nulle' },
            { title: 'Etudes complexes' },
            { title: 'Creativite tactique' }
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
          title: 'Les desequilibres',
          subLessons: [
            { title: 'Structure de pions vs developpement' },
            { title: 'Materiel vs initiative' },
            { title: 'Espace vs pieces actives' },
            { title: 'Evaluation dynamique' },
            { title: 'Prise de decision' }
          ]
        },
        {
          id: 65,
          title: 'Theorie d\'ouvertures : Lignes critiques',
          subLessons: [
            { title: 'Analyse des lignes principales' },
            { title: 'Nouveautes theoriques' },
            { title: 'Preparation contre un adversaire' },
            { title: 'Memorisation efficace' },
            { title: 'Utilisation des engines' }
          ]
        },
        {
          id: 66,
          title: 'Theorie d\'ouvertures : Systemes anti-mainline',
          subLessons: [
            { title: 'Surprise opening' },
            { title: 'Repertoire secondaire' },
            { title: 'Gambits dangereux' },
            { title: 'Lignes de surprise' }
          ]
        },
        {
          id: 67,
          title: 'Le milieu de jeu complexe',
          subLessons: [
            { title: 'Positions critiques' },
            { title: 'Evaluation des positions complexes' },
            { title: 'Plans dans des positions inconnues' },
            { title: 'Gestion de l\'incertitude' },
            { title: 'Parties decisives' }
          ]
        },
        {
          id: 68,
          title: 'Technique de conversion',
          subLessons: [
            { title: 'Convertir un avantage materiel' },
            { title: 'Convertir un avantage positionnel' },
            { title: 'Simplification technique' },
            { title: 'Eviter les pieges' }
          ]
        },
        {
          id: 69,
          title: 'Defense dans les positions difficiles',
          subLessons: [
            { title: 'Ressources cachees' },
            { title: 'Defense active' },
            { title: 'Contre-jeu pratique' },
            { title: 'Psychologie defensive' },
            { title: 'Exemples celebres' }
          ]
        },
        {
          id: 70,
          title: 'Finales complexes (1)',
          subLessons: [
            { title: 'Tour et Fou vs Tour' },
            { title: 'Tour et Cavalier vs Tour' },
            { title: 'Deux Tours vs Dame' },
            { title: 'Finales theoriques avancees' }
          ]
        },
        {
          id: 71,
          title: 'Finales complexes (2)',
          subLessons: [
            { title: 'Dame vs deux pieces' },
            { title: 'Trois pieces vs Dame' },
            { title: 'Finales de pieces lourdes' },
            { title: 'Positions exceptionnelles' },
            { title: 'Etude approfondie' }
          ]
        },
        {
          id: 72,
          title: 'Analyse avec moteur',
          subLessons: [
            { title: 'Utilisation efficace de Stockfish' },
            { title: 'Interpretation des evaluations' },
            { title: 'Quand faire confiance au moteur' },
            { title: 'Analyse critique' },
            { title: 'Ameliorer son jeu avec les engines' }
          ]
        },
        {
          id: 73,
          title: 'Preparation specifique aux adversaires',
          subLessons: [
            { title: 'Etude du repertoire adverse' },
            { title: 'Identifier les faiblesses' },
            { title: 'Preparer des surprises' },
            { title: 'Adaptation du style' }
          ]
        },
        {
          id: 74,
          title: 'Gestion du temps en competition',
          subLessons: [
            { title: 'Cadences rapides' },
            { title: 'Zeitnot et prise de decision' },
            { title: 'Increment et gestion' },
            { title: 'Exercices en temps reel' }
          ]
        },
        {
          id: 75,
          title: 'Psychologie avancee',
          subLessons: [
            { title: 'Lecture de l\'adversaire' },
            { title: 'Gerer la pression' },
            { title: 'Resilience mentale' },
            { title: 'Pic de performance' },
            { title: 'Recuperation entre parties' }
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
          title: 'Etude des champions du monde',
          subLessons: [
            { title: 'Steinitz et les principes modernes' },
            { title: 'Alekhine et l\'attaque' },
            { title: 'Capablanca et la technique' },
            { title: 'Fischer et la precision' },
            { title: 'Kasparov et la dynamique' },
            { title: 'Carlsen et l\'universalite' }
          ]
        },
        {
          id: 78,
          title: 'Entrainement intensif',
          subLessons: [
            { title: 'Programme d\'entrainement' },
            { title: 'Exercices quotidiens' },
            { title: 'Analyse reguliere' },
            { title: 'Objectifs mesurables' },
            { title: 'Suivi des progres' }
          ]
        },
        {
          id: 79,
          title: 'Preparation aux normes de titre',
          subLessons: [
            { title: 'Comprendre le systeme Elo' },
            { title: 'Normes FM, IM, GM' },
            { title: 'Selection des tournois' },
            { title: 'Strategie de performance' }
          ]
        },
        {
          id: 80,
          title: 'Revision et evaluation Niveau D',
          subLessons: [
            { title: 'Test tactique expert' },
            { title: 'Evaluation strategique avancee' },
            { title: 'Finales theoriques' },
            { title: 'Analyse complete de partie' },
            { title: 'Plan de progression personnalise' },
            { title: 'Certification du niveau' }
          ]
        },
        {
          id: 111,
          title: 'Calcul a longue portee',
          subLessons: [
            { title: 'Calcul de 10+ coups' },
            { title: 'Visualisation avancee' },
            { title: 'Verification systematique' },
            { title: 'Exercices de haut niveau' }
          ]
        },
        {
          id: 112,
          title: 'Intuition et jugement positionnel',
          subLessons: [
            { title: 'Developper son intuition' },
            { title: 'Jugement sans calcul' },
            { title: 'Sens positionnel' },
            { title: 'Exercices d\'evaluation' }
          ]
        },
        {
          id: 113,
          title: 'Preparation d\'ouverture specifique',
          subLessons: [
            { title: 'Etude approfondie d\'une ligne' },
            { title: 'Memorisation efficace' },
            { title: 'Nouveautes a preparer' },
            { title: 'Fichiers de preparation' }
          ]
        },
        {
          id: 114,
          title: 'Les transformations de pions',
          subLessons: [
            { title: 'Changer la structure' },
            { title: 'Ouverture vs fermeture du centre' },
            { title: 'Percees de pions' },
            { title: 'Timing des transformations' }
          ]
        },
        {
          id: 115,
          title: 'Finales de pieces lourdes',
          subLessons: [
            { title: 'Dame et Tour vs Dame et Tour' },
            { title: 'Technique de conversion' },
            { title: 'Positions theoriques' },
            { title: 'Exercices pratiques' }
          ]
        },
        {
          id: 116,
          title: 'Analyse de parties de Carlsen',
          subLessons: [
            { title: 'Le style universel de Carlsen' },
            { title: 'Technique en finale' },
            { title: 'Parties commentees' },
            { title: 'Lecons a retenir' }
          ]
        },
        {
          id: 117,
          title: 'L\'art de la defense',
          subLessons: [
            { title: 'Defense passive vs active' },
            { title: 'Ressources defensives' },
            { title: 'Sauver des positions perdues' },
            { title: 'Psychologie defensive' }
          ]
        },
        {
          id: 118,
          title: 'Psychologie en competition',
          subLessons: [
            { title: 'Gerer la pression' },
            { title: 'Concentration maximale' },
            { title: 'Recuperation entre parties' },
            { title: 'Mentalite de champion' }
          ]
        },
        {
          id: 119,
          title: 'Strategies pour gagner des points Elo',
          subLessons: [
            { title: 'Choix des tournois' },
            { title: 'Gestion des adversaires' },
            { title: 'Optimisation de la performance' },
            { title: 'Progression constante' }
          ]
        },
        {
          id: 120,
          title: 'Examen final Niveau D',
          subLessons: [
            { title: 'Test tactique expert' },
            { title: 'Evaluation strategique complete' },
            { title: 'Finales complexes' },
            { title: 'Analyse de partie complete' },
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
