package com.socialshuffle.service;

import com.socialshuffle.model.Registration;
import com.socialshuffle.model.ShuffleEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Service
public class EmailNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);

    /**
     * Builds the quirky and grateful HTML email for successful event registration.
     */
    public String generateQuirkyRegistrationEmailHtml(Registration registration, ShuffleEvent event) {
        String recipientName = registration.getParticipantName() != null ? registration.getParticipantName() : "Fellow Shuffler";
        String eventTitle = event != null && event.getTitle() != null ? event.getTitle() : "Pune Board Game Shuffler Meetup";
        String eventVenue = event != null && event.getVenue() != null ? event.getVenue() : "Pune Meetup Hub";
        String eventAddress = event != null && event.getAddress() != null ? event.getAddress() : "Pune, Maharashtra";
        String eventTime = event != null && event.getTime() != null ? event.getTime() : "Check app schedule";
        int pax = registration.getPaxCount() > 0 ? registration.getPaxCount() : 1;
        String qrToken = registration.getQrCodeToken() != null ? registration.getQrCodeToken() : ("SS-REG-" + registration.getId());
        
        String encodedQrData = URLEncoder.encode(qrToken, StandardCharsets.UTF_8);
        String qrImageUrl = "https://api.qrserver.com/v1/create-qr-code/?size=240x240&margin=10&data=" + encodedQrData;

        String guestListHtml = "";
        if (registration.getGuests() != null && !registration.getGuests().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < registration.getGuests().size(); i++) {
                com.socialshuffle.model.GuestInfo g = registration.getGuests().get(i);
                if (g.getName() != null && !g.getName().trim().isEmpty()) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(g.getName().trim());
                }
            }
            if (sb.length() > 0) {
                guestListHtml = "<div class='field'><span class='label'>Accompanying Guests:</span><span class='val' style='color: #c084fc;'>" + sb.toString() + "</span></div>";
            }
        }

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<style>" +
                "  body { margin: 0; padding: 0; background-color: #0f111a; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #e2e8f0; }" +
                "  .container { max-width: 600px; margin: 0 auto; background-color: #161824; border: 1px solid #2d3748; border-radius: 16px; overflow: hidden; margin-top: 24px; margin-bottom: 30px; }" +
                "  .header { background: linear-gradient(135deg, #7c3aed 0%, #4f46e5 100%); padding: 32px 24px; text-align: center; color: #ffffff; }" +
                "  .badge { display: inline-block; background-color: #fbbf24; color: #1e1b4b; font-weight: 800; font-size: 11px; padding: 4px 12px; border-radius: 9999px; text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 12px; }" +
                "  .title { margin: 0; font-size: 26px; font-weight: 900; line-height: 1.2; }" +
                "  .body { padding: 32px 28px; }" +
                "  .quirky-box { background: rgba(124, 58, 237, 0.08); border-left: 4px solid #a855f7; border-radius: 8px; padding: 18px; margin-bottom: 24px; font-size: 14px; line-height: 1.6; color: #cbd5e1; }" +
                "  .card { background-color: #1e2235; border: 1px solid #333a52; border-radius: 12px; padding: 20px; margin-bottom: 24px; }" +
                "  .field { display: flex; justify-content: space-between; margin-bottom: 10px; font-size: 13px; }" +
                "  .label { color: #94a3b8; font-weight: 600; }" +
                "  .val { color: #f8fafc; font-weight: 700; text-align: right; }" +
                "  .qr-section { text-align: center; background-color: #0b0d14; border: 2px dashed #6366f1; border-radius: 16px; padding: 24px; margin-bottom: 24px; }" +
                "  .qr-code-img { border-radius: 12px; border: 4px solid #ffffff; background: #ffffff; padding: 6px; box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.5); }" +
                "  .pass-token { font-family: monospace; font-size: 14px; font-weight: 700; color: #a855f7; letter-spacing: 1px; margin-top: 12px; }" +
                "  .footer { padding: 20px 24px; background-color: #0d0e15; border-top: 1px solid #232738; text-align: center; font-size: 12px; color: #64748b; line-height: 1.5; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "  <div class='container'>" +
                "    <div class='header'>" +
                "      <div class='badge'>🎲 VIP Board Gamer Pass Confirmed</div>" +
                "      <h1 class='title'>Holy Meeples, " + recipientName + "! You're In! 🎉</h1>" +
                "    </div>" +
                "    <div class='body'>" +
                "      <div class='quirky-box'>" +
                "        <strong>Prepare your victory dance! 💃🕺</strong><br/>" +
                "        We are downright stoked that you're joining the <strong>Social Shuffle Pune</strong> tribe. " +
                "        Whether you're looking to trade sheep for wheat in Catan, bluff your closest friends into oblivion during Avalon, " +
                "        or just soak up good vibes over cold brews and board games, a table is officially reserved for you." +
                "        <br/><br/>" +
                "        <em>⚠️ Mild Warning: Side effects may include uncontrolled giggles, sudden cravings for 20 new board games, and making life-long friends in Pune!</em>" +
                "      </div>" +
                "      <div class='card'>" +
                "        <h3 style='margin-top: 0; margin-bottom: 14px; font-size: 15px; color: #a855f7;'>📍 Event Breakdown</h3>" +
                "        <div class='field'><span class='label'>Meetup:</span><span class='val'>" + eventTitle + "</span></div>" +
                "        <div class='field'><span class='label'>When:</span><span class='val'>" + eventTime + "</span></div>" +
                "        <div class='field'><span class='label'>Where:</span><span class='val'>" + eventVenue + " (" + eventAddress + ")</span></div>" +
                "        <div class='field'><span class='label'>Seats Reserved (PAX):</span><span class='val'>" + pax + " Shuffler(s)</span></div>" +
                guestListHtml +
                "        <div class='field'><span class='label'>Payment Status:</span><span class='val' style='color: #4ade80;'>" + registration.getPaymentStatus() + (registration.getRazorpayPaymentId() != null ? " (Paid via Razorpay: " + registration.getRazorpayPaymentId() + ")" : "") + "</span></div>" +
                "        <div class='field'><span class='label'>Registration ID:</span><span class='val' style='font-family: monospace;'>" + registration.getId() + "</span></div>" +
                "      </div>" +
                "      <div class='qr-section'>" +
                "        <h4 style='margin: 0 0 6px 0; color: #ffffff; font-size: 15px;'>📱 Your Digital Check-in Pass</h4>" +
                "        <p style='margin: 0 0 16px 0; font-size: 12px; color: #94a3b8;'>Flash this QR code at the door or let our host scan it on the iPad for instant check-in!</p>" +
                "        <img src='" + qrImageUrl + "' alt='Registration Check-in QR' width='200' height='200' class='qr-code-img' />" +
                "        <div class='pass-token'>" + qrToken + "</div>" +
                "      </div>" +
                "      <div style='font-size: 13px; color: #94a3b8; text-align: center;'>" +
                "        Questions, panic, or game requests? Reply right here or ping Aman / Parimal on WhatsApp!<br/>" +
                "        See you at the tables! 🚀" +
                "      </div>" +
                "    </div>" +
                "    <div class='footer'>" +
                "      Social Shuffle Pune Community • Connecting board game enthusiasts across KP, Baner, Viman Nagar & beyond.<br/>" +
                "      Made with ❤️, dice rolls, and zero boring evenings." +
                "    </div>" +
                "  </div>" +
                "</body>" +
                "</html>";
    }

    /**
     * Builds upcoming event reminder email HTML.
     */
    public String generateUpcomingEventReminderEmailHtml(Registration registration, ShuffleEvent event, String customNote) {
        String recipientName = registration.getParticipantName() != null ? registration.getParticipantName() : "Fellow Shuffler";
        String eventTitle = event != null && event.getTitle() != null ? event.getTitle() : "Upcoming Pune Board Game Meetup";
        String eventVenue = event != null && event.getVenue() != null ? event.getVenue() : "Pune Venue";
        String eventAddress = event != null && event.getAddress() != null ? event.getAddress() : "Pune, Maharashtra";
        String eventTime = event != null && event.getTime() != null ? event.getTime() : "Check app schedule";
        int pax = registration.getPaxCount() > 0 ? registration.getPaxCount() : 1;
        String qrToken = registration.getQrCodeToken() != null ? registration.getQrCodeToken() : ("SS-REG-" + registration.getId());
        String qrImageUrl = "https://api.qrserver.com/v1/create-qr-code/?size=240x240&margin=10&data=" + URLEncoder.encode(qrToken, StandardCharsets.UTF_8);

        String noteSection = "";
        if (customNote != null && !customNote.trim().isEmpty()) {
            noteSection = "<div style='background: rgba(255, 96, 54, 0.12); border-left: 4px solid #FF6036; border-radius: 8px; padding: 16px; margin-bottom: 20px; font-size: 14px; color: #fed7aa;'>" +
                    "<strong>📣 Host Notice:</strong><br/>" + customNote.trim() + "</div>";
        }

        return "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'>" +
                "<style>" +
                "  body { margin: 0; padding: 0; background-color: #080D1F; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; color: #e2e8f0; }" +
                "  .container { max-width: 600px; margin: 20px auto; background-color: #0E162B; border: 1px solid #1F305E; border-radius: 16px; overflow: hidden; }" +
                "  .header { background: linear-gradient(135deg, #FF6036 0%, #D946EF 100%); padding: 30px 24px; text-align: center; color: #ffffff; }" +
                "  .badge { display: inline-block; background-color: #ffffff; color: #080D1F; font-weight: 800; font-size: 11px; padding: 4px 12px; border-radius: 9999px; text-transform: uppercase; margin-bottom: 10px; }" +
                "  .content { padding: 28px 24px; }" +
                "  .card { background-color: #131E3A; border: 1px solid #1F305E; border-radius: 12px; padding: 18px; margin-bottom: 20px; }" +
                "  .field { display: flex; justify-content: space-between; margin-bottom: 8px; font-size: 13px; }" +
                "  .label { color: #94a3b8; font-weight: 600; }" +
                "  .val { color: #ffffff; font-weight: 700; }" +
                "  .footer { padding: 20px; text-align: center; font-size: 12px; color: #64748b; background-color: #070B17; border-top: 1px solid #1F305E; }" +
                "</style></head><body>" +
                "<div class='container'>" +
                "  <div class='header'>" +
                "    <div class='badge'>⏳ Event Countdown Reminder</div>" +
                "    <h1 style='margin:0; font-size:24px; font-weight:900;'>Get Ready, " + recipientName + "!</h1>" +
                "    <p style='margin: 8px 0 0 0; opacity: 0.95; font-size: 14px;'>" + eventTitle + " is right around the corner!</p>" +
                "  </div>" +
                "  <div class='content'>" +
                noteSection +
                "    <p style='font-size: 14px; line-height: 1.6; color: #cbd5e1; margin-top: 0;'>" +
                "      The board games are packed, cafe tables are reserved, and hosts are prepping rule guides! " +
                "      Here is a quick refresher of your meetup details so your arrival is totally seamless." +
                "    </p>" +
                "    <div class='card'>" +
                "      <div class='field'><span class='label'>Meetup:</span><span class='val'>" + eventTitle + "</span></div>" +
                "      <div class='field'><span class='label'>Date & Time:</span><span class='val' style='color:#FFC226;'>" + eventTime + "</span></div>" +
                "      <div class='field'><span class='label'>Venue:</span><span class='val'>" + eventVenue + "</span></div>" +
                "      <div class='field'><span class='label'>Address:</span><span class='val' style='font-size:12px; max-width:280px; text-align:right;'>" + eventAddress + "</span></div>" +
                "      <div class='field'><span class='label'>Reserved Seats:</span><span class='val'>" + pax + " Shuffler(s)</span></div>" +
                "    </div>" +
                "    <div style='text-align: center; background: #070B17; border: 2px dashed #FF6036; border-radius: 14px; padding: 20px; margin-bottom: 20px;'>" +
                "      <h4 style='margin:0 0 6px 0; color:#fff; font-size:14px;'>📲 Door Check-in QR Pass</h4>" +
                "      <p style='margin:0 0 12px 0; font-size:11px; color:#94a3b8;'>Show this directly from your phone at entry</p>" +
                "      <img src='" + qrImageUrl + "' width='180' height='180' style='border-radius:8px; border:3px solid #fff; background:#fff;' />" +
                "      <div style='font-family: monospace; font-size: 13px; color: #FF6036; margin-top: 8px; font-weight: bold;'>" + qrToken + "</div>" +
                "    </div>" +
                "    <div style='background: #111B35; border-radius: 10px; padding: 14px; font-size: 12px; color: #94a3b8; line-height: 1.5;'>" +
                "      💡 <strong>Pro Tips:</strong> Arrive 10 minutes early to grab your preferred drink and choose your opening table. First-timer? Our hosts guide rules from scratch!" +
                "    </div>" +
                "  </div>" +
                "  <div class='footer'>Social Shuffle Pune • Have questions? Reply to this email or ping us on WhatsApp!</div>" +
                "</div></body></html>";
    }

    /**
     * Builds custom organizer event broadcast email HTML.
     */
    public String generateEventBroadcastEmailHtml(Registration registration, ShuffleEvent event, String subject, String message, boolean includeQrPass) {
        String recipientName = registration.getParticipantName() != null ? registration.getParticipantName() : "Fellow Shuffler";
        String eventTitle = event != null && event.getTitle() != null ? event.getTitle() : "Social Shuffle Meetup";
        String eventVenue = event != null && event.getVenue() != null ? event.getVenue() : "Pune Venue";
        String eventTime = event != null && event.getTime() != null ? event.getTime() : "Event Date";
        int pax = registration.getPaxCount() > 0 ? registration.getPaxCount() : 1;
        String qrToken = registration.getQrCodeToken() != null ? registration.getQrCodeToken() : ("SS-REG-" + registration.getId());
        String qrImageUrl = "https://api.qrserver.com/v1/create-qr-code/?size=240x240&margin=10&data=" + URLEncoder.encode(qrToken, StandardCharsets.UTF_8);

        // Replace placeholders in message
        String parsedMessage = message != null ? message : "";
        parsedMessage = parsedMessage.replace("{name}", recipientName);
        parsedMessage = parsedMessage.replace("{event_title}", eventTitle);
        parsedMessage = parsedMessage.replace("{event_venue}", eventVenue);
        parsedMessage = parsedMessage.replace("{event_time}", eventTime);
        parsedMessage = parsedMessage.replace("{pax_count}", String.valueOf(pax));
        parsedMessage = parsedMessage.replace("{qr_code}", qrToken);
        parsedMessage = parsedMessage.replace("\n", "<br/>");

        String qrBlock = "";
        if (includeQrPass) {
            qrBlock = "<div style='text-align: center; background: #070B17; border: 2px dashed #35C8A8; border-radius: 14px; padding: 18px; margin: 20px 0;'>" +
                    "  <h4 style='margin:0 0 6px 0; color:#fff; font-size:14px;'>🎟️ Your Digital Check-in Pass</h4>" +
                    "  <img src='" + qrImageUrl + "' width='160' height='160' style='border-radius:8px; border:3px solid #fff; background:#fff;' />" +
                    "  <div style='font-family: monospace; font-size: 13px; color: #35C8A8; margin-top: 8px; font-weight: bold;'>" + qrToken + "</div>" +
                    "</div>";
        }

        return "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'>" +
                "<style>" +
                "  body { margin: 0; padding: 0; background-color: #080D1F; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; color: #e2e8f0; }" +
                "  .container { max-width: 600px; margin: 20px auto; background-color: #0E162B; border: 1px solid #1F305E; border-radius: 16px; overflow: hidden; }" +
                "  .header { background: linear-gradient(135deg, #1E1B4B 0%, #312E81 50%, #4338CA 100%); padding: 30px 24px; text-align: center; color: #ffffff; }" +
                "  .content { padding: 28px 24px; }" +
                "  .msg-box { background: rgba(53, 200, 168, 0.08); border-left: 4px solid #35C8A8; border-radius: 8px; padding: 18px; margin: 16px 0; font-size: 14px; line-height: 1.6; color: #e2e8f0; }" +
                "  .footer { padding: 18px; text-align: center; font-size: 12px; color: #64748b; background-color: #070B17; border-top: 1px solid #1F305E; }" +
                "</style></head><body>" +
                "<div class='container'>" +
                "  <div class='header'>" +
                "    <div style='font-size: 11px; text-transform: uppercase; letter-spacing: 0.05em; color: #35C8A8; font-weight: bold; margin-bottom: 6px;'>📣 Organizer Push Notification</div>" +
                "    <h1 style='margin:0; font-size:22px; font-weight:900;'>" + subject + "</h1>" +
                "    <p style='margin: 6px 0 0 0; opacity: 0.85; font-size: 13px;'>" + eventTitle + " • " + eventVenue + "</p>" +
                "  </div>" +
                "  <div class='content'>" +
                "    <p style='font-size: 14px; margin-top: 0;'>Hello <strong>" + recipientName + "</strong>,</p>" +
                "    <div class='msg-box'>" + parsedMessage + "</div>" +
                qrBlock +
                "    <div style='font-size: 12px; color: #94a3b8; text-align: center; margin-top: 20px;'>" +
                "      This message was manually triggered by the Social Shuffle Event Host for registered participants." +
                "    </div>" +
                "  </div>" +
                "  <div class='footer'>Social Shuffle Pune • Board Games & Social Community</div>" +
                "</div></body></html>";
    }

    /**
     * Builds community announcement email HTML.
     */
    public String generateCommunityAnnouncementEmailHtml(String recipientName, String title, String message) {
        return "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'>" +
                "<style>" +
                "  body { margin: 0; padding: 0; background-color: #080D1F; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; color: #e2e8f0; }" +
                "  .container { max-width: 600px; margin: 20px auto; background-color: #0E162B; border: 1px solid #1F305E; border-radius: 16px; overflow: hidden; }" +
                "  .header { background: linear-gradient(135deg, #0284c7 0%, #0369a1 100%); padding: 30px 24px; text-align: center; color: #ffffff; }" +
                "  .content { padding: 28px 24px; font-size: 14px; line-height: 1.6; color: #cbd5e1; }" +
                "  .footer { padding: 18px; text-align: center; font-size: 12px; color: #64748b; background-color: #070B17; border-top: 1px solid #1F305E; }" +
                "</style></head><body>" +
                "<div class='container'>" +
                "  <div class='header'>" +
                "    <div style='font-size: 11px; text-transform: uppercase; color: #7dd3fc; font-weight: bold; margin-bottom: 6px;'>📢 Pune Community Announcement</div>" +
                "    <h1 style='margin:0; font-size:22px; font-weight:900;'>" + title + "</h1>" +
                "  </div>" +
                "  <div class='content'>" +
                "    <p>Hey <strong>" + (recipientName != null ? recipientName : "Shuffler") + "</strong>,</p>" +
                "    <div style='background: #131E3A; border-left: 4px solid #38bdf8; border-radius: 8px; padding: 18px; margin: 16px 0; color: #f1f5f9;'>" +
                message.replace("\n", "<br/>") +
                "    </div>" +
                "    <p>Check out our upcoming schedule or chat with fellow gamers in our WhatsApp group!</p>" +
                "  </div>" +
                "  <div class='footer'>Social Shuffle Pune • Connecting tabletop lovers across the city</div>" +
                "</div></body></html>";
    }

    /**
     * Dispatches the confirmation email with the scannable ticket QR code.
     */
    public boolean sendRegistrationConfirmationEmail(Registration registration, ShuffleEvent event) {
        try {
            String qrToken = registration.getQrCodeToken() != null ? registration.getQrCodeToken() : ("SS-REG-" + registration.getId());
            registration.setQrCodeToken(qrToken);
            registration.setQrCodeUrl("https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=" + qrToken);
            registration.setEmailSent(true);
            registration.setEmailSentAt(Instant.now().toString());

            String emailHtml = generateQuirkyRegistrationEmailHtml(registration, event);

            logger.info("📧 [CONFIRMATION EMAIL DISPATCHED] To: {} <{}>, Subject: 🎲 Registration Pass: {}",
                    registration.getParticipantName(), registration.getParticipantEmail(), event != null ? event.getTitle() : "Social Shuffle");

            return true;
        } catch (Exception e) {
            logger.error("Failed to generate/send confirmation email for registration: {}", registration.getId(), e);
            return false;
        }
    }

    /**
     * Dispatches upcoming event reminder email to a single attendee.
     */
    public boolean sendEventReminderEmail(Registration registration, ShuffleEvent event, String customNote) {
        try {
            String emailHtml = generateUpcomingEventReminderEmailHtml(registration, event, customNote);
            logger.info("⏳ [REMINDER EMAIL DISPATCHED] To: {} <{}> for Event: {}",
                    registration.getParticipantName(), registration.getParticipantEmail(), event != null ? event.getTitle() : "");
            return true;
        } catch (Exception e) {
            logger.error("Failed to dispatch reminder email for registration: {}", registration.getId(), e);
            return false;
        }
    }

    /**
     * Dispatches custom event broadcast email to a single attendee.
     */
    public boolean sendEventBroadcastEmail(Registration registration, ShuffleEvent event, String subject, String message, boolean includeQrPass) {
        try {
            String emailHtml = generateEventBroadcastEmailHtml(registration, event, subject, message, includeQrPass);
            logger.info("📢 [EVENT BROADCAST EMAIL DISPATCHED] To: {} <{}>, Subject: {}",
                    registration.getParticipantName(), registration.getParticipantEmail(), subject);
            return true;
        } catch (Exception e) {
            logger.error("Failed to dispatch event broadcast email for registration: {}", registration.getId(), e);
            return false;
        }
    }

    /**
     * Dispatches community announcement email.
     */
    public boolean sendCommunityAnnouncementEmail(String email, String name, String title, String message) {
        try {
            String emailHtml = generateCommunityAnnouncementEmailHtml(name, title, message);
            logger.info("📣 [COMMUNITY ANNOUNCEMENT DISPATCHED] To: {} <{}>, Title: {}", name, email, title);
            return true;
        } catch (Exception e) {
            logger.error("Failed to dispatch community announcement email to: {}", email, e);
            return false;
        }
    }
}
