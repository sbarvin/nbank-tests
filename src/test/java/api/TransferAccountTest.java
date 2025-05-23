package api;

import generators.RandomData;
import models.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import requests.*;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static constants.ApiErrorMessages.INVALID_TRANSFER;

public class TransferAccountTest {

    private static CreateUserRequest userRequest;
    private static Map<Long, Double> startBalance = new HashMap<>();
    private static List<Long> accountIds = new ArrayList<>();

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

        // создаем аккаунт(счет) в кол-ве 2 штук для перевода денег между ними
        List.of(1, 2).forEach(
                num -> accountIds.add(
                        new CreateAccountRequester(RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                                ResponseSpecs.entityWasCreated())
                                .post(null)
                                .extract().jsonPath().getLong("id")
                )
        );

        // пополнение всех аккаунтов (счетов) на 100
        accountIds.forEach(
                accountId -> {
                    var depositRequest = new DepositAccountRequest(accountId, 100d);

                    new DepositeAccountRequester(RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                            ResponseSpecs.requestReturnsOK())
                            .post(depositRequest);
                }
        );
    }

    @BeforeEach
    public void setupStartBalance() {
        //получаем информацию о началном балансе счетов
        startBalance = getAccountBalances(accountIds);
    }

    @Test
    void userCanTransferWithValidData() {

        var senderAccountId = startBalance.keySet().stream().toList().getFirst();
        var receiverAccountId = startBalance.keySet().stream().toList().getLast();
        var amount = 1d;

        //формируем данные для перевода денег с одного счета на другой
        var transferRequest = TransferAccountRequest.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .amount(amount)
                .build();

        //переводим деньги
        new TransferAccountRequester(RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .post(transferRequest);


        //проверяем, что на счете отправителя баланс стал меньше на 1, а на счете получателя больше на 1
        var actualBalance = getAccountBalances(accountIds);

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
        var transferRequest = TransferAccountRequest.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .amount(amount)
                .build();

        //переводим деньги
        new TransferAccountRequester(RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsBadRequest(INVALID_TRANSFER.getMessage()))
                .post(transferRequest);

        //проверяем, что на счете отправителя и получателя баланс не изменился
        var actualBalance = getAccountBalances(accountIds);

        Assertions.assertEquals(startBalance, actualBalance);
    }

    @Test
    void userCannotTransferToNonExistentAccount() {

        var senderAccountId = startBalance.keySet().stream().toList().getFirst();
        var receiverAccountId = 9999999;
        var amount = 1d;

        //формируем данные для перевода денег с несуществующего аккаунта на существующий
        var transferRequest = TransferAccountRequest.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .amount(amount)
                .build();

        //переводим деньги
        new TransferAccountRequester(RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsBadRequest(INVALID_TRANSFER.getMessage()))
                .post(transferRequest);

        //проверяем, что на счете получателя баланс не изменился
        var actualBalance = getAccountBalances(accountIds);

        Assertions.assertEquals(startBalance, actualBalance);
    }

    //получение информации о балансе счетов
    private Map<Long, Double> getAccountBalances(List<Long> accountIds) {
        Map<Long, Double> accountBalances = new HashMap<>();
        accountIds.forEach(
                accountId -> accountBalances.put(accountId, getAccountBalance(accountId))
        );
        return accountBalances;
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
