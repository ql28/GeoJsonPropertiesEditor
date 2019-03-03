package application;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import org.apache.commons.lang.ClassUtils;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.type.Descriptors;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.PropertyDescriptor;

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
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
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
    private ListView<Object> parametersList;
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
    private Button addParametersButton;
    @FXML
    private TextField pointHeightTextField;
    @FXML
    private Button pointHeightButton;
    
    /************************/
    /*--------DATA----------*/
    /************************/
    
    private Stage stage;
    private Scene scene;
    private File selectedFile;
    private File saveFile;
        
    private FeatureCollection<SimpleFeatureType, SimpleFeature> fc;
    private SimpleFeature selectedFeature;
    private SimpleFeatureType featureTypeParameters;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        System.out.println("Bienvenue");
        saveMenuItem.setDisable(true);
        featuresList.setVisible(false);
                
//        //texfield formatter
//        Pattern validEditingState = Pattern.compile("-?(([1-9][0-9]*)|0)?(\\.[0-9]*)?");
//        UnaryOperator<TextFormatter.Change> filter = c -> {
//            String text = c.getControlNewText();
//            if (validEditingState.matcher(text).matches()) return c;
//            else return null;
//        };
//        StringConverter<Double> converter = new StringConverter<Double>() {
//            @Override
//            public Double fromString(String s) {
//                if (s.isEmpty() || "-".equals(s) || ".".equals(s) || "-.".equals(s)) return 0.0 ;
//                else return Double.valueOf(s);
//            }
//            @Override
//            public String toString(Double d) {
//                return d.toString();
//            }
//        };
//        TextFormatter<Double> textFormatter = new TextFormatter<>(converter, 0.0, filter);
//        pointHeightTextField.setTextFormatter(textFormatter);
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
        	loadFeatureCollectionParameters();
        	saveMenuItem.setDisable(false);
        }
    }
    
    //save new values in geojson
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
    private void saveFeatureParameters() {
    	int i = 0;
//    	for (Data<Number, Number> data : series.getData()) {
//        	ApplicationUtils.saveCoordinates(selectedFeature, i, (double)data.getYValue());
//        	i++;
//        }
    }
    
    @FXML
    private void saveParametersConfiguration(){
    	
    }
    
    @FXML
    private void addParameters(){
    	HBox hb = new HBox();
		TextField paramName = new TextField();
		hb.getChildren().add(paramName);
		ChoiceBox<Serializable> cb = new ChoiceBox<Serializable>(FXCollections.observableArrayList(Long.class, String.class, Boolean.class));
		hb.getChildren().add(cb);
		parametersVBox.getChildren().add(hb);
    }
    
    /****** OTHER METHODS ******/
    public void loadFeatureCollectionParameters(){
    	featureTypeParameters = ApplicationUtils.loadFeatureCollectionParameters(fc);
    	Collection<PropertyDescriptor> descriptors = featureTypeParameters.getDescriptors();
    	ArrayList<Object> ret = new ArrayList<Object>();
    	Iterator<PropertyDescriptor> i = descriptors.iterator();
    	addParametersButton.setDisable(false);
    	while(i.hasNext()){
    		
    		PropertyDescriptor desc = i.next();

    		ret.add(desc);
    		//).add((desc.getName().toString(), desc.getType().getBinding().toString());
    		
//    		HBox hb = new HBox();
//    		TextField paramName = new TextField();
//    		paramName.setText(desc.getName().toString());
//    		hb.getChildren().add(paramName);
//    		if(desc.getType().getBinding() == Boolean.class){
//    			CheckBox cb = new CheckBox();
//    			hb.getChildren().add(cb);
//    		}
//    		else if(desc.getType().getBinding() == Long.class){
//    			TextField tf = new TextField();
//    			hb.getChildren().add(tf);	
//    		}
//    		else if(desc.getType().getBinding() == String.class){
//    			TextField tf = new TextField();
//    			hb.getChildren().add(tf);
//    		}
//			parametersVBox.getChildren().add(hb);
    		
        	HBox hb = new HBox();
    		TextField paramName = new TextField();
    		paramName.setText(desc.getName().toString());
    		hb.getChildren().add(paramName);
    		ChoiceBox<Serializable> cb = new ChoiceBox<Serializable>(FXCollections.observableArrayList(Long.class, String.class, Boolean.class));
    		cb.getSelectionModel().select(desc.getType().getBinding());
    		hb.getChildren().add(cb);
    		if(cb.getSelectionModel().getSelectedIndex() != -1)	parametersVBox.getChildren().add(hb);
    	}
    	System.out.println(featureTypeParameters.getDescriptors().isEmpty());
    }
    
    public void addActionClicList() {
    	try {
    		fc = ApplicationUtils.geoJsonToFeatureCollection(selectedFile);
    		featuresList.setItems(null);
    		//lastData = null;
    		//pointHeightTextField.setDisable(true);
    		//pointHeightButton.setDisable(true);
        	//series.getData().clear();
			ObservableList<Object> observableList = FXCollections.observableArrayList(fc.toArray());
			featuresList.setVisible(true);
			featuresList.setItems(observableList);
			featuresList.setOnMouseClicked(new EventHandler<MouseEvent>() {
		        @Override
		        public void handle(MouseEvent event) {
		        	selectedFeature = (SimpleFeature) featuresList.getSelectionModel().getSelectedItem();
		        	parametersVBox.getChildren().clear();
		        	
		        	Collection<Property> properties = selectedFeature.getProperties();
		        	Iterator<Property> i = properties.iterator();
		        	addParametersButton.setDisable(false);
		        	while(i.hasNext()){
		        		Property prop = i.next();		        		
		            	HBox hb = new HBox();
		        		Label paramName = new Label();
		        		paramName.setText(prop.getName().toString());
		        		hb.getChildren().add(paramName);
		        		if(prop.getType().getBinding() == Boolean.class){
		        			CheckBox cb = new CheckBox();
		        			hb.getChildren().add(cb);
		        		}
		        		else if(prop.getType().getBinding() == Long.class){
		        			TextField tf = new TextField();
		        			tf.setText(prop.getValue().toString());
		        			hb.getChildren().add(tf);	
		        		}
		        		else if(prop.getType().getBinding() == String.class){
		        			TextField tf = new TextField();
		        			tf.setText(prop.getValue().toString());
		        			hb.getChildren().add(tf);
		        		}
		    			parametersVBox.getChildren().add(hb);
		    		}	        	
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
