# POTENTIA — QA / Release Candidate Checklist

## Automated gates

- [ ] `./gradlew testDebugUnitTest`
- [ ] `./gradlew connectedDebugAndroidTest`
- [ ] Creative parity: seluruh 25 vector Python ↔ Android lulus
- [ ] `./gradlew lintDebug`
- [ ] `./gradlew assembleDebug`
- [ ] `./gradlew assembleRelease` (R8 + resource shrinking)

## Manual regression

### Onboarding / navigation
- [ ] Fresh install membuka onboarding sekali.
- [ ] System Back konsisten di Settings, History, Comparison, Result, Growth, Pilot Data.
- [ ] Back saat Processing tidak membatalkan scoring.
- [ ] Notification membuka tab Tumbuh.

### Assessment recovery
- [ ] Isi ≥10 soal lalu rotate: posisi + jawaban tetap.
- [ ] Tutup/relaunch di tengah sesi: Resume tersedia.
- [ ] Free text Creative yang belum Next tetap tersimpan setelah background/stop.
- [ ] Restart meminta konfirmasi sebelum draft dihapus.
- [ ] Setelah result berhasil disimpan, draft lama hilang.

### Scoring / results
- [ ] 47 item bisa diselesaikan end-to-end.
- [ ] Result memakai hasil aktual, bukan data demo.
- [ ] Creative menampilkan experimental/out-of-domain warning.
- [ ] 0–100 tidak disebut percentile/IQ/norma populasi.
- [ ] History dan Comparison memakai sesi nyata.

### Growth V3
- [ ] Mini Challenge baru selesai setelah jawaban diperiksa.
- [ ] Guided Reflection baru tersimpan setelah field penting terisi.
- [ ] Progress tetap ada setelah relaunch.
- [ ] UI tidak menyatakan aktivitas selesai sebagai bukti kemampuan meningkat.

### Settings / reminder
- [ ] Android 13+ meminta izin notifikasi saat reminder diaktifkan.
- [ ] Toggle OFF membatalkan unique worker.
- [ ] Toggle Growth OFF benar-benar menyembunyikan rekomendasi.

### Pilot/privacy
- [ ] Personal mode tidak menyimpan raw per-item responses.
- [ ] Pilot pseudonim menyimpan participant/session ID dan raw responses sesuai consent.
- [ ] Withdrawal menghentikan sesi pilot baru.
- [ ] Delete pilot data benar-benar menghapus data pilot lokal.
- [ ] JSON/CSV export berhasil dibagikan.
- [ ] Tidak ada permission INTERNET.

## Known limitations — jangan disembunyikan

1. Item bank V4.1 belum psychometrically validated.
2. Creative AI technically works tetapi model English AUT tidak tervalidasi untuk respons Potentia Bahasa Indonesia.
3. Weekly reminder memakai WorkManager, sehingga waktu eksekusi bersifat inexact/sekitar jadwal.
4. Pilot mode di aplikasi bukan pengganti persetujuan etik/informed consent institusional untuk penelitian formal.
5. Build ini masih RC sampai seluruh automated + manual gates di atas lulus pada perangkat nyata.
