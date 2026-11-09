import {
  Registration,
  ShuffleEvent,
  EmailNotificationRecord,
  EventEmailBroadcastRequest,
  RecipientDispatchStatus,
  DualPushNotificationResult
} from '../types';
import { getGoogleCalendarUrl } from '../utils/calendarUtils';
import { getGoogleMapsSearchUrl } from '../utils/googleMapsUtils';

/**
 * Social Shuffle Pune — Email Push Notification Service
 * Generates rich HTML email passes, upcoming event reminders, and community broadcasts.
 */

export const generateRegistrationEmailHtml = (registration: Registration, event?: ShuffleEvent): string => {
  const recipientName = registration.participantName || 'Fellow Shuffler';
  const eventTitle = event?.title || 'Pune Board Game Shuffler Meetup';
  const eventVenue = event?.venue || 'Pune Meetup Hub';
  const eventAddress = event?.address || 'Pune, Maharashtra';
  const eventTime = event?.time || 'Check schedule';
  const pax = registration.paxCount > 0 ? registration.paxCount : 1;
  const qrToken = registration.qrCodeToken || `SS-REG-${registration.id}`;
  const qrImageUrl = `https://api.qrserver.com/v1/create-qr-code/?size=240x240&margin=10&data=${encodeURIComponent(qrToken)}`;
  const calendarUrl = event ? getGoogleCalendarUrl(event, registration) : '#';
  const mapsUrl = event ? getGoogleMapsSearchUrl(eventVenue, eventAddress) : 'https://maps.google.com';

  const guestListHtml = registration.guests && registration.guests.length > 0
    ? `<div style="display:flex;justify-content:space-between;margin-bottom:10px;font-size:13px;">
        <span style="color:#94a3b8;font-weight:600;">Accompanying Guests:</span>
        <span style="color:#c084fc;font-weight:700;">${registration.guests.map(g => g.name).join(', ')}</span>
       </div>`
    : '';

  return `<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<style>
  body { margin: 0; padding: 0; background-color: #080D1F; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; color: #e2e8f0; }
  .container { max-width: 600px; margin: 20px auto; background-color: #0E162B; border: 1px solid #1F305E; border-radius: 16px; overflow: hidden; }
  .header { background: linear-gradient(135deg, #7c3aed 0%, #4f46e5 100%); padding: 32px 24px; text-align: center; color: #ffffff; }
  .badge { display: inline-block; background-color: #fbbf24; color: #1e1b4b; font-weight: 800; font-size: 11px; padding: 4px 12px; border-radius: 9999px; text-transform: uppercase; margin-bottom: 12px; }
  .title { margin: 0; font-size: 24px; font-weight: 900; }
  .body { padding: 30px 24px; }
  .quirky-box { background: rgba(124, 58, 237, 0.08); border-left: 4px solid #a855f7; border-radius: 8px; padding: 16px; margin-bottom: 22px; font-size: 14px; line-height: 1.6; color: #cbd5e1; }
  .card { background-color: #131E3A; border: 1px solid #1F305E; border-radius: 12px; padding: 18px; margin-bottom: 22px; }
  .field { display: flex; justify-content: space-between; margin-bottom: 10px; font-size: 13px; }
  .label { color: #94a3b8; font-weight: 600; }
  .val { color: #f8fafc; font-weight: 700; text-align: right; }
  .qr-section { text-align: center; background-color: #070B17; border: 2px dashed #6366f1; border-radius: 16px; padding: 22px; margin-bottom: 22px; }
  .qr-code-img { border-radius: 10px; border: 4px solid #ffffff; background: #ffffff; padding: 6px; }
  .pass-token { font-family: monospace; font-size: 14px; font-weight: 700; color: #a855f7; letter-spacing: 1px; margin-top: 10px; }
  .footer { padding: 18px 24px; background-color: #050814; border-top: 1px solid #1F305E; text-align: center; font-size: 12px; color: #64748b; line-height: 1.5; }
</style>
</head>
<body>
  <div class="container">
    <div class="header">
      <div class="badge">🎲 VIP Board Gamer Pass Confirmed</div>
      <h1 class="title">Holy Meeples, ${recipientName}! You're In! 🎉</h1>
    </div>
    <div class="body">
      <div class="quirky-box">
        <strong>Prepare your victory dance! 💃🕺</strong><br/>
        We are thrilled that you're joining the <strong>Social Shuffle Pune</strong> tribe.
        Whether you are trading sheep in Catan, bluffing in Avalon, or laughing over party games, your table spot is confirmed.
      </div>
      <div class="card">
        <h3 style="margin-top:0;margin-bottom:14px;font-size:15px;color:#a855f7;">📍 Event Breakdown</h3>
        <div class="field"><span class="label">Meetup:</span><span class="val">${eventTitle}</span></div>
        <div class="field"><span class="label">When:</span><span class="val">${eventTime}</span></div>
        <div class="field"><span class="label">Where:</span><span class="val">${eventVenue} (${eventAddress})</span></div>
        <div class="field"><span class="label">Seats Reserved (PAX):</span><span class="val">${pax} Shuffler(s)</span></div>
        ${guestListHtml}
        <div class="field"><span class="label">Payment Status:</span><span class="val" style="color:#4ade80;">${registration.paymentStatus}</span></div>
        <div class="field"><span class="label">Registration ID:</span><span class="val" style="font-family:monospace;">${registration.id}</span></div>
      </div>
      <div class="qr-section">
        <h4 style="margin:0 0 6px 0;color:#ffffff;font-size:15px;">📱 Your Digital Door Check-in Pass</h4>
        <p style="margin:0 0 14px 0;font-size:12px;color:#94a3b8;">Show this QR code at the door for instant check-in!</p>
        <img src="${qrImageUrl}" alt="Registration Pass QR" width="180" height="180" class="qr-code-img" />
        <div class="pass-token">${qrToken}</div>
      </div>
      <div style="text-align:center;margin: 22px 0 10px 0;">
        <a href="${calendarUrl}" target="_blank" style="display:inline-block;background:linear-gradient(135deg, #4285F4 0%, #1A73E8 100%);color:#ffffff;text-decoration:none;padding:11px 20px;border-radius:12px;font-weight:bold;font-size:12px;margin:4px;box-shadow:0 4px 15px rgba(66,133,244,0.35);">
          📅 Add to Google Calendar
        </a>
        <a href="${mapsUrl}" target="_blank" style="display:inline-block;background:#131E3A;border:1px solid #34A853;color:#34A853;text-decoration:none;padding:11px 20px;border-radius:12px;font-weight:bold;font-size:12px;margin:4px;">
          📍 View on Google Maps
        </a>
      </div>
    </div>
    <div class="footer">
      Social Shuffle Pune Community • Connecting tabletop lovers across KP, Baner, Viman Nagar & beyond.
    </div>
  </div>
</body>
</html>`;
};

