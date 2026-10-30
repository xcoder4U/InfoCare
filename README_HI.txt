INFOCARE STUDIO - ANDROID SOURCE PROJECT

इस ZIP में InfoCare Studio v57 की HTML और Android Native WebView wrapper है।
मुख्य सुविधाएँ:
- JPG को Pictures/InfoCare Studio में save करना
- PDF को Downloads/InfoCare Studio में save करना
- Android file picker से image/PDF upload
- GitHub Actions से APK और AAB build

महत्वपूर्ण:
यह तैयार APK नहीं, Android source project है। ZIP के अंदर gradlew/gradlew.bat नहीं रखे गए हैं, क्योंकि पहले दिए गए launcher standard Gradle Wrapper नहीं थे। GitHub Actions workflow अपने build environment में Gradle 8.9 और Java 17 सेट करके build करता है।

GitHub से APK बनाने का तरीका:
1. ZIP extract करें।
2. Extract किए हुए folder के अंदर की सभी files और folders GitHub repository के ROOT में upload करें। ध्यान दें: app, build.gradle, settings.gradle और .github सीधे root में होने चाहिए।
3. GitHub repository में Actions खोलें।
4. “Build InfoCare APK and AAB” workflow चुनें और Run workflow दबाएँ।
5. Build सफल होने पर नीचे Artifacts में “InfoCare-APK-and-AAB” डाउनलोड करें।
6. ZIP artifact extract करें। Debug APK Android फोन में install किया जा सकता है। AAB आमतौर पर Play Store publishing के लिए होता है।

ZIP-to-APK converter के बारे में:
अगर किसी third-party converter की Docker image में Gradle उपलब्ध नहीं है और वह “Gradle is unavailable. Redeploy the latest Docker image.” दिखाता है, तो source ZIP बदलने से उस converter का Docker/Gradle environment ठीक नहीं होगा। इस project को दिए गए GitHub Actions workflow से build करें या Android Studio में खोलें।

सीमाएँ:
इस environment में Android SDK/Gradle उपलब्ध नहीं था, इसलिए APK compile या device test नहीं हुआ है। पहले Passport Photo, ID Card और A4 Document के JPG/PDF save तथा image/PDF upload को फोन पर जाँचें। Release AAB signing configuration के बिना unsigned रहेगा।
