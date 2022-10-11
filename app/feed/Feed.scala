package feed

import io.circe.Json
import json.{ErgoJson, Register, RegisterType}
import mint.{Client, NFTType, TweetExplorer}
import org.ergoplatform.appkit.{Address, ErgoToken, InputBox}

import java.nio.charset.StandardCharsets
import javax.inject.Inject

class Feed @Inject() (client: Client, explorer: TweetExplorer) {

  def get(address: Address): Seq[Json] = {
    val unspentBox: List[InputBox] =
      client.getAllUnspentBox(address).filter(box => !box.getTokens.isEmpty)
    val tokens = unspentBox
      .flatten(box => box.getTokens.toArray())
      .asInstanceOf[Seq[ErgoToken]]

    // Get tokens
    val tweets = tokens
      .flatMap(token => getTweet(token.getId.toString))
      .sortBy(tweet => tweet.creationHeight)(Ordering.Long.reverse)
    tweets.map(tweet => tweet.toJson)
  }

  def getTweet(tokenId: String): Option[Tweet] =
    try {
      val box = explorer.getBoxById(tokenId)
      val spentTx = explorer.getConfirmedTx(
        box.hcursor.downField("spentTransactionId").as[String].getOrElse("")
      )
      val nftOutBox = spentTx.hcursor
        .downField("outputs")
        .as[Array[Json]]
        .getOrElse(null)
        .head
      val tweetRegister = ErgoJson.getRegister(
        nftOutBox.hcursor
          .downField("additionalRegisters")
          .as[Json]
          .getOrElse(null),
        RegisterType.CollByte,
        Register.R7
      )
      val isTweet = tweetRegister.nonEmpty && tweetRegister.get
        .asInstanceOf[Array[Byte]]
        .sameElements(NFTType.tweet)
      if (isTweet) {
        val additionalRegisterJson =
          nftOutBox.hcursor
            .downField("additionalRegisters")
            .as[Json]
            .getOrElse(null)

        val tweetTypePairUnparsed = ErgoJson
          .getRegister(
            additionalRegisterJson,
            RegisterType.PairIntCollByte,
            Register.R9
          )
          .get
          .asInstanceOf[(Int, Array[Byte])]
        val nftLink: String =
          if (tweetTypePairUnparsed._2.nonEmpty)
            new String(tweetTypePairUnparsed._2, StandardCharsets.UTF_8)
          else ""

        val tweetTypePair = (tweetTypePairUnparsed._1, nftLink)

        val tweet = Tweet(
          id = new String(
            ErgoJson
              .getRegister(
                additionalRegisterJson,
                RegisterType.CollByte,
                Register.R4
              )
              .get
              .asInstanceOf[Array[Byte]],
            StandardCharsets.UTF_8
          ),
          message = new String(
            ErgoJson
              .getRegister(
                additionalRegisterJson,
                RegisterType.CollByte,
                Register.R5
              )
              .get
              .asInstanceOf[Array[Byte]],
            StandardCharsets.UTF_8
          ),
          creationHeight =
            nftOutBox.hcursor.downField("creationHeight").as[Long].getOrElse(0),
          author = Address
            .create(
              nftOutBox.hcursor.downField("address").as[String].getOrElse("")
            )
        )
        val tweetWithType = tweet.copy(tweetType = tweetTypePair)

        Option(tweetWithType)
      } else {
        Option.empty
      }
    } catch {
      case e: Throwable => {
        println(e.getMessage)
        Option.empty
      }
    }
}

case class Tweet(
  id: String,
  message: String,
  creationHeight: Long,
  author: Address,
  tweetType: (Int, String) = null
) {

  def toJson: Json =
    Json.fromFields(
      List(
        ("id", Json.fromString(this.id)),
        ("message", Json.fromString(this.message)),
        ("creationHeight", Json.fromLong(creationHeight)),
        ("authorAddress", Json.fromString(author.toString)),
        (
          "tweetType",
          Json.fromFields(
            List(
              ("type", Json.fromInt(tweetType._1)),
              ("toId", Json.fromString(tweetType._2))
            )
          )
        )
      )
    )
}
