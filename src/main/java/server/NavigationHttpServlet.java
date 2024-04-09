package server;

import data.Node;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.dom4j.DocumentException;
import util.database.DatabaseOperation;
import util.navigation.NavBasicInfo;
import util.navigation.Navigation;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;


public class NavigationHttpServlet extends HttpServlet {
    private DatabaseOperation mDatabaseOperation;   // Database operation class
    private final DiskFileItemFactory factory = new DiskFileItemFactory();    // Create a DiskFileItemFactory factory
    private final ServletFileUpload upload = new ServletFileUpload(factory);     // Create a file upload parser
    private Navigation mNavigationUtil;

    @Override
    public void init() throws ServletException {
        super.init();
        String driverName = "org.postgresql.Driver";    // Driver name
        String url = "jdbc:postgresql://127.0.0.1:5432/indoor_map_para";
        String user = "postgres";
        String password = "c724797";
        mDatabaseOperation = new DatabaseOperation(driverName, url, user, password);
        mNavigationUtil = new Navigation(mDatabaseOperation);
        // Solve the problem of Chinese garbled characters in the uploaded file names
        upload.setHeaderEncoding("UTF-8");
        System.out.println("Server navigation module loaded successfully");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Check if the submitted data is multipart form data
        if (!ServletFileUpload.isMultipartContent(req)) {
            // Retrieve data in the traditional way
            return;
        }
        /*
          Use the ServletFileUpload parser to parse the uploaded data.
          The parsing result returns a List<FileItem> collection, with each FileItem corresponding to an input item in the form
         */
        List<FileItem> list = null;
        try {
            list = upload.parseRequest(req);
        } catch (FileUploadException e) {
            e.printStackTrace();
            System.out.println("Error parsing uploaded data" + e.getMessage());
        }
        NavBasicInfo navBasicInfo = new NavBasicInfo(); // Navigation basic information
        for (FileItem item : Objects.requireNonNull(list)) {
            // If the FileItem encapsulates ordinary input data
            if (item.isFormField()) {
                String name = item.getFieldName();
                // Solve the problem of Chinese garbled characters in ordinary input data
                String value = item.getString("UTF-8");
                switch (name) {
                    case "area_name":
                        navBasicInfo.setDesAreaName(value);
                        break;
                    case "indoor":
                        navBasicInfo.setIndoor(Boolean.valueOf(value));
                        break;
                    case "latitude":
                        navBasicInfo.setLat(Double.valueOf(value));
                        break;
                    case "longitude":
                        navBasicInfo.setLon(Double.valueOf(value));
                        break;
                    case "floor":
                        navBasicInfo.setFloor(Integer.valueOf(value));
                        break;
                    default:
                        break;
                }
            }
        }
        String[] specificArea = navBasicInfo.getDesAreaName().split(",");
        /*
          Status code statusCode represents the following meanings:
          0 means invalid navigation or error
          1 means navigation from outdoor to outdoor, 2 means from outdoor to indoor
          3 means from indoor to outdoor, 4 means from indoor to indoor
          5 means from indoor to outdoor to indoor
         */
        int statusCode = 0;
        double lat = navBasicInfo.getLat();
        double lon = navBasicInfo.getLon();
        StringBuilder navStrBuilder = new StringBuilder();
        OutputStream outputStream = resp.getOutputStream();
        Node startNode, endNode;
        List<Node> navNodeList;
        String indoorAreaName;
        try {
            if (navBasicInfo.isIndoor()) {   // If the starting area is indoors
                switch (specificArea.length) {
                    case 1: // The target area is outdoors
                        statusCode = 3;
                        startNode = new Node(lat, lon);
                        // Get the name of the indoor area where the starting point is located
                        indoorAreaName = getAreaNameByNode(lat, lon);
                        // Get the latitude and longitude of the intersection point between the indoor area and the outdoor area
                        Node middleNode = mNavigationUtil.getOutdoorKeyNavNode(indoorAreaName);
                        // Get the indoor navigation path
                        navNodeList = mNavigationUtil.getIndoorNavNode(startNode, middleNode, indoorAreaName);
                        endNode = mNavigationUtil.getOutdoorKeyNavNode(specificArea[0]);
                        navStrBuilder.append(statusCode).append("\n")
                                .append("start").append("\n")
                                .append(startNode.toString()).append("\n")
                                .append("end").append("\n")
                                .append(endNode.toString()).append("\n")
                                .append("indoor").append("\n");
                        for (Node node : navNodeList) {
                            navStrBuilder.append(node.toString()).append("\n");
                        }
                        navStrBuilder.append("outdoor").append("\n")
                                .append(middleNode.toString()).append("\n")
                                .append(endNode.toString()).append("\n");
                        break;
                    case 2: // The target area is indoors (divided into 2 cases, namely from indoor to indoor and from indoor to outdoor and then to indoor)
                        // First, determine whether the target indoor area and the current location are in the same building
                        boolean inSameArea = judgeIsInSameIndoorArea(lat, lon, specificArea);
                        /*
                          If in the same indoor area, the operations to be performed are as follows:
                          Generate Node objects with latitude and longitude based on the destination name as the end point
                          Use the starting point, the end point, and the node file of the indoor area for path planning to obtain the node list of indoor navigation
                         */
                        if (inSameArea) {
                            statusCode = 4;
                            startNode = new Node(lat, lon);
                            endNode = mNavigationUtil.getIndoorDestNavNode(specificArea);
                            // Get the indoor navigation path
                            navNodeList = mNavigationUtil.getIndoorNavNode(startNode, endNode, specificArea[0]);
                            navStrBuilder.append(statusCode).append("\n")
                                    .append("start").append("\n")
                                    .append(startNode.toString()).append("\n")
                                    .append("end").append("\n")
                                    .append(endNode.toString()).append("\n")
                                    .append("indoor").append("\n");
                            for (Node node : navNodeList) {
                                navStrBuilder.append(node.toString()).append("\n");
                            }
                        }
                        /*
                          If not in the same area, this corresponds to actual situations such as: navigating from the first floor of Building A to the first floor of Building B.
                          The operations to be performed are as follows:
                          Generate Node objects with latitude and longitude based on the destination name as the end point
                          Get the latitude and longitude of the intersection points of the two indoor areas inside and outside
                          Finally, perform indoor path planning in the two indoor areas, and outdoor path planning between the two intermediate Node objects
                         */
                        else {
                            statusCode = 5;
                            startNode = new Node(lat, lon);
                            // Get the name of the indoor area where the starting point is located
                            indoorAreaName = getAreaNameByNode(lat, lon);
                            // Get the latitude and longitude of the intersection point between the indoor area and the outdoor area
                            Node fMiddleNode = mNavigationUtil.getOutdoorKeyNavNode(indoorAreaName);
                            // Get the indoor navigation path
                            List<Node> fNavNodeList = mNavigationUtil.getIndoorNavNode(startNode, fMiddleNode, indoorAreaName);
                            Node sMiddleNode = mNavigationUtil.getOutdoorKeyNavNode(specificArea[0]);
                            // Get the indoor navigation path
                            endNode = mNavigationUtil.getIndoorDestNavNode(specificArea);   // Target node
                            List<Node> sNavNodeList = mNavigationUtil.getIndoorNavNode(sMiddleNode, endNode, specificArea[0]);
                            navStrBuilder.append(statusCode).append("\n")
                                    .append("start").append("\n")
                                    .append(startNode.toString()).append("\n")
                                    .append("end").append("\n")
                                    .append(endNode.toString()).append("\n")
                                    .append("indoor").append("\n");
                            for (Node node : fNavNodeList) {
                                navStrBuilder.append(node.toString()).append("\n");
                            }
                            navStrBuilder.append("outdoor").append("\n")
                                    .append(fMiddleNode.toString()).append("\n")
                                    .append(sMiddleNode.toString()).append("\n")
                                    .append("indoor").append("\n");
                            for (Node node : sNavNodeList) {
                                navStrBuilder.append(node.toString()).append("\n");
                            }
                        }
                        break;
                    default:
                        break;

                }
            } else {                        // If the starting area is outdoors
                switch (specificArea.length) {
                    /*
                      If the target area is outdoors, get the latitude and longitude of the outdoor target area based on the outdoor target area name, and add it to navNodeList
                     */
                    case 1:
                        statusCode = 1;
                        startNode = new Node(lat, lon);
                        endNode = mNavigationUtil.getOutdoorKeyNavNode(specificArea[0]);
                        navStrBuilder.append(statusCode).append("\n")
                                .append("start").append("\n")
                                .append(startNode.toString()).append("\n")
                                .append("end").append("\n")
                                .append(endNode.toString()).append("\n")
                                .append("outdoor").append("\n")
                                .append(startNode.toString()).append("\n")
                                .append(endNode.toString()).append("\n");
                        break;
                    case 2:                 // If the target area is indoors
                        statusCode = 2;
                        startNode = new Node(lat, lon);
                        Node middleNode = mNavigationUtil.getOutdoorKeyNavNode(specificArea[0]);
                        endNode = mNavigationUtil.getIndoorDestNavNode(specificArea);
                        // Get the indoor navigation path
                        navNodeList = mNavigationUtil.getIndoorNavNode(middleNode, endNode, specificArea[0]);
                        navStrBuilder.append(statusCode).append("\n")
                                .append("start").append("\n")
                                .append(startNode.toString()).append("\n")
                                .append("end").append("\n")
                                .append(endNode.toString()).append("\n")
                                .append("outdoor").append("\n")
                                .append(startNode.toString()).append("\n")
                                .append(middleNode.toString()).append("\n");
                        navStrBuilder.append("indoor").append("\n");
                        for (Node node : navNodeList) {
                            navStrBuilder.append(node.toString()).append("\n");
                        }
                        break;
                    default:
                        break;
                }
            }
            outputStream.write(navStrBuilder.toString().getBytes());
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Error getting target location information: " + e.getMessage());
        } catch (DocumentException e) {
            e.printStackTrace();
            System.out.println("Error getting indoor navigation path: " + e.getMessage());
        }
    }


