# Publish Olog Server releases to maven central

The Phoebus Olog service uses the maven release plugin to prepare the publish the olog server binaries to maven central
using the sonatype repositories.

## Setup

Create a sonatype account and update the maven settings.xml file with your sonatype credentials

```xml
<servers>
 <server>
    <id>phoebus-releases</id>
    <username>shroffk</username>
    <password>*******</password>
 </server>
</servers>
```

## Prepare the release

```
mvn release:prepare
```

In this step will ensure there are no uncommitted changes, ensure the versions number are correct, tag the scm, etc..
A full list of checks is documented [here](https://maven.apache.org/maven-release/maven-release-plugin/examples/prepare-release.html):

## Perform the release

```
mvn release:perform
```

Checkout the release tag, build, sign and push the build binaries to sonatype.

:::{note}
Mac OS users should invoke `export GPG_TTY=$(tty)` prior to `mvn release:perform`.
:::

## Publish

Open the staging repository in [sonatype](https://s01.oss.sonatype.org/#stagingRepositories) and hit the *publish* button

