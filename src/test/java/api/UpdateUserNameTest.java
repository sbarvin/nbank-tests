package api;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;

public class UpdateUserNameTest {

    private static String AUTH_TOKEN;

    @BeforeAll
    public static void setup() {
        RestAssured.filters(
                List.of(new RequestLoggingFilter(),
                        new ResponseLoggingFilter()));

        // создание пользователя, если он ранее не добавлялся
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                        {
                          "username": "barvinsk1",
                          "password": "Barvinsk2000#",
                          "role": "USER"
                        }
                        """)
                .post("http://localhost:4111/api/v1/admin/users");

        // получаем токен юзера
        AUTH_TOKEN = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "username": "barvinsk1",
                          "password": "Barvinsk2000#"
                        }
                        """)
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("Authorization");
    }

    @Test
    void userCanUpdateNameWithValidData() {

        // обновляем имя
        String newName = RandomStringUtils.randomAlphabetic(10);
        given()
                .header("Authorization", AUTH_TOKEN)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "name": "%s"
                        }                          
                        """.formatted(newName))
                .when()
                .put("http://localhost:4111/api/v1/customer/profile")
                .then()
                .statusCode(HttpStatus.SC_OK);

        //проверяем, что имя обновилось
        given()
                .header("Authorization", AUTH_TOKEN)
                .contentType(ContentType.JSON)
                .when()
                .get("http://localhost:4111/api/v1/customer/profile")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("name", Matchers.equalTo(newName));
    }
}
