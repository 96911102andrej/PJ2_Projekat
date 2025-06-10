package graf;

import stanice.Station;
import odlasci.Departures;

import java.util.*;

/**
 * Represents a directed weighted graph of stations and connections.
 */
public class Graph {
    private final Map<Station, List<Connection>> adjacencyList;

    public Graph() {
        this.adjacencyList = new HashMap<>();
    }

    public void addStation(Station station) {
        adjacencyList.putIfAbsent(station, new ArrayList<>());
    }

    public void addConnection(Departures departure) {
        Station source = departure.getSource();
        Station destination = departure.getDestination();
        addStation(source);
        addStation(destination);
        adjacencyList.get(source).add(new Connection(source, destination, departure.getPrice(),
                departure.getTravelTime(), false, departure));
    }

    public void addTransfer(Station source, Station destination, double transferCost, long transferTime) {
        addStation(source);
        addStation(destination);
        adjacencyList.get(source).add(new Connection(source, destination, transferCost, transferTime, true, null));
    }

    public List<Connection> getConnections(Station station) {
        return adjacencyList.getOrDefault(station, Collections.emptyList());
    }

    public Set<Station> getStations() {
        return adjacencyList.keySet();
    }

    /**
     * Inner class representing a connection (edge) between two stations.
     */
    public static class Connection {
        private final Station source;
        private final Station destination;
        private final double cost;
        private final long travelTime; // in minutes
        private final boolean isTransfer;
        private final Departures departure; // null for transfers

        public Connection(Station source, Station destination, double cost, long travelTime,
                          boolean isTransfer, Departures departure) {
            this.source = source;
            this.destination = destination;
            this.cost = cost;
            this.travelTime = travelTime;
            this.isTransfer = isTransfer;
            this.departure = departure;
        }

        public Station getSource() { return source; }
        public Station getDestination() { return destination; }
        public double getCost() { return cost; }
        public long getTravelTime() { return travelTime; }
        public boolean isTransfer() { return isTransfer; }
        public Departures getDeparture() { return departure; }
    }
}