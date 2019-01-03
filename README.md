## Prerequisites

* Install `gradle` version 5+, `docker`, `npm`.
* Run `sudo npm install -g coffeescript` 

## Building and running locally

#### Lazy way

This will run SQooL Frontend as if it was in the cloud (more or less). This way requires less typing but is generally 
not well suited for hack-compile-reload development cycles.
 
* Run `gradle jibDockerBuild`, `gradle docker`
* Run `cd devops && ./sqool-frontend-up.sh`

#### Efficient way

When running this way, you can reload your frontend (or even your static files) without having to rebuild Docker images, 
which naturally makes development cycles faster.

* Run PostgreSQL on standard port 5432 e.g. using Docker: 
```
docker run -d --name sqool-frontend_postgres -p 5432:5432 postgres:11` 
```
* Initialize database schema from `sqool-frontend-database.sql` file, e.g. using Docker again:
```
cd src/main/sql
docker run --rm --link sqool-frontend_postgres:postgres -v $(pwd):/workspace postgres:11 psql -h postgres -U postgres -f /workspace/sqool-frontend-database.sql
```
* Run SQooL Frontend server. Default values of command line arguments point to PostgreSQL running on localhost:5432 with
user `postgres` and no password:
```
/opt/gradle-5.0/bin/gradle run --args='--contest au'
```

If your PostgreSQL setup is different, pass appropriate values to command line arguments.


## Production deployment