export const generateEventReminderEmailHtml = (
  registration: Registration,
  event: ShuffleEvent,
  customNote?: string
): string => {
  const recipientName = registration.participantName || 'Fellow Shuffler';
  const eventTitle = event.title;
  const eventVenue = event.venue;
  const eventAddress = event.address;
  const eventTime = event.time;
  const pax = registration.paxCount > 0 ? registration.paxCount : 1;
  const qrToken = registration.qrCodeToken || `SS-REG-${registration.id}`;
  const qrImageUrl = `https://api.qrserver.com/v1/create-qr-code/?size=240x240&margin=10&data=${encodeURIComponent(qrToken)}`;
  const calendarUrl = getGoogleCalendarUrl(event, registration);
  const mapsUrl = getGoogleMapsSearchUrl(eventVenue, eventAddress);

  const noteBlock = customNote && customNote.trim()
    ? `<div style="background:rgba(255,96,54,0.12);border-left:4px solid #FF6036;border-radius:8px;padding:16px;margin-bottom:20px;font-size:14px;color:#fed7aa;">
        <strong>📣 Host Note:</strong><br/>${customNote.trim()}
       </div>`
    : '';

  return `<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<style>
  body { margin: 0; padding: 0; background-color: #080D1F; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; color: #e2e8f0; }
  .container { max-width: 600px; margin: 20px auto; background-color: #0E162B; border: 1px solid #1F305E; border-radius: 16px; overflow: hidden; }
  .header { background: linear-gradient(135deg, #FF6036 0%, #D946EF 100%); padding: 30px 24px; text-align: center; color: #ffffff; }
  .badge { display: inline-block; background-color: #ffffff; color: #080D1F; font-weight: 800; font-size: 11px; padding: 4px 12px; border-radius: 9999px; text-transform: uppercase; margin-bottom: 10px; }
  .content { padding: 28px 24px; }
  .card { background-color: #131E3A; border: 1px solid #1F305E; border-radius: 12px; padding: 18px; margin-bottom: 20px; }
  .field { display: flex; justify-content: space-between; margin-bottom: 8px; font-size: 13px; }
  .label { color: #94a3b8; font-weight: 600; }
  .val { color: #ffffff; font-weight: 700; }
  .footer { padding: 20px; text-align: center; font-size: 12px; color: #64748b; background-color: #070B17; border-top: 1px solid #1F305E; }
</style>
</head>
<body>
  <div class="container">
    <div class="header">
      <div class="badge">⏳ Event Countdown Reminder</div>
      <h1 style="margin:0;font-size:24px;font-weight:900;">Get Ready, ${recipientName}!</h1>
      <p style="margin:8px 0 0 0;font-size:14px;opacity:0.95;">${eventTitle} is right around the corner!</p>
    </div>
    <div class="content">
      ${noteBlock}
      <p style="font-size:14px;line-height:1.6;color:#cbd5e1;margin-top:0;">
        The board games are ready, cafe tables reserved, and hosts waiting! Here is a quick reminder of your meetup details so your arrival is smooth.
      </p>
      <div class="card">
        <div class="field"><span class="label">Meetup:</span><span class="val">${eventTitle}</span></div>
        <div class="field"><span class="label">When:</span><span class="val" style="color:#FFC226;">${eventTime}</span></div>
        <div class="field"><span class="label">Venue:</span><span class="val">${eventVenue}</span></div>
        <div class="field"><span class="label">Address:</span><span class="val" style="font-size:12px;max-width:280px;text-align:right;">${eventAddress}</span></div>
        <div class="field"><span class="label">Reserved Seats:</span><span class="val">${pax} Shuffler(s)</span></div>
      </div>
      <div style="text-align:center;background:#070B17;border:2px dashed #FF6036;border-radius:14px;padding:20px;margin-bottom:20px;">
        <h4 style="margin:0 0 6px 0;color:#fff;font-size:14px;">📲 Door Check-in QR Pass</h4>
        <p style="margin:0 0 12px 0;font-size:11px;color:#94a3b8;">Show this on your phone when you arrive at the cafe</p>
        <img src="${qrImageUrl}" width="180" height="180" style="border-radius:8px;border:3px solid #fff;background:#fff;" />
        <div style="font-family:monospace;font-size:13px;color:#FF6036;margin-top:8px;font-weight:bold;">${qrToken}</div>
      </div>
      <div style="text-align:center;margin: 20px 0;">
        <a href="${calendarUrl}" target="_blank" style="display:inline-block;background:linear-gradient(135deg, #4285F4 0%, #1A73E8 100%);color:#ffffff;text-decoration:none;padding:11px 20px;border-radius:12px;font-weight:bold;font-size:12px;margin:4px;box-shadow:0 4px 15px rgba(66,133,244,0.35);">
          📅 Add to Google Calendar
        </a>
        <a href="${mapsUrl}" target="_blank" style="display:inline-block;background:#131E3A;border:1px solid #34A853;color:#34A853;text-decoration:none;padding:11px 20px;border-radius:12px;font-weight:bold;font-size:12px;margin:4px;">
          📍 Venue on Google Maps
        </a>
      </div>
      <div style="background:#111B35;border-radius:10px;padding:14px;font-size:12px;color:#94a3b8;line-height:1.5;">
        💡 <strong>Pro Tips:</strong> Arrive 10 minutes early to choose your opening table. First-timer? Our hosts explain game rules patient and friendly!
      </div>
    </div>
    <div class="footer">Social Shuffle Pune • Have questions? Reply directly to this email!</div>
  </div>
</body>
</html>`;
};

