package exercises

import org.scalatest.funsuite.AnyFunSuite

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.bus.amba3.apb._
import spinal.sim._

class HandShakePipeTest extends AnyFunSuite {
  // implicit class StreamExtend(stream: Stream[UInt]) {
  //   def init() {
  //     stream.valid #= false
  //     stream.payload #= 0
  //     stream.ready #= true
  //   }

  //   def simPush(payload: Int)(cd: ClockDomain): Unit ={
  //     stream.valid #= true
  //     stream.payload #= payload
  //     cd.waitSampling()
  //     while (!stream.ready.toBoolean) {
  //       cd.waitSampling()
  //     }
  //     stream.valid #= false
  //     stream.payload #= 0
  //   }

  //   def monitor(queue: Queue[Int])(cd: ClockDomain) {
  //     while (true) {
  //       cd.waitSampling()
  //       if (stream.valid.toBoolean && stream.ready.toBoolean) {
  //         queue.enqueue(stream.payload.toInt)
  //       }
  //     }
  //   }
  // }

  // class StreamTopTB(width: Int) extends StreamTop(width) {
  //   def init() {
  //     io.sin.init()
  //     io.sout.init()
  //   }

  //   def simPush(payload: Int) {
  //     io.sin.simPush(payload)(clockDomain)
  //   }

  //   val queue = Queue[Int]()
  //   def monitor() {
  //     io.sout.monitor(queue)(clockDomain)
  //   }
  // }

  class PipeStream(width: Int) extends Component {
    val io = new Bundle {
      val sin = slave Stream(UInt(width bits))
      val sout = master Stream(UInt(width bits))
    }
  }

  class M2SPipeStream(width: Int) extends PipeStream(width) {
    io.sout << io.sin.m2sPipe()
  }

  class S2MPipeStream(width: Int) extends PipeStream(width) {
    io.sout << io.sin.s2mPipe()
  }

  class BothPipeStream(width: Int) extends PipeStream(width) {
    io.sout << io.sin.m2sPipe().s2mPipe()
  }

  class BothPipeStream2(width: Int) extends PipeStream(width) {
    io.sout << io.sin.s2mPipe().m2sPipe()
  }

  abstract class PipeTB(width: Int) extends Component {
    val pipe: HandShakePipe
    val stream: PipeStream
  }

  class M2SPipeTB(width: Int) extends PipeTB(width) {
    override val pipe = new M2SHandShakePipe(width)
    override val stream = new M2SPipeStream(width)

    pipe.io.simPublic()
    stream.io.simPublic()
  }

  class S2MPipeTB(width: Int) extends PipeTB(width) {
    override val pipe = new S2MHandShakePipe(width)
    override val stream = new S2MPipeStream(width)

    pipe.io.simPublic()
    stream.io.simPublic()

    pipe.readyReg.simPublic()
    pipe.validReg.simPublic()
    pipe.doutReg.simPublic()
  }

  class BothPipeTB(width: Int) extends PipeTB(width) {
    override val pipe = new BothHandShakePipe(width)
    override val stream = new BothPipeStream(width)

    pipe.io.simPublic()
    stream.io.simPublic()
  }

  class BothPipe2TB(width: Int) extends PipeTB(width) {
    override val pipe = new BothHandShakePipe2(width)
    override val stream = new BothPipeStream2(width)

    pipe.io.simPublic()
    stream.io.simPublic()
  }

  class BothPipe3TB(width: Int) extends PipeTB(width) {
    override val pipe = new BothHandShakePipe3(width)
    override val stream = new BothPipeStream2(width)

    pipe.io.simPublic()
    stream.io.simPublic()
  }

