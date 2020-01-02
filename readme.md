# Test App Loader 

A small application that loads another app for test purposes.
It is designed to set the `installer package name` for the target app
to a specific value.
This allows configuration properties to persist after an apps storage
is completely cleared.
It is used to set the backend for an app on a test device so that it survives
the test suite's reset.

This app is designed to be operated via ADB commands.  
The APK to be installed can be transferred to the test device either
via the ADB push command or can be loaded from a URL.

In some cases ADB push is blocked.  To work around that, Test App Loader will
download from a local server on the host machine on port 8080.

