package algorithm;

import data.WifiDataContainer;
import olddata.WifiFingerBssidContainer;
import olddata.SpatialPosition;
import util.database.DataPreHandle;
import util.database.DatabaseOperation;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * This class implements the following positioning algorithms:
 * NN, K-NN, W-K-NN, Kalman Filter
 */
public class PositionAlgorithm {
    private DatabaseOperation databaseOperation;    // Database operation utility class object
    private WifiFingerBssidContainer wifiFingerBssidContainer;  // Container object for the RSSID sequence used for Wifi positioning
    private DataPreHandle dataPreHandle;
    public final static int K_NN = 1;
    public final static int W_K_NN = 2;

    /**
     * Constructor of PositionAlgorithm
     *
     * @param databaseOperation        Database operation utility class object
     * @param wifiFingerBssidContainer Container for the RSSID sequence used for Wifi positioning
     */
    
    // TEST COMMENT 101
    public PositionAlgorithm(DatabaseOperation databaseOperation, WifiFingerBssidContainer wifiFingerBssidContainer) {
        this.databaseOperation = databaseOperation;
        this.wifiFingerBssidContainer = wifiFingerBssidContainer;
        this.dataPreHandle = new DataPreHandle();
    }

    /**
     * Positioning based on Wifi signal strength using the NN algorithm
     *
     * @param bssidList List of BSSID of wifi data collected by the client
     * @param levelList List of signal strengths collected by the client
     * @return SpatialPosition object encapsulating the positioning result
     * @throws SQLException SQL exception, thrown upwards
     */
    public SpatialPosition nnLocate(List<String> bssidList, List<Float> levelList) throws SQLException {
        return wknnLocate(bssidList, levelList, 1, K_NN);
    }

    /**
     * Positioning based on Wifi signal strength using the K-NN algorithm
     *
     * @param bssidList   List of BSSID of wifi data collected by the client
     * @param levelList   List of signal strengths collected by the client
     * @param resultCount Number of positioning results to be filtered out
     * @return SpatialPosition object encapsulating the positioning result
     * @throws SQLException SQL exception, thrown upwards
     */
    public SpatialPosition knnLocate(List<String> bssidList, List<Float> levelList, int resultCount) throws SQLException {
        return wknnLocate(bssidList, levelList, resultCount, K_NN);
    }

