package graf;

import podaci.TransportDataParser;
import podaci.TransportDataGenerator;
import stanice.BusStation;
import stanice.Station;
import stanice.TrainStation;
import odlasci.Departures;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Builds a graph from TransportDataParser output.
 */
public class GraphBuilder {
    private final Graph graph;
    private final Properties config;
    private final Map<String, Station> stationMap;
    private final Map<String, Station> busStationMap;
    private final Map<String, Station> trainStationMap;

    public GraphBuilder() {
        this.graph = new Graph();
        this.config = loadProperties();
        this.stationMap = new HashMap<>();
        this.busStationMap = new HashMap<>();
        this.trainStationMap = new HashMap<>();
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        try {
            props.load(Files.newInputStream(Paths.get("config.properties")));
        } catch (IOException e) {
            props.setProperty("transfer_time", "10");
            props.setProperty("transfer_cost", "5.0");
        }
        return props;
    }

    public Graph buildFromJson(String jsonFilePath) throws IOException {
        TransportDataParser.TransportData data = TransportDataParser.readTransportData(jsonFilePath);

        for (TransportDataGenerator.Station s : data.stations) {
            Station busStation = new BusStation(s.busStation, s.city);
            Station trainStation = new TrainStation(s.trainStation, s.city);
            stationMap.put(s.busStation, busStation);
            stationMap.put(s.trainStation, trainStation);
            busStationMap.put(s.city, busStation);
            trainStationMap.put(s.city, trainStation);
            graph.addStation(busStation);
            graph.addStation(trainStation);

            double transferCost = Double.parseDouble(config.getProperty("transfer_cost", "5.0"));
            long transferTime = Long.parseLong(config.getProperty("transfer_time", "10"));
            graph.addTransfer(busStation, trainStation, transferCost, transferTime);
            graph.addTransfer(trainStation, busStation, transferCost, transferTime);
        }

        int skipped = 0;
        for (TransportDataGenerator.Departure d : data.departures) {
            Station source = stationMap.get(d.from);
            Station destination;
            if (d.type.equals("autobus")) {
                destination = busStationMap.get(d.to);
            } else if (d.type.equals("voz")) {
                destination = trainStationMap.get(d.to);
            } else {
                System.out.println("Invalid departure type: " + d.type);
                skipped++;
                continue;
            }

            if (source == null || destination == null) {
                System.out.println("Skipping departure: from=" + d.from + ", to=" + d.to);
                skipped++;
                continue;
            }

            LocalTime departureTime = LocalTime.parse(d.departureTime);
            LocalTime arrivalTime = departureTime.plusMinutes(d.duration);
            Departures departure = new Departures(source, destination, departureTime,
                    arrivalTime, d.price, d.minTransferTime);
            source.addDeparture(departure);
            graph.addConnection(departure);
        }
        if (skipped > 0) {
            System.out.println("Skipped " + skipped + " departures due to invalid stations or types.");
        }

        return graph;
    }

    public Map<String, Station> getBusStationMap() {
        return Collections.unmodifiableMap(busStationMap);
    }

    public Map<String, Station> getTrainStationMap() {
        return Collections.unmodifiableMap(trainStationMap);
    }
}
