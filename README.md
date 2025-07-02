# Secret Vault API

A microservice for managing short-lived secrets.

Run the application:

```
./gradlew bootRun
```

In a real-world production system, I'd use a cache like Redis or Hazelcast instead of `ConcurrentHashMap` and scale the application horizontally. The cache would need to be configured for durability across restarts to avoid data loss.
