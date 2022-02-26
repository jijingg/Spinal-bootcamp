package exercises

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba3.apb._

class ApbArbiter(
    apbConfig: Apb3Config,
    numInputSlaves: Int
) extends Component {
  require(numInputSlaves > 0)

  val io = new Bundle {
    val en = in Bool ()
    val masterOut = master(Apb3(apbConfig))
    val slavesIn = Vec(slave(Apb3(apbConfig)), numInputSlaves)
  }

  io.masterOut.PSEL := 0
  io.masterOut.PENABLE := False
  io.masterOut.PADDR := 0
  io.masterOut.PWDATA := 0
  io.masterOut.PWRITE := False

  val slaveRegVec = Vec(Reg(Apb3(apbConfig)), numInputSlaves)
  for ((apbReg, apbIn) <- slaveRegVec.zip(io.slavesIn)) {
    apbReg.PREADY := False
    apbReg.PRDATA := 0
    apbReg << apbIn
  }

  val done =
    io.masterOut.PREADY && io.masterOut.PSEL === B(1) && io.masterOut.PENABLE;

  val apbReqBits = Bits(numInputSlaves bits)
  for ((slaveReg, idx) <- slaveRegVec.zipWithIndex) {
    when(
      io.slavesIn(idx).PSEL === B(1) && io.slavesIn(idx).PENABLE && !io
        .slavesIn(idx)
        .PREADY
    ) {
      apbReqBits(idx) := True
    }.otherwise {
      apbReqBits(idx) := False
    }
  }

  val priority = Reg(Bits(numInputSlaves bits)) init (1)
  val select = OHMasking.roundRobin(apbReqBits, priority)
  val selectIdx = OHToUInt(select)

  when(io.en) {
    val selectSlaveReg = slaveRegVec(selectIdx)
    when(select.orR) {
      io.masterOut << selectSlaveReg
    }

    when(done) {
      priority := priority.rotateLeft(1)
    }
  }
}

object ApbArbiter {
  def main(args: Array[String]): Unit = {
    val apbConfig = Apb3Config(
      addressWidth = 4,
      dataWidth = 32,
      selWidth = 1,
      useSlaveError = false
    )
    val numInputSlaves = 4

    SpinalVerilog(new ApbArbiter(apbConfig, numInputSlaves))
  }
}
