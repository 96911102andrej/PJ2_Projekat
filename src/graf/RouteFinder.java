package graf;

import stanice.Station;
import odlasci.Departures;
import java.time.LocalTime;
import java.util.*;

/**
 * Finds optimal routes in the transportation graph based on different criteria.
 */
public class RouteFinder {
    public enum Criterion { TIME, COST, TRANSFERS }

    /**
     * Represents a route with its connections and metrics.
     */
    public static class Route {
        private final List<Graph.Connection> connections;
        private final double totalCost;
        private final long totalTime;
        private final int transfers;
        private final List<Double> waitingTimes;

        Route(List<Graph.Connection> connections, double totalCost, long totalTime, int transfers, List<Double> waitingTimes) {
            this.connections = new ArrayList<>(connections);
            this.totalCost = totalCost;
            this.totalTime = totalTime;
            this.transfers = transfers;
            this.waitingTimes = new ArrayList<>(waitingTimes);
        }

        public List<Graph.Connection> getConnections() {
            return Collections.unmodifiableList(connections);
        }

        public double getTotalCost() {
            return totalCost;
        }

        public long getTotalTime() {
            return totalTime;
        }

        public int getTransfers() {
            return transfers;
        }

        public List<Double> getWaitingTimes() {
            return Collections.unmodifiableList(waitingTimes);
        }
    }

    /**
     * State class for Dijkstra's algorithm to track multiple paths.
     */
    private static class State implements Comparable<State> {
        Station station;
        double weight;
        LocalTime currentTime;
        int transfers;
        List<Graph.Connection> path;
        List<Double> waitingTimes;
        Station lastTransportStation; // Last station where we boarded a transport

        State(Station station, double weight, LocalTime currentTime, int transfers,
              List<Graph.Connection> path, List<Double> waitingTimes, Station lastTransportStation) {
            this.station = station;
            this.weight = weight;
            this.currentTime = currentTime;
            this.transfers = transfers;
            this.path = path;
            this.waitingTimes = waitingTimes;
            this.lastTransportStation = lastTransportStation;
        }

        @Override
        public int compareTo(State other) {
            return Double.compare(this.weight, other.weight);
        }
    }

    /**
     * Finds the top N optimal routes based on the specified criterion.
     */
    public List<Route> findTopRoutes(Graph graph, Station source, Station destination, Criterion criterion, LocalTime startTime, int maxRoutes) {
        if (criterion == Criterion.TRANSFERS) {
            return findTopRoutesBFS(graph, source, destination, maxRoutes);
        } else {
            return findTopRoutesDijkstra(graph, source, destination, criterion, startTime, maxRoutes);
        }
    }

    private List<Route> findTopRoutesDijkstra(Graph graph, Station source, Station destination, Criterion criterion, LocalTime startTime, int maxRoutes) {
        PriorityQueue<State> queue = new PriorityQueue<>();
        Map<Station, List<State>> bestStates = new HashMap<>();
        List<Route> topRoutes = new ArrayList<>();

        queue.add(new State(source, 0.0, startTime, 0, new ArrayList<>(), new ArrayList<>(), source));
        bestStates.computeIfAbsent(source, k -> new ArrayList<>()).add(
                new State(source, 0.0, startTime, 0, new ArrayList<>(), new ArrayList<>(), source));

        while (!queue.isEmpty()) {
            State current = queue.poll();
            Station currentStation = current.station;
            LocalTime currentTime = current.currentTime;
            int currentTransfers = current.transfers;
            List<Graph.Connection> currentPath = current.path;
            List<Double> currentWaitingTimes = current.waitingTimes;
            Station lastTransportStation = current.lastTransportStation;

            if (currentStation.equals(destination)) {
                double totalCost = currentPath.stream().mapToDouble(Graph.Connection::getCost).sum();
                long totalTravelTime = currentPath.stream().mapToLong(Graph.Connection::getTravelTime).sum();
                long totalWaitingTime = currentWaitingTimes.stream().mapToLong(Double::longValue).sum();
                long totalTime = totalTravelTime + totalWaitingTime;
                topRoutes.add(new Route(currentPath, totalCost, Math.max(totalTime, 0), currentTransfers, currentWaitingTimes));
                if (topRoutes.size() >= maxRoutes) {
                    break;
                }
                continue;
            }

            for (Graph.Connection conn : graph.getConnections(currentStation)) {
                if (conn.getTravelTime() <= 0 && !conn.isTransfer()) {
                    continue;
                }

                Station nextStation = conn.getDestination();
                double waitingTime = calculateWaitingTime(conn, currentTime);
                double newWeight;

                // Determine if this is a transfer
                boolean isTransfer = false;
                Station newLastTransportStation = lastTransportStation;

                if (conn.isTransfer()) {
                    // This is a transfer between bus and train in the same city
                    isTransfer = true;
                } else {
                    // Check if we're changing transport (different departure)
                    if (!conn.getSource().equals(lastTransportStation)) {
                        isTransfer = true;
                        newLastTransportStation = conn.getSource();
                    }
                }

                int newTransfers = currentTransfers + (isTransfer ? 1 : 0);

                switch (criterion) {
                    case TIME:
                        newWeight = current.weight + conn.getTravelTime() + waitingTime;
                        break;
                    case COST:
                        newWeight = current.weight + conn.getCost();
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported criterion for Dijkstra");
                }

                List<State> stationStates = bestStates.computeIfAbsent(nextStation, k -> new ArrayList<>());
                if (stationStates.size() >= maxRoutes) {
                    State worstState = stationStates.stream().max(Comparator.comparingDouble(s -> s.weight)).orElse(null);
                    if (worstState != null && newWeight >= worstState.weight) {
                        continue;
                    }
                    stationStates.remove(worstState);
                }

                List<Graph.Connection> newPath = new ArrayList<>(currentPath);
                newPath.add(conn);
                List<Double> newWaitingTimes = new ArrayList<>(currentWaitingTimes);
                newWaitingTimes.add(waitingTime);
                LocalTime nextTime = conn.isTransfer() ? currentTime.plusMinutes((long) waitingTime)
                        : conn.getDeparture().getArrivalTime();
                State newState = new State(nextStation, newWeight, nextTime, newTransfers, newPath, newWaitingTimes, newLastTransportStation);
                stationStates.add(newState);
                queue.add(newState);
            }
        }

        return topRoutes;
    }