    /**
     * Determine whether the current location and the navigation destination are in the same indoor area
     *
     * @param lat      Current location latitude
     * @param lon      Current location longitude
     * @param areaName Destination name
     * @return Whether in the same indoor area
     */
    private boolean judgeIsInSameIndoorArea(double lat, double lon, String[] areaName) throws SQLException {
        /*
          First, get the area name where the starting point is located
         */

        String startArea = getAreaNameByNode(lat, lon);
        /*
          Get the original name of the destination from the server
         */
        String destFirstName = areaName[0]; // Destination's first-level name, such as Zihuan Building
        Connection connection = mDatabaseOperation.getConnection();
        Statement stmt = connection.createStatement();
        String sqlStr = "SELECT ori_name FROM area_names WHERE another_name = " + "'" + destFirstName + "'";
        ResultSet rs = stmt.executeQuery(sqlStr);
        rs.next();
        String destArea = rs.getString("ori_name");
        return destArea.equals(startArea);
    }

    /**
     * Get the name of the indoor area where the point is located
     *
     * @param lat Current location latitude
     * @param lon Current location longitude
     * @return Name of the indoor area where the point is located
     */
    private String getAreaNameByNode(double lat, double lon) throws SQLException {
        String sqlStr = "SELECT area_name FROM indoor_map WHERE " + " ST_WITHIN(ST_GeomFromText('POINT( " +
                lon + " " + lat + " )',4326), indoor_map.enter_area)";
        Connection connection = mDatabaseOperation.getConnection();
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sqlStr);
        rs.next();
        return rs.getString("area_name");
    }
}
