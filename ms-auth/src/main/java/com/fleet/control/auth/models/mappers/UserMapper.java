package com.fleet.control.auth.models.mappers;

import com.fleet.control.auth.models.dto.token.response.TokenResponse;
import com.fleet.control.auth.models.dto.user.request.RegisterRequest;
import com.fleet.control.auth.models.entities.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "role", constant = "MANAGER")
    @Mapping(target = "password", ignore = true)
    UserEntity toUserEntity(RegisterRequest request);

    @Mapping(target = "token", source = "token")
    @Mapping(target = "tokenType", constant = "Bearer")
    @Mapping(target = "expiresIn", expression = "java(3600L)")
    @Mapping(target = "userId", source = "userId")
    TokenResponse toTokenResponse(String token, String userId);
}