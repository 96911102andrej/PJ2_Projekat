package racun;

import graf.Graph;
import graf.RouteFinder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class ReceiptManager {

    public static void saveReceipt(RouteFinder.Route route, LocalDate date) {
        try {
            Path racuniFolder = Paths.get("racuni");
            if (!Files.exists(racuniFolder)) {
                Files.createDirectory(racuniFolder);
            }

            String receiptId = UUID.randomUUID().toString();
            Path receiptPath = racuniFolder.resolve("racun_" + receiptId + ".txt");

            StringBuilder content = new StringBuilder();
            content.append("Račun ID: ").append(receiptId).append("\n");
            content.append("Datum: ").append(date).append("\n");
            content.append("Ruta:\n");

            LocalTime currentTime = LocalTime.of(8, 0);
            for (int i = 0; i < route.getConnections().size(); i++) {
                Graph.Connection conn = route.getConnections().get(i);
                double waitingTime = route.getWaitingTimes().get(i);
                if (conn.isTransfer()) {
                    content.append(String.format("Transfer: %s -> %s (%.2f KM, %.0f min)\n",
                            conn.getSource().getId(), conn.getDestination().getId(),
                            conn.getCost(), waitingTime));
                } else {
                    content.append(String.format("Prevoz: %s -> %s (%s to %s, %.2f KM, %d min)\n",
                            conn.getSource().getId(), conn.getDestination().getId(),
                            conn.getDeparture().getDepartureTime(), conn.getDeparture().getArrivalTime(),
                            conn.getCost(), conn.getTravelTime()));
                }
                currentTime = conn.isTransfer() ? currentTime.plusMinutes((long) waitingTime) : conn.getDeparture().getArrivalTime();
            }

            content.append(String.format("Cijena: %.2f KM\n", route.getTotalCost()));
            content.append(String.format("Trajanje: %d min\n", route.getTotalTime()));
            content.append(String.format("Presjedanja: %d\n", route.getTransfers()));

            Files.writeString(receiptPath, content.toString());
        } catch (IOException e) {
            System.err.println("Greška prilikom čuvanja računa: " + e.getMessage());
        }
    }
}