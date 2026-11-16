package hu.szakdolgozat.borbely.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A jelszószabály egységtesztjei, Spring-környezet nélkül.
 */
class StrongPasswordValidatorTest {

    @Test
    void acceptsPasswordWithLetterAndDigit() {
        assertThat(StrongPasswordValidator.problem("Titkos123")).isNull();
    }

    @Test
    void acceptsAccentedLetters() {
        assertThat(StrongPasswordValidator.problem("Árvíztűrő1")).isNull();
    }

    @Test
    void rejectsBlankPassword() {
        assertThat(StrongPasswordValidator.problem("   ")).contains("kötelező");
        assertThat(StrongPasswordValidator.problem(null)).contains("kötelező");
    }

    @Test
    void rejectsTooShortPassword() {
        assertThat(StrongPasswordValidator.problem("Ab1")).contains("legalább 8");
    }

    @Test
    void rejectsPasswordWithoutDigit() {
        assertThat(StrongPasswordValidator.problem("csakbetuk")).contains("számjegy");
    }

    @Test
    void rejectsPasswordWithoutLetter() {
        assertThat(StrongPasswordValidator.problem("12345678")).contains("betű");
    }

    @Test
    void rejectsMoreThan64Characters() {
        assertThat(StrongPasswordValidator.problem("a1".repeat(33))).contains("túl hosszú");
    }

    @Test
    void rejectsMoreThan72BytesEvenUnder64Characters() {
        // 36 darab "á" (2-2 bájt) és egy számjegy: 37 karakter, de 73 bájt.
        assertThat(StrongPasswordValidator.problem("á".repeat(36) + "1")).contains("túl hosszú");
    }
}
