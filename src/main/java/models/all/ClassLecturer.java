package models.all;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.Serializable;

public class ClassLecturer implements Serializable{

    private String lecturerID;
    private String firstName;
    private String lastName;
    private String contactNumber;
    private String email;
    private byte[] lecturerImage;

    public ClassLecturer(String lecturerID, String firstName, String lastName, String contactNumber, String email, byte[] lecturerImage) {
        this.lecturerID = lecturerID;
        this.firstName = firstName;
        this.lastName = lastName;
        this.contactNumber = contactNumber;
        this.email = email;
        this.lecturerImage = lecturerImage;
    }

    public String getLecturerID() {
        return lecturerID;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public String getEmail() {
        return email;
    }

    public Image getLecturerImage() {
        if (lecturerImage != null) {
            try {
                return SwingFXUtils.toFXImage(ImageIO.read(new ByteArrayInputStream(lecturerImage)), null);
            } catch (Exception ex) {
            }
        }
        return null;
    }
}
