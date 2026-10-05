package com.example.shortener;

import static org.junit.jupiter.api.Assertions.*;

import com.example.shortener.service.CodeGenerator;
import java.util.Random;
import org.junit.jupiter.api.Test;

class CodeGeneratorTest {
    @Test void producesRequestedLengthWithBase62Chars() {
        String code = CodeGenerator.generate(new Random(1), 7);
        assertEquals(7, code.length());
        assertTrue(code.matches("[0-9A-Za-z]{7}"));
    }
    @Test void sameSeedIsDeterministic() {
        assertEquals(CodeGenerator.generate(new Random(42), 10), CodeGenerator.generate(new Random(42), 10));
    }
    @Test void differentCallsUsuallyDiffer() {
        Random r = new Random(7);
        assertNotEquals(CodeGenerator.generate(r, 8), CodeGenerator.generate(r, 8));
    }
    @Test void rejectsNonPositiveLength() {
        assertThrows(IllegalArgumentException.class, () -> CodeGenerator.generate(new Random(), 0));
    }
}
