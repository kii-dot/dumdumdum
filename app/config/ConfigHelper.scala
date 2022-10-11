package config

object Configs extends ConfigHelper {
  lazy val dumdumdumsNFT: String = readKey("tokens.dumdumdumsNFT")

  lazy val dumdumdumsProfileToken: String = readKey(
    "tokens.dumdumdumsProfileToken"
  )
  lazy val serviceOwner: String = readKey("service.owner")
}
