package org.umaxcode.service;

import org.umaxcode.domain.dto.request.UserCreationDto;

public interface UserAuthService {

    String register(UserCreationDto request);
}
