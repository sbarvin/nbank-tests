package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.TransferAccountRequest;
import requests.interfaces.Postable;

import static io.restassured.RestAssured.given;

public class TransferAccountRequester extends Request implements Postable<TransferAccountRequest> {

    public TransferAccountRequester(RequestSpecification requestSpecification,
                                    ResponseSpecification responseSpecification) {
        super(requestSpecification, responseSpecification);
    }


    public ValidatableResponse post(TransferAccountRequest model) {
        return given()
                .spec(requestSpecification)
                .body(model)
                .when()
                .post("/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .spec(responseSpecification);
    }

}
