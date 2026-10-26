# Cloud Storage Management System (Java Swing + MySQL + JDBC)

## Folder structure
```
CloudStorageManagementSystem/
├── src/
│   ├── LoginMain.java       (entry point – Login screen)
│   ├── RegisterFrame.java   (User registration)
│   ├── Dashboard.java       (Upload, Download, Search, Rename, Delete, Share, Folders, Storage)
│   ├── DBConnection.java    (JDBC connection)
│   ├── User.java            (model class)
│   └── PasswordUtil.java    (SHA-256 password hashing)
├── sql/schema.sql           (database + tables)
├── output_screenshots/      (sample output images)
├── lib/                     (put mysql-connector-j-*.jar here)
├── run.sh / run.bat         (compile + run)
└── cloud_storage/           (created automatically – uploaded files)
```

## Setup
1. Install JDK 11+ and MySQL.
2. Run `sql/schema.sql` in MySQL.
3. Download MySQL Connector/J (https://dev.mysql.com/downloads/connector/j/) and copy the
   `.jar` into `lib/`.
4. Set DB credentials (defaults: root / password):
   - Linux/Mac: `export CSMS_DB_USER=root CSMS_DB_PASS=yourpass`
   - Windows:   `set CSMS_DB_USER=root` and `set CSMS_DB_PASS=yourpass`
5. Run `./run.sh` (Linux/Mac) or `run.bat` (Windows).

Eclipse: File > New > Java Project, copy `src/*.java` into the project's src,
add the connector jar via Build Path > Add External JARs, then run `LoginMain`.
