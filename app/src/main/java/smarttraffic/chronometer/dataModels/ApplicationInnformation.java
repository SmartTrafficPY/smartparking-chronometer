package smarttraffic.chronometer.dataModels;

/**
 * Created by Joaquin on 10/2019.
 * <p>
 * smarttraffic.chronometer.dataModels
 */
public class ApplicationInnformation {

    private int application_id;

    public ApplicationInnformation(int application_id) {
        this.application_id = application_id;
    }

    public int getApplication_id() {
        return application_id;
    }

    public void setApplication_id(int application_id) {
        this.application_id = application_id;
    }
}
