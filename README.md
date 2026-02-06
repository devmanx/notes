# Notes (Android MVP)

Minimalna aplikacja Android do tworzenia i przeglądania notatek w plikach `.txt`.
Notatki są grupowane labelami, a metadane są przechowywane w `notes_index.json`
obok plików tekstowych.

## Założenia MVP
- Notatki są przechowywane jako zwykłe pliki `.txt`.
- Aplikacja zarządza katalogiem `notes` w `getExternalFilesDir`.
- Labele są przechowywane w indeksie JSON.
- Backup tworzy zaszyfrowany plik `.enc` (AES-GCM) i przekazuje go do warstwy storage.

## Backup Google Drive
Warstwa backupu opiera się na interfejsie `DriveUploader`. Wystarczy dostarczyć
implementację, która użyje Google Drive API (np. z Google Sign-In + REST API).
To pozwala łatwo dodać kolejne storage w przyszłości.

## Przyszła rozbudowa
- Dodanie kolejnych providerów (np. S3, Dropbox) przez kolejne implementacje `BackupStorage`.
- Ekran konfiguracji źródeł backupu.
