package util.database;

import data.WifiDataContainer;

import java.util.List;

/**
 * Preprocesses data sent to the server
 */
public class DataPreHandle {

    /**
     * Preprocesses WiFi data
     *
     * @param bssidList         List of BSSIDs
     * @param levelList         List of WiFi signal levels
     * @param wifiFingerBssidList List of BSSIDs from WiFi fingerprint data
     * @return WifiDataContainer containing preprocessed data
     */
    public WifiDataContainer preHandleWifiData(List<String> bssidList, List<Float> levelList,
                                                List<String> wifiFingerBssidList) {
        // Remove BSSIDs not present in the WiFi fingerprint list
        for (int i = 0; i < bssidList.size(); i++) {
            if (!wifiFingerBssidList.contains(bssidList.get(i))) {
                bssidList.remove(i);
                levelList.remove(i);
                i--;
            }
        }

        // Add missing WiFi sources with signal strength set to -100
        for (String wifiFingerBssid : wifiFingerBssidList) {
            if (!bssidList.contains(wifiFingerBssid)) {
                bssidList.add(wifiFingerBssid);
                levelList.add(-100f);
            }
        }

        // Convert lists to arrays
        String[] bssidArray = bssidList.toArray(new String[0]);
        Float[] levelArray = levelList.toArray(new Float[bssidList.size()]);

        // Create and return WifiDataContainer
        return new WifiDataContainer(bssidArray, levelArray);
    }
}
