package pl.bgnat.master.xscrapper.model;

public record UserCredential(String username, String email,String password, String cookiePath) {
    public enum User {
        USER_1, USER_2, USER_3, USER_4, USER_5
    }
}
