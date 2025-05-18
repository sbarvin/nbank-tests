package models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerProfileResponse extends BaseModel {

    private String message;
    private Customer customer;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Customer extends BaseModel {

        private long id;
        private String username;
        private String password;
        private String name;
        private String role;
        private List<DepositAccountResponse> accounts;
    }
}
