package kontroleri;

import podaci.TransportDataGenerator;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PocetnaKontroler {

    @FXML private TextField rowField;
    @FXML private TextField colField;
    @FXML private Label totalTicketsSoldLabel;
    @FXML private Label totalEarningsLabel;
    @FXML private Button generateButton;

    @FXML
    private void initialize() {
        System.out.println("PocetnaKontroler initialized");
        updateTicketStats();
        if (generateButton == null) {
            System.err.println("generateButton is null - check FXML binding");
        }
    }

    @FXML
    private void generateAndProceed() {
        System.out.println("generateAndProceed started");
        try {
            String rowText = rowField.getText().trim();
            String colText = colField.getText().trim();
            System.out.println("Row input: " + rowText + ", Col input: " + colText);

            int rows = Integer.parseInt(rowText);
            int cols = Integer.parseInt(colText);

            if (rows < 1 || cols < 1) {
                showAlert("Greška", "Dimenzije matrice moraju biti veće od 0.");
                System.out.println("Invalid dimensions: rows=" + rows + ", cols=" + cols);
                return;
            }

            System.out.println("Generating transport data...");
            TransportDataGenerator generator = new TransportDataGenerator(rows, cols);
            TransportDataGenerator.TransportData data = generator.generateData();
            generator.saveToJson(data, "transport_data.json");
            System.out.println("Transport data saved to transport_data.json");

            System.out.println("Glavna.fxml resource: " + getClass().getResource("/resursi/Glavna.fxml"));
            if (getClass().getResource("/resursi/Glavna.fxml") == null) {
                throw new IOException("Glavna.fxml not found in /resursi/");
            }

            System.out.println("Loading Glavna.fxml...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resursi/Glavna.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Pretraga ruta");
            stage.setResizable(true); // Allow resizing of Glavna window
            stage.show();
            System.out.println("Glavna.fxml window opened");

            Stage currentStage = (Stage) generateButton.getScene().getWindow();
            currentStage.close();
            System.out.println("Pocetna window closed");

        } catch (NumberFormatException e) {
            showAlert("Greška", "Unesite validne brojeve za redove i kolone.");
            System.err.println("NumberFormatException: " + e.getMessage());
        } catch (IOException e) {
            showAlert("Greška", "Greška prilikom učitavanja Glavna.fxml: " + e.getMessage());
            System.err.println("IOException: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            showAlert("Greška", "Nepoznata greška: " + e.getMessage());
            System.err.println("Unexpected exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateTicketStats() {
        try {
            Path racuniFolder = Paths.get("racuni");
            if (!Files.exists(racuniFolder)) {
                Files.createDirectory(racuniFolder);
            }

            List<Path> receiptFiles = Files.list(racuniFolder)
                    .filter(p -> p.toString().endsWith(".txt"))
                    .toList();

            int totalTickets = receiptFiles.size();
            double totalEarnings = 0.0;

            Pattern[] patterns = {
                    Pattern.compile("Cijena: ([\\d.,]+) KM"),
                    Pattern.compile("Ukupno: ([\\d.,]+) KM")
            };
            for (Path receipt : receiptFiles) {
                String content = Files.readString(receipt);
                boolean found = false;
                for (Pattern costPattern : patterns) {
                    Matcher matcher = costPattern.matcher(content);
                    if (matcher.find()) {
                        String value = matcher.group(1).replace(',', '.');
                        totalEarnings += Double.parseDouble(value);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    System.err.println("Nije pronađena cijena u: " + receipt.getFileName());
                }
            }

            totalTicketsSoldLabel.setText(String.valueOf(totalTickets));
            totalEarningsLabel.setText(String.format("%.2f", totalEarnings));
            System.out.println("Ticket stats updated: tickets=" + totalTickets + ", earnings=" + totalEarnings);

        } catch (Exception e) {
            totalTicketsSoldLabel.setText("0");
            totalEarningsLabel.setText("0.00");
            System.err.println("Error updating ticket stats: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}