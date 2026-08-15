package com.example.backend.auth.api;

import com.example.backend.auth.audit.SecurityAuditService;
import com.example.backend.auth.dto.AuthDtos;
import com.example.backend.auth.model.ClientInfo;
import com.example.backend.auth.service.AuthService;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthService authService;
    private final SecurityAuditService securityAuditService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid AuthDtos.RegisterReq req, HttpServletRequest request) {
        if (!verifyStudent(req.getStudentId())) {
            securityAuditService.logSuspiciousActivity("REGISTRATION_FAILURE", "Student id validation failed: " + req.getStudentId(), request);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid student id");
        }
        authService.register(req);
        securityAuditService.logRegistration(req.getUsername(), request);
        return ResponseEntity.ok(Map.of("code", 0, "message", "OK"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid AuthDtos.LoginReq req, HttpServletRequest request) {
        try {
            Map<String, Object> data = authService.login(req, resolveClient(request));
            securityAuditService.logLoginSuccess(req.getIdentifier(), request);
            return ResponseEntity.ok(Map.of("code", 0, "message", "OK", "data", data));
        } catch (RuntimeException e) {
            securityAuditService.logLoginFailure(req.getIdentifier(), request, e.getMessage());
            throw e;
        }
    }

    @PostMapping("/code")
    public ResponseEntity<?> sendCode(@RequestBody @Valid AuthDtos.SendCodeReq req) {
        Map<String, Object> data = authService.sendCode(req);
        return ResponseEntity.ok(Map.of("code", 0, "message", "OK", "data", data));
    }

    @PostMapping("/password/code")
    public ResponseEntity<?> sendResetPasswordCode(@RequestBody @Valid AuthDtos.SendCodeReq req) {
        Map<String, Object> data = authService.sendResetPasswordCode(req);
        return ResponseEntity.ok(Map.of("code", 0, "message", "OK", "data", data));
    }

    @PostMapping("/password/reset")
    public ResponseEntity<?> resetPassword(@RequestBody @Valid AuthDtos.ResetPasswordReq req) {
        authService.resetPassword(req);
        return ResponseEntity.ok(Map.of("code", 0, "message", "OK"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody @Valid AuthDtos.TokenRefreshReq req) {
        Map<String, Object> data = authService.refresh(req);
        return ResponseEntity.ok(Map.of("code", 0, "message", "OK", "data", data));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody @Valid AuthDtos.LogoutReq req) {
        authService.logout(req.getRefreshToken());
        return ResponseEntity.ok(Map.of("code", 0, "message", "OK"));
    }

    @GetMapping("/check-username")
    public ResponseEntity<?> checkUsername(@RequestParam String username) {
        boolean available = authService.isUsernameAvailable(username);
        return ResponseEntity.ok(Map.of("code", 0, "message", "OK", "data", Map.of("available", available)));
    }

    @PostMapping("/hic/verify")
    public ResponseEntity<?> hicVerify(Authentication authentication, @RequestBody @Valid AuthDtos.HicVerifyReq req) {
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (authService.verifyHic(authentication.getName(), req.getKey())) {
            return ResponseEntity.ok(Map.of("code", 0, "message", "OK", "data", Map.of("hic", 1)));
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid auth key");
    }


    /**
     * Extracts client IP and device information for login audit records.
     */
    private ClientInfo resolveClient(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        String ip = forwardedFor != null && !forwardedFor.isBlank()
                ? forwardedFor.split(",")[0].trim()
                : request.getRemoteAddr();
        return new ClientInfo(ip, request.getHeader("User-Agent"));
    }
    private static final int MIN_YEAR = 17;
    private static final Set<String> VALID_COLLEGE_CODES = new HashSet<>();

    static {
        VALID_COLLEGE_CODES.add("01");
        VALID_COLLEGE_CODES.add("02");
        VALID_COLLEGE_CODES.add("03");
        VALID_COLLEGE_CODES.add("04");
        VALID_COLLEGE_CODES.add("05");
        VALID_COLLEGE_CODES.add("06");
        VALID_COLLEGE_CODES.add("07");
        VALID_COLLEGE_CODES.add("08");
        VALID_COLLEGE_CODES.add("09");
        VALID_COLLEGE_CODES.add("10");
        VALID_COLLEGE_CODES.add("11");
        VALID_COLLEGE_CODES.add("12");
        VALID_COLLEGE_CODES.add("13");
        VALID_COLLEGE_CODES.add("14");
        VALID_COLLEGE_CODES.add("15");
        VALID_COLLEGE_CODES.add("16");
        VALID_COLLEGE_CODES.add("17");
        VALID_COLLEGE_CODES.add("18");
        VALID_COLLEGE_CODES.add("19");
        VALID_COLLEGE_CODES.add("20");
    }

    /**
     * Keeps the original school-number gate before account creation.
     */
    public boolean verifyStudent(String studentId) {
        if (studentId == null) return false;
        if (studentId.length() != 11 || !studentId.matches("\\d{11}")) return false;
        try {
            int year = Integer.parseInt(studentId.substring(0, 2));
            int currentYear = LocalDate.now().getYear() % 100;
            return year >= MIN_YEAR && year <= currentYear + 1 && VALID_COLLEGE_CODES.contains(studentId.substring(2, 4));
        } catch (Exception e) {
            return false;
        }
    }
}