# Scala Learning Series: 2 - Build Scala 3 project using Tekton

Lets setup a scala3 project build process that generates a native image using [Tekton](https://tekton.dev/), [GraalVM](https://www.graalvm.org/) and [SBT](https://www.scala-sbt.org/).

## Tekton CICD

Tekton is a powerful cloud native open source framework for CI/CD pipelines, allowing developers to build, test and deploy across cloud providers. Tekton installs and runs as an extension on a Kubernetes cluster and comprises a set of Kubernetes Custom Resources Definitions(CRD) that define the building blocks you can create and reuse for your pipelines.

### Tekton Terminology

- Task
  - A Task is a collection of Steps that you define and arrange in a specific order of execution as part of your continuous integration flow. A Task executes as a Pod on your Kubernetes cluster.
- Pipeline
  - A Pipeline is a collection of Tasks that you define and arrange in a specific order of execution as part of your continuous integration flow.
- TaskRun
  - A TaskRun allows you to instantiate and execute a Task on-cluster.
- PipelineRun
  - A PipelineRun allows you to instantiate and execute a Pipeline on-cluster.
- Workspaces
  - Workspaces allow Tasks to declare parts of the filesystem that need to be provided at runtime by TaskRuns.

![Tekton Pipeline](tekton.png)

```shell
// install minikube
minikube start --memory 9243  --cpus 4

// install Tekton's CR's
kubectl apply --filename https://storage.googleapis.com/tekton-releases/pipeline/latest/release.yaml

// install Tekton Dashboard for visualization
kubectl apply --filename https://storage.googleapis.com/tekton-releases/dashboard/latest/release.yaml

// install Tekton Triggers and interceptors
kubectl apply --filename https://storage.googleapis.com/tekton-releases/triggers/latest/release.yaml
kubectl apply --filename https://storage.googleapis.com/tekton-releases/triggers/latest/interceptors.yaml

// wait for tekton install to complete
kubectl get pods --namespace tekton-pipelines --watch
NAME                                          READY   STATUS    RESTARTS   AGE
tekton-dashboard-d6478b774-c462b              1/1     Running   0          3m
tekton-pipelines-controller-bffdddb78-8frmt   1/1     Running   0          3m5s
tekton-pipelines-webhook-5b86665f9d-qc2mb     1/1     Running   0          3m5s

// install tekton tasks from tekton hub.
kubectl apply -f https://raw.githubusercontent.com/tektoncd/catalog/main/task/git-clone/0.9/git-clone.yaml
kubectl apply -f https://raw.githubusercontent.com/tektoncd/catalog/main/task/kaniko/0.6/kaniko.yaml

// create docker hub config.json as secret in k8
kaf docker-hub-secret.yml

// tail pipelinerun log.
tkn pipelinerun logs scala3-learn-pipelinerun -f
```

### Create a new scala 3 project using giter8 template

Lets setup the environment needed for a scala project. I am using [Coursier](https://get-coursier.io/) to make installation of scala tooling easier. Coursier is a scala application manager and artifact fetcher

```shell
brew install coursier/formulas/coursier
coursier install cs
cs setup
cs install sbt sbtn ammonite giter8 scala-cli scala scalac scalafmt
```

Once we have coursier installed and the required scala tools, lets install Java. We will install graalvm 17 community edition for this tutorial.

```shell
cs java --jvm graalvm-java17:22.3.1 --setup
```

The above command instructs coursier to install java and also update shell configuration with JAVA_HOME env variable.

```shell
echo $JAVA_HOME
/Users/vijayakkineni/Library/Caches/Coursier/arc/https/github.com/graalvm/graalvm-ce-builds/releases/download/vm-22.3.1/graalvm-ce-java17-darwin-amd64-22.3.1.tar.gz/graalvm-ce-java17-22.3.1/Contents/Home
```

Lets use [SBT](https://www.scala-sbt.org/) as the build tool for this project. [Giter8](http://www.foundweekends.org/giter8/) is command line tool to generate a scala project from templates.

```shell
sbt new scala/scala3.g8

copying runtime jar...
[info] welcome to sbt 1.8.2 (GraalVM Community Java 17.0.6)
[info] loading global plugins from /Users/vijayakkineni/.sbt/1.0/plugins
[info] set current project to new (in build file:/private/var/folders/lz/wr4rz8ys7bb62fn9p6513m900000gn/T/sbt_197e0a25/new/)
A template to demonstrate a minimal Scala 3 application

name [Scala 3 Project Template]: scala3-learn

Template applied in /Users/vijayakkineni/scala/./scala3-learn
```
  
I am using Visual Studio Code as IDE but you can use the IDE of your choice. Install VSCode and [Metals](https://scalameta.org/metals/) extension for VSCode along with Scaladex and Java extensions.

Open *scala3-learn* in your favorite IDE and lets inspect the structure of the project.

TODO: Project Strucutre Image

### Scala Tooling

#### Scalafmt

[Scalafmt](https://scalameta.org/scalafmt/) is a code formatter which has support for IntelliJ, VSCode and various other IDE's. To configure scalafmt add the following lines to .scalafmt.conf. The below configuration is based on my little experience with scala. Please comment on standards everyone follows in the comments section.

To install scalafmt. Add sbt plugin to *project/plugins.sbt*

```scala
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.0")
```

And create a .scalafmt.conf in the projects root directory as below.

```hocon
version = 3.7.1
runner.dialect = scala3

maxColumn = 80

lineEndings = unix

# Defaults
assumeStandardLibraryStripMargin = false
align.stripMargin = true

danglingParentheses = {
    defnSite = true
    callSite = true
    ctrlSite = true
    tupleSite = true
}

docstrings = {
    oneline = fold
    removeEmpty = true
    style = AsteriskSpace
    wrap = no
}

includeNoParensInSelectChains = true

newlines {
  alwaysBeforeElseAfterCurlyIf = yes
  avoidInResultType = yes
  avoidForSimpleOverflow = [slc]
  beforeCurlyLambdaParams = multilineWithCaseOnly
  afterCurlyLambdaParams = squash
  implicitParamListModifierForce = [after]
  inInterpolation = avoid
}

project {
  excludeFilters = [
    ".metals"
  ]
}

rewrite {
  
  rules = [
    AvoidInfix
    PreferCurlyFors
    RedundantBraces
    RedundantParens
    SortModifiers
  ]

  sortModifiers {
    order = [
      final
      sealed
      abstract
      override
      implicit
      private
      protected
      lazy
    ]
  }

  redundantBraces {
    stringInterpolation = true
    ifElseExpressions = yes
  }

  imports {
    sort = scalastyle
    expand = true
  }

  scala3 {
    convertToNewSyntax = true
    removeOptionalBraces = true
  }
}

project.includePaths."+" = ["glob:**.md"]
```

Metals automatically uses scalafmt to respond to formatting requests from the editor, according to the configuration defined in .scalafmt.conf.

In VSCode, if there is no .scalafmt.conf, upon receiving the first format request Metals will create the .scalafmt.conf file for you.

A few handy scalafmt sbt tasks:

- scalafmtSbtCheck (Checks if *.sbt and project/*.scala files are formatted)
- scalafmtSbt: Format *.sbt and project/*.scala files.
- scalafmtCheck: Check if the scala sources under the project have been formatted.
- scalafmt: Format main sources of myproject project.
- Test/scalafmt: Format test sources of myproject project

#### Scalafix

[Scalafix](https://scalacenter.github.io/scalafix/) is a refactoring and linting tool for scala.

Lets install scalafix in sbt.

```scala
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.10.4")
```

And create a .scalafix.conf in the projects root directory as below.

```hocon
rules = [
  DisableSyntax
  LeakingImplicitClassVal
  NoAutoTupling
  NoValInForComprehension
  RedundantSyntax
  OrganizeImports
]

OrganizeImports {
  blankLines = Auto
  coalesceToWildcardImportThreshold = 1
  expandRelative = true
  groupExplicitlyImportedImplicitsSeparately = false
  groupedImports = AggressiveMerge
  groups = [
    "re:javax?\\\\."
    "scala."
    "*"
    "$package;format="lower, package"$."
  ]
  importSelectorsOrder = Ascii
  importsOrder = Ascii
  preset = DEFAULT
  removeUnused = false
}
```

Scalafix comes with a small set of built-in rules. Rules are either **syntactic** or **semantic**.
**Syntactic**: the rule can run directly on source code without compilation.
**Semantic**: the rule requires input sources to be compiled beforehand with the Scala compiler and the SemanticDB compiler plugin enabled.

A few handy scalafix sbt tasks:

- scalafix *args*: Invoke scalafix command line interface directly
- scalafixAll *args*: Invoke scalafix across all configurations where scalafix is enabled.

#### Useful SBT Plugins

**Wartremover** - Is a scala linter and aids by removing some language's nasty features. I have turned on all available errors.

```scala
  //project/plugins.sbt
  addSbtPlugin("org.wartremover" % "sbt-wartremover" % "3.0.9")

  //build.sbt
  wartremoverErrors ++= Warts.all
```

**sbt-updates** - Display your sbt project's dependency updates.

```scala
  addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.6.4")
```

**sbt-tpolecat** - is an SBT plugin for automagically configuring scalac options according to the project Scala version.

```scala
  addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % "0.4.2")
```

#### Inspecting the build

#### Show list of projects and builds

The projects command displays the currently loaded projects. The projects are grouped by their enclosing build and the current project is indicated by an asterisk

```bash
sbt:scala3-learn> projects
[info] In file:/Users/vijayakkineni/scala/scala3-learn/
[info]   * root
```

#### Show the classpath used for compilation or testing

For the *Compile* classpath.

```bash
sbt:scala3-learn> show Compile/dependencyClasspath
[info] * Attributed(/Users/vijayakkineni/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala3-library_3/3.2.2/scala3-library_3-3.2.2.jar)
[info] * Attributed(/Users/vijayakkineni/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-library/2.13.10/scala-library-2.13.10.jar)
```

For the *Test* classpath.

```bash
sbt:scala3-learn> show Test/dependencyClasspath
[info] * Attributed(/Users/vijayakkineni/scala/scala3-learn/target/scala-3.2.2/classes)
[info] * Attributed(/Users/vijayakkineni/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala3-library_3/3.2.2/scala3-library_3-3.2.2.jar)
[info] * Attributed(/Users/vijayakkineni/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scalameta/munit_3/0.7.29/munit_3-0.7.29.jar)
[info] * Attributed(/Users/vijayakkineni/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-library/2.13.10/scala-library-2.13.10.jar)
[info] * Attributed(/Users/vijayakkineni/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scalameta/junit-interface/0.7.29/junit-interface-0.7.29.jar)
[info] * Attributed(/Users/vijayakkineni/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/junit/junit/4.13.2/junit-4.13.2.jar)
[info] * Attributed(/Users/vijayakkineni/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/test-interface/1.0/test-interface-1.0.jar)
[info] * Attributed(/Users/vijayakkineni/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar)
```

#### Show the main classes detected in a project

sbt detects the classes with public, static main methods for use by the run method and to tab-complete the runMain method.

```bash
sbt:scala3-learn> show Compile/discoveredMainClasses
[info] * hello
```

#### Show the Test classes detected in a project

Sbt detects tests according to fingerprints provided by test frameworks. The definedTestNames task provides as its result the list of test names detected in this way.

```bash
sbt:scala3-learn> show Test/definedTestNames
[info] * MySuite
```

Thanks for reading and stay tuned for more on scala. The above should provide a good hello world project. Checkout some of the awesome Giter8 templates to begin with a scala3 project. Please let me know if you want a beginner’s take on a scala topic. The project above can be found on [github](https://github.com/akkinenivijay/scala3-learn).