package gradovi;

import stanice.BusStation;
import stanice.TrainStation;

public class City {

    private String name;
    private int x;
    private int y;
    private BusStation busStation;
    private TrainStation trainStation;

    public City() {
        x=0;
        y=0;
    }

    public City(String name, int x, int y, BusStation busStation, TrainStation trainStation) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.busStation = busStation;
        this.trainStation = trainStation;
    }

    public String getName() {return name;}
    public void setName(String name) {this.name = name;}

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }

    public int getY() { return y; }
    public void setY(int y) { this.y = y; }

    public BusStation getBusStation() {return busStation;}
    public void setBusStation(BusStation busStation) {this.busStation = busStation;}

    public TrainStation getTrainStation() {return trainStation;}
    public void setTrainStation(TrainStation trainStation) {this.trainStation = trainStation;}
}