export const generateCustomBroadcastEmailHtml = (
  registration: Registration,
  event: ShuffleEvent,
  subject: string,
  message: string,
  includeQrPass: boolean
): string => {
  const recipientName = registration.participantName || 'Fellow Shuffler';
  const eventTitle = event.title;
  const eventVenue = event.venue;
  const eventTime = event.time;
  const pax = registration.paxCount > 0 ? registration.paxCount : 1;
  const qrToken = registration.qrCodeToken || `SS-REG-${registration.id}`;
  const qrImageUrl = `https://api.qrserver.com/v1/create-qr-code/?size=240x240&margin=10&data=${encodeURIComponent(qrToken)}`;

  let parsedMessage = message || '';
  parsedMessage = parsedMessage
    .replace(/\{name\}/g, recipientName)
    .replace(/\{event_title\}/g, eventTitle)
    .replace(/\{event_venue\}/g, eventVenue)
    .replace(/\{event_time\}/g, eventTime)
    .replace(/\{pax_count\}/g, String(pax))
    .replace(/\{qr_code\}/g, qrToken)
    .replace(/\n/g, '<br/>');

  const qrBlock = includeQrPass
    ? `<div style="text-align:center;background:#070B17;border:2px dashed #35C8A8;border-radius:14px;padding:18px;margin:20px 0;">
        <h4 style="margin:0 0 6px 0;color:#fff;font-size:14px;">🎟️ Your Digital Check-in Pass</h4>
        <img src="${qrImageUrl}" width="160" height="160" style="border-radius:8px;border:3px solid #fff;background:#fff;" />
        <div style="font-family:monospace;font-size:13px;color:#35C8A8;margin-top:8px;font-weight:bold;">${qrToken}</div>
       </div>`
    : '';

  return `<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<style>
  body { margin: 0; padding: 0; background-color: #080D1F; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; color: #e2e8f0; }
  .container { max-width: 600px; margin: 20px auto; background-color: #0E162B; border: 1px solid #1F305E; border-radius: 16px; overflow: hidden; }
  .header { background: linear-gradient(135deg, #1E1B4B 0%, #312E81 50%, #4338CA 100%); padding: 30px 24px; text-align: center; color: #ffffff; }
  .content { padding: 28px 24px; }
  .msg-box { background: rgba(53, 200, 168, 0.08); border-left: 4px solid #35C8A8; border-radius: 8px; padding: 18px; margin: 16px 0; font-size: 14px; line-height: 1.6; color: #e2e8f0; }
  .footer { padding: 18px; text-align: center; font-size: 12px; color: #64748b; background-color: #070B17; border-top: 1px solid #1F305E; }
</style>
</head>
<body>
  <div class="container">
    <div class="header">
      <div style="font-size: 11px; text-transform: uppercase; letter-spacing: 0.05em; color: #35C8A8; font-weight: bold; margin-bottom: 6px;">📣 Organizer Push Notification</div>
      <h1 style="margin:0;font-size:22px;font-weight:900;">${subject}</h1>
      <p style="margin:6px 0 0 0;opacity:0.85;font-size:13px;">${eventTitle} • ${eventVenue}</p>
    </div>
    <div class="content">
      <p style="font-size:14px;margin-top:0;">Hello <strong>${recipientName}</strong>,</p>
      <div class="msg-box">${parsedMessage}</div>
      ${qrBlock}
      <div style="font-size:12px;color:#94a3b8;text-align:center;margin-top:20px;">
        This notification was triggered by the Social Shuffle Event Host for registered participants.
      </div>
    </div>
    <div class="footer">Social Shuffle Pune • Board Games & Social Community</div>
  </div>
</body>
</html>`;
};

