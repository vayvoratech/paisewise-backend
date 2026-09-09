package in.sapphirus.rupee.auth.api;

import in.sapphirus.rupee.auth.api.AuthDtos.*;
import in.sapphirus.rupee.auth.service.AuthService;
import in.sapphirus.rupee.security.JwtProperties;
import in.sapphirus.rupee.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import in.sapphirus.rupee.security.ResourceServerSecurityConfig;

@WebMvcTest(controllers = AuthController.class, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ResourceServerSecurityConfig.class)
}, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
})
@DirtiesContext
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Test
    void register_validRequest_returnsCreated() throws Exception {
        UserView userView = new UserView(UUID.randomUUID().toString(), "Rahul", "9876543210", "rahul@example.com", false);
        Tokens tokens = new Tokens("accessToken", "refreshToken");
        when(authService.register(any(RegisterRequest.class))).thenReturn(new AuthResponse(userView, tokens));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"9876543210\",\"name\":\"Rahul\",\"email\":\"rahul@example.com\",\"password\":\"secret1\",\"confirmPassword\":\"secret1\"}"))
                .andExpect(status().isCreated());

        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    void register_invalidEmail_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"9876543210\",\"name\":\"Rahul\",\"email\":\"invalid-email\",\"password\":\"secret1\",\"confirmPassword\":\"secret1\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void login_validRequest_returnsOk() throws Exception {
        UserView userView = new UserView(UUID.randomUUID().toString(), "Rahul", "9876543210", "rahul@example.com", false);
        Tokens tokens = new Tokens("accessToken", "refreshToken");
        when(authService.login(any(LoginRequest.class))).thenReturn(new AuthResponse(userView, tokens));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"9876543210\",\"password\":\"secret1\"}"))
                .andExpect(status().isOk());

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    void login_missingPhone_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"\",\"password\":\"secret1\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void refresh_validRequest_returnsOk() throws Exception {
        when(authService.refresh("token")).thenReturn(new Tokens("newAccess", "newRefresh"));

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"token\"}"))
                .andExpect(status().isOk());

        verify(authService, times(1)).refresh("token");
    }

    @Test
    void refreshTokenAlias_validRequest_returnsOk() throws Exception {
        when(authService.refresh("token")).thenReturn(new Tokens("newAccess", "newRefresh"));

        mockMvc.perform(post("/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"token\"}"))
                .andExpect(status().isOk());

        verify(authService, times(1)).refresh("token");
    }

    @Test
    void forgotPassword_validRequest_returnsOk() throws Exception {
        doNothing().when(authService).forgotPassword("rahul@example.com");

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"rahul@example.com\"}"))
                .andExpect(status().isOk());

        verify(authService, times(1)).forgotPassword("rahul@example.com");
    }

    @Test
    void verifyOtp_validOtp_returnsOk() throws Exception {
        when(authService.verifyOtp("rahul@example.com", "123456")).thenReturn(true);

        mockMvc.perform(post("/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"rahul@example.com\",\"otp\":\"123456\"}"))
                .andExpect(status().isOk());

        verify(authService, times(1)).verifyOtp("rahul@example.com", "123456");
    }

    @Test
    void verifyOtp_invalidOtpFormat_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"rahul@example.com\",\"otp\":\"123\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void resetPassword_validRequest_returnsOk() throws Exception {
        doNothing().when(authService).resetPassword("rahul@example.com", "newpass123");

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"rahul@example.com\",\"newPassword\":\"newpass123\"}"))
                .andExpect(status().isOk());

        verify(authService, times(1)).resetPassword("rahul@example.com", "newpass123");
    }

    @Test
    void sendOtp_validRequest_returnsOk() throws Exception {
        doNothing().when(authService).sendOtpForPhone("9876543210");

        mockMvc.perform(post("/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"9876543210\"}"))
                .andExpect(status().isOk());

        verify(authService, times(1)).sendOtpForPhone("9876543210");
    }

    @Test
    void setMpin_validRequest_returnsOk() throws Exception {
        doNothing().when(authService).setMpin("rahul@example.com", "123456");

        mockMvc.perform(post("/auth/set-mpin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"rahul@example.com\",\"mpin\":\"123456\"}"))
                .andExpect(status().isOk());

        verify(authService, times(1)).setMpin("rahul@example.com", "123456");
    }
}
