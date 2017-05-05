package models;

import java.io.Serializable;

public class ClassTime implements Serializable {

    private String roomNumber;
    private int dayOfWeek;
    private int startSlot;
    private int endSlot;

    public ClassTime(String roomNumber, int dayOfWeek, int startSlot, int endSlot) {
        this.roomNumber = roomNumber;
        this.dayOfWeek = dayOfWeek;
        this.startSlot = startSlot;
        this.endSlot = endSlot;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public int getStartSlot() {
        return startSlot;
    }

    public int getEndSlot() {
        return endSlot;
    }
}
