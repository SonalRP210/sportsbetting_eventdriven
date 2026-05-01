package com.sportsbetting.settlementservice;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class ApplicationTest {

    @Test
    void mainCallsSpringApplicationRun() {
        try (MockedStatic<SpringApplication> mocked = mockStatic(SpringApplication.class)) {
            Application.main(new String[]{});
            mocked.verify(() -> SpringApplication.run(Application.class, new String[]{}));
        }
    }
}
