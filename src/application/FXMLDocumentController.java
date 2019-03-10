package application;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

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
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
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
    @FXML
    private MenuBar mainMenu;
    @FXML
    private VBox parametersVBox;
    @FXML
    private HBox parametersConfigurationHBox;
    @FXML
    private HBox featureIdHBox;
    @FXML
    private LineChart<Number,Number> chart;
    @FXML
    private NumberAxis xAxis;
    @FXML
    private NumberAxis yAxis;
    @FXML
    private MenuItem openMenuItem;
    @FXML
    private MenuItem saveMenuItem; 
    @FXML
    private MenuItem closeMenuItem;
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
    private TextField featureIdTextField;
    @FXML
    private ChoiceBox<String> crsChoiceBox;
    @FXML
    private TextField pointHeightTextField;
    @FXML
    private Button pointHeightButton;
    
    /************************/
    /*--------DATA----------*/
    /************************/
    
    //general variables
    private Stage stage;
    private Scene scene;
    private File selectedFile;
    private File saveFile;
    private FeatureCollection<SimpleFeatureType, SimpleFeature> fc;
    private SimpleFeature selectedFeature;
    private SimpleFeatureType featureTypeParameters;
    
    //chart variables
    private Series<Number, Number> series;
    private Data<Number, Number> lastData;
    
    //parameters variables
    private CoordinateReferenceSystem selectedCRS;
    private List<Button> delButtonList = new ArrayList<Button>();
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
    	
    	saveMenuItem.setDisable(true);

    	//initialize the chart
        series = new Series<Number, Number>();
        chart.setAnimated(false);        
        chart.getData().add(series);
        chart.setLegendVisible(false);
        
        //initialize the CRS list
        crsChoiceBox.setItems(FXCollections.observableArrayList(
    	    "EPSG:4326",
    	    "EPSG:2154",
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

        //texfield formatter to force numbers for the texfield point height
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

	/**
	 * Create a file chooser windows to open a GeoJson file and load features
	 * @param event ActionEvent called on menu click
	 */
    @FXML
    private void openGeoJson(ActionEvent event) {    	
    	//create file chooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open GeoJson");
        fileChooser.getExtensionFilters().addAll(new ExtensionFilter("Json Files", "*.json"), new ExtensionFilter("All Files", "*.*"));
        selectedFile = fileChooser.showOpenDialog(stage);
        //load the feature collection and initialize the features list
        if (selectedFile != null) {
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

    /**
	 * Create a file chooser windows to save a GeoJson file
	 * @param event ActionEvent called on menu click
	 */
    @FXML
    private void saveGeoJson(ActionEvent event) {
    	//create file chooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save GeoJson");
        fileChooser.getExtensionFilters().addAll(new ExtensionFilter("Json Files", "*.json"), new ExtensionFilter("All Files", "*.*"));
        if(selectedFile != null){
        	fileChooser.setInitialDirectory(selectedFile.getParentFile());
        	fileChooser.setInitialFileName(selectedFile.getName());
        }
        saveFile = fileChooser.showSaveDialog(stage);
        //write the feature collection in a file
        if (saveFile != null){
        	try {
				ApplicationUtils.featureCollectionToGeoJsonFile(fc, saveFile.getParentFile(), saveFile.getName());
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
    }    

    /**
	 * Quit the application
	 * @param event ActionEvent called on menu click
	 */
    @FXML
    private void quitApplication(ActionEvent event) {
    	Platform.exit();
    }
   
    /**
	 * Save coordinates changed in the graph
	 */
    @FXML
    private void saveFeatureCoordinates() {
    	int i = 0;
    	for (Data<Number, Number> data : series.getData()) {
        	ApplicationUtils.saveCoordinates(selectedFeature, i, (double)data.getYValue());
        	i++;
        }
    }
    
    /**
     * Update a selected node of the graph with the value typed in the point height texfield
     * @param e action called on enter or on "OK" button click
     */
    @FXML
    public void changeNodeValue(ActionEvent e) {
    	if(lastData != null) lastData.setYValue(Double.parseDouble(pointHeightTextField.getText()));
    }    
    
    /**
     * Create a new FeatureCollection from the parameters list define in the parameterVBox. Then recreate all features to match the new schema.
     */
    @FXML
    private void saveParametersConfiguration(){
    	//Define the new feature type
		SimpleFeatureTypeBuilder simpleFeatureTypeBuilder = new SimpleFeatureTypeBuilder();
		simpleFeatureTypeBuilder.setName("featureType");
		simpleFeatureTypeBuilder.setCRS(selectedCRS);
		simpleFeatureTypeBuilder.add("geometry", featureTypeParameters.getGeometryDescriptor().getType().getBinding());	
		parametersVBox.getChildren().forEach(child ->{
    		ObservableList<Node> params = ((HBox) child).getChildren();
			try{ simpleFeatureTypeBuilder.add(((TextField)params.get(0)).getText(), (Class<?>)((ChoiceBox)params.get(1)).getSelectionModel().getSelectedItem()); }
			catch(Exception e){}
		});
		//create the new feature collection
		SimpleFeatureBuilder simpleFeatureBuilder = new SimpleFeatureBuilder(simpleFeatureTypeBuilder.buildFeatureType());
		DefaultFeatureCollection resultFeatureCollection = new DefaultFeatureCollection(null, simpleFeatureBuilder.getFeatureType());
		//recreate all the features
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
			SimpleFeature sf = simpleFeatureBuilder.buildFeature(myFeature.getID());
			resultFeatureCollection.add(sf);
		}
		//reload the features list
		fc = resultFeatureCollection;
    	addActionClicList();
		loadFeatureCollectionParameters();
	}
    
    /**
     * Save the new parameters values of a selected feature 
     */
    @FXML
    private void saveParametersValues(){	
    	//iterate through all the parameters field
    	parametersVBox.getChildren().forEach(child ->{
    		ObservableList<Node> params = ((HBox) child).getChildren();
    		
    		//load each parameter name
    		String parameterName = ((Label)params.get(0)).getText();
    		//then get the parameter value, varying from the type 
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
        		selectedFeature.getProperty(parameterName).setValue(parameterBool);
    		}
    	});
    	//recreate a new feature. Mandatory to update the feature ID
    	DefaultFeatureCollection dfc = new DefaultFeatureCollection(fc);
    	SimpleFeatureBuilder sfb = new SimpleFeatureBuilder(fc.getSchema());
    	Collection<PropertyDescriptor> descriptors = fc.getSchema().getDescriptors();
    	Iterator<PropertyDescriptor> i = descriptors.iterator();
    	while(i.hasNext()){
    		PropertyDescriptor prop = i.next();
    		if(selectedFeature.getProperty(prop.getName()) != null){
    			sfb.add(selectedFeature.getProperty(prop.getName()).getValue());
    		}
    		else sfb.add(null);
    	}
		SimpleFeature sf = sfb.buildFeature(featureIdTextField.getText());
		dfc.add(sf);    	
		//update the feature collection and features list
		dfc.remove(selectedFeature);
		selectedFeature = sf;
		fc = dfc;
    	addActionClicList();		
    }
    
    /**
     * Add a new parameter to the parameterVBox configuration list
     */
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
    /**
     * Load the parameters list from the feature collection and switch the interface
     */
    public void loadFeatureCollectionParameters(){
    	//update interface
    	parametersVBox.getChildren().clear();
    	parametersConfigurationHBox.setVisible(true);
    	showParametersButton.setVisible(false);
    	featureIdHBox.setVisible(false);
    	saveParametersConfigButton.setVisible(true);
    	saveParametersValuesButton.setVisible(false);
    	//load parameters
    	featureTypeParameters = ApplicationUtils.loadFeatureCollectionParameters(fc);
    	Collection<PropertyDescriptor> descriptors = featureTypeParameters.getDescriptors();
    	ArrayList<Object> ret = new ArrayList<Object>();
    	Iterator<PropertyDescriptor> i = descriptors.iterator();
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
    
    /**
     * Handle click on delete parameter button to the list
     */
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
    
    /**
     * Handle dragging action on a chart node
     */
    public void addActionDragPoint() {
        for (Data<Number, Number> data : series.getData()) {
            Node node = data.getNode() ;
            node.setCursor(Cursor.HAND);
            //update selected node and style on click on a node
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
            //update values in real time on drag
            node.setOnMouseDragged(e -> {
                Point2D pointInScene = new Point2D(e.getSceneX(), e.getSceneY());
                double yAxisLoc = yAxis.sceneToLocal(pointInScene).getY();
                Number y = yAxis.getValueForDisplay(yAxisLoc);
                data.setYValue(y);            
                pointHeightTextField.setText(y.toString());
            });
        }
	}
    
    /**
     * Handle features list selection click and load parameters list values
     */
    public void addActionClicList() {
    	//features list initialization
		featuresList.setItems(null);
		ObservableList<Object> observableList = FXCollections.observableArrayList(fc.toArray());
		featuresList.setVisible(true);
		featuresList.setItems(observableList);
		featuresList.setOnMouseClicked(new EventHandler<MouseEvent>() {
	        @Override
	        public void handle(MouseEvent event) {
	        	//update interface
	        	parametersConfigurationHBox.setVisible(false);
	        	showParametersButton.setVisible(true);
	        	featureIdHBox.setVisible(true);
	        	saveParametersConfigButton.setVisible(false);
	        	saveParametersValuesButton.setVisible(true);
	        	parametersVBox.getChildren().clear();

	        	//update selected feature
	        	selectedFeature = (SimpleFeature) featuresList.getSelectionModel().getSelectedItem();
	        	
	        	//load chart
	        	series.getData().clear();
	    		lastData = null;
	    		pointHeightTextField.setDisable(true);
	    		pointHeightButton.setDisable(true);
	            ArrayList<Data<Number, Number>> datas = ApplicationUtils.loadCoordinates(selectedFeature, selectedCRS);			            	            
	            datas.forEach(data -> {
	            	series.getData().add(data);
	            });
	            addActionDragPoint();
	        	
	            //load parameters values
	            featureIdTextField.setText(selectedFeature.getID());
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
		//change the displayed name of features to match the ID
		featuresList.setCellFactory(lv -> new ListCell<Object>(){
			@Override
			protected void updateItem(Object item, boolean empty){
				super.updateItem(item, empty);
				SimpleFeature sf = (SimpleFeature) item;
				if(sf!=null){
					setText(sf.getID());
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
