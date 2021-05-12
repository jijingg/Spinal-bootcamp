package exercises

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.sim._
import spinal.lib.bus.amba3.apb._

import scala.collection.mutable
import scala.util.Random

object ApbBridgeSim {
  def runSimulation(dut: ApbBridge): Unit = {
    SimTimeout(1000 * 1000)
    val writeQ = mutable.Queue[(Long, Long)]()
    val readQ = mutable.Queue[(Long, Long)]()

    // Fork a thread to manage the clock domains signals
    val clocksThread = fork {
      // Clear the clock domains' signals, to be sure the simulation captures their first edges.
      dut.clkM.fallingEdge()
      dut.clkS.fallingEdge()
      dut.clkM.deassertReset()
      dut.clkS.deassertReset()
      sleep(0)

      // Do the resets.
      dut.clkM.assertReset()
      dut.clkS.assertReset()
      sleep(5)
      dut.clkM.deassertReset()
      dut.clkS.deassertReset()
      sleep(1)

      // Forever, randomly toggle one of the clocks.
      // This will create asynchronous clocks without fixed frequencies.
      while (true) {
        if (Random.nextBoolean()) {
          dut.clkM.clockToggle()
        } else {
          dut.clkS.clockToggle()
        }
        sleep(1)
      }
    }

    sleep(0)
    dut.io.apbS.PENABLE #= false
    dut.io.apbS.PADDR #= 0
    dut.io.apbS.PWDATA #= 0
    dut.io.apbS.PRDATA #= 0
    dut.io.apbS.PWRITE #= false
    dut.io.apbS.PSEL #= 0
    dut.io.apbS.PREADY #= false
    dut.io.apbS.PSLVERROR #= false
    dut.io.apbM.PENABLE #= false
    dut.io.apbM.PADDR #= 0
    dut.io.apbM.PWDATA #= 0
    dut.io.apbM.PRDATA #= 0
    dut.io.apbM.PWRITE #= false
    dut.io.apbM.PSEL #= 0
    dut.io.apbM.PREADY #= false
    dut.io.apbM.PSLVERROR #= false
    sleep(6)

    var readMatch = 0
    val upStream = fork {
      while (true) {
        dut.io.apbS.PENABLE #= false
        dut.io.apbS.PADDR.randomize()
        dut.io.apbS.PWDATA.randomize()
        dut.io.apbS.PWRITE.randomize()
        dut.io.apbS.PSEL.randomize()
        //println(s"BEFORE pselS=${dut.io.apbS.PSEL.toInt}")
        dut.clkS.waitSampling()
        //assert(dut.io.apbS.PSEL.toInt == 1 && !dut.io.apbS.PENABLE.toBoolean)
        if (dut.io.apbS.PSEL.toInt == 1) {
          dut.io.apbS.PENABLE #= true
          //assert(dut.io.apbS.PENABLE.toBoolean)
          //println(s"pwriteS=${dut.io.apbS.PWRITE.toBoolean}")
          //assert(dut.io.apbS.PWRITE.toBoolean)
          if (dut.io.apbS.PWRITE.toBoolean) {
            println(
              s"writeS=${dut.io.apbS.PWDATA.toLong}, addrS=${dut.io.apbS.PADDR.toLong}"
            )
            //dut.clkM.waitSampling()
            writeQ.enqueue(
              (dut.io.apbS.PADDR.toLong, dut.io.apbS.PWDATA.toLong)
            )
          }
        }
        sleep(0)

        //assert(dut.io.apbS.PENABLE.toBoolean)
        //println(s"penableS=${dut.io.apbS.PENABLE.toBoolean}")
        if (dut.io.apbS.PENABLE.toBoolean) {
          assert(!dut.io.apbS.PREADY.toBoolean)
          println(s"BEFORE readyS=${dut.io.apbS.PREADY.toBoolean}")
          waitUntil(dut.io.apbS.PREADY.toBoolean)
          println(s"AFTER readyS=${dut.io.apbS.PREADY.toBoolean}")

          if (!dut.io.apbS.PWRITE.toBoolean) {
            val readTuple = readQ.dequeue()
            println(
              s"readS tuple=${readTuple}, addrS=${dut.io.apbS.PADDR.toLong}, readS=${dut.io.apbS.PRDATA.toLong}"
            )
            assert(
              (dut.io.apbS.PADDR.toLong, dut.io.apbS.PRDATA.toLong) == readTuple
            )
            readMatch += 1
          }
          //assert(dut.io.apbS.PENABLE.toBoolean)
          dut.clkS.waitSampling()
          dut.io.apbS.PSEL #= 0
          dut.io.apbS.PENABLE #= false
        }
        //println(s"pselS=${dut.io.apbS.PSEL.toInt}, penableS=${dut.io.apbS.PENABLE.toBoolean}")
      }
    }

    var writeMatch = 0
    val downStream = fork {
      while (true) {
        //for (i <- 0 until 2) {
        dut.io.apbM.PREADY #= false
        dut.io.apbM.PSLVERROR #= false
        waitUntil(dut.io.apbM.PSEL.toInt == 1)
        println(s"pselM=${dut.io.apbM.PSEL.toInt}")
        waitUntil(dut.io.apbM.PENABLE.toBoolean)
        println(s"penableM=${dut.io.apbM.PENABLE.toBoolean}")
        dut.io.apbM.PRDATA.randomize()
        dut.clkM.waitSampling()
        if (dut.io.apbM.PENABLE.toBoolean) {
          //assert(dut.io.apbS.PENABLE.toBoolean)
          //assert(dut.io.apbM.PENABLE.toBoolean)
          assert(!dut.io.apbM.PREADY.toBoolean)
          //assert(dut.io.apbM.PWRITE.toBoolean)
          if (!dut.io.apbM.PWRITE.toBoolean) {
            println(
              s"readM=${dut.io.apbM.PRDATA.toLong}, addrM=${dut.io.apbS.PADDR.toLong}"
            )
            readQ.enqueue((dut.io.apbS.PADDR.toLong, dut.io.apbM.PRDATA.toLong))
          } else {
            val writeTuple = writeQ.dequeue()
            println(
              s"writeM tuple=${writeTuple}, addrM=${dut.io.apbM.PADDR.toLong}, writeM=${dut.io.apbM.PWDATA.toLong}"
            )
            assert(
              (
                dut.io.apbM.PADDR.toLong,
                dut.io.apbM.PWDATA.toLong
              ) == writeTuple
            )
            writeMatch += 1
          }
          //println(s"BEFORE preadyM=${dut.io.apbM.PREADY.toBoolean}")
          dut.io.apbM.PREADY #= true
          dut.clkM.waitSampling()
          println(s"AFTER preadyM=${dut.io.apbM.PREADY.toBoolean}")
          assert(
            dut.io.apbM.PREADY.toBoolean && dut.io.apbM.PENABLE.toBoolean && dut.io.apbM.PSEL.toInt == 1
          )
          dut.io.apbM.PREADY #= false
          waitUntil(
            !dut.io.apbM.PENABLE.toBoolean && dut.io.apbM.PSEL.toInt == 0
          )
        }
      }
    }

    waitUntil(writeMatch > 1000 && readMatch > 1000)
    simSuccess()
  }

  def main(args: Array[String]): Unit = {
    // Compile the Component for the simulator.
    val compiled = SimConfig.withWave.allOptimisation.compile {
      val dut = new ApbBridge(
        apbConfig = Apb3Config(
          addressWidth = 4,
          dataWidth = 32,
          selWidth = 1,
          useSlaveError = true
        ),
        clkS = ClockDomain.external("clkS"),
        clkM = ClockDomain.external("clkM")
      )
      dut.io.apbM.simPublic()
      dut.io.apbS.simPublic()
      dut
    }

    // Run the simulation.
    compiled.doSim(runSimulation(_))
  }
}
