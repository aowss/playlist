This is an implementation of the [HTTP Live Streaming's Playlist](https://tools.ietf.org/html/rfc8216#section-4).

## Testing

We use [JUnit 5](https://junit.org/junit5/) for testing and as indicated [in the JUnit documentation](https://junit.org/junit5/docs/current/user-guide/#running-tests-build-maven), we use [Maven Surefire](https://maven.apache.org/surefire/maven-surefire-plugin)’s native support instead of using the `junit-platform-surefire-provider` dependency.  

Therefore, as mentioned [here](https://maven.apache.org/surefire/maven-surefire-plugin/examples/junit-platform.html), the only thing you have to do, is to add this dependency :

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter-engine</artifactId>
    <version>${junit.version}</version>
    <scope>test</scope>
</dependency>
```