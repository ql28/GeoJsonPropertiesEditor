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
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.type.Descriptors;
import org.geotools.referencing.CRS;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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
import javafx.scene.control.SelectionModel;
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
    private Button showParametersButton;
    @FXML
    private Button saveParametersConfigButton;
    @FXML
    private Button saveParametersValuesButton;
    @FXML
    private ChoiceBox<String> crsChoiceBox;
    
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

    private CoordinateReferenceSystem selectedCRS;
    private List<Button> delButtonList = new ArrayList<Button>();
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        System.out.println("Bienvenue");
        saveMenuItem.setDisable(true);
        featuresList.setVisible(false);
        
        crsChoiceBox.setItems(FXCollections.observableArrayList(
    	    "EPSG:4326",
    	    "EPSG:32735",
    	    "EPSG:23032"
    	));
        
        crsChoiceBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				try {
					selectedCRS = CRS.decode(crsChoiceBox.getSelectionModel().getSelectedItem().toString());
				} catch (FactoryException e) {
					e.printStackTrace();
				}
            }
        });
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
    		try {
				fc = ApplicationUtils.geoJsonToFeatureCollection(selectedFile);
				selectedCRS = ApplicationUtils.geoJsonToCoordinateReferenceSystem(selectedFile);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
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
    private void saveParametersConfiguration(){
		SimpleFeatureTypeBuilder simpleFeatureTypeBuilder = new SimpleFeatureTypeBuilder();
		simpleFeatureTypeBuilder.setName("featureType");
		simpleFeatureTypeBuilder.setCRS(selectedCRS);
		simpleFeatureTypeBuilder.add("geometry", featureTypeParameters.getGeometryDescriptor().getType().getBinding());
				
		parametersVBox.getChildren().forEach(child ->{
    		ObservableList<Node> params = ((HBox) child).getChildren();
			try{ simpleFeatureTypeBuilder.add(((TextField)params.get(0)).getText(), (Class<?>)((ChoiceBox)params.get(1)).getSelectionModel().getSelectedItem()); }
			catch(Exception e){}
		});
		
		// init DefaultFeatureCollection
		SimpleFeatureBuilder simpleFeatureBuilder = new SimpleFeatureBuilder(simpleFeatureTypeBuilder.buildFeatureType());
		DefaultFeatureCollection resultFeatureCollection = new DefaultFeatureCollection(null, simpleFeatureBuilder.getFeatureType());
		FeatureIterator<SimpleFeature> iterator = fc.features();
		while(iterator.hasNext()){
			SimpleFeature myFeature = iterator.next();
	    	Collection<PropertyDescriptor> descriptors = resultFeatureCollection.getSchema().getDescriptors();
	    	Iterator<PropertyDescriptor> i = descriptors.iterator();
	    	while(i.hasNext()){
	    		PropertyDescriptor prop = i.next();
	    		if(myFeature.getProperty(prop.getName()) != null){
	    			simpleFeatureBuilder.add(myFeature.getProperty(prop.getName()).getValue());
	    		}
	    		else{
	    			if(prop.getType().getBinding() == Double.class) simpleFeatureBuilder.add(0.0);
	    			else if(prop.getType().getBinding() == Long.class) simpleFeatureBuilder.add(0);
	    			else if(prop.getType().getBinding() == String.class) simpleFeatureBuilder.add("");
	    			else if(prop.getType().getBinding() == Boolean.class) simpleFeatureBuilder.add(false);
	    			else simpleFeatureBuilder.add(null);
	    		}
	    	}
			SimpleFeature sf = simpleFeatureBuilder.buildFeature(null);
			resultFeatureCollection.add(sf);
		}
		
		fc = resultFeatureCollection;
    	addActionClicList();
		loadFeatureCollectionParameters();
	}
    
    @FXML
    private void saveParametersValues(){
    	parametersVBox.getChildren().forEach(child ->{
    		ObservableList<Node> params = ((HBox) child).getChildren();
    		
    		String parameterName = ((Label)params.get(0)).getText();

    		if(selectedFeature.getProperty(parameterName).getType().getBinding() == Double.class){
    			double parameterDouble = Double.parseDouble(((TextField)params.get(1)).getText());
    			selectedFeature.getProperty(parameterName).setValue(parameterDouble);    			
    		}
    		else if(selectedFeature.getProperty(parameterName).getType().getBinding() == Long.class){
    			long parameterLong = Long.parseLong(((TextField)params.get(1)).getText());
    			selectedFeature.getProperty(parameterName).setValue(parameterLong);    			
    		}
    		else if(selectedFeature.getProperty(parameterName).getType().getBinding() == String.class){
    			String parameterString = ((TextField)params.get(1)).getText();
        		selectedFeature.getProperty(parameterName).setValue(parameterString);
    		}
    		else if(selectedFeature.getProperty(parameterName).getType().getBinding() == Boolean.class){
    			boolean parameterBool = ((CheckBox)params.get(1)).selectedProperty().getValue(); 
    			System.out.println(parameterBool);
        		selectedFeature.getProperty(parameterName).setValue(parameterBool);
    		}
    	});
    }
    
    @FXML
    private void addParameters(){
    	HBox hb = new HBox();
		TextField paramName = new TextField();
		hb.getChildren().add(paramName);
		ChoiceBox<Serializable> cb = new ChoiceBox<Serializable>(FXCollections.observableArrayList(Long.class, Double.class, String.class, Boolean.class));
		hb.getChildren().add(cb);
		Button delButton = new Button();
		delButton.setText("X");
		delButtonList.add(delButton);
		hb.getChildren().add(delButton);
		parametersVBox.getChildren().add(hb);
    	addActionClicDelButton();
    }   
    
    /****** OTHER METHODS ******/
    public void loadFeatureCollectionParameters(){
    	featureTypeParameters = ApplicationUtils.loadFeatureCollectionParameters(fc);
    	Collection<PropertyDescriptor> descriptors = featureTypeParameters.getDescriptors();
    	ArrayList<Object> ret = new ArrayList<Object>();
    	Iterator<PropertyDescriptor> i = descriptors.iterator();
    	parametersVBox.getChildren().clear();
    	addParametersButton.setDisable(false);
    	crsChoiceBox.setDisable(false);
    	showParametersButton.setDisable(true);
    	saveParametersConfigButton.setDisable(false);
    	saveParametersValuesButton.setDisable(true);
    	while(i.hasNext()){    		
    		PropertyDescriptor desc = i.next();
    		ret.add(desc);
    	
        	HBox hb = new HBox();
    		TextField paramName = new TextField();
    		paramName.setText(desc.getName().toString());
    		hb.getChildren().add(paramName);
    		ChoiceBox<Serializable> cb = new ChoiceBox<Serializable>(FXCollections.observableArrayList(Long.class, Double.class, String.class, Boolean.class));
    		cb.getSelectionModel().select(desc.getType().getBinding());
    		hb.getChildren().add(cb);
    		Button delButton = new Button();
    		delButton.setText("X");
    		delButtonList.add(delButton);
    		hb.getChildren().add(delButton);
    		if(cb.getSelectionModel().getSelectedIndex() != -1)	parametersVBox.getChildren().add(hb);
    	}
    	addActionClicDelButton();
    }
    
    public void addActionClicDelButton(){    	
    	delButtonList.forEach(delButton ->{
    		delButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
    			@Override
    			public void handle(MouseEvent arg0) {
    				parametersVBox.getChildren().remove(delButton.getParent());
    			}    			
    		});	
    	});
    }
    
    public void addActionClicList() {
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
	        	addParametersButton.setDisable(true);
	        	crsChoiceBox.setDisable(true);
	        	showParametersButton.setDisable(false);
	        	saveParametersConfigButton.setDisable(true);
	        	saveParametersValuesButton.setDisable(false);
	        	parametersVBox.getChildren().clear();
	        	
	        	Collection<Property> properties = selectedFeature.getProperties();
	        	Iterator<Property> i = properties.iterator();
	        	while(i.hasNext()){
	        		Property prop = i.next();		        		
	            	HBox hb = new HBox();
	        		Label paramName = new Label();
	        		paramName.setText(prop.getName().toString());
	        		paramName.setPrefWidth(200);
	        		hb.getChildren().add(paramName);
	        		if(prop.getType().getBinding() == Boolean.class){
	        			CheckBox cb = new CheckBox();
	        			try{ cb.selectedProperty().set(Boolean.parseBoolean(prop.getValue().toString())); }
	        			catch(Exception e){}
	        			hb.getChildren().add(cb);
		    			parametersVBox.getChildren().add(hb);
	        		}
	        		else if(prop.getType().getBinding() == Double.class){
	        			TextField tf = new TextField();
	        			try{ tf.setText(prop.getValue().toString()); }
	        			catch(Exception e){}
	        			hb.getChildren().add(tf);	
		    			parametersVBox.getChildren().add(hb);
	        		}
	        		else if(prop.getType().getBinding() == Long.class){
	        			TextField tf = new TextField();
	        			try{ tf.setText(prop.getValue().toString()); }
	        			catch(Exception e){}
	        			hb.getChildren().add(tf);	
		    			parametersVBox.getChildren().add(hb);
	        		}
	        		else if(prop.getType().getBinding() == String.class){
	        			TextField tf = new TextField();
	        			try{ tf.setText(prop.getValue().toString()); }
	        			catch(Exception e){}
	        			hb.getChildren().add(tf);
		    			parametersVBox.getChildren().add(hb);
	        		}
	    		}	        	
	        }
	    });
	}
    
    void findStage(Stage stage) {
        this.stage = stage;
    }
    
    void findScene(Scene scene) {
        this.scene = scene;
    }
}