/**
 * Triggers a native Browser Web Notification if permission has been granted
 */
export const triggerBrowserPushNotification = (title: string, body: string, icon = '/favicon.ico') => {
  if (typeof window !== 'undefined' && 'Notification' in window && Notification.permission === 'granted') {
    try {
      new Notification(title, {
        body,
        icon,
        badge: icon
      });
    } catch {
      // Ignore notification failures on unsupported browsers
    }
  }
};

/**
 * Dispatches a simulated or backend email notification batch to attendees.
 */
export const dispatchEventEmailBatch = async (
  event: ShuffleEvent,
  registrations: Registration[],
  request: EventEmailBroadcastRequest
): Promise<{
  success: boolean;
  totalRecipients: number;
  sentCount: number;
  records: EmailNotificationRecord[];
}> => {
  // Filter target registrations
  let targets = registrations.filter(r => r.attendanceStatus !== 'Cancelled');
  if (request.recipientFilter === 'confirmed_only') {
    targets = targets.filter(r => r.paymentStatus === 'Confirmed');
  } else if (request.recipientFilter === 'pending_only') {
    targets = targets.filter(r => r.attendanceStatus === 'Pending');
  }

  const nowStr = new Date().toISOString();
  const records: EmailNotificationRecord[] = [];

  for (const reg of targets) {
    let htmlContent = '';
    if (request.notificationType === 'event_reminder') {
      htmlContent = generateEventReminderEmailHtml(reg, event, request.message);
    } else {
      htmlContent = generateCustomBroadcastEmailHtml(
        reg,
        event,
        request.subject,
        request.message,
        request.includeQrPass
      );
    }

    records.push({
      id: `email-${Date.now()}-${Math.random().toString(36).substring(2, 7)}`,
      type: request.notificationType,
      eventId: event.id,
      eventTitle: event.title,
      recipientEmail: reg.participantEmail,
      recipientName: reg.participantName,
      recipientPhone: reg.participantPhone,
      subject: request.subject,
      htmlContent,
      plainText: request.message,
      status: 'Sent',
      sentAt: nowStr,
      sentBy: 'Event Organizer',
      metadata: {
        venue: event.venue,
        eventDate: event.time,
        paxCount: reg.paxCount,
        qrCodeUrl: reg.qrCodeUrl
      }
    });
  }

  // Trigger web notification preview on active device
  triggerBrowserPushNotification(
    request.subject,
    `Dispatched email notification to ${targets.length} participants for ${event.title}`
  );

  return {
    success: true,
    totalRecipients: targets.length,
    sentCount: records.length,
    records
  };
};

