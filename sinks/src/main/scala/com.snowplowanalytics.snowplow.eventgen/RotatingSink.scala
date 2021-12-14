package com.snowplowanalytics.snowplow.eventgen

import cats.effect.{Ref, Resource, Sync}
import fs2.{INothing, Pipe, Stream}
import fs2.io.file.{Files, Flags, Path}
import cats.implicits._

import java.nio.file.{Path => JPath}
import java.net.URI
import blobstore.s3.S3Store
import blobstore.url.Url
import cats.effect.Async
import software.amazon.awssdk.services.s3.S3AsyncClient


abstract class RotatingSink[F[_]] {
  def write(in: Stream[F, Byte]): F[Unit]

  def checkpoint: F[Unit]
}

object RotatingSink {
  def void[F[_] : Sync]: Resource[F, RotatingSink[F]] = Resource.eval {
    Sync[F].delay(new RotatingSink[F] {
      override def checkpoint: F[Unit] = Sync[F].unit

      override def write(in: Stream[F, Byte]): F[Unit] = Sync[F].unit
    })
  }

  def apply[F[_] : Async, V](payloadsPerFile: Int, makePipe: Int => Pipe[F, Byte, V]): Resource[F, RotatingSink[F]] =
    Resource.make {
      for {
        index <- Ref[F].of(0)
        count <- Ref[F].of(0)
        pendingStreamRef <- Ref[F].of(Stream.emits(Seq.empty[Byte]).covary[F])
      } yield new RotatingSink[F] {

        override def checkpoint: F[Unit] = for {
          pipe <- index.updateAndGet(_ + 1).map(makePipe)
          pendingStream <- pendingStreamRef.get
          _ <- pendingStream.through(pipe).compile.drain
          _ <- count.set(1)
          _ <- pendingStreamRef.set(Stream.emits(Seq.empty[Byte]).covary[F])
        } yield ()


        override def write(in: Stream[F, Byte]): F[Unit] =
          count.get.flatMap(currentCount =>
            count.update(_ + 1) >> pendingStreamRef.update(_ ++ in).>>(
              if (currentCount >= payloadsPerFile) checkpoint else Sync[F].unit)
          )
      }
    }(_.checkpoint)


  def s3[F[_] : Async](prefix: String, outputDir: URI, payloadsPerFile: Int): Resource[F, RotatingSink[F]] = {
    val store: S3Store[F] = S3Store[F](S3AsyncClient.builder().build())

    def makeS3Sink: Int => Pipe[F, Byte, Unit] =
      (idx: Int) =>
        (in: Stream[F, Byte]) => {
          Stream.eval(Url.parseF[F](s"$outputDir/${prefix}_${pad(idx)}")).flatMap(url =>
            in.through(store.put(url, overwrite = true, None, None)))
        }

    RotatingSink(payloadsPerFile, makeS3Sink)
  }

  def file[F[_] : Async](prefix: String, outputDir: URI, payloadsPerFile: Int): Resource[F, RotatingSink[F]] = {
    val catDir = Path.fromNioPath(JPath.of(outputDir))

    def makeFileSink: Int => Pipe[F, Byte, INothing] =
      (idx: Int) =>
        (in: Stream[F, Byte]) => {
          Stream.eval(Files[F].createDirectory(catDir).attempt.void) >>
            in.through(Files[F].writeAll(catDir.resolve(s"${prefix}_${pad(idx)}"), Flags.Write))
        }

    RotatingSink(payloadsPerFile, makeFileSink)
  }

  private def pad(int: Int): String = {
    val zeros = "0".repeat(4 - int.toString.length)
    s"$zeros$int"
  }
}
