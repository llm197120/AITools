package org.jeecg.modules.homeai.storage.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeaiLocalHttpUrlTest {

    @Test
    void allowsLoopbackAndLan() {
        assertTrue(HomeaiLocalHttpUrl.isLoopbackOrPrivate("http://127.0.0.1:3000"));
        assertTrue(HomeaiLocalHttpUrl.isLoopbackOrPrivate("http://localhost:8012/"));
        assertTrue(HomeaiLocalHttpUrl.isLoopbackOrPrivate("http://192.168.1.8:3000"));
        assertTrue(HomeaiLocalHttpUrl.isLoopbackOrPrivate("http://10.0.0.2:8012"));
        assertTrue(HomeaiLocalHttpUrl.isLoopbackOrPrivate("http://172.16.0.4:3000"));
        assertTrue(HomeaiLocalHttpUrl.isLoopbackOrPrivate("http://host.docker.internal:3000"));
    }

    @Test
    void rejectsPublicHosts() {
        assertFalse(HomeaiLocalHttpUrl.isLoopbackOrPrivate(""));
        assertFalse(HomeaiLocalHttpUrl.isLoopbackOrPrivate("https://gotenberg.example.com"));
        assertFalse(HomeaiLocalHttpUrl.isLoopbackOrPrivate("https://kkview.cn"));
        assertFalse(HomeaiLocalHttpUrl.isLoopbackOrPrivate("http://8.8.8.8:3000"));
        assertFalse(HomeaiLocalHttpUrl.isLoopbackOrPrivate("https://api.cloudconvert.com"));
    }
}
