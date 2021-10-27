package com.snowplowanalytics.snowplow.eventgen

import cats.effect.{Async, Ref, Resource, Sync}
import cats.effect.std.Hotswap
import fs2.Stream
import fs2.io.file.{FileHandle, Files, Flags, Path}
import cats.implicits._
import java.nio.file.{Path => JPath}
import java.net.URI

abstract class RotatingFileSink[F[_]] {
  def write(in: Stream[F, Byte]): F[Unit]
}

object RotatingFileSink {
  def void[F[_] : Sync]: Resource[F, RotatingFileSink[F]] = Resource.eval {
    Sync[F].delay(new RotatingFileSink[F] {
      override def write(in: Stream[F, Byte]): F[Unit] = Sync[F].unit
    })
  }

  def apply[F[_] : Async](prefix: String, outputDir: URI, eventsPerFile: Int): Resource[F, RotatingFileSink[F]] =
    Hotswap.create[F, FileHandle[F]].flatMap { hs =>
      def fileHandle(id: Int): Resource[F, FileHandle[F]] = {
        val catDir = Path.fromNioPath(JPath.of(outputDir))
        val mkDir = Files[F].createDirectory(catDir).attempt.void
        Resource.eval(mkDir) >>
          Files[F].open(catDir.resolve(s"${prefix}_${pad(id)}"), Flags.Write)
      }

      def writeToHandle(in: Stream[F, Byte], fileHandle: FileHandle[F]): F[Unit] =
        in.chunks.evalTapChunk(
          chunk => fileHandle.size.flatMap(fileHandle.write(chunk, _))
        ).compile.drain


      Resource.eval {
        for {
          index <- Ref[F].of(0)
          count <- Ref[F].of(0)
          fh <- hs.swap(fileHandle(0))
          outputFh <- Ref[F].of(fh)
        } yield new RotatingFileSink[F] {
          override def write(in: Stream[F, Byte]): F[Unit] =
            count.get.flatMap { currentCount =>
              if (currentCount < eventsPerFile)
                for {
                  currentFile <- outputFh.get
                  _ <- writeToHandle(in, currentFile)
                  _ <- count.update(_ + 1)
                } yield ()
              else
                for {
                  _ <- count.set(1)
                  idx <- index.updateAndGet(_ + 1)
                  f <- hs.swap(fileHandle(idx))
                  _ <- outputFh.set(f)
                  _ <- writeToHandle(in, f)
                } yield ()
            }
        }
      }
    }

  private def pad(int: Int): String = {
    val zeros = "0".repeat(4 - int.toString.length)
    s"$zeros$int"
  }
}
