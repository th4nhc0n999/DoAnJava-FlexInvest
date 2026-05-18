package doanjava;
import Model.AccountModel;
import Model.User;
import Model.Account;
import View.MainPage;
import java.sql.Timestamp;

public class DoAnJava {
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            Account acc = new Account();
            acc.setUsername("customer01");
            acc.setAccountId(1);
            Model.User u = new Model.User();
            u.setRoleId(3);   // 1=Admin, 2=Staff, 3=Customer
            u.setUserId(1);
            AccountModel am = new AccountModel(acc, u, "mockToken", new java.sql.Timestamp(System.currentTimeMillis()));
            new MainPage(am);
        });
    }
}
