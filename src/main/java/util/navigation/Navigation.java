package util.navigation;

import data.Node;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import util.database.DatabaseOperation;
import util.file.StringOperation;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static data.ConstData.INFINITY;
import static util.navigation.Calculation.calDistance;

/**
 * Navigation and path planning algorithm implementation class.
 */
public class Navigation {
    private DatabaseOperation mDatabaseOperation;

    public Navigation(DatabaseOperation databaseOperation) {
        mDatabaseOperation = databaseOperation;
    }

    /**
     * Get the necessary path nodes for navigation to the destination.
     *
     * @param destAreaName Destination area name
     * @return Necessary path nodes for navigation to the destination
     */
    public Node getOutdoorKeyNavNode(String destAreaName) throws SQLException {
        Connection connection = mDatabaseOperation.getConnection();
        Statement stmt = connection.createStatement();
        String sqlStr = "SELECT ori_name FROM area_names WHERE another_name = " + "'" + destAreaName + "'";
        ResultSet rs = stmt.executeQuery(sqlStr);
        rs.next();
        String destAreaOriName = rs.getString("ori_name");
        sqlStr = "SELECT ST_AsText(junc_node) as dest_pos FROM area_para WHERE area_name = '"
                + destAreaOriName + "'";
        rs = stmt.executeQuery(sqlStr);
        rs.next();
        String destStr = rs.getString("dest_pos");
        return StringOperation.getPosFromStr(destStr);

    }

    /**
     * Get the indoor destination node.
     *
     * @param destAreaName Destination area name
     * @return Node representing the navigation target
     */
    public Node getIndoorDestNavNode(String[] destAreaName) throws SQLException {
        Connection connection = mDatabaseOperation.getConnection();
        Statement stmt = connection.createStatement();
        String sqlStr = "SELECT ST_AsText(junc_node) as dest_pos FROM indoor_area_para WHERE out_area_name = '" +
                destAreaName[0] + "'" + " and " + "area_name = '" + destAreaName[1] + "'";
        ResultSet rs = stmt.executeQuery(sqlStr);
        rs.next();
        String destStr = rs.getString("dest_pos");
        return StringOperation.getPosFromStr(destStr);
    }

