# Gson reads these classes by field name from assets/database.json.
-keep class com.fonolousa.app.data.FonoDatabase { *; }
-keep class com.fonolousa.app.data.Categoria { *; }
-keep class com.fonolousa.app.data.Nivel { *; }
-keep class com.fonolousa.app.data.ItemFono { *; }

# Keep Room entity constructors and fields stable across minified releases.
-keep class com.fonolousa.app.data.local.*Entity { *; }

-keepattributes Signature
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
