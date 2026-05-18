package doanjava;

import View.LoginForm;

public class DoAnJava {
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            new LoginForm();
        });
    }
}
