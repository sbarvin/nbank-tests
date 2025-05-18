package api;

import models.CreateUserRequest;
import models.DepositAccountRequest;
import models.DepositAccountResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.skelethon.requesters.ValidatedCrudRequester;
import requests.steps.AccountSteps;
import requests.steps.AdminSteps;
import specs.RequestSpecs;
import specs.ResponseSpecs;

public class DepositAccountTest {

    private static CreateUserRequest userRequest;
    private static long accountId;
    private static double startBalance;

    @BeforeAll
    public static void setupUserAndAccount() {

        //создаем пользователя
        userRequest = AdminSteps.createUser();

        //создаем аккаунт (счет)
        accountId = AccountSteps.createAccounts(userRequest, 1).getFirst();
    }

    @BeforeEach
    public void setupStartBalance() {

        //запоминаем текущий баланс счета
        startBalance = AccountSteps.getAccountBalance(userRequest, accountId);
    }

    @Test
    void userCanDepositWithValidData() {

        //формируем данные для пополнения счета
        var depositRequest = new DepositAccountRequest(accountId, 0.001d);

        //пополняем счет
        new ValidatedCrudRequester<DepositAccountResponse>(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.ACCOUNTS_DEPOSIT,
                ResponseSpecs.requestReturnsOK()
        ).post(depositRequest);

        //проверяем баланс счета после поплнения
        var actualBalance = AccountSteps.getAccountBalance(userRequest, accountId);
        Assertions.assertEquals(startBalance + depositRequest.getBalance(), actualBalance);

    }

    @ParameterizedTest(name = "Сумма пополнения = {0}")
    @ValueSource(doubles = {-1.0, 0.0})
    void userCannotDepositInvalidBalance(double balance) {

        //формируем данные для пополнения счета
        var depositRequest = new DepositAccountRequest(accountId, balance);

        //пополняем счет
        new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.ACCOUNTS_DEPOSIT,
                ResponseSpecs.requestReturnsBadRequest("Invalid account or amount")
        ).post(depositRequest);

        //проверяем, что баланс счета не изменился
        var actualBalance = AccountSteps.getAccountBalance(userRequest, accountId);
        Assertions.assertEquals(startBalance, actualBalance);
    }

    @Test
    void userCannotDepositWithNonExistentAccount() {

        //формируем данные для пополнения счета
        var depositRequest = new DepositAccountRequest(999999, 0.001d);

        //пополняем счет
        new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.ACCOUNTS_DEPOSIT,
                ResponseSpecs.requestReturnsForbidden("Unauthorized access to account")
        ).post(depositRequest);
    }
}
