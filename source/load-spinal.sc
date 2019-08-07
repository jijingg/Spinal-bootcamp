import $ivy.`com.github.spinalhdl::spinalhdl-core:1.3.5` 
import $ivy.`com.github.spinalhdl::spinalhdl-lib:1.3.5` 

import spinal.core._
import spinal.lib._
import spinal.core.sim._

implicit class SpinalReportExtend(sp :SpinalReport[Component]) {
  def getRtlString():String = {
    assert(sp.generatedSourcesPaths.size == 1)
    scala.io.Source.fromFile(sp.generatedSourcesPaths.head).mkString
  }
}

def showRtl(dut: => Component, mode:SpinalMode = `Verilog`) = {
  println(SpinalConfig(
    mode,
    targetDirectory="rtl/"
  ).generate(dut).getRtlString) 
}

def showVhdl(dut: => Component) = showRtl(dut,VHDL)   

@deprecated("Deprecated, showRtl is recommended", "spinal-bootcamp 0.1.0")
def showVerilog(dut: => Component, moduleName:String) = {
  SpinalConfig(
    mode = Verilog,
    targetDirectory="rtl/"
  ).generate(dut)
  println(scala.io.Source.fromFile("rtl/"+moduleName+".v").mkString)
}

