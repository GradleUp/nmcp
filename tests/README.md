# Tests

Integration tests that check the contents of the zip bundle. To run from the root of the repo:

```shell
./gradlew -p tests/kmp checkZip
./gradlew -p tests/jvm checkZip
```

Open `tests/kmp` or `tests/jvm` in IntelliJ for development 