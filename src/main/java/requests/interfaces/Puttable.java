package requests.interfaces;

import io.restassured.response.ValidatableResponse;

public interface Puttable<T> {
    ValidatableResponse put(T model);
}
