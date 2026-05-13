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
            acc.setUsername("admin");
            User u = new User();
            u.setRoleId(1);
            AccountModel am = new AccountModel(acc, u, "mockToken", new Timestamp(System.currentTimeMillis()));
            new MainPage(am);
        });
    }
}
