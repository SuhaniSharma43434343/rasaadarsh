# RASAADARSH - Intelligent Rasaushadhi Cabinet

RASAADARSH is a premium Android application designed for Ayurvedic practitioners and researchers to access, search, and manage a vast database of clinical **Rasaushadhies** (Mineral-based Ayurvedic formulations).

## 🌟 Key Features

- **Clinical Database**: Access to 250+ verified medicine records transcribed from classical Rasashastra texts.
- **AI Assistant**: An intelligent chatbot powered by Gemini 2.0 to compare medicines, explain therapeutic actions, and assist in clinical reasoning.
- **Advanced Filtering**: Multi-select filters for Dosha (Vata, Pitta, Kapha), Ingredients, and Specific Diseases.
- **Practitioner Branding**: Professional profiles allowing doctors to personalize shared reports with their clinic name and credentials.
- **Clinical Notes**: Securely record personal observations for each formulation.


## 🛠️ Technical Architecture

- **UI Framework**: Modern declarative UI built with **Jetpack Compose**.
- **Architecture**: **MVVM** (Model-View-ViewModel) for clean separation of concerns.
- **Database**: Local **Room Persistence Library** with streaming JSON ingestion for high performance and low memory footprint.
- **Networking**: Secure API integration with **OpenRouter** for AI capabilities.
- **Dependency Management**: Gradle with Kotlin DSL and KSP for annotation processing.

## 📂 Project Structure

- `app/src/main/java/com/example/rasaushadhies/ui/viewmodels`: Business logic and data management.
- `app/src/main/java/com/example/rasaushadhies/ui/screens`: Composable UI components for all app screens.
- `app/src/main/java/com/example/rasaushadhies/ui/data`: Data models and legacy clinical definitions.
- `app/src/main/java/com/example/rasaushadhies/data/local`: Room DAO, Entities, and Type Converters.
- `app/src/main/assets`: Compressed clinical data source (`medicines.json`).

## 🚀 Getting Started

1. Open the project in **Android Studio (Ladybug or later)**.
2. Ensure you have the latest **Kotlin** and **Compose** plugins.
3. Build the project using `./gradlew assembleDebug`.
4. Deploy to a physical device or emulator running **Android 7.0 (API 24)** or higher.

## 📄 License & Disclaimer

Information in this app is for educational purposes for registered Ayurvedic Physicians. Formulations must only be used under professional supervision.

---
© 2024 · **Parul Institute of Ayurved** · Parul University
