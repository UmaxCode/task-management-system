package org.umaxcode.service;

import org.umaxcode.domain.dto.request.UserCreationDto;
import org.umaxcode.domain.dto.response.UserDto;

import java.util.List;

public interface UserAuthService {

    String register(UserCreationDto request);

    List<UserDto> fetchAllUsers();
}
