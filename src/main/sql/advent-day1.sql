
CREATE TABLE Government(id SERIAL PRIMARY KEY, value TEXT UNIQUE NOT NULL);

-- Планета, её название, расстояние до Земли, политический строй
CREATE TABLE Planet(
                       id SERIAL PRIMARY KEY,
                       name TEXT UNIQUE NOT NULL,
                       distance NUMERIC(5,2) NOT NULL,
                       government_id INT REFERENCES Government,
                       is_inhabited BOOLEAN NOT NULL);

-- Значения рейтинга пилотов
CREATE TYPE Rating AS ENUM('Harmless', 'Poor', 'Average', 'Competent', 'Dangerous', 'Deadly', 'Elite');

-- Пилот корабля
CREATE TABLE Commander(
                          id SERIAL PRIMARY KEY,
                          name TEXT UNIQUE NOT NULL,
                          rating Rating NOT NULL);

-- Космический корабль, вместимость пассажиров и класс корабля
CREATE TABLE Spacecraft(
                           id SERIAL PRIMARY KEY,
                           capacity INT NOT NULL CHECK(capacity > 0),
                           name TEXT NOT NULL UNIQUE,
                           class INT NOT NULL CHECK(class BETWEEN 1 AND 3));

-- Полет на планету в означеную дату, выполняемый кораблем, пилотируемый капитаном
CREATE TABLE Flight(id INT PRIMARY KEY,
                    spacecraft_id INT REFERENCES Spacecraft,
                    commander_id INT REFERENCES Commander,
                    planet_id INT REFERENCES Planet,
                    date DATE
);

-- Стоимость полета до планеты на корабле означенного класса
CREATE TABLE Price(
                      planet_id INT REFERENCES Planet NOT NULL,
                      spacecraft_class INT NOT NULL CHECK(spacecraft_class BETWEEN 1 AND 3),
                      price INT NOT NULL CHECK(price>0),
                      UNIQUE(planet_id, spacecraft_class));

-- Раса пассажира
CREATE TYPE Race AS ENUM('Elves', 'Men', 'Trolls');

-- Пассажир
CREATE TABLE Pax(
                    id INT PRIMARY KEY,
                    name TEXT NOT NULL,
                    race Race NOT NULL);

-- Резервирование места на полет
CREATE TABLE Booking(
                        ref_num TEXT PRIMARY KEY,
                        pax_id INT NOT NULL REFERENCES Pax,
                        flight_id INT NOT NULL REFERENCES Flight ON DELETE CASCADE);


CREATE FUNCTION GenGauss() RETURNS DOUBLE PRECISION AS $$
SELECT (random()+random()+random()+random()+random()) / 5::DOUBLE PRECISION;
$$ LANGUAGE SQL;


INSERT INTO Government(value)
SELECT unnest(ARRAY['Анархия', 'Коммунизм', 'Конфедерация', 'Олигархия', 'Демократия', 'Диктатура', 'Феодализм']);

-- ================================
-- Капитаны со случайными рейтингами
WITH Names AS (
    SELECT unnest(ARRAY['Громозека', 'Ким', 'Буран', 'Зелёный', 'Горбовский', 'Ийон Тихий', 'Форд Префект', 'Комов', 'Каммерер', 'Гагарин', 'Титов', 'Леонов', 'Крикалев', 'Армстронг', 'Олдрин']) AS name
), Ratings AS (
    select enumsortorder AS rating_num, enumlabel::Rating AS rating_value
    from pg_catalog.pg_enum
    WHERE enumtypid = 'rating'::regtype ORDER BY enumsortorder
),
     NameRating AS (
         SELECT name, (0.5 + random() * (SELECT MAX(rating_num) FROM Ratings))::int
             AS rating_num FROM Names
     )
INSERT INTO Commander(name, rating)
SELECT name, rating_value FROM NameRating JOIN Ratings USING(rating_num);

-- ================================
-- Перечисление планет со случайными расстояниями и правительствами
WITH PlanetNames AS (
    SELECT unnest(ARRAY[
        'Tibedied', 'Qube', 'Leleer', 'Biarge', 'Xequerin', 'Tiraor', 'Rabedira', 'Lave',
        'Zaatxe', 'Diusreza', 'Teaatis', 'Riinus', 'Esbiza', 'Ontimaxe', 'Cebetela', 'Ceedra',
        'Rizala', 'Atriso', 'Teanrebi', 'Azaqu', 'Retila', 'Sotiqu', 'Inleus', 'Onrira', 'Ceinzala',
        'Biisza', 'Legees', 'Quator', 'Arexe', 'Atrabiin', 'Usanat', 'Xeesle', 'Oreseren', 'Inera',
        'Inus', 'Isence', 'Reesdice', 'Terea', 'Orgetibe', 'Reorte', 'Ququor', 'Geinona',
        'Anarlaqu', 'Oresri', 'Esesla', 'Socelage', 'Riedquat', 'Gerege', 'Usle', 'Malama',
        'Aesbion', 'Alaza', 'Xeaqu', 'Raoror', 'Ororqu', 'Leesti', 'Geisgeza', 'Zainlabi',
        'Uscela', 'Isveve', 'Tioranin', 'Learorce', 'Esusti', 'Ususor', 'Maregeis', 'Aate',
        'Sori', 'Cemave', 'Arusqudi', 'Eredve', 'Regeatge', 'Edinso', 'Ra', 'Aronar',
        'Arraesso', 'Cevege', 'Orteve', 'Geerra', 'Soinuste', 'Erlage', 'Xeaan', 'Veis',
        'Ensoreus', 'Riveis', 'Bivea', 'Ermaso', 'Velete', 'Engema', 'Atrienxe', 'Beusrior',
        'Ontiat', 'Atarza', 'Arazaes', 'Xeeranre', 'Quzadi', 'Isti', 'Digebiti', 'Leoned',
        'Enzaer', 'Teraed'
        ]) AS name
)
INSERT INTO Planet(name, distance, government_id, is_inhabited)
SELECT name, (random() * 1000)::numeric(5,2), (0.5 + GenGauss() * (SELECT COUNT(*) FROM Government))::int, random()::INT::BOOLEAN
FROM PlanetNames;

