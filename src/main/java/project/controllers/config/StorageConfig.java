package project.controllers.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import project.services.ImageService;


@Configuration
public class StorageConfig implements WebMvcConfigurer, CommandLineRunner {

    @Value("${upload.path}")
    private String location;

    @Autowired
    private ImageService imageService;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(location + "/**/")
                .addResourceLocations("file:" + location + "/");
    }

    @Override
    public void run(String... args) {
        imageService.init();
    }
}