  def simTest(dut: PipeTB): Unit = {
    SimTimeout(1000)
    dut.clockDomain.forkStimulus(2)

    dut.pipe.io.input.valid #= false
    dut.pipe.io.input.payload #= 0
    dut.pipe.io.input.ready #= true
    dut.pipe.io.output.valid #= false
    dut.pipe.io.output.payload #= 0
    dut.pipe.io.output.ready #= true
    dut.stream.io.sin.valid #= false
    dut.stream.io.sin.payload #= 0
    dut.stream.io.sin.ready #= true
    dut.stream.io.sout.valid #= false
    dut.stream.io.sout.payload #= 0
    dut.stream.io.sout.ready #= true
    sleep(0)

    val rand = new scala.util.Random()
    var din = 0

    val sender = fork {
      while (true) {
        dut.pipe.io.input.valid #= true
        dut.pipe.io.input.payload #= din
        dut.stream.io.sin.valid #= true
        dut.stream.io.sin.payload #= din
        dut.clockDomain.waitSampling()
        assert(
          dut.pipe.io.input.ready.toBoolean == dut.stream.io.sin.ready.toBoolean,
          s"pipe ri=${dut.pipe.io.input.ready.toBoolean} not match stream ri=${dut.stream.io.sin.ready.toBoolean}"
        )
        if (dut.pipe.io.input.valid.toBoolean && dut.pipe.io.input.ready.toBoolean) {
          din += 1
        }
      }
    }

    var matchCnt = 0
    val receiver = fork {
      while (true) {
        val randBool = rand.nextBoolean()
        dut.pipe.io.output.ready #= randBool
        dut.stream.io.sout.ready #= randBool
        dut.clockDomain.waitSampling()
        assert(
          dut.pipe.io.output.valid.toBoolean == dut.stream.io.sout.valid.toBoolean,
          s"pipe vo=${dut.pipe.io.output.valid.toBoolean} not match stream vo=${dut.stream.io.sout.valid.toBoolean}"
        )
        if (dut.pipe.io.output.valid.toBoolean && dut.pipe.io.output.ready.toBoolean) {
          assert(
            dut.pipe.io.output.payload.toInt == dut.stream.io.sout.payload.toInt,
            s"pipe dout=${dut.pipe.io.output.payload.toInt} not match stream dout=${dut.stream.io.sout.payload.toInt}"
          )
          matchCnt += 1
          if (matchCnt > 100) {
            simSuccess()
          }
        }
      }
    }

    while (true) {
      dut.clockDomain.waitSampling()
        // pipe_vr=${dut.pipe.validReg.toBoolean},
        // pipe_rr=${dut.pipe.readyReg.toBoolean},
        // pipe_dr=${dut.pipe.doutReg.toInt},
      println(s"""
        pipe_din=${dut.pipe.io.input.payload.toInt},
        pipe_dout=${dut.pipe.io.output.payload.toInt},
        pipe_vo=${dut.pipe.io.output.valid.toBoolean},
        pipe_ro=${dut.pipe.io.output.ready.toBoolean},
        pipe_vi=${dut.pipe.io.input.valid.toBoolean},
        pipe_ri=${dut.pipe.io.input.ready.toBoolean},
        stream_din=${dut.stream.io.sin.payload.toInt},
        stream_dout=${dut.stream.io.sout.payload.toInt},
        stream_vo=${dut.stream.io.sout.valid.toBoolean},
        stream_ro=${dut.stream.io.sout.ready.toBoolean},
        stream_vi=${dut.stream.io.sin.valid.toBoolean},
        stream_ri=${dut.stream.io.sin.ready.toBoolean},
      """)
    }
  }

  val width = 16

  test("valid pipeline") {
    SimConfig
    .withFstWave
    .compile(new M2SPipeTB(width))
    .doSim(simTest(_))
  }

  test("ready pipeline") {
    SimConfig
    .withWave
    .compile(new S2MPipeTB(width))
    .doSim(simTest(_))
  }

  test("valid pipeline + ready pipeline") {
    SimConfig
    .withWave
    .compile(new BothPipeTB(width))
    .doSim(simTest(_))
  }

  test("ready pipeline + valid pipeline") {
    SimConfig
    .withWave
    .compile(new BothPipe2TB(width))
    .doSim(simTest(_))
  }

  test("pipeline fsm") {
    SimConfig
    .withWave
    .compile(new BothPipe3TB(width))
    .doSim(simTest(_))
  }
}

