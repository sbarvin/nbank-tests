package api;

import generators.RandomData;
import models.CreateUserRequest;
import models.CustomerProfileResponse;
import models.DepositAccountRequest;
import models.UserRole;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import requests.AdminCreateUserRequester;
import requests.CreateAccountRequester;
import requests.CustomerProfileRequester;
import requests.DepositeAccountRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

public class DepositAccountTest {

    private static CreateUserRequest userRequest;
    private static long accountId;
    private static double startBalance;

    @BeforeAll
    public static void setupUserAndAccount() {

        //формируем данные пользователя
        userRequest = new CreateUserRequest(
                RandomData.getUsername(),
                RandomData.getPassword(),
                UserRole.USER.toString()
        );

        //создаем пользователя
        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(userRequest);

        //создаем аккаунт (счет)
        accountId = new CreateAccountRequester(RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post(null)
                .extract().jsonPath().getLong("id");

    }

    @BeforeEach
    public void setupStartBalance() {
        //запоминаем текущий баланс счета
        startBalance = getAccountBalance(accountId);
    }

    @Test
    void userCanDepositWithValidData() {

        //формируем данные для пополнения счета
        var depositRequest = new DepositAccountRequest(accountId, 0.001d);

        //пополняем счет
        new DepositeAccountRequester(RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .post(depositRequest);

        //проверяем баланс счета после поплнения
        var actualBalance = getAccountBalance(accountId);
        Assertions.assertEquals(startBalance + depositRequest.getBalance(), actualBalance);

    }

    @ParameterizedTest(name = "Сумма пополнения = {0}")
    @ValueSource(doubles = {-1.0, 0.0})
    void userCannotDepositInvalidBalance(double balance) {

        //формируем данные для пополнения счета
        var depositRequest = new DepositAccountRequest(accountId, balance);

        //пополняем счет
        new DepositeAccountRequester(RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsBadRequest("Invalid account or amount"))
                .post(depositRequest);

        //проверяем, что баланс счета не изменился
        var actualBalance = getAccountBalance(accountId);
        Assertions.assertEquals(startBalance, actualBalance);
    }

    @Test
    void userCannotDepositWithNonExistentAccount() {
        //формируем данные для пополнения счета
        var depositRequest = new DepositAccountRequest(999999, 0.001d);

        //пополняем счет
        new DepositeAccountRequester(RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsForbidden("Unauthorized access to account"))
                .post(depositRequest);
    }

    //получение баланса по id счета
    private double getAccountBalance(long accountId) {
        CustomerProfileResponse.Customer customer = new CustomerProfileRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .get()
                .extract().as(CustomerProfileResponse.Customer.class);

        return customer.getAccounts().stream()
                .filter(a -> a.getId() == accountId)
                .findFirst()
                .get().getBalance();
    }
}
