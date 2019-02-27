package application;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class FXMLDocumentController implements Initializable {

    /************************/
    /*------INTERFACE-------*/
    /************************/
	
	@FXML
	private BorderPane contentPane;
	
	/******  CONTENT PANEL   ******/
    @FXML
    private MenuBar mainMenu;
    @FXML
    private VBox parametersVBox;
    @FXML
    private LineChart<Number,Number> chart;
    @FXML
    private NumberAxis xAxis;
    @FXML
    private NumberAxis yAxis;
    
    /****** MENU BAR ******/
    @FXML
    private MenuItem openMenuItem;
    @FXML
    private MenuItem saveMenuItem; 
    @FXML
    private MenuItem closeMenuItem; 

    /****** PARAMETERS PANEL ******/
    @FXML
    private ListView<Object> featuresList;
    @FXML
    private Button saveFeatureCoordinatesButton;
    @FXML
    private Button graphGoLeftButton;
    @FXML
    private Button graphGoRightButton;
    @FXML
    private Button graphZoomInButton;
    @FXML
    private Button graphZoomOutButton;
    @FXML
    private TextField pointHeightTextField;
    @FXML
    private Button pointHeightButton;
    
    /************************/
    /*--------DATA----------*/
    /************************/
    
    private Stage stage;
    private Scene scene;
    private Series<Number, Number> series;
    private File selectedFile;
    private File saveFile;
    
    private Data<Number, Number> lastData;
    
    private FeatureCollection<SimpleFeatureType, SimpleFeature> fc;
    private SimpleFeature selectedFeature;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        System.out.println("Bienvenue");
        //affichage du graph (add parameter data)
        series = new Series<Number, Number>();
        chart.setAnimated(false);        
        chart.getData().add(series);
        chart.setLegendVisible(false);
        saveMenuItem.setDisable(true);
        featuresList.setVisible(false);
                
        //texfield formatter
        Pattern validEditingState = Pattern.compile("-?(([1-9][0-9]*)|0)?(\\.[0-9]*)?");
        UnaryOperator<TextFormatter.Change> filter = c -> {
            String text = c.getControlNewText();
            if (validEditingState.matcher(text).matches()) return c;
            else return null;
        };
        StringConverter<Double> converter = new StringConverter<Double>() {
            @Override
            public Double fromString(String s) {
                if (s.isEmpty() || "-".equals(s) || ".".equals(s) || "-.".equals(s)) return 0.0 ;
                else return Double.valueOf(s);
            }
            @Override
            public String toString(Double d) {
                return d.toString();
            }
        };
        TextFormatter<Double> textFormatter = new TextFormatter<>(converter, 0.0, filter);
        pointHeightTextField.setTextFormatter(textFormatter);
    }
	
    /****** MENU BAR METHODS ******/
    //load geojson
    @FXML
    private void openGeoJson(ActionEvent event) {
        System.out.println("Open GeoJson");
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open GeoJson");
        fileChooser.getExtensionFilters().addAll(new ExtensionFilter("Json Files", "*.json"), new ExtensionFilter("All Files", "*.*"));
        selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
        	System.out.println(selectedFile.getName());
        	addActionClicList();
        	saveMenuItem.setDisable(false);
        }
    }
    
    //save new values in geojson
    //load geojson
    @FXML
    private void saveGeoJson(ActionEvent event) {
        System.out.println("Save GeoJson");
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save GeoJson");
        fileChooser.getExtensionFilters().addAll(new ExtensionFilter("Json Files", "*.json"), new ExtensionFilter("All Files", "*.*"));
        if(selectedFile != null){
        	fileChooser.setInitialDirectory(selectedFile.getParentFile());
        	fileChooser.setInitialFileName(selectedFile.getName());
        }
        saveFile = fileChooser.showSaveDialog(stage);
        
        if (saveFile != null){
        	System.out.println(saveFile.getPath());
        	System.out.println(saveFile.getName());
        	try {
				ApplicationUtils.featureCollectionToGeoJsonFile(fc, saveFile.getParentFile(), saveFile.getName());
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
    }    
    
    @FXML
    private void quitApplication(ActionEvent event) {
    	Platform.exit();
    }
    
    @FXML
    private void about(ActionEvent event) {
    	
    }
    
    /****** PARAMETERS PANEL METHODS ******/
    //load geojson
    @FXML
    private void graphGoLeft(ActionEvent event) {
    	System.out.println(xAxis.getLowerBound());
    	xAxis.setLowerBound(xAxis.getLowerBound() -1);
    }
    
    @FXML
    private void saveFeatureCoordinates() {
    	int i = 0;
    	for (Data<Number, Number> data : series.getData()) {
        	ApplicationUtils.saveCoordinates(selectedFeature, i, (double)data.getYValue());
        	i++;
        }
    }
    
    @FXML
    public void changeNodeValue(ActionEvent e) {
    	if(lastData != null) lastData.setYValue(Double.parseDouble(pointHeightTextField.getText()));
    }    
    
    
    
    /****** OTHER METHODS ******/
    public void addActionDragPoint() {
        for (Data<Number, Number> data : series.getData()) {
            Node node = data.getNode() ;
            node.setCursor(Cursor.HAND);
            node.setOnMousePressed(e -> {
            	if(lastData != data){
            		if(lastData != null){
            			lastData.getNode().setStyle("");
            		}
            		else {
                		pointHeightTextField.setDisable(false);
                		pointHeightButton.setDisable(false);
            		}
            		lastData = data;
            		node.setStyle("-fx-background-color: #00AA00, #000000;");
                    pointHeightTextField.setText(lastData.getYValue().toString());
            	}            	
            });
            node.setOnMouseDragged(e -> {
                Point2D pointInScene = new Point2D(e.getSceneX(), e.getSceneY());
                double yAxisLoc = yAxis.sceneToLocal(pointInScene).getY();
                Number y = yAxis.getValueForDisplay(yAxisLoc);
                data.setYValue(y);            
                pointHeightTextField.setText(y.toString());
            });
        }
	}
    
    public void addActionClicList() {
    	try {
    		fc = ApplicationUtils.geoJsonToFeatureCollection(selectedFile);
    		featuresList.setItems(null);
    		lastData = null;
    		pointHeightTextField.setDisable(true);
    		pointHeightButton.setDisable(true);
        	series.getData().clear();
			ObservableList<Object> observableList = FXCollections.observableArrayList(fc.toArray());
			featuresList.setVisible(true);
			featuresList.setItems(observableList);
			featuresList.setOnMouseClicked(new EventHandler<MouseEvent>() {
		        @Override
		        public void handle(MouseEvent event) {
		        	selectedFeature = (SimpleFeature) featuresList.getSelectionModel().getSelectedItem();
		        	series.getData().clear();
		    		lastData = null;
		    		pointHeightTextField.setDisable(true);
		    		pointHeightButton.setDisable(true);
		            ArrayList<Data<Number, Number>> datas = ApplicationUtils.loadCoordinates(selectedFeature);			            	            
		            datas.forEach(data -> {
		            	series.getData().add(data);
		            });
		            addActionDragPoint();
		        }
		    });
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
    
    void findStage(Stage stage) {
        this.stage = stage;
    }
    
    void findScene(Scene scene) {
        this.scene = scene;
    }
}
