[![Build Status](https://travis-ci.org/skuzzle/semantic-version.svg?branch=master)](https://travis-ci.org/skuzzle/semantic-version)
[SonarQube](https://www.serverd.de/sonar/dashboard/index/359)

semantic-version
================

This is a single-class [semantic version 2.0.0](http://semver.org/)
implementation for java 7. It requires no further dependencies and is thereby
easy to use within your own projects. You may simply copy the single class to
your source folder and apply any modifications which suit your needs (as long
as you preserve the license header).

Go to the [implementation](https://github.com/skuzzle/semantic-version/blob/master/src/main/java/de/skuzzle/Version.java)
or have a look at the [JavaDoc](http://www.semantic-version.skuzzle.de/0.4.0/doc).

This repository contains JUnit tests of the provided implementation against the
semantic versioning specification.

## Versioning and Feedback
This implementation is currently in its initial phase and thereby versioned
*0.4.0* in terms of semantic versioning. Please feel free to provide feedback
by filing an issue to improve the implementation.

## Maven Dependency
semantic-version is available through the Maven Central Repository. Just add
the following dependency to your pom:

```xml
<dependency>
    <groupId>de.skuzzle</groupId>
    <artifactId>semantic-version</artifactId>
    <version>0.4.0</version>
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
```

`equals`, `hashCode` and `toString` are implemented appropriately. In rare cases
it might be useful to compare versions with including the build meta data field.
If you need to do so, you can use

```java
v1.compareToWithBuildMetaData(v2)
v1.equalsIncludeBuildMetaData(v2)
```

There also exist static methods and comparators for comparing two versions.
