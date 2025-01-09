package org.umaxcode.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PasswordGenerator {

    // Character sets for password generation
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()-_=+[]{}|;:,.<>?";

    private static final String ALL_CHARACTERS = LOWERCASE + UPPERCASE + DIGITS + SYMBOLS;

    private static final int MIN_PASSWORD_LENGTH = 8;

    public static String generatePassword() {

        SecureRandom random = new SecureRandom();

        // Ensure password contains at least one uppercase, one lowercase, one digit, and one symbol
        List<Character> passwordList = new ArrayList<>();

        passwordList.add(getRandomCharacter(UPPERCASE, random)); // At least one uppercase letter
        passwordList.add(getRandomCharacter(LOWERCASE, random)); // At least one lowercase letter
        passwordList.add(getRandomCharacter(DIGITS, random));    // At least one digit
        passwordList.add(getRandomCharacter(SYMBOLS, random));   // At least one symbol

        // Fill the remaining length with random characters from all available sets
        for (int i = passwordList.size(); i < MIN_PASSWORD_LENGTH; i++) {
            passwordList.add(getRandomCharacter(ALL_CHARACTERS, random));
        }

        // Shuffle the password list to randomize character positions
        Collections.shuffle(passwordList, random);

        // Convert the list of characters back to a string
        StringBuilder shuffledPassword = new StringBuilder();
        for (char c : passwordList) {
            shuffledPassword.append(c);
        }

        return shuffledPassword.toString();
    }

    private static char getRandomCharacter(String characterSet, SecureRandom random) {
        return characterSet.charAt(random.nextInt(characterSet.length()));
    }
}
