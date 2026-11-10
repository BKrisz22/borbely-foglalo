package hu.szakdolgozat.borbely;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class BorbelyApplicationTests {

    @Test
    void contextLoads() {
    }
}
