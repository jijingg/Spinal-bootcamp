package exercises

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.sim._

import scala.collection.mutable
import scala.util.Random

object FifoCCSim {
  def runSimulation[T <: BitVector](dut: FifoCC[T]): Unit = {
    SimTimeout(1000 * 1000)
    val queueModel = mutable.Queue[Long]()

    dut.io.wEn #= false
    dut.io.rEn #= false

    // Create asynchronous clocks
    dut.pushClock.forkStimulus(2)
    sleep(17)
    dut.popClock.forkStimulus(5)
    sleep(100)

    // Do the resets
    dut.pushClock.assertReset()
    dut.popClock.assertReset()
    sleep(10)
    dut.pushClock.deassertReset()
    dut.popClock.deassertReset()
    sleep(100)

    // Push data randomly, and fill the queueModel with pushed transactions
    val pushThread = fork {
      while (true) {
        dut.io.wEn.randomize()
        dut.io.wData.randomize()
        dut.pushClock.waitSampling()

        if (dut.io.wEn.toBoolean && !dut.io.wFull.toBoolean) {
          queueModel.enqueue(dut.io.wData.toLong)
        }
        dut.io.wEn #= false
      }
    }

    var readMatch = 0
    // Pop data randomly, and check that it match with the queueModel
    val popThread = fork {
      dut.pushClock.waitSampling(20)
      while (true) {
        dut.io.rEn.randomize()
        dut.popClock.waitSampling()

        if (dut.io.rEn.toBoolean && !dut.io.rEmpty.toBoolean) {
          assert(dut.io.rData.toLong == queueModel.dequeue())
          readMatch += 1
        }
      }
    }

    waitUntil(readMatch > 50000)
    simSuccess()
  }

  def main(args: Array[String]): Unit = {
    // Compile the Component for the simulator
    val compiled = SimConfig.withWave.allOptimisation.compile {
      val dut = new FifoCC(
        dataType = Bits(32 bits),
        depth = 32,
        pushClock = ClockDomain.external("clkPush"),
        popClock = ClockDomain.external("clkPop")
      )
      // dut.pushArea.wAddr.simPublic()
      // dut.pushArea.rAddrInPushArea.simPublic()
      // dut.popArea.rAddr.simPublic()
      // dut.popArea.wAddrInPopArea.simPublic()
      dut
    }

    // Run the simulation.
    compiled.doSim(runSimulation(_))
  }
}
