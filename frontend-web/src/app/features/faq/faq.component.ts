import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { SeoService } from '../../core/services/seo.service';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import {
  heroChevronDown,
  heroChevronUp,
  heroQuestionMarkCircle,
  heroCreditCard,
  heroCalendarDays,
  heroUserGroup,
  heroAcademicCap,
  heroVideoCamera,
  heroBell,
  heroShieldCheck
} from '@ng-icons/heroicons/outline';

interface FaqItem {
  question: string;
  answer: string;
  category: string;
}

@Component({
  selector: 'app-faq',
  standalone: true,
  imports: [RouterLink, NgIconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  viewProviders: [provideIcons({
    heroChevronDown,
    heroChevronUp,
    heroQuestionMarkCircle,
    heroCreditCard,
    heroCalendarDays,
    heroUserGroup,
    heroAcademicCap,
    heroVideoCamera,
    heroBell,
    heroShieldCheck
  })],
  templateUrl: './faq.component.html',
  styleUrl: './faq.component.scss'
})
export class FaqComponent {
  authService = inject(AuthService);
  private seoService = inject(SeoService);

  openIndex = signal<number | null>(null);
  activeCategory = signal<string>('all');
  mobileMenuOpen = signal(false);

  categories = [
    { id: 'all', label: 'Toutes' },
    { id: 'general', label: 'General' },
    { id: 'lessons', label: 'Cours' },
    { id: 'premium', label: 'Premium' },
    { id: 'payment', label: 'Paiement' },
    { id: 'coach', label: 'Coachs' }
  ];

  faqs: FaqItem[] = [
    // General
    {
      category: 'general',
      question: 'Qu\'est-ce que mychess ?',
      answer: 'mychess est une plateforme de cours d\'echecs en ligne qui met en relation des joueurs avec des coachs qualifies. Vous pouvez reserver des cours en visioconference, suivre votre progression et ameliorer votre niveau aux echecs.'
    },
    {
      category: 'general',
      question: 'Comment fonctionne la progression ?',
      answer: 'mychess propose 4 niveaux de progression : Pion (debutant), Cavalier (intermediaire), Reine (avance) et Roi (expert). Chaque niveau comprend des cours structures que vous validez avec votre coach pour progresser.'
    },
    {
      category: 'general',
      question: 'Dois-je installer un logiciel ?',
      answer: 'Non, mychess fonctionne entierement dans votre navigateur. Les cours en visioconference utilisent Jitsi Meet, qui ne necessite aucune installation. Vous avez juste besoin d\'une webcam et d\'un micro.'
    },
    // Lessons
    {
      category: 'lessons',
      question: 'Comment reserver un cours ?',
      answer: 'Parcourez la liste des coachs, consultez leurs profils et disponibilites, puis selectionnez un creneau qui vous convient. Apres le paiement, le coach confirme la reservation et vous recevez un email avec le lien de la visioconference.'
    },
    {
      category: 'lessons',
      question: 'Combien coute un cours ?',
      answer: 'Chaque coach fixe librement son tarif, generalement entre 30 et 80 euros de l\'heure. Le prix est affiche sur le profil de chaque coach avant la reservation.'
    },
    {
      category: 'lessons',
      question: 'Puis-je annuler un cours ?',
      answer: 'Oui. Si vous annulez plus de 24h avant le cours, vous etes rembourse a 100%. Entre 2h et 24h avant, le remboursement est de 50%. Moins de 2h avant, aucun remboursement n\'est possible.'
    },
    {
      category: 'lessons',
      question: 'Que se passe-t-il si le coach annule ?',
      answer: 'Si le coach annule ou ne confirme pas votre reservation sous 24h, vous etes automatiquement rembourse a 100%.'
    },
    // Premium
    {
      category: 'premium',
      question: 'Quels sont les avantages Premium ?',
      answer: 'L\'abonnement Premium (4,99 euros/mois) inclut : le revisionnage illimite de vos cours, les notifications prioritaires quand vos coachs favoris publient des creneaux, un badge Premium sur votre profil, et l\'acces a l\'entrainement contre myChessBot.'
    },
    {
      category: 'premium',
      question: 'Comment fonctionne l\'essai gratuit ?',
      answer: 'Vous pouvez essayer Premium gratuitement pendant 14 jours, sans entrer de carte bancaire. A la fin de l\'essai, vous choisissez de vous abonner ou non. Aucun engagement.'
    },
    {
      category: 'premium',
      question: 'Puis-je annuler mon abonnement ?',
      answer: 'Oui, l\'abonnement est sans engagement. Vous pouvez annuler a tout moment depuis votre espace abonnement. Vous conservez l\'acces Premium jusqu\'a la fin de la periode payee.'
    },
    // Payment
    {
      category: 'payment',
      question: 'Quels moyens de paiement acceptez-vous ?',
      answer: 'Nous acceptons les cartes bancaires (Visa, Mastercard, American Express) via Stripe, le leader mondial du paiement securise en ligne.'
    },
    {
      category: 'payment',
      question: 'Comment fonctionne le portefeuille ?',
      answer: 'Vous pouvez crediter votre portefeuille mychess (minimum 10 euros) et utiliser ce solde pour payer vos cours. Les remboursements sont automatiquement credites sur votre portefeuille.'
    },
    {
      category: 'payment',
      question: 'Puis-je obtenir une facture ?',
      answer: 'Oui, une facture PDF est generee automatiquement pour chaque paiement. Vous pouvez les telecharger depuis votre espace "Mes factures".'
    },
    // Coach
    {
      category: 'coach',
      question: 'Comment devenir coach sur mychess ?',
      answer: 'Inscrivez-vous en tant que coach, completez votre profil avec votre experience et vos qualifications, puis configurez vos disponibilites et votre tarif horaire. Une fois votre compte Stripe Connect configure, vous pouvez recevoir des reservations.'
    },
    {
      category: 'coach',
      question: 'Quelle commission preleve mychess ?',
      answer: 'mychess preleve 12.5% sur chaque cours (10% pour la plateforme + 2.5% de frais Stripe). Vous recevez le reste directement sur votre compte bancaire.'
    },
    {
      category: 'coach',
      question: 'Comment sont payes les coachs ?',
      answer: 'Les paiements sont transferes via Stripe Connect directement sur votre compte bancaire. Vous devez configurer votre compte Stripe Connect dans les parametres pour recevoir vos paiements.'
    }
  ];

  constructor() {
    this.seoService.updateMetaTags({
      title: 'FAQ - mychess | Questions frequentes',
      description: 'Trouvez les reponses a vos questions sur mychess : cours d\'echecs, abonnement Premium, paiements, coachs et plus encore.',
      keywords: 'mychess, faq, questions, aide, support, cours echecs'
    });
  }

  toggleFaq(index: number): void {
    this.openIndex.update(current => current === index ? null : index);
  }

  toggleMobileMenu(): void {
    this.mobileMenuOpen.update(v => !v);
  }

  closeMobileMenu(): void {
    this.mobileMenuOpen.set(false);
  }

  setCategory(category: string): void {
    this.activeCategory.set(category);
    this.openIndex.set(null);
  }

  get filteredFaqs(): FaqItem[] {
    const category = this.activeCategory();
    if (category === 'all') {
      return this.faqs;
    }
    return this.faqs.filter(faq => faq.category === category);
  }
}