    /**
     * Get the necessary nodes for the indoor part of navigation process.
     *
     * @param startNode Starting node
     * @param endNode   End node
     * @param areaName  Area name (e.g., Zihuan Building, Wendiange Library, etc.)
     * @return Node list
     */
    public List<Node> getIndoorNavNode(Node startNode, Node endNode, String areaName) throws SQLException,
            IOException, DocumentException {
        String sqlStr = "SELECT save_path FROM node_path WHERE area_name = '" + areaName + "'";
        Connection connection = mDatabaseOperation.getConnection();
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sqlStr);
        rs.next();
        String nodePath = rs.getString("save_path");
        SAXReader reader = new SAXReader();
        File xmlFile = new File(nodePath);
        Document document = reader.read(xmlFile);
        Element node = document.getRootElement();
        Iterator nodeIt = node.elementIterator();
        List<Node> nodeList = new ArrayList<>();
        while (nodeIt.hasNext()) {
            Element navElement = (Element) nodeIt.next();
            int id = Integer.valueOf(navElement.attribute(0).getValue());
            double lat = Double.valueOf(navElement.attribute(1).getValue());
            double lon = Double.valueOf(navElement.attribute(2).getValue());
            String initAdjNodeIdStr = navElement.attribute(3).getValue();
            nodeList.add(new Node(id, lat, lon, initAdjNodeIdStr));
        }
        int startIndex = 0, endIndex = 0;
        double startDis = INFINITY, endDis = INFINITY, tempDis;
        for (Node nodeElem : nodeList) {
            tempDis = calDistance(startNode, nodeElem);
            if (tempDis < startDis) {
                startDis = tempDis;
                startIndex = nodeElem.getId();
            }
            tempDis = calDistance(endNode, nodeElem);
            if (tempDis < endDis) {
                endDis = tempDis;
                endIndex = nodeElem.getId();
            }
        }
        int nodeCount = nodeList.size();
        double[][] adjMatrix = new double[nodeCount][nodeCount];
        for (int i = 0; i < nodeCount; i++) {
            for (int j = 0; j < nodeCount; j++) {
                if (i == j) {
                    adjMatrix[i][j] = 0;
                } else {
                    adjMatrix[i][j] = INFINITY;
                }
            }
        }
        for (int i = 0; i < nodeCount; i++) {
            List<Integer> adjNodeIdList = nodeList.get(i).getAdjNodeId();
            for (Integer adjNodeId : adjNodeIdList) {
                double dis = calDistance(nodeList.get(i), nodeList.get(adjNodeId));
                if (dis < adjMatrix[i][adjNodeId]) {
                    adjMatrix[i][adjNodeId] = dis;
                    adjMatrix[adjNodeId][i] = dis;
                }
            }
        }
        List<Integer> navNodeIndexList = getMinRoute(startIndex, endIndex, adjMatrix);
        List<Node> navNodeList = new ArrayList<>();
        for (Integer index : navNodeIndexList) {
            navNodeList.add(nodeList.get(index));
        }
        return navNodeList;
    }

    private List<Integer> getMinRoute(int startIndex, int destIndex, double[][] adjMatrix) {
        int nodeNum = adjMatrix.length;
        boolean[] find = new boolean[nodeNum];
        double[] dist = new double[nodeNum];
        int[] prev = new int[nodeNum];
        List<Integer> nodeList = new ArrayList<>();
        for (int i = 0; i < nodeNum; i++) {
            dist[i] = adjMatrix[startIndex][i];
            find[i] = false;
            if (dist[i] == INFINITY)
                prev[i] = -1;
            else
                prev[i] = startIndex;
        }
        dist[startIndex] = 0;
        find[startIndex] = true;
        for (int i = 1; i < nodeNum; i++) {
            double minDis = INFINITY;
            int u = startIndex;
            for (int j = 0; j < nodeNum; ++j) {
                if ((!find[j]) && dist[j] < minDis) {
                    u = j;
                    minDis = dist[j];
                }
            }
            find[u] = true;
            for (int j = 0; j < nodeNum; j++) {
                if ((!find[j]) && adjMatrix[u][j] < INFINITY) {
                    if (dist[u] + adjMatrix[u][j] < dist[j]) {
                        dist[j] = dist[u] + adjMatrix[u][j];
                        prev[j] = u;
                    }
                }
            }
        }
        int k = 1;
        nodeList.add(destIndex);
        while (prev[destIndex] != startIndex) {
            nodeList.add(prev[destIndex]);
            destIndex = nodeList.get(k);
            k++;
        }
        nodeList.add(startIndex);
        return nodeList;
    }

    public static void main(String[] args) throws DocumentException {
//        double[][] adjMatrix = {
//                {0, 1, INFINITY, INFINITY, INFINITY, INFINITY, INFINITY, INFINITY, INFINITY, INFINITY},
//                {1, 0, 1, 1, 1, INFINITY, INFINITY, INFINITY, INFINITY, INFINITY},
//                {INFINITY, 1, 0, INFINITY, INFINITY, INFINITY, INFINITY, INFINITY, INFINITY, INFINITY},
//                {INFINITY, 1, INFINITY, 0, INFINITY, INFINITY, INFINITY, INFINITY, INFINITY, INFINITY},
//                {INFINITY, 1, INFINITY, INFINITY, 0, 1, INFINITY, INFINITY, INFINITY, INFINITY},
//                {INFINITY, INFINITY, INFINITY, INFINITY, 1, 0, 1, 1, INFINITY, INFINITY},
//                {INFINITY, INFINITY, INFINITY, INFINITY, INFINITY, 1, 0, 1, 1, 1},
//                {INFINITY, INFINITY, INFINITY, INFINITY, INFINITY, INFINITY, 1, 0, INFINITY, INFINITY},
//                {INFINITY, INFINITY, INFINITY, INFINITY, INFINITY, INFINITY, 1, INFINITY, 0, INFINITY},
//                {INFINITY, INFINITY, INFINITY, INFINITY, INFINITY, INFINITY, 1, INFINITY, INFINITY, 0},
//        };
//        System.out.println(getMinRoute(1, 7, adjMatrix));
    }
}
