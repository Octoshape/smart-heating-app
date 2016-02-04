package ch.ethz.smartheating.model;

/**
 * A {@link Thermostat} object consists of a temperature, an RFID, a room id determining which room
 * it belongs to and its name.
 */
public class Thermostat {
    private String RFID;
    private int roomID;
    private String name;
    private double temperature;

    public Thermostat(String RFID, int roomID, String name, double temperature) {
        this.RFID = RFID;
        this.roomID = roomID;
        this.name = name;
        this.temperature = temperature;
    }

    public String getRFID() {
        return RFID;
    }

    public void setRFID(String RFID) {
        this.RFID = RFID;
    }

    public int getRoomID() {
        return roomID;
    }

    public void setRoomID(int roomID) {
        this.roomID = roomID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }
}
