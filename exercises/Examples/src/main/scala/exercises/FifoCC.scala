package exercises

import spinal.core._
import spinal.lib._

class FifoCC[T <: Data](
    dataType: HardType[T],
    val depth: Int,
    val pushClock: ClockDomain,
    val popClock: ClockDomain
) extends Component {
  require(
    isPow2(depth) & depth >= 16,
    "FIFO depth must be power of 2 and larger than 16"
  )

  val io = new Bundle {
    val wData = in(dataType())
    val wEn = in Bool ()
    val wFull = out Bool ()
    val rData = out(dataType())
    val rEn = in Bool ()
    val rEmpty = out Bool ()
  }

  val ADDR_WIDTH = log2Up(depth + 1)
  val mem = Mem(dataType, depth)

  val popToPushGray = Bits(ADDR_WIDTH bits) addTag (crossClockDomain)
  val pushToPopGray = Bits(ADDR_WIDTH bits) addTag (crossClockDomain)

  val pushArea = new ClockingArea(pushClock) {
    val wAddr = Reg(UInt(ADDR_WIDTH bits)) init (0)
    val wAddrInc = io.wEn && !io.wFull
    mem.write(
      enable = wAddrInc,
      address = wAddr.resized,
      data = io.wData
    )
    wAddr := wAddr + wAddrInc.asUInt

    val wAddrGray = toGray(wAddr)
    val rAddrGrayInPushArea =
      BufferCC(popToPushGray, init = B(0, ADDR_WIDTH bits))
    // val rAddrInPushArea = fromGray(rAddrGrayInPushArea)

    io.wFull := (wAddrGray(
      ADDR_WIDTH - 1 downto ADDR_WIDTH - 2
    ) === ~rAddrGrayInPushArea(ADDR_WIDTH - 1 downto ADDR_WIDTH - 2)
      && wAddrGray(ADDR_WIDTH - 3 downto 0) === rAddrGrayInPushArea(
        ADDR_WIDTH - 3 downto 0
      ))
  }

  val popArea = new ClockingArea(popClock) {
    val rAddr = Reg(UInt(ADDR_WIDTH bits)) init (0)
    val rAddrFire = io.rEn && !io.rEmpty
    // io.rData := mem.readSync(rAddr.resized, enable = rAddrFire, clockCrossing = true)
    io.rData := mem.readAsync(rAddr.resized)
    rAddr := rAddr + rAddrFire.asUInt

    val rAddrGray = toGray(rAddr)
    val wAddrGrayInPopArea =
      BufferCC(pushToPopGray, init = B(0, ADDR_WIDTH bits))
    // val wAddrInPopArea = fromGray(wAddrGrayInPopArea)

    io.rEmpty := rAddrGray === wAddrGrayInPopArea
  }

  popToPushGray := popArea.rAddrGray
  pushToPopGray := pushArea.wAddrGray
}
