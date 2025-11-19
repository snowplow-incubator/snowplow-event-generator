/*
 * Copyright (c) 2021-2025 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.snowplowanalytics.snowplow.eventgen

import com.snowplowanalytics.snowplow.eventgen.protocol.common.ClusterAlgorithm
import org.specs2.mutable.Specification
import org.specs2.ScalaCheck
import org.scalacheck.{Gen, Prop}

/** Invariant tests for ClusterAlgorithm.
  *
  * Tests that verify determinism, consistency, and correctness properties that must always hold regardless of
  * configuration.
  */
class ClusterAlgorithmInvariantsSpec extends Specification with ScalaCheck {

  "Determinism" should {

    "same identity always produces same identifiers" >> {
      Prop.forAll(userIdGen, configGen) { (userId, config) =>
        val cookies1 = ClusterAlgorithm.generateIdentifiers(
          userId,
          "cookie",
          config.cookiesPerUser,
          config.numUsers,
          config.sharedIdentifierRate,
          config.usersPerCluster,
          config.numSharedCookies
        )
        val cookies2 = ClusterAlgorithm.generateIdentifiers(
          userId,
          "cookie",
          config.cookiesPerUser,
          config.numUsers,
          config.sharedIdentifierRate,
          config.usersPerCluster,
          config.numSharedCookies
        )
        val cookies3 = ClusterAlgorithm.generateIdentifiers(
          userId,
          "cookie",
          config.cookiesPerUser,
          config.numUsers,
          config.sharedIdentifierRate,
          config.usersPerCluster,
          config.numSharedCookies
        )

        (cookies1 must_== cookies2).and(cookies2 must_== cookies3)
      }
    }

    "cluster assignment is deterministic" >> {
      Prop.forAll(userIdGen, configGen) { (userId, config) =>
        val cluster1 = ClusterAlgorithm.getClusterId(
          userId,
          config.numUsers,
          config.sharedIdentifierRate,
          config.usersPerCluster
        )
        val cluster2 = ClusterAlgorithm.getClusterId(
          userId,
          config.numUsers,
          config.sharedIdentifierRate,
          config.usersPerCluster
        )
        val cluster3 = ClusterAlgorithm.getClusterId(
          userId,
          config.numUsers,
          config.sharedIdentifierRate,
          config.usersPerCluster
        )

        (cluster1 must_== cluster2).and(cluster2 must_== cluster3)
      }
    }

    "isInCluster is deterministic" >> {
      Prop.forAll(userIdGen, sharedIdentifierRateGen) { (userId, sharedIdentifierRate) =>
        val result1 = ClusterAlgorithm.isInCluster(userId, sharedIdentifierRate)
        val result2 = ClusterAlgorithm.isInCluster(userId, sharedIdentifierRate)
        val result3 = ClusterAlgorithm.isInCluster(userId, sharedIdentifierRate)

        (result1 must_== result2).and(result2 must_== result3)
      }
    }
  }

  "Identifier uniqueness" should {

    "identifiers from same identity are always distinct" >> {
      Prop.forAll(userIdGen, configGen) { (userId, config) =>
        val cookies = ClusterAlgorithm.generateIdentifiers(
          userId,
          "cookie",
          config.cookiesPerUser,
          config.numUsers,
          config.sharedIdentifierRate,
          config.usersPerCluster,
          config.numSharedCookies
        )

        cookies.distinct.size must_== cookies.size
      }
    }

    "unique identifiers are different for different identities" >> {
      val id1 = 42L
      val id2 = 99L

      val unique1 = ClusterAlgorithm.generateUniqueIdentifier(id1, "cookie", 0)
      val unique2 = ClusterAlgorithm.generateUniqueIdentifier(id2, "cookie", 0)

      unique1 must_!= unique2
    }

    "shared identifiers are same for identities in same cluster" >> {
      val clusterId = 123L

      val shared1 = ClusterAlgorithm.generateSharedIdentifier(clusterId, "cookie", 0)
      val shared2 = ClusterAlgorithm.generateSharedIdentifier(clusterId, "cookie", 0)

      shared1 must_== shared2
    }
  }

