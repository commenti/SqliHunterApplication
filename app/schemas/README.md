# Room Schema Export Directory

Room Gradle plugin (androidx.room 2.6+) yahan schema JSON files likhta hai,
har build pe `kspDebugKotlin` / `kspReleaseKotlin` ke through.

Example output:
    app/schemas/com.yourapp.sqliautohunter.data.local.AppDatabase/1.json

Yeh files git mein REHNI chahiye — future Room migrations inhi se diff hoti hain.
Manually edit mat karo, Room regenerate karega.
