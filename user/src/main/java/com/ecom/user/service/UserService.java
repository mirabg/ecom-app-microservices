package com.ecom.user.service;

import com.ecom.user.client.KeycloakAdminClient;
import com.ecom.user.dto.AddressDTO;
import com.ecom.user.model.User;
import com.ecom.user.dto.UserRequest;
import com.ecom.user.dto.UserResponse;
import com.ecom.user.model.Address;
import com.ecom.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final KeycloakAdminClient keycloakAdminClient;

    public UserService(UserRepository userRepository, KeycloakAdminClient keycloakAdminClient) {
        this.userRepository = userRepository;
        this.keycloakAdminClient = keycloakAdminClient;
    }

    public List<UserResponse> fetchAllUsers(){
        return userRepository.findAll().stream().map(this::mapToUserResponse).toList();
    }

    public Optional<UserResponse> fetchUserById(String id){
        return userRepository.findById(id).map(this::mapToUserResponse);
    }

    /**
     * Provisions the user in Keycloak first (so we have an identity/"sub" to
     * key off of), then creates the local profile with keycloakId populated.
     * If the local save fails, the Keycloak user is deleted so the two stores
     * don't drift out of sync.
     */
    @Transactional
    public UserResponse createUser(UserRequest userRequest){
        String keycloakId = keycloakAdminClient.createUser(userRequest);
        try {
            User user = mapUserRequestToUser(userRequest);
            user.setKeycloakId(keycloakId);
            User saved = userRepository.save(user);
            return mapToUserResponse(saved);
        } catch (RuntimeException ex) {
            log.error("Local persistence failed after creating Keycloak user {} - rolling back", keycloakId, ex);
            keycloakAdminClient.deleteUser(keycloakId);
            throw ex;
        }
    }

    @Transactional
    public boolean updateUser(String id, UserRequest userRequest){
        return userRepository.findById(id)
                .map(existingUser -> {
                    existingUser.setFirstName(userRequest.getFirstName());
                    existingUser.setLastName(userRequest.getLastName());
                    existingUser.setEmail(userRequest.getEmail());
                    existingUser.setPhone(userRequest.getPhone());

                    if (userRequest.getAddress() == null) {
                        existingUser.setAddress(null);
                    } else {
                        Address address = existingUser.getAddress();
                        if (address == null) {
                            address = new Address();
                            existingUser.setAddress(address);
                        }
                        address.setStreet(userRequest.getAddress().getStreet());
                        address.setCity(userRequest.getAddress().getCity());
                        address.setState(userRequest.getAddress().getState());
                        address.setZip(userRequest.getAddress().getZip());
                        address.setCountry(userRequest.getAddress().getCountry());
                    }

                    userRepository.save(existingUser);
                    return true;
                })
                .orElse(false);
    }

    private  UserResponse mapToUserResponse(User user){
        UserResponse userResponse = new UserResponse();
        userResponse.setId(user.getId());
        userResponse.setKeycloakId(user.getKeycloakId());
        userResponse.setFirstName(user.getFirstName());
        userResponse.setLastName(user.getLastName());
        userResponse.setEmail(user.getEmail());
        userResponse.setPhone(user.getPhone());
        userResponse.setRole(user.getRole());
        if (user.getAddress() != null) {
            AddressDTO addressDTO = new AddressDTO();
            addressDTO.setStreet(user.getAddress().getStreet());
            addressDTO.setCity(user.getAddress().getCity());
            addressDTO.setState(user.getAddress().getState());
            addressDTO.setZip(user.getAddress().getZip());
            addressDTO.setCountry(user.getAddress().getCountry());
            userResponse.setAddress(addressDTO);
        }
        return userResponse;
    }

    private User mapUserRequestToUser(UserRequest userRequest){
        User user = new User();
        user.setFirstName(userRequest.getFirstName());
        user.setLastName(userRequest.getLastName());
        user.setEmail(userRequest.getEmail());
        user.setPhone(userRequest.getPhone());
        if (userRequest.getAddress() != null) {
            Address address = new Address();
            address.setStreet(userRequest.getAddress().getStreet());
            address.setCity(userRequest.getAddress().getCity());
            address.setState(userRequest.getAddress().getState());
            address.setZip(userRequest.getAddress().getZip());
            address.setCountry(userRequest.getAddress().getCountry());
            user.setAddress(address);
        }
        return user;
    }
}