  "Cluster membership" should {

    "identities in same cluster have same clusterId" >> {
      val numUsers             = 1000L
      val sharedIdentifierRate = 0.3
      val usersPerCluster      = 5

      // Find an identity that's in a cluster
      val identityInCluster = (0L until 100L).find { id =>
        ClusterAlgorithm.isInCluster(id, sharedIdentifierRate)
      }.get

      val clusterId = ClusterAlgorithm
        .getClusterId(
          identityInCluster,
          numUsers,
          sharedIdentifierRate,
          usersPerCluster
        )
        .get

      // Find another identity in the same cluster
      val sameCluster = (0L until numUsers).find { id =>
        id != identityInCluster &&
        ClusterAlgorithm.getClusterId(id, numUsers, sharedIdentifierRate, usersPerCluster).contains(clusterId)
      }

      sameCluster must beSome
    }

    "isolated identities have no cluster" >> {
      Prop.forAll(userIdGen, configGen) { (userId, config) =>
        val isInCluster = ClusterAlgorithm.isInCluster(userId, config.sharedIdentifierRate)
        val clusterId = ClusterAlgorithm.getClusterId(
          userId,
          config.numUsers,
          config.sharedIdentifierRate,
          config.usersPerCluster
        )

        (!isInCluster) ==> (clusterId must beNone)
      }
    }

    "clustered identities have a cluster" >> {
      Prop.forAll(userIdGen, configGen) { (userId, config) =>
        val isInCluster = ClusterAlgorithm.isInCluster(userId, config.sharedIdentifierRate)
        val clusterId = ClusterAlgorithm.getClusterId(
          userId,
          config.numUsers,
          config.sharedIdentifierRate,
          config.usersPerCluster
        )

        isInCluster ==> (clusterId must beSome)
      }
    }
  }

  "Identifier sharing" should {

    "identities in same cluster share at least one identifier" >> {
      val numUsers             = 1000L
      val sharedIdentifierRate = 0.5
      val usersPerCluster      = 5
      val numShared            = 1
      val numIdentifiers       = 5

      // Find a cluster with at least 2 identities
      val clusterId = 5L
      val identitiesInCluster = (0L until numUsers)
        .filter { id =>
          ClusterAlgorithm.getClusterId(id, numUsers, sharedIdentifierRate, usersPerCluster).contains(clusterId)
        }
        .take(2)

      if (identitiesInCluster.size >= 2) {
        val id1 = identitiesInCluster(0)
        val id2 = identitiesInCluster(1)

        val cookies1 = ClusterAlgorithm.generateIdentifiers(
          id1,
          "cookie",
          numIdentifiers,
          numUsers,
          sharedIdentifierRate,
          usersPerCluster,
          numShared
        )
        val cookies2 = ClusterAlgorithm.generateIdentifiers(
          id2,
          "cookie",
          numIdentifiers,
          numUsers,
          sharedIdentifierRate,
          usersPerCluster,
          numShared
        )

        val shared = cookies1.toSet.intersect(cookies2.toSet)
        shared.size must beGreaterThan(0)
      } else {
        ok // Skip if we can't find a cluster with 2+ identities
      }
    }

    "isolated identities don't share with others in different cluster" >> {
      val numUsers             = 1000L
      val sharedIdentifierRate = 0.1
      val usersPerCluster      = 5
      val numShared            = 1
      val numIdentifiers       = 5

      // Find an isolated identity
      val isolatedId = (0L until numUsers).find { id =>
        !ClusterAlgorithm.isInCluster(id, sharedIdentifierRate)
      }

      // Find a clustered identity
      val clusteredId = (0L until numUsers).find { id =>
        ClusterAlgorithm.isInCluster(id, sharedIdentifierRate)
      }

      if (isolatedId.isDefined && clusteredId.isDefined) {
        val cookies1 = ClusterAlgorithm.generateIdentifiers(
          isolatedId.get,
          "cookie",
          numIdentifiers,
          numUsers,
          sharedIdentifierRate,
          usersPerCluster,
          numShared
        )
        val cookies2 = ClusterAlgorithm.generateIdentifiers(
          clusteredId.get,
          "cookie",
          numIdentifiers,
          numUsers,
          sharedIdentifierRate,
          usersPerCluster,
          numShared
        )

        val shared = cookies1.toSet.intersect(cookies2.toSet)
        shared.size must_== 0
      } else {
        ok // Skip if we can't find suitable identities
      }
    }
  }

