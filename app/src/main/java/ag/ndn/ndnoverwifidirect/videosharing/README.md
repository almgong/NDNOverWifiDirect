The videosharing package contains all logic and implementation needed to create 
a functional video media player and sharer running over NDN. Sharing here means being able to load
and watch a video both locally and remotely on another device, roughly at the same time.

No tested, concrete implementation of sync is expected, as that is another area of interest
altogether. Video sharing is ROUGHLY simultaneous, in the sense that network and/or device
limitations can degrade parallel viewing of the same media samples.

The structure of this package will be such that one can simply export it as a jar, and easily
integrate it to another Android application.