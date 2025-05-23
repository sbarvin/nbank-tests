package ui;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import generators.RandomData;
import models.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.skelethon.requesters.ValidatedCrudRequester;
import requests.steps.AccountSteps;
import requests.steps.AdminSteps;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.List;

import static com.codeborne.selenide.Condition.disappear;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.*;
import static org.assertj.core.api.Assertions.assertThat;

public class TransferAccountTest extends BaseTest {

    private static CreateUserRequest userSenderRequest;
    private static CreateUserRequest userReceiverRequest;
    private static String senderAuthToken;
    private static long senderAccountId;
    private static long receiverAccountId;
    private static DepositAccountResponse startSenderAccountInfo;
    private static DepositAccountResponse startReceiverAccountInfo;

    @BeforeAll
    public static void setupUser() {

        //создаем пользователя отправителя и получателя
        userSenderRequest = AdminSteps.createUser();
        userReceiverRequest = AdminSteps.createUser();

        //получаем authToken
        senderAuthToken = new CrudRequester(
                RequestSpecs.unauthSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestReturnsOK())
                .post(LoginUserRequest.builder().username(userSenderRequest.getUsername()).password(userSenderRequest.getPassword()).build())
                .extract()
                .header("Authorization");

        //создаем аккаунт (счет)
        senderAccountId = AccountSteps.createAccounts(userSenderRequest, 1).getFirst();
        receiverAccountId = AccountSteps.createAccounts(userReceiverRequest, 1).getFirst();

        //пополняем счет отправителя на 100
        AccountSteps.depositAccount(userSenderRequest, senderAccountId, 100d);

        //обновляем имена пользователей
        List.of(userSenderRequest, userReceiverRequest).forEach(
                userRequest -> {
                    new ValidatedCrudRequester<CustomerProfileResponse>(
                            RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                            Endpoint.CUSTOMER_PROFILE_UPDATE,
                            ResponseSpecs.requestReturnsOK()
                    ).put(new CustomerProfileRequest(userRequest.getUsername()));
                }
        );
    }

    @BeforeEach
    public void setupStartAccountInfo() {

        //запоминаем текущую информацию по счету
        startSenderAccountInfo = AccountSteps.getAccountInfo(userSenderRequest, senderAccountId);
        startReceiverAccountInfo = AccountSteps.getAccountInfo(userReceiverRequest, receiverAccountId);
    }

    @Test
    public void userCanTransferWithCorrectData() {
        //авторизуемся и переходим на /dashboard
        Selenide.open("/");

        Selenide.executeJavaScript("localStorage.setItem('authToken', arguments[0]);", senderAuthToken);

        Selenide.open("/dashboard");

        //нажимаем на кнопку 🔄 Make a Transfer
        $$("button").findBy(Condition.exactText("🔄 Make a Transfer")).click();

        //проверяем, что на странице /deposit есть тег h1 с текстом 💰 Deposit Money
        $$("h1").findBy(Condition.exactText("🔄 Make a Transfer")).shouldBe(visible);

        //выбираем счет, с которого хотим перевести
        $(".account-selector")
                .selectOption(
                        String.format("%s (Balance: $%s)",
                                startSenderAccountInfo.getAccountNumber(),
                                String.format("%.2f", startSenderAccountInfo.getBalance())
                        ).replace(",", ".")
                );

        //вводим имя получателя
        $("input[placeholder='Enter recipient name']").setValue(userReceiverRequest.getUsername());

        //вводим номер счета получателя
        $("input[placeholder='Enter recipient account number']").setValue(startReceiverAccountInfo.getAccountNumber());

        //вводим сумму перевода
        var transferAmount = "1.00";
        $("input[placeholder='Enter amount']").setValue(transferAmount);

        //подтверждаем Confirm details are correct
        $("#confirmCheck").click();

        //нажимаем на кнопку 🚀 Send Transfer
        $$("button").findBy(Condition.exactText("🚀 Send Transfer")).click();

        //Проверить текст аллерта
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains(String.format("✅ Successfully transferred $%s to account %s!", transferAmount, startReceiverAccountInfo.getAccountNumber()));

        //проверяем, что на счете отправителя баланс стал меньше, а на счете получателя больше на сумму перевода
        var actualSenderAccountInfo = AccountSteps.getAccountInfo(userSenderRequest, senderAccountId);
        var actualReceiverAccountInfo = AccountSteps.getAccountInfo(userReceiverRequest, receiverAccountId);

        Assertions.assertAll(
                () -> Assertions.assertEquals(
                        startSenderAccountInfo.getBalance() - Double.valueOf(transferAmount),
                        actualSenderAccountInfo.getBalance()
                ),
                () -> Assertions.assertEquals(
                        startReceiverAccountInfo.getBalance() + Double.valueOf(transferAmount),
                        actualReceiverAccountInfo.getBalance()
                )
        );
    }

