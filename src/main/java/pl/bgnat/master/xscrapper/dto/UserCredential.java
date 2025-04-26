package pl.bgnat.master.xscrapper.dto;

import java.util.concurrent.atomic.AtomicInteger;

public record UserCredential(String username,
                             String email,
                             String password,
                             String cookiePath,
                             boolean useProxy,
                             UserProxy proxy) {

    public enum User {
        USER_1, USER_2, USER_3, USER_4, USER_5;

        private static final AtomicInteger currentIndex = new AtomicInteger(0);
        public static final int SIZE = values().length;

        public static User getNextAvailableUser() {
            int nextIndex = currentIndex.getAndUpdate(i -> (i + 1) % SIZE);
            return values()[nextIndex];
        }
    }

    public static User getUser(int userId) {
        return User.values()[userId];
    }
}

