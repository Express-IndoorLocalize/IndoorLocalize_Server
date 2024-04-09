package olddata;

/**
 * This class is used to store accelerometer data.
 * Author: Qchrx
 */
public class Accelerometer extends PositionData {
    public Accelerometer(int equipmentID, double accX, double accY, double accZ) {
        super(equipmentID, accX, accY, accZ);
    }

    public Accelerometer(int equipmentID, Double[] arrayAcc) {
        super(equipmentID, arrayAcc);
    }

    public Accelerometer(int id, int equipmentID, double accX, double accY, double accZ) {
        super(id, equipmentID, accX, accY, accZ);
    }

    public Accelerometer(int id, int equipmentID, Double[] arrayAcc) {
        super(id, equipmentID, arrayAcc);
    }
}