UPDATE Planet SET distance=(SELECT MIN(distance) FROM Planet) WHERE distance=(SELECT MAX(distance) FROM Planet);

-- ================================
-- Стоимость билета увеличивается с увеличением расстояния и повышением класса корабля
WITH Planets AS (
    SELECT id, distance FROM Planet
), Classes AS (
    SELECT generate_series(1,3) AS spacecraft_class
)
INSERT INTO Price (planet_id, spacecraft_class, price)
SELECT id, spacecraft_class, (random() * 1000 + distance + 300*(4 - spacecraft_class))::INT
FROM Planets CROSS JOIN Classes;

-- ================================
-- Перечисление кораблей со случайными классами и вместимостью
WITH Names AS (
    SELECT unnest(ARRAY[
        'Кедр', 'Орел', 'Сокол', 'Беркут', 'Ястреб', 'Чайка', 'Рубин', 'Алмаз', 'Аргон', 'Амур', 'Байкал', 'Антей', 'Буран'
        ]) AS name
)
INSERT INTO Spacecraft(name, capacity, class)
SELECT name, (3+random()*20)::INT, (0.5+random()*3)::INT
FROM Names;

-- ================================
-- Случайные полеты
WITH MaxValues AS (
    SELECT (SELECT MAX(id) FROM Spacecraft) AS spacecraft,
           (SELECT MAX(id) - 1 FROM Commander) AS commander,
           (SELECT MAX(id) FROM Planet) AS planet
),
     Flights AS (
         SELECT generate_series(1, 500) AS id
     )
INSERT INTO Flight(id, spacecraft_id, commander_id, planet_id, date)
SELECT id, (0.5 + random()*spacecraft)::INT,
       (0.5 + random()*commander)::INT,
       (0.5 + random()*planet)::INT,
       ('2084-01-01'::DATE + random()*365*5 * INTERVAL '1 day')::DATE
FROM MaxValues CROSS JOIN Flights;

-- ================================
-- Случайные пассажиры
WITH Paxes AS(
    SELECT generate_series(1, 100) AS id
), Races AS (
    select enumsortorder AS race_num, enumlabel::Race AS race_value
    from pg_catalog.pg_enum
    WHERE enumtypid = 'race'::regtype ORDER BY enumsortorder
), PaxRace AS (
    SELECT id, 'Pax' || id::TEXT AS name, (0.5 + random() * (SELECT MAX(race_num) FROM Races))::int
                                 AS race_num FROM Paxes
)
INSERT INTO Pax(id, name, race)
SELECT id, name, race_value FROM PaxRace JOIN Races USING(race_num);

-- ================================
-- Случайные бронирования
WITH Bookings AS (
    SELECT generate_series(1, 2000) AS id
),
     MaxValues AS (
         SELECT (SELECT MAX(id) FROM Pax) AS pax,
                (SELECT MAX(id) FROM Flight) AS flight
     )
INSERT INTO Booking(ref_num, pax_id, flight_id)
SELECT substring(md5(id::TEXT)::TEXT for 10), (0.5 + random() * pax)::INT, (0.5 + random() * flight)::INT
FROM Bookings CROSS JOIN MaxValues;

INSERT INTO Flight(id, spacecraft_id, commander_id, planet_id, date)
SELECT * FROM (VALUES
               (1001, 5, 3, 1, ('2084-01-01'::DATE + random()*365*5 * INTERVAL '1 day')::DATE),
               (1002, 3, 3, 2, ('2084-01-01'::DATE + random()*365*5 * INTERVAL '1 day')::DATE),
               (1003, 1, 7, 1, ('2084-01-01'::DATE + random()*365*5 * INTERVAL '1 day')::DATE),
               (1004, 2, 7, 2, ('2084-01-01'::DATE + random()*365*5 * INTERVAL '1 day')::DATE),
               (1005, 3, NULL, 50, ('2084-01-01'::DATE + random()*365*5 * INTERVAL '1 day')::DATE)
              ) T ON CONFLICT(id) DO NOTHING;
INSERT INTO Planet(name, distance, government_id, is_inhabited) VALUES ('Ololola', 239, 5, true);
INSERT INTO Flight(id, spacecraft_id) VALUES (1000, 1);

