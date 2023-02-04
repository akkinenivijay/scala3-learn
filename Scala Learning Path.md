# Scala Learning Series: 1 - Environment Setup

## Ide and Tooling Setup

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
