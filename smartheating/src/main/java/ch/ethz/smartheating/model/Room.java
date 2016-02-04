package ch.ethz.smartheating.model;

/**
 * A {@link Room} object consists of a temperature, a name and the local ID in the database.
 */
public class Room {
    private String name;
    private Integer ID;
    private double temperature;

    public Room(String name, Integer ID, double temperature) {
        this.name = name;
        this.ID = ID;
        this.temperature = temperature;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }
}
