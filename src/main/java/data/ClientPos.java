package data;

public class ClientPos {
    private double latitude;    // Latitude
    private double longitude;   // Longitude
    private boolean isIndoor;   // Whether the position is indoor
    private int floor;  // Floor

    public ClientPos() {
    }

    public ClientPos(double longitude, double latitude, boolean isIndoor) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.isIndoor = isIndoor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public void setIsIndoor(boolean indoor) {
        isIndoor = indoor;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public boolean getIsIndoor() {
        return isIndoor;
    }

    public int getFloor() {
        return floor;
    }

    @Override
    public String toString() {
        return latitude + "," + longitude;
    }
}
