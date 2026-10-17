# LinguaFree R8 rules. Libraries (Room, Hilt, kotlinx.serialization, Lottie, Media3) ship consumer rules.
# Keep @Serializable content models intact for asset parsing.
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-keepclassmembers @kotlinx.serialization.Serializable class org.linguafree.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class org.linguafree.**$$serializer { *; }