    /**
     * Positioning based on Wifi signal strength using the W-K-NN algorithm
     *
     * @param bssidList   List of BSSID of wifi data collected by the client
     * @param levelList   List of signal strengths collected by the client
     * @param resultCount Number of positioning results to be filtered out
     * @return SpatialPosition object encapsulating the positioning result
     * @throws SQLException SQL exception, thrown upwards
     */
    public SpatialPosition wknnLocate(List<String> bssidList, List<Float> levelList,
                                       int resultCount, int type) throws SQLException {
        List<String> wifiFingerBssidList = wifiFingerBssidContainer.getWifiFingerBssidList();
        WifiDataContainer wifiDataContainer =
                dataPreHandle.preHandleWifiData(bssidList, levelList, wifiFingerBssidList); // Preprocess the collected Wifi data

        // Convert lists to arrays for subsequent operations
        String[] bssidArray = wifiDataContainer.getBssidArray();
        Float[] levelArray = wifiDataContainer.getLevelArray();
        // SQL statement for W-K-NN algorithm
        String sqlString = "SELECT w7.x_pos,w7.y_pos,w7.floor,w7.level+w8.level AS level FROM \n" +
                "(SELECT w5.x_pos,w5.y_pos,w5.floor,w5.level+w6.level AS level FROM \n" +
                "(SELECT w3.x_pos,w3.y_pos,w3.floor,w3.level+w4.level AS level FROM \n" +
                "(SELECT w1.x_pos,w1.y_pos,w1.floor,w1.level+w2.level AS level FROM \n" +
                "(SELECT x_pos,y_pos,floor,(wifi_level-(" + levelArray[0] + "))*(wifi_level-(" + levelArray[0] + ")) as level \n" +
                "FROM wifi_finger WHERE rssid = '" + bssidArray[0] + "') AS w1 JOIN \n" +
                "(SELECT x_pos,y_pos,floor, (wifi_level-(" + levelArray[1] + "))*(wifi_level-(" + levelArray[1] + ")) as level \n" +
                "FROM wifi_finger WHERE rssid = '" + bssidArray[1] + "') AS w2 \n" +
                "ON (w1.x_pos = w2.x_pos AND w1.y_pos = w2.y_pos)) AS w3 JOIN \n" +
                "(SELECT x_pos,y_pos,floor,(wifi_level-(" + levelArray[2] + "))*(wifi_level-(" + levelArray[2] + ")) as level \n" +
                "FROM wifi_finger WHERE rssid = '" + bssidArray[2] + "') AS w4 \n" +
                "ON (w3.x_pos = w4.x_pos AND w3.y_pos=w4.y_pos)) AS w5 JOIN \n" +
                "(SELECT x_pos,y_pos,floor,(wifi_level-(" + levelArray[3] + "))*(wifi_level-(" + levelArray[3] + ")) as level \n" +
                "FROM wifi_finger WHERE rssid = '" + bssidArray[3] + "') AS w6 \n" +
                "ON (w5.x_pos = w6.x_pos AND w5.y_pos=w6.y_pos)) AS w7 JOIN \n" +
                "(SELECT x_pos,y_pos,floor,(wifi_level-(" + levelArray[4] + "))*(wifi_level-(" + levelArray[4] + ")) as level \n" +
                "FROM wifi_finger WHERE rssid = '" + bssidArray[4] + "') AS w8 \n" +
                "ON (w7.x_pos = w8.x_pos AND w7.y_pos=w8.y_pos) " + " ORDER BY level";

        Connection connection = databaseOperation.getConnection();
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sqlString);
        SpatialPosition spatialPosition;
        int labelCount = 1;
        int floor = 0;
        List<SpatialPosition> spList = new ArrayList<>();
        while (rs.next() && labelCount <= resultCount) {
            double x = rs.getDouble("x_pos");
            double y = rs.getDouble("y_pos");
            float levelVar = rs.getFloat("level");
            floor = rs.getInt("floor");
            spatialPosition = new SpatialPosition(x, y, floor, levelVar);
            spList.add(spatialPosition);
            labelCount++;
        }
        float finalX = 0;
        float finalY = 0;
        float finalVar = 0;
        switch (type) {
            case K_NN:
                for (SpatialPosition aSpList : spList) {
                    finalX += aSpList.getXPos();
                    finalY += aSpList.getYPos();
                }
                return new SpatialPosition(finalX / spList.size(), finalY / spList.size(), floor);
            case W_K_NN:
                for (SpatialPosition aSpList : spList) {
                    finalX += aSpList.getXPos() * Math.sqrt(aSpList.getLevelVar());
                    finalY += aSpList.getYPos() * Math.sqrt(aSpList.getLevelVar());
                    finalVar += Math.sqrt(aSpList.getLevelVar());
                }
            default:
                return new SpatialPosition(finalX / finalVar, finalY / finalVar, floor);
        }
    }

    /**
     * Initializes Kalman Filter for positioning algorithm results
     *
     * @param bssidList List of BSSID of wifi data collected by the client
     * @param levelList List of signal strengths collected by the client
     * @param kfCount   Current filter round
     * @return SpatialPosition object encapsulating the positioning result
     * @throws SQLException SQL exception, thrown upwards
     */
    public SpatialPosition kalmanFilterLocate(List<String> bssidList, List<Float> levelList, int kfCount)
            throws SQLException {
        SpatialPosition observePos = wknnLocate(bssidList, levelList, 4, W_K_NN);
        if (kfCount == 0) {
            lastStatusSP = observePos;
            mseP = observeR;
            return lastStatusSP;
        } else {
            mseP += statusQ;
            float matrixH = mseP / (mseP + observeR);
            double nowX = lastStatusSP.getXPos() + matrixH * (observePos.getXPos() - lastStatusSP.getXPos());
            double nowY = lastStatusSP.getYPos() + matrixH * (observePos.getYPos() - lastStatusSP.getYPos());
            lastStatusSP = new SpatialPosition(nowX, nowY);
            mseP *= (1 - matrixH);
            return lastStatusSP;
        }
    }

    // Kalman Filter coefficients
    private SpatialPosition lastStatusSP;
    private static final float observeR = 3.5f;    // Observation error
    private static final float statusQ = 0.8f;     // State prediction error
    private static float mseP;                     // Optimal MSE at time T

    /**
     * Testing the algorithm
     */
    public static void main(String[] args) throws SQLException {
        int PORT = 8723;    // Port to listen on
        String driverName = "org.postgresql.Driver";    // Driver name
        String url = "jdbc:postgresql://127.0.0.1:5432/indoor_location_data";
        String user = "postgres";
        String password = "c724797";

        DatabaseOperation dbc = new DatabaseOperation(driverName, url, user, password);

        WifiFingerBssidContainer wfbc = new WifiFingerBssidContainer("40:e3:d6:76:44:33",
                "40:e3:d6:76:44:30", "40:e3:d6:76:43:a3", "94:b4:0f:cc:89:b1", "40:e3:d6:76:44:03");
        PositionAlgorithm pa = new PositionAlgorithm(dbc, wfbc);

        List<String> list1 = new ArrayList<>();
        List<Float> list2 = new ArrayList<>();
        list1.add("40:e3:d6:76:44:33");
        list1.add("40:e3:d6:76:44:30");
        list1.add("40:e3:d6:76:43:a3");
        list1.add("94:b4:0f:cc:89:b1");
        list1.add("40:e3:d6:76:44:03");

        list2.add(-100.0f);
        list2.add(-100.0f);
        list2.add(-90.0f);
        list2.add(-75.0f);
        list2.add(-60.0f);
        System.out.println(pa.wknnLocate(list1, list2, 3, W_K_NN));
    }
}
