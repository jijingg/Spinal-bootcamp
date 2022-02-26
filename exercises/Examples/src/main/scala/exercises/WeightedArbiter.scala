package exercises

import spinal.core._
import spinal.lib._

class WeightedArbiter(numReq: Int, weightWidth: Int = 8) extends Component {
  val io = new Bundle {
    val clear = in Bool ()
    val weights = in Vec (UInt(weightWidth bits), numReq)
    val reqs = in Bits (numReq bits)
    val grant = out Bits (numReq bits)
  }

  def isOneHot(oneHot: Bits) = new Composite(oneHot) {
    val wordWidth = oneHot.getWidth
    val bitsVec = Vec(Bool(), wordWidth)
    bitsVec(0) := oneHot(0)
    for (idx <- 1 until wordWidth) {
      bitsVec(idx) := bitsVec(idx - 1) ^ oneHot(idx)
    }

    val xorRslt = bitsVec.asBits
    val shouldBeAllOnes = (xorRslt | ~oneHot)
    val isNotAllZeros = oneHot.orR
    val isAllOnes = shouldBeAllOnes.andR
    val result = isAllOnes && isNotAllZeros
  }.result

  def priorityArbitrate(
      reqs: Bits,
      reqBase: Bits
  ) = new Composite(this) {
    val reqMask = (reqs.asUInt - reqBase.asUInt).asBits
    val grant = reqs & ~reqMask
  }.grant

  def updateWeights(
      initialWeightVec: Vec[UInt],
      weightVec: Vec[UInt],
      grant: Bits,
      reload: Bool,
      numReq: Int,
      weightWidth: Int
  ) = new Area {
    val grantIdx = OHToUInt(grant)
    val updateWeight = grant.orR // grant is zero or not
    val weights = reload ? initialWeightVec | weightVec

    when(reload) {
      weightVecReg := initialWeightVec
    }
    when(updateWeight) {
      weightVec(grantIdx) := weights(grantIdx) - 1

      assert(
        assertion = weights(grantIdx) > 0,
        message = "granted request should have weight > 0",
        severity = ERROR
      )
    }
  }

  def updateLastGrant(
      lastGrant: Bits,
      clear: Bool,
      update: Bool,
      grant: Bits
  ) = new Area {
    when(clear) {
      lastGrant := 1
    } elsewhen (update) {
      when(grant.msb === True) { // Finish one round
        lastGrant := 1
      } otherwise {
        lastGrant := grant
      }

      val checkOH = isOneHot(grant)
      // report(L"grant=${grant}")
      assert(
        assertion = checkOH,
        message = L"grant=${grant} should be one hot",
        severity = ERROR
      )
    }
  }

  val initialWeightVecReg = Reg(Vec(UInt(weightWidth bits), numReq))
  val initialWeightMaskReg = Reg(Bits(numReq bits))
  when(io.clear) {
    initialWeightVecReg := io.weights
    initialWeightMaskReg := io.weights.map(w => w.orR).asBits()
  }

  val reload = Bool()
  val weightVecReg = Reg(Vec(UInt(weightWidth bits), numReq))
  val weightMask = weightVecReg.map(w => w.orR).asBits()
  updateWeights(io.weights, weightVecReg, io.grant, reload, numReq, weightWidth)

  val lastGrantReg = Reg(Bits(numReq bits))

  val roundRobinMask = ~(lastGrantReg | (lastGrantReg.asUInt - 1).asBits)
  val maskedReqs = io.reqs & weightMask & roundRobinMask
  val hasMaskedReqs = maskedReqs.orR
  val maskedGrant = priorityArbitrate(maskedReqs, lastGrantReg)
  reload := io.clear | (io.reqs.orR & !hasMaskedReqs)

  val weightedReqs = io.reqs & initialWeightMaskReg
  val hasReqs = weightedReqs.orR
  val weightedReqBase = B(1, numReq bits)
  val unmaskedGrant = priorityArbitrate(weightedReqs, weightedReqBase)

  val noMaskedReqs = B(numReq bits, default -> !hasMaskedReqs)
  io.grant := (noMaskedReqs & unmaskedGrant) | maskedGrant

  updateLastGrant(lastGrantReg, io.clear, hasReqs, io.grant)
}

object WeightedArbiter {
  def main(args: Array[String]): Unit = {
    SpinalVerilog(new WeightedArbiter(numReq = 4)).printPruned()
  }
}
