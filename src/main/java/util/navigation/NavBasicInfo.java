package util.navigation;

/**
 * Navigation basic information.
 */
public class NavBasicInfo {
    private String desAreaName; // Destination area
    private double lat; // Starting point latitude
    private double lon; // Starting point longitude
    private int floor; // Floor
    private boolean indoor; // Whether the starting point is indoors

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public int getFloor() {
        return floor;
    }

    public String getDesAreaName() {
        return desAreaName;
    }

    public boolean isIndoor() {
        return indoor;
    }

    public void setIndoor(boolean indoor) {
        this.indoor = indoor;
    }

    public void setDesAreaName(String desAreaName) {
        this.desAreaName = desAreaName;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }
}
