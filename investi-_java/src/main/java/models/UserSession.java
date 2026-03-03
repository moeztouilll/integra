package models;

public class UserSession {
    public enum Role {
        USER, ADMIN
    }

    private long id;
    private String firstName;
    private String lastName;
    private String email;
    private Role role;

    public UserSession(long id, String firstName, String lastName, String email, Role role) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
    }

    public long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public String toString() {
        return firstName + " " + lastName + " (" + role.name() + ")";
    }
}
