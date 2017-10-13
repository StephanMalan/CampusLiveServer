package models.all;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.Serializable;

public class ContactDetails implements Serializable {

    private String name;
    private String position;
    private String contactNumber;
    private String email;
    private byte[] imageBytes;

    public ContactDetails(String name, String position, String contactNumber, String email, byte[] imageBytes) {
        this.name = name;
        this.position = position;
        this.contactNumber = contactNumber;
        this.email = email;
        this.imageBytes = imageBytes;
    }

    public String getName() {
        return name;
    }

    public String getPosition() {
        return position;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public String getEmail() {
        return email;
    }

    public Image getImage() {
        try {
            return SwingFXUtils.toFXImage(ImageIO.read(new ByteArrayInputStream(imageBytes)), null);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
