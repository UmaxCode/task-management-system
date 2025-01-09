package org.umaxcode.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.umaxcode.domain.dto.request.UserCreationDto;
import org.umaxcode.domain.dto.response.SuccessResponse;
import org.umaxcode.domain.dto.response.UserDto;
import org.umaxcode.service.UserAuthService;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserAuthController {

    private final UserAuthService userAuthService;

    @PostMapping("/signup")
    @PreAuthorize(value = "hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public SuccessResponse signup(@RequestBody UserCreationDto request) {

        String message = userAuthService.register(request);
        return SuccessResponse.builder()
                .message(message)
                .build();
    }

    @GetMapping
    @PreAuthorize(value = "hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse fetchAllUsers() {

        List<UserDto> listOfUsers = userAuthService.fetchAllUsers();
        return SuccessResponse.builder()
                .message("Users fetched successfully")
                .data(listOfUsers)
                .build();
    }
}
