# Журнал спортивних тренувань (КЗП, ЛР3, варіант 2)

Абстрактний `Training` має підтипи `OutdoorTraining` і `IndoorTraining` з різною формулою `load()`, вид - enum `TrainingKind`, рівність - `equals`/`hashCode`. До звіту додано розділ поліморфного навантаження.

## Формат входу

UTF-8, один запис у рядку, поля розділено `;`:

```text
date;kind;minutes;distanceKm;calories
2026-09-01;біг;45;8.50;520
```

Допустимі види: біг, велосипед, ходьба (надворі), плавання, тренажери (у приміщенні).
Хибні рядки пропускаються з повідомленням про номер рядка й причину.

## Збірка та запуск

```text
mvnw.cmd verify
java -jar target/kzp-lab-andrushko-1.2.0.jar
java -jar target/kzp-lab-andrushko-1.2.0.jar --input data/input.csv --output out/report.txt
java -jar target/kzp-lab-andrushko-1.2.0.jar --help
java -jar target/kzp-lab-andrushko-1.2.0.jar --version
```

На macOS та Ubuntu замість `mvnw.cmd` використовуйте `./mvnw`. Звіт про виконання - у `REPORT.md`.
