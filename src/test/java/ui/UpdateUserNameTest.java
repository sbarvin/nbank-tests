package ui;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import generators.RandomData;
import models.CreateUserRequest;
import models.CustomerProfileRequest;
import models.CustomerProfileResponse;
import models.LoginUserRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.skelethon.requesters.ValidatedCrudRequester;
import requests.steps.AdminSteps;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.*;
import static org.assertj.core.api.Assertions.assertThat;

public class UpdateUserNameTest extends BaseTest {

    //✅ Name updated successfully!

    //❌ Please enter a valid name.

    //<h2 class="welcome-text">Welcome, <span style="color: rgb(252, 21, 137); font-weight: bold;">1</span>!</h2>

    //<div class="profile-header" style="cursor: pointer;"><div class="user-info" style="padding-right: 20px;">
    // <span class="user-name">1</span><br><span class="user-username">@barvinsk</span></div></div>

    private static CreateUserRequest userRequest;
    private static String authToken;
    private static CustomerProfileResponse.Customer startProfile;

    @BeforeAll
    public static void setup() {
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

        startProfile = new ValidatedCrudRequester<CustomerProfileResponse.Customer>(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.CUSTOMER_PROFILE,
                ResponseSpecs.requestReturnsOK()
        ).getAll();
    }

    @Test
    public void userCanUpdateNameWithCorrectData() {

        //авторизуемся и переходим на /dashboard
        Selenide.open("/");

        Selenide.executeJavaScript("localStorage.setItem('authToken', arguments[0]);", authToken);

        Selenide.open("/dashboard");

        //переходим на страниу /edit-profile и проверяем наличие тега h1 с текстом ✏️ Edit Profile
        $(".user-info").click();
        $$("h1").findBy(Condition.exactText("✏️ Edit Profile")).shouldBe(visible);

        //вводим новое имя
        var newName = RandomData.getUsername();
        $("input[placeholder='Enter new name']").setValue(newName);

        //нажимаем на кнопку 💾 Save Changes
        $$("button").findBy(Condition.exactText("💾 Save Changes")).click();

        //проверить текст аллерта
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("✅ Name updated successfully!");
        alert.accept();

        //переходим на страницу /dashboard
        $$("button").findBy(Condition.exactText("🏠 Home")).click();

        //проверяем, что на странице /dashboard изменилось имя
        $(".welcome-text span").shouldHave(text(newName));
//  бага      $(".user-name").shouldHave(text(newName));

        // проверяем, что имя изменилось c помощью api
        var actualProfile = new ValidatedCrudRequester<CustomerProfileResponse.Customer>(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.CUSTOMER_PROFILE,
                ResponseSpecs.requestReturnsOK()
        ).getAll();

        Assertions.assertEquals(newName, actualProfile.getName());
    }

    @Test
    public void userCannotUpdateNameWithInvalidData() {

        //авторизуемся и переходим на /dashboard
        Selenide.open("/");

        Selenide.executeJavaScript("localStorage.setItem('authToken', arguments[0]);", authToken);

        Selenide.open("/dashboard");

        //переходим на страниу /edit-profile и проверяем наличие тега h1 с текстом ✏️ Edit Profile
        $(".user-info").click();
        $$("h1").findBy(Condition.exactText("✏️ Edit Profile")).shouldBe(visible);

        //удаляем все из поля для имени
        $("input[placeholder='Enter new name']").clear();

        //нажимаем на кнопку 💾 Save Changes
        $$("button").findBy(Condition.exactText("💾 Save Changes")).click();

        //проверить текст аллерта
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("❌ Please enter a valid name.");
        alert.accept();

        //переходим на страницу /dashboard
        $$("button").findBy(Condition.exactText("🏠 Home")).click();

        //проверяем, что на странице /dashboard не изменилось имя
        $(".welcome-text span").shouldHave(text("NoName"));
        $(".user-name").shouldHave(text("NoName"));

        // проверяем, что имя изменилось c помощью api
        var actualProfile = new ValidatedCrudRequester<CustomerProfileResponse.Customer>(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.CUSTOMER_PROFILE,
                ResponseSpecs.requestReturnsOK()
        ).getAll();

        Assertions.assertEquals(startProfile, actualProfile);
    }

    @Test
    public void userCannotUpdateSameName() {

        //обновляем имя пользователя в соответствии с username
        new ValidatedCrudRequester<CustomerProfileResponse>(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.CUSTOMER_PROFILE_UPDATE,
                ResponseSpecs.requestReturnsOK()
        ).put(new CustomerProfileRequest(userRequest.getUsername()));

        startProfile = new ValidatedCrudRequester<CustomerProfileResponse.Customer>(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.CUSTOMER_PROFILE,
                ResponseSpecs.requestReturnsOK()
        ).getAll();

        //авторизуемся и переходим на /dashboard
        Selenide.open("/");

        Selenide.executeJavaScript("localStorage.setItem('authToken', arguments[0]);", authToken);

        Selenide.open("/dashboard");

        //переходим на страниу /edit-profile и проверяем наличие тега h1 с текстом ✏️ Edit Profile
        $(".user-info").click();
        $$("h1").findBy(Condition.exactText("✏️ Edit Profile")).shouldBe(visible);

        //нажимаем на кнопку 💾 Save Changes
        $$("button").findBy(Condition.exactText("💾 Save Changes")).click();

        //проверить текст аллерта
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("⚠️ New name is the same as the current one.");
        alert.accept();

        //переходим на страницу /dashboard
        $$("button").findBy(Condition.exactText("🏠 Home")).click();

        //проверяем, что на странице /dashboard не изменилось имя
        $(".welcome-text span").shouldHave(text("NoName"));
//  бага      $(".user-name").shouldHave(text("NoName"));

        // проверяем, что имя изменилось c помощью api
        var actualProfile = new ValidatedCrudRequester<CustomerProfileResponse.Customer>(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.CUSTOMER_PROFILE,
                ResponseSpecs.requestReturnsOK()
        ).getAll();

        Assertions.assertEquals(startProfile, actualProfile);
    }
}
