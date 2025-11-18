
package com.cloudproject.community_backend.service;

import com.cloudproject.community_backend.dto.LoginRequest;
import com.cloudproject.community_backend.entity.User;
import com.cloudproject.community_backend.repository.UserRepository;
import com.cloudproject.community_backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public String login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "존재하지 않는 이메일입니다."));

        boolean matched = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if (!matched) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다.");
        }
        return jwtUtil.generateToken(user.getEmail(), user.getId());

            }
}
