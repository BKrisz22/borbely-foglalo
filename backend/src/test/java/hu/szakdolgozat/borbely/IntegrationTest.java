package hu.szakdolgozat.borbely;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Az integrációs tesztek közös annotációja: teljes Spring-környezet, {@code MockMvc}
 * és Testcontainersben futó PostgreSQL.
 * A tesztek fix JWT-kulccsal és tulajdonosi fiók nélkül futnak, így nem függnek a {@code .env} fájltól.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(properties = {
        "app.jwt.secret=dGVzenQtand0LWt1bGNzLWxlZ2FsYWJiLTMyLWJhanQtaG9zc3p1",
        "app.owner.email=",
        "app.owner.password="
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public @interface IntegrationTest {
}
