package edu.connexion3a8.services;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Random;

public class EmailService {

    private final String smtpHost;
    private final int smtpPort;
    private final String emailFrom;
    private final String appPassword;
    private String currentOtp;

    public EmailService() {
        Properties config = loadConfig();
        this.smtpHost = config.getProperty("smtp.host", "smtp.gmail.com");
        this.smtpPort = Integer.parseInt(config.getProperty("smtp.port", "587"));
        this.emailFrom = config.getProperty("smtp.email");
        this.appPassword = config.getProperty("smtp.password");
    }

    private Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream is = getClass().getResourceAsStream("/config.properties")) {
            if (is == null) {
                throw new RuntimeException("config.properties not found! Copy config.properties.example to config.properties and fill in your credentials.");
            }
            props.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }
        return props;
    }

    public String generateOtp() {
        currentOtp = String.format("%06d", new Random().nextInt(999999));
        return currentOtp;
    }

    public boolean verifyOtp(String inputOtp) {
        return currentOtp != null && currentOtp.equals(inputOtp);
    }

    public void sendOtpEmail(String toEmail, String otp) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", String.valueOf(smtpPort));

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(emailFrom, appPassword);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(emailFrom));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject("INVESTI - Email Verification Code");
        message.setContent(
            "<div style='font-family:Arial,sans-serif;max-width:480px;margin:auto;padding:30px;border:1px solid #e0e0e0;border-radius:12px;'>" +
            "<h2 style='color:#456990;text-align:center;'>INVESTI</h2>" +
            "<p style='text-align:center;color:#333;'>Your verification code is:</p>" +
            "<h1 style='text-align:center;letter-spacing:8px;color:#9B7E46;font-size:36px;'>" + otp + "</h1>" +
            "<p style='text-align:center;color:#888;font-size:13px;'>This code expires in 5 minutes.</p>" +
            "</div>",
            "text/html; charset=utf-8"
        );

        Transport.send(message);
    }
}
