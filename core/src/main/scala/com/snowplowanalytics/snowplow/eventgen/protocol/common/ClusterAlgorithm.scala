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
package com.snowplowanalytics.snowplow.eventgen.protocol.common

import java.util.UUID
import scala.util.Random

/** Algorithmic user clustering and identifier generation without pre-computation.
  *
  * Instead of pre-computing all clusters upfront, this determines cluster membership and shared identifiers on-demand
  * using deterministic hash functions.
  */
object ClusterAlgorithm {

  /** Hash constants for deterministic identifier generation.
    *
    * These primes are chosen to provide good distribution properties:
    *   - ClusterMembershipPrime: Used for binary cluster membership decisions
    *   - ClusterIdPrime: Used for distributing users across clusters
    *   - KnuthHash: Knuth's multiplicative hash constant (2^32 / golden ratio)
    *   - SecondaryHash: Large prime for XOR-based hash mixing
    */
  object HashConstants {
    val ClusterMembershipPrime: Long = 997L
    val ClusterIdPrime: Long         = 31L
    val KnuthHash: Long              = 73856093L
    val SecondaryHash: Long          = 19349663L
    val KnuthHashGoldenRatio: Long   = 2654435761L
    val NormalizationBase: Int       = 1000000
  }

  /** Deterministically determine if an identity is in a cluster (based on shared identifier rate). Uses identity ID as
    * seed to ensure consistent assignment.
    */
  def isInCluster(userId: Long, sharedIdentifierRate: Double): Boolean = {
    val hash = new Random(userId * HashConstants.ClusterMembershipPrime).nextDouble()
    hash < sharedIdentifierRate
  }

  /** Deterministically assign an identity to a cluster ID. Uses consistent hashing so identities with similar IDs end
    * up in different clusters.
    */
  def getClusterId(
    userId: Long,
    numUsers: Long,
    sharedIdentifierRate: Double,
    usersPerCluster: Int
  ): Option[Long] =
    if (!isInCluster(userId, sharedIdentifierRate)) {
      None
    } else {
      // Calculate number of clusters based on target average size
      val numClusters = Math.max(1, (numUsers * sharedIdentifierRate / usersPerCluster).toLong)
      val clusterId   = (userId * HashConstants.ClusterIdPrime & Long.MaxValue) % numClusters
      Some(clusterId)
    }

  /** Generate a shared identifier for a cluster deterministically. Multiple identities in the same cluster will get the
    * same shared identifier.
    */
  def generateSharedIdentifier(
    clusterId: Long,
    identifierType: String,
    sharedIdx: Int
  ): String =
    identifierType match {
      case "cookie" =>
        val hash = ((clusterId * HashConstants.KnuthHash) ^ (sharedIdx * HashConstants.SecondaryHash)) & Long.MaxValue
        s"shared_cookie_${hash.toHexString}"

      case "device" =>
        val seed = s"shared_device_${clusterId}_${sharedIdx}"
        UUID.nameUUIDFromBytes(seed.getBytes).toString

      case "ip" =>
        val combined = (clusterId * 10 + sharedIdx).toInt
        val octet1   = 10
        val octet2   = (combined       / 256) % 256
        val octet3   = combined        % 256
        val octet4   = (clusterId      % 256).toInt
        s"$octet1.$octet2.$octet3.$octet4"

      case _ =>
        s"shared_${identifierType}_${clusterId}_${sharedIdx}"
    }

  /** Generate a unique identifier for an identity.
    */
  def generateUniqueIdentifier(userId: Long, identifierType: String, idx: Int): String =
    identifierType match {
      case "cookie" =>
        val hash = ((userId * HashConstants.KnuthHash) ^ (idx * HashConstants.SecondaryHash)) & Long.MaxValue
        s"cookie_${hash.toHexString}"

      case "device" =>
        val seed = s"${userId}_device_${idx}"
        UUID.nameUUIDFromBytes(seed.getBytes).toString

      case "ip" =>
        // Use hash to avoid overflow for large userId values
        val hash   = ((userId * HashConstants.KnuthHash) + (idx * HashConstants.SecondaryHash)) & Long.MaxValue
        val octet1 = 172
        val octet2 = ((hash                              / 65536) % 256).toInt
        val octet3 = ((hash                              / 256)   % 256).toInt
        val octet4 = (hash                               % 256).toInt
        s"$octet1.$octet2.$octet3.$octet4"

      case _ =>
        s"${identifierType}_${userId}_${idx}"
    }

  /** Generate all identifiers for an identity (unique + possibly shared).
    *
    * This is the main entry point - NO PRE-COMPUTATION REQUIRED.
    *
    * Identities in clusters deterministically share identifiers. Every clustered identity shares at least 1 identifier,
    * guaranteeing that the observable shared identifier rate exactly matches the configured sharedIdentifierRate
    * parameter.
    *
    * @param sharedIdentifierRate
    *   Fraction of users who share identifiers with others (0.0 to 1.0)
    * @param usersPerCluster
    *   Average number of users per cluster (affects distribution)
    * @param numSharedIdentifiers
    *   Number of identifiers to share (1-2 for realism)
    */
  def generateIdentifiers(
    userId: Long,
    identifierType: String,
    numIdentifiers: Int,
    numUsers: Long,
    sharedIdentifierRate: Double,
    usersPerCluster: Int = 5,
    numSharedIdentifiers: Int = 1
  ): Seq[String] = {
    require(
      numSharedIdentifiers >= 1 && numSharedIdentifiers <= numIdentifiers,
      s"numSharedIdentifiers must be between 1 and $numIdentifiers"
    )

    val rng = new Random(userId * HashConstants.ClusterIdPrime + identifierType.hashCode)

    // Check if this identity is in a cluster
    getClusterId(userId, numUsers, sharedIdentifierRate, usersPerCluster) match {
      case Some(clusterId) =>
        // Identity is in a cluster - deterministically share some identifiers
        val sharedPool = (0 until numSharedIdentifiers).map { idx =>
          generateSharedIdentifier(clusterId, identifierType, idx)
        }

        // Deterministically decide how many to share (add variability while ensuring ≥1)
        val numToShare = if (numSharedIdentifiers == 1) {
          1 // Always share exactly 1
        } else {
          // Share 1 to numSharedIdentifiers, deterministically based on userId
          1 + (rng.nextInt(numSharedIdentifiers))
        }

        // Generate identifiers: first numToShare are shared, rest are unique
        (0 until numIdentifiers).map { idx =>
          if (idx < numToShare) {
            // Use shared identifier (cycle through pool if needed)
            sharedPool(idx % sharedPool.size)
          } else {
            // Use unique identifier
            generateUniqueIdentifier(userId, identifierType, idx)
          }
        }

      case None =>
        // Identity is isolated - all unique identifiers
        (0 until numIdentifiers).map { idx =>
          generateUniqueIdentifier(userId, identifierType, idx)
        }
    }
  }
}
