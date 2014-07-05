[![Build Status](https://travis-ci.org/skuzzle/semantic-version.svg?branch=master)](https://travis-ci.org/skuzzle/semantic-version)

semantic-version
================

This is a single-class _semantic version 2.0.0_ implementation for java 7. It requires no further dependencies and is thereby easy to use within your own projects. You may simply copy the single class to your source folder and apply any modifications which suit your needs (as long as you preserve the license header).

Go to [implementation](https://github.com/skuzzle/semantic-version/blob/master/src/main/java/de/skuzzle/Version.java)

See http://semver.org/ for the specification of semantic versioning

This repository contains JUnit tests of the provided implementation against the semantic versioning specification.

## Versioning and Feedback
This implementation is currently in its initial phase and thereby versioned *0.2.0* in terms of semantic versioning. Please feel free to provide feedback by filing an issue to improve the implementation.


## Usage

Usage of the `Version` class is simple. You can obtain instances using the static factory methods:

```java
Version v1 = Version.parseVersion("1.0.2-rc1.2+build-20142402");

Version v2 = Version.create("1.0.2");

Version v3 = Version.create(1, 0, 2);
```

Versions can be compared as they implement `Comparable`:

```java
if (v1.compareTo(v2) < 0) { ... }
```

`equals`, `hashCode` and `toString` are implemented appropriately.
