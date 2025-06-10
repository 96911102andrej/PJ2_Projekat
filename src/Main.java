import graf.Graph;
import graf.GraphBuilder;
import graf.RouteFinder;
import stanice.Station;
import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Main class to input starting and ending cities and find optimal routes.
 */
public class Main {
    public static void main(String[] args) {
        // Step 1: Load JSON file
        String jsonFile = "transport_data.json";
        System.out.println("Using pre-generated JSON file: " + jsonFile);

        // Step 2: Build the graph
        GraphBuilder builder = new GraphBuilder();
        Graph graph;
        try {
            graph = builder.buildFromJson(jsonFile);
            System.out.println("Graph constructed with " + graph.getStations().size() + " stations and " +
                    graph.getStations().stream().mapToInt(s -> graph.getConnections(s).size()).sum() + " connections.");
        } catch (IOException e) {
            System.err.println("Error building graph: " + e.getMessage());
            return;
        }

        // Step 3: Input starting and ending cities
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter starting city (e.g., G_0_0): ");
        String startCity = scanner.nextLine().trim();
        System.out.print("Enter ending city (e.g., G_1_1): ");
        String endCity = scanner.nextLine().trim();
        scanner.close();

        // Step 4: Map cities to stations
        Station[] sourceStations = new Station[2];
        Station[] destStations = new Station[2];
        sourceStations[0] = builder.getBusStationMap().get(startCity);
        sourceStations[1] = builder.getTrainStationMap().get(startCity);
        destStations[0] = builder.getBusStationMap().get(endCity);
        destStations[1] = builder.getTrainStationMap().get(endCity);

        if (sourceStations[0] == null || sourceStations[1] == null) {
            System.out.println("Invalid starting city: " + startCity);
            return;
        }
        if (destStations[0] == null || destStations[1] == null) {
            System.out.println("Invalid ending city: " + endCity);
            return;
        }

        // Step 5: Find and print top 5 routes for each criterion
        RouteFinder routeFinder = new RouteFinder();
        LocalTime startTime = LocalTime.of(8, 0);

        // Shortest time
        System.out.println("\nTop 5 shortest time routes from " + startCity + " to " + endCity + ":");
        List<RouteFinder.Route> timeRoutes = findAllRoutes(graph, routeFinder, sourceStations, destStations, RouteFinder.Criterion.TIME, startTime, 5);
        printRoutes(timeRoutes);

        // Lowest cost
        System.out.println("\nTop 5 lowest cost routes from " + startCity + " to " + endCity + ":");
        List<RouteFinder.Route> costRoutes = findAllRoutes(graph, routeFinder, sourceStations, destStations, RouteFinder.Criterion.COST, startTime, 5);
        printRoutes(costRoutes);

        // Fewest transfers
        System.out.println("\nTop 5 routes with fewest transfers from " + startCity + " to " + endCity + ":");
        List<RouteFinder.Route> transfersRoutes = findAllRoutes(graph, routeFinder, sourceStations, destStations, RouteFinder.Criterion.TRANSFERS, startTime, 5);
        printRoutes(transfersRoutes);
    }

    private static List<RouteFinder.Route> findAllRoutes(Graph graph, RouteFinder routeFinder, Station[] sourceStations,
                                                         Station[] destStations, RouteFinder.Criterion criterion,
                                                         LocalTime startTime, int maxRoutes) {
        List<RouteFinder.Route> allRoutes = new ArrayList<>();
        for (Station source : sourceStations) {
            for (Station dest : destStations) {
                List<RouteFinder.Route> routes = routeFinder.findTopRoutes(graph, source, dest, criterion, startTime, maxRoutes);
                allRoutes.addAll(routes);
            }
        }
        // Sort and take top 5 based on criterion
        Comparator<RouteFinder.Route> comparator = switch (criterion) {
            case TIME -> Comparator.comparingLong(RouteFinder.Route::getTotalTime);
            case COST -> Comparator.comparingDouble(RouteFinder.Route::getTotalCost);
            case TRANSFERS -> Comparator.comparingInt(RouteFinder.Route::getTransfers);
        };
        return allRoutes.stream()
                .sorted(comparator)
                .limit(maxRoutes)
                .collect(Collectors.toList());
    }

    private static void printRoutes(List<RouteFinder.Route> routes) {
        if (routes.isEmpty()) {
            System.out.println("No routes found.");
            return;
        }

        for (int i = 0; i < routes.size(); i++) {
            RouteFinder.Route route = routes.get(i);
            System.out.println("\nRoute " + (i + 1) + ":");
            System.out.println("Route details:");
            List<Double> waitingTimes = route.getWaitingTimes();
            LocalTime currentTime = LocalTime.of(8, 0);
            for (int j = 0; j < route.getConnections().size(); j++) {
                Graph.Connection conn = route.getConnections().get(j);
                double waitingTime = waitingTimes.get(j);
                if (conn.isTransfer()) {
                    System.out.printf("Transfer: %s -> %s (%.2f KM, %d min, Waiting: %.0f min, CurrentTime: %s)\n",
                            conn.getSource().getId(), conn.getDestination().getId(),
                            conn.getCost(), conn.getTravelTime(), waitingTime, currentTime);
                    currentTime = currentTime.plusMinutes((long) waitingTime);
                } else {
                    System.out.printf("Travel: %s -> %s (%s to %s, %.2f KM, %d min, Waiting: %.0f min, MinWaitingTime: %d min, CurrentTime: %s)\n",
                            conn.getSource().getId(), conn.getDestination().getId(),
                            conn.getDeparture().getDepartureTime(), conn.getDeparture().getArrivalTime(),
                            conn.getCost(), conn.getTravelTime(), waitingTime, conn.getDeparture().getMinWaitingTime(), currentTime);
                    currentTime = conn.getDeparture().getArrivalTime();
                }
            }
            System.out.printf("Total: %.2f KM, %d min, %d transfers\n",
                    route.getTotalCost(), route.getTotalTime(), route.getTransfers());
        }
    }
}