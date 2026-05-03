package Controller;

import DAO.UserDAO;
import Model.User;
import Utils.PasswordUtils;

public class ForgotPasswordController {

    private final UserDAO userDAO = new UserDAO();

    /** Returns the userId (int as String) if email exists and is active, else null. */
    public String verifyEmail(String email) {
        User user = userDAO.getUserByEmail(email);
        if (user == null)             return null;
        if (user.getIsDeleted() == 1) return null;
        return String.valueOf(user.getUserId());
    }

    public boolean resetPassword(String userId, String newPassword) {
        try {
            int id = Integer.parseInt(userId);
            return userDAO.updatePassword(id, PasswordUtils.hash(newPassword));
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
