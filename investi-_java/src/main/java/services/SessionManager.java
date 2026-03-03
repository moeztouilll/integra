package services;

import models.UserSession;
import java.util.ArrayList;
import java.util.List;

public class SessionManager {
    private static UserSession currentUser;
    private static final List<UserSession> ALL_USERS = new ArrayList<>();

    static {
        ALL_USERS.add(new UserSession(1, "Moez", "Touil", "touilmoez358@gmail.com", UserSession.Role.USER));
        ALL_USERS.add(new UserSession(2, "Toutou", "Mezmez", "toutoumezmez@gmail.com", UserSession.Role.USER));
        ALL_USERS.add(new UserSession(3, "Admin", "System", "admin@pidev.tn", UserSession.Role.ADMIN));
    }

    public static List<UserSession> getAllUsers() {
        return ALL_USERS;
    }

    public static UserSession getCurrentUser() {
        if (currentUser == null) {
            currentUser = ALL_USERS.get(0); // Default to User 1
        }
        return currentUser;
    }

    public static void setCurrentUser(UserSession user) {
        currentUser = user;
    }
}
