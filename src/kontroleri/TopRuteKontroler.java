package kontroleri;

import graf.RouteFinder;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;

public class TopRuteKontroler {

    @FXML private TableView<RouteFinder.Route> routeTable;

    @FXML
    private TableColumn<RouteFinder.Route, String> pathColumn;
    @FXML
    private TableColumn<RouteFinder.Route, Long> timeColumn;
    @FXML
    private TableColumn<RouteFinder.Route, Double> costColumn;
    @FXML
    private TableColumn<RouteFinder.Route, Integer> transfersColumn;

    public TopRuteKontroler() {
        // No-args constructor required by FXMLLoader
    }

    @FXML
    private void initialize() {
        // Initialize table columns
        pathColumn.setCellValueFactory(cellData -> new SimpleStringProperty(getDetailedPathNoCost(cellData.getValue())));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("totalTime"));
        costColumn.setCellValueFactory(new PropertyValueFactory<>("totalCost"));
        transfersColumn.setCellValueFactory(new PropertyValueFactory<>("transfers"));
    }

    public void setRoutes(List<RouteFinder.Route> routes, RouteFinder.Criterion criterion) {
        if (routeTable != null) {
            // Process each route to compute path with transition markers
            for (RouteFinder.Route route : routes) {
                if (route.getPath() == null) {
                    List<graf.Graph.Connection> connections = route.getConnections();
                    if (!connections.isEmpty()) {
                        StringBuilder path = new StringBuilder();
                        path.append(connections.get(0).getSource().getCityId());
                        for (graf.Graph.Connection conn : connections) {
                            path.append(" -> ").append(conn.getDestination().getCityId());
                        }
                        // Note: This is read-only; modify Route class for persistent storage
                        // route.setPath(path.toString()); // Uncomment if setPath exists
                    }
                }
            }
            routeTable.setItems(FXCollections.observableArrayList(routes));
            // Sort based on criterion
            switch (criterion) {
                case TIME -> routeTable.getSortOrder().add(timeColumn);
                case COST -> routeTable.getSortOrder().add(costColumn);
                case TRANSFERS -> routeTable.getSortOrder().add(transfersColumn);
            }
            timeColumn.setSortType(TableColumn.SortType.ASCENDING);
            costColumn.setSortType(TableColumn.SortType.ASCENDING);
            transfersColumn.setSortType(TableColumn.SortType.ASCENDING);
            routeTable.sort();
        }
    }

    // Placeholder method to determine if a connection is a train
    private boolean isTrainConnection(graf.Graph.Connection conn) {
        // Replace with the actual method or logic from your Connection class
        // Example: return conn.isTrain(); or return conn.getMode() == TransportType.TRAIN;
        return false; // Default to false; update based on your implementation
    }

    private String getDetailedPathNoCost(RouteFinder.Route route) {
        StringBuilder sb = new StringBuilder();
        List<graf.Graph.Connection> connections = route.getConnections();
        if (connections.isEmpty()) return "";
        sb.append(connections.get(0).getSource().getId());
        for (graf.Graph.Connection conn : connections) {
            sb.append(" (");
            if (!conn.isTransfer()) {
                sb.append(conn.getDeparture().getDepartureTime())
                        .append("-")
                        .append(conn.getDeparture().getArrivalTime())
                        .append(", ");
            }
            sb.append(String.format("%d min", conn.getTravelTime()));
            sb.append(") -> ");
            sb.append(conn.getDestination().getId());
        }
        return sb.toString();
    }
}