  "Hash distribution" should {

    "distributes identities across multiple cluster buckets" >> {
      val numUsers             = 10000L
      val sharedIdentifierRate = 0.3
      val usersPerCluster      = 5

      val clusterIds = (0L until numUsers)
        .filter(id => ClusterAlgorithm.isInCluster(id, sharedIdentifierRate))
        .flatMap { id =>
          ClusterAlgorithm.getClusterId(id, numUsers, sharedIdentifierRate, usersPerCluster)
        }
        .distinct

      // Should create many clusters, not collapse into just a few
      clusterIds.size must beGreaterThan(100)
    }
  }

  "Collision resistance" should {

    "unique identifiers are actually unique across different identities" >> {
      val numUsers                  = 1000L
      val identifierType            = "cookie"
      val numIdentifiersPerIdentity = 3

      val allIdentifiers = (0L until numUsers).flatMap { id =>
        (0 until numIdentifiersPerIdentity).map { idx =>
          ClusterAlgorithm.generateUniqueIdentifier(id, identifierType, idx)
        }
      }

      // All should be unique (no collisions)
      allIdentifiers.distinct.size must_== allIdentifiers.size
    }
  }

  "Edge cases" should {

    "handles mergeRate = 0.0 (all isolated)" in {
      val numUsers             = 100L
      val sharedIdentifierRate = 0.0

      val clustered = (0L until numUsers).count { id =>
        ClusterAlgorithm.isInCluster(id, sharedIdentifierRate)
      }

      clustered must_== 0
    }

    "handles mergeRate = 1.0 (all clustered)" in {
      val numUsers             = 100L
      val sharedIdentifierRate = 1.0

      val clustered = (0L until numUsers).count { id =>
        ClusterAlgorithm.isInCluster(id, sharedIdentifierRate)
      }

      clustered must_== 100
    }

    "handles numUsers = 1" in {
      val userId               = 0L
      val numUsers             = 1L
      val sharedIdentifierRate = 0.5
      val usersPerCluster      = 5

      val clusterId = ClusterAlgorithm.getClusterId(
        userId,
        numUsers,
        sharedIdentifierRate,
        usersPerCluster
      )

      // With 1 identity, cluster ID should be 0 if clustered
      if (ClusterAlgorithm.isInCluster(userId, sharedIdentifierRate)) {
        clusterId must beSome(0L)
      } else {
        clusterId must beNone
      }
    }
  }

  // Test data generators
  lazy val userIdGen: Gen[Long]                 = Gen.choose(0L, 10000L)
  lazy val sharedIdentifierRateGen: Gen[Double] = Gen.choose(0.0, 1.0)

  case class TestConfig(
    numUsers: Long,
    sharedIdentifierRate: Double,
    usersPerCluster: Int,
    cookiesPerUser: Int,
    numSharedCookies: Int
  )

  lazy val configGen: Gen[TestConfig] = for {
    numUsers             <- Gen.choose(100L, 10000L)
    sharedIdentifierRate <- Gen.choose(0.0, 0.8)
    usersPerCluster      <- Gen.choose(2, 20)
    cookiesPerUser       <- Gen.choose(2, 10)
    numSharedCookies     <- Gen.choose(1, cookiesPerUser)
  } yield TestConfig(numUsers, sharedIdentifierRate, usersPerCluster, cookiesPerUser, numSharedCookies)
}
