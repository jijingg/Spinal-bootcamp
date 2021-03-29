package exercises

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.sim._
import spinal.lib.sim.SimData

class HandShakePipe(width: Int) extends Component {
  val io = new Bundle {
    val input = new Bundle {
      val valid = in Bool
      val payload = in UInt (width bits)
      val ready = out Bool
    }

    val output = new Bundle {
      val valid = out Bool
      val payload = out UInt (width bits)
      val ready = in Bool
    }
  }
}

class M2SHandShakePipe(
    width: Int,
    noBubble: Boolean = true
) extends HandShakePipe(width) {
  val doutReg = Reg(UInt(width bits)) init (0)
  val validReg = Reg(Bool) init (False)
  io.output.payload := doutReg
  io.output.valid := validReg

  if (noBubble) {
    when(io.input.ready) {
      doutReg := io.input.payload
      validReg := io.input.valid
    }

    io.input.ready := io.output.ready || !validReg
  } else {
    when(io.input.ready) {
      doutReg := io.input.payload
      validReg := io.input.valid
    }

    io.input.ready := io.output.ready
  }
}

class S2MHandShakePipe(
    width: Int,
    noBubble: Boolean = true
) extends HandShakePipe(width) {
  val readyReg = RegNext(io.output.ready) init (False)
  val doutReg = Reg(UInt(width bits)) init (0)
  val validReg = Reg(Bool) init (False)

  if (noBubble) {
    // io.output.payload := validReg ? doutReg | io.input.payload

    when(io.output.ready && !readyReg) {
      io.output.payload := validReg ? doutReg | io.input.payload
      io.output.valid := validReg || io.input.valid
      io.input.ready := !validReg
      doutReg := 0
      validReg := False
    }.elsewhen(io.output.ready && readyReg) {
      io.output.payload := io.input.payload
      io.output.valid := io.input.valid
      io.input.ready := readyReg // True
      doutReg := 0
      validReg := False
    }.elsewhen(!io.output.ready && readyReg) { // Cache input data if valid
      io.output.payload := io.input.payload
      io.output.valid := io.input.valid
      io.input.ready := readyReg // True
      doutReg := io.input.payload
      validReg := io.input.valid
    }.otherwise { // !io.output.ready && !readyReg
      io.output.payload := validReg ? doutReg | io.input.payload
      io.output.valid := io.input.valid || validReg
      io.input.ready := !validReg
      when(io.input.valid && io.input.ready) { // Cache input data if valid
        doutReg := io.input.payload
        validReg := True
      }
    }
  } else {
    io.input.ready := readyReg
    when(io.output.ready && !readyReg) {
      io.output.payload := doutReg
      io.output.valid := validReg
    }.elsewhen(io.output.ready && readyReg) {
      io.output.payload := io.input.payload
      io.output.valid := io.input.valid
    }.elsewhen(!io.output.ready && readyReg) {
      io.output.payload := io.input.payload
      io.output.valid := io.input.valid
    }.otherwise { // !io.output.ready && !readyReg
      io.output.payload := io.input.payload
      io.output.valid := io.input.valid
    }
  }
}
