package stanice;

import odlasci.Departures;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Abstract class representing a bus or train station in a city.
 */
public abstract class Station {
    private final String id; // e.g., A_X_Y or Z_X_Y
    private final String cityId; // e.g., G_X_Y
    private final String type; // "bus" or "train"
    private final List<Departures> departures;

    public Station(String id, String cityId, String type) {
        this.id = id;
        this.cityId = cityId;
        this.type = type;
        this.departures = new ArrayList<>();
    }

    public String getId() { return id; }
    public String getCityId() { return cityId; }
    public String getType() { return type; }
    public List<Departures> getDepartures() { return new ArrayList<>(departures); }
    public void addDeparture(Departures departure) { departures.add(departure); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Station station = (Station) o;
        return id.equals(station.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id + " (" + type + ")";
    }
}