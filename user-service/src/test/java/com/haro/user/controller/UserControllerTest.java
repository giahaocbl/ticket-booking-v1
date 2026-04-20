package com.haro.user.controller;

import com.haro.user.dto.ChangePasswordRequest;
import com.haro.user.dto.UpdateProfileRequest;
import com.haro.user.dto.UserRegistrationRequest;
import com.haro.user.dto.UserResponse;
import com.haro.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @Test
    void registerReturnsCreatedUser() {
        UserRegistrationRequest request = new UserRegistrationRequest("user@test.com", "secret123", "User");
        UserResponse expected = new UserResponse(
                UUID.randomUUID(),
                "user@test.com",
                "User",
                null,
                null,
                "active",
                null,
                null,
                null,
                Set.of("USER")
        );
        when(userService.registerUser(request)).thenReturn(expected);

        ResponseEntity<UserResponse> response = userController.register(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(userService).registerUser(request);
    }

    @Test
    void getUserReturnsOkUser() {
        UUID id = UUID.randomUUID();
        UserResponse expected = new UserResponse(id, "user@test.com", "User", null, null, "active", null, null, null, Set.of("USER"));
        when(userService.getUser(id)).thenReturn(expected);

        ResponseEntity<UserResponse> response = userController.getUser(id);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(userService).getUser(id);
    }

    @Test
    void updateProfileReturnsUpdatedUser() {
        UUID id = UUID.randomUUID();
        UpdateProfileRequest request = new UpdateProfileRequest("Updated Name", "123456789", "https://example.com/avatar.png");
        UserResponse expected = new UserResponse(id, "user@test.com", "Updated Name", "123456789", "https://example.com/avatar.png", "active", null, null, null, Set.of("USER"));
        when(userService.updateProfile(id, request)).thenReturn(expected);

        ResponseEntity<UserResponse> response = userController.updateProfile(id, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(userService).updateProfile(id, request);
    }

    @Test
    void changePasswordReturnsNoContent() {
        UUID id = UUID.randomUUID();
        ChangePasswordRequest request = new ChangePasswordRequest("oldPassword123", "newPassword123");

        ResponseEntity<Void> response = userController.changePassword(id, request);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(userService).changePassword(id, request);
    }
}
