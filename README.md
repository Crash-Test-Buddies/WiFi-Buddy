# Wi-Fi Direct Handler

The goal of this project is to document and understand Wi-Fi Peer-to-Peer / Wi-Fi Direct on Android through an application that demonstrates its use, and also to build a library that simplifies the use of Wi-Fi Direct on Android. If this project is successful, it will remove some of the hurdles preventing developers from using Wi-Fi Direct in Android applications.

Android's implementation of Wi-Fi Direct is typically dreaded by developers and used only as a last resort if the app actually needs it. Its documentation is notoriously confusing, and often requested features such as no-prompt connections [are ignored](https://code.google.com/p/android/issues/detail?id=30880). If we can make it easier to develop Wi-Fi Direct apps on Android, then maybe the momentum will lead to better documentation and more development of Wi-Fi Direct within Android itself.

### Goals

Library

- Good documentation that explains _why_
- Consistent service discovery (i.e. why can't these adjacent devices find each other?)
- Fast connections
- Sane error handling (turning error codes into human readable strings)
- Logging of everything
- POJOs to replace complicated returns (looking at you dnsSdTxtRecord and dnsSdService)
- No prompt connections between devices
- Graceful disconnects when connection is lost
- Merge adjacent groups (if possible)
- Swap group owner (if possible)
- Large groups

Tester App

- Control over most variables in connection process (Group owner intent, name of the service, records broadcast in service, etc.)
- Fluent interface that is self explanatory
- Clear visual indication of what is happening with Wi-Fi Direct (displaying errors in a friendly way)

### Non-goals
- Rewrite Android's Wi-Fi Direct implementation (although we don't mind inspiring someone else to do so)
- Perfect compatibility on all devices

## Basic App Usage

Build the project with Gradle and you are good to go.
