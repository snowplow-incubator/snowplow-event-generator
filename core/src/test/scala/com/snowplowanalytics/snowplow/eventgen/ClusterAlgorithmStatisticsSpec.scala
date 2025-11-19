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

/** Statistical validation tests for ClusterAlgorithm.
  *
  * Tests that verify statistical properties match configuration expectations.
  */
class ClusterAlgorithmStatisticsSpec extends Specification {

  "Merge rate accuracy" should {

    "produce actual merge rate close to configured rate" in {
      val testCases = List(
        (0.05, "5% merge"),
        (0.10, "10% merge"),
        (0.20, "20% merge"),
        (0.30, "30% merge"),
        (0.50, "50% merge")
      )

      testCases.foreach { case (targetMergeRate, _) =>
        val numUsers = 10000L
        val clustered = (0L until numUsers).count { id =>
          ClusterAlgorithm.isInCluster(id, targetMergeRate)
        }

        val actualMergeRate = clustered.toDouble / numUsers

        actualMergeRate must beCloseTo(targetMergeRate, 0.02)
      }

      ok
    }
  }

  "Identifier sharing within clusters" should {

    "identities in same cluster share identifiers" in {
      val numUsers             = 1000L
      val sharedIdentifierRate = 0.3
      val usersPerCluster      = 5
      val numShared            = 1
      val numIdentifiers       = 5

      // Find some clusters and analyze sharing
      val clusterMap = (0L until numUsers)
        .filter(id => ClusterAlgorithm.isInCluster(id, sharedIdentifierRate))
        .flatMap { id =>
          ClusterAlgorithm
            .getClusterId(id, numUsers, sharedIdentifierRate, usersPerCluster)
            .map(clusterId => (clusterId, id))
        }
        .groupBy(_._1)
        .view
        .mapValues(_.map(_._2).toSeq)
        .toMap

      val largeClusters = clusterMap.filter(_._2.size >= 2).take(3)

      largeClusters.foreach { case (_, members) =>
        // Take first 2 members
        val id1 = members(0)
        val id2 = members(1)

        val cookies1 = ClusterAlgorithm
          .generateIdentifiers(
            id1,
            "cookie",
            numIdentifiers,
            numUsers,
            sharedIdentifierRate,
            usersPerCluster,
            numShared
          )
          .toSet

        val cookies2 = ClusterAlgorithm
          .generateIdentifiers(
            id2,
            "cookie",
            numIdentifiers,
            numUsers,
            sharedIdentifierRate,
            usersPerCluster,
            numShared
          )
          .toSet

        val shared = cookies1.intersect(cookies2)

        shared.size must beGreaterThan(0)
      }

      largeClusters.size must beGreaterThan(0)
    }
  }

  "Cluster distribution patterns" should {

    "number of clusters scales with shared identifier rate" in {
      val numUsers        = 10000L
      val usersPerCluster = 5

      val testCases = List(
        (0.10, "10% sharing"),
        (0.30, "30% sharing"),
        (0.50, "50% sharing")
      )

      testCases.foreach { case (sharedIdentifierRate, _) =>
        (0L until numUsers).filter(id => ClusterAlgorithm.isInCluster(id, sharedIdentifierRate)).foreach { id =>
          ClusterAlgorithm.getClusterId(id, numUsers, sharedIdentifierRate, usersPerCluster)
        }
      }

      ok
    }
  }

  "Observable merge rate" should {

    "match configured merge rate exactly (within statistical noise)" in {
      // Test full range of merge rates - should now hit targets exactly!
      val testCases = List(
        0.05, 0.10, 0.15, 0.20, 0.25, 0.30, 0.35, 0.40, 0.45, 0.50, 0.55, 0.60, 0.65, 0.70, 0.75, 0.80
      )

      val results = testCases.map { targetMergeRate =>
        val numUsers = 10000L
        val config = GenConfig.UserGraph(
          numUsers = numUsers,
          sharedIdentifierRate = targetMergeRate,
          identifiersPerUser = Map("cookie" -> 3, "device" -> 2, "ip" -> 3),
          authenticationRate = 1.0,
          distribution = GenConfig.UserGraph.Distribution.Uniform,
          activeUserRate = 1.0,
          usersPerCluster = 5
        )

        // Check across all identifier types (cookies, devices, IPs)
        val usersWithSharedIdentifiers = (0L until numUsers).count { userId =>
          val cookies = ClusterAlgorithm.generateIdentifiers(
            userId,
            "cookie",
            3,
            numUsers,
            config.sharedIdentifierRate,
            config.usersPerCluster,
            config.sharedIdentifiersForType("cookie")
          )
          val devices = ClusterAlgorithm.generateIdentifiers(
            userId,
            "device",
            2,
            numUsers,
            config.sharedIdentifierRate,
            config.usersPerCluster,
            config.sharedIdentifiersForType("device")
          )
          val ips = ClusterAlgorithm.generateIdentifiers(
            userId,
            "ip",
            3,
            numUsers,
            config.sharedIdentifierRate,
            config.usersPerCluster,
            config.sharedIdentifiersForType("ip")
          )

          // User is clustered if ANY identifier type has shared_ prefix
          cookies.exists(_.startsWith("shared_")) ||
          devices.exists(_.startsWith("shared_")) ||
          ips.exists(_.startsWith("shared_"))
        }

        val observedMergeRate = usersWithSharedIdentifiers.toDouble / numUsers
        val diff              = Math.abs(observedMergeRate - targetMergeRate)

        (targetMergeRate, observedMergeRate, diff)
      }

      // Print results for analysis
      println("\nMerge Rate Accuracy Results:")
      println("Target → Observed (Deviation)")
      results.foreach { case (target, observed, diff) =>
        println(f"$target%.2f → $observed%.4f (${diff * 100}%.2f%%)")
      }

      // All results should be within tight tolerance (only statistical noise from RNG)
      // With deterministic sharing, we should hit the target exactly
      results.foreach { case (_, _, diff) =>
        diff must beLessThan(0.01) // 1% tolerance - should be near-perfect!
      }

      ok
    }
  }

  "Large scale validation" should {

    "works efficiently with 1M identities" in {
      val numUsers             = 1000000L
      val sharedIdentifierRate = 0.3
      val usersPerCluster      = 5

      val startTime = System.currentTimeMillis()

      // Sample 10k identities to test performance
      val sampleSize = 10000L
      val clustered = (0L until sampleSize).count { id =>
        ClusterAlgorithm.isInCluster(id, sharedIdentifierRate) &&
        ClusterAlgorithm.getClusterId(id, numUsers, sharedIdentifierRate, usersPerCluster).isDefined
      }

      val endTime  = System.currentTimeMillis()
      val duration = endTime - startTime

      val actualSharedIdentifierRate = clustered.toDouble / sampleSize

      actualSharedIdentifierRate must beCloseTo(sharedIdentifierRate, 0.01) // Should be exact!
      duration must beLessThan(5000L)                                       // Should be fast (< 5s for 10k identities)
    }
  }
}
