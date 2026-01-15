export type SubscriptionPlan = 'BASIC' | 'STANDARD' | 'PREMIUM';

export interface Subscription {
  id: number;
  studentId: number;
  studentName?: string;
  planType: SubscriptionPlan;
  planName?: string;
  priceCents: number;
  monthlyQuota: number;
  lessonsUsedThisMonth: number;
  remainingLessons: number;
  startDate: string;
  endDate?: string;
  cancelledAt?: string;
  isActive: boolean;
  stripeSubscriptionId?: string;
  createdAt: string;
}

export interface SubscriptionPlanDetails {
  type: SubscriptionPlan;
  name: string;
  priceCents: number;
  monthlyQuota: number;
  features: string[];
  popular?: boolean;
}

export const SUBSCRIPTION_PLANS: SubscriptionPlanDetails[] = [
  {
    type: 'BASIC',
    name: 'Basic',
    priceCents: 6900,
    monthlyQuota: 4,
    features: [
      '4 cours par mois',
      'Accès au cursus complet',
      'Suivi de progression',
      'Support par email'
    ]
  },
  {
    type: 'STANDARD',
    name: 'Standard',
    priceCents: 12900,
    monthlyQuota: 8,
    features: [
      '8 cours par mois',
      'Accès au cursus complet',
      'Suivi de progression',
      'Support prioritaire',
      'Replays des sessions'
    ],
    popular: true
  },
  {
    type: 'PREMIUM',
    name: 'Premium',
    priceCents: 17900,
    monthlyQuota: 12,
    features: [
      '12 cours par mois',
      'Accès au cursus complet',
      'Suivi de progression',
      'Support VIP 24/7',
      'Replays des sessions',
      'Analyse de parties personnalisée'
    ]
  }
];
