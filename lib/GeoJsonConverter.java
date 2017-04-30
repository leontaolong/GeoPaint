import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import java.util.List;

/**
 * Code for converting Polylines into GeoJSON formatted Strings 
 * @author Joel Ross 
 * @author Kyungmin Lee
 * @version Spring 2016
 */
public class GeoJsonConverter {

    /**
     * Returns a GeoJSON String representing the given list of Polylines. Style information
     * (width and color) is included as properties.
     * @param lines A list of Polylines
     * @return A GeoJSON FeatureCollection of LineStrings
     */
    public static String convertToGeoJson(List<Polyline> lines) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"type\": \"FeatureCollection\", " +
                "\"features\": [");
        for (Polyline line : lines) {
            builder.append("{ \"type\": \"Feature\", " + //new feature for each line
                    "\"geometry\": { \"type\": \"LineString\", "+ "\"coordinates\": [ ");
            for(LatLng point : line.getPoints()) { //add points
                builder.append("["+point.longitude+","+point.latitude+"],"); //invert lat/lng for GeoJSON

            }
            builder = builder.deleteCharAt(builder.length() - 1); //remove trailing comma
            builder.append("]},"); //end geometry
            builder.append("\"properties\": { ");
            builder.append("\"color\": " + line.getColor() + ","); //color property
            builder.append("\"width\": " + line.getWidth()); //width property
            builder.append("} },"); //end properties/feature
        }
        builder = builder.deleteCharAt(builder.length() - 1); //remove trailing comma
        builder.append("]}"); //end json
        return builder.toString();
    }
}