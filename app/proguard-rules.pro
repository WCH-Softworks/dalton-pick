# R8 rules for the release build. AndroidX, Compose and CameraX ship their own consumer rules, and
# classes named in the manifest (activities, services, the receiver, the tile) are kept automatically.

# Readable line numbers in Play Console crash reports. The mapping file travels inside the AAB.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
