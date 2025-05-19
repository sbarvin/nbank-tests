package ui;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import models.CreateUserRequest;
import models.DepositAccountResponse;
import models.LoginUserRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.steps.AccountSteps;
import requests.steps.AdminSteps;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.*;
import static org.assertj.core.api.Assertions.assertThat;

public class DepositAccountTest extends BaseTest {

    private static CreateUserRequest userRequest;
    private static String authToken;
    private static long accountId;
    private static DepositAccountResponse startAccountInfo;

    @BeforeAll
    public static void setupUser() {

        //создаем пользователя
        userRequest = AdminSteps.createUser();

        //получаем authToken
        authToken = new CrudRequester(
                RequestSpecs.unauthSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestReturnsOK())
                .post(LoginUserRequest.builder().username(userRequest.getUsername()).password(userRequest.getPassword()).build())
                .extract()
                .header("Authorization");

        //создаем аккаунт (счет)
        accountId = AccountSteps.createAccounts(userRequest, 1).getFirst();
    }

    @BeforeEach
    public void setupStartAccountInfo() {

        //запоминаем текущую информацию по счету
        startAccountInfo = AccountSteps.getAccountInfo(userRequest, accountId);
    }

    @Test
    public void userCanDepositIfAccountExists() {

        //авторизуемся и переходим на /dashboard
        Selenide.open("/");

        Selenide.executeJavaScript("localStorage.setItem('authToken', arguments[0]);", authToken);

        Selenide.open("/dashboard");

        //нажимаем на кнопку 💰 Deposit Money
        $$("button").findBy(Condition.exactText("💰 Deposit Money")).click();

        //проверяем, что на странице /deposit есть тег h1 с текстом 💰 Deposit Money
        $$("h1").findBy(Condition.exactText("💰 Deposit Money")).shouldBe(visible);

        //выбираем счет, который хотим пополнить
        $(".account-selector")
                .selectOption(
                        String.format("%s (Balance: $%s)",
                                startAccountInfo.getAccountNumber(),
                                String.format("%.2f", startAccountInfo.getBalance())
                        ).replace(",", ".")
                );

        //вводим сумму пополнения
        var depositAmount = "50.10";
        $(".deposit-input").setValue(depositAmount);

        //нажимаем на кнопку Deposit
        $$("button").findBy(Condition.exactText("💵 Deposit")).click();

        //Проверить текст аллерта
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains(String.format("Successfully deposited $%s to account %s!", depositAmount, startAccountInfo.getAccountNumber()));

        //Проверям, что произошло перенаправление на страницу /dashboard
        $$("h1").findBy(Condition.exactText("User Dashboard")).shouldBe(visible);

        //проверяем баланс счета после поплнения
        var actualBalance = AccountSteps.getAccountBalance(userRequest, accountId);
        Assertions.assertEquals(startAccountInfo.getBalance() + Double.parseDouble(depositAmount), actualBalance);

    }

    @Test
    public void userCannotDepositIfAccountNotChosen() {
        //авторизуемся и переходим на /dashboard
        Selenide.open("/");

        Selenide.executeJavaScript("localStorage.setItem('authToken', arguments[0]);", authToken);

        Selenide.open("/dashboard");

        //нажимаем на кнопку 💰 Deposit Money
        $$("button").findBy(Condition.exactText("💰 Deposit Money")).click();

        //проверяем, что на странице /deposit есть тег h1 с текстом 💰 Deposit Money
        $$("h1").findBy(Condition.exactText("💰 Deposit Money")).shouldBe(visible);

        //выбираем счет, который хотим пополнить
        $(".account-selector").selectOption("-- Choose an account --");

        //вводим сумму пополнения
        var depositAmount = "50.10";
        $(".deposit-input").setValue(depositAmount);

        //нажимаем на кнопку Deposit
        $$("button").findBy(Condition.exactText("💵 Deposit")).click();

        //Проверить текст аллерта
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("❌ Please select an account.");

        //Проверям, что мы остались на той же странцие
        $$("h1").findBy(Condition.exactText("💰 Deposit Money")).shouldBe(visible);

        //проверяем баланс счета после поплнения не изменился
        var actualBalance = AccountSteps.getAccountInfo(userRequest, accountId);
        Assertions.assertEquals(startAccountInfo, actualBalance);
    }

    @Test
    public void userCannotDepositInvalidBalance() {
        //авторизуемся и переходим на /dashboard
        Selenide.open("/");

        Selenide.executeJavaScript("localStorage.setItem('authToken', arguments[0]);", authToken);

        Selenide.open("/dashboard");

        //нажимаем на кнопку 💰 Deposit Money
        $$("button").findBy(Condition.exactText("💰 Deposit Money")).click();

        //проверяем, что на странице /deposit есть тег h1 с текстом 💰 Deposit Money
        $$("h1").findBy(Condition.exactText("💰 Deposit Money")).shouldBe(visible);

        //выбираем счет, который хотим пополнить
        $(".account-selector")
                .selectOption(
                        String.format("%s (Balance: $%s)",
                                startAccountInfo.getAccountNumber(),
                                String.format("%.2f", startAccountInfo.getBalance())
                        ).replace(",", ".")
                );

        //вводим сумму пополнения
        var depositAmount = "-50.10";
        $(".deposit-input").setValue(depositAmount);

        //нажимаем на кнопку Deposit
        $$("button").findBy(Condition.exactText("💵 Deposit")).click();

        //Проверить текст аллерта
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("❌ Please enter a valid amount.");

        //Проверям, что мы остались на той же странцие
        $$("h1").findBy(Condition.exactText("💰 Deposit Money")).shouldBe(visible);

        //проверяем баланс счета после поплнения не изменился
        var actualBalance = AccountSteps.getAccountInfo(userRequest, accountId);
        Assertions.assertEquals(startAccountInfo, actualBalance);
    }
}
