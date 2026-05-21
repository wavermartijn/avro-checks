### API features specifications
* When calling the check function, use a builder pattern to construct the request. Like:
```java
AvroCompatibilityChecker.check()
.forCandidate(newSchema)
.withCompatibility(CompatibilityLevel.FULL)
.withOlderSchema(oldSchema)
.withOlderSchema(olderSchema)
.check();
``` 

but also 
```java
AvroCompatibilityChecker.check()
.forCandidate(newSchema)
.withCompatibility(CompatibilityLevel.FULL)
.withHistory(oldSchema, olderSchema)
.check();
``` 
and
```java
AvroCompatibilityChecker.check()
.forCandidate(newSchema)
.withCompatibility(CompatibilityLevel.FULL)
.withHistory(List.of(oldSchema, olderSchema))
.check();
``` 

* For the application, create a implementention with quarkus to natively build the cli to be used as a command line tool. Scripts should be for mac and windows and should be able to run the cli without having to install anything with GraalVM
