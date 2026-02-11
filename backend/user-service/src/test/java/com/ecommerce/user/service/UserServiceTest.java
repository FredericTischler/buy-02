package com.ecommerce.user.service;

import com.ecommerce.user.dto.LoginRequest;
import com.ecommerce.user.dto.RegisterRequest;
import com.ecommerce.user.model.Role;
import com.ecommerce.user.model.User;
import com.ecommerce.user.repository.UserRepository;
import com.ecommerce.user.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @TempDir
    Path tempDir;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, jwtUtil);
        ReflectionTestUtils.setField(userService, "avatarDir", tempDir.toString());
    }

    @Test
    void register_shouldEncodePasswordAndPersistUser() {
        RegisterRequest request = new RegisterRequest("Alice", "alice@mail.com", "password123", Role.CLIENT);

        when(userRepository.existsByEmail("alice@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");

        String message = userService.register(request);

        assertThat(message).contains("succès");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("hashed");
    }

    @Test
    void register_shouldThrowWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("Alice", "alice@mail.com", "password123", Role.CLIENT);
        when(userRepository.existsByEmail("alice@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("déjà utilisé");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_shouldReturnTokenWhenCredentialsAreValid() {
        User user = new User();
        user.setId("user-1");
        user.setEmail("alice@mail.com");
        user.setName("Alice");
        user.setPassword("hashed");
        user.setRole(Role.CLIENT);

        when(userRepository.findByEmail("alice@mail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtUtil.generateToken("user-1", "alice@mail.com", "CLIENT", "Alice")).thenReturn("jwt-token");

        LoginRequest request = new LoginRequest("alice@mail.com", "password123");

        var response = userService.login(request);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUserId()).isEqualTo("user-1");
        verify(jwtUtil).generateToken("user-1", "alice@mail.com", "CLIENT", "Alice");
    }

    @Test
    void login_shouldFailWhenUserNotFound() {
        when(userRepository.findByEmail("ghost@mail.com")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest("ghost@mail.com", "password");

        assertThatThrownBy(() -> userService.login(request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Email ou mot de passe invalide");
    }

    @Test
    void login_shouldFailWhenPasswordInvalid() {
        User user = new User();
        user.setEmail("alice@mail.com");
        user.setPassword("hashed");

        when(userRepository.findByEmail("alice@mail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad", "hashed")).thenReturn(false);

        LoginRequest request = new LoginRequest("alice@mail.com", "bad");

        assertThatThrownBy(() -> userService.login(request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Email ou mot de passe invalide");
    }

    @Test
    void getProfile_shouldReturnUserResponse() {
        User user = new User();
        user.setId("user-1");
        user.setName("Alice");
        user.setEmail("alice@mail.com");
        user.setRole(Role.CLIENT);

        when(userRepository.findByEmail("alice@mail.com")).thenReturn(Optional.of(user));

        var response = userService.getProfile("alice@mail.com");

        assertThat(response.getId()).isEqualTo("user-1");
        assertThat(response.getName()).isEqualTo("Alice");
        assertThat(response.getRole()).isEqualTo(Role.CLIENT);
    }

    @Test
    void getProfile_shouldThrowWhenUserMissing() {
        when(userRepository.findByEmail("ghost@mail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile("ghost@mail.com"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Utilisateur non trouvé");
    }

    @Test
    void updateProfile_shouldUpdateFields() {
        User user = new User();
        user.setEmail("alice@mail.com");
        user.setName("Old");
        user.setAvatar("/old.png");

        when(userRepository.findByEmail("alice@mail.com")).thenReturn(Optional.of(user));

        var response = userService.updateProfile("alice@mail.com", "NewName", "/new.png");

        assertThat(response.getName()).isEqualTo("NewName");
        assertThat(response.getAvatar()).isEqualTo("/new.png");
        verify(userRepository).save(user);
    }

    @Test
    void emailExists_shouldDelegateToRepository() {
        when(userRepository.existsByEmail("alice@mail.com")).thenReturn(true);

        assertThat(userService.emailExists("alice@mail.com")).isTrue();
        verify(userRepository).existsByEmail("alice@mail.com");
    }

    @Test
    void uploadAvatar_shouldStoreFileAndUpdateUser() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(2048L);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getOriginalFilename()).thenReturn("avatar.png");
        InputStream inputStream = new ByteArrayInputStream("image".getBytes());
        when(file.getInputStream()).thenReturn(inputStream);

        User user = new User();
        user.setId("user-1");
        user.setEmail("alice@mail.com");

        when(userRepository.findByEmail("alice@mail.com")).thenReturn(Optional.of(user));

        String url = userService.uploadAvatar("alice@mail.com", file);

        assertThat(url).startsWith("/uploads/avatars/");
        verify(userRepository).save(user);
        try (var files = Files.list(tempDir)) {
            assertThat(files.count()).isEqualTo(1);
        }
    }

    @Test
    void uploadAvatar_shouldRejectEmptyFile() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        assertThatThrownBy(() -> userService.uploadAvatar("alice@mail.com", file))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("vide");
    }

    @Test
    void uploadAvatar_shouldRejectOversizedFile() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(6 * 1024 * 1024L);

        assertThatThrownBy(() -> userService.uploadAvatar("alice@mail.com", file))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("dépasser 5MB");
    }

    @Test
    void uploadAvatar_shouldRejectInvalidContentType() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("text/plain");

        assertThatThrownBy(() -> userService.uploadAvatar("alice@mail.com", file))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("image");
    }

    @Test
    void uploadAvatar_shouldThrowWhenUserNotFound() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("image/png");
        when(userRepository.findByEmail("ghost@mail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.uploadAvatar("ghost@mail.com", file))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Utilisateur non trouvé");
    }

    @Test
    void uploadAvatar_shouldWrapIoExceptions() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getOriginalFilename()).thenReturn("avatar.png");
        when(file.getInputStream()).thenThrow(new IOException("disk full"));

        User user = new User();
        user.setId("user-1");
        user.setEmail("alice@mail.com");
        when(userRepository.findByEmail("alice@mail.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.uploadAvatar("alice@mail.com", file))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Erreur lors de l'upload");
    }

    @Test
    void getUserById_shouldDelegateToRepository() {
        User user = new User();
        when(userRepository.findById("id")).thenReturn(Optional.of(user));

        Optional<User> result = userService.getUserById("id");

        assertThat(result).contains(user);
        verify(userRepository).findById("id");
    }

    // ==================== WISHLIST TESTS ====================

    @Test
    void getWishlist_shouldReturnEmptyListWhenWishlistIsNull() {
        User user = new User();
        user.setEmail("alice@mail.com");
        user.setWishlist(null);
        when(userRepository.findByEmail("alice@mail.com")).thenReturn(Optional.of(user));

        var result = userService.getWishlist("alice@mail.com");

        assertThat(result).isEmpty();
    }

    @Test
    void getWishlist_shouldReturnWishlistItems() {
        User user = new User();
        user.setEmail("alice@mail.com");
        user.setWishlist(java.util.Arrays.asList("product-1", "product-2"));
        when(userRepository.findByEmail("alice@mail.com")).thenReturn(Optional.of(user));

        var result = userService.getWishlist("alice@mail.com");

        assertThat(result).containsExactly("product-1", "product-2");
    }

    @Test
    void getWishlist_shouldThrowWhenUserNotFound() {
        when(userRepository.findByEmail("ghost@mail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getWishlist("ghost@mail.com"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Utilisateur non trouvé");
    }

    @Test
    void addToWishlist_shouldAddProductToWishlist() {
        User user = new User();
        user.setEmail("alice@mail.com");
        user.setWishlist(new java.util.ArrayList<>());
        when(userRepository.findByEmail("alice@mail.com")).thenReturn(Optional.of(user));

        var result = userService.addToWishlist("alice@mail.com", "product-1");

        assertThat(result).contains("product-1");
        verify(userRepository).save(user);
    }

    @Test
    void addToWishlist_shouldNotAddDuplicateProduct() {
        User user = new User();
        user.setEmail("alice@mail.com");
        user.setWishlist(new java.util.ArrayList<>(java.util.Arrays.asList("product-1")));
        when(userRepository.findByEmail("alice@mail.com")).thenReturn(Optional.of(user));

        var result = userService.addToWishlist("alice@mail.com", "product-1");

        assertThat(result).hasSize(1)
            .containsExactly("product-1");
    }

    @Test
    void addToWishlist_shouldInitializeWishlistIfNull() {
        User user = new User();
        user.setEmail("alice@mail.com");
        user.setWishlist(null);
        when(userRepository.findByEmail("alice@mail.com")).thenReturn(Optional.of(user));

        var result = userService.addToWishlist("alice@mail.com", "product-1");

        assertThat(result).contains("product-1");
        verify(userRepository).save(user);
    }

    @Test
    void removeFromWishlist_shouldRemoveProductFromWishlist() {
        User user = new User();
        user.setEmail("alice@mail.com");
        user.setWishlist(new java.util.ArrayList<>(java.util.Arrays.asList("product-1", "product-2")));
        when(userRepository.findByEmail("alice@mail.com")).thenReturn(Optional.of(user));

        var result = userService.removeFromWishlist("alice@mail.com", "product-1");

        assertThat(result).containsExactly("product-2");
        verify(userRepository).save(user);
    }

    @Test
    void removeFromWishlist_shouldHandleNullWishlist() {
        User user = new User();
        user.setEmail("alice@mail.com");
        user.setWishlist(null);
        when(userRepository.findByEmail("alice@mail.com")).thenReturn(Optional.of(user));

        var result = userService.removeFromWishlist("alice@mail.com", "product-1");

        assertThat(result).isEmpty();
    }

    @Test
    void isInWishlist_shouldReturnTrueWhenProductExists() {
        User user = new User();
        user.setEmail("alice@mail.com");
        user.setWishlist(java.util.Arrays.asList("product-1", "product-2"));
        when(userRepository.findByEmail("alice@mail.com")).thenReturn(Optional.of(user));

        boolean result = userService.isInWishlist("alice@mail.com", "product-1");

        assertThat(result).isTrue();
    }

    @Test
    void isInWishlist_shouldReturnFalseWhenProductNotExists() {
        User user = new User();
        user.setEmail("alice@mail.com");
        user.setWishlist(java.util.Arrays.asList("product-1"));
        when(userRepository.findByEmail("alice@mail.com")).thenReturn(Optional.of(user));

        boolean result = userService.isInWishlist("alice@mail.com", "product-999");

        assertThat(result).isFalse();
    }

    @Test
    void isInWishlist_shouldReturnFalseWhenWishlistIsNull() {
        User user = new User();
        user.setEmail("alice@mail.com");
        user.setWishlist(null);
        when(userRepository.findByEmail("alice@mail.com")).thenReturn(Optional.of(user));

        boolean result = userService.isInWishlist("alice@mail.com", "product-1");

        assertThat(result).isFalse();
    }

    @Test
    void clearWishlist_shouldEmptyWishlist() {
        User user = new User();
        user.setEmail("alice@mail.com");
        user.setWishlist(new java.util.ArrayList<>(java.util.Arrays.asList("product-1", "product-2")));
        when(userRepository.findByEmail("alice@mail.com")).thenReturn(Optional.of(user));

        userService.clearWishlist("alice@mail.com");

        assertThat(user.getWishlist()).isEmpty();
        verify(userRepository).save(user);
    }

    @Test
    void clearWishlist_shouldThrowWhenUserNotFound() {
        when(userRepository.findByEmail("ghost@mail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.clearWishlist("ghost@mail.com"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Utilisateur non trouvé");
    }
}
