package uhh_praktikum_fea.webserver;

import org.jobimtext.api.struct.WebThesaurusDatastructure;

import java.io.BufferedInputStream;

public class DTHelper {
    WebThesaurusDatastructure dt;

    public DTHelper(String config) throws java.io.IOException {
        this.loadDTconfig(config);
    }

    public void loadDTconfig(String configName) {
        dt = new WebThesaurusDatastructure(new BufferedInputStream(getClass().getClassLoader().getResourceAsStream(configName)));
        dt.connect();
    }

    public Long getTermCount(String string) {
        return dt.getTermCount(string);
    }
}
