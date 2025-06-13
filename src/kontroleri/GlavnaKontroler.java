package kontroleri;

import graf.Graph;
import graf.GraphBuilder;
import graf.RouteFinder;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import kontroleri.TopRuteKontroler;
import podaci.TransportDataParser;
import racun.ReceiptManager;
import stanice.Station;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javafx.scene.image.Image;

public class GlavnaKontroler {

    @FXML private ComboBox<String> startPointComboBox;
    @FXML private ComboBox<String> destinationComboBox;
    @FXML private RadioButton timeRadioButton;
    @FXML private RadioButton priceRadioButton;
    @FXML private RadioButton transfersRadioButton;
    @FXML private Button findRoutesButton;
    @FXML private Button buyTicketButton;
    @FXML private Button showTopRoutesButton;
    @FXML private Label pathLabel;
    @FXML private Label timeLabel;
    @FXML private Label priceLabel;
    @FXML private Label transfersLabel;
    @FXML private Canvas graphCanvas;

    private Graph graph;
    private GraphBuilder graphBuilder;
    private RouteFinder routeFinder;
    private TransportDataParser.TransportData transportData;
    private RouteFinder.Route optimalRoute;

    @FXML
    private void initialize() {
        try {
            // Load transport data and build graph
            graphBuilder = new GraphBuilder();
            graph = graphBuilder.buildFromJson("transport_data.json");
            transportData = TransportDataParser.readTransportData("transport_data.json");
            routeFinder = new RouteFinder();

            // Populate combo boxes with city names in grid order
            if (transportData != null && transportData.countryMap != null) {
                List<String> cities = new ArrayList<>(graphBuilder.getBusStationMap().keySet());
                cities.sort(new Comparator<String>() {
                    @Override
                    public int compare(String city1, String city2) {
                        int[] coords1 = parseCityCoords(city1);
                        int[] coords2 = parseCityCoords(city2);
                        int rowDiff = coords1[0] - coords2[0]; // Compare rows first
                        if (rowDiff != 0) return rowDiff;
                        return coords1[1] - coords2[1]; // If rows are equal, compare columns
                    }
                });
                startPointComboBox.getItems().addAll(cities);
                destinationComboBox.getItems().addAll(cities);
                // Set initial selection to G_0_0


            } else {
                System.err.println("Transport data or countryMap is null");
            }

            // Set up radio buttons
            ToggleGroup criterionGroup = new ToggleGroup();
            timeRadioButton.setToggleGroup(criterionGroup);
            priceRadioButton.setToggleGroup(criterionGroup);
            transfersRadioButton.setToggleGroup(criterionGroup);
            timeRadioButton.setSelected(true);

            // Set up button actions
            findRoutesButton.setOnAction(event -> findOptimalRoute());
            showTopRoutesButton.setOnAction(event -> showTopRoutes());
            buyTicketButton.setOnAction(event -> buyTicket());

            // Wait for the Canvas to be added to a Scene
            graphCanvas.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    // Bind canvas size to Scene size with a minimum
                    graphCanvas.widthProperty().bind(newScene.widthProperty());
                    graphCanvas.heightProperty().bind(newScene.heightProperty());

                    // Redraw graph on window resize
                    ChangeListener<Number> resizeListener = (observable, oldValue, newValue) -> drawGraph();
                    newScene.widthProperty().addListener(resizeListener);
                    newScene.heightProperty().addListener(resizeListener);

                    // Draw initial graph
                    drawGraph();
                }
            });

        } catch (IOException e) {
            showAlert("Error", "Failed to load transport data: " + e.getMessage());
        }
    }

    private void drawGraph() {
        GraphicsContext gc = graphCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, graphCanvas.getWidth(), graphCanvas.getHeight());

        if (transportData == null || transportData.countryMap == null || graph == null) {
            System.err.println("Transport data, countryMap, or graph is null, cannot draw graph");
            return;
        }

        // Calculate canvas size based on network dimensions and window size
        int rows = transportData.countryMap.length;
        int cols = transportData.countryMap[0].length;
        double margin = 20; // Margin for the graph
        double availableWidth = graphCanvas.getWidth() - 2 * margin;
        double availableHeight = graphCanvas.getHeight() - 2 * margin;

        // Set dynamic node spacing with a minimum to avoid overlap
        double minNodeSpacing = 15; // Increased minimum spacing for better visibility
        double nodeSpacingX = Math.max(minNodeSpacing, availableWidth / (cols > 1 ? cols - 1 : 1));
        double nodeSpacingY = Math.max(minNodeSpacing, availableHeight / (rows > 1 ? rows - 1 : 1));
        double nodeSpacing = Math.min(nodeSpacingX, nodeSpacingY);

        // Debug: Verify grid order
        System.out.println("Drawing graph with " + rows + " rows and " + cols + " cols");
        for (Station station : graph.getStations()) {
            int[] coords = parseCityCoords(station.getCityId());
            System.out.println("Station: " + station.getCityId() + " at coords [" + coords[0] + ", " + coords[1] + "]");
        }

        // Draw edges first
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        int edgeCount = 0;
        for (Station station : graph.getStations()) {
            int[] coords = parseCityCoords(station.getCityId());
            double x1 = margin + coords[1] * nodeSpacing; // X increases rightward
            double y1 = margin + coords[0] * nodeSpacing; // Y increases downward

            for (Graph.Connection conn : graph.getConnections(station)) {
                int[] destCoords = parseCityCoords(conn.getDestination().getCityId());
                double x2 = margin + destCoords[1] * nodeSpacing;
                double y2 = margin + destCoords[0] * nodeSpacing;
                gc.strokeLine(x1, y1, x2, y2);
                edgeCount++;
            }
        }
        System.out.println("Drawn " + edgeCount + " edges");

        // Draw nodes (all blue) without labels
        gc.setFill(Color.BLUE);
        gc.setStroke(Color.BLACK);
        for (Station station : graph.getStations()) {
            int[] coords = parseCityCoords(station.getCityId());
            double x = margin + coords[1] * nodeSpacing; // X increases rightward
            double y = margin + coords[0] * nodeSpacing; // Y increases downward
            double radius = Math.max(2, minNodeSpacing / 3); // Adjusted radius for visibility
            gc.fillOval(x - radius, y - radius, 2 * radius, 2 * radius);
            gc.strokeOval(x - radius, y - radius, 2 * radius, 2 * radius);
        }

        // Highlight optimal route if exists
        if (optimalRoute != null) {
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(2);
            for (Graph.Connection conn : optimalRoute.getConnections()) {
                int[] sourceCoords = parseCityCoords(conn.getSource().getCityId());
                int[] destCoords = parseCityCoords(conn.getDestination().getCityId());
                double x1 = margin + sourceCoords[1] * nodeSpacing;
                double y1 = margin + sourceCoords[0] * nodeSpacing;
                double x2 = margin + destCoords[1] * nodeSpacing;
                double y2 = margin + destCoords[0] * nodeSpacing;
                gc.strokeLine(x1, y1, x2, y2);
            }
        }
    }

    private int[] parseCityCoords(String cityId) {
        // City ID format: G_X_Y
        String[] parts = cityId.split("_");
        return new int[]{Integer.parseInt(parts[1]), Integer.parseInt(parts[2])}; // [row, col]
    }

    private void findOptimalRoute() {
        String startCity = startPointComboBox.getValue();
        String destCity = destinationComboBox.getValue();
        if (startCity == null || destCity == null) {
            showAlert("Error", "Please select both start and destination cities.");
            return;
        }

        RouteFinder.Criterion criterion = timeRadioButton.isSelected() ? RouteFinder.Criterion.TIME :
                priceRadioButton.isSelected() ? RouteFinder.Criterion.COST : RouteFinder.Criterion.TRANSFERS;

        Station[] sourceStations = {graphBuilder.getBusStationMap().get(startCity), graphBuilder.getTrainStationMap().get(startCity)};
        Station[] destStations = {graphBuilder.getBusStationMap().get(destCity), graphBuilder.getTrainStationMap().get(destCity)};

        List<RouteFinder.Route> routes = findAllRoutes(sourceStations, destStations, criterion, LocalTime.of(8, 0), 1);
        if (routes.isEmpty()) {
            showAlert("No Routes", "No routes found between " + startCity + " and " + destCity);
            return;
        }

        optimalRoute = routes.get(0);
        updateRouteDisplay(optimalRoute);
        drawGraph();
    }

    private String getDetailedPathNoCost(RouteFinder.Route route) {
        StringBuilder sb = new StringBuilder();
        List<Graph.Connection> connections = route.getConnections();
        if (connections.isEmpty()) return "";
        // Prvi grad
        sb.append(connections.get(0).getSource().getId());
        for (Graph.Connection conn : connections) {
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

    private void updateRouteDisplay(RouteFinder.Route route) {
        String detailedPath = getDetailedPathNoCost(route);
        String[] elements = detailedPath.split(" -> ");
        StringBuilder multiLine = new StringBuilder();
        for (int i = 0; i < elements.length; i++) {
            if (i > 0) multiLine.append(" -> ");
            multiLine.append(elements[i]);
            if ((i + 1) % 3 == 0 && i != elements.length - 1) {
                multiLine.append("\n");
            }
        }
        pathLabel.setText(detailedPath);
        pathLabel.setTooltip(new Tooltip(multiLine.toString()));
        timeLabel.setText(route.getTotalTime() + " min");
        priceLabel.setText(String.format("%.2f KM", route.getTotalCost()));
        transfersLabel.setText(String.valueOf(route.getTransfers()));
    }

    private void showTopRoutes() {
        try {
            String startCity = startPointComboBox.getValue();
            String destCity = destinationComboBox.getValue();
            if (startCity == null || destCity == null) {
                showAlert("Error", "Please select both start and destination cities.");
                return;
            }

            RouteFinder.Criterion criterion = timeRadioButton.isSelected() ? RouteFinder.Criterion.TIME :
                    priceRadioButton.isSelected() ? RouteFinder.Criterion.COST : RouteFinder.Criterion.TRANSFERS;

            Station[] sourceStations = {graphBuilder.getBusStationMap().get(startCity), graphBuilder.getTrainStationMap().get(startCity)};
            Station[] destStations = {graphBuilder.getBusStationMap().get(destCity), graphBuilder.getTrainStationMap().get(destCity)};

            List<RouteFinder.Route> routes = findAllRoutes(sourceStations, destStations, criterion, LocalTime.of(8, 0), 5);

            System.out.println("Attempting to load TopRute.fxml from: " + getClass().getResource("/resursi/TopRute.fxml"));
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resursi/TopRute.fxml"));
            BorderPane root = loader.load();
            TopRuteKontroler controller = loader.getController();
            if (controller == null) {
                throw new IOException("Failed to initialize TopRuteKontroler from TopRute.fxml. Check fx:controller and class existence.");
            }
            System.out.println("Controller loaded successfully: " + controller);
            controller.setRoutes(routes, criterion);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Top 5 Routes");
            stage.setResizable(true);
            stage.show();

        } catch (IOException e) {
            showAlert("Error", "Failed to load TopRute.fxml or initialize controller: " + e.getMessage());
            System.err.println("IOException in showTopRoutes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void buyTicket() {
        if (optimalRoute == null) {
            showAlert("Error", "No route selected. Please find a route first.");
            return;
        }
        ReceiptManager.saveReceipt(optimalRoute, LocalDate.now());

        // Update earnings in PocetnaKontroler
        try {
            // Find the Pocetna window and update its earnings
            for (javafx.stage.Window window : javafx.stage.Window.getWindows()) {
                if (window instanceof Stage) {
                    Stage stage = (Stage) window;
                    if (stage.getTitle() != null && stage.getTitle().equals("Generisanje Transportnih Podataka")) {
                        Scene scene = stage.getScene();
                        if (scene != null && scene.getRoot() instanceof javafx.scene.layout.VBox) {
                            javafx.scene.layout.VBox root = (javafx.scene.layout.VBox) scene.getRoot();
                            // Find PocetnaKontroler and update earnings
                            for (javafx.scene.Node node : root.getChildren()) {
                                if (node instanceof Label && node.getId() != null && node.getId().equals("totalEarningsLabel")) {
                                    // Get current earnings
                                    String currentEarnings = ((Label) node).getText();
                                    double currentTotal = Double.parseDouble(currentEarnings);
                                    // Add new ticket cost
                                    double newTotal = currentTotal + optimalRoute.getTotalCost();
                                    // Update label
                                    ((Label) node).setText(String.format("%.2f", newTotal));
                                    break;
                                }
                            }
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating earnings in PocetnaKontroler: " + e.getMessage());
        }

        showAlert("Uspješno", "Karta je kupljena, a račun je sačuvan u folderu 'racuni'.");
    }

    private List<RouteFinder.Route> findAllRoutes(Station[] sourceStations, Station[] destStations,
                                                  RouteFinder.Criterion criterion, LocalTime startTime, int maxRoutes) {
        List<RouteFinder.Route> allRoutes = new ArrayList<>();
        for (Station source : sourceStations) {
            for (Station dest : destStations) {
                allRoutes.addAll(routeFinder.findTopRoutes(graph, source, dest, criterion, startTime, maxRoutes));
            }
        }
        allRoutes.sort(switch (criterion) {
            case TIME -> Comparator.comparingLong(RouteFinder.Route::getTotalTime)
                    .thenComparingDouble(RouteFinder.Route::getTotalCost)
                    .thenComparingInt(RouteFinder.Route::getTransfers);
            case COST -> Comparator.comparingDouble(RouteFinder.Route::getTotalCost)
                    .thenComparingLong(RouteFinder.Route::getTotalTime)
                    .thenComparingInt(RouteFinder.Route::getTransfers);
            case TRANSFERS -> Comparator.comparingInt(RouteFinder.Route::getTransfers)
                    .thenComparingDouble(RouteFinder.Route::getTotalCost)
                    .thenComparingLong(RouteFinder.Route::getTotalTime);
        });
        return allRoutes.subList(0, Math.min(maxRoutes, allRoutes.size()));
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