    @Test
    public void userCannotTransferInvalidAmount() {
        //авторизуемся и переходим на /dashboard
        Selenide.open("/");

        Selenide.executeJavaScript("localStorage.setItem('authToken', arguments[0]);", senderAuthToken);

        Selenide.open("/dashboard");

        //нажимаем на кнопку 🔄 Make a Transfer
        $$("button").findBy(Condition.exactText("🔄 Make a Transfer")).click();

        //проверяем, что на странице /deposit есть тег h1 с текстом 💰 Deposit Money
        $$("h1").findBy(Condition.exactText("🔄 Make a Transfer")).shouldBe(visible);

        //выбираем счет, с которого хотим перевести
        $(".account-selector")
                .selectOption(
                        String.format("%s (Balance: $%s)",
                                startSenderAccountInfo.getAccountNumber(),
                                String.format("%.2f", startSenderAccountInfo.getBalance())
                        ).replace(",", ".")
                );

        //вводим имя получателя
        $("input[placeholder='Enter recipient name']").setValue(userReceiverRequest.getUsername());

        //вводим номер счета получателя
        $("input[placeholder='Enter recipient account number']").setValue(startReceiverAccountInfo.getAccountNumber());

        //вводим сумму перевода
        var transferAmount = "-1";
        $("input[placeholder='Enter amount']").setValue(transferAmount);

        //подтверждаем Confirm details are correct
        $("#confirmCheck").click();

        //нажимаем на кнопку 🚀 Send Transfer
        $$("button").findBy(Condition.exactText("🚀 Send Transfer")).click();

        //Проверить текст аллерта
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("❌ Error: Invalid transfer: insufficient funds or invalid accounts");

        //проверяем, что на счете отправителя и получателя баланс не изменился
        var actualSenderAccountInfo = AccountSteps.getAccountInfo(userSenderRequest, senderAccountId);
        var actualReceiverAccountInfo = AccountSteps.getAccountInfo(userReceiverRequest, receiverAccountId);

        Assertions.assertAll(
                () -> Assertions.assertEquals(startSenderAccountInfo, actualSenderAccountInfo),
                () -> Assertions.assertEquals(startReceiverAccountInfo, actualReceiverAccountInfo)
        );
    }

    @Test
    public void userCannotTransferWithUncheckedConfirmationBox() {
        //авторизуемся и переходим на /dashboard
        Selenide.open("/");

        Selenide.executeJavaScript("localStorage.setItem('authToken', arguments[0]);", senderAuthToken);

        Selenide.open("/dashboard");

        //нажимаем на кнопку 🔄 Make a Transfer
        $$("button").findBy(Condition.exactText("🔄 Make a Transfer")).click();

        //проверяем, что на странице /deposit есть тег h1 с текстом 💰 Deposit Money
        $$("h1").findBy(Condition.exactText("🔄 Make a Transfer")).shouldBe(visible);

        //выбираем счет, с которого хотим перевести
        $(".account-selector")
                .selectOption(
                        String.format("%s (Balance: $%s)",
                                startSenderAccountInfo.getAccountNumber(),
                                String.format("%.2f", startSenderAccountInfo.getBalance())
                        ).replace(",", ".")
                );

        //вводим имя получателя
        $("input[placeholder='Enter recipient name']").setValue(userReceiverRequest.getUsername());

        //вводим номер счета получателя
        $("input[placeholder='Enter recipient account number']").setValue(startReceiverAccountInfo.getAccountNumber());

        //вводим сумму перевода
        var transferAmount = "1";
        $("input[placeholder='Enter amount']").setValue(transferAmount);

        //нажимаем на кнопку 🚀 Send Transfer
        $$("button").findBy(Condition.exactText("🚀 Send Transfer")).click();

        //Проверить текст аллерта
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("❌ Please fill all fields and confirm.");
        //проверяем, что на счете отправителя и получателя баланс не изменился
        var actualSenderAccountInfo = AccountSteps.getAccountInfo(userSenderRequest, senderAccountId);
        var actualReceiverAccountInfo = AccountSteps.getAccountInfo(userReceiverRequest, receiverAccountId);

        Assertions.assertAll(
                () -> Assertions.assertEquals(startSenderAccountInfo, actualSenderAccountInfo),
                () -> Assertions.assertEquals(startReceiverAccountInfo, actualReceiverAccountInfo)
        );
    }

