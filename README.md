# PlexAPI-Java

## What is this?
This is a wrapper for Plex's API that handles authentication for you.

## How to include the library?
**Gradle:**
In build.gradle, add the following:
```
repositories {
  //... Other repositories here.
  maven { url 'https://jitpack.io' }
}

dependencies {
  //... Other dependencies here.
  implementation 'com.github.MoSadie:PlexAPI-Java:Tag'
}
```

## How to use?
1. Create a new PlexApi object with all the parameters possible.
2. Authenicate using either `authenticate(user, pass)`, `authenticate(authToken)`, or `startPinAuth()` methods.
