# 🏋️‍♂️ SchFit

> **Part of The Sch Suite** — Android için geliştirilmiş; çevrimdışı öncelikli (offline-first), Jetpack Compose ve Gemini AI destekli modern kişisel fitness, antrenman hacmi ve kilo takip uygulaması.
>
> *Native Android workout tracker, progressive overload logger, and physique progression vault powered by Jetpack Compose & Gemini AI.*

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![Room Database](https://img.shields.io/badge/Storage-Room_DB-FF8F00?style=flat-square)](https://developer.android.com/training/data-storage/room)
[![Gemini AI](https://img.shields.io/badge/AI-Google_Gemini-8E24AA?style=flat-square&logo=googlegemini&logoColor=white)](https://deepmind.google/technologies/gemini/)
[![Sch Suite](https://img.shields.io/badge/Ecosystem-The_Sch_Suite-f59e0b?style=flat-square)](https://github.com/schadenfreuds/SchBudget)
[![License: MIT](https://img.shields.io/badge/License-MIT-emerald.svg?style=flat-square)](LICENSE)

---

## ✨ Öne Çıkan Özellikler

1. **⚡ Canlı Antrenman Seansı & Dinamik Sayaç (`ActiveWorkoutScreen`):**
   - Set, tekrar, ağırlık ve dinlenme süresi takibi.
   - Boks zili, antrenör düdüğü ve dijital zamanlayıcı ses efektleri (`sound_boxing_bell.wav`, `sound_gym_whistle.wav`).
2. **📈 Hacim & Aşamalı Yükleme (Progressive Overload):**
   - Kas grubu bazlı haftalık set ve tonaj analizleri (`WorkoutVolumeScreen`).
3. **⚖️ Kilo ve Vücut Kompozisyonu Takibi (`WeightTrackingScreen`):**
   - Hedef kilo pusulası (92 kg ➔ 75 kg atletik hedef), haftalık ağırlık ortalamaları ve trend grafikleri.
4. **📸 Gemini AI Fotoğraf & Işık İyileştirme (`GeminiLightingService.kt`):**
   - Spor salonu ilerleme ve form fotoğraflarını yapay zeka ile otomatik optimize eden ışık düzeltme motoru.
5. **🌍 5 Dilli Küresel Arayüz (i18n):**
   - Türkçe (TR), İngilizce (EN), Almanca (DE), İspanyolca (ES) ve İtalyanca (IT) tam dil desteği.
6. **🔒 %100 Gizlilik & Çevrimdışı Çalışma (Room DB):**
   - Tüm antrenman geçmişi internete ihtiyaç duymadan cihazınızdaki yerel veritabanında güvenle saklanır.

---

## 🛠️ Kurulum & Geliştirme

1. Projeyi klonlayın:
   ```bash
   git clone https://github.com/schadenfreuds/SchFit.git
   ```
2. **Android Studio** (Ladybug / Iguana veya daha yeni) ile açın.
3. Gradle senkronizasyonunun tamamlanmasını bekleyin.
4. Android cihazınızı (USB Hata Ayıklama ile) veya emülatörünüzü bağlayıp `Run 'app'` deyin.

---

## 🏛️ Mimari & Teknolojiler

- **Mimari:** MVVM (Model-View-ViewModel) + Repository Pattern + Clean Architecture
- **UI:** 100% Jetpack Compose + Material 3 + Dynamic Theming
- **Yerel Veritabanı:** Room SQLite (Coroutines Flow ile reaktif akış)
- **Yapay Zeka:** Gemini AI REST SDK
- **Ekosistem:** The Sch Suite (CanOS)

---

## 📄 Lisans
Bu proje [MIT Lisansı](LICENSE) altında açık kaynak olarak paylaşılmıştır.
