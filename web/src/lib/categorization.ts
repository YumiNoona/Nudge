/**
 * On-device categorization engine — TypeScript mirror of DefaultCategorizationEngine.
 * Starts with built-in merchant→category rules, learns from user corrections.
 */

export type CategorizationSource = 'built_in_rule' | 'user_learned' | 'heuristic_fallback';

export interface CategorizationResult {
  categoryId: string | null; // category type string (e.g., "food", "transport")
  confidence: number; // 0.0 - 1.0
  source: CategorizationSource;
}

// Built-in rules: merchant name → category type
const BUILT_IN_RULES: Record<string, string> = {
  // Food & Dining
  swiggy: 'food', zomato: 'food', 'uber eats': 'food', doordash: 'food',
  "mcdonald's": 'food', starbucks: 'food', chipotle: 'food',
  "domino's": 'food', 'pizza hut': 'food', subway: 'food',
  'burger king': 'food', kfc: 'food', dunzo: 'food',
  faasos: 'food',

  // Transport
  uber: 'transport', ola: 'transport', lyft: 'transport', rapido: 'transport',
  irctc: 'transport', makemytrip: 'transport', goibibo: 'transport',
  'transport for london': 'transport',

  // Groceries
  bigbasket: 'groceries', blinkit: 'groceries', zepto: 'groceries',
  dmart: 'groceries', jiomart: 'groceries', tesco: 'groceries',
  "sainsbury's": 'groceries', walmart: 'groceries', costco: 'groceries',

  // Shopping
  amazon: 'shopping', flipkart: 'shopping', myntra: 'shopping',
  target: 'shopping',

  // Entertainment
  netflix: 'entertainment', hotstar: 'entertainment', spotify: 'entertainment',
  youtube: 'entertainment', bookmyshow: 'entertainment', steam: 'entertainment',

  // Utilities
  electricity: 'utilities', airtel: 'utilities', jio: 'utilities',
  vodafone: 'utilities', 'tata sky': 'utilities',

  // Subscriptions
  'google play': 'subscriptions', microsoft: 'subscriptions', apple: 'subscriptions',
  adobe: 'subscriptions', dropbox: 'subscriptions',

  // Healthcare
  apollo: 'healthcare', practo: 'healthcare', pharmeasy: 'healthcare',
  walgreens: 'healthcare', cvs: 'healthcare',

  // Insurance
  lic: 'investments', insurance: 'investments',
};

const KEYWORD_RULES: [string, string[]][] = [
  ['food', ['food', 'restaurant', 'cafe', 'diner', 'kitchen', 'pizza', 'burger', 'sushi', 'taco', 'grill', 'curry', 'bar']],
  ['transport', ['uber', 'ola', 'lyft', 'taxi', 'cab', 'auto', 'metro', 'bus', 'train', 'flight', 'petrol', 'fuel', 'parking', 'toll']],
  ['groceries', ['grocery', 'supermarket', 'mart', 'store', 'vegetable', 'fruit', 'provision']],
  ['shopping', ['mall', 'store', 'shop', 'retail', 'fashion', 'clothing', 'electronics']],
  ['entertainment', ['movie', 'cinema', 'theatre', 'game', 'gaming', 'arcade', 'concert', 'streaming']],
  ['utilities', ['electric', 'water', 'gas', 'bill', 'recharge', 'mobile', 'broadband', 'wifi', 'internet', 'dth']],
  ['healthcare', ['doctor', 'hospital', 'clinic', 'pharmacy', 'medical', 'health', 'medicine', 'dental']],
  ['education', ['school', 'college', 'university', 'course', 'tuition', 'learning', 'class', 'books']],
  ['subscriptions', ['subscription', 'membership', 'saas', 'cloud', 'hosting']],
  ['investments', ['mutual fund', 'stock', 'share', 'sip', 'investment', 'trading', 'demat', 'insurance', 'premium']],
  ['rent', ['rent', 'lease', 'pg', 'hostel', 'flat']],
  ['travel', ['hotel', 'resort', 'stay', 'booking', 'trip', 'vacation', 'tour']],
  ['personal_care', ['salon', 'spa', 'barber', 'parlour', 'beauty', 'gym', 'fitness', 'yoga']],
];

export class CategorizationEngine {
  private userRules: Map<string, string> = new Map();
  private userConfidence: Map<string, number> = new Map();

  autoCategorize(merchant: string): CategorizationResult {
    const lowered = merchant.toLowerCase().trim();

    // 1. User-learned rules first
    for (const [pattern, catId] of this.userRules) {
      if (lowered.includes(pattern.toLowerCase())) {
        return { categoryId: catId, confidence: 0.95, source: 'user_learned' };
      }
    }

    // 2. Built-in rules
    for (const [key, catType] of Object.entries(BUILT_IN_RULES)) {
      if (lowered.includes(key)) {
        return { categoryId: catType, confidence: 0.80, source: 'built_in_rule' };
      }
    }

    // 3. Heuristic keyword matching
    for (const [catType, keywords] of KEYWORD_RULES) {
      if (keywords.some((kw) => lowered.includes(kw))) {
        return { categoryId: catType, confidence: 0.50, source: 'heuristic_fallback' };
      }
    }

    return { categoryId: null, confidence: 0, source: 'heuristic_fallback' };
  }

  learn(merchant: string, categoryId: string): void {
    const key = merchant.toLowerCase().trim();
    this.userRules.set(key, categoryId);
    this.userConfidence.set(key, (this.userConfidence.get(key) || 0) + 1);
  }

  exportMappings(): Record<string, string> {
    return Object.fromEntries(this.userRules);
  }

  importMappings(mappings: Record<string, string>): void {
    for (const [key, value] of Object.entries(mappings)) {
      this.userRules.set(key.toLowerCase().trim(), value);
    }
  }
}

export const categorizationEngine = new CategorizationEngine();
