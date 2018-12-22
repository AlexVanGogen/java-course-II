# Torrent

### Tracker

Run:
```
java -jar tracker.jar
```

Tracker will run and listen port 8082.

### Client

Run:

```
java -jar client.jar <clientport>
```

Client will run and try to connect to the tracker.

Before connection, client will be asked for typing paths to files client wants to distribute.

After that, client will be able to send requests to tracker or some seeder.

Available commands:

* list -- show files on tracker
* upload <filename> -- tell tracker the client wants to distribute file with passed name
* sources <fileid> -- ask tracker to give info about active distributors that have file with given id (or its fragments)
* update <fileid>* -- ask tracker the client can be used as seeder and distribute files with passed ids
* get <fileid> -- try to download file with given id (or just missing fragments)