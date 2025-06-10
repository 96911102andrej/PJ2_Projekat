import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;




public class TransportApp extends Application {

    public static void main (String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage){
        try{
            Parent root = FXMLLoader.load(getClass().getResource("/resursi/Pocetna.fxml"));
            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            primaryStage.show();
            primaryStage.setTitle("Početna");
            Image icon = new Image("auto.png");
            primaryStage.getIcons().add(icon);
            primaryStage.setResizable(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
