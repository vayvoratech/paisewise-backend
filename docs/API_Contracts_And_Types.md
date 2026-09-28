# Rupee API Contracts & TypeScript Interfaces (Based on Real Controllers)

## 1. Auth Service (`/auth`)
```typescript
interface RegisterRequest { name: string; phone: string; email: string; password: string; confirmPassword: String; }
interface LoginRequest { phione: string; password: string; }
interface RefreshRequest { refreshToken: string; }
interface AuthResponse { token: string; refreshToken: string; }
interface Tokens { accessToken: string; refreshToken: string; }
interface ForgotPasswordRequest { email: string; }
interface VerifyOtpRequest { email: string; otp: string; }
interface ResetPasswordRequest { newPassword: string; confirmPassword: String; }

2. Learn Service (/learn)
interface LessonView {
  id: string;
  chapter: string;
  chapterNo: number;
  index: number;
  total: number;
  title: string;
  quizXp: number;
  segments: string;     // Raw JSON string
  jargonWords: string;  // Raw JSON string
}

interface JargonView {
  term: string;
  definition: string;
  analogy: string;
  example: string;
}

interface QuizView {
  id: string;
  prompt: string;
  seconds: number;
  xp: number;
  options: string;      // Raw JSON string
  explanation: string;
}

3. Community Service (/community)
interface ReplyView {
  author: string;
  verifiedHelper: boolean;
  text: string;
}

interface PostView {
  id: string;
  author: string;
  location: string;
  ago: string;
  tag: string;
  avatarColor: string;
  text: string;
  replies: ReplyView[];
}

interface FeedView {
  onlineCount: number;
  posts: PostView[];
}

interface CreatePostRequest {
  text: string;
  tag?: string;
}

4. Portfolio Service (/portfolio)
interface HoldingView {
  symbol: string;
  quantity: number;
  avgCost: number;
  currentPrice: number;
  value: number;
  gainAbs: number;
  gainPct: number;
}

interface SummaryView {
  holdingsValue: number;
  invested: number;
  gainAbs: number;
  gainPct: number;
  insight: string;
  holdings: HoldingView[];
}

5. Practice Service (/practice)
interface StockView {
  symbol: string;
  name: string;
  price: number;
  changePct: number;
  emoji: string;
  trend: string;        // Raw JSON string
}

interface PositionView {
  symbol: string;
  quantity: number;
  reservedQuantity: number;
}

interface AccountView {
  balance: number;
  reservedBalance: number;
  positions: PositionView[];
}

interface PlaceOrderRequest {
  symbol: string;
  side: string;         // "BUY" | "SELL"
  shares: number;
  orderType: string;    // "MARKET" | "LIMIT"
  price?: number;       // required for LIMIT orders
  clientOrderId?: string;
}

interface OrderReceipt {
  orderId: string;
  symbol: string;
  side: string;
  shares: number;
  pricePerShare: number;
  totalPaid: number;
  orderType: string;
  status: string;
  xpEarned: number;
}

6. Profile Service (/profile)
interface ProfileView {
  userId: string;
  name: string;
  handle: string;
  city: string;
  level: number;
  dayStreak: number;
  xpTotal: number;
  lessonsCompleted: number;
  language: string;
  dailyReminders: boolean;
  kycVerified: boolean;
}

interface BadgeView {
  emoji: string;
  title: string;
  category: string;
}

interface SettingsUpdate {
  language?: string;
  dailyReminders?: boolean;
}