package com.nullhappens.fs2


object Basics extends App {
  import fs2.Stream
  import cats.effect.IO
  val s0 = Stream.empty
  val s1 = Stream.emit(1)
  val s1a = Stream(1, 2, 3)
  val s1b = Stream.emits(List(1, 2, 3))

  s1.toList
  s1.toVector

  (Stream(1, 2, 3) ++ Stream(4, 5)).toList
  Stream(1, 2, 3).map(_ + 1).toList
  Stream(1, 2, 3).filter(_ % 2 != 0).toList
  Stream(1, 2, 3).fold(0)(_ + _).toList
  Stream(None, Some(2), Some(3)).collect{ case Some(i) => i }.toList
  Stream.range(0,5).intersperse(42).toList
  Stream(1, 2, 3).flatMap(i => Stream(i, i)).toList
  Stream(1, 2, 3).repeat.take(9).toList

  val eff = Stream.eval(IO { println("Being Run!"); 1 + 1 })
  // def eval[F[_], A](f: F[A]): Stream[F, A]
  val ra = eff.compile.toVector // gather all output into a vector
  val rb = eff.compile.drain  // purely for effects
  val rc = eff.compile.fold(0)(_ + _) // run and accumulate some result

  ra.unsafeRunSync()
  rb.unsafeRunSync()
  rc.unsafeRunSync()

  import fs2.Chunk

  val s1c = Stream.chunk(Chunk.doubles(Array(1.0, 2.0, 3.0)))
  s1c.mapChunks{ ds =>
    val doubles = ds.toDoubles
    doubles
  }

  val appendEx1 = Stream(1, 2, 3) ++ Stream.emit(42)
  val appendEx2 = Stream(1, 2, 3) ++ Stream.eval(IO.pure(4))

  appendEx1.toVector
  appendEx2.compile.toVector.unsafeRunSync()
  appendEx1.map(_ + 1).toList
  appendEx1.flatMap(i => Stream.emits(List(i, i))).toList

  // Error handling
  val err = Stream.raiseError[IO](new Exception("FAIL!"))
  val err2 = Stream(1, 2, 3) ++ (throw new Exception("FAIL!"))
  val err3 = Stream.eval(IO(throw new Exception("Error in effect!")))

  // the dumb way
  try err.compile.toList.unsafeRunSync() catch { case e: Exception => println(e) }
  try err2.toList catch { case e: Exception => println(e)}
  try err3.compile.drain.unsafeRunSync catch { case e: Exception => println(e) }

  // the nicer way
  err.handleErrorWith{ e => Stream.emit(e.getMessage)}
    .compile
    .toList
    .unsafeRunSync()

  // Resource cleanup (don't do the above if you need to cleanup)
  val count = new java.util.concurrent.atomic.AtomicLong(0)
  val acquire = IO { println("incremented: " + count.incrementAndGet()); ()}
  val release = IO { println("decremented: " + count.decrementAndGet()); ()}

  val b1 = Stream.bracket(acquire)(_ => release)
    .flatMap(_ => Stream(1, 2, 3) ++ err)
    .attempt
    .compile
    .toList
    .unsafeRunSync()

  println(b1)
  println(count.get())
}
