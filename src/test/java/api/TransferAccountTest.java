package api;

import models.CreateUserRequest;
import models.TransferAccountRequest;
import models.TransferAccountResponse;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransferAccountTest {

    private static CreateUserRequest userRequest;
    private static Map<Long, Double> startBalance = new HashMap<>();
    private static List<Long> accountIds = new ArrayList<>();

    @BeforeAll
    public static void setupUserAndAccount() {

        //создаем пользователя
        userRequest = AdminSteps.createUser();

        //создаем аккаунт(счет) в кол-ве 2 штук для перевода денег между ними
        accountIds = AccountSteps.createAccounts(userRequest, 2);

        // пополнение всех аккаунтов (счетов) на 100
        accountIds.forEach(
                accountId -> AccountSteps.depositAccount(userRequest, accountId, 100d)
        );
    }

    @BeforeEach
    public void setupStartBalance() {
        //получаем информацию о началном балансе счетов
        startBalance = AccountSteps.getAccountBalances(userRequest, accountIds);
    }

    @Test
    void userCanTransferWithValidData() {

        var senderAccountId = startBalance.keySet().stream().toList().getFirst();
        var receiverAccountId = startBalance.keySet().stream().toList().getLast();
        var amount = 1d;

        //формируем данные для перевода денег с одного счета на другой
        var transferRequest = new TransferAccountRequest(senderAccountId, receiverAccountId, amount);

        //переводим деньги
        new ValidatedCrudRequester<TransferAccountResponse>(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.ACCOUNTS_TRANSFER,
                ResponseSpecs.requestReturnsOK()
        ).post(transferRequest);

        //проверяем, что на счете отправителя баланс стал меньше на 1, а на счете получателя больше на 1
        var actualBalance = AccountSteps.getAccountBalances(userRequest, accountIds);

        Assertions.assertAll(
                () -> Assertions.assertEquals(
                        startBalance.get(senderAccountId) - amount,
                        actualBalance.get(senderAccountId)
                ),
                () -> Assertions.assertEquals(
                        startBalance.get(receiverAccountId) + amount,
                        actualBalance.get(receiverAccountId)
                )
        );
    }

    @ParameterizedTest(name = "Сумма перевода = {0}")
    @ValueSource(doubles = {-1.0, 0.0, 9999999})
    void userCannotTransferInvalidAmount(double amount) {

        var senderAccountId = startBalance.keySet().stream().toList().getFirst();
        var receiverAccountId = startBalance.keySet().stream().toList().getLast();

        //формируем данные для перевода денег с одного счета на другой
        var transferRequest = new TransferAccountRequest(senderAccountId, receiverAccountId, amount);

        //переводим деньги
        new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.ACCOUNTS_TRANSFER,
                ResponseSpecs.requestReturnsBadRequest("Invalid transfer: insufficient funds or invalid accounts")
        ).post(transferRequest);

        //проверяем, что на счете отправителя и получателя баланс не изменился
        var actualBalance = AccountSteps.getAccountBalances(userRequest, accountIds);
        Assertions.assertEquals(startBalance, actualBalance);
    }

    @Test
    void userCannotTransferToNonExistentAccount() {

        var senderAccountId = startBalance.keySet().stream().toList().getFirst();
        var receiverAccountId = 9999999;
        var amount = 1d;

        //формируем данные для перевода денег с несуществующего аккаунта на существующий
        var transferRequest = new TransferAccountRequest(senderAccountId, receiverAccountId, amount);

        //переводим деньги
        new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.ACCOUNTS_TRANSFER,
                ResponseSpecs.requestReturnsBadRequest("Invalid transfer: insufficient funds or invalid accounts")
        ).post(transferRequest);

        //проверяем, что на счете получателя баланс не изменился
        var actualBalance = AccountSteps.getAccountBalances(userRequest, accountIds);
        Assertions.assertEquals(startBalance, actualBalance);
    }
}
