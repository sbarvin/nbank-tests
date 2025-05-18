package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.CreateUserRequest;
import requests.interfaces.Postable;

import static io.restassured.RestAssured.given;

public class AdminCreateUserRequester extends Request implements Postable<CreateUserRequest> {
    public AdminCreateUserRequester(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        super(requestSpecification, responseSpecification);
    }


    public ValidatableResponse post(CreateUserRequest model) {
        return given()
                .spec(requestSpecification)
                .body(model)
                .post("/api/v1/admin/users")
                .then()
                .assertThat()
                .spec(responseSpecification);
    }
}