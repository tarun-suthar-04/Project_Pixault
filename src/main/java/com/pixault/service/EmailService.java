package com.pixault.service;

import java.util.Properties;

import com.pixault.config.ConfigLoader;

import jakarta.mail.*;
import jakarta.mail.internet.*;

public class EmailService {

    private static final String FROM_EMAIL = ConfigLoader.get("mail.username");
    private static final String APP_PASSWORD = ConfigLoader.get("mail.password");

    public static void sendOTP(String toEmail, String otp) {
        Properties props = new Properties();

        props.put("mail.smtp.host", ConfigLoader.get("mail.smtp.host"));
        // props.put("mail.smtp.port", "465");
        props.put("mail.smtp.port", ConfigLoader.get("mail.smtp.port"));
        props.put("mail.smtp.auth", ConfigLoader.get("mail.auth"));
        props.put("mail.smtp.starttls.enable", ConfigLoader.get("mail.starttls"));
        // props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.connectiontimeout", ConfigLoader.get("mail.connectiontimeout"));
        props.put("mail.smtp.timeout", ConfigLoader.get("mail.timeout"));
        props.put("mail.smtp.writetimeout", ConfigLoader.get("mail.writetimeout"));
        props.put("mail.debug", ConfigLoader.get("mail.debug"));

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, APP_PASSWORD);
            }
        });

        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(FROM_EMAIL));

            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            msg.setSubject("Pixault OTP Verification");
            msg.setText("Your Pixault OTP is : " + otp);
            Transport.send(msg);
            System.out.println("OTP send successfully");
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

}
