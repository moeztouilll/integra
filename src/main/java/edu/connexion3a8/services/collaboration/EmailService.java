package edu.connexion3a8.services.collaboration;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

/**
 * EmailService — sends email notifications via Gmail SMTP.
 *
 * ⚠️ SETUP: Replace SENDER_EMAIL and SENDER_PASSWORD with your Gmail
 * credentials.
 * For Gmail, use an "App Password" (not your regular password):
 * -> Google Account -> Security -> 2-Step Verification -> App Passwords
 *
 * This service is called on:
 * - Investment ACCEPTED (email to entrepreneur + investor)
 * - Investment REFUSED (email to investor)
 * - Project VALIDATED (email to entrepreneur)
 */
public class EmailService {

        // ─── CONFIG: Change these to real values ───────────────────────────────────
        private static final String SENDER_EMAIL = "investi.platform2000@gmail.com";
        private static final String SENDER_PASSWORD = "sach nzim glfo oaui"; // Gmail App Password
        private static final boolean EMAIL_ENABLED = true;
        // ──────────────────────────────────────────────────────────────────────────

        private static Session createSession() {
                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.port", "587");
                props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

                return Session.getInstance(props, new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                                return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
                        }
                });
        }

        /**
         * Core method — sends an email. Silently logs if EMAIL_ENABLED is false.
         */
        private static void send(String toEmail, String subject, String body) {
                if (!EMAIL_ENABLED) {
                        System.out.println("[EmailService] (Disabled) Would send email to: " + toEmail);
                        System.out.println("  Subject: " + subject);
                        System.out.println("  Body: " + body);
                        return;
                }
                try {
                        Session session = createSession();
                        Message message = new MimeMessage(session);
                        message.setFrom(new InternetAddress(SENDER_EMAIL, "Investi Platform"));
                        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
                        message.setSubject(subject);

                        // HTML body
                        message.setContent(buildHtmlBody(subject, body), "text/html; charset=utf-8");

                        Transport.send(message);
                        System.out.println("[EmailService] Email sent to: " + toEmail);

                        javafx.application.Platform.runLater(() -> {
                                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                                                javafx.scene.control.Alert.AlertType.INFORMATION);
                                alert.setTitle("Email Sent");
                                alert.setHeaderText(null);
                                alert.setContentText("Notification email successfully sent to: " + toEmail);
                                alert.show(); // Use show() to not block
                        });

                } catch (Exception e) {
                        System.err.println("[EmailService] Failed to send email: " + e.getMessage());
                        javafx.application.Platform.runLater(() -> {
                                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                                                javafx.scene.control.Alert.AlertType.ERROR);
                                alert.setTitle("Email Failed");
                                alert.setHeaderText(null);
                                alert.setContentText("Failed to send email to " + toEmail + "\nReason: "
                                                + e.getMessage());
                                alert.show();
                        });
                }
        }

        private static String buildHtmlBody(String title, String message) {
                return "<!DOCTYPE html><html><body style=\"margin:0;padding:0;background-color:#F7F0F5;font-family:'Helvetica Neue', Helvetica, Arial, sans-serif;\">"
                                + "<table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color:#F7F0F5;padding:40px 20px;\">"
                                + "<tr><td align=\"center\">"
                                + "<table width=\"600\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color:#ffffff;border-radius:16px;box-shadow:0 10px 25px rgba(0,0,0,0.05);overflow:hidden;\">"
                                + "<!-- Header -->"
                                + "<tr><td style=\"background:linear-gradient(135deg, #456990 0%, #304a66 100%);padding:40px;text-align:center;\">"
                                + "<h1 style=\"color:#ffffff;margin:0;font-size:28px;letter-spacing:1px;font-weight:700;\">INVESTI</h1>"
                                + "<p style=\"color:#c2d6eb;margin:10px 0 0 0;font-size:14px;letter-spacing:2px;text-transform:uppercase;\">Premium Platform</p>"
                                + "</td></tr>"
                                + "<!-- Content -->"
                                + "<tr><td style=\"padding:40px;\">"
                                + "<h2 style=\"color:#000501;font-size:22px;margin:0 0 20px 0;\">" + title + "</h2>"
                                + "<p style=\"color:#64748b;font-size:16px;line-height:1.6;margin:0 0 30px 0;\">"
                                + message + "</p>"
                                + "<table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\"><tr><td align=\"center\">"
                                + "<a href=\"#\" style=\"background-color:#A62639;color:#ffffff;text-decoration:none;padding:14px 32px;border-radius:30px;font-weight:bold;font-size:14px;text-transform:uppercase;letter-spacing:1px;display:inline-block;\">Log in to Investi</a>"
                                + "</td></tr></table>"
                                + "</td></tr>"
                                + "<!-- Footer -->"
                                + "<tr><td style=\"background-color:#f8fafc;padding:30px;text-align:center;border-top:1px solid #e2e8f0;\">"
                                + "<p style=\"color:#94a3b8;font-size:12px;margin:0;\">This is an automated notification from Investi Platform.</p>"
                                + "<p style=\"color:#94a3b8;font-size:12px;margin:10px 0 0 0;\">&copy; 2026 Investi. All rights reserved.</p>"
                                + "</td></tr>"
                                + "</table>"
                                + "</td></tr></table>"
                                + "</body></html>";
        }

        // ─── Public notification methods ────────────────────────────────────────────

        /**
         * Called when an entrepreneur ACCEPTS an investment offer.
         */
        public static void sendInvestmentAccepted(String entrepreneurEmail, String investorEmail,
                        String projectTitle, double amount) {
                // Email to entrepreneur
                send(entrepreneurEmail,
                                "🎉 Investment Accepted — " + projectTitle,
                                "Congratulations! You have accepted an investment of <strong>$"
                                                + String.format("%.2f", amount)
                                                + "</strong> for your project <strong>" + projectTitle + "</strong>. "
                                                + "Your project is now marked as <strong>FUNDED</strong>.");

                // Email to investor
                send(investorEmail,
                                "✅ Your Investment Was Accepted — " + projectTitle,
                                "Great news! Your investment of <strong>$" + String.format("%.2f", amount)
                                                + "</strong> in the project <strong>" + projectTitle
                                                + "</strong> has been <strong>ACCEPTED</strong> by the entrepreneur.");
        }

        /**
         * Called when an entrepreneur REFUSES an investment offer.
         */
        public static void sendInvestmentRefused(String investorEmail, String projectTitle, double amount) {
                send(investorEmail,
                                "❌ Investment Offer Declined — " + projectTitle,
                                "Unfortunately, your investment offer of <strong>$" + String.format("%.2f", amount)
                                                + "</strong> for project <strong>" + projectTitle
                                                + "</strong> was not accepted at this time. "
                                                + "Keep exploring other opportunities on Investi!");
        }

        /**
         * Called when an admin VALIDATES a project (status -> OPEN).
         */
        public static void sendProjectValidated(String entrepreneurEmail, String projectTitle) {
                send(entrepreneurEmail,
                                "✅ Project Validated — " + projectTitle,
                                "Your project <strong>" + projectTitle
                                                + "</strong> has been reviewed and approved by our admin team. "
                                                + "It is now <strong>OPEN</strong> for investors to discover and fund!");
        }

        /**
         * Called when an admin REJECTS a project.
         */
        public static void sendProjectRejected(String entrepreneurEmail, String projectTitle) {
                send(entrepreneurEmail,
                                "❌ Project Rejected — " + projectTitle,
                                "Unfortunately, your project <strong>" + projectTitle
                                                + "</strong> was not approved in its current form. "
                                                + "Please review our guidelines and consider resubmitting with improvements.");
        }

        /**
         * Called when an investor CREATES a new investment offer.
         */
        public static void sendNewInvestmentOffer(String entrepreneurEmail, String projectTitle, double amount) {
                send(entrepreneurEmail,
                                "💰 New Investment Offer Received! — " + projectTitle,
                                "Great news! Your project <strong>" + projectTitle
                                                + "</strong> just received a new investment offer of <strong>$"
                                                + String.format("%.2f", amount) + "</strong>. "
                                                + "<br><br>Log in to your Investi dashboard to review and accept or decline the offer.");
        }

        /**
         * Called at the start of the month to remind an investor to pay.
         */
        public static void sendPaymentReminder(String investorEmail, String projectTitle, double amountDue) {
                send(investorEmail,
                                "🔔 Payment Reminder — " + projectTitle,
                                "Hello Investor!<br>This is a polite reminder that your monthly milestone payment of <strong>$"
                                                + String.format("%.2f", amountDue)
                                                + "</strong> for the project <strong>" + projectTitle
                                                + "</strong> is due.<br>Please log in to your dashboard to complete the payment.");
        }

        /**
         * Called when an investor marks a monthly payment as done.
         */
        public static void sendPaymentConfirmation(String entrepreneurEmail, String investorEmail, String projectTitle,
                        double amount) {
                // Email to entrepreneur
                send(entrepreneurEmail,
                                "💰 Payment Received — " + projectTitle,
                                "Good news! An investor has just fulfilled their monthly payment of <strong>$"
                                                + String.format("%.2f", amount)
                                                + "</strong> for your project <strong>" + projectTitle + "</strong>.");

                // Email to investor
                send(investorEmail,
                                "✅ Payment Confirmed — " + projectTitle,
                                "Your monthly payment of <strong>$" + String.format("%.2f", amount)
                                                + "</strong> for project <strong>" + projectTitle
                                                + "</strong> has been successfully processed. Thank you for your commitment!");
        }

        /**
         * Called when an admin approve an investment (UNDER_REVIEW -> PENDING).
         */
        public static void sendInvestmentApprovedByAdmin(String investorEmail, String projectTitle) {
                send(investorEmail,
                                "✅ Investment Offer Approved by Admin — " + projectTitle,
                                "Your investment offer for project <strong>" + projectTitle
                                                + "</strong> has passed our administrative review. "
                                                + "It has now been sent to the entrepreneur for their consideration.");
        }

        /**
         * Called when an investor is late on their monthly payment.
         */
        public static void sendLatePaymentWarning(String investorEmail, String projectTitle, double amountDue) {
                send(investorEmail,
                                "⚠️ URGENT: Payment Overdue — " + projectTitle,
                                "Our records show that your monthly payment of <strong>$"
                                                + String.format("%.2f", amountDue)
                                                + "</strong> for project <strong>" + projectTitle
                                                + "</strong> is now <strong>OVERDUE</strong> (past the 7-day grace period from your investment anniversary). "
                                                + "<br><br>Please log in and fulfill your commitment to keep the project healthy.");
        }

        /**
         * Called when an entrepreneur adds a new milestone to a collaboration.
         */
        public static void sendMilestoneAdded(String investorEmail, String projectTitle, String milestoneTitle) {
                send(investorEmail,
                                "📌 New Milestone Added — " + projectTitle,
                                "The entrepreneur has added a new milestone: <strong>" + milestoneTitle
                                                + "</strong> to the project <strong>" + projectTitle + "</strong>. "
                                                + "<br><br>Log in to track the progress of this new objective.");
        }

        /**
         * Called when an entrepreneur marks a milestone as completed.
         */
        public static void sendMilestoneCompleted(String investorEmail, String projectTitle, String milestoneTitle) {
                send(investorEmail,
                                "✅ Milestone Completed! — " + projectTitle,
                                "Great news! The milestone <strong>" + milestoneTitle
                                                + "</strong> for project <strong>" + projectTitle
                                                + "</strong> has been marked as <strong>COMPLETED</strong>. "
                                                + "<br><br>This contributes to the overall progress of your investment.");
        }
}
