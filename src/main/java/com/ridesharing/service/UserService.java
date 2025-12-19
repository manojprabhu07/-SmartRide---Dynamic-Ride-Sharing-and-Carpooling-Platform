package com.ridesharing.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ridesharing.dto.UserProfileDto;
import com.ridesharing.entity.User;
import com.ridesharing.entity.UserRole;
import com.ridesharing.exception.UserNotFoundException;
import com.ridesharing.repository.UserRepository;
import com.ridesharing.security.UserPrincipal;

import java.util.List;

@Service
@Transactional
public class UserService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String phoneNumber) throws UsernameNotFoundException {
        User user = userRepository.findByPhoneNumberAndIsActiveTrue(phoneNumber)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with phone number: " + phoneNumber));

        return UserPrincipal.create(user);
    }

    public User createUser(String firstName, String lastName, String phoneNumber, String email, String password, UserRole role) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhoneNumber(phoneNumber);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setIsVerified(false);
        user.setIsActive(true);

        return userRepository.save(user);
    }

    public User getUserByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new UserNotFoundException("phoneNumber", phoneNumber));
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    public UserProfileDto getUserProfile(String phoneNumber) {
        User user = getUserByPhoneNumber(phoneNumber);
        return new UserProfileDto(user.getFirstName(), user.getLastName(), user.getEmail(), user.getProfilePicture());
    }

    public UserProfileDto updateUserProfile(String phoneNumber, UserProfileDto profileDto) {
        User user = getUserByPhoneNumber(phoneNumber);
        
        if (profileDto.getFirstName() != null) {
            user.setFirstName(profileDto.getFirstName());
        }
        if (profileDto.getLastName() != null) {
            user.setLastName(profileDto.getLastName());
        }
        if (profileDto.getEmail() != null) {
            user.setEmail(profileDto.getEmail());
        }
        if (profileDto.getProfilePicture() != null) {
            user.setProfilePicture(profileDto.getProfilePicture());
        }

        User updatedUser = userRepository.save(user);
        return new UserProfileDto(updatedUser.getFirstName(), updatedUser.getLastName(), 
                                 updatedUser.getEmail(), updatedUser.getProfilePicture());
    }

    public void deleteUser(Long userId) {
        User user = getUserById(userId);
        user.setIsActive(false);
        userRepository.save(user);
        logger.info("User with ID {} marked as inactive (soft deleted)", userId);
    }

    public List<User> getAllUsers() {
        // Only return active users for admin dashboard
        return userRepository.findByIsActiveTrue();
    }

    public boolean existsByPhoneNumber(String phoneNumber) {
        return userRepository.existsByPhoneNumber(phoneNumber);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public void verifyUser(String phoneNumber) {
        User user = getUserByPhoneNumber(phoneNumber);
        user.setIsVerified(true);
        userRepository.save(user);
    }
}