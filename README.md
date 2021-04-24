# shapeless: generic programming for Scala

**shapeless** is a type class and dependent type based generic programming library for Scala. It had its origins in
several talks by Miles Sabin ([@milessabin][milessabin]), given over the course of 2011, on implementing [scrap your
boilerplate][syb] and [higher rank polymorphism][higherrank] in Scala. Since then it has evolved from being a resolutely
experimental project into a library which, while still testing the limits of what's possible in Scala, is being used
widely in production systems wherever there are arities to be abstracted over and boilerplate to be scrapped.

[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/milessabin/shapeless)
[![Maven Central](https://img.shields.io/maven-central/v/com.chuusai/shapeless_2.13.svg)](https://maven-badges.herokuapp.com/maven-central/com.chuusai/shapeless_2.13)
[![Scala.js](https://www.scala-js.org/assets/badges/scalajs-0.6.8.svg)](https://www.scala-js.org)

## Projects which use shapeless

There is a wide variety of projects which use shapeless in one way or another ... see the
[incomplete list of projects][built-with-shapeless] for ideas and inspiration. If you are using shapeless and your
project isn't listed yet, please add it.

[built-with-shapeless]: https://github.com/milessabin/shapeless/wiki/Built-with-shapeless

## Finding out more about the project

The [feature overview for shapeless-2.0.0][features200] provides a very incomplete introduction to shapeless.
Additional information can be found in subsequent [release notes][relnotes220]. If you are upgrading from
shapeless-2.0.0 you will find the [migration guide][migration210] useful. We're not satisfied with the current state
of the documentation and would love help in improving it. You can find an excellent guide to Shapeless here:
[The Type Astronaut's Guide to Shapeless](https://github.com/underscoreio/shapeless-guide).

shapeless is part of the [Typelevel][typelevel] family of projects. It is an Open Source project under the Apache
License v2, hosted on [github][source]. Binary artefacts are published to the
[Sonatype OSS Repository Hosting service][sonatype] and synced to Maven Central.

Most discussion of shapeless and generic programming in Scala happens on the shapeless [Gitter channel][gitter]. There
is also a [mailing list][group] and [IRC channel][irc], but these are largely dormant now that most activity has moved
to Gitter. Questions about shapeless are often asked and answered under the [shapeless tag on StackOverflow][so]. Some
articles on the implementation techniques can be found on [Miles's blog][blog], and Olivera, Moors and Odersky, [Type
Classes as Object and Implicits][tcoi] is useful background material.

[features200]: https://github.com/milessabin/shapeless/wiki/Feature-overview:-shapeless-2.0.0
[relnotes]: https://github.com/milessabin/shapeless/wiki/Release-notes:-shapeless-2.0.0
[relnotes220]: https://github.com/milessabin/shapeless/wiki/Release-notes:-shapeless-2.2.0
[migration]: https://github.com/milessabin/shapeless/wiki/Migration-guide:-shapeless-1.2.4-to-2.0.0
[migration210]: https://github.com/milessabin/shapeless/wiki/Migration-guide:-shapeless-2.0.0-to-2.1.0
[milessabin]: https://twitter.com/milessabin
[syb]: https://www.microsoft.com/en-us/research/publication/scrap-your-boilerplate-with-class/
[higherrank]: http://homes.sice.indiana.edu/ccshan/cs252/usage.pdf
[typelevel]: http://typelevel.org/
[scalaz]: https://github.com/scalaz/scalaz
[spire]: https://github.com/non/spire
[tcoi]: http://ropas.snu.ac.kr/~bruno/papers/TypeClasses.pdf
[source]: https://github.com/milessabin/shapeless
[sonatype]: https://oss.sonatype.org/index.html#nexus-search;quick~shapeless
[wiki]: https://github.com/milessabin/shapeless/wiki
[group]: https://groups.google.com/group/typelevel
[so]: http://stackoverflow.com/questions/tagged/shapeless
[gitter]: https://gitter.im/milessabin/shapeless
[irc]: http://webchat.freenode.net?channels=%23shapeless
[blog]: http://milessabin.com/blog

## Participation

The shapeless project supports the [Scala Code of Conduct][codeofconduct] and wants all of its
channels (mailing list, Gitter, IRC, github, etc.) to be welcoming environments for everyone.

Whilst shapeless is a somewhat "advanced" Scala library, it is a lot more approachable than many people think.
Contributors are usually available to field questions, give advice and discuss ideas on the [Gitter channel][gitter],
and for people wanting to take their first steps at contributing we have a selection of open issues flagged up as
being [good candidates to take on][goodfirstissue]. No contribution is too small, and guidance is always available.

[codeofconduct]: https://www.scala-lang.org/conduct/
[goodfirstissue]: https://github.com/milessabin/shapeless/issues?q=is%3Aopen+is%3Aissue+label%3A%22good+first+issue%22

## Using shapeless

Binary release artefacts are published to the [Sonatype OSS Repository Hosting service][sonatype] and synced to Maven
Central. Snapshots of the main branch are built using GitHub actions and automatically published to the Sonatype
OSS Snapshot repository.

### Try shapeless with an Ammonite instant REPL

The quickest way to get to a REPL prompt with the latest version of shapeless on the class path is to run the
provided ["try shapeless"][try-shapeless] script, which has no dependencies other than an installed JDK. This script
downloads and installs [coursier][coursier] and uses it to fetch the [Ammonite][ammonite] REPL and the latest version
of shapeless. It then drops you immediately into a REPL session,

```text
% curl -s https://raw.githubusercontent.com/milessabin/shapeless/main/scripts/try-shapeless.sh | bash
Loading...
Compiling (synthetic)/ammonite/predef/interpBridge.sc
Compiling (synthetic)/ammonite/predef/replBridge.sc
Compiling (synthetic)/ammonite/predef/DefaultPredef.sc
Compiling /home/miles/projects/shapeless/(console)
Welcome to the Ammonite Repl 1.6.8
(Scala 2.13.1 Java 1.8.0_212)
If you like Ammonite, please support our development at www.patreon.com/lihaoyi
@ 23 :: "foo" :: true :: HNil 
res0: Int :: String :: Boolean :: HNil = 23 :: "foo" :: true :: HNil

@ Bye!
%
```

[try-shapeless]: https://github.com/milessabin/shapeless/blob/main/scripts/try-shapeless.sh
[coursier]: https://github.com/alexarchambault/coursier
[ammonite]: https://github.com/lihaoyi/Ammonite

### shapeless-2.3.3 with SBT

To include the Sonatype repositories in your SBT build you should add,

```scala
resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)
```


[ci]: https://travis-ci.org/milessabin/shapeless


Builds are available for Scala 2.11.x, 2.12.x and 2.13.x. The main line of development for
shapeless 2.3.3 is Scala 2.13.2.

```scala
scalaVersion := "2.13.5"

libraryDependencies ++= Seq(
  "com.chuusai" %% "shapeless" % "2.3.3"
)
```

For using snapshots of Shapeless you should add,
```scala
scalaVersion := "2.13.5"

libraryDependencies ++= Seq(
  "com.chuusai" %% "shapeless" % "2.4.0-SNAPSHOT"
)
```



### shapeless-2.3.3 with Maven

shapeless is also available for projects using the Maven build tool via the following dependency,

```xml
<dependency>
  <groupId>com.chuusai</groupId>
  <artifactId>shapeless_2.13</artifactId>
  <version>2.3.3</version>
</dependency>
```

### Older releases

Please use a current release if possible. If unavoidable, you can find [usage information for older
releases][olderusage] on the shapeless wiki.

[olderusage]: https://github.com/milessabin/shapeless/wiki/Using-shapeless:-older-releases

## Building shapeless

shapeless is built with SBT 1.3.10 or later, and its main branch is built with Scala 2.13.2 by default but also
cross-builds for 2.11.12 and 2.12.12.

[namehashing]: https://github.com/sbt/sbt/issues/1640

## Contributors

+ Alex Zorab <alec.zorab@gmail.com> [@aleczorab](https://twitter.com/aleczorab)
+ Alessandro Lacava <alessandrolacava@gmail.com> [@lambdista](https://twitter.com/lambdista)
+ Alexander Konovalov <alex.knvl@gmail.com> [@alexknvl](https://twitter.com/alexknvl)
+ Alexandre Archambault <alexandre.archambault@gmail.com> [@alxarchambault](https://twitter.com/alxarchambault)
+ Alistair Johnson <alistair.johnson@johnsonusm.com> [@AlistairUSM](https://twitter.com/AlistairUSM)
+ Allison H. <allisonhb@gmx.com>
+ Alois Cochard <alois.cochard@gmail.com> [@aloiscochard](https://twitter.com/aloiscochard)
+ Andreas Koestler <andreas.koestler@gmail.com> [@AndreasKostler](https://twitter.com/AndreasKostler)
+ Andrew Brett <github@bretts.org> [@Ephemerix](https://twitter.com/Ephemerix)
+ Aristotelis Dossas <teldosas@gmail.com> [@teldosas](https://twitter.com/teldosas)
+ Arya Irani <arya.irani@gmail.com> [@aryairani](https://twitter.com/aryairani)
+ Ben Hutchison <brhutchison@gmail.com> [@ben_hutchison](https://twitter.com/ben_hutchison)
+ Ben James <ben.james@guardian.co.uk> [@bmjames](https://twitter.com/bmjames)
+ Brian McKenna <brian@brianmckenna.org> [@puffnfresh](https://twitter.com/puffnfresh)
+ Brian Zeligson <brian.zeligson@gmail.com> [@beezee](https://twitter.com/bzeg)
+ Bryn Keller <xoltar@xoltar.org> [@brynkeller](https://twitter.com/brynkeller)
+ Carlos Quiroz [@carlosmquiroz](https://twitter.com/carlosmquiroz)
+ Chris Hodapp <clhodapp1@gmail.com> [@clhodapp](https://twitter.com/clhodapp)
+ Cody Allen <ceedubs@gmail.com> [@fourierstrick](https://twitter.com/fourierstrick)
+ Dale Wijnand <dale.wijnand@gmail.com> [@dwijnand](https://twitter.com/dwijnand)
+ Daniel Urban <urban.dani@gmail.com>
+ Dario Rexin <dario.rexin@r3-tech.de> [@evonox](https://twitter.com/evonox)
+ Dave Gurnell <d.j.gurnell@gmail.com> [@davegurnell](https://twitter.com/davegurnell)
+ David Barri <japgolly@gmail.com> [@japgolly](https://twitter.com/japgolly)
+ Denis Mikhaylov <notxcain@gmail.com> [@notxcain](https://twitter.com/@notxcain)
+ Dmitry Kovalev <kdn.kovalev@gmail.com>
+ Eugene Burmako <xeno.by@gmail.com> [@xeno_by](https://twitter.com/xeno_by)
+ Fabio Labella <fabio.labella2@gmail.com> [@SystemFw](https://twitter.com/SystemFw)
+ Filipe Nepomuceno <filinep@gmail.com>
+ Frank S. Thomas <frank@timepit.eu> [@fst9000](https://twitter.com/fst9000)
+ George Leontiev <folone@gmail.com> [@folone](https://twitter.com/folone)
+ Georgi Krastev <joro.kr.21@gmail.com> [@Joro_Kr](https://twitter.com/joro_kr)
+ Hamish Dickenson <hamish.dickson@gmail.com> [@hamishdickson](https://twitter.com/hamishdickson)
+ Harrison Houghton <hhoughton@learningobjects.com>
+ Howard Branch <purestgreen@gmail.com> [@purestgreen](https://twitter.com/purestgreen)
+ Huw Giddens <hgiddens@gmail.com>
+ Hywel Andrews <hywel.andrews@agoda.com>
+ Ievgen Garkusha <ievgen@riskident.com>
+ Jacob Barber <jacoby6000@gmail.com> [@jacoby6000](https://twitter.com/jacoby6000)
+ Jason Zaugg <jzaugg@gmail.com> [@retronym](https://twitter.com/retronym)
+ Jean-Baptiste Giraudeau <jb@giraudeau.info> [@jb9i](https://twitter.com/jb9i)
+ Jean-Remi Desjardins <jeanremi.desjardins@gmail.com> [@jrdesjardins](https://twitter.com/jrdesjardins)
+ Jeff Martin <jmartin@homeaway.com>
+ Jeff Wilde <jeff@robo.ai>
+ Jeremy R. Smith <jeremyrsmith@gmail.com> [@jeremyrsmith](https://twitter.com/jeremyrsmith)
+ Jisoo Park <xxxyel@gmail.com> [@guersam](https://twitter.com/guersam)
+ Johannes Rudolph <johannes.rudolph@gmail.com> [@virtualvoid](https://twitter.com/virtualvoid)
+ Johnny Everson <khronnuz@gmail.com> [@johnny_everson](https://twitter.com/johnny_everson)
+ Jolse Maginnis jolse.maginnis@pearson.com [@doolse2](https://twitter.com/doolse2)
+ Joni Freeman <joni.freeman@ri.fi> [@jonifreeman](https://twitter.com/jonifreeman)
+ Joseph Price <josephprice@iheartmedia.com>
+ Juan José Vázquez Delgado <juanjo.vazquez.delgado@tecsisa.com> [@juanjovazquez](https://twitter.com/juanjovazquez)
+ Julien Tournay <boudhevil@gmail.com> [@skaalf](https://twitter.com/skaalf)
+ Jules Gosnell <jules_gosnell@yahoo.com>
+ Kailuo Wang <kailuo.wang@gmail.com> [@kailuowang](https://twitter.com/kailuowang)
+ Kazuki Moriyama <pharcydetip@gmail.com> [@kazchimo](https://twitter.com/Kazuki_Moriyama)
+ Kenji Yoshida <6b656e6a69@gmail.com> [@xuwei_k](https://twitter.com/xuwei_k)
+ Kevin Wright <kev.lee.wright@gmail.com> [@thecoda](https://twitter.com/thecoda)
+ Lars Hupel <lars.hupel@mytum.de> [@larsr_h](https://twitter.com/larsr_h)
+ Lukasz Golebiewski <lukasz.golebiewski@gmail.com> [@LukaszGobiewsk1](https://twitter.com/LukaszGobiewsk1)
+ Mario Pastorelli <mario.pastorelli@teralytics.ch> [@mapastr](https://twitter.com/mapastr)
+ Matthew Taylor <matthew.t@tbfe.net>
+ Mathias Doenitz <mathias@spray.io> [@sirthias](https://twitter.com/sirthias)
+ Michael Donaghy <md401@srcf.ucam.org>
+ Michael Pilquist <mpilquist@gmail.com> [@mpilquist](https://twitter.com/mpilquist)
+ Michael Zuber <michaelg.zuber@gmail.com> [@mgzuber91](https://twitter.com/mgzuber91)
+ Mike Limansky <mike.limansky@gmail.com> [@mike_limansky](https://twitter.com/mike_limansky)
+ Miles Sabin <miles@milessabin.com> [@milessabin](https://twitter.com/milessabin)
+ n4to4 <n4to4k@gmail.com> [@n4to4](https://twitter.com/n4to4)
+ Neville Li <neville@spotify.com> [@sinisa_lyh](https://twitter.com/sinisa_lyh)
+ Nikolas Evangelopoulos <nikolas@jkl.gr>
+ Oleg Aleshko <olegych@tut.by> [@OlegYch](https://twitter.com/OlegYch)
+ Olivier Blanvillain <olivier.blanvillain@gmail.com>
+ Olli Helenius <liff@iki.fi> [@ollijh](https://twitter.com/ollijh)
+ Owein Reese <owreese@gmail.com> [@OweinReese](https://twitter.com/OweinReese)
+ Paolo G. Giarrusso <p.giarrusso@gmail.com> [@blaisorblade](https://twitter.com/blaisorblade)
+ Pascal Voitot <pascal.voitot.dev@gmail.com> [@mandubian](https://twitter.com/mandubian)
+ Pavel Chlupacek <pavel.chlupacek@spinoco.com> [@pacmanius](https://twitter.com/pacmanius)
+ Peter Neyens <peter.neyens@gmail.com> [@pneyens](https://twitter.com/pneyens)
+ Peter Schmitz <petrischmitz@gmail.com> [@peterschmitz\_](https://twitter.com/peterschmitz_)
+ Renato Cavalcanti <renato@strongtyped.io> [@renatocaval](https://twitter.com/renatocaval)
+ Rob Norris <rob_norris@mac.com> [@tpolecat](https://twitter.com/tpolecat)
+ Robert Hensing <spam@roberthensing.nl>
+ Ronan Michaux <ronan_michaux@yahoo.com> [@ronan_michaux](https://twitter.com/ronan_michaux)
+ Ryadh Khsib <ryadh.khsib@gmail.com>
+ Ryo Hongo <ryoppy0516@gmail.com> [@ryoppy516](https://twitter.com/ryoppy516)
+ Sam Halliday <sam.halliday@gmail.com> [@fommil](https://twitter.com/fommil)
+ Sarah Gerweck <sarah.a180@gmail.com> [@SGerweck](https://twitter.com/SGerweck)
+ Sébastien Doeraene <sjrdoeraene@gmail.com> [@sjrdoeraene](https://twitter.com/sjrdoeraene)
+ Simon Hafner <hafnersimon@gmail.com> [@reactormonk](https://twitter.com/reactormonk)
+ Stacy Curl <stacy.curl@gmail.com> [@stacycurl](https://twitter.com/stacycurl)
+ Stanislav Savulchik <s.savulchik@gmail.com> [@savulchik](https://twitter.com/savulchik)
+ Stephen Compall <scompall@nocandysw.com> [@S11001001](https://twitter.com/S11001001)
+ Tin Pavlinic <tin.pavlinic@gmail.com> [@triggerNZ](https://twitter.com/triggerNZ)
+ Tom Switzer <thomas.switzer@gmail.com> [@tixxit](https://twitter.com/tixxit)
+ Tomas Mikula <tomas.mikula@gmail.com> [@tomas_mikula](https://twitter.com/tomas_mikula)
+ Travis Brown <travisrobertbrown@gmail.com> [@travisbrown](https://twitter.com/travisbrown)
+ Valentin Kasas <valentin.kasas@gmail.com> [@ValentinKasas](https://twitter.com/ValentinKasas)
+ Valerian Barbot <valerian.barbot@onzo.com> [@etaty](https://twitter.com/etaty)
+ Valy Diarrassouba <v.diarrassouba@gmail.com>
+ Vladimir Matveev <vladimir.matweev@gmail.com> [@netvlm](https://twitter.com/netvlm)
+ Vladimir Pavkin <vpavkin@gmail.com> [@vlpavkin](https://twitter.com/vlpavkin)
+ William Harvey <harveywi@cse.ohio-state.edu>
+ Yang Bo (杨博) <pop.atry@gmail.com> [@Atry](https://twitter.com/Atry)
+ Zainab Ali <zainab.ali.london@gmail.com> [@_zainabali_](https://twitter.com/_zainabali_)
