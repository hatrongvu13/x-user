package com.htv.xuser.controller;

import com.htv.xuser.model.dto.UserDto;
import com.htv.xuser.model.response.ApiResponse;
import com.htv.xuser.model.response.PageResponse;
import com.htv.xuser.services.msg.MessageService;
import com.htv.xuser.services.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * UserController
 *
 * Base: /api/v1/users
 */
@RestController
@RequestMapping(value = "/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final MessageService msg;

    // ── GET /me ───────────────────────────────────────────────────────────────

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto.ProfileResponse>> getMyProfile(
            @AuthenticationPrincipal UserDetails principal) {

        var data = userService.getMyProfile(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    // ── PUT /me ───────────────────────────────────────────────────────────────

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserDto.ProfileResponse>> updateMyProfile(
            @Valid @RequestBody UserDto.UpdateRequest req,
            @AuthenticationPrincipal UserDetails principal) {

        var data = userService.updateMyProfile(principal.getUsername(), req);
        return ResponseEntity.ok(ApiResponse.ok(data, msg.get("success.user.updated")));
    }

    // ── GET / — danh sách, phân trang ─────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAuthority('USER:READ')")
    public ResponseEntity<ApiResponse<PageResponse<UserDto.SummaryResponse>>> search(
            @Valid UserDto.SearchRequest req) {

        var data = userService.search(req);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    // ── GET /{id} ─────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER:READ')")
    public ResponseEntity<ApiResponse<UserDto.DetailResponse>> getById(
            @PathVariable UUID id) {

        var data = userService.getById(id);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    // ── POST / — admin tạo user ───────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAuthority('USER:WRITE')")
    public ResponseEntity<ApiResponse<UserDto.DetailResponse>> create(
            @Valid @RequestBody UserDto.AdminCreateRequest req) {

        var data = userService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(data, msg.get("success.user.created")));
    }

    // ── PUT /{id} ─────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER:WRITE')")
    public ResponseEntity<ApiResponse<UserDto.DetailResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UserDto.UpdateRequest req) {

        var data = userService.update(id, req);
        return ResponseEntity.ok(ApiResponse.ok(data, msg.get("success.user.updated")));
    }

    // ── PATCH /{id}/status ────────────────────────────────────────────────────

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('USER:WRITE')")
    public ResponseEntity<ApiResponse<UserDto.DetailResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UserDto.UpdateStatusRequest req) {

        var data = userService.updateStatus(id, req);
        return ResponseEntity.ok(ApiResponse.ok(data, msg.get("success.user.status.updated")));
    }

    // ── DELETE /{id} ──────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER:DELETE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.ok(ApiResponse.noContent(msg.get("success.user.deleted")));
    }

    // ── POST /{id}/roles ──────────────────────────────────────────────────────

    @PostMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('ROLE:WRITE')")
    public ResponseEntity<ApiResponse<UserDto.DetailResponse>> assignRole(
            @PathVariable UUID id,
            @Valid @RequestBody UserDto.AssignRoleRequest req) {

        var data = userService.assignRole(id, req.getRoleId());
        return ResponseEntity.ok(ApiResponse.ok(data, msg.get("success.user.role.assigned")));
    }

    // ── DELETE /{id}/roles/{rid} ──────────────────────────────────────────────

    @DeleteMapping("/{id}/roles/{rid}")
    @PreAuthorize("hasAuthority('ROLE:WRITE')")
    public ResponseEntity<ApiResponse<UserDto.DetailResponse>> removeRole(
            @PathVariable UUID id,
            @PathVariable UUID rid) {

        var data = userService.removeRole(id, rid);
        return ResponseEntity.ok(ApiResponse.ok(data, msg.get("success.user.role.removed")));
    }
}
