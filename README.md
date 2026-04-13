# Mappe

![Mappe Version](https://jitpack.io/v/xihadulislam/mappe.svg)

© 2025 [@Xihad Islam](https://github.com/xihadulislam/). All rights reserved.

Mappe is an Android library that simplifies background task execution. Give any task a name, get a dedicated thread pool — one line of code.

---

## What's New in v2.0.0

- **`Mappe.on(name)`** — create a dedicated pool with any name you choose
- **`withDelay(ms)`** — schedule a task after a delay without blocking any thread
- **`executeAndShutUp(runnable)`** — run a task then shut down the executor automatically
- **Robust coroutine executor** — channel-based worker pool with bounded memory and backpressure
- **`withThreadPoolSize(n)`** now controls actual coroutine worker count (was a no-op in v1)
- **Thread-safe cancellation** — `@Volatile isCanceled` ensures `cancel()` is visible across threads
- **`Mappe.getCoroutineReport()`** — live status of all coroutine worker pools

---

## Installation

### Step 1 — Add JitPack repository

**`settings.gradle.kts`**
```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

**`settings.gradle`**
```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

### Step 2 — Add the dependency

**Gradle (Kotlin DSL)**
```kotlin
dependencies {
    implementation("com.github.xihadulislam:mappe:2.0.0")
}
```

**Gradle (Groovy)**
```groovy
dependencies {
    implementation 'com.github.xihadulislam:mappe:2.0.0'
}
```

**Maven**
```xml
<dependency>
    <groupId>com.github.xihadulislam</groupId>
    <artifactId>mappe</artifactId>
    <version>2.0.0</version>
</dependency>
```

---

## Usage

### `Mappe.on(name)` — the core API

Pass any name and get a dedicated background pool for it.
Two calls with the same name always share the same underlying pool.

```java
Mappe.on("payment").execute(() -> {
    processPayment();
});

Mappe.on("sync").execute(() -> {
    syncContacts();
});

Mappe.on("reports").execute(() -> {
    generateReport();
});
```

### Chain options onto `on()`

Every option is chainable before `execute()`.

**Control concurrency**
```java
// limit to 2 threads for this pool
Mappe.on("reports")
    .withThreadPoolSize(2)
    .execute(() -> generateReport());
```

**Run tasks one at a time**
```java
Mappe.on("db-writes")
    .serially()
    .execute(() -> writeToDatabase());
```

**Delay execution**
```java
// runs after 3 seconds — no thread is blocked during the wait
Mappe.on("retry")
    .withDelay(3000)
    .execute(() -> retryRequest());
```

**Execute then shut down**
```java
// submit the task, then close the pool automatically when it finishes
Mappe.on("one-shot")
    .executeAndShutUp(() -> doFinalCleanup());
```

### Main thread

```java
Mappe.onMainThread().execute(() -> {
    textView.setText("done");
});
```

### Convenience methods

Named shortcuts for common task types — each delegates to `on()` with a preset name.

| Method | Pool name |
|--------|-----------|
| `onBackgroundThread()` | Default |
| `onIOBackgroundThread()` | I/O |
| `onAllBackgroundThread()` | All |
| `onBulkBackgroundThread()` | Bulk |
| `onLogBackgroundThread()` | Log |
| `onSocketBackgroundThread()` | Socket |
| `onPrintBackgroundThread()` | Print |
| `onFileDownloadBackgroundThread()` | File-downloading |

```java
Mappe.onFileDownloadBackgroundThread().execute(() -> downloadFile(url));
Mappe.onLogBackgroundThread().execute(() -> writeLog(event));
```

### Task cancellation

```java
CancelableTask task = new CancelableTask() {
    @Override
    protected void doWork() {
        // skipped entirely if cancel() was called before execution
    }
};

Mappe.on("upload").execute(task);

// cancel from any thread at any time
task.cancel();
```

### Background work with UI callback

```kotlin
Mappe.on("fetch").execute(object : UiRelatedTask<String>() {
    override fun doWork(): String {
        return fetchDataFromNetwork()   // background thread
    }

    override fun thenDoUiRelatedWork(result: String) {
        textView.text = result          // main thread
    }
})
```

### Background work with progress updates

```kotlin
Mappe.on("import").execute(object : UiRelatedProgressTask<String, Int>() {
    override fun doWork(): String {
        for (i in 1..100) {
            publishProgress(i)              // posts to main thread
        }
        return "done"
    }

    override fun onProgressUpdate(progress: Int) {
        progressBar.progress = progress     // main thread
    }

    override fun thenDoUiRelatedWork(result: String) {
        progressBar.visibility = View.GONE
    }
})
```

### Monitoring

```java
// Thread executor pools
Log.d("Mappe", Mappe.getThreadReport().toString());

// Coroutine worker pools
Log.d("Mappe", Mappe.getCoroutineReport().toString());
```

---

## Requirements

- **Min SDK**: 24
- **Compile SDK**: 35
- **Java**: 8+
- **Kotlin**: 1.9+

---

## Contributing

Contributions are welcome. Open an issue or submit a pull request.

---

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
