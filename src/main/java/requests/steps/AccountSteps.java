package requests.steps;

import models.*;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.ValidatedCrudRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountSteps {

    public static List<Long> createAccounts(CreateUserRequest userRequest, int count) {
        List<Long> accountIds = new ArrayList<>();
        for (var i = 0; i < count; i++) {

            var accountId = new ValidatedCrudRequester<CreateAccountResponse>(
                    RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                    Endpoint.ACCOUNTS,
                    ResponseSpecs.entityWasCreated()
            ).post(null).getId();
            accountIds.add(accountId);
        }
        return accountIds;
    }

    public static void depositAccount(CreateUserRequest userRequest, long accountId, double balance) {
        //формируем данные для пополнения счета
        var depositRequest = new DepositAccountRequest(accountId, balance);

        //пополняем счет
        new ValidatedCrudRequester<DepositAccountResponse>(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.ACCOUNTS_DEPOSIT,
                ResponseSpecs.requestReturnsOK()
        ).post(depositRequest);
    }

    public static double getAccountBalance(CreateUserRequest userRequest, long accountId) {

        var customerProfile = new ValidatedCrudRequester<CustomerProfileResponse.Customer>(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.CUSTOMER_PROFILE,
                ResponseSpecs.requestReturnsOK()
        ).getAll();

        return customerProfile.getAccounts().stream()
                .filter(a -> a.getId() == accountId)
                .findFirst()
                .get().getBalance();
    }

    //получение информации о балансе счетов
    public static Map<Long, Double> getAccountBalances(CreateUserRequest userRequest, List<Long> accountIds) {
        Map<Long, Double> accountBalances = new HashMap<>();
        accountIds.forEach(
                accountId -> accountBalances.put(accountId, getAccountBalance(userRequest, accountId))
        );
        return accountBalances;
    }
}
