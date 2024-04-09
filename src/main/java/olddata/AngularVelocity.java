package olddata;

/**
 * This class is used to store angular velocity data.
 * Author: Qchrx
 */
public class AngularVelocity extends PositionData {

    public AngularVelocity(int equipmentID, double angX, double angY, double angZ) {
        super(equipmentID, angX, angY, angZ);
    }

    public AngularVelocity(int equipmentID, Double[] arrayAng) {
        super(equipmentID, arrayAng);
    }

    public AngularVelocity(int id, int equipmentID, double angX, double angY, double angZ) {
        super(id, equipmentID, angX, angY, angZ);
    }

    public AngularVelocity(int id, int equipmentID, Double[] arrayAng) {
        super(id, equipmentID, arrayAng);
    }
}
