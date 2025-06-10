package podaci;

import javax.json.*;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class TransportDataParser {

    public static class TransportData {
        public String[][] countryMap;
        public List<TransportDataGenerator.Station> stations;
        public List<TransportDataGenerator.Departure> departures;
    }

    public static TransportData readTransportData(String filename) {
        TransportData data = new TransportData();

        try (JsonReader reader = Json.createReader(new FileReader(filename))) {
            JsonObject root = reader.readObject();

            // Parsiranje countryMap
            JsonArray mapArray = root.getJsonArray("countryMap");
            int rows = mapArray.size();
            int cols = mapArray.getJsonArray(0).size();
            data.countryMap = new String[rows][cols];

            for (int i = 0; i < rows; i++) {
                JsonArray row = mapArray.getJsonArray(i);
                for (int j = 0; j < cols; j++) {
                    data.countryMap[i][j] = row.getString(j);
                }
            }

            // Parsiranje stations
            data.stations = new ArrayList<>();
            JsonArray stationsArray = root.getJsonArray("stations");
            for (JsonValue val : stationsArray) {
                JsonObject obj = val.asJsonObject();
                TransportDataGenerator.Station s = new TransportDataGenerator.Station();
                s.city = obj.getString("city");
                s.busStation = obj.getString("busStation");
                s.trainStation = obj.getString("trainStation");
                data.stations.add(s);
            }

            // Parsiranje departures
            data.departures = new ArrayList<>();
            JsonArray departuresArray = root.getJsonArray("departures");
            for (JsonValue val : departuresArray) {
                JsonObject obj = val.asJsonObject();
                TransportDataGenerator.Departure d = new TransportDataGenerator.Departure();
                d.type = obj.getString("type");
                d.from = obj.getString("from");
                d.to = obj.getString("to");
                d.departureTime = obj.getString("departureTime");
                d.duration = obj.getInt("duration");
                d.price = obj.getInt("price");
                d.minTransferTime = obj.getInt("minTransferTime");
                data.departures.add(d);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return data;
    }

    public static void main(String[] args) {
        TransportData data = readTransportData("transport_data.json");
        System.out.println("Učitano: " + data.countryMap.length + " x " + data.countryMap[0].length + " mapa.");
        System.out.println("Broj stanica: " + data.stations.size());
        System.out.println("Broj polazaka: " + data.departures.size());
    }
}
