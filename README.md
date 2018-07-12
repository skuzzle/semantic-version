[![Build Status](https://travis-ci.org/skuzzle/semantic-version.svg?branch=master)](https://travis-ci.org/skuzzle/semantic-version)
[![Coverage Status](https://coveralls.io/repos/github/skuzzle/semantic-version/badge.svg?branch=master)](https://coveralls.io/github/skuzzle/semantic-version?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.skuzzle/semantic-version/badge.svg)](https://maven-badges.herokuapp.com/maven-central/de.skuzzle/semantic-version)
[![JavaDoc](http://javadoc-badge.appspot.com/de.skuzzle/semantic-version.svg?label=JavaDoc)](http://javadoc-badge.appspot.com/de.skuzzle/semantic-version)


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
  as method references in Java 8
* Stable: Ready for production since release 1.0.0 

## Maven Dependency
semantic-version is available through the Maven Central Repository. Just add
the following dependency to your pom:

```xml
<dependency>
    <groupId>de.skuzzle</groupId>
    <artifactId>semantic-version</artifactId>
    <version>1.2.0</version>
</dependency>
```

## Usage

Usage of the `Version` class is simple. You can obtain instances using the
static factory methods:

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

Versions can be compared as they implement `Comparable`:

```java
if (v1.compareTo(v2) < 0) { ... }
if (v1.isGreaterThan(v2)) { ... }
if (v1.isLowerThan(v2)) { ... }
```

You can derive new versions from existing ones by modifying a single field:

```java
Version v1 = Version.create(1, 0, 0)
        .withMinor(2)
        .withPatch(3)
        .withPreRelease("alpha-1")
        .withBuildMetaData("build-20161022");
```

`equals`, `hashCode` and `toString` are implemented appropriately. In rare cases
it might be useful to compare versions with including the build meta data field.
If you need to do so, you can use

```java
v1.compareToWithBuildMetaData(v2)
v1.equalsWithBuildMetaData(v2)
```

There also exist static methods and comparators for comparing two versions.
