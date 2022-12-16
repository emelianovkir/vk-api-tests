package com.vk;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.vk.api.sdk.client.ClientResponse;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.likes.Type;
import com.vk.api.sdk.objects.wall.WallpostFull;
import org.apache.commons.lang3.RandomUtils;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class LikesTest {

    private VkApiClient apiClient;
    private UserActor userActor;

    private WallpostFull wallPost;

    @BeforeMethod
    void beforeMethod() throws InterruptedException {
        //crutch to avoid error 'Too many requests per second'
        Thread.sleep(200);
    }

    @BeforeTest
    void beforeTest() throws ClientException, ApiException {
        var vkClient = new VkClient();

        apiClient = vkClient.getApiClient();
        userActor = vkClient.getUserActor();

        var postResponse = apiClient.wall()
                .post(userActor)
                .message("Automatically generated post for likes :)")
                .execute();

        var postUid = "%s_%s".formatted(userActor.getId(), postResponse.getPostId());
        var wallPosts = apiClient.wall()
                .getByIdExtended(userActor, postUid)
                .execute()
                .getItems();

        Assert.assertEquals(wallPosts.size(), 1, "One post must be created!");
        wallPost = wallPosts.get(0);
    }

    @Test(groups = {"positive"})
    void addOneLikeTest() throws ClientException, ApiException {
        var addResponse = apiClient.likes()
                .add(userActor, Type.POST, wallPost.getId())
                .execute();

        Assert.assertEquals(addResponse.getLikes(), 1, "One like must be added!");
    }

    @Test(dependsOnMethods = "addOneLikeTest", groups = {"positive"})
    void isLikedByUserTest() throws ClientException, ApiException {
        var isLikedResponse = apiClient.likes()
                .isLiked(userActor, Type.POST, wallPost.getId())
                .execute();

        Assert.assertTrue(isLikedResponse.isLiked(), "The post must be on the user's 'Likes' list");
    }

    @Test(dependsOnMethods = "addOneLikeTest", groups = {"positive"})
    void listLikesHasOneTest() throws ClientException, ApiException {
        var listResult = apiClient.likes().getList(userActor, Type.POST)
                .itemId(wallPost.getId())
                .execute();

        Assert.assertEquals(listResult.getCount(), 1, "Post must contain 1 like!");
        Assert.assertEquals(listResult.getItems().get(0), userActor.getId(), "Like must belong to our user!");
    }

    @Test(dependsOnMethods = {"isLikedByUserTest", "listLikesHasOneTest"}, groups = {"positive"})
    void deleteOneLikeTest() throws ClientException, ApiException {
        var deleteResult = apiClient.likes()
                .delete(userActor, Type.POST, wallPost.getId())
                .execute();

        Assert.assertEquals(deleteResult.getLikes(), 0, "Like must be deleted!");
    }

    @Test(dependsOnMethods = "deleteOneLikeTest", groups = {"positive"})
    void isNotLikedByUserTest() throws ClientException, ApiException {
        var isLikedResponse = apiClient.likes()
                .isLiked(userActor, Type.POST, wallPost.getId())
                .execute();

        Assert.assertFalse(isLikedResponse.isLiked(), "The post mustn't be on the user's 'Likes' list");
    }

    @Test(dependsOnMethods = "deleteOneLikeTest", groups = {"positive"})
    void listLikesEmptyTest() throws ClientException, ApiException {
        var listResult = apiClient.likes().getList(userActor, Type.POST)
                .itemId(wallPost.getId())
                .execute();

        Assert.assertEquals(listResult.getCount(), 0, "Post must contain 0 like!");
    }

    @Test(groups = {"negative"})
    void addOneLikeToNonExistTest() throws ClientException {
        var randomId = RandomUtils.nextInt(99999, 9999999);
        var clientResponse = apiClient.likes()
                .add(userActor, Type.POST, randomId)
                .executeAsRaw();

        assertThat(parseErrorMsg(clientResponse))
                .as("Error message must contain 'object not found'")
                .containsIgnoringCase("object not found");
    }

    @Test(groups = {"negative"})
    void isLikedByUserToRestrictedTypeTest() throws ClientException {
        var randomId = RandomUtils.nextInt(99999, 9999999);
        var clientResponse = apiClient.likes()
                .isLiked(userActor, Type.MARKET, randomId)
                .executeAsRaw();

        assertThat(parseErrorMsg(clientResponse))
                .as("Error message must contain 'access restriction'")
                .containsIgnoringCase("access restriction");
    }

    @Test(groups = {"negative"})
    void listLikesToNonExistTest() throws ClientException {
        var randomId = RandomUtils.nextInt(99999, 9999999);
        var clientResponse = apiClient.likes().getList(userActor, Type.POST)
                .itemId(randomId)
                .executeAsRaw();

        assertThat(parseErrorMsg(clientResponse))
                .as("Error message must contain 'this post does not exist'")
                .containsIgnoringCase("this post does not exist");
    }

    @Test(groups = {"negative"})
    void deleteLikeFromNonExistTest() throws ClientException {
        var randomId = RandomUtils.nextInt(99999, 9999999);
        var clientResponse = apiClient.likes()
                .delete(userActor, Type.POST, randomId)
                .executeAsRaw();

        assertThat(parseErrorMsg(clientResponse))
                .as("Error message must contain 'object was not found'")
                .containsIgnoringCase("object was not found");
    }


    @AfterTest
    void afterTest() throws ClientException, ApiException {
        var okResponse = apiClient.wall()
                .delete(userActor)
                .postId(wallPost.getId())
                .execute();
        Assert.assertEquals(okResponse.getValue(), "1", "Post must be deleted after tests!");
    }

    public String parseErrorMsg(ClientResponse clientResponse) {
        var jsonResponse = new Gson()
                .fromJson(clientResponse.getContent(), JsonObject.class);

        var error = jsonResponse.get("error");
        assertThat(error)
                .as("Content must contain 'error' section! Response: %s", jsonResponse.toString())
                .isNotNull();

        var errorMsg = error.getAsJsonObject()
                .get("error_msg");
        assertThat(errorMsg)
                .as("Content must contain 'error.error_msg' field! Response: %s", jsonResponse.toString())
                .isNotNull();

        return errorMsg.getAsString();
    }

}
