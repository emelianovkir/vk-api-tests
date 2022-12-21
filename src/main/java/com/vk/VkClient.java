package com.vk;

import com.vk.api.sdk.client.AbstractQueryBuilder;
import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.Validable;
import com.vk.api.sdk.objects.likes.responses.IsLikedResponse;
import com.vk.api.sdk.objects.wall.responses.PostResponse;

import java.util.NoSuchElementException;
import java.util.Optional;

public class VkClient {

    private static final String REDIRECT_URI = "https://oauth.vk.com/blank.html";

    private final VkApiClient vk;
    private final UserActor userActor;

    public VkClient() {
        final var USER_ID = Optional.ofNullable(System.getenv("VK_USER_ID"))
                .orElseThrow(() -> new NoSuchElementException("VK_USER_ID is not defined"));

        final var ACCESS_TOKEN = Optional.ofNullable(System.getenv("VK_ACCESS_TOKEN"))
                .orElseThrow(() -> new NoSuchElementException("VK_ACCESS_TOKEN is not defined"));

        TransportClient transportClient = new HttpTransportClient();
        vk = new VkApiClient(transportClient);
        userActor = new UserActor(Integer.parseInt(USER_ID), ACCESS_TOKEN);
    }

    public VkClient(Boolean authorizationFlow) {
        try {
            final var APP_ID = Optional.ofNullable(System.getenv("VK_APP_ID"))
                    .orElseThrow(() -> new NoSuchElementException("VK_APP_ID is not defined"));

            final var CLIENT_SECRET = Optional.ofNullable(System.getenv("VK_CLIENT_SECRET"))
                    .orElseThrow(() -> new NoSuchElementException("VK_CLIENT_SECRET is not defined"));

            final var CODE = Optional.ofNullable(System.getenv("VK_CODE"))
                    .orElseThrow(() -> new NoSuchElementException("VK_CODE is not defined"));

            TransportClient transportClient = new HttpTransportClient();
            vk = new VkApiClient(transportClient);

            var authResponse = vk.oAuth()
                    .userAuthorizationCodeFlow(Integer.parseInt(APP_ID), CLIENT_SECRET, REDIRECT_URI, CODE)
                    .execute();

            userActor = new UserActor(authResponse.getUserId(), authResponse.getAccessToken());
        } catch (ApiException | ClientException e) {
            throw new RuntimeException(e);
        }
    }

    <T> Validable execute(AbstractQueryBuilder query) {
        try {
            var response = query.execute();

            if (response instanceof PostResponse)
                return (PostResponse) response;
            else if (response instanceof IsLikedResponse) {
                return (IsLikedResponse) response;
            }
            else {
                return (Validable) response;
            }

        } catch (ApiException | ClientException e) {
            throw new RuntimeException(e);
        }

    }

    public VkApiClient getApiClient() {
        return vk;
    }

    public UserActor getUserActor() {
        return userActor;
    }

}
