package edu.connexion3a8.services.gamification;

import edu.connexion3a8.entities.gamification.Badge;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

/**
 * Email notification service for sending badge achievement emails
 */
public class EmailService {
    
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String FROM_EMAIL = "investi.platform2000@gmail.com"; // TODO: Configure
    private static final String FROM_PASSWORD = "sach nzim glfo oaui"; // TODO: Configure (use App Password, not regular password)
    private static final String FROM_NAME = "INVESTI";
    
    private Session session;
    
    public EmailService() {
        setupSession();
    }
    
    /**
     * Setup email session with SMTP configuration
     */
    private void setupSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.ssl.trust", SMTP_HOST);
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        
        session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, FROM_PASSWORD);
            }
        });
    }
    
    /**
     * Send badge achievement email to user
     * @param userEmail User's email address
     * @param userName User's name
     * @param badge Badge that was earned
     */
    public void sendBadgeAchievementEmail(String userEmail, String userName, Badge badge) {
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL, FROM_NAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(userEmail));
            message.setSubject("🏆 Congratulations! You've Earned a New Badge!");
            
            String htmlContent = buildBadgeEmailHTML(userName, badge);
            message.setContent(htmlContent, "text/html; charset=utf-8");
            
            // Send email in background thread to avoid blocking UI
            new Thread(() -> {
                try {
                    Transport.send(message);
                    System.out.println("✅ Badge achievement email sent to: " + userEmail);
                } catch (MessagingException e) {
                    System.err.println("❌ Failed to send email: " + e.getMessage());
                    e.printStackTrace();
                }
            }).start();
            
        } catch (Exception e) {
            System.err.println("❌ Error preparing email: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Build HTML email content for badge achievement
     */
    private String buildBadgeEmailHTML(String userName, Badge badge) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<style>" +
                "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f6fa; margin: 0; padding: 0; }" +
                ".container { max-width: 600px; margin: 40px auto; background-color: white; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.1); }" +
                ".header { background: linear-gradient(135deg, #FFD700 0%, #FFA500 100%); padding: 40px 20px; text-align: center; }" +
                ".trophy { font-size: 80px; margin: 0; }" +
                ".header h1 { color: white; margin: 10px 0 0 0; font-size: 28px; text-shadow: 2px 2px 4px rgba(0,0,0,0.2); }" +
                ".content { padding: 40px 30px; }" +
                ".greeting { font-size: 18px; color: #333; margin-bottom: 20px; }" +
                ".badge-card { background: linear-gradient(135deg, #456990 0%, #2C4A5E 100%); border-radius: 10px; padding: 30px; text-align: center; margin: 30px 0; }" +
                ".badge-icon { font-size: 60px; margin-bottom: 15px; }" +
                ".badge-name { color: #FFD700; font-size: 32px; font-weight: bold; margin: 10px 0; text-shadow: 2px 2px 4px rgba(0,0,0,0.3); }" +
                ".badge-description { color: #F7F0F5; font-size: 16px; margin: 15px 0; line-height: 1.6; }" +
                ".badge-points { background-color: rgba(0,0,0,0.3); color: #FFD700; padding: 12px 24px; border-radius: 25px; display: inline-block; margin-top: 15px; font-weight: 600; }" +
                ".message { color: #555; font-size: 16px; line-height: 1.8; margin: 20px 0; }" +
                ".cta-button { display: inline-block; background: linear-gradient(135deg, #9B7E46 0%, #C8A84E 100%); color: white; padding: 15px 40px; text-decoration: none; border-radius: 8px; font-weight: bold; margin: 20px 0; box-shadow: 0 4px 15px rgba(155,126,70,0.3); }" +
                ".cta-button:hover { background: linear-gradient(135deg, #C8A84E 0%, #E4C45E 100%); }" +
                ".footer { background-color: #f8f9fa; padding: 20px; text-align: center; color: #666; font-size: 14px; border-top: 1px solid #e0e0e0; }" +
                ".footer a { color: #456990; text-decoration: none; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<div class='trophy'>🏆</div>" +
                "<h1>Badge Unlocked!</h1>" +
                "</div>" +
                "<div class='content'>" +
                "<p class='greeting'>Hi " + userName + ",</p>" +
                "<p class='message'>Congratulations! You've just earned a new badge on your learning journey. Your dedication and hard work have paid off!</p>" +
                "<div class='badge-card'>" +
                "<div class='badge-icon'>🎖️</div>" +
                "<div class='badge-name'>" + badge.getName() + "</div>" +
                "<div class='badge-description'>" + badge.getDescription() + "</div>" +
                "<div class='badge-points'>⭐ " + badge.getPointsRequired() + " Points Required</div>" +
                "</div>" +
                "<p class='message'>Keep up the excellent work! Continue learning and unlock even more achievements.</p>" +
                "<center>" +
                "<a href='#' class='cta-button'>View Your Badges</a>" +
                "</center>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>This is an automated message from Learning Platform</p>" +
                "<p>© 2026 Learning Platform. All rights reserved.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
    
    /**
     * Test email configuration
     */
    public boolean testConnection() {
        try {
            Transport transport = session.getTransport("smtp");
            transport.connect(SMTP_HOST, FROM_EMAIL, FROM_PASSWORD);
            transport.close();
            System.out.println("✅ Email configuration is valid");
            return true;
        } catch (Exception e) {
            System.err.println("❌ Email configuration test failed: " + e.getMessage());
            return false;
        }
    }
}
