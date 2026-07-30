package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.auth.LoginRequest;
import br.com.sergio.gestaopedidos.dto.auth.LoginResponse;
import br.com.sergio.gestaopedidos.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {

        var authenticationToken =
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.senha()
                );

        var authentication =
                authenticationManager.authenticate(authenticationToken);

        UserDetails usuarioAutenticado =
                (UserDetails) authentication.getPrincipal();

        String token = jwtService.gerarToken(usuarioAutenticado);

        return new LoginResponse(
                token,
                "Bearer"
        );
    }
}