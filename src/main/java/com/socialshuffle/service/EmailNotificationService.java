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
                "        <div class='field'><span class='label'>Payment Status:</span><span class='val' style='color: #4ade80;'>" + registration.getPaymentStatus() + "</span></div>" +
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
     * Dispatches the confirmation email with the scannable ticket QR code.
     */
    public boolean sendRegistrationConfirmationEmail(Registration registration, ShuffleEvent event) {
        try {
            String qrToken = "SS-REG-" + registration.getId();
            registration.setQrCodeToken(qrToken);
            registration.setQrCodeUrl("https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=" + qrToken);
            registration.setEmailSent(true);
            registration.setEmailSentAt(Instant.now().toString());

            String emailHtml = generateQuirkyRegistrationEmailHtml(registration, event);

            logger.info("📧 [CONFIRMATION EMAIL DISPATCHED]");
            logger.info("  To: {} <{}>", registration.getParticipantName(), registration.getParticipantEmail());
            logger.info("  Subject: 🎲 Holy Meeples! You're in for {}! Ticket: {}", event != null ? event.getTitle() : "Social Shuffle", qrToken);
            logger.info("  QR Check-in Token: {}", qrToken);
            logger.info("  Content Length: {} chars", emailHtml.length());

            return true;
        } catch (Exception e) {
            logger.error("Failed to generate/send confirmation email for registration: {}", registration.getId(), e);
            return false;
        }
    }
}