    @Test
    public void userCanRepeatExistingTransferFromHistory() {
        //авторизуемся и переходим на /dashboard
        Selenide.open("/");

        Selenide.executeJavaScript("localStorage.setItem('authToken', arguments[0]);", senderAuthToken);

        Selenide.open("/dashboard");

        //нажимаем на кнопку 🔄 Make a Transfer
        $$("button").findBy(Condition.exactText("🔄 Make a Transfer")).click();

        //проверяем, что на странице /deposit есть тег h1 с текстом 💰 Deposit Money
        $$("h1").findBy(Condition.exactText("🔄 Make a Transfer")).shouldBe(visible);

        //выбираем счет, с которого хотим перевести
        $(".account-selector")
                .selectOption(
                        String.format("%s (Balance: $%s)",
                                startSenderAccountInfo.getAccountNumber(),
                                String.format("%.2f", startSenderAccountInfo.getBalance())
                        ).replace(",", ".")
                );

        //вводим имя получателя
        $("input[placeholder='Enter recipient name']").setValue(userReceiverRequest.getUsername());

        //вводим номер счета получателя
        $("input[placeholder='Enter recipient account number']").setValue(startReceiverAccountInfo.getAccountNumber());

        //вводим сумму перевода
        var transferAmount = "3.00";
        $("input[placeholder='Enter amount']").setValue(transferAmount);

        //подтверждаем Confirm details are correct
        $("#confirmCheck").click();

        //нажимаем на кнопку 🚀 Send Transfer
        $$("button").findBy(Condition.exactText("🚀 Send Transfer")).click();

        //Проверить текст аллерта
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains(String.format("✅ Successfully transferred $%s to account %s!", transferAmount, startReceiverAccountInfo.getAccountNumber()));

        //проверяем, что на счете отправителя баланс стал меньше, а на счете получателя больше на сумму перевода
        var afterTransferSenderAccountInfo = AccountSteps.getAccountInfo(userSenderRequest, senderAccountId);
        var afterTransferReceiverAccountInfo = AccountSteps.getAccountInfo(userReceiverRequest, receiverAccountId);

        Assertions.assertAll(
                () -> Assertions.assertEquals(
                        startSenderAccountInfo.getBalance() - Double.valueOf(transferAmount),
                        afterTransferSenderAccountInfo.getBalance()
                ),
                () -> Assertions.assertEquals(
                        startReceiverAccountInfo.getBalance() + Double.valueOf(transferAmount),
                        afterTransferReceiverAccountInfo.getBalance()
                )
        );

        //переходим на форму 🔁 Transfer Again
        $$("button").findBy(Condition.exactText("🔁 Transfer Again")).click();

        //находим последнюю транзакцию
        $("input[placeholder='Enter name to find transactions']").setValue(userReceiverRequest.getUsername());

        //нажимаем на кнопку 🔍 Search Transactions
        $$("button").findBy(Condition.exactText("🔍 Search Transactions")).click();

        //нажимаем на кнопку 🔁 Repeat
        $$("button").findBy(Condition.exactText("🔁 Repeat")).click();

        //проверяем, что открылось модальное окно
        $$(".modal-title").findBy(Condition.exactText("🔁 Repeat transfer")).shouldBe(visible);

        //выбираем счет, с которого хотим перевести
        $("select.form-control")
                .selectOption(
                        String.format("%s (Balance: $%s)",
                                startSenderAccountInfo.getAccountNumber(),
                                String.format("%.2f", startSenderAccountInfo.getBalance())
                        ).replace(",", ".")
                );

        //не вводим сумму перевода, по умолчанию в поле стоит 1
        var defatultTransferRepeatAmount = "1";

        //подтверждаем Confirm details are correct
        $("#confirmCheck").click();

        //нажимаем на кнопку 🚀 Send Transfer
        $$("button").findBy(Condition.exactText("🚀 Send Transfer")).click();

        //Проверить текст аллерта
        alert = switchTo().alert();
        assertThat(alert.getText()).contains(
                String.format("✅ Transfer of $%s successful from Account %s to %s!",
                        defatultTransferRepeatAmount, senderAccountId, receiverAccountId
                )
        );

        //проверяем, что на счете отправителя баланс стал меньше, а на счете получателя больше на сумму перевода
        var afterRepeatTransferSenderAccountInfo = AccountSteps.getAccountInfo(userSenderRequest, senderAccountId);
        var afterRepeatTransferReceiverAccountInfo = AccountSteps.getAccountInfo(userReceiverRequest, receiverAccountId);

        Assertions.assertAll(
                () -> Assertions.assertEquals(
                        afterTransferSenderAccountInfo.getBalance() - Double.valueOf(defatultTransferRepeatAmount),
                        afterRepeatTransferSenderAccountInfo.getBalance()
                ),
                () -> Assertions.assertEquals(
                        afterTransferReceiverAccountInfo.getBalance() + Double.valueOf(defatultTransferRepeatAmount),
                        afterRepeatTransferReceiverAccountInfo.getBalance()
                )
        );
    }

