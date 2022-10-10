package profile

import boxes.{Box, BoxWrapper}
import commons.ErgCommons
import org.ergoplatform.appkit.{BlockchainContext, ErgoContract, ErgoId, ErgoToken}
import registers.Register

class Profile {

}

case class ProfileBox(
                       override val value: Long = ErgCommons.MinBoxFee,
                       override val tokens: Seq[ErgoToken],
                       override val id: ErgoId = null,
                       override val box: Option[Box] = Option.empty
                     ) extends BoxWrapper {

  override def getContract(implicit ctx: BlockchainContext): ErgoContract = ???

  override def R4: Option[Register[_]] = super.R4

  override def R5: Option[Register[_]] = super.R5
}