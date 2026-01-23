export type SubscriptionPlan = 'PREMIUM';

export interface Subscription {
  id: number;
  studentId: number;
  studentName?: string;
  planType: SubscriptionPlan;
  planName?: string;
  priceCents: number;
  features?: string[];
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
  features: string[];
  popular?: boolean;
}

export const SUBSCRIPTION_PLANS: SubscriptionPlanDetails[] = [
  {
    type: 'PREMIUM',
    name: 'Premium',
    priceCents: 499,
    features: [
      'Revisionnage des cours - Accès aux enregistrements vidéo',
      'Notifications prioritaires - Alertes créneaux des coachs favoris',
      'Accès prioritaire - Voir les disponibilités 24h avant',
      'Statistiques avancées - Dashboard détaillé de progression',
      'Badge Premium - Badge doré visible sur le profil'
    ],
    popular: true
  }
];
