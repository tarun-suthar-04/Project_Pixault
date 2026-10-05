import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class TestJavaMail {
    public static void main(String[] args) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "465");
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.connectiontimeout", "5000");
            props.put("mail.smtp.timeout", "5000");
            
            Session session = Session.getInstance(props);
            session.setDebug(true);
            
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("test@example.com"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse("test@example.com"));
            message.setSubject("Test");
            message.setContent("Test", "text/html; charset=utf-8");
            
            System.out.println("Getting transport...");
            try (Transport transport = session.getTransport("smtp")) {
                System.out.println("Connecting...");
                transport.connect("smtp.gmail.com", 465, "", "");
                System.out.println("Sending...");
                transport.sendMessage(message, message.getAllRecipients());
                System.out.println("Done!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
