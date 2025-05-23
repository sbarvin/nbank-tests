package api;

import generators.RandomData;
import models.CreateUserRequest;
import models.CustomerProfileRequest;
import models.CustomerProfileResponse;
import models.constants.UserRole;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import requests.AdminCreateUserRequester;
import requests.CustomerProfileRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

public class UpdateUserNameTest {

    private static CreateUserRequest userRequest;

    @BeforeAll
    public static void setup() {
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
    }

    @Test
    void userCanUpdateNameWithValidData() {

        // формируем запрос на изменение имени
        var updateProfileRequest = CustomerProfileRequest.builder()
                .name(RandomStringUtils.randomAlphabetic(10))
                .build();

        // изменяем имя
        new CustomerProfileRequester(RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .put(updateProfileRequest);

        // проверяем, что имя изменилось
        var actualProfile = new CustomerProfileRequester(RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .get()
                .extract().as(CustomerProfileResponse.Customer.class);

        Assertions.assertEquals(updateProfileRequest.getName(), actualProfile.getName());
    }
}
