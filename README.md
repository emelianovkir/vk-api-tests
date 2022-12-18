# vk-api-tests

Smoke tests for "I like" methods

https://dev.vk.com/method/likes

## General info
- Java 17
- vk-java-sdk https://dev.vk.com/sdk/java

## Run tests

You need to add user credentials to environment variables before running tests.
```
export VK_USER_ID="0123456"
export VK_ACCESS_TOKEN="vk1.a.XXX"
```

After that the tests can be run with the commandline or IDE.
```
./gradlew clean test -Dgroups=negative,positive
```
