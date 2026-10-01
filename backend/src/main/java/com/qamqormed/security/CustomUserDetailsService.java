package com.qamqormed.security;

import com.qamqormed.entity.User;
import com.qamqormed.repository.UserRepository;
import com.qamqormed.util.PhoneNormalizer;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        final String phone;
        try {
            phone = PhoneNormalizer.normalize(username);
        } catch (IllegalArgumentException exception) {
            throw new UsernameNotFoundException("User not found");
        }

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new AuthenticatedUser(user.getId(), user.getPhone(), user.getPassword(), user.getRole(), user.isEnabled());
    }
}
