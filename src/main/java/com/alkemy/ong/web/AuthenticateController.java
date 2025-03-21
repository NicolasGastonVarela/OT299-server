package com.alkemy.ong.web;

import com.alkemy.ong.domain.users.User;
import com.alkemy.ong.domain.users.UserService;
import com.alkemy.ong.web.security.JwtUtil;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/auth")
@AllArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AuthenticateController{
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder encoder;
    private final UserService service;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = {@Content(
                    mediaType = "application/json", examples = {@ExampleObject(name= "errors",
                    value = "{errors: [Invalid Input]}")})}),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = {@Content(
                    mediaType = "application/json", examples = {@ExampleObject(name= "errors",
                    value = "Las credenciales ingresadas no son validas o no está autorizado para realizar esta operación")})}),
            @ApiResponse(responseCode = "404", description = "Not Found", content = {@Content(
                    mediaType = "application/json", examples = {@ExampleObject(name= "errors",
                    value = "error: User not found with: id :")})}),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = {@Content(
                    mediaType = "application/json", examples = {@ExampleObject(name= "errors",
                    value = "{error: [Internal Server Error]}")})})
    })
    public ResponseEntity<UserAuthResponseDTO> authenticate(@Valid @RequestBody UserAuthDTO userAuthDTO) throws Exception {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(userAuthDTO.email, userAuthDTO.password));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserRegisteredDTO user = toDTO(service.findByEmail(userAuthDTO.getEmail()));
        UserDetails userDetails = service.loadUserByUsername(user.email);
        String jwtToken = jwtUtil.generateToken(userDetails);

        return new ResponseEntity<>(new UserAuthResponseDTO(jwtToken), HttpStatus.OK);
    }

    @PostMapping("/register")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User Created"),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = {@Content(
                    mediaType = "application/json", examples = {@ExampleObject(name= "errors",
                    value = "{errors: [Invalid Input]}")})}),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = {@Content(
                    mediaType = "application/json", examples = {@ExampleObject(name= "errors",
                    value = "{error: [Internal Server Error]}")})})
    })
    public ResponseEntity<UserRegisteredDTO> register(@Valid @RequestBody UserToRegisterDTO userToRegisterDTO) {
        String passwordEncrypted = encoder.encode(userToRegisterDTO.getPassword());
        userToRegisterDTO.setPassword(passwordEncrypted);
        UserRegisteredDTO userRegisteredDTO = toDTO(service.save(toModel(userToRegisterDTO)));
        return new ResponseEntity<>(userRegisteredDTO, HttpStatus.CREATED);
    }

    @GetMapping("/me")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = {@Content(
                    mediaType = "application/json", examples = {@ExampleObject(name= "errors",
                    value = "{errors: [Invalid Input]}")})}),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = {@Content(
                    mediaType = "application/json", examples = {@ExampleObject(name= "errors",
                    value = "Las credenciales ingresadas no son validas o no está autorizado para realizar esta operación")})}),
            @ApiResponse(responseCode = "404", description = "Not Found", content = {@Content(
                    mediaType = "application/json", examples = {@ExampleObject(name= "errors",
                    value = "error: User not found with: id :")})}),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = {@Content(
                    mediaType = "application/json", examples = {@ExampleObject(name= "errors",
                    value = "{error: [Internal Server Error]}")})})
    })
    public ResponseEntity<UserRegisteredDTO> getAuthUser(@RequestHeader(HttpHeaders.AUTHORIZATION) String token){
        String email = jwtUtil.getUsernameFromToken(token);
        UserRegisteredDTO userAuthenticated = toDTO(service.findByEmail(email));
        return new ResponseEntity<>(userAuthenticated, HttpStatus.OK);
    }


    private UserRegisteredDTO toDTO(User user) {
        return UserRegisteredDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .roleId(user.getRoleId())
                .build();
    }

    private User toModel(UserToRegisterDTO userToRegisterDTO) {
        return User.builder()
                .firstName(userToRegisterDTO.getFirstName())
                .lastName(userToRegisterDTO.getLastName())
                .email(userToRegisterDTO.getEmail())
                .password(userToRegisterDTO.getPassword())
                .roleId(userToRegisterDTO.getRoleId())
                .build();
    }

    // ------------ DTOs

    @Setter
    @Getter
    @Builder
    public static class UserAuthDTO {
        @NotNull
        private String email;
        @NotNull
        private String password;
    }

    @Getter
    @Setter
    @Builder
    public static class UserAuthResponseDTO {
        private String jwt;
    }

    @Setter
    @Getter
    @Builder
    public static class UserRegisteredDTO {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
        private Long roleId;
    }

    @Setter
    @Getter
    @Builder
    public static class UserToRegisterDTO {
        @NotNull
        private String firstName;
        @NotNull
        private String lastName;
        @NotNull
        private String email;
        @NotNull
        private String password;
        @NotNull
        private Long roleId;
    }
}