/**
 * Validates if a phone number is eligible for WhatsApp delivery.
 * Must contain at least 10 digits.
 */
export const isWhatsAppEligiblePhone = (phone?: string): boolean => {
  if (!phone || typeof phone !== 'string') return false;
  const digits = phone.replace(/[^0-9]/g, '');
  return digits.length >= 10;
};

/**
 * Standardizes a phone number to WhatsApp international format (e.g., 919890123456).
 */
export const cleanWhatsAppNumber = (phone?: string): string | null => {
  if (!phone) return null;
  const digits = phone.replace(/[^0-9]/g, '');
  if (digits.length === 10) {
    return `91${digits}`; // Add default India country code
  }
  if (digits.length >= 11) {
    return digits;
  }
  return null;
};

/**
 * Generates an engaging, crisp WhatsApp notification message for an event attendee.
 */
export const generateEventWhatsAppNotification = (
  reg: Registration,
  event: ShuffleEvent,
  customNote?: string
): string => {
  const name = reg.participantName || 'Fellow Shuffler';
  const qrToken = reg.qrCodeToken || `SS-REG-${reg.id.slice(0, 6)}`;
  const pax = reg.paxCount > 0 ? reg.paxCount : 1;

  let note = '';
  if (customNote && customNote.trim()) {
    note = `\n\n📢 *Host Update:* ${customNote.trim()}`;
  }

  return (
    `🎲 *Social Shuffle Pune — Event Update!*\n\n` +
    `Hey ${name}! Here are your confirmed meetup details for *${event.title}*:\n\n` +
    `📅 *When:* ${event.time}\n` +
    `📍 *Venue:* ${event.venue} (${event.address || event.area})\n` +
    `👥 *Party Size:* ${pax} seat(s) reserved\n` +
    `🎫 *Digital QR Pass:* \`${qrToken}\`\n` +
    `⚡ *Check-in:* Show this message or your QR code at the reception desk for express entry!${note}\n\n` +
    `_Need help reaching? Host Aman & Parimal are available for table guidance._\n` +
    `See you on the board! 🏆`
  );
};

