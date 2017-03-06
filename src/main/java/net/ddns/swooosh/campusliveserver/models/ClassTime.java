package net.ddns.swooosh.campusliveserver.models;

import java.io.Serializable;

public class ClassTime implements Serializable {

    private int classTimeID;
    private String roomNumber;
    private int dayOfWeek;
    private int startSlot;
    private int endSlot;

    public ClassTime(int classTimeID, String roomNumber, int dayOfWeek, int startSlot, int endSlot) {
        this.classTimeID = classTimeID;
        this.roomNumber = roomNumber;
        this.dayOfWeek = dayOfWeek;
        this.startSlot = startSlot;
        this.endSlot = endSlot;
    }

    public int getClassTimeID() {
        return classTimeID;
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
