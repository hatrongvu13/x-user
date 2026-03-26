package com.htv.xuser.services.user;

import com.htv.xuser.model.dto.UserDto;
import com.htv.xuser.model.response.PageResponse;

import java.util.UUID;

public interface UserService {
    UserDto.ProfileResponse getMyProfile(String email);
    UserDto.ProfileResponse updateMyProfile(String email, UserDto.UpdateRequest req);
    PageResponse<UserDto.SummaryResponse> search(UserDto.SearchRequest req);
    UserDto.DetailResponse getById(UUID id);
    UserDto.DetailResponse create(UserDto.AdminCreateRequest req);
    UserDto.DetailResponse update(UUID id, UserDto.UpdateRequest req);
    UserDto.DetailResponse updateStatus(UUID id, UserDto.UpdateStatusRequest req);
    void delete(UUID id);
    UserDto.DetailResponse assignRole(UUID userId, UUID roleId);
    UserDto.DetailResponse removeRole(UUID userId, UUID roleId);
}
