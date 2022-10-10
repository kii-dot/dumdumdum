package feed

import io.circe.Json
import json.{ErgoJson, Register, RegisterType}
import mint.{Client, NFTType, TweetExplorer}
import org.ergoplatform.appkit.{Address, ErgoToken, InputBox}

import java.nio.charset.StandardCharsets
import javax.inject.Inject

class Feed @Inject()(client: Client, explorer: TweetExplorer) {
  def get(address: Address): Seq[Json] = {
    val unspentBox: List[InputBox] = client.getAllUnspentBox(address).filter(box => !box.getTokens.isEmpty)
    val tokens = unspentBox.flatten(box => box.getTokens.toArray()).asInstanceOf[Seq[ErgoToken]]

    // Get tokens
    val getBoxes = tokens.map(token => explorer.getBoxById(token.getId.toString))
    val getTxForBoxes = getBoxes.map(boxJson => explorer.getConfirmedTx(boxJson.hcursor.downField("spentTransactionId").as[String].getOrElse("")))
    val getNFTOutBoxFromTx = getTxForBoxes.map(txJson => txJson.hcursor.downField("outputs").as[Array[Json]].getOrElse(null))
      .map(boxArray => boxArray.head)

    val getBoxesWithAdditionalRegisters = getNFTOutBoxFromTx.filter(json => json.hcursor.downField("additionalRegisters").as[Json].getOrElse(null) != null)

    val getTweetNFT = getBoxesWithAdditionalRegisters
      .filter(json => ErgoJson.getRegister(json.hcursor.downField("additionalRegisters").as[Json].getOrElse(null), RegisterType.CollByte, Register.R7).nonEmpty)
      .filter(json => ErgoJson.getRegister(
        json.hcursor.downField("additionalRegisters").as[Json].getOrElse(null),
        RegisterType.CollByte,
        Register.R7).get.asInstanceOf[Array[Byte]] sameElements NFTType.tweet)

    val getTweet = getTweetNFT.map(json => {
      val additionalRegisterJson =
        json.hcursor.downField("additionalRegisters").as[Json].getOrElse(null)
      Tweet(
        id = new String(ErgoJson.getRegister(
          additionalRegisterJson,
          RegisterType.CollByte,
          Register.R4).get.asInstanceOf[Array[Byte]], StandardCharsets.UTF_8),
        message = new String(ErgoJson.getRegister(
          additionalRegisterJson,
          RegisterType.CollByte,
          Register.R5
        ).get.asInstanceOf[Array[Byte]], StandardCharsets.UTF_8),
        creationHeight = json.hcursor.downField("creationHeight").as[Long].getOrElse(0),
        author = address
      )
    })
      .sortBy(tweet => tweet.creationHeight)(Ordering.Long.reverse)

    getTweet.map(tweet => tweet.toJson)
  }
}

case class Tweet(id: String, message: String, creationHeight: Long, author: Address) {
  def toJson: Json = {
    Json.fromFields(
      List(
        ("id", Json.fromString(this.id)),
        ("message", Json.fromString(this.message)),
        ("creationHeight", Json.fromLong(creationHeight)),
        ("authorAddress", Json.fromString(author.toString))
      )
    )
  }
}