    @Test
    public void userCannotSeeMatchingTransactionsWhenNoMatchFound() {
        //авторизуемся и переходим на /dashboard
        Selenide.open("/");

        Selenide.executeJavaScript("localStorage.setItem('authToken', arguments[0]);", senderAuthToken);

        Selenide.open("/dashboard");

        //нажимаем на кнопку 🔄 Make a Transfer
        $$("button").findBy(Condition.exactText("🔄 Make a Transfer")).click();

        //проверяем, что на странице /deposit есть тег h1 с текстом 💰 Deposit Money
        $$("h1").findBy(Condition.exactText("🔄 Make a Transfer")).shouldBe(visible);

        //выбираем счет, с которого хотим перевести
        $(".account-selector")
                .selectOption(
                        String.format("%s (Balance: $%s)",
                                startSenderAccountInfo.getAccountNumber(),
                                String.format("%.2f", startSenderAccountInfo.getBalance())
                        ).replace(",", ".")
                );

        //вводим имя получателя
        $("input[placeholder='Enter recipient name']").setValue(userReceiverRequest.getUsername());

        //вводим номер счета получателя
        $("input[placeholder='Enter recipient account number']").setValue(startReceiverAccountInfo.getAccountNumber());

        //вводим сумму перевода
        var transferAmount = "3.00";
        $("input[placeholder='Enter amount']").setValue(transferAmount);

        //подтверждаем Confirm details are correct
        $("#confirmCheck").click();

        //нажимаем на кнопку 🚀 Send Transfer
        $$("button").findBy(Condition.exactText("🚀 Send Transfer")).click();

        //Проверить текст аллерта
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains(String.format("✅ Successfully transferred $%s to account %s!", transferAmount, startReceiverAccountInfo.getAccountNumber()));

        //проверяем, что на счете отправителя баланс стал меньше, а на счете получателя больше на сумму перевода
        var afterTransferSenderAccountInfo = AccountSteps.getAccountInfo(userSenderRequest, senderAccountId);
        var afterTransferReceiverAccountInfo = AccountSteps.getAccountInfo(userReceiverRequest, receiverAccountId);

        Assertions.assertAll(
                () -> Assertions.assertEquals(
                        startSenderAccountInfo.getBalance() - Double.valueOf(transferAmount),
                        afterTransferSenderAccountInfo.getBalance()
                ),
                () -> Assertions.assertEquals(
                        startReceiverAccountInfo.getBalance() + Double.valueOf(transferAmount),
                        afterTransferReceiverAccountInfo.getBalance()
                )
        );

        //переходим на форму 🔁 Transfer Again
        $$("button").findBy(Condition.exactText("🔁 Transfer Again")).click();

        //находим последнюю транзакцию
        $("input[placeholder='Enter name to find transactions']").setValue(RandomData.getUsername());

        //нажимаем на кнопку 🔍 Search Transactions
        $$("button").findBy(Condition.exactText("🔍 Search Transactions")).click();

        //Проверить текст аллерта
        alert = switchTo().alert();
        assertThat(alert.getText()).contains("❌ No matching users found.");

        //проверяем, что на счете отправителя и получателя баланс не изменился
        var actualSenderAccountInfo = AccountSteps.getAccountInfo(userSenderRequest, senderAccountId);
        var actualReceiverAccountInfo = AccountSteps.getAccountInfo(userReceiverRequest, receiverAccountId);

        Assertions.assertAll(
                () -> Assertions.assertEquals(afterTransferSenderAccountInfo, actualSenderAccountInfo),
                () -> Assertions.assertEquals(afterTransferReceiverAccountInfo, actualReceiverAccountInfo)
        );
    }

