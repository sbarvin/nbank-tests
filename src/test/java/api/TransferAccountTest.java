package api;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;

public class TransferAccountTest {

    private static String AUTH_TOKEN;
    private static List<Long> accountIds = new ArrayList<>();

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

        // создаем аккаунт(счет) в кол-ве 2 штук для перевода денег между ними
        List.of(1, 2).forEach(
                num -> accountIds.add(
                        given()
                                .header("Authorization", AUTH_TOKEN)
                                .contentType(ContentType.JSON)
                                .post("http://localhost:4111/api/v1/accounts")
                                .then()
                                .assertThat()
                                .statusCode(HttpStatus.SC_CREATED)
                                .extract().jsonPath().getLong("id")
                )
        );

        // пополнение всех аккаунтов (счетов) на 100
        accountIds.forEach(
                accountId -> given()
                        .header("Authorization", AUTH_TOKEN)
                        .contentType(ContentType.JSON)
                        .body("""
                                {
                                  "id": %s, 
                                  "balance": 100
                                }
                                """.formatted(accountId))
                        .when()
                        .post("http://localhost:4111/api/v1/accounts/deposit")
                        .then()
                        .statusCode(HttpStatus.SC_OK)
        );
    }

    @Test
    void userCanTransferWithValidData() {

        //переводим деньги со счета 1 на счет 2
        given()
                .header("Authorization", AUTH_TOKEN)
                .contentType(ContentType.JSON)
                .body("""
                        {
                        "senderAccountId": %s, 
                        "receiverAccountId": %s, 
                        "amount": 1}
                        """.formatted(accountIds.get(0), accountIds.get(1)))
                .when()
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    void userCannotTransferWithNegativeAmount() {
        given()
                .header("Authorization", AUTH_TOKEN)
                .contentType(ContentType.JSON)
                .body("""
                        {
                        "senderAccountId": %s, 
                        "receiverAccountId": %s, 
                        "amount": -1}
                        """.formatted(accountIds.get(0), accountIds.get(1)))
                .when()
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    void userCannotTransferZeroAmount() {
        given()
                .header("Authorization", AUTH_TOKEN)
                .contentType(ContentType.JSON)
                .body("""
                        {
                        "senderAccountId": %s, 
                        "receiverAccountId": %s, 
                        "amount": 0}
                        """.formatted(accountIds.get(0), accountIds.get(1)))
                .when()
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    void userCannotTransferAmountGreaterThanAccountBalance() {
        given()
                .header("Authorization", AUTH_TOKEN)
                .contentType(ContentType.JSON)
                .body("""
                        {
                        "senderAccountId": %s, 
                        "receiverAccountId": %s, 
                        "amount": 9999999}
                        """.formatted(accountIds.get(0), accountIds.get(1)))
                .when()
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    void userCannotTransferToNonExistentAccount() {
        given()
                .header("Authorization", AUTH_TOKEN)
                .contentType(ContentType.JSON)
                .body("""
                        {
                        "senderAccountId": %s, 
                        "receiverAccountId": 9999999, 
                        "amount": 100}
                        """.formatted(accountIds.get(0), accountIds.get(1)))
                .when()
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

}
