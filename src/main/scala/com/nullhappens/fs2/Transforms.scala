package com.nullhappens.fs2
import fs2._

object Transforms extends App {

  // Implement take with scanChunkOpts
  def tk[F[_], O](n: Long): Pipe[F, O, O] =
    in => in.scanChunksOpt(n){ n =>
      if (n <= 0) None
      else Some(c => c.size match {
        case m if m < n => (n - m, c)
        case m => (0, c.take(n.toInt))
      })
    }

  println(Stream(1, 2, 3).through(tk(2)).toList)

  // Implement take with Pull
  def tk2[F[_], O](n: Long): Pipe[F, O, O] = {
    def go(s: Stream[F, O], n: Long): Pull[F, O, Unit] = {
      s.pull.uncons.flatMap{
        case Some((hd, tl)) =>
          hd.size match {
            case m if m <= n => Pull.output(hd) >> go(tl, n - m)
            case _ => Pull.output(hd.take(n.toInt)) >> Pull.done
          }
        case None => Pull.done
      }
    }
    in => go(in, n).stream
  }

  println(Stream(1, 2, 3, 4).through(tk2(2)).toList)

}
