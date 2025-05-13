# Tests

Integration tests that check the contents of the zip bundle. To run from the root of the repo:

```shell
./gradlew -p tests/jvm checkZip
./gradlew -p tests/kmp build
```

* `tests/jvm` can be used to deploy to the real Central Portal:

```shell
./gradlew -p tests/jvm publishAggregationToCentralPortal
./gradlew -p tests/jvm publishAggregationToCentralSnapshots
```
* `tests/kmp` uses a mockserver to verify the contents uploaded without having to do a real upload every time.

Open `tests/kmp` or `tests/jvm` in IntelliJ for development 
