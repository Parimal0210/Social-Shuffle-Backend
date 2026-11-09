/**
 * Social Shuffle — Community & Event Management Platform Types
 * Pune's Biggest Board Game Meetup Community
 */

export type UserRole = 'admin' | 'participant';

export interface AuthUser {
  id: string;
  name: string;
  email: string;
  phone: string;
  role: UserRole;
  area: string;
  avatar?: string;
  bio?: string;
  participantId?: string;
  authProvider?: 'local' | 'google';
  lastActiveAt?: string;
  loginAt?: string;
  designation?: string;
  adminRole?: AdminRole | string;
  status?: string;
  joinedDate?: string;
  permissions?: string[];
}

export interface LoginAuditEntry {
  id: string;
  userId: string;
  name: string;
  email: string;
  role: string;
  timestamp: string;
  date: string;
}

export interface GoogleUserProfile {
  email: string;
  name: string;
  avatar?: string;
  phone?: string;
  area?: string;
  googleId?: string;
}

export interface LoginCredentials {
  emailOrPhone: string;
  email?: string;
  password?: string;
  name?: string;
  phone?: string;
  designation?: string;
  role?: 'admin' | 'participant';
}

export interface RegisterCredentials {
  name: string;
  email: string;
  phone: string;
  password?: string;
  area: string;
  bio?: string;
  avatar?: string;
}

export interface AuthResponse {
  token: string;
  user: AuthUser;
  participant?: Participant;
  message?: string;
}

export type AdminRole = 'Super Admin' | 'Event Organizer' | 'Volunteer/Staff';

export interface AdminRecord {
  id: string;
  name: string;
  email: string;
  phone?: string;
  role: 'admin';
  designation: string;
  avatar?: string;
  bio?: string;
  area?: string;
  active: boolean;
  createdAt: string;
}

export type EventCategory =
  | 'Regular Meetup'
  | 'Tournament'
  | 'Theme Night'
  | 'Secret Event'
  | 'Collaborative Event'
  | string;

export interface Participant {
  id: string;
  name: string;
  email: string;
  phone: string;
  area: string; // Pune locality e.g., 'Koregaon Park', 'Baner', 'Viman Nagar'
  password?: string; // Stored credential so participant can login henceforth with password
  username?: string;
  avatar?: string;
  bio?: string;
  joinedDate: string;
  totalEventsAttended: number;
  totalRegistrations: number;
  gamesPlayedIds: string[];
  venuesVisited: string[];
  totalPaxBrought: number;
  lastAttendedEventId?: string;
  lastAttendedEventTitle?: string;
}

export type GameCategory =
  | 'Party'
  | 'Strategy'
  | 'Bluffing'
  | 'Social Deduction'
  | 'Card Games'
  | 'Family'
  | 'Team Games'
  | 'Long Format'
  | 'Tournament';

export type GameDifficulty = 'Beginner' | 'Medium' | 'Advanced';

export interface Game {
  id: string;
  name: string;
  category: GameCategory;
  difficulty: GameDifficulty;
  players: string; // e.g., '3-8 Players'
  duration: string; // e.g., '30-45 mins'
  description: string;
  active: boolean;
  isActive?: boolean;
  playsCount: number;
  tags?: string[];
  rating?: number;
  image?: string;
}

export type BoardGame = Game;

export type EventStatus = 'upcoming' | 'active' | 'completed' | 'archived';

export interface ShuffleEvent {
  id: string;
  number: number; // e.g. 35 for Social Shuffle #35
  title: string;
  date: string; // YYYY-MM-DD
  time: string; // e.g. "Sunday, 4:00 PM – 7:30 PM"
  venue: string;
  address: string;
  area: string;
  category: string;
  capacity: number;
  ticketPrice: number; // INR
  description: string;
  notes: string;
  plannedGames: string[]; // Game IDs
  status: EventStatus;
  coverImage?: string;
}

export type PaymentStatus = 'Confirmed' | 'Pending' | 'Waived';

export type AttendanceStatus = 'Checked In' | 'Pending' | 'Cancelled' | 'No Show';

export interface GuestInfo {
  id: string;
  name: string;
  email?: string;
  phone?: string;
  area?: string;
}

export interface Registration {
  id: string;
  eventId: string;
  participantId: string;
  participantName: string;
  participantEmail: string;
  participantPhone: string;
  participantArea: string;
  paxCount: number;
  guests: GuestInfo[];
  paymentStatus: PaymentStatus;
  attendanceStatus: AttendanceStatus;
  checkInTime?: string;
  gamesPlayed: string[]; // Game IDs played during this event
  notes?: string;
  registeredAt: string;
  qrCodeToken?: string;
  qrCodeUrl?: string;
  emailSent?: boolean;
  emailSentAt?: string;
  razorpayPaymentId?: string;
  razorpayOrderId?: string;
  amountPaid?: number;
}

