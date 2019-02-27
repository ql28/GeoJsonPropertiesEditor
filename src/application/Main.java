package application;
	
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
	
    @Override
    public void start(Stage stage) throws Exception {
		
		stage.setTitle("test chart");

    	FXMLLoader loader = new FXMLLoader(getClass().getResource("FXMLDocument.fxml"));
        Parent root = (Parent)loader.load();

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        stage.show();
        
        //affichage de l'animation au lancement
        FXMLDocumentController controller = (FXMLDocumentController)loader.getController();
        controller.findStage(stage); 
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}