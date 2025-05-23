package models.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ApiErrorMessages {
    INVALID_ACCOUNT_OR_AMOUNT("Invalid account or amount"),
    UNAUTHORIZED_ACCESS_TO_ACCOUNT("Unauthorized access to account"),
    INVALID_TRANSFER("Invalid transfer: insufficient funds or invalid accounts");

    private final String message;
}