    @Test
    public void userCannotRepeatTransferWithInvalidAmount() {
        //авторизуемся и переходим на /dashboard
        Selenide.open("/");

        Selenide.executeJavaScript("localStorage.setItem('authToken', arguments[0]);", senderAuthToken);

        Selenide.open("/dashboard");

        //нажимаем на кнопку 🔄 Make a Transfer
        $$("button").findBy(Condition.exactText("🔄 Make a Transfer")).click();

        //проверяем, что на странице /deposit есть тег h1 с текстом 💰 Deposit Money
        $$("h1").findBy(Condition.exactText("🔄 Make a Transfer")).shouldBe(visible);

        //выбираем счет, с которого хотим перевести
        $(".account-selector")
                .selectOption(
                        String.format("%s (Balance: $%s)",
                                startSenderAccountInfo.getAccountNumber(),
                                String.format("%.2f", startSenderAccountInfo.getBalance())
                        ).replace(",", ".")
                );

        //вводим имя получателя
        $("input[placeholder='Enter recipient name']").setValue(userReceiverRequest.getUsername());

        //вводим номер счета получателя
        $("input[placeholder='Enter recipient account number']").setValue(startReceiverAccountInfo.getAccountNumber());

        //вводим сумму перевода
        var transferAmount = "3.00";
        $("input[placeholder='Enter amount']").setValue(transferAmount);

        //подтверждаем Confirm details are correct
        $("#confirmCheck").click();

        //нажимаем на кнопку 🚀 Send Transfer
        $$("button").findBy(Condition.exactText("🚀 Send Transfer")).click();

        //Проверить текст аллерта
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains(String.format("✅ Successfully transferred $%s to account %s!", transferAmount, startReceiverAccountInfo.getAccountNumber()));

        //проверяем, что на счете отправителя баланс стал меньше, а на счете получателя больше на сумму перевода
        var afterTransferSenderAccountInfo = AccountSteps.getAccountInfo(userSenderRequest, senderAccountId);
        var afterTransferReceiverAccountInfo = AccountSteps.getAccountInfo(userReceiverRequest, receiverAccountId);

        Assertions.assertAll(
                () -> Assertions.assertEquals(
                        startSenderAccountInfo.getBalance() - Double.valueOf(transferAmount),
                        afterTransferSenderAccountInfo.getBalance()
                ),
                () -> Assertions.assertEquals(
                        startReceiverAccountInfo.getBalance() + Double.valueOf(transferAmount),
                        afterTransferReceiverAccountInfo.getBalance()
                )
        );

        //переходим на форму 🔁 Transfer Again
        $$("button").findBy(Condition.exactText("🔁 Transfer Again")).click();

        //находим последнюю транзакцию
        $("input[placeholder='Enter name to find transactions']").setValue(userReceiverRequest.getUsername());

        //нажимаем на кнопку 🔍 Search Transactions
        $$("button").findBy(Condition.exactText("🔍 Search Transactions")).click();

        //нажимаем на кнопку 🔁 Repeat
        $$("button").findBy(Condition.exactText("🔁 Repeat")).click();

        //проверяем, что открылось модальное окно
        $$(".modal-title").findBy(Condition.exactText("🔁 Repeat transfer")).shouldBe(visible);

        //выбираем счет, с которого хотим перевести
        $("select.form-control")
                .selectOption(
                        String.format("%s (Balance: $%s)",
                                startSenderAccountInfo.getAccountNumber(),
                                String.format("%.2f", startSenderAccountInfo.getBalance())
                        ).replace(",", ".")
                );

        //вводим сумму перевода равную 0
        $(".form-control[type='number']").setValue("0.00");

        //подтверждаем Confirm details are correct
        $("#confirmCheck").click();

        //нажимаем на кнопку 🚀 Send Transfer
        $$("button").findBy(Condition.exactText("🚀 Send Transfer")).click();

        //Проверить текст аллерта
        alert = switchTo().alert();
        assertThat(alert.getText()).contains("❌ Transfer failed: Please try again.");

        //проверяем, что на счете отправителя и получателя баланс не изменился
        var actualSenderAccountInfo = AccountSteps.getAccountInfo(userSenderRequest, senderAccountId);
        var actualReceiverAccountInfo = AccountSteps.getAccountInfo(userReceiverRequest, receiverAccountId);

        Assertions.assertAll(
                () -> Assertions.assertEquals(afterTransferSenderAccountInfo, actualSenderAccountInfo),
                () -> Assertions.assertEquals(afterTransferReceiverAccountInfo, actualReceiverAccountInfo)
        );
    }

