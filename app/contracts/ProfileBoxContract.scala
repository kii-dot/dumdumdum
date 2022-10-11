package contracts

import config.Configs.dumdumdumsProfileToken
import org.ergoplatform.appkit.{Address, BlockchainContext}

case class ProfileBoxContract(contract: Contract, ownerPk: Address)

object ProfileBoxContract {

  def build(
    ownerPk: Address
  )(implicit ctx: BlockchainContext): ProfileBoxContract = {
    val constants: List[(String, Any)] = List(
      (ProfileBoxContractConstants.ownerPk, ownerPk.getPublicKey),
      (
        ProfileBoxContractConstants.dumdumdumProfileToken,
        dumdumdumsProfileToken
      )
    )

    ProfileBoxContract(
      Contract.build(
        DumDumDumContracts.ProfileBox.contractScript,
        constants = constants: _*
      ),
      ownerPk = ownerPk
    )
  }

  def getContract(ownerPk: Address)(
    implicit ctx: BlockchainContext
  ): ProfileBoxContract = this.build(ownerPk)
}

object ProfileBoxContractConstants {
  val dumdumdumProfileToken: String = "_DumDumDumProfileToken"
  val ownerPk: String = "_OwnerPK"
}
