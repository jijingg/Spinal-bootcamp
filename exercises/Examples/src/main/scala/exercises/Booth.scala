// https://www.cnblogs.com/hiramlee0534/p/3440168.html

package exercises

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.sim._

class Booth(width: Int) extends Component {
  require(width > 1)

  val io = new Bundle {
    val load = in Bool ()
    val multiplicand = in SInt (width bits)
    val multipler = in SInt (width bits)
    val ready = out Bool ()
    val product = out SInt (2 * width bits)
  }

  val buf = Reg(SInt(2 * width + 1 bits)) init (0)
  val upperPart = buf(2 * width downto width + 1)
  val lowerPart = buf(width downto 0)
  val cnt = CounterFreeRun(stateCount = width + 2)

  buf.simPublic()
  upperPart.simPublic()
  lowerPart.simPublic()
  cnt.value.simPublic()

  when(io.load) {
    buf := S(0, width bits) @@ io.multipler @@ S(0, 1 bit)
  }.elsewhen(!io.ready) {
    switch(buf(1 downto 0).asBits) {
      is(0, 3) {
        buf := (buf >> 1).resized
      }
      is(1) {
        buf := (((upperPart + io.multiplicand) @@ lowerPart) >> 1).resized
      }
      is(2) {
        buf := (((upperPart - io.multiplicand) @@ lowerPart) >> 1).resized
      }
      // is (3) {
      //   buf := (buf >> 1).resized
      // }
    }
  }.otherwise {
    cnt.clear()
  }

  io.ready := cnt.willOverflow
  io.product := buf >> 1
}

object Booth {
  def main(args: Array[String]): Unit = {
    SpinalSystemVerilog(new Booth(8))
  }
}

object BoothSim extends App {
  SimConfig.withWave
    //   .withConfig(SpinalConfig(
    //     defaultClockDomainFrequency = FixedFrequency(100 MHz),
    //     defaultConfigForClockDomains = ClockDomainConfig(resetKind = SYNC)))
    .compile(new Booth(6))
    .doSim { dut =>
      SimTimeout(100)

      dut.clockDomain.forkStimulus(2)

      val multiplicand = 4
      val multipler = -3

      dut.io.multiplicand #= multiplicand
      dut.io.multipler #= multipler
      dut.io.load #= true
      dut.clockDomain.waitSampling()
      dut.io.load #= false
      //dut.clockDomain.waitSampling()
      while (true) {
        dut.clockDomain.waitSampling()
        println(s"""
          C=${dut.cnt.value.toInt},
          B=${dut.buf.toInt},
          U=${dut.upperPart.toInt},
          L=${dut.lowerPart.toInt},
          P=${dut.io.product.toInt},
          ready=${dut.io.ready.toBoolean}""")

        if (dut.io.ready.toBoolean == true) {
          assert(dut.io.product.toInt == multiplicand * multipler)
          simSuccess()
        }
      }
    }
}
