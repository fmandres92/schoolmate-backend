package com.schoolmate.api.controller;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.support.TestJsonMapperFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DevToolsControllerContractTest {

    @Mock private ClockProvider clockProvider;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        DevToolsController controller = new DevToolsController(clockProvider);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setValidator(validator)
            .setMessageConverters(new JacksonJsonHttpMessageConverter(TestJsonMapperFactory.create()))
            .build();
    }

    @Test
    void getClock_retorna200ConEstado() throws Exception {
        when(clockProvider.now()).thenReturn(LocalDateTime.of(2026, 2, 25, 10, 30));
        when(clockProvider.isOverridden()).thenReturn(false);

        mockMvc.perform(get("/api/dev/clock"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentDateTime").value("2026-02-25T10:30"))
            .andExpect(jsonPath("$.isOverridden").value(false));
    }

    @Test
    void setClock_retorna200YDelega() throws Exception {
        when(clockProvider.now()).thenReturn(LocalDateTime.of(2026, 2, 25, 12, 0));
        when(clockProvider.isOverridden()).thenReturn(true);

        String body = """
            {"dateTime":"2026-02-25T12:00:00"}
            """;

        mockMvc.perform(post("/api/dev/clock")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isOverridden").value(true));

        verify(clockProvider).setFixed(LocalDateTime.of(2026, 2, 25, 12, 0));
    }

    @Test
    void resetClock_retorna200YDelega() throws Exception {
        when(clockProvider.now()).thenReturn(LocalDateTime.of(2026, 2, 25, 9, 0));
        when(clockProvider.isOverridden()).thenReturn(false);

        mockMvc.perform(delete("/api/dev/clock"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentDateTime").value("2026-02-25T09:00"))
            .andExpect(jsonPath("$.isOverridden").value(false));

        verify(clockProvider).reset();
    }

    @Test
    void setClock_conBodyInvalido_retorna400YNoDelega() throws Exception {
        mockMvc.perform(post("/api/dev/clock")
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(clockProvider);
    }
}
