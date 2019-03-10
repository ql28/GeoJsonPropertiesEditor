package application;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.geoserver.config.util.XStreamPersister.CRSConverter;
import org.geotools.feature.FeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.util.CRSConverterFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import javafx.scene.chart.XYChart.Data;
import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicData;

public class ApplicationUtils {

	private static FeatureJSON featureJSON;
	
	/**
	 * create a feature collection from a file
	 * @param featureCollectionFile the file containing a feature collection
	 * @return a feature collection
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public static FeatureCollection<SimpleFeatureType, SimpleFeature> geoJsonToFeatureCollection(File featureCollectionFile) throws FileNotFoundException, IOException {
		featureJSON = new FeatureJSON();
		return featureJSON.readFeatureCollection(new FileInputStream(featureCollectionFile));
	}
	
	/**
	 * Load the schema defining the FeatureCollection structure
	 * @param fc the feature collection
	 * @return
	 */
	public static SimpleFeatureType loadFeatureCollectionParameters(FeatureCollection<SimpleFeatureType, SimpleFeature> fc){
		return fc.getSchema();
	}
		
	/**
	 * Load coordinates of a feature and initialize Z value
	 * @param sf the feature we want to load
	 * @param myCRS the CRS of the feature collection
	 * @return ArrayList containing all the coordinates formatted for the chart (distance, height)
	 */
	public static ArrayList<Data<Number, Number>> loadCoordinates(SimpleFeature sf, CoordinateReferenceSystem myCRS){
		ArrayList<Data<Number, Number>> ret = new ArrayList<Data<Number, Number>>();
		Coordinate[] c = ((Geometry) sf.getDefaultGeometryProperty().getValue()).getCoordinates();
        double dist = 0;
    	ret.add(new Data<Number, Number>(dist, (Double.isNaN(c[0].z) == true ? 0 : c[0].z)));
        for(int i = 1; i < c.length; i++){
        	dist += getDistanceFromCoordinates(c[i-1], c[i], myCRS);
        	ret.add(new Data<Number, Number>(dist, (Double.isNaN(c[i].z) == true ? 0 : c[i].z)));
        }
		return ret;
	}
	
	/**
	 * Save the coordinates from the chart to the feature's Geometry
	 * @param sf the feature we want to update
	 * @param idCoord the id of the point of the Geometry
	 * @param val the Z value
	 */
	public static void saveCoordinates(SimpleFeature sf, int idCoord, double val){
		Coordinate[] c = ((Geometry) sf.getDefaultGeometryProperty().getValue()).getCoordinates();
		c[idCoord].z = val;
	}
	
	/**
	 * Get the distance between two coordinates
	 * @param coordinate1 first coordinate
	 * @param coordinate2 second coordinate
	 * @return The distance traveled between the 2 points in meters
	 */
	public static double getDistanceFromCoordinates(Coordinate coordinate1, Coordinate coordinate2, CoordinateReferenceSystem myCrs){
		GeodeticCalculator gc = new GeodeticCalculator(myCrs);
		try {
			gc.setStartingPosition(JTS.toDirectPosition(coordinate1, myCrs));
			gc.setDestinationPosition(JTS.toDirectPosition(coordinate2, myCrs));
		} catch (TransformException e) {
			e.printStackTrace();
		}
		double dist = gc.getOrthodromicDistance();
		return dist;
	}
	
	/**
	 * Create a GeoJson from a FeatureCollection
	 * @param featureCollection the FeatureCollection we want to write in a file
	 * @param dir the full path to the write directory
	 * @param fileName the name of the file
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void featureCollectionToGeoJsonFile(FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection, File dir, String fileName) throws FileNotFoundException, IOException {
		featureJSON = new FeatureJSON(new GeometryJSON(15));
		featureJSON.setEncodeFeatureCollectionCRS(true);
		featureJSON.writeFeatureCollection(featureCollection, new FileOutputStream(new File(dir, fileName)));
	}
	
	/**
	 * Find the CoordinateReferenceSystem in a file
	 * @param f the file we search in
	 * @return CoordinateReferenceSystem
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static CoordinateReferenceSystem geoJsonToCoordinateReferenceSystem(File f) throws FileNotFoundException, IOException {
		featureJSON = new FeatureJSON();
		return featureJSON.readCRS(f);
	}
}
