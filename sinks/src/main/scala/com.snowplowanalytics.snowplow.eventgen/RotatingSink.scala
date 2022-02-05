package com.snowplowanalytics.snowplow.eventgen

import fs2.{INothing, Pipe, Pull, Stream}
import fs2.io.file.{Files, Flags, Path}
import fs2.concurrent.Channel

import java.nio.file.{Path => JPath}
import java.net.URI
import blobstore.s3.S3Store
import blobstore.url.Url
import cats.effect.Async
import cats.syntax.all._
import software.amazon.awssdk.services.s3.S3AsyncClient


object RotatingSink {

  def rotate[F[_]: Async, A](max: Int)(toPipe: Int => Pipe[F, A, INothing]): Pipe[F, A, INothing] = {
    def newChannel = Channel.synchronous[F, A]

    def go(in: Stream[F, A],
           aCount: Int,
           streamCount: Int,
           outputs: Channel[F, Stream[F, INothing]],
           chan: Channel[F, A]): Pull[F, INothing, Unit] =
      in.pull.uncons1.flatMap {
        case None => Pull.eval(outputs.close *> chan.close) *> Pull.done
        case Some((head, rest)) =>
          for {
            streamCount <- if (aCount == 0)
                Pull.eval {
                  outputs
                    .send(chan.stream.through(toPipe(streamCount + 1)))
                    .as(streamCount + 1)
                } else Pull.pure(streamCount)
            _ <- Pull.eval(chan.send(head))
            aCount <- Pull.pure(aCount + 1)
            chan <- if (aCount == max) Pull.eval(chan.close *> newChannel) else Pull.pure(chan)
            aCount <- if (aCount == max) Pull.pure(0) else Pull.pure(aCount)
            _ <- go(rest, aCount, streamCount, outputs, chan)
          } yield ()
      }

    in =>
      (for {
        outputs <- Stream.eval(Channel.synchronous[F, Stream[F, INothing]])
        current <- Stream.eval(newChannel)
      } yield go(in, 0, 0, outputs, current).stream.merge(outputs.stream.flatMap(identity[Stream[F, INothing]])))
      .flatMap(identity[Stream[F, INothing]])
  }

  def s3[F[_]: Async](prefix: String, idx: Int, outputDir: URI): Pipe[F, Byte, INothing] =
    in =>
      Stream.eval(Url.parseF[F](s"$outputDir/${prefix}_${pad(idx)}")).flatMap { url =>
        val store = S3Store[F](S3AsyncClient.builder().build())
        in.through(store.put(url, overwrite = true, None, None))
      }.drain

  def file[F[_]: Async](prefix: String, idx: Int, outputDir: URI): Pipe[F, Byte, INothing] = {
    val catDir = Path.fromNioPath(JPath.of(outputDir))
    _.through(Files[F].writeAll(catDir.resolve(s"${prefix}_${pad(idx)}"), Flags.Write))
  }

  private def pad(idx: Int): String = f"$idx%04d"
}
