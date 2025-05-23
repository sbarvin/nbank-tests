package api;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;

public class DepositAccountTest {

    private static String AUTH_TOKEN;
    private static long accountId;

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

        // создаем аккаунт(счет)
        accountId = given()
                .header("Authorization", AUTH_TOKEN)
                .contentType(ContentType.JSON)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract().jsonPath().getLong("id");

    }

    @Test
    void userCanDepositWithValidData() {
        given()
                .header("Authorization", AUTH_TOKEN)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "id": %s, 
                          "balance": 0.001
                        }
                        """.formatted(accountId))
                .when()
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    void userCannotDepositNegativeBalance() {
        given()
                .header("Authorization", AUTH_TOKEN)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "id": %s, 
                          "balance": -0.001
                        }
                        """.formatted(accountId))
                .when()
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    void userCannotDepositZeroBalance() {
        given()
                .header("Authorization", AUTH_TOKEN)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "id": %s, 
                          "balance": 0
                        }
                        """.formatted(accountId))
                .when()
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    void userCannotDepositWithNonExistentAccount() {
        given()
                .header("Authorization", AUTH_TOKEN)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "id": 999999, 
                          "balance": 1
                        }
                        """)
                .when()
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);
    }
}
