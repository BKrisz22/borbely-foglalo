package hu.szakdolgozat.borbely;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BorbelyApplication {

    public static void main(String[] args) {
        SpringApplication.run(BorbelyApplication.class, args);
    }
}