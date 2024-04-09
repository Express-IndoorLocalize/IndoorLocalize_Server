package olddata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * WifiFingerBssidContainer serves as a container holding the RSSIDs of APs as WiFi positioning sources,
 * enabling sharing between modules.
 */
public class WifiFingerBssidContainer {
    private List<String> wifiFingerBssidList;

    public WifiFingerBssidContainer(List<String> wifiFingerBssidList) {
        this.wifiFingerBssidList = wifiFingerBssidList;
    }

    public WifiFingerBssidContainer(String... wifiFingerBssidArray) {
        wifiFingerBssidList = new ArrayList<>(Arrays.asList(wifiFingerBssidArray));
    }

    public List<String> getWifiFingerBssidList() {
        return wifiFingerBssidList;
    }

    @Override
    public String toString() {
        return wifiFingerBssidList.toString();
    }
}
