package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.CustomerProfileRequest;
import requests.interfaces.GettableAll;
import requests.interfaces.Puttable;

import static io.restassured.RestAssured.given;

public class CustomerProfileRequester extends Request implements Puttable<CustomerProfileRequest>, GettableAll {
    public CustomerProfileRequester(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        super(requestSpecification, responseSpecification);
    }

    public ValidatableResponse put(CustomerProfileRequest model) {
        return  given()
                .spec(requestSpecification)
                .body(model)
                .put("/api/v1/customer/profile")
                .then()
                .assertThat()
                .spec(responseSpecification);
    }

    public ValidatableResponse get() {
        return  given()
                .spec(requestSpecification)
                .get("/api/v1/customer/profile")
                .then()
                .assertThat()
                .spec(responseSpecification);
    }
}
