package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.DepositAccountRequest;
import requests.interfaces.Postable;

import static io.restassured.RestAssured.given;

public class DepositeAccountRequester extends Request implements Postable<DepositAccountRequest> {

    public DepositeAccountRequester(RequestSpecification requestSpecification,
                                    ResponseSpecification responseSpecification) {
        super(requestSpecification, responseSpecification);
    }

    public ValidatableResponse post(DepositAccountRequest model) {
        return given()
                .spec(requestSpecification)
                .body(model)
                .when()
                .post("/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .spec(responseSpecification);
    }
}
