package Utils;

import Model.AccountModel;

public class SessionManager {

    private static AccountModel currentAccount;

    public static void login(AccountModel account) {
        currentAccount = account;
    }

    public static AccountModel getCurrent() {
        return currentAccount;
    }

    public static void logout() {
        currentAccount = null;
    }

    public static boolean isLoggedIn() {
        return currentAccount != null;
    }
}
