package net.ddns.swooosh.campusliveserver.main;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class Email {

    public Boolean emailPassword(String username, String email, String password) {
        try {
            Properties props = System.getProperties();
            props.put("mail.smtp.starttls.enable", true);
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.user", "campuslive.recovery@gmail.com");
            props.put("mail.smtp.password", "campus.live");
            props.put("mail.smtp.port", "587");
            props.put("mail.smtp.auth", true);
            Session session = Session.getInstance(props, null);
            Transport transport = session.getTransport("smtp");
            transport.connect("smtp.gmail.com", "campuslive.recovery", "campus.live");
            MimeMessage message = new MimeMessage(session);
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            message.setSubject("CampusLive - Forgot password");
            message.setText("Dear " + username + "\nYou requested to send your CampusLive password to your email.\nIf this wasn't you please contact us.\n\nPassword:\t" + password);
            System.out.println("test1");
            transport.sendMessage(message, message.getAllRecipients());
            System.out.println("test2");
            return true;
        } catch (Exception ex) {
            System.out.println("Server> " + ex);
            return false;
        }
    }

}
