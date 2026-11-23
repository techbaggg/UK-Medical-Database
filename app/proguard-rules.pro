# UK Medical Database - R8 Optimization
-repackageclasses 'com.drgenz.a'
-allowaccessmodification

# Keep WorkManager Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class com.drgenz.UkDmDInitialImportWorker { *; }
-keep class com.drgenz.UkDmDSqlite { *; }
-keep class com.drgenz.MainActivity { *; }
-keep class com.drgenz.BackCompat { *; }
