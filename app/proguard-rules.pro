# Preserve reflection-based JSON models and bundled Spotify protocol classes.
-keep class com.universe.remote.data.web.** { *; }
-keep class com.universe.remote.data.auth.** { *; }
-keep class com.spotify.** { *; }
-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,AnnotationDefault

# App Remote defaults to Gson (verified in ConnectionParams); Jackson is an unused optional adapter.
-dontwarn com.fasterxml.jackson.core.JsonGenerator
-dontwarn com.fasterxml.jackson.core.JsonParser
-dontwarn com.fasterxml.jackson.core.JsonProcessingException
-dontwarn com.fasterxml.jackson.databind.DeserializationContext
-dontwarn com.fasterxml.jackson.databind.DeserializationFeature
-dontwarn com.fasterxml.jackson.databind.JsonNode
-dontwarn com.fasterxml.jackson.databind.ObjectMapper
-dontwarn com.fasterxml.jackson.databind.SerializationFeature
-dontwarn com.fasterxml.jackson.databind.SerializerProvider
-dontwarn com.fasterxml.jackson.databind.deser.std.StdDeserializer
-dontwarn com.fasterxml.jackson.databind.ser.std.StdSerializer
-dontwarn com.spotify.base.annotations.NotNull
