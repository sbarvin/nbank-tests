package requests.interfaces;

import io.restassured.response.ValidatableResponse;

public interface Postable<T> {
    ValidatableResponse post(T model);
}
