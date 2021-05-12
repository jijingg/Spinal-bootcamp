package exercises

import org.scalatest.funsuite.AnyFunSuite

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.sim._

class FifoCCTest extends AnyFunSuite {
  test("async fifo test") {
    FifoCCSim.main(null)
  }

  test("sync fifo test") {
    val cd = ClockDomain.external("clk")
    val compiled = SimConfig.withWave.compile(
      rtl = new FifoCC(
        dataType = Bits(32 bits),
        depth = 16,
        pushClock = cd,
        popClock = cd
      )
    )

    // Run the simulation
    compiled.doSim(FifoCCSim.runSimulation(_))
  }
}
