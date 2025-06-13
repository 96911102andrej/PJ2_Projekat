package podaci;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class TransportDataGenerator {
    private final int n;
    private final int m;
    private static final int DEPARTURES_PER_STATION = 15;
    private final Random random = new Random();

    public TransportDataGenerator(int n, int m) {
        this.n = n;
        this.m = m;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Unesite broj redova (n): ");
        int rows = scanner.nextInt();

        System.out.println("Unesite broj kolona (m): ");
        int cols = scanner.nextInt();

        TransportDataGenerator generator = new TransportDataGenerator(rows, cols);
        TransportData data = generator.generateData();
        generator.saveToJson(data, "transport_data.json");

        System.out.println("Podaci za matricu " + rows + "x" + cols + " su generisani i sačuvani kao transport_data.json");
    }


    public static class TransportData {
        public String[][] countryMap;
        public List<Station> stations;
        public List<Departure> departures;
    }

    public static class Station {
        public String city;
        public String busStation;
        public String trainStation;
    }

    public static class Departure {
        public String type; // "autobus" ili "voz"
        public String from;
        public String to;
        public String departureTime;
        public int duration; // u minutama
        public int price;
        public int minTransferTime; // u minutama
    }

    public TransportData generateData() {
        TransportData data = new TransportData();
        data.countryMap = generateCountryMap();
        data.stations = generateStations();
        data.departures = generateDepartures(data.stations);
        return data;
    }

    private String[][] generateCountryMap() {
        String[][] countryMap = new String[n][m];
        for (int x = 0; x < n; x++) {
            for (int y = 0; y < m; y++) {
                countryMap[x][y] = "G_" + x + "_" + y;
            }
        }
        return countryMap;
    }

    private List<Station> generateStations() {
        List<Station> stations = new ArrayList<>();
        for (int x = 0; x < n; x++) {
            for (int y = 0; y < m; y++) {
                Station station = new Station();
                station.city = "G_" + x + "_" + y;
                station.busStation = "A_" + x + "_" + y;
                station.trainStation = "Z_" + x + "_" + y;
                stations.add(station);
            }
        }
        return stations;
    }

    private List<Departure> generateDepartures(List<Station> stations) {
        List<Departure> departures = new ArrayList<>();

        for (Station station : stations) {
            int x = Integer.parseInt(station.city.split("_")[1]);
            int y = Integer.parseInt(station.city.split("_")[2]);

            for (int i = 0; i < DEPARTURES_PER_STATION; i++) {
                departures.add(generateDeparture("autobus", station.busStation, x, y));
                departures.add(generateDeparture("voz", station.trainStation, x, y));
            }
        }
        return departures;
    }

    private Departure generateDeparture(String type, String from, int x, int y) {
        Departure departure = new Departure();
        departure.type = type;
        departure.from = from;

        List<String> neighbors = getNeighbors(x, y);
        departure.to = neighbors.isEmpty() ? from : neighbors.get(random.nextInt(neighbors.size()));

        int hour = random.nextInt(24);
        int minute = random.nextInt(4) * 15;
        departure.departureTime = String.format("%02d:%02d", hour, minute);

        departure.duration = 30 + random.nextInt(151); // 30–180 min
        departure.price = 100 + random.nextInt(901);   // 100–1000 KM
        departure.minTransferTime = 5 + random.nextInt(26); // 5–30 min

        return departure;
    }

    private List<String> getNeighbors(int x, int y) {
        List<String> neighbors = new ArrayList<>();
        int[][] directions = { {-1, 0}, {1, 0}, {0, -1}, {0, 1} };

        for (int[] dir : directions) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            if (nx >= 0 && nx < n && ny >= 0 && ny < m) {
                neighbors.add("G_" + nx + "_" + ny);
            }
        }
        return neighbors;
    }

    public void saveToJson(TransportData data, String filename) {
        try (FileWriter file = new FileWriter(filename)) {
            StringBuilder json = new StringBuilder();
            json.append("{\n");

            // countryMap
            json.append("  \"countryMap\": [\n");
            for (int i = 0; i < n; i++) {
                json.append("    [");
                for (int j = 0; j < m; j++) {
                    json.append("\"").append(data.countryMap[i][j]).append("\"");
                    if (j < m - 1) json.append(", ");
                }
                json.append("]");
                if (i < n - 1) json.append(",");
                json.append("\n");
            }
            json.append("  ],\n");

            // stations
            json.append("  \"stations\": [\n");
            for (int i = 0; i < data.stations.size(); i++) {
                Station s = data.stations.get(i);
                json.append("    {\"city\": \"").append(s.city)
                        .append("\", \"busStation\": \"").append(s.busStation)
                        .append("\", \"trainStation\": \"").append(s.trainStation)
                        .append("\"}");
                if (i < data.stations.size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("  ],\n");

            // departures
            json.append("  \"departures\": [\n");
            for (int i = 0; i < data.departures.size(); i++) {
                Departure d = data.departures.get(i);
                json.append("    {\"type\": \"").append(d.type)
                        .append("\", \"from\": \"").append(d.from)
                        .append("\", \"to\": \"").append(d.to)
                        .append("\", \"departureTime\": \"").append(d.departureTime)
                        .append("\", \"duration\": ").append(d.duration)
                        .append(", \"price\": ").append(d.price)
                        .append(", \"minTransferTime\": ").append(d.minTransferTime)
                        .append("}");
                if (i < data.departures.size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("  ]\n");

            json.append("}");
            file.write(json.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}