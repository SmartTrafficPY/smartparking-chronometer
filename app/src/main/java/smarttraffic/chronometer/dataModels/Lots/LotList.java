package smarttraffic.chronometer.dataModels.Lots;

import java.util.List;

/**
 * Created by Joaquin on 09/2019.
 * <p>
 * smarttraffic.smartparking.dataModels
 */

public class LotList {

    private String type;
    private List<Lot> features;

    public LotList() {
        // Persistence Constructor
    }

    @Override
    public String toString() {
        return "LotList{" +
                "type='" + type + '\'' +
                ", features=" + features +
                '}';
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Lot> getFeatures() {
        return features;
    }

    public void setFeatures(List<Lot> features) {
        this.features = features;
    }
}
