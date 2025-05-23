package models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountTransactionsResponse extends BaseModel {

    private List<Transaction> transactions;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Transaction {

        private long id;
        private double amount;
        private String type;
        private String timestamp;
        private long relatedAccountId;
    }
}