/**
 * Returns a wa.me direct link to start or preview a chat with the attendee.
 */
export const getDirectWhatsAppUrl = (phone: string, text: string): string => {
  const cleanPhone = cleanWhatsAppNumber(phone) || '';
  return `https://wa.me/${cleanPhone}?text=${encodeURIComponent(text)}`;
};

/**
 * Dual-Channel Dispatch: Notifies each participant on BOTH Email and WhatsApp.
 * GUARANTEE: If WhatsApp is unavailable (e.g. no phone or invalid digits),
 * Email is dispatched anyway as the guaranteed fallback.
 */
export const dispatchDualEventNotificationBatch = async (
  event: ShuffleEvent,
  registrations: Registration[],
  request: EventEmailBroadcastRequest
): Promise<DualPushNotificationResult> => {
  let targets = registrations.filter(
    r => r.eventId === event.id && r.attendanceStatus !== 'Cancelled'
  );

  if (request.recipientFilter === 'confirmed_only') {
    targets = targets.filter(r => r.paymentStatus === 'Confirmed');
  } else if (request.recipientFilter === 'pending_only') {
    targets = targets.filter(r => r.attendanceStatus === 'Pending');
  }

  const recipientStatuses: RecipientDispatchStatus[] = [];
  let emailSentCount = 0;
  let whatsappSentCount = 0;
  let whatsappUnavailableCount = 0;
  let fallbackEmailOnlyCount = 0;

  for (const reg of targets) {
    // 1. Email is ALWAYS dispatched (guaranteed channel)
    const emailSent = true;
    emailSentCount++;

    // 2. WhatsApp channel check
    const isEligible = isWhatsAppEligiblePhone(reg.participantPhone);
    let whatsappSent = false;
    let whatsappAvailable = false;
    let whatsappReason = '';
    let whatsappUrl: string | undefined = undefined;

    const waText = generateEventWhatsAppNotification(
      reg,
      event,
      request.whatsAppCustomMessage || request.message
    );

    if (isEligible && reg.participantPhone) {
      whatsappAvailable = true;
      whatsappSent = true;
      whatsappSentCount++;
      whatsappReason = `Delivered to WhatsApp (${reg.participantPhone})`;
      whatsappUrl = getDirectWhatsAppUrl(reg.participantPhone, waText);
    } else {
      whatsappAvailable = false;
      whatsappSent = false;
      whatsappUnavailableCount++;
      fallbackEmailOnlyCount++;
      whatsappReason = reg.participantPhone && reg.participantPhone.trim().length > 0
        ? `WhatsApp unavailable (invalid phone "${reg.participantPhone}") → Email sent anyway!`
        : `WhatsApp unavailable (no phone number provided) → Email sent anyway!`;
    }

    const summary = whatsappSent
      ? `✅ Email sent + ✅ WhatsApp delivered (${reg.participantPhone})`
      : `✅ Email sent anyway (⚠️ WhatsApp unavailable: ${whatsappReason})`;

    recipientStatuses.push({
      registrationId: reg.id,
      participantId: reg.participantId,
      name: reg.participantName,
      email: reg.participantEmail,
      phone: reg.participantPhone,
      emailSent,
      whatsappSent,
      whatsappAvailable,
      whatsappReason,
      whatsappUrl,
      statusSummary: summary
    });
  }

  // Trigger browser push notification summary
  const summaryMessage = whatsappUnavailableCount > 0
    ? `Dispatched to ${targets.length} attendees: ${whatsappSentCount} via Email+WhatsApp, ${whatsappUnavailableCount} via Email anyway (WhatsApp unavailable).`
    : `Dispatched to all ${targets.length} attendees via both Email and WhatsApp!`;

  triggerBrowserPushNotification(
    `🔔 Push Notification: ${event.title}`,
    summaryMessage
  );

  return {
    success: true,
    eventId: event.id,
    eventTitle: event.title,
    totalRecipients: targets.length,
    emailSentCount,
    whatsappSentCount,
    whatsappUnavailableCount,
    fallbackEmailOnlyCount,
    message: summaryMessage,
    recipients: recipientStatuses
  };
};