    @Test
    public void userCannotTransferWithUncheckedConfirmationBoxAndСanCloseModalForm() {
        //авторизуемся и переходим на /dashboard
        Selenide.open("/");

        Selenide.executeJavaScript("localStorage.setItem('authToken', arguments[0]);", senderAuthToken);

        Selenide.open("/dashboard");

        //нажимаем на кнопку 🔄 Make a Transfer
        $$("button").findBy(Condition.exactText("🔄 Make a Transfer")).click();

        //проверяем, что на странице /deposit есть тег h1 с текстом 💰 Deposit Money
        $$("h1").findBy(Condition.exactText("🔄 Make a Transfer")).shouldBe(visible);

        //выбираем счет, с которого хотим перевести
        $(".account-selector")
                .selectOption(
                        String.format("%s (Balance: $%s)",
                                startSenderAccountInfo.getAccountNumber(),
                                String.format("%.2f", startSenderAccountInfo.getBalance())
                        ).replace(",", ".")
                );

        //вводим имя получателя
        $("input[placeholder='Enter recipient name']").setValue(userReceiverRequest.getUsername());

        //вводим номер счета получателя
        $("input[placeholder='Enter recipient account number']").setValue(startReceiverAccountInfo.getAccountNumber());

        //вводим сумму перевода
        var transferAmount = "3.00";
        $("input[placeholder='Enter amount']").setValue(transferAmount);

        //подтверждаем Confirm details are correct
        $("#confirmCheck").click();

        //нажимаем на кнопку 🚀 Send Transfer
        $$("button").findBy(Condition.exactText("🚀 Send Transfer")).click();

        //Проверить текст аллерта
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains(String.format("✅ Successfully transferred $%s to account %s!", transferAmount, startReceiverAccountInfo.getAccountNumber()));

        //проверяем, что на счете отправителя баланс стал меньше, а на счете получателя больше на сумму перевода
        var afterTransferSenderAccountInfo = AccountSteps.getAccountInfo(userSenderRequest, senderAccountId);
        var afterTransferReceiverAccountInfo = AccountSteps.getAccountInfo(userReceiverRequest, receiverAccountId);

        Assertions.assertAll(
                () -> Assertions.assertEquals(
                        startSenderAccountInfo.getBalance() - Double.valueOf(transferAmount),
                        afterTransferSenderAccountInfo.getBalance()
                ),
                () -> Assertions.assertEquals(
                        startReceiverAccountInfo.getBalance() + Double.valueOf(transferAmount),
                        afterTransferReceiverAccountInfo.getBalance()
                )
        );

        //переходим на форму 🔁 Transfer Again
        $$("button").findBy(Condition.exactText("🔁 Transfer Again")).click();

        //находим последнюю транзакцию
        $("input[placeholder='Enter name to find transactions']").setValue(userReceiverRequest.getUsername());

        //нажимаем на кнопку 🔍 Search Transactions
        $$("button").findBy(Condition.exactText("🔍 Search Transactions")).click();

        //нажимаем на кнопку 🔁 Repeat
        $$("button").findBy(Condition.exactText("🔁 Repeat")).click();

        //проверяем, что открылось модальное окно
        $$(".modal-title").findBy(Condition.exactText("🔁 Repeat transfer")).shouldBe(visible);

        //выбираем счет, с которого хотим перевести
        $("select.form-control")
                .selectOption(
                        String.format("%s (Balance: $%s)",
                                startSenderAccountInfo.getAccountNumber(),
                                String.format("%.2f", startSenderAccountInfo.getBalance())
                        ).replace(",", ".")
                );

        //проверяем, что кнопка 🚀 Send Transfer задезейблина
        $$("button").findBy(Condition.exactText("🚀 Send Transfer")).shouldBe(Condition.disabled);

        //закрываем окно
        $$("button").findBy(Condition.exactText("Cancel")).click();
        $(".modal-content").should(disappear);

    }

}
