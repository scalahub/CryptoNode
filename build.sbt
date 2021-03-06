name := "CryptoNode"

version := "0.1"

lazy val btc = (project in file("btc")).settings(
  libraryDependencies += "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.3",
  libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.6.0-M5",
  libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "2.0.0-M1",
  libraryDependencies += "org.bouncycastle" % "bcprov-jdk15on" % "1.61",
  libraryDependencies += "commons-codec" % "commons-codec" % "1.12",
  libraryDependencies += "commons-io" % "commons-io" % "2.6",
  libraryDependencies += "org.json" % "json" % "20140107",
  libraryDependencies += "org.bitcoinj" % "bitcoinj-core" % "0.14.7" % Test,
  name := "btc",
  mainClass in (Test, run) := Some("org.sh.cryptonode.RunStandAloneTests")
)

lazy val bitcoind = (project in file("bitcoind")).dependsOn(btc).settings(
  name := "bitcoind",
  mainClass in (Test, run) := Some("org.sh.cryptonode.btc.bitcoind.BitcoindTxParserTest")
)

lazy val bch = (project in file("bch")).dependsOn(btc).settings(
  name := "bch",
  mainClass in (Test, run) := Some("org.sh.cryptonode.bch.TestUAHF")
)

lazy val root = (project in file(".")).aggregate(btc,bch,bitcoind).settings(
  mainClass in (Test, run) := Some("org.sh.cryptonode.btc.TestBitcoinPeer"),
  name := "CryptoNode"
).dependsOn(
  btc % "compile->compile;test->test", bch,bitcoind
)

Project.inConfig(Test)(baseAssemblySettings)
