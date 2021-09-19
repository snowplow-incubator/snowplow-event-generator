package com.snowplowanalytics.snowplow.eventgen

import scala.util.Random

import java.nio.file.Paths

import cats.implicits._

import fs2.{ Stream, Pipe }
import fs2.compression.gzip
import fs2.io.file.{createDirectory, writeAll}

import cats.effect.{IO, IOApp, ExitCode, Blocker, Sync, ConcurrentEffect, ContextShift}
import cats.effect.concurrent.Ref

import blobstore.s3.S3Store
import blobstore.url.Url

// import software.amazon.awssdk.services.s3.S3AsyncClient

import com.snowplowanalytics.snowplow.analytics.scalasdk.Event

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    eventStreamPar("foo", GeneratorConfig(1000, 400, 5000, Duplicates.Distribution.Default)).as(ExitCode.Success)

  case class GeneratorConfig(total: Int, minPerFile: Int, maxPerFile: Int, duplicates: Duplicates.Distribution)

  def pad(int: Int): String = {
    val zeros = "0".repeat(4 - int.toString.length)
    s"$zeros$int"
  }

  /** Create a stream */
  def createSink(config: GeneratorConfig, getName: IO[String], totalAfter: Ref[IO, Int]): IO[(Int, String, Stream[IO, Event])] =
    for {
      perFile  <- IO(Random.between(config.minPerFile, config.maxPerFile))
      totalNow <- totalAfter.updateAndGet(_ + perFile)
      name     <- getName
    } yield (totalNow, name, EnrichedEventGen.eventStream.take(perFile.toLong))


  def eventStreamPar(dir: String, cfg: GeneratorConfig): IO[Unit] =
    Blocker[IO].use { blocker =>
      for {
        cpus         <- IO(Runtime.getRuntime.availableProcessors)  // RNG is CPU-bound
        fileCounter  <- Ref.of[IO, Int](0)
        totalCounter <- Ref.of[IO, Int](0)    // Every stream before starting increases this with 
        // store         = S3Store[IO](S3AsyncClient.builder().build())
                                              // an amount of events it'd like to write
        getName       = fileCounter.updateAndGet(_ + 1).map(pad).map(i => s"part-$i.gz")
        sink          = Stream.repeatEval(createSink(cfg, getName, totalCounter))
                              .collectWhile { 
                                case (cardinality, path, stream) if cardinality < cfg.total =>
                                  stream.map(_.toTsv)
                                        .intersperse("\n")
                                        .through(fs2.text.utf8Encode)
                                        .through(gzip[IO]())
                                        // .through(writeBlob[IO](store, dir, path))
                                        .through(writeFile[IO](dir, path, blocker))
                              }
                              .parJoin(cpus)
        _            <- sink.compile.drain
      } yield ()
    }

  def writeFile[F[_]: Sync: ContextShift](base: String, fileName: String, blocker: Blocker): Pipe[F, Byte, Unit] =
    (in: Stream[F, Byte]) => {
      val mkDir = createDirectory[F](blocker, Paths.get(base)).attempt.as(Paths.get(base).toAbsolutePath)
      Stream.eval(mkDir).flatMap { dir =>
        in.through(writeAll[F](Paths.get(s"$dir/$fileName"), blocker))
      }
    }
  

  def writeBlob[F[_]: ConcurrentEffect](store: S3Store[F], base: String, fileName: String): Pipe[F, Byte, Unit] =
    (in: Stream[F, Byte]) => 
      Stream.eval(Url.parseF[F](s"$base/$fileName")).flatMap { url =>
        in.through(store.put(url, true, None, None))
      }.onFinalize(ConcurrentEffect[F].delay(println(s"Done with $fileName")))
}