export interface EventFeedback {
  id: string;
  eventId: string;
  eventTitle: string;
  participantId?: string;
  participantName?: string;
  isAnonymous: boolean;
  overallRating: number; // 1-5
  venueRating: number;
  gameRating: number;
  hostRating: number;
  suggestions: string;
  createdAt: string;
  submittedAt?: string;
}

export type ReportCategory =
  | 'Event issue'
  | 'Venue issue'
  | 'Community concern'
  | 'Uncomfortable interaction'
  | 'Harassment'
  | 'Other';

export interface SafetyReport {
  id: string;
  eventId?: string;
  eventTitle?: string;
  type: ReportCategory;
  description: string;
  dateTime?: string;
  personInvolved?: string;
  isAnonymous: boolean;
  reporterName?: string;
  reporterContact?: string;
  status: 'New' | 'Investigating' | 'Resolved';
  resolutionNotes?: string;
  createdAt: string;
  submittedAt?: string;
}

export type VolunteerStatus = 'New' | 'Under Review' | 'Accepted' | 'Rejected' | 'On Hold';

export interface VolunteerApplication {
  id: string;
  name: string;
  email: string;
  phone: string;
  area: string;
  reason: string;
  experience: string;
  skills: string;
  availability: string;
  preferredResponsibilities: string[];
  additionalInfo?: string;
  status: VolunteerStatus;
  submittedAt: string;
}

export interface AuditLog {
  id: string;
  adminName: string;
  action: string;
  target: string;
  details: string;
  description?: string;
  timestamp: string;
}

export type NotificationType = 'event' | 'registration' | 'community' | 'system';

export interface NotificationItem {
  id: string;
  title: string;
  message: string;
  type: NotificationType;
  timestamp: string;
  read: boolean;
  recipientEmail?: string;
  eventId?: string;
  deliveryStatus?: 'Sent' | 'Delivered' | 'Failed';
  emailHtml?: string;
  actionUrl?: string;
}

export type EmailNotificationType = 'event_reminder' | 'registration_confirmation' | 'community_announcement' | 'custom_broadcast';

export interface EmailNotificationRecord {
  id: string;
  type: EmailNotificationType;
  eventId?: string;
  eventTitle?: string;
  recipientEmail: string;
  recipientName: string;
  recipientPhone?: string;
  subject: string;
  htmlContent: string;
  plainText: string;
  status: 'Sent' | 'Delivered' | 'Failed';
  sentAt: string;
  sentBy?: string;
  metadata?: {
    venue?: string;
    eventDate?: string;
    paxCount?: number;
    qrCodeUrl?: string;
  };
}

export interface EventEmailBroadcastRequest {
  eventId: string;
  subject: string;
  message: string;
  notificationType: EmailNotificationType;
  recipientFilter: 'all' | 'confirmed_only' | 'pending_only';
  includeQrPass: boolean;
  includeVenueMap: boolean;
  customHeading?: string;
  channels?: ('email' | 'whatsapp')[];
  whatsAppCustomMessage?: string;
}

export interface RecipientDispatchStatus {
  registrationId: string;
  participantId?: string;
  name: string;
  email: string;
  phone?: string;
  emailSent: boolean;
  emailMessageId?: string;
  emailPreviewUrl?: string;
  whatsappSent: boolean;
  whatsappAvailable: boolean;
  whatsappReason?: string;
  whatsappUrl?: string;
  statusSummary: string;
}

export interface DualPushNotificationResult {
  success: boolean;
  eventId: string;
  eventTitle: string;
  totalRecipients: number;
  emailSentCount: number;
  whatsappSentCount: number;
  whatsappUnavailableCount: number;
  fallbackEmailOnlyCount: number;
  message: string;
  recipients: RecipientDispatchStatus[];
}

export type WhatsAppMessageType =
  | 'announcement'
  | 'sold_out'
  | 'seats_remaining'
  | 'event_reminder'
  | 'game_roster'
  | 'custom';

export interface WhatsAppCommunityBroadcastRecord {
  id: string;
  eventId?: string;
  eventTitle?: string;
  templateType: WhatsAppMessageType;
  messageContent: string;
  targetGroup: string;
  triggeredBy: string;
  triggeredAt: string;
  method: 'whatsapp_web' | 'copied_clipboard' | 'group_link';
}

export interface MetricCard {
  id: string;
  value: string;
  label: string;
  color?: string;
  subtext?: string;
}

export interface CommunityStats {
  id: string;
  membersCount: string;
  membersCountNumeric: number;
  gamesTaught: string;
  gamesTaughtNumeric: number;
  soloPercentage: string;
  soloPercentageNumeric: number;
  eventsHosted: string;
  eventsHostedLabel: string;
  eventsHostedNumeric: number;
  neighborhoods: string[];
  heroHeadline: string;
  heroSchedule: string;
  heroDescription: string;
  communityTagline: string;
  whyJoinText: string;
  curatedMatchmakingText?: string;
  metricCards?: MetricCard[];
  updatedAt?: string;
  updatedBy?: string;
  liveCounts?: {
    participants: number;
    games: number;
    events: number;
    registrations: number;
  };
}

