package hu.szakdolgozat.borbely;

import org.springframework.boot.SpringApplication;

public class TestBorbelyApplication {

    public static void main(String[] args) {
        SpringApplication.from(BorbelyApplication::main).with(TestcontainersConfiguration.class).run(args);
    }
}
