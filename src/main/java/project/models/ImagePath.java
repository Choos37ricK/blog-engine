package project.models;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ImagePath {

    @Value("${response.host}")
    private String host;

    @Value("${server.port}")
    private String port;

    public String getDefaultImagePath() {
        return "http://" + host + ":" + port + "/src/main/resources/uploads/default-1.png";
    }

    public String getImagePath() {
        return "/upload/";
    }
}
