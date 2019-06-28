[![Build Status](https://travis-ci.org/skuzzle/semantic-version.svg?branch=master)](https://travis-ci.org/skuzzle/semantic-version)
[![Coverage Status](https://coveralls.io/repos/github/skuzzle/semantic-version/badge.svg?branch=master)](https://coveralls.io/github/skuzzle/semantic-version?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.skuzzle/semantic-version/badge.svg)](https://maven-badges.herokuapp.com/maven-central/de.skuzzle/semantic-version)
[![JavaDoc](http://javadoc-badge.appspot.com/de.skuzzle/semantic-version.svg?label=JavaDoc)](http://javadoc-badge.appspot.com/de.skuzzle/semantic-version)
![Twitter Follow](https://img.shields.io/twitter/follow/skuzzleOSS.svg?style=social)


semantic-version
================

This is a single-class [semantic version 2.0.0](http://semver.org/)
implementation for java 6+. It requires no further dependencies and is thereby
easy to use within your own projects. Key features:

* Lightweight: consists of only a single file, no dependencies
* Immutable: strict immutability ensures easy handling and thread safety
* Serializable: Objects can be serialized using Java's `ObjectOutputStream`.
* Fast: Many performance improvements make this the fastest semver implementation in java
  around (according to parsing and sorting performance)
* Compatible: Supports Java 6 but also provides many methods that are suitable to be used 
  as method references in Java 8. Latest release also features a Java 9 module-info!
* Stable: Ready for production since release 1.0.0 

## Maven Dependency
semantic-version is available through the Maven Central Repository. Just add
the following dependency to your pom:

If you are using Java >=9 use this release:
```xml
<dependency>
    <groupId>de.skuzzle</groupId>
    <artifactId>semantic-version</artifactId>
    <version>2.0.0</version>
</dependency>
```

If you are using Java 6, 7 or 8 use this release:
```xml
<dependency>
    <groupId>de.skuzzle</groupId>
    <artifactId>semantic-version</artifactId>
    <version>1.2.0</version>
</dependency>
```

## Java 9

Release `2.0.0` is bundled as a JPMS module. If you are using it in your Java 9 project,
add the following line to your `module-info.java`:

```
module com.your.module {
    // ...
    requires de.skuzzle.semantic;
}
```

## Usage

### Creation and parsing 
```java
// Version with pre-release and build meta data field
Version v1 = Version.parseVersion("1.0.2-rc1.2+build-20142402");
Version v2 = Version.create(1, 0 , 2, "rc1.2", "build-20142402");

// Simple version
Version v3 = Version.parseVersion("1.0.2");
Version v4 = Version.create(1, 0, 2);

// Version with no pre-release field but with build meta data field
Version v5 = Version.parseVersion("1.0.2+build-20142402");
Version v6 = Version.create(1, 0, 2, "", "build-20142402");

```

### Comparing
Versions can be compared as they implement `Comparable`:

```java
if (v1.compareTo(v2) < 0) { ... }
if (v1.isGreaterThan(v2)) { ... }
if (v1.isLowerThan(v2)) { ... }
```
In rare cases it might be useful to compare versions with including the build meta data 
field. If you need to do so, you can use

```java
v1.compareToWithBuildMetaData(v2)
v1.equalsWithBuildMetaData(v2)
```

There also exist static methods and comparators for comparing two versions.

### Deriving
You can derive new versions from existing ones by modifying a single field:

```java
Version v1 = Version.create(1, 0, 0)
        .withMinor(2)
        .withPatch(3)
        .withPreRelease("alpha-1")
        .withBuildMetaData("build-20161022");
```

### Incrementing
Versions can also be incremented using any of the `next...` methods:

```
// Gives 2.0.0
Version.create(1, 2, 3).nextMajor();

// Gives 1.3.0
Version.create(1, 2, 3).nextMinor();

// Gives 1.2.4
Version.create(1, 2, 3).nextPatch();
```

All `next...` methods will drop the pre-release and build meta data fields but provide an 
overload to set a new pre-release:

```
// Gives 2.0.0-SNAPSHOT
Version.create(1, 2, 3).nextMajor("SNAPSHOT");
```

The identifier parts can be incremented as well:

```
// Gives 1.2.3-1
Version.create(1, 2, 3).nextPreRelease();

// Gives 1.2.3+1
Version.create(1, 2, 3).nextBuildMetaData();
```

Incrementing the identifier behaves as follows:
* In case the identifier is currently empty, it becomes `1` in the result.
* If the identifier's last part is numeric, that last part will be incremented in the result.
* If the last part is not numeric, the identifier is interpreted as `identifier.0` which becomes `identifier.1` after increment.

Version | After increment
--------| ---------------
`1.2.3`| `1.2.3-1`
`1.2.3+build.meta.data` | `1.2.3-1`
`1.2.3-foo` | `1.2.3-foo.1`
`1.2.3-foo.1` | `1.2.3-foo.2`

### Serialization
Versions can be written to/read from streams by Java's `ObjectOutputStream` and 
`ObjectInputStream` classes out of the box:

```java 
new ObjectOutputStream(yourOutStream).writeObject(Version.parseVersion("1.2.3"));
Version version = (Version) new ObjectInputStream(yourInStream).readObject();
```

Serializing Versions from and to json is also possible but requires third party libraries
like `jackson` or `gson`. Support for those is not built in (in order to not ship extra 
dependencies) but examples can be found within the unit tests 
[here (jackson)](https://github.com/skuzzle/semantic-version/blob/master/src/test/java/de/skuzzle/semantic/CustomJacksonSerialization.java) 
and [here (gson)](https://github.com/skuzzle/semantic-version/blob/master/src/test/java/de/skuzzle/semantic/CustomGsonSerialization.java). Both examples will serialize the Version as its String representation as 
opposed to destructing it into its single fields.
