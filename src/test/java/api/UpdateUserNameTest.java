package api;

import generators.RandomModelGenerator;
import models.CreateUserRequest;
import models.CustomerProfileRequest;
import models.CustomerProfileResponse;
import models.comparison.ModelAssertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.ValidatedCrudRequester;
import requests.steps.AdminSteps;
import specs.RequestSpecs;
import specs.ResponseSpecs;

public class UpdateUserNameTest {

    private static CreateUserRequest userRequest;

    @BeforeAll
    public static void setup() {
        //создаем пользователя
        userRequest = AdminSteps.createUser();
    }

    @Test
    void userCanUpdateNameWithValidData() {

        // формируем запрос на изменение имени
        var updateProfileRequest = RandomModelGenerator.generate(CustomerProfileRequest.class);

        // изменяем имя
        new ValidatedCrudRequester<CustomerProfileResponse>(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.CUSTOMER_PROFILE_UPDATE,
                ResponseSpecs.requestReturnsOK()
        ).put(updateProfileRequest);

        // проверяем, что имя изменилось
        var actualProfile = new ValidatedCrudRequester<CustomerProfileResponse.Customer>(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.CUSTOMER_PROFILE,
                ResponseSpecs.requestReturnsOK()
        ).getAll();

        ModelAssertions.assertThatModels(updateProfileRequest, actualProfile).match();
    }
}
