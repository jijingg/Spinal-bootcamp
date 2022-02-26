package exercises

import org.scalatest.funsuite.AnyFunSuite

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.bus.amba3.apb._
import spinal.sim._

class ApbBridgeTest extends AnyFunSuite {
  test("async bridge test") {
    ApbBridgeSim.main(null)
  }

  test("sync bridge test") {
    val cd = ClockDomain.external("clk")
    val compiled = SimConfig.withWave.compile(
      rtl = new ApbBridge(
        apbConfig = Apb3Config(
          addressWidth  = 4,
          dataWidth     = 32,
          selWidth      = 1,
          useSlaveError = true
        ),
        clkS = cd,
        clkM = cd
      )
    )

    // Run the simulation
    compiled.doSim(ApbBridgeSim.runSimulation(_))
  }
}
