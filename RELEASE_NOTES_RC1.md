# 0.2.0-rc1

Release-candidate hardening untuk Potentia Research Pilot:

- Gradle/AGP ditetapkan ke kombinasi stabil untuk compileSdk 35.
- R8 + resource shrinking diaktifkan pada release build.
- Launcher icon/adaptive icon ditambahkan.
- System Back navigation ditangani untuk flow utama dan draft assessment diamankan saat keluar sesi.
- Result screen mendapat tombol Back dan mengingat asal navigasi (Home/History/Profile/Assessment).
- Settings menampilkan versionName dari package, bukan string Core V2 lama.
- Pilot export cache hanya menyisakan export terbaru.
- Creative parity instrumented test sekarang menjalankan seluruh 25 vector.
- Release-readiness instrumented checks ditambahkan.
- `.gitignore` ditambahkan agar `local.properties`, build output, IDE state, dan signing material tidak ter-commit.

Tidak ada perubahan pada formula scoring, item bank, bobot SJT, atau model Creative.
