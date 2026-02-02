import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
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
  questionKey: string;
  answerKey: string;
  category: string;
}

interface FaqCategory {
  id: string;
  labelKey: string;
}

@Component({
  selector: 'app-faq',
  standalone: true,
  imports: [RouterLink, NgIconComponent, TranslateModule],
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

  categories: FaqCategory[] = [
    { id: 'all', labelKey: 'common.all' },
    { id: 'general', labelKey: 'faq.categories.general' },
    { id: 'lessons', labelKey: 'faq.categories.lessons' },
    { id: 'premium', labelKey: 'common.premium' },
    { id: 'payment', labelKey: 'faq.categories.payments' },
    { id: 'coach', labelKey: 'faq.categories.coaches' }
  ];

  faqs: FaqItem[] = [
    // General
    {
      category: 'general',
      questionKey: 'faq.questions.whatIsMychess.q',
      answerKey: 'faq.questions.whatIsMychess.a'
    },
    {
      category: 'general',
      questionKey: 'faq.questions.howToStart.q',
      answerKey: 'faq.questions.howToStart.a'
    },
    {
      category: 'general',
      questionKey: 'faq.questions.technicalRequirements.q',
      answerKey: 'faq.questions.technicalRequirements.a'
    },
    // Lessons
    {
      category: 'lessons',
      questionKey: 'faq.questions.howBookLesson.q',
      answerKey: 'faq.questions.howBookLesson.a'
    },
    {
      category: 'lessons',
      questionKey: 'faq.questions.coachRates.q',
      answerKey: 'faq.questions.coachRates.a'
    },
    {
      category: 'lessons',
      questionKey: 'faq.questions.cancelLesson.q',
      answerKey: 'faq.questions.cancelLesson.a'
    },
    {
      category: 'lessons',
      questionKey: 'faq.questions.firstLessonFree.q',
      answerKey: 'faq.questions.firstLessonFree.a'
    },
    // Premium
    {
      category: 'premium',
      questionKey: 'faq.questions.premiumBenefits.q',
      answerKey: 'faq.questions.premiumBenefits.a'
    },
    {
      category: 'premium',
      questionKey: 'faq.questions.howLessonsWork.q',
      answerKey: 'faq.questions.howLessonsWork.a'
    },
    // Payment
    {
      category: 'payment',
      questionKey: 'faq.questions.paymentMethods.q',
      answerKey: 'faq.questions.paymentMethods.a'
    },
    {
      category: 'payment',
      questionKey: 'faq.questions.dataPrivacy.q',
      answerKey: 'faq.questions.dataPrivacy.a'
    },
    // Coach
    {
      category: 'coach',
      questionKey: 'faq.questions.becomeCoach.q',
      answerKey: 'faq.questions.becomeCoach.a'
    },
    {
      category: 'coach',
      questionKey: 'faq.questions.coachPayments.q',
      answerKey: 'faq.questions.coachPayments.a'
    },
    {
      category: 'coach',
      questionKey: 'faq.questions.whoAreCoaches.q',
      answerKey: 'faq.questions.whoAreCoaches.a'
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

  trackByQuestionKey(index: number, faq: FaqItem): string {
    return faq.questionKey;
  }
}
