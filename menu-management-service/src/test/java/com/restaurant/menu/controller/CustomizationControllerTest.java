package com.restaurant.menu.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import com.restaurant.menu.service.CustomizationService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomizationController.class)
@AutoConfigureMockMvc(addFilters = false)
public class CustomizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomizationService customizationService;

    @Test
    public void testGetBuildOptions_Success() throws Exception {
        mockMvc.perform(get("/api/customizations/build-options?primaryCategory=Burger"))
                .andExpect(status().isOk());
    }
}
