package org.alex.controller;

import org.alex.exception.NoRouteFoundException;
import org.alex.exception.UnknownCountryException;
import org.alex.model.IsoCode;
import org.alex.service.RoutingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests RoutingController + GlobalExceptionHandler through Spring MVC's
 * real request-dispatch machinery (path-variable binding, exception
 * resolution, Jackson serialization), via MockMvc — without opening a real
 * network socket.
 *
 * @WebMvcTest excludes ALL @Service/@Component beans from the context by
 * design (this is the whole point of Spring's "slice test" annotations —
 * narrowing the context to one architectural layer). That means
 * RoutingService is never loaded here at all, not even for real — it must
 * be mocked, not CountryDataService. (An earlier version of this test
 * mocked CountryDataService instead, which failed with
 * NoSuchBeanDefinitionException for RoutingService, since RoutingController
 * needs a RoutingService bean that @WebMvcTest never creates.) Mocking
 * RoutingService directly is arguably the more correct boundary anyway:
 * RoutingController's actual unit of responsibility is "given whatever
 * RoutingService returns or throws, produce the right HTTP response" — it
 * doesn't know or care how RoutingService gets its data, so this test
 * shouldn't either.
 */
@WebMvcTest(RoutingController.class)
class RoutingControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoutingService routingService;

    private final IsoCode cze = new IsoCode("CZE");
    private final IsoCode aut = new IsoCode("AUT");
    private final IsoCode ita = new IsoCode("ITA");

    @Test
    void getRoutingCzeToItaReturns200WithExpectedRouteJson() throws Exception {
        when(routingService.findRoute(cze, ita)).thenReturn(List.of(cze, aut, ita));

        mockMvc.perform(get("/routing/CZE/ITA"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.route").isArray())
                .andExpect(jsonPath("$.route[0]").value("CZE"))
                .andExpect(jsonPath("$.route[1]").value("AUT"))
                .andExpect(jsonPath("$.route[2]").value("ITA"));
    }

    @Test
    void getRoutingReturns400WhenNoRouteFoundExceptionThrown() throws Exception {
        when(routingService.findRoute(any(), any()))
                .thenThrow(new NoRouteFoundException("No land route exists from CZE to ABW"));

        mockMvc.perform(get("/routing/CZE/ABW"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void getRoutingReturns400WhenUnknownCountryExceptionThrown() throws Exception {
        when(routingService.findRoute(any(), any()))
                .thenThrow(new UnknownCountryException("Unknown destination country code: ZZZ"));

        mockMvc.perform(get("/routing/CZE/ZZZ"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void getRoutingWithMalformedCodeReturns400WithoutEverCallingRoutingService() throws Exception {
        // "CZ" fails IsoCode's own validation inside the controller, before
        // routingService.findRoute(...) would ever be called — so this test
        // deliberately leaves routingService unstubbed for this case.
        mockMvc.perform(get("/routing/CZ/ITA")) // 2 letters, invalid shape
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}