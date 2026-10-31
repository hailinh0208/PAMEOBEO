export type Role = 'CUSTOMER' | 'ADMIN';

export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'COOKING' | 'DELIVERING' | 'COMPLETED' | 'CANCELLED';

export interface TokenResponse {
  tokenType: string;
  accessToken: string;
  accessTokenExpiresAt: string;
  refreshToken: string;
  refreshTokenExpiresAt: string;
}

export interface CurrentUser {
  id: string;
  email: string;
  name: string;
  roles: Role[];
  expiresAt: number;
}

export type Meat = 'CHICKEN' | 'TUNA' | 'BEEF' | 'SALMON';

export interface RecipeLine {
  ingredient: string;
  label: string;
  grams: number;
}

/** One combination: 1 flavor or a mix of 2, with or without liver, with its own price. */
export interface MenuItem {
  id: string;
  name: string;
  meats: Meat[];
  mix: boolean;
  withLiver: boolean;
  priceVnd: number;
  active: boolean;
  /** Photos uploaded by the shop, cover first (empty: the emoji is shown) */
  imageIds: string[];
}

/** Public menu: ingredient names only, the quantities stay in the kitchen. */
export interface PublicMenuItem extends MenuItem {
  ingredients: string[];
  /** Real number of jars sold over the last 30 days */
  jarsSold: number;
}

/** Top of the shop: best sellers, completed with the admin's picks ("Gợi ý của tiệm"). */
export interface Highlight {
  item: PublicMenuItem;
  reason: 'BEST_SELLER' | 'FEATURED';
}

/** Admin menu: with the grams of each ingredient in one jar. */
export interface AdminMenuItem extends MenuItem {
  featured: boolean;
  recipe: RecipeLine[];
}

export interface CookingPlan {
  cookingDate: string;
  reservedJars: number;
  jarsWithoutDetails: number;
  items: { menuItemId: string; name: string; jars: number }[];
  ingredients: { ingredient: string; label: string; grams: number }[];
}

export interface Availability {
  cookingDate: string;
  orderCutoffAt: string;
  capacity: number;
  reserved: number;
  remaining: number;
}

export interface OrderLine {
  menuItemId: string;
  name: string;
  unitPriceVnd: number;
  quantity: number;
}

export interface Order {
  id: string;
  status: OrderStatus;
  statusReason: string | null;
  message: string;
  cookingDate: string;
  deliveryWindow: string;
  cancellableUntil: string;
  deliveryAddress: string;
  totalVnd: number;
  items: OrderLine[];
  createdAt: string;
  updatedAt: string;
}

export interface PlaceOrderRequest {
  items: { menuItemId: string; quantity: number }[];
  deliveryAddress: string;
  confirmNextCookingDay: boolean;
}

export interface ApiError {
  code: string;
  message: string;
  suggestedCookingDate?: string;
}

export interface ChaosSettings {
  delayMs: number;
  failurePercent: number;
}

export interface CircuitBreakerState {
  state: 'CLOSED' | 'OPEN' | 'HALF_OPEN' | 'DISABLED' | 'FORCED_OPEN' | string;
  failureRate: string;
  slowCallRate: string;
  bufferedCalls: number;
  failedCalls: number;
  slowCalls: number;
  notPermittedCalls: number;
}

export interface NotificationEntry {
  orderId: string;
  recipient: 'CUSTOMER' | 'SHOP';
  toAddress: string;
  subject: string;
  status: string;
  thread: string;
  sentAt: string;
}
