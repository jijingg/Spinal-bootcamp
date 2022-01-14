package exercises

import org.scalatest.funsuite.AnyFunSuite

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.sim._

class WeightedArbiterTest extends AnyFunSuite {
  test("weighted arbiter test") {
    val numReq = 8
    val weightWidth = 4
    val compiled = SimConfig.withWave.allOptimisation.compile(
      new WeightedArbiter(numReq, weightWidth)
    )
    .doSim { dut =>
      dut.clockDomain.forkStimulus(5)

      dut.io.clear #= true
      val numNonZeroWeights = (0 until numReq).filter(_ > 0).length
      for (idx <- 0 until numReq) {
        dut.io.weights(idx) #= idx
      }
      dut.io.reqs #= 0

      dut.clockDomain.waitSampling(3)
      dut.io.clear #= false

      val reqVal = scala.math.pow(2, numReq).toInt - 1
      dut.clockDomain.waitSampling(3)
      dut.io.reqs #= reqVal - 1

      for (itr <- 0 until numReq) {
        dut.clockDomain.waitSampling()
        dut.io.reqs #= dut.io.reqs.toInt - dut.io.grant.toInt
        if (dut.io.reqs.toInt > 0) {
          assert(
            dut.io.grant.toInt == (1 << (itr + 1)),
            s"grant=${dut.io.grant.toInt} not match expected ${1 << (itr + 1)} when itr=$itr"
          )
        }
      }

      val numRemainingNonZeroWeights = numNonZeroWeights - 1
      dut.io.reqs #= reqVal
      for {
        numNonZeroWeightReqs <- (numRemainingNonZeroWeights until 0 by -1)
        bitIdx <- 0 until numNonZeroWeightReqs
      } {
        dut.clockDomain.waitSampling()
        val grantBitIdx = bitIdx + numReq - numNonZeroWeightReqs
        // println(s"grant=${dut.io.grant.toInt}, numNonZeroWeightReqs=$numNonZeroWeightReqs, grantBitIdx=$grantBitIdx")
        val expectedGrant = 1 << grantBitIdx
        assert(
          dut.io.grant.toInt == expectedGrant,
          s"grant=${dut.io.grant.toInt} not match expected $expectedGrant"
        )
      }

      for {
        itr <- 0 until 3
        numNonZeroWeightReqs <- (numNonZeroWeights until 0 by -1)
        bitIdx <- 0 until numNonZeroWeightReqs
      } {
        dut.clockDomain.waitSampling()
        val grantBitIdx = bitIdx + numReq - numNonZeroWeightReqs
        // println(s"grant=${dut.io.grant.toInt}, itr=$itr, numNonZeroWeightReqs=$numNonZeroWeightReqs, grantBitIdx=$grantBitIdx")
        val expectedGrant = 1 << grantBitIdx
        assert(
          dut.io.grant.toInt == expectedGrant,
          s"grant=${dut.io.grant.toInt} not match expected $expectedGrant"
        )
      }
    }
  }
}
