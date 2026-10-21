package com.example.store.user;

import java.util.Locale;

final class EmailAddress {

    private EmailAddress() {
    }

    static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
