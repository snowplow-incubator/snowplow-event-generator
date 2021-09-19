package com.snowplowanalytics.snowplow.eventgen

import cats.effect.{ IO, Resource }
import cats.effect.concurrent.Ref
import cats.implicits._

import java.util.UUID

import org.scalacheck.Gen

import com.snowplowanalytics.snowplow.eventgen.Duplicates.Duplicate.No
import com.snowplowanalytics.snowplow.eventgen.Duplicates.Duplicate.Synthetic
import com.snowplowanalytics.snowplow.eventgen.Duplicates.Duplicate.Natural


/**
  * Logic responsible for duplicate (event id + fingerprint) generation.
  *
  * Duplicate generation is very special because contrary to other generators
  * it doesn't imply randomness, instead we need a "controlled randomness",
  * i.e. for every generation of a `Pair` we need to decide if it's unique or not.
  * At the same time, we must stick to the expected numbers - if user specified that
  * they need 5% of natural duplicates - our controlled randomness has to be skewing
  * to match the expectation, although no precise results guaranteed
  *
  * This module pre-generates a subset of pairs that will be used as duplicates.
  * Then during event-generation the generator decides whether the event is going to be a
  * duplicate, and if it is then it gets the pair from the pre-generated set (only id if
  * it's a synthetic duplicate and whole pair if it's a natural dupe)
  *
  * Deduplication:
  * Input:  [(x 1), (x 1), (x 2), (x 3)]
  * Output: [(x 1),        (y 2), (z 3)]
  */
object Duplicates {

  /** 32-char string */
  type Hash = String

  /** 
   * Count for every pair, i.e. how many natural and syntetic duplicate
   * the pair has in the dataset already
   * Used mostly for better distribution, but curently the implementation
   * is too fair and too slow
   */
  final case class Count(natural: Int, synthetic: Int) {
    def plusNatural: Count =
      Count(natural + 1, synthetic)
    def plusSynthetic: Count =
      Count(natural, synthetic + 1)
  }

  /** 
   * A pair that can be re-used across several events to make them duplicates 
   * For synthetic duplicates only `id` is reused, `hash` will be unique
   */
  case class Pair(id: UUID, hash: Hash)

  /**
   * Pregenerate a set of pairs that will be used as duplicates
   * It's a `Ref` because this set will be mutated every time an event
   * generator decides to make an event a duplicate
   *
   * @param cardinality amount of pairs, i.e. total amount of duplicates,
   *                    both natural and synthetic. All natural duplicates,
   *                    i.e. ones with `Count(n, m) if n > 1` must be squashed
   *                    into `max(1, m)` during deduplication process. Ones with 
   *                    `Count(_, m) if m > 1` will remain `m` but with new ids.
   *                    Natural deduplication runs first, so `Count(2, 3)`
   *                    will result into three unique events
   */
  def pregeneratePairs(cardinality: Int): Resource[IO, Ref[IO, State]] = {
    val pairs = runGen(genPair)
      .replicateA(cardinality)
      .map { pairs => pairs.map(pair => pair -> Count(0, 0)).toMap }
      .flatMap { pairs => Ref.of[IO, State](State(pairs, false)) }

    Resource.eval(pairs)
  }

  /** Convert `Gen` into `IO` */
  def runGen[A](gen: Gen[A]): IO[A] = {
    def go(attempt: Int): IO[A] =
      if (attempt >= 5) IO.raiseError(new RuntimeException("Couldn't generate a pair after several attempts"))
      else IO(gen.sample).flatMap {
        case Some(a) => IO.pure(a)
        case None => go(attempt + 1)
      }

    go(1)
  }

  val genHash: Gen[Hash] = Gen.listOfN(32, Gen.frequency((3, Gen.numChar), (9, Gen.choose(97.toChar, 102.toChar)))).map(_.mkString)
  val genPair: Gen[Pair] = Gen.uuid.flatMap { id => genHash.flatMap { hash => Pair(id, hash) } }


  /**
    * Get a pair of event id and fingerprint, according to specified distribution
    *
    * @param state pre-generated set of duplicate pairs
    * @param config distribution settings
    * @param total total amount of events in the dataset being generated
    *
    */
  def generatePair(state: Ref[IO, State], config: Distribution, total: Int): IO[Pair] = {
    val distribution = Distribution.build(config, total)
    for {
      kind <- runGen(distribution)
      pair <- getPair(state, kind)
    } yield pair
  }

  /**
    *
    *
    * @param pairs
    * @param isFull means that all events were pregenerated, i.e. count is >0 for all pairs
    */
  case class State(pairs: Map[Pair, Count], isFull: Boolean) {
    def plus(pair: Pair, f: Count => Count): State = {
      val updated = pairs.get(pair) match {
        case Some(count) => pairs.updated(pair, f(count))
        case None => throw new IllegalStateException(s"Pair $pair is not found in $pairs")
      }
      State(updated, isFull)
    }
  }

  /** 
   * Duplicate distribution settings
   * @param natPerc percentage of natural duplicates in the whole dataset (0 to 100)
   * @param synPerc percentage of synthetic duplicates in the whole dataset (0 to 100)
   * @param totalDupes exact cardinality of pre-generated duplicate set, e.g. for a dataset
   *                   of 100 events natPerc=10,totalDupes=10 will lean towards 10 duplicates
   *                   each of which is encountered only twice in the dataset, whereas
   *                   natPerc=10,totalDupes=1 will result into 1 event encountered 11 times
   */
  case class Distribution(natPerc: Int, synPerc: Int, totalDupes: Int)

  object Distribution {
    /**
     * Typical distribution
     * 5% of natural dupes
     * 3% of synthetic dupes
     * 100 unique duplicates
     */
    val Default = Distribution(5, 3, 100)

    /** Build according to the total amount of events */
    def build(config: Distribution, totalEvents: Int): Gen[Duplicate] =
      config match {
        case Distribution(natPerc, synPerc, _) =>
          val howManyNatDupes = (totalEvents / 100) * natPerc
          val howManySynDupes = (totalEvents / 100) * synPerc
          Gen.frequency(howManyNatDupes -> Duplicate.Natural,
                        howManySynDupes -> Duplicate.Synthetic,
                        (totalEvents - howManyNatDupes.max(howManySynDupes)) -> Duplicate.No)
        }

  }

  /** Kind of a duplicate */
  sealed trait Duplicate

  object Duplicate {
    /** Completely unique event */
    case object No extends Duplicate
    /** Event with "known" event id, but unique fingerprint */
    case object Synthetic extends Duplicate
    /** Event with "known" event id and fingerprint */
    case object Natural extends Duplicate
  }

  /** Pull a pair from duplicate set or generate new pair */
  def getPair(state: Ref[IO, State], kind: Duplicate): IO[Pair] =
    kind match {
      case No => runGen(genPair)
      case Synthetic => 
        val id = state.modify { s =>
          // TODO: This distribution has following problems:
          //       * It's very uniform
          //       * Every synthetic duplicate has natural duplicates
          //       * It's slow
          val (pair, _) = s.pairs.minBy { case (_, count) => count.synthetic }
          (s.plus(pair, _.plusSynthetic), pair.id)
        }
        id.flatMap { id => runGen(genHash).map { hash => (Pair(id, hash)) } }
      case Natural => state.modify { s =>
        val (pair, _) = s.pairs.minBy { case (_, count) => count.natural }
        (s.plus(pair, _.plusNatural), pair)
      }
    }

}

