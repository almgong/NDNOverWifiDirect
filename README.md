# NDNOverWifiDirect

An implementation of a simple protocol for enabling NDN communication between
WiFi Direct compatible devices. For demo purposes, there is a videosharing application
bundled with this project found in app/src/main/.../videosharing.

The relevant files for the protocol are found in:

```
app/src/main/.../callback
app/src/main/.../model
app/src/main/.../runnable
app/src/main/.../service
app/src/main/.../task
app/src/main/.../utils
AndroidManifest.xml (for some of the WiFi needed permissions)
```

Note that there is an example, working integration with the NFD-Android project
[here](https://github.com/almgong/NFD-android). It has been tested with API level
19.

