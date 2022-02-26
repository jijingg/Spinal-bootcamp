package exercises

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.sim._
import spinal.lib.bus.amba3.apb._
import spinal.lib.bus.misc._

class ApbBridge(
    val apbConfig: Apb3Config,
    val clkM: ClockDomain,
    val clkS: ClockDomain
) extends Component {
  val io = new Bundle {
    val apbM = master(Apb3(apbConfig))
    val apbS = slave(Apb3(apbConfig))
  }

  io.apbM.PADDR := io.apbS.PADDR;
  io.apbM.PWDATA := io.apbS.PWDATA;
  io.apbM.PWRITE := io.apbS.PWRITE;
  io.apbS.PRDATA := io.apbM.PRDATA;
  io.apbS.PSLVERROR := io.apbM.PSLVERROR;

  val doneM = clkM(RegInit(False))

  val cdS = new ClockingArea(clkS) {
    val reqS = RegInit(False)
    val readyS = RegInit(False)
    val doneS = BufferCC(doneM, False)
    when(io.apbS.PSEL === B"1" && !io.apbS.PENABLE) {
      reqS := !reqS
      readyS := False
    }.elsewhen(io.apbS.PSEL === B"1" && io.apbS.PENABLE && doneS.edge(False)) {
      readyS := True
    }.otherwise {
      readyS := False
    }
  }
  io.apbS.PREADY := cdS.readyS

  val cdM = new ClockingArea(clkM) {
    val reqM = BufferCC(cdS.reqS, False)
    val pselM = Reg(Bits(apbConfig.selWidth bits)) init (B"0")
    val penableM = RegInit(False)
    when(reqM.edge(False)) {
      pselM := B"1"
    }.elsewhen(io.apbM.PREADY && pselM === B"1" && penableM) {
      doneM := !doneM
      penableM := False
      pselM := B"0"
    }.elsewhen(pselM === B"1") {
      penableM := True
    }
  }
  io.apbM.PSEL := cdM.pselM
  io.apbM.PENABLE := cdM.penableM
}

object ApbBridge {
  def main(args: Array[String]): Unit = {
    SpinalVerilog(
      new ApbBridge(
        apbConfig = Apb3Config(
          addressWidth = 4,
          dataWidth = 32,
          selWidth = 1,
          useSlaveError = true
        ),
        clkS = ClockDomain.external("clkS"),
        clkM = ClockDomain.external("clkM")
      )
    )
  }
}
