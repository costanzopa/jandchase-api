package com.jandprocu.jandchase.api.usersms;

import com.jandprocu.jandchase.api.usersms.rest.request.*;
import com.jandprocu.jandchase.api.usersms.rest.response.*;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UsersMsApplicationTests {

    @Autowired
    TestRestTemplate restTemplate;

    @LocalServerPort
    int randomServerPort;

    @Value("${local.server.url}")
    String localHost;

    private UserCreateRequest createRequest;
    private UserUpdateRequest updateRequest;
    private MultiValueMap<String, String> headers;


    @Before
    public void setUp() {
        createRequest = new UserCreateRequest();
        createRequest.setUserName("THIRD_USER");
        createRequest.setFirstName("ThirdUser");
        createRequest.setLastName("ThirdUser");
        createRequest.setEmail("third_user@email.test");
        createRequest.setPassword("12345678");

        updateRequest = new UserUpdateRequest();
        updateRequest.setUserName("FIRST_USER");
        updateRequest.setFirstName("FirstUser");
        updateRequest.setLastName("FirstUser");
        updateRequest.setEmail("first_update_user@email.test");
        updateRequest.setEnable(Boolean.FALSE);

        headers = new LinkedMultiValueMap<>();
        headers.add("Content-Type", "application/json");
    }

    @Test
    public void createUser_OK_ReturnsUserDetails() {
        //arrange
        HttpEntity<UserCreateRequest> request = new HttpEntity<>(createRequest);

        //act
        ResponseEntity<UserCreateResponse> response = restTemplate.postForEntity(
                localHost + randomServerPort +
                        "/", request, UserCreateResponse.class);

        //assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getUserName()).isEqualTo("THIRD_USER");
        assertThat(response.getBody().getFirstName()).isEqualTo("ThirdUser");
        assertThat(response.getBody().getLastName()).isEqualTo("ThirdUser");
        assertThat(response.getBody().getEmail()).isEqualTo("third_user@email.test");
        assertThat(response.getBody().getRoles().isEmpty()).isFalse();
    }

    @Test
    public void getUserByUserId_OK_ReturnsUserDetails() {
        //act
        ResponseEntity<UserGetResponse> entity = restTemplate.exchange(
                localHost + randomServerPort + "/FIRST_USER_ID",
                HttpMethod.GET, new HttpEntity<>(headers),
                UserGetResponse.class);

        //assert
        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getBody().getFirstName()).isEqualTo("FirstUser");
        assertThat(entity.getBody().getLastName()).isEqualTo("FirstUser");
        assertThat(entity.getBody().getEmail()).isEqualTo("first_user@email.test");
        assertThat(entity.getBody().getEnable()).isTrue();
        assertThat(entity.getBody().getRoles().get(0).getName()).isEqualTo("ROLE_USER");
    }

    @Test
    public void getUserByUserName_OK_ReturnsUserDetailsForOAuth() {
        //act
        ResponseEntity<UserGetOAuthResponse> entity = restTemplate.exchange(
                localHost + randomServerPort + "/getByUserName/SECOND_USER",
                HttpMethod.GET, new HttpEntity<>(headers),
                UserGetOAuthResponse.class);

        //assert
        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getBody().getFirstName()).isEqualTo("SecondUser");
        assertThat(entity.getBody().getLastName()).isEqualTo("SecondUser");
        assertThat(entity.getBody().getEmail()).isEqualTo("second_user@email.test");
        assertThat(entity.getBody().getEnable()).isTrue();
        assertThat(entity.getBody().getRoles().get(0).getName()).isEqualTo("ROLE_USER");
        assertThat(entity.getBody().getPassword()).isEqualTo("12345678");
    }


    @Test
    public void updateUserByUserId_OK_ReturnsUserUpdateDetails() {
        //arrange
        HttpEntity<UserRequest> request = new HttpEntity<>(updateRequest, headers);

        //act
        ResponseEntity<UserGetResponse> entity = restTemplate.exchange(
                localHost + randomServerPort + "/FIRST_USER_ID",
                HttpMethod.PUT, request, UserGetResponse.class);

        //assert
        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getBody().getFirstName()).isEqualTo("FirstUser");
        assertThat(entity.getBody().getLastName()).isEqualTo("FirstUser");
        assertThat(entity.getBody().getEmail()).isEqualTo("first_update_user@email.test");
        assertThat(entity.getBody().getEnable()).isFalse();
    }

    @Test
    public void deleteUserByUserId_OK() {
        //act
        restTemplate.exchange(localHost + randomServerPort + "/FIFTH_USER_ID",
                HttpMethod.DELETE, new HttpEntity<>(headers), String.class)
                .getStatusCode().equals(HttpStatus.OK);
    }


    @Test
    public void addRoleToUser_OK() {
        //arrange
        List<String> roles = Arrays.asList("ROLE_ADMIN");
        HttpEntity<List<String>> request = new HttpEntity<>(roles, headers);

        //act
        ResponseEntity<UserGetResponse> entity = restTemplate.exchange(
                localHost + randomServerPort + "/FIRST_USER_ID/addRoles",
                HttpMethod.POST, request, UserGetResponse.class);

        //assert
        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getBody().getFirstName()).isEqualTo("FirstUser");
        assertThat(entity.getBody().getLastName()).isEqualTo("FirstUser");
        assertThat(entity.getBody().getRoles().size()).isEqualTo(2);
    }

    @Test
    public void removeRoleToUser_OK() {
        //arrange
        List<String> roles = Arrays.asList("ROLE_USER");
        HttpEntity<List<String>> request = new HttpEntity<>(roles, headers);

        //act
        ResponseEntity<UserGetResponse> entity = restTemplate.exchange(
                localHost + randomServerPort + "/SECOND_USER_ID/removeRoles",
                HttpMethod.DELETE, request, UserGetResponse.class);

        //assert
        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getBody().getRoles().size()).isEqualTo(1);
    }


    @Test
    public void createRole_OK_ReturnsRoleDetails() {
        //arrange
        RoleRequest roleRequest = new RoleRequest();
        roleRequest.setName("ROLE_DEVELOPER");
        roleRequest.setDescription("Role for developers.");

        HttpEntity<RoleRequest> request = new HttpEntity<>(roleRequest, headers);

        //act
        ResponseEntity<RoleResponse> entity = restTemplate.exchange(
                localHost + randomServerPort + "/roles/",
                HttpMethod.POST, request, RoleResponse.class);


        //assert
        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(entity.getBody().getName()).isEqualTo("ROLE_DEVELOPER");
        assertThat(entity.getBody().getDescription()).isEqualTo("Role for developers.");

    }

    @Test
    public void getRole_OK_returnsRoleDetails() {
        //act
        ResponseEntity<RoleResponse> entity = restTemplate.exchange(
                localHost + randomServerPort + "/roles/ROLE_ADMIN",
                HttpMethod.GET, new HttpEntity<>(headers),
                RoleResponse.class);

        //assert
        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getBody().getName()).isEqualTo("ROLE_ADMIN");
        assertThat(entity.getBody().getDescription()).isEqualTo("Role for common admin users.");
    }


    @Test
    public void updateRole_OK_ReturnsRoleDetails() {
        //arrange
        String description = "Role for developers Updated.";

        HttpEntity<String> request = new HttpEntity<>(description, headers);

        //act
        ResponseEntity<RoleResponse> entity = restTemplate.exchange(
                localHost + randomServerPort + "/roles/ROLE_DEVELOPER",
                HttpMethod.PUT, request, RoleResponse.class);


        //assert
        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getBody().getName()).isEqualTo("ROLE_DEVELOPER");
        assertThat(entity.getBody().getDescription()).isEqualTo("Role for developers Updated.");
    }


    @Test
    public void deleteRole_OK_ReturnsRoleDetails() {
        //act
        restTemplate.exchange(localHost + randomServerPort + "/roles/ROLE_TO_DELETE",
                HttpMethod.DELETE, new HttpEntity<>(headers), String.class)
                .getStatusCode().equals(HttpStatus.OK);
    }

}
