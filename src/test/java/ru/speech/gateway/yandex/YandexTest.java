package ru.speech.gateway.yandex;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Ivan
 */
public class YandexTest {
    
    public YandexTest() {
    }
    @Test
    public void testCharsToNames() {
        assertEquals(Yandex.charsToNames("ас"), "Алексей.Сергей");
        assertEquals(Yandex.charsToNames(" х а т "), "Харитон.Алексей.Тарас");
        assertEquals(Yandex.charsToNames("хат"), "Харитон.Алексей.Тарас");
        assertEquals(Yandex.charsToNames("xaт"), "Харитон.Алексей.Тарас");
        assertEquals(Yandex.charsToNames("ВАТ"), "Вадим.Алексей.Тарас");
        assertEquals(Yandex.charsToNames("ВАТ123"), "Вадим.Алексей.Тарас.1.2.3");
        assertEquals(Yandex.charsToNames("123"), "1.2.3");
    }

    
}
