package exercises

import org.scalatest.funsuite.AnyFunSuite

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.bus.amba3.apb._
import spinal.sim._

class ApbArbiterTest extends AnyFunSuite {
  test("apb arbiter test") {
    val apbConfig = Apb3Config(
      addressWidth  = 4,
      dataWidth     = 32,
      selWidth      = 1,
      useSlaveError = false
    )
    val numInputSlaves = 8
    SimConfig.withWave.compile {
      val dut = new ApbArbiter(apbConfig, numInputSlaves)
      dut.select.simPublic()
      dut.priority.simPublic()
      dut.apbReqBits.simPublic()
      dut.selectIdx.simPublic()
      dut
    }
    .doSim { dut =>
      SimTimeout(500)

      dut.clockDomain.forkStimulus(2)

      val apbMasters = fork {
        for (idx <- 0 until numInputSlaves) {
          dut.io.slavesIn(idx).PSEL #= 1
          dut.io.slavesIn(idx).PENABLE #= true
          dut.io.slavesIn(idx).PADDR #= idx
          dut.io.slavesIn(idx).PWDATA #= 0
          dut.io.slavesIn(idx).PWRITE #= false
          dut.io.slavesIn(idx).PRDATA #= numInputSlaves
          dut.io.slavesIn(idx).PREADY #= false
        }
        while (true) {
          dut.clockDomain.waitSampling()
          for (idx <- 0 until numInputSlaves) {
            if (dut.io.slavesIn(idx).PREADY.toBoolean) {
              dut.io.slavesIn(idx).PSEL #= 0
              dut.io.slavesIn(idx).PENABLE #= false
              assert(
                dut.io.slavesIn(idx).PRDATA.toLong == idx + 1,
                s"PRDATA[${idx}]${dut.io.slavesIn(idx).PRDATA.toLong} != ${idx} + 1"
              )
            }
          }
        }
      }

      dut.io.en #= false
      dut.io.masterOut.PREADY #= false
      dut.clockDomain.waitSampling(2)
      dut.io.en #= true

      for (round <- 0 until numInputSlaves) {
        dut.clockDomain.waitSampling()
        dut.io.masterOut.PREADY #= false

        dut.clockDomain.waitSampling(2)
        println(s"""
          round=${round}
          reqs=${dut.apbReqBits.toInt},
          select=${dut.select.toInt},
          selectIdx=${dut.selectIdx.toInt},
          priority=${dut.priority.toInt},
        """)

        dut.io.masterOut.PREADY #= true
        dut.io.masterOut.PRDATA #= dut.selectIdx.toInt + 1
      }

      dut.clockDomain.waitSampling()
      dut.io.masterOut.PRDATA #= numInputSlaves
      dut.io.masterOut.PREADY #= false
      dut.clockDomain.waitSampling()
    }
  }
}
