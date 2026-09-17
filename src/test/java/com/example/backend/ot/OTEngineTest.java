package com.example.backend.ot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OTEngineTest {

    @Test
    void testApplyInsertAtEnd() {
        String doc = "Hello";
        TextOperation op = new TextOperation().retain(5).insert(" World");
        String result = OTEngine.apply(doc, op);
        assertEquals("Hello World", result);
    }

    @Test
    void testApplyInsertAtBeginning() {
        String doc = "World";
        TextOperation op = new TextOperation().insert("Hello ").retain(5);
        String result = OTEngine.apply(doc, op);
        assertEquals("Hello World", result);
    }

    @Test
    void testApplyDelete() {
        String doc = "Hello World";
        TextOperation op = new TextOperation().retain(5).delete(6);
        String result = OTEngine.apply(doc, op);
        assertEquals("Hello", result);
    }

    @Test
    void testConcurrentInsertConvergence() {
        String initialDoc = "Code";

        // User A inserts " Live" at index 4
        TextOperation opA = new TextOperation().retain(4).insert(" Live");
        // User B inserts " Fast" at index 4
        TextOperation opB = new TextOperation().retain(4).insert(" Fast");

        // Transform opA against opB
        TextOperation opAPrime = OTEngine.transform(opA, opB, OTEngine.Priority.LEFT);
        // Transform opB against opA
        TextOperation opBPrime = OTEngine.transform(opB, opA, OTEngine.Priority.RIGHT);

        // Apply in both orders:
        // doc -> opA -> opB'
        String docA = OTEngine.apply(initialDoc, opA);
        String finalDoc1 = OTEngine.apply(docA, opBPrime);

        // doc -> opB -> opA'
        String docB = OTEngine.apply(initialDoc, opB);
        String finalDoc2 = OTEngine.apply(docB, opAPrime);

        // Both clients must converge to the identical final state!
        assertEquals(finalDoc1, finalDoc2);
        assertEquals("Code Live Fast", finalDoc1);
    }
}
