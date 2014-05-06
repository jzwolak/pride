Pride
=====

Pride is a tool to help you manage the local development of large modular applications built with Gradle. It consists of a command-line tool to manage your prides of modules, and a Gradle plugin to add some extra functionality to the Gradle projects.

[![Build Status](https://travis-ci.org/prezi/pride.svg?branch=master)](https://travis-ci.org/prezi/pride)

## How does it work?

Pride works with the concept of modules: Git repositories that contain individual Gradle projects that depend on each other. You can build large applications of such modules, but while working on the application in your local development environment, you rarely need to work on all of the modules at the same time.

If you only want to work on one module at a time, it's no problem, as Gradle will load the module's dependencies from whatever artifact repository you are deploying your built modules into. But things get more complicated when you want to change multiple interdependent modules at the same time. You will end up having to combine your modules into a single Gradle project, or relying on installing your modules in a local Ivy or Maven repository so that dependent modules can get to them.

Pride tries a different route. We call the set of modules you want to work on a "pride." A pride is a directory with all these modules locally cloned next to each other. What Pride does is that it generates a Gradle project on top of them, so that Gradle can see all your modules in a single Gradle multi-project.

```
+-- pride-root/
    +-- some-module/
    |   +- build.gradle
    |
    +-- some-other-module/
    |   +- build.gradle
    |
    +-- build.gradle      <--+--- these are generated by Pride
    +-- settings.gradle   <--+
```

Dependencies between the projects in a pride are resolved amongst themselves, so you don't need to install artifacts anywhere. Builds are also faster, because Gradle knows a lot about your pride, and after a change it can build only what is strictly necessary. All dependencies that are not part of the pride are resolved to external dependencies as usual.

### Workflow

Prides are designed to be relatively short-lived entities, born when you want to change something, and discarded when you are finished with your changes. You can have multiple prides, even containing the same modules.

Here's a typical Pride session:

```bash
# Create the pride
mkdir quick-fix-for-securty-bug
cd quick-fix-for-security-bug
pride init

# Add modules that need fixing (these are resolved from repo.base.url, see below)
pride add network-component backend-api

# Now you do some work and realize you also need another module (with an absolute URL)
pride add https://github.com/myself/myproject

# Once you changed stuff, you probably want to run "check" on all your projects:
gradle check

# If you are happy with your changes, you want to see what's changed
pride do -- git status --short

# You commit and push, and then discard the pride directory altogether
```

## Get Pride

### Prerequisites

You need [Gradle](http://gradle.org/) and [Git](http://git-scm.org/) installed.

### Installing Pride

If you want to install the newest version of Pride, or upgrade your existing installation, run this:

    $ \curl -sSL http://href.prezi.com/install-pride | bash

This will install a symlink in `/usr/local/bin`.

If you are installing Pride for the first time, it's recommended to set the base URL for all your Git repositories, for example:

    $ pride config repo.base.url git@github.com:prezi

So when you execute `pride add some-module`, it will clone `git@github.com:prezi/some-module`.

### Building from source

If you want to experiment with Pride:

```shell
git clone git@github.com:prezi/pride.git
cd pride
gradle installApp
export PATH=$PATH:`pwd`/pride/build/install/pride/bin
```

Note: On Windows you will need to add `pride/build/install/pride/bin` to the `PATH` manually.

Check if everything works via:

    $ pride version
    Pride version 0.5

## Usage

### Command line

Pride has an extensive help system (much similar to Git), so it's easy to start with:

    $ pride help

To create a new pride do this in an empty directory:

    $ pride init

To add modules by cloning them:

    $ pride add <repo>

Where `<repo>` is either a full repository URL (like `git@github.com:prezi/pride.git`), or the name of the repository under the `repo.base.url` configuration setting.

### The `pride` plugin

Pride has some additional functionalities to dependency resolution, so you need to apply the `pride` plugin on all your projects where you want to use these. This should be simple:

```groovy
buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath "com.prezi.gradle.pride:gradle-pride-plugin:0.5"
    }
}

apply plugin: "pride"
```

#### Using `dynamicDependencies { ... }`

The Pride plugin adds the concept of dynamically resolvable dependencies to Gradle. What it means is that a dependency can be resolved to either a local project if it is present in the pride (`project(path: "...")`), or to an external dependency (`group: "...", name: "...", version: "..."`).

Because Gradle does not have this functionality built-in, you cannot use `dependencies { ... }` to declare inter-module dependencies. Instead you will have to use a similar concept introduced by the Pride plugin: `dynamicDependencies { ... }`.

You declare a dependency like this:

```groovy
dynamicDependencies {
	compile group: "com.example", name: "example", version: "1.+"
}
```

If your pride contains the `com.example:example` project, the Pride plugin will convert this dependency into:

```groovy
dependencies {
	compile project(path: ":com.example:exampe")
}
```

If the `com.example:example` project is not part of the current pride (or if you are building this project outside of a pride), the dependency will be converted like this:

```groovy
dependencies {
	compile group: "com.example", name: "example", version: "1.+"
}
```

**Note:** You can still declare dependencies via the `dependencies { ... }` block, but they will not be resolved to project dependencies, even if the corresponding project is part of the pride.

#### Relative project references

If you want your project to work in a pride as well as in stand-alone mode, you cannot use `project(":some-other-subproject")` and `project(path: ":some-other-subproject")` in your builds to refer to other subprojects in the project, as Gradle does not support relative paths that point above the current project. Instead of these you can use `relativeProject(":some-other-subproject")` and `relativeProject(path: ":some-other-subproject")`. These methods are also provided by the `pride` plugin. These will be resolved properly both in stand-alone and pride-mode.


## Limitations and caveats

Craft your modules so that they are buildable on their own (*stand-alone mode*) as well as a part of a pride (*pride-mode*). It's not hard at all.

* Only use `include(...)` in `settings.gradle` -- Pride needs to merge all module's `settings.gradle`s, and it does not support arbitrary code.
* Do not use `buildSrc` to store your additional build logic. It's not a very good feature to start with, and Pride doesn't support it. Apply additional build logic from `something.gradle` instead.
* Do not rely on `project.rootDir` or `rootProject` either. These properties change depending on whether your project is part of a pride or not. Instead always refer to the parent project with `relativeProject(":")` and take the `projectDir` from that.

## Repo caching

To quickly create (and discard) prides, Git repos of modules are cached locally. Here's what Pride does when you `add` a module with cache enabled:

* checks in its cache directory if it already has a clone of the module
    * if it doesn't exist, it creates a mirror clone of it (see `--mirror` in [git-clone](http://git-scm.com/docs/git-clone))
    * if it exists, it does a `git fetch --all` on it
* clones the cached repo to your pride
* sets `origin` to point to the original repo

You can disable caching on a per-repo basis by using `pride add --no-repo-cache repo-name`. Or you can disable it by setting this in `~/.prideconfig`:

    repo.cache.always=false

## Why the name?

Working with a large modular application is like herding cats. Dangerous cats. Like a pride of lions.

![](http://i62.tinypic.com/2hs3g4o.jpg)

## License

Pride is available under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).

## Software used

* Gradle (http://gradle.org) to build stuff
* Airline (https://github.com/airlift/airline) for command-line handling
