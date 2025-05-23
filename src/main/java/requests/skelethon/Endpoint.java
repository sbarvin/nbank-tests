package requests.skelethon;

import lombok.AllArgsConstructor;
import lombok.Getter;
import models.*;

@Getter
@AllArgsConstructor
public enum Endpoint {
    ADMIN_USER(
            "/admin/users",
            CreateUserRequest.class,
            CreateUserResponse.class
    ),

    LOGIN(
            "/auth/login",
            LoginUserRequest.class,
            LoginUserResponse.class
    ),

    ACCOUNTS(
            "/accounts",
            BaseModel.class,
            CreateAccountResponse.class
    ),

    ACCOUNTS_DEPOSIT(
            "/accounts/deposit",
            DepositAccountRequest.class,
            DepositAccountResponse.class
    ),

    ACCOUNTS_TRANSFER(
            "/accounts/transfer",
            TransferAccountRequest.class,
            TransferAccountResponse.class
    ),

    CUSTOMER_PROFILE_UPDATE(
            "/customer/profile",
            CustomerProfileRequest.class,
            CustomerProfileResponse.class
    ),

    CUSTOMER_PROFILE(
            "/customer/profile",
            CustomerProfileRequest.class,
            CustomerProfileResponse.Customer.class
    );


    private final String url;
    private final Class<? extends BaseModel> requestModel;
    private final Class<? extends BaseModel> responseModel;
}
