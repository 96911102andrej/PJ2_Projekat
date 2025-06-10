package odlasci;

import stanice.Station;
import java.time.LocalTime;

/**
 * Represents a departure schedule from one station to another.
 */
public class Departures {
    private final Station source;
    private final Station destination;
    private final LocalTime departureTime;
    private final LocalTime arrivalTime;
    private final double price;
    private final int minWaitingTime; // Minimum waiting time at destination (in minutes)

    public Departures(Station source, Station destination, LocalTime departureTime,
                      LocalTime arrivalTime, double price, int minWaitingTime) {
        this.source = source;
        this.destination = destination;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.price = price;
        this.minWaitingTime = minWaitingTime;
    }

    public Station getSource() { return source; }
    public Station getDestination() { return destination; }
    public LocalTime getDepartureTime() { return departureTime; }
    public LocalTime getArrivalTime() { return arrivalTime; }
    public double getPrice() { return price; }
    public int getMinWaitingTime() { return minWaitingTime; }

    /**
     * Calculates travel time in minutes.
     */
    public long getTravelTime() {
        return java.time.Duration.between(departureTime, arrivalTime).toMinutes();
    }

    @Override
    public String toString() {
        return String.format("%s -> %s (%s to %s, %.2f KM)",
                source.getId(), destination.getId(), departureTime, arrivalTime, price);
    }
}