# Mappe

![Mappe Version](https://jitpack.io/v/xihadulislam/mappe.svg)

Mappe is a powerful Java library designed to simplify task execution on main and background threads. It provides a flexible and easy-to-use API for managing concurrency in your JVM and Android applications.

## Features

- **Flexible Execution Models**: Choose between traditional thread-based execution or coroutine-based execution.
- **Main Thread Support**: Execute tasks on the main thread, ensuring smooth UI updates.
- **Customizable Background Executors**: Tailor background tasks with dedicated executors for different purposes (e.g., file downloads).
- **Dynamic Thread Pool Management**: Automatically adjusts the thread pool size based on available system resources.
- **Thread-Safe**: Implemented with thread-safe practices for reliable concurrent execution.
- **Simple API**: Intuitive methods for task submission with minimal boilerplate code.

## Getting Started

### Prerequisites

- Java Development Kit (JDK) installed.
- An IDE or build system (like Gradle or Maven) for your Java project.

### Installation

#### Step 1: Add JitPack Repository

Depending on your build system, include the JitPack repository in your project configuration.

- **For Gradle**:
  
  In your `settings.gradle` or `settings.gradle.kts`:
  
  ```groovy
  dependencyResolutionManagement {
      repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
      repositories {
          mavenCentral()
          maven { url 'https://jitpack.io' }
      }
  }


##### For Maven: In your pom.xml :
 ```groovy
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
 ```

##### For SBT: In your build.sbt
 ```
resolvers += "jitpack" at "https://jitpack.io"
 ```

#### Step 2: Add the Dependency
Add Mappe to your project dependencies.

For Gradle:
 ```
dependencies {
    implementation 'com.github.xihadulislam:mappe:1.0.0'
}
 ```

For Maven:
 ```
<dependency>
    <groupId>com.github.xihadulislam</groupId>
    <artifactId>mappe</artifactId>
    <version>1.0.0</version>
</dependency>
 ```
For SBT:
 ```
libraryDependencies += "com.github.xihadulislam" % "mappe" % "1.0.0"
 ```
#### Usage
Here's a quick example of how to use the Mappe library in your project:
 ```
public class Example {
    public static void main(String[] args) {
        // Execute a task on the main thread
        Mappe.onMainThread().execute(() -> {
            System.out.println("Running on main thread");
        });

        // Execute a background task
        Mappe.onBackgroundThread().execute(() -> {
            System.out.println("Running in the background");
        });

        // Execute a file download task
        Mappe.onFileDownloadBackgroundThread().execute(() -> {
            System.out.println("Downloading a file...");
        });
        
        // Execute a coroutine task
        Mappe.onCoroutineExecutor().executeCoroutine(() -> {
            System.out.println("Running coroutine task");
        });
    }
}
 ```
#### License
This project is licensed under the MIT License. See the LICENSE file for details.


#### Contributing
Contributions are welcome! Please feel free to submit a pull request or open an issue for discussion.
