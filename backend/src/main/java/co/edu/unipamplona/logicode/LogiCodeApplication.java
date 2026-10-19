package co.edu.unipamplona.logicode;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LogiCodeApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogiCodeApplication.class, args);
    }
}