    private List<Route> findTopRoutesBFS(Graph graph, Station source, Station destination, int maxRoutes) {
        Queue<State> queue = new LinkedList<>();
        Map<Station, List<State>> bestStates = new HashMap<>();
        List<Route> topRoutes = new ArrayList<>();

        queue.add(new State(source, 0, LocalTime.of(0, 0), 0, new ArrayList<>(), new ArrayList<>(), source));
        bestStates.computeIfAbsent(source, k -> new ArrayList<>()).add(
                new State(source, 0, LocalTime.of(0, 0), 0, new ArrayList<>(), new ArrayList<>(), source));

        while (!queue.isEmpty()) {
            State current = queue.poll();
            Station currentStation = current.station;
            int currentTransfers = current.transfers;
            List<Graph.Connection> currentPath = current.path;
            List<Double> currentWaitingTimes = current.waitingTimes;
            Station lastTransportStation = current.lastTransportStation;

            if (currentStation.equals(destination)) {
                double totalCost = currentPath.stream().mapToDouble(Graph.Connection::getCost).sum();
                long totalTravelTime = currentPath.stream().mapToLong(Graph.Connection::getTravelTime).sum();
                long totalWaitingTime = currentWaitingTimes.stream().mapToLong(Double::longValue).sum();
                long totalTime = totalTravelTime + totalWaitingTime;
                topRoutes.add(new Route(currentPath, totalCost, Math.max(totalTime, 0), currentTransfers, currentWaitingTimes));
                if (topRoutes.size() >= maxRoutes) {
                    break;
                }
                continue;
            }

            for (Graph.Connection conn : graph.getConnections(currentStation)) {
                Station nextStation = conn.getDestination();

                // Determine if this is a transfer
                boolean isTransfer = false;
                Station newLastTransportStation = lastTransportStation;

                if (conn.isTransfer()) {
                    // This is a transfer between bus and train in the same city
                    isTransfer = true;
                } else {
                    // Check if we're changing transport (different departure)
                    if (!conn.getSource().equals(lastTransportStation)) {
                        isTransfer = true;
                        newLastTransportStation = conn.getSource();
                    }
                }

                int newTransfers = currentTransfers + (isTransfer ? 1 : 0);

                List<State> stationStates = bestStates.computeIfAbsent(nextStation, k -> new ArrayList<>());
                if (stationStates.size() >= maxRoutes) {
                    State worstState = stationStates.stream().max(Comparator.comparingInt(s -> s.transfers)).orElse(null);
                    if (worstState != null && newTransfers >= worstState.transfers) {
                        continue;
                    }
                    stationStates.remove(worstState);
                }

                List<Graph.Connection> newPath = new ArrayList<>(currentPath);
                newPath.add(conn);
                List<Double> newWaitingTimes = new ArrayList<>(currentWaitingTimes);
                newWaitingTimes.add(0.0);
                State newState = new State(nextStation, newTransfers, LocalTime.of(0, 0), newTransfers, newPath, newWaitingTimes, newLastTransportStation);
                stationStates.add(newState);
                queue.add(newState);
            }
        }

        return topRoutes;
    }

    private double calculateWaitingTime(Graph.Connection conn, LocalTime currentTime) {
        if (conn.isTransfer()) {
            long transferTime = conn.getTravelTime();
            if (transferTime <= 0) {
                System.out.println("Invalid transfer time: " + transferTime + " for connection from " +
                        conn.getSource().getId() + " to " + conn.getDestination().getId());
                return 10;
            }
            return transferTime;
        }
        Departures dep = conn.getDeparture();
        LocalTime depTime = dep.getDepartureTime();
        long waiting = java.time.Duration.between(currentTime, depTime).toMinutes();
        if (waiting < 0) {
            waiting += 24 * 60;
        }
        long minWaitingTime = dep.getMinWaitingTime();
        if (minWaitingTime <= 0) {
            System.out.println("Invalid minWaitingTime: " + minWaitingTime + " for departure from " +
                    dep.getSource().getId() + " to " + dep.getDestination().getId());
            minWaitingTime = 5;
        }
        return waiting + minWaitingTime;
    }
}